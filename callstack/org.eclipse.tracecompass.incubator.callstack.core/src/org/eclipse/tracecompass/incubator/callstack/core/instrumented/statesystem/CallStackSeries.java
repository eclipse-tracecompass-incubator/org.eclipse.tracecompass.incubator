/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.incubator.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.incubator.analysis.core.model.ModelManager;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.flamechart.CallStack;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.ICalledFunction;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackHostUtils.IHostIdProvider;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackHostUtils.IHostIdResolver;
import org.eclipse.tracecompass.incubator.internal.callstack.core.Activator;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.InstrumentedCallStackElement;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.InstrumentedGroupDescriptor;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.callgraph.CalledFunctionFactory;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

/**
 * A callstack series contain the information necessary to build all the
 * different callstacks from a same pattern.
 *
 * Example: Let's take a trace that registers function entry and exit for
 * threads and where events also provide information on some other stackable
 * application component:
 *
 * The structure of this callstack in the state system could be as follows:
 *
 * <pre>
 *  Per PID
 *    [pid]
 *        [tid]
 *            callstack
 *               1  -> function name
 *               2  -> function name
 *               3  -> function name
 *  Per component
 *    [application component]
 *       [tid]
 *           callstack
 *               1 -> some string
 *               2 -> some string
 * </pre>
 *
 * There are 2 {@link CallStackSeries} in this example, one starting by "Per
 * PID" and another "Per component". For the first series, there could be 3
 * {@link ICallStackGroupDescriptor}: "Per PID/*", "*", "callstack".
 *
 * If the function names happen to be addresses in an executable and the PID is
 * the key to map those symbols to actual function names, then the first group
 * "Per PID/*" would be the symbol key group.
 *
 * Each group descriptor can get the corresponding {@link ICallStackElement}s,
 * ie, for the first group, it would be all the individual pids in the state
 * system, and for the second group, it would be the application components.
 * Each element that is not a leaf element (check with
 * {@link ICallStackElement#isLeaf()}) will have a next group descriptor that
 * can fetch the elements under it. The last group will resolve to leaf elements
 * and each leaf elements has one {@link CallStack} object.
 *
 * @author Geneviève Bastien
 */
public class CallStackSeries implements ISegmentStore<ISegment> {

    /**
     * Interface for classes that provide a thread ID at time t for a callstack. The
     * thread ID can be used to calculate extra statistics per thread, for example,
     * the CPU time of each call site.
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
        int getThreadId(long time);

    }

    /**
     * This class uses the value of an attribute as a thread ID.
     */
    private static final class AttributeValueThreadProvider implements IThreadIdProvider {

        private final ITmfStateSystem fSs;
        private final int fQuark;
        private @Nullable ITmfStateInterval fInterval;
        private int fLastThreadId = IHostModel.UNKNOWN_TID;

        public AttributeValueThreadProvider(ITmfStateSystem ss, int quark) {
            fSs = ss;
            fQuark = quark;
        }

