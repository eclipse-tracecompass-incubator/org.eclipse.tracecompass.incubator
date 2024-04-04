/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.uftrace.core.analysis;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxTidAspect;
import org.eclipse.tracecompass.analysis.profiling.core.callstack.CallStackStateProvider;
import org.eclipse.tracecompass.incubator.internal.uftrace.core.trace.DatEvent;
import org.eclipse.tracecompass.incubator.internal.uftrace.core.trace.UfEventType;
import org.eclipse.tracecompass.incubator.internal.uftrace.core.trace.Uftrace.PidAspect;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.MultiAspect;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * UFTrace callstack provider
 *
 * @author Matthew Khouzam
 */
public class UfCallstackProvider extends CallStackStateProvider {


    private final ITmfEventAspect<Integer> fTidAspect;
    private final ITmfEventAspect<Integer> fPidAspect;

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     */
    public UfCallstackProvider(@NonNull ITmfTrace trace) {
        super(trace);
        fTidAspect = (ITmfEventAspect<Integer>) MultiAspect.<Integer>create(TmfTraceUtils.getEventAspects(trace, LinuxTidAspect.class), LinuxTidAspect.class);
        fPidAspect = (ITmfEventAspect<Integer>) MultiAspect.<Integer>create(TmfTraceUtils.getEventAspects(trace, PidAspect.class), PidAspect.class);
    }

    @Override
    public int getVersion() {
        return 3;
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
        Integer resolve = fPidAspect.resolve(event);
        return resolve == null ? -1 : resolve.intValue();
    }

    @Override
    protected long getThreadId(@NonNull ITmfEvent event) {
        Integer resolve = fTidAspect.resolve(event);
        return resolve == null ? -1 : resolve.longValue();
    }
}
