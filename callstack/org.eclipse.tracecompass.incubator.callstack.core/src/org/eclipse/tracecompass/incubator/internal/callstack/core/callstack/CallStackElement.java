/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.callstack;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.CallStackSeries.IThreadIdResolver;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.ICallStackGroupDescriptor;
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
public class CallStackElement implements ICallStackElement {

    private final ITmfStateSystem fStateSystem;
    private final int fQuark;
    private final CallStackGroupDescriptor fGroup;
    private final @Nullable CallStackGroupDescriptor fNextGroup;
    private final @Nullable CallStackElement fParent;
    private final String fHostId;

    private @Nullable ICallStackElement fSymbolKeyElement;
    private @Nullable List<ICallStackElement> fChildren;
    private @Nullable IThreadIdResolver fThreadIdProvider = null;

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
     * @param symbolKeyElement
     *            The symbol key element
     * @param threadIdResolver
     *            The object describing how to resolve the thread ID
     * @param parent
     *            The parent element or <code>null</code> if this is the root
     *            element
     */
    public CallStackElement(String hostId, ITmfStateSystem stateSystem, Integer quark,
            CallStackGroupDescriptor group,
            @Nullable CallStackGroupDescriptor nextGroup,
            @Nullable ICallStackElement symbolKeyElement,
            @Nullable IThreadIdResolver threadIdResolver,
            @Nullable CallStackElement parent) {
        fStateSystem = stateSystem;
        fQuark = quark;
        fGroup = group;
        fNextGroup = nextGroup;
        fHostId = hostId;
        fSymbolKeyElement = symbolKeyElement;
        fThreadIdProvider = threadIdResolver;
        fParent = parent;
    }

    @Override
    public List<ICallStackElement> getChildren() {
        List<ICallStackElement> children = fChildren;
        if (children == null) {
            // Get the elements from the next group in the hierarchy
            CallStackGroupDescriptor nextGroup = fNextGroup;
            if (nextGroup == null) {
                children = Collections.EMPTY_LIST;
            } else {
                children = nextGroup.getElements(this, fQuark, fSymbolKeyElement, fThreadIdProvider);
            }
            fChildren = children;
        }
        return children;
    }

    @Override
    public void setSymbolKeyElement(ICallStackElement element) {
        fSymbolKeyElement = element;
    }

    /**
     * Get the element used to retrieve symbol keys
     *
     * @return The element used to retrieve symbol keys or <code>null</code> if
     *         unavailable
     */
    protected @Nullable ICallStackElement getSymbolKeyElement() {
        return fSymbolKeyElement;
    }

    /**
     * Get the thread ID provider, the object that will retrieve the thread ID
     * of a given stack
     *
     * @return The thread ID provider or <code>null</code> if unavailable
     */
    protected @Nullable IThreadIdResolver getThreadIdProvider() {
        return fThreadIdProvider;
    }

    @Override
    public String getHostId() {
        return fHostId;
    }

    @Override
    public ICallStackGroupDescriptor getGroup() {
        return fGroup;
    }

    @Override
    public @Nullable CallStackGroupDescriptor getNextGroup() {
        return fNextGroup;
    }

    @Override
    public @NonNull String getName() {
        if (fQuark == ITmfStateSystem.ROOT_ATTRIBUTE) {
            return StringUtils.EMPTY;
        }
        return fStateSystem.getAttributeName(fQuark);
    }

    @Override
    public int getSymbolKeyAt(long startTime) {
        int processId = ICallStackElement.DEFAULT_SYMBOL_KEY;
        ICallStackElement symbolKeyElement = fSymbolKeyElement;
        // if there is no symbol key element, return the default value
        if (symbolKeyElement == null) {
            return processId;
        }
        // If the symbol key element is not this one, then call the symbol key
        // element's value
        if (symbolKeyElement != this) {
            return symbolKeyElement.getSymbolKeyAt(startTime);
        }
        // This element is the symbol key eleemnt, so try to find the key if the
        // quark is not the root attribute
        if (fQuark != ITmfStateSystem.ROOT_ATTRIBUTE) {
            try {
                // Query a time that is within the bounds of the state system
                long start = Math.max(fStateSystem.getStartTime(), startTime);
                start = Math.max(startTime, fStateSystem.getCurrentEndTime());

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

    @Override
    public boolean isSymbolKeyElement() {
        return (fSymbolKeyElement == this);
    }

    @Override
    public @Nullable CallStackElement getParentElement() {
        return fParent;
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

}
