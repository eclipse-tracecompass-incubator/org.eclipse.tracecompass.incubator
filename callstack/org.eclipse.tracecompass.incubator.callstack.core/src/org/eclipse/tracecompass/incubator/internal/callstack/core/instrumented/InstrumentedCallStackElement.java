/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.base.CallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.flamechart.CallStack;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackSeries.IThreadIdProvider;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackSeries.IThreadIdResolver;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue.Type;

/**
 * A callstack element corresponding to an attribute in the state system
 *
 * @author Geneviève Bastien
 */
public class InstrumentedCallStackElement extends CallStackElement {

    private static final String INSTRUMENTED = "instrumented"; //$NON-NLS-1$

    private final ITmfStateSystem fStateSystem;
    private final int fQuark;
    private final String fHostId;
    private final @Nullable IThreadIdResolver fThreadIdResolver;

    private @Nullable Collection<ICallStackElement> fChildren;
    private @Nullable IThreadIdProvider fThreadIdProvider = null;

    /**
     * Constructor
     *
     * @param hostId
     *            The ID of the host this callstack part of
     * @param stateSystem
     *            The state system containing the callstack
     * @param quark
     *            The quark corresponding to this element
     * @param group
     *            The group descriptor of this element
     * @param nextGroup
     *            The group descriptor of the next group of elements
     * @param threadIdResolver
     *            The object describing how to resolve the thread ID
     * @param parent
     *            The parent element or <code>null</code> if this is the root
     *            element
     */
    public InstrumentedCallStackElement(String hostId, ITmfStateSystem stateSystem, Integer quark,
            InstrumentedGroupDescriptor group,
            @Nullable InstrumentedGroupDescriptor nextGroup,
            @Nullable IThreadIdResolver threadIdResolver,
            @Nullable InstrumentedCallStackElement parent) {
        super(INSTRUMENTED, group, nextGroup, parent);
        fStateSystem = stateSystem;
        fQuark = quark;
        fHostId = hostId;
        fThreadIdResolver = threadIdResolver;
        if (threadIdResolver != null) {
            fThreadIdProvider = threadIdResolver.resolve(hostId, this);
        }
    }

    @Override
    public Collection<ICallStackElement> getChildren() {
        Collection<ICallStackElement> children = fChildren;
        if (children == null) {
            // Get the elements from the next group in the hierarchy
            @Nullable
            ICallStackGroupDescriptor nextGroup = getNextGroup();
            if (!(nextGroup instanceof InstrumentedGroupDescriptor)) {
                children = Collections.EMPTY_LIST;
            } else {
                children = getNextGroupElements((InstrumentedGroupDescriptor) nextGroup);
            }
            fChildren = children;
        }
        return children;
    }

    @Override
    public @Nullable InstrumentedCallStackElement getParentElement() {
        return (InstrumentedCallStackElement) super.getParentElement();
    }

    /**
     * Create the root elements from a root group and its thread ID resolver
     *
     * @param rootGroup
     *            The root group descriptor
     * @param resolver
     *            the thread ID resolver
     * @return A collection of elements that are roots of the given callstack
     *         grouping
     */
    public static Collection<ICallStackElement> getRootElements(InstrumentedGroupDescriptor rootGroup, @Nullable IThreadIdResolver resolver) {
        return getNextElements(rootGroup, rootGroup.getStateSystem(), ITmfStateSystem.ROOT_ATTRIBUTE, rootGroup.getHostId(), resolver, null);
    }

    private Collection<ICallStackElement> getNextGroupElements(InstrumentedGroupDescriptor nextGroup) {
        return getNextElements(nextGroup, fStateSystem, fQuark, fHostId, fThreadIdResolver, this);
    }

