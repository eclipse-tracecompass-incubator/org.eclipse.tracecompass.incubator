/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.callstack;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.CallStack;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.ICallStackLeafElement;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.CallStackSeries.IThreadIdProvider;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.CallStackSeries.IThreadIdResolver;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;

/**
 *  A callstack leaf element corresponding to an attribute in the state system
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
    public CallStackLeafElement(String hostId, ITmfStateSystem ss, int quark, @Nullable ICallStackElement symbolKeyElement, @Nullable IThreadIdResolver threadIdResolver, @Nullable CallStackElement parent) {
        super(hostId, ss, quark, null, symbolKeyElement, threadIdResolver, parent);
        if (threadIdResolver != null) {
            fThreadIdProvider = threadIdResolver.resolve(hostId, this);
        } else {
            fThreadIdProvider = null;
        }
    }

    @Override
    public @NonNull List<@NonNull ICallStackElement> getChildren() {
        return Collections.EMPTY_LIST;
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
        List<Integer> subAttributes = getStateSystem().getSubAttributes(getQuark(), false);
        return new CallStack(getStateSystem(), subAttributes, getSymbolKeyElement(), getHostId(), fThreadIdProvider);
    }

    @Override
    public @NonNull Collection<ICallStackLeafElement> getLeafElements() {
        return Collections.singletonList(this);
    }

}
