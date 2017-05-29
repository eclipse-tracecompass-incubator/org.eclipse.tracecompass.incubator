/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.callstack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.incubator.analysis.core.model.ModelManager;
import org.eclipse.tracecompass.incubator.internal.callstack.core.callstack.CallStackAllGroupDescriptor;
import org.eclipse.tracecompass.incubator.internal.callstack.core.callstack.CallStackElement;
import org.eclipse.tracecompass.incubator.internal.callstack.core.callstack.CallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.internal.callstack.core.callstack.CallStackLeafElement;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;

/**
 * A callstack series contain the information necessary to build all the
 * different callstacks from a same pattern.
 *
 * @author Geneviève Bastien
 */
public class CallStackSeries {

    /**
     * Interface for classes that provide a thread ID at time t for a callstack
     */
    public interface IThreadIdProvider {

        /**
         * Get the ID of callstack thread at a given time
         *
         * @param time
         *            The time of request
         * @return The ID of the thread, or {@link IHostModel#UNKNOWN_TID} if
         *         unavailable
         */
        int getTheadId(long time);

    }

    /**
     * This class uses the value of an attribute as a thread ID.
     */
    private static final class AttributeValueThreadProvider implements IThreadIdProvider {

        private final ITmfStateSystem fSs;
        private final int fQuark;
        private transient @Nullable ITmfStateInterval fInterval;
        private transient int fLastThreadId = IHostModel.UNKNOWN_TID;

        public AttributeValueThreadProvider(ITmfStateSystem ss, int quark) {
            fSs = ss;
            fQuark = quark;
        }

        @Override
        public int getTheadId(long time) {
            ITmfStateInterval interval = fInterval;
            int tid = fLastThreadId;
            if (interval != null) {
                if (time >= interval.getStartTime() && time <= interval.getEndTime()) {
                    return fLastThreadId;
                }
            }
            try {
                interval = fSs.querySingleState(time, fQuark);
                switch (interval.getStateValue().getType()) {
                case INTEGER:
                    tid = interval.getStateValue().unboxInt();
                    break;
                case LONG:
                    tid = (int) interval.getStateValue().unboxLong();
                    break;
                case STRING:
                    try {
                        Integer.valueOf(interval.getStateValue().unboxStr());
                    } catch (NumberFormatException e) {
                        tid = IHostModel.UNKNOWN_TID;
                    }
                    break;
                case NULL: /* Fallthrough cases */
                case DOUBLE: /* Fallthrough cases */
                case CUSTOM: /* Fallthrough cases */
                default:
                    break;

                }
            } catch (StateSystemDisposedException e) {
                interval = null;
                tid = IHostModel.UNKNOWN_TID;
            }
            fInterval = interval;
            fLastThreadId = tid;
            return tid;
        }

    }

    /**
     * This class uses the value of an attribute as a thread ID.
     */
    private static final class AttributeNameThreadProvider implements IThreadIdProvider {

        private final int fTid;

        public AttributeNameThreadProvider(ITmfStateSystem ss, int quark) {
            int tid = IHostModel.UNKNOWN_TID;
            try {
                String attributeName = ss.getAttributeName(quark);
                tid = Integer.valueOf(attributeName);
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                tid = IHostModel.UNKNOWN_TID;
            }
            fTid = tid;
        }

        @Override
        public int getTheadId(long time) {
            return fTid;
        }

    }

    /**
     * This class will retrieve the thread ID
     */
    private static final class CpuThreadProvider implements IThreadIdProvider {

        private final ITmfStateSystem fSs;
        private final int fCpuQuark;
        private final IHostModel fModel;

        public CpuThreadProvider(String hostId, ITmfStateSystem ss, int quark, String[] path) {
            fSs = ss;
            fModel = ModelManager.getModelFor(hostId);
            // Get the cpu quark
            List<@NonNull Integer> quarks = ss.getQuarks(quark, path);
            fCpuQuark = quarks.isEmpty() ? ITmfStateSystem.INVALID_ATTRIBUTE : quarks.get(0);
        }

        @Override
        public int getTheadId(long time) {
            if (fCpuQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                return IHostModel.UNKNOWN_TID;
            }
            // Get the CPU
            try {
                ITmfStateInterval querySingleState = fSs.querySingleState(time, fCpuQuark);

                if (querySingleState.getStateValue().isNull()) {
                    return IHostModel.UNKNOWN_TID;
                }
                int cpu = querySingleState.getStateValue().unboxInt();
                return fModel.getThreadOnCpu(cpu, time);
            } catch (StateSystemDisposedException e) {

            }
            return IHostModel.UNKNOWN_TID;
        }

    }

    /**
     * Interface for describing how a callstack will get the thread ID
     */
    public interface IThreadIdResolver {

        /**
         * Get the actual thread ID provider from this resolver
         *
         * @param hostId
         *            The ID of the host the callstack is from
         * @param element
         *            The leaf element of the callstack
         * @return The thread ID provider
         */
        @Nullable
        IThreadIdProvider resolve(String hostId, ICallStackLeafElement element);

    }

    /**
     * This class will resolve the thread ID provider by the value of a
     * attribute at a given depth
     */
    public static final class AttributeValueThreadResolver implements IThreadIdResolver {

        private int fLevel;

        /**
         * Constructor
         *
         * @param level
         *            The depth of the element whose value will be used to
         *            retrieve the thread ID
         */
        public AttributeValueThreadResolver(int level) {
            fLevel = level;
        }