        @Override
        public int getThreadId(long time) {
            ITmfStateInterval interval = fInterval;
            int tid = fLastThreadId;
            if (interval != null && interval.intersects(time)) {
                return fLastThreadId;
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
        public int getThreadId(long time) {
            return fTid;
        }

    }

    /**
     * This class will retrieve the thread ID
     */
    private static final class CpuThreadProvider implements IThreadIdProvider {

        private final ITmfStateSystem fSs;
        private final int fCpuQuark;
        private final IHostIdProvider fHostProvider;

        public CpuThreadProvider(IHostIdProvider hostProvider, ITmfStateSystem ss, int quark, String[] path) {
            fSs = ss;
            fHostProvider = hostProvider;
            // Get the cpu quark
            List<@NonNull Integer> quarks = ss.getQuarks(quark, path);
            fCpuQuark = quarks.isEmpty() ? ITmfStateSystem.INVALID_ATTRIBUTE : quarks.get(0);
        }

        @Override
        public int getThreadId(long time) {
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
                // The thread running is the one on the CPU at the beginning of this interval
                long startTime = querySingleState.getStartTime();
                IHostModel model = ModelManager.getModelFor(fHostProvider.apply(startTime));
                return model.getThreadOnCpu(cpu, startTime);
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
         * @param hostProvider
         *            The provider of the host ID for the callstack
         * @param element
         *            The leaf element of the callstack
         * @return The thread ID provider
         */
        @Nullable IThreadIdProvider resolve(IHostIdProvider hostProvider, ICallStackElement element);

    }

    /**
     * This class will resolve the thread ID provider by the value of a attribute at
     * a given depth
     */
    public static final class AttributeValueThreadResolver implements IThreadIdResolver {

        private int fLevel;

        /**
         * Constructor
         *
         * @param level
         *            The depth of the element whose value will be used to retrieve the
         *            thread ID
         */
        public AttributeValueThreadResolver(int level) {
            fLevel = level;
        }

        @Override
        public @Nullable IThreadIdProvider resolve(IHostIdProvider hostProvider, ICallStackElement element) {
            if (!(element instanceof InstrumentedCallStackElement)) {
                throw new IllegalArgumentException();
            }
            InstrumentedCallStackElement insElement = (InstrumentedCallStackElement) element;

            List<InstrumentedCallStackElement> elements = new ArrayList<>();
            InstrumentedCallStackElement el = insElement;
            while (el != null) {
                elements.add(el);
                el = el.getParentElement();
            }
            Collections.reverse(elements);
            if (elements.size() <= fLevel) {
                return null;
            }
            InstrumentedCallStackElement stackElement = elements.get(fLevel);
            return new AttributeValueThreadProvider(stackElement.getStateSystem(), stackElement.getQuark());
        }

    }

    /**
     * This class will resolve the thread ID provider by the value of a attribute at
     * a given depth
     */
    public static final class AttributeNameThreadResolver implements IThreadIdResolver {

        private int fLevel;

        /**
         * Constructor
         *
         * @param level
         *            The depth of the element whose value will be used to retrieve the
         *            thread ID
         */
        public AttributeNameThreadResolver(int level) {
            fLevel = level;
        }

        @Override
        public @Nullable IThreadIdProvider resolve(IHostIdProvider hostProvider, ICallStackElement element) {
            if (!(element instanceof InstrumentedCallStackElement)) {
                throw new IllegalArgumentException();
            }
            InstrumentedCallStackElement insElement = (InstrumentedCallStackElement) element;

            List<InstrumentedCallStackElement> elements = new ArrayList<>();
            InstrumentedCallStackElement el = insElement;
            while (el != null) {
                elements.add(el);
                el = el.getParentElement();
            }
            Collections.reverse(elements);
            if (elements.size() <= fLevel) {
                return null;
            }
            InstrumentedCallStackElement stackElement = elements.get(fLevel);
            return new AttributeNameThreadProvider(stackElement.getStateSystem(), stackElement.getQuark());
        }

    }

    /**
     * This class will resolve the thread ID from the CPU on which the callstack was
     * running at a given time
     */
    public static final class CpuResolver implements IThreadIdResolver {

        private String[] fPath;

        /**
         * Constructor
         *
         * @param path
         *            The path relative to the leaf element that will contain the CPU ID
         */
        public CpuResolver(String[] path) {
            fPath = path;
        }

        @Override
        public @Nullable IThreadIdProvider resolve(IHostIdProvider hostProvider, ICallStackElement element) {
            if (!(element instanceof InstrumentedCallStackElement)) {
                throw new IllegalArgumentException();
            }
            InstrumentedCallStackElement insElement = (InstrumentedCallStackElement) element;

            return new CpuThreadProvider(hostProvider, insElement.getStateSystem(), insElement.getQuark(), fPath);
        }

    }

    private final InstrumentedGroupDescriptor fRootGroup;
    private final String fName;
    private final @Nullable IThreadIdResolver fResolver;
    private final IHostIdResolver fHostResolver;

    /**
     * Constructor
     *
     * @param ss
     *            The state system containing this call stack
     * @param patternPaths
     *            The patterns for the different levels of the callstack in the
     *            state system. Any further level path is relative to the previous
     *            one.
     * @param symbolKeyLevelIndex
     *            The index in the list of the list to be used as a key to the
     *            symbol provider. The data at this level must be an integer, for
     *            instance a process ID
     * @param name
     *            A name for this callstack
     * @param hostResolver
     *            The host ID resolver for this callstack
     * @param threadResolver
     *            The thread resolver
     */
    public CallStackSeries(ITmfStateSystem ss, List<String[]> patternPaths, int symbolKeyLevelIndex, String name, IHostIdResolver hostResolver, @Nullable IThreadIdResolver threadResolver) {
        // Build the groups from the state system and pattern paths
        if (patternPaths.isEmpty()) {
            throw new IllegalArgumentException("State system callstack: the list of paths should not be empty"); //$NON-NLS-1$
        }
        int startIndex = patternPaths.size() - 1;
        InstrumentedGroupDescriptor prevLevel = new InstrumentedGroupDescriptor(ss, patternPaths.get(startIndex), null, symbolKeyLevelIndex == startIndex ? true : false);
        for (int i = startIndex - 1; i >= 0; i--) {
            InstrumentedGroupDescriptor level = new InstrumentedGroupDescriptor(ss, patternPaths.get(i), prevLevel, symbolKeyLevelIndex == i ? true : false);
            prevLevel = level;
        }
        fRootGroup = prevLevel;
        fName = name;
        fResolver = threadResolver;
        fHostResolver = hostResolver;
    }

    /**
     * Get the root elements of this callstack series
     *
     * @return The root elements of the callstack series
     */
    public Collection<ICallStackElement> getRootElements() {
        return InstrumentedCallStackElement.getRootElements(fRootGroup, fHostResolver, fResolver);
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
     * Get the name of this callstack series
     *
     * @return The name of the callstack series
     */
    public String getName() {
        return fName;
    }

    // ---------------------------------------------------
    // Segment store methods
    // ---------------------------------------------------

    private Collection<ICallStackElement> getLeafElements(ICallStackElement element) {
        if (element.isLeaf()) {
            return Collections.singleton(element);
        }
        List<ICallStackElement> list = new ArrayList<>();
        element.getChildren().forEach(e -> list.addAll(getLeafElements(e)));
        return list;
    }

    @Override
    public int size() {
        return Iterators.size(iterator());
    }

    @Override
    public boolean isEmpty() {
        return !iterator().hasNext();
    }

    @Override
    public boolean contains(@Nullable Object o) {
        // narrow down search when object is a segment
        if (o instanceof ICalledFunction) {
            ICalledFunction seg = (ICalledFunction) o;
            Iterable<@NonNull ISegment> iterable = getIntersectingElements(seg.getStart());
            return Iterables.contains(iterable, seg);
        }
        return false;
    }

    @Override
    public Iterator<ISegment> iterator() {
        ITmfStateSystem stateSystem = fRootGroup.getStateSystem();
        long start = stateSystem.getStartTime();
        long end = stateSystem.getCurrentEndTime();
        return getIntersectingElements(start, end).iterator();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("This segment store can potentially cause OutOfMemoryExceptions"); //$NON-NLS-1$
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("This segment store can potentially cause OutOfMemoryExceptions"); //$NON-NLS-1$
    }

    @Override
    public boolean add(ISegment e) {
        throw new UnsupportedOperationException("This segment store does not support adding new segments"); //$NON-NLS-1$
    }

    @Override
    public boolean containsAll(@Nullable Collection<?> c) {
        if (c == null) {
            return false;
        }
        /*
         * Check that all elements in the collection are indeed ISegments, and
         * find their min end and max start time
         */
        long minEnd = Long.MAX_VALUE, maxStart = Long.MIN_VALUE;
        for (Object o : c) {
            if (o instanceof ICalledFunction) {
                ICalledFunction seg = (ICalledFunction) o;
                minEnd = Math.min(minEnd, seg.getEnd());
                maxStart = Math.max(maxStart, seg.getStart());
            } else {
                return false;
            }
        }
        if (minEnd > maxStart) {
            /*
             * all segments intersect a common range, we just need to intersect
             * a time stamp in that range
             */
            minEnd = maxStart;
        }

        /* Iterate through possible segments until we have found them all */
        Iterator<@NonNull ISegment> iterator = getIntersectingElements(minEnd, maxStart).iterator();
        int unFound = c.size();
        while (iterator.hasNext() && unFound > 0) {
            ISegment seg = iterator.next();
            for (Object o : c) {
                if (Objects.equals(o, seg)) {
                    unFound--;
                }
            }
        }
        return unFound == 0;
    }

    @Override
    public boolean addAll(@Nullable Collection<? extends ISegment> c) {
        throw new UnsupportedOperationException("This segment store does not support adding new segments"); //$NON-NLS-1$
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("This segment store does not support clearing the data"); //$NON-NLS-1$
    }

    private Map<Integer, CallStack> getCallStackQuarks() {
        Map<Integer, CallStack> quarkToElement = new HashMap<>();
        // Get the leaf elements and their callstacks
        getRootElements().stream().flatMap(e -> getLeafElements(e).stream())
                .filter(e -> e instanceof InstrumentedCallStackElement)
                .map(e -> (InstrumentedCallStackElement) e)
                .forEach(e -> {
                    e.getStackQuarks().forEach(c -> quarkToElement.put(c, e.getCallStack()));
                });
        return quarkToElement;
    }

    @Override
    public Iterable<ISegment> getIntersectingElements(long start, long end) {
        ITmfStateSystem stateSystem = fRootGroup.getStateSystem();
        long startTime = Math.max(start - 1, stateSystem.getStartTime());
        long endTime = Math.min(end, stateSystem.getCurrentEndTime());
        if (startTime > endTime) {
            return Collections.emptyList();
        }
        Map<Integer, CallStack> quarksToElement = getCallStackQuarks();
        try {
            Iterable<ITmfStateInterval> query2d = stateSystem.query2D(quarksToElement.keySet(), startTime, endTime);
            query2d = Iterables.filter(query2d, interval -> !interval.getStateValue().isNull());
            Function<ITmfStateInterval, ICalledFunction> fct = interval -> {
                CallStack callstack = quarksToElement.get(interval.getAttribute());
                if (callstack == null) {
                    throw new NullPointerException("The quark was in that map in the first place, there must be a callstack to go with it!"); //$NON-NLS-1$
                }
                HostThread hostThread = callstack.getHostThread(interval.getStartTime());
                if (hostThread == null) {
                    hostThread = new HostThread(StringUtils.EMPTY, IHostModel.UNKNOWN_TID);
                }
                return CalledFunctionFactory.create(interval.getStartTime(), interval.getEndTime() + 1, 0, interval.getStateValue(), -1, hostThread.getTid(),
                        null, ModelManager.getModelFor(hostThread.getHost()));
            };
            return Iterables.transform(query2d, fct);
        } catch (StateSystemDisposedException e) {
            Activator.getInstance().logError("Error getting intersecting elements: StateSystemDisposed"); //$NON-NLS-1$
        }
        return Collections.emptyList();
    }

    @Override
    public void dispose() {
        // Nothing to do
    }

}
