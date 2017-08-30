/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.uftrace.core.analysis;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackStateProvider;
import org.eclipse.tracecompass.incubator.internal.uftrace.core.trace.DatEvent;
import org.eclipse.tracecompass.incubator.internal.uftrace.core.trace.UfEventType;
import org.eclipse.tracecompass.incubator.internal.uftrace.core.trace.Uftrace;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * UFTrace callstack provider
 *
 * @author Matthew Khouzam
 */
public class UfCallstackProvider extends CallStackStateProvider {

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     */
    public UfCallstackProvider(@NonNull Uftrace trace) {
        super(trace);
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public @NonNull Uftrace getTrace() {
        ITmfTrace trace = super.getTrace();
        if (trace instanceof Uftrace) {
            return (Uftrace) trace;
        }
        throw new IllegalStateException("Trace of the wrong type " + trace); //$NON-NLS-1$
    }

    @Override
    public @NonNull CallStackStateProvider getNewInstance() {
        return new UfCallstackProvider(getTrace());
    }

    @Override
    protected boolean considerEvent(@NonNull ITmfEvent event) {
        return true;
    }

    @Override
    protected @Nullable ITmfStateValue functionEntry(@NonNull ITmfEvent event) {
        ITmfEventField content = event.getContent();
        Object payload = content.getValue();
        if (payload instanceof DatEvent) {
            DatEvent ev = (DatEvent) payload;
            if (event.getType().equals(UfEventType.ENTRY)) {
                return TmfStateValue.newValueLong(ev.getAddress());
            }
        }
        return null;
    }

    @Override
    protected @Nullable ITmfStateValue functionExit(@NonNull ITmfEvent event) {
        ITmfEventField content = event.getContent();
        Object payload = content.getValue();
        if (payload instanceof DatEvent) {
            DatEvent ev = (DatEvent) payload;
            if (event.getType().equals(UfEventType.EXIT)) {
                return TmfStateValue.newValueLong(ev.getAddress());
            }
        }
        return null;
    }

    @Override
    protected int getProcessId(@NonNull ITmfEvent event) {
        return getTrace().getTasks().getPid((int) getThreadId(event));
    }

    @Override
    protected long getThreadId(@NonNull ITmfEvent event) {
        ITmfEventField content = event.getContent();
        Object payload = content.getValue();
        if (payload instanceof DatEvent) {
            DatEvent ev = (DatEvent) payload;
            return ev.getTid();
        }
        return 0;
    }
}
