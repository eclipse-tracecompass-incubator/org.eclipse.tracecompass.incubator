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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.CallStack;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.CallStackSeries.IThreadIdProvider;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.CallStackSeries.IThreadIdResolver;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.ICallStackLeafElement;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.statesystem.CallStackAnalysis;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;

/**
 * A callstack leaf element corresponding to an attribute in the state system.
 * The leaf element is created with a quark that represents the last call stack
 * group descriptor, but under this element in the state system, there should be
 * another element {@link #CALL_STACK}, under which the actual stack is.
 *
 * Note: The main leaf element may also serve to store other data than the
 * callstack, like the thread ID for some use cases, etc. That is why there is
 * an additional element to store the callstack, but that should be completely
 * invisible to the users of this class.
 *
 * @author Geneviève Bastien
 */
public class CallStackLeafElement extends CallStackElement implements ICallStackLeafElement {

    private final @Nullable IThreadIdProvider fThreadIdProvider;

    /**
     * Constructor
     *
     * @param hostId
     *            The ID of the host this callstack part of
     * @param ss
     *            The state system containing the callstack
     * @param group
     *            The group corresponding to this element
     * @param quark
     *            The quark corresponding to this element
     * @param symbolKeyElement
     *            The symbol key element
     * @param threadIdResolver
     *            The object describing how to resolve the thread ID
     * @param parent
     *            The parent element or <code>null</code> if this is the root
     *            element
     */
    public CallStackLeafElement(String hostId, ITmfStateSystem ss, int quark, CallStackGroupDescriptor group, @Nullable ICallStackElement symbolKeyElement, @Nullable IThreadIdResolver threadIdResolver, @Nullable CallStackElement parent) {
        super(hostId, ss, quark, group, null, symbolKeyElement, threadIdResolver, parent);
        if (threadIdResolver != null) {
            fThreadIdProvider = threadIdResolver.resolve(hostId, this);
        } else {
            fThreadIdProvider = null;
        }
    }

    @Override
    public @NonNull List<@NonNull ICallStackElement> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public int getSymbolKeyAt(long startTime) {
        int processId = -1;
        ICallStackElement symbolKeyElement = getSymbolKeyElement();
        if (symbolKeyElement != null && symbolKeyElement != this) {
            return symbolKeyElement.getSymbolKeyAt(startTime);
        }
        return processId;
    }

    @Override
    public @NonNull CallStack getCallStack() {
        int stackQuark = getStateSystem().optQuarkRelative(getQuark(), CallStackAnalysis.CALL_STACK);
        if (stackQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            throw new IllegalStateException("The leaf element should have an element called " + CallStackAnalysis.CALL_STACK); //$NON-NLS-1$
        }
        List<Integer> subAttributes = getStateSystem().getSubAttributes(stackQuark, false);
        return new CallStack(getStateSystem(), subAttributes, getSymbolKeyElement(), getHostId(), fThreadIdProvider);
    }

}