        @Override
        public @Nullable IThreadIdProvider resolve(String hostId, ICallStackLeafElement element) {
            if (!(element instanceof CallStackLeafElement)) {
                throw new IllegalArgumentException();
            }
            CallStackLeafElement leaf = (CallStackLeafElement) element;

            List<CallStackElement> elements = new ArrayList<>();
            CallStackElement el = leaf;
            while (el != null) {
                elements.add(el);
                el = el.getParentElement();
            }
            Collections.reverse(elements);
            if (elements.size() < fLevel) {
                return null;
            }
            CallStackElement stackElement = elements.get(fLevel);
            return new AttributeValueThreadProvider(stackElement.getStateSystem(), stackElement.getQuark());
        }

    }

    /**
     * This class will resolve the thread ID provider by the value of a
     * attribute at a given depth
     */
    public static final class AttributeNameThreadResolver implements IThreadIdResolver {

        private int fLevel;

        /**
         * Constructor
         *
         * @param level
         *            The depth of the element whose value will be used to
         *            retrieve the thread ID
         */
        public AttributeNameThreadResolver(int level) {
            fLevel = level;
        }

        @Override
        public @Nullable IThreadIdProvider resolve(String hostId, ICallStackLeafElement element) {
            if (!(element instanceof CallStackLeafElement)) {
                throw new IllegalArgumentException();
            }
            CallStackLeafElement leaf = (CallStackLeafElement) element;

            List<CallStackElement> elements = new ArrayList<>();
            CallStackElement el = leaf;
            while (el != null) {
                elements.add(el);
                el = el.getParentElement();
            }
            Collections.reverse(elements);
            if (elements.size() < fLevel) {
                return null;
            }
            CallStackElement stackElement = elements.get(fLevel);
            return new AttributeNameThreadProvider(stackElement.getStateSystem(), stackElement.getQuark());
        }

    }

    /**
     * This class will resolve the thread ID from the CPU on which the callstack
     * was running at a given time
     */
    public static final class CpuResolver implements IThreadIdResolver {

        private String[] fPath;

        /**
         * Constructor
         *
         * @param path
         *            The path relative to the leaf element that will contain
         *            the CPU ID
         */
        public CpuResolver(String[] path) {
            fPath = path;
        }

        @Override
        public @Nullable IThreadIdProvider resolve(String hostId, ICallStackLeafElement element) {
            if (!(element instanceof CallStackLeafElement)) {
                throw new IllegalArgumentException();
            }
            CallStackLeafElement leaf = (CallStackLeafElement) element;

            return new CpuThreadProvider(hostId, leaf.getStateSystem(), leaf.getQuark(), fPath);
        }

    }

    private final ICallStackGroupDescriptor fRootGroup;
    private final ICallStackGroupDescriptor fAllGroup;
    private final String fName;
    private @Nullable IThreadIdResolver fResolver;

    /**
     * Constructor
     *
     * @param ss
     *            The state system containing this call stack
     * @param patternPaths
     *            The patterns for the different levels of the callstack in the
     *            state system. Any further level path is relative to the
     *            previous one.
     * @param symbolKeyLevelIndex
     *            The index in the list of the list to be used as a key to the
     *            symbol provider. The data at this level must be an integer,
     *            for instance a process ID
     * @param name
     *            A name for this callstack
     * @param hostId
     *            The ID of the host where this callstack happens
     * @param threadResolver
     *            The thread resolver
     */
    public CallStackSeries(ITmfStateSystem ss, List<String[]> patternPaths, int symbolKeyLevelIndex, String name, String hostId, @Nullable IThreadIdResolver threadResolver) {
        // Build the groups from the state system and pattern paths
        if (patternPaths.isEmpty()) {
            throw new IllegalArgumentException("State system callstack: the list of paths should not be empty"); //$NON-NLS-1$
        }
        int startIndex = patternPaths.size() - 1;
        CallStackGroupDescriptor prevLevel = new CallStackGroupDescriptor(ss, patternPaths.get(startIndex), null, symbolKeyLevelIndex == startIndex ? true : false, hostId);
        for (int i = startIndex - 1; i >= 0; i--) {
            CallStackGroupDescriptor level = new CallStackGroupDescriptor(ss, patternPaths.get(i), prevLevel, symbolKeyLevelIndex == i ? true : false, hostId);
            prevLevel = level;
        }
        fRootGroup = prevLevel;
        fName = name;
        fResolver = threadResolver;
        fAllGroup = new CallStackAllGroupDescriptor(this);
    }

    /**
     * Get the root elements of this callstack series
     *
     * @return The root elements of the callstack series
     */
    public List<ICallStackElement> getRootElements() {
        return fRootGroup.getElements(null, ITmfStateSystem.ROOT_ATTRIBUTE, null, fResolver);
    }

    /**
     * Get the root group of the callstack series
     *
     * @return The root group descriptor
     */
    public ICallStackGroupDescriptor getRootGroup() {
        return fRootGroup;
    }

    /**
     * Get a group descriptor that describes the aggregation of all groups of
     * the series
     *
     * @return The group descriptor for all series
     */
    public ICallStackGroupDescriptor getAllGroup() {
        return fAllGroup;
    }

    /**
     * Get the name of this callstack series
     *
     * @return The name of the callstack series
     */
    public String getName() {
        return fName;
    }

    /**
     * Get all the final elements of this series that correspond to different
     * callstacks
     *
     * @return The list of final elements
     */
    public List<ICallStackLeafElement> getLeafElements() {
        List<ICallStackLeafElement> leafElements = new ArrayList<>();
        getRootElements().forEach(el -> leafElements.addAll(el.getLeafElements()));
        return leafElements;
    }

}