    private static Collection<ICallStackElement> getNextElements(InstrumentedGroupDescriptor nextGroup, ITmfStateSystem stateSystem, int baseQuark, String hostId, @Nullable IThreadIdResolver threadIdProvider, @Nullable InstrumentedCallStackElement parent) {
        // Get the elements from the base quark at the given pattern
        List<Integer> quarks = stateSystem.getQuarks(baseQuark, nextGroup.getSubPattern());
        if (quarks.isEmpty()) {
            return Collections.emptyList();
        }

        InstrumentedGroupDescriptor nextLevel = nextGroup.getNextGroup();
        // If the next level is null, then this is a callstack final element
        List<ICallStackElement> elements = new ArrayList<>(quarks.size());
        for (Integer quark : quarks) {
            InstrumentedCallStackElement element = new InstrumentedCallStackElement(hostId, stateSystem, quark,
                    nextGroup, nextLevel, threadIdProvider, parent);
            if (nextGroup.isSymbolKeyGroup()) {
                element.setSymbolKeyElement(element);
            }
            elements.add(element);
        }
        return elements;
    }

    /**
     * Get the thread ID resolver, the object that will retrieve the thread ID of a
     * given stack
     *
     * @return The thread ID provider or <code>null</code> if unavailable
     */
    protected @Nullable IThreadIdResolver getThreadIdResolver() {
        return fThreadIdResolver;
    }

    /**
     * Get the ID of the host this callstack is part of
     *
     * FIXME: Can a callstack cover multiple hosts? Definitely
     *
     * @return the ID of the host this callstack is for
     */
    public String getHostId() {
        return fHostId;
    }

    @Override
    public @NonNull String getName() {
        if (fQuark == ITmfStateSystem.ROOT_ATTRIBUTE) {
            return StringUtils.EMPTY;
        }
        return fStateSystem.getAttributeName(fQuark);
    }

    @Override
    public int retrieveSymbolKeyAt(long startTime) {
        int processId = CallStackElement.DEFAULT_SYMBOL_KEY;
        if (fQuark != ITmfStateSystem.ROOT_ATTRIBUTE) {
            try {
                // Query a time that is within the bounds of the state system
                long start = Math.max(fStateSystem.getStartTime(), startTime);
                start = Math.max(start, fStateSystem.getCurrentEndTime());

                // Query the value of the quark at the requested time
                ITmfStateInterval interval = fStateSystem.querySingleState(start, fQuark);
                ITmfStateValue processStateValue = interval.getStateValue();
                // If the state value is an integer, assume it is the symbol we
                // are looking for
                if (processStateValue.getType() == Type.INTEGER) {
                    processId = processStateValue.unboxInt();
                } else {
                    try {
                        // Otherwise, try to take the attribute name as the key
                        String processName = fStateSystem.getAttributeName(fQuark);
                        processId = Integer.parseInt(processName);
                    } catch (NumberFormatException e) {
                        /* use default processId */
                    }
                }
            } catch (StateSystemDisposedException e) {
                // ignore
            }
        }
        return processId;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + ": [" + fQuark + ']'; //$NON-NLS-1$
    }

    /**
     * Get the state system containing the callstack data
     *
     * @return The state system
     */
    public ITmfStateSystem getStateSystem() {
        return fStateSystem;
    }

    /**
     * Get the quark corresponding to this element
     *
     * @return The quark
     */
    public int getQuark() {
        return fQuark;
    }

    /**
     * Get the callstack associated with this element if this is a leaf element. If
     * it is not a leaf, it throw a {@link NoSuchElementException}
     *
     * @return The call stack
     */
    public CallStack getCallStack() {
        if (!isLeaf()) {
            throw new NoSuchElementException();
        }
        int stackQuark = getStateSystem().optQuarkRelative(getQuark(), InstrumentedCallStackAnalysis.CALL_STACK);
        if (stackQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            throw new IllegalStateException("The leaf element should have an element called " + InstrumentedCallStackAnalysis.CALL_STACK); //$NON-NLS-1$
        }
        List<Integer> subAttributes = getStateSystem().getSubAttributes(stackQuark, false);
        return new CallStack(getStateSystem(), subAttributes, this, getHostId(), fThreadIdProvider);
    }

}
