/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.context;

import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.event.ITraceEventConstants;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.event.TraceEventPhases;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableSet;

/**
 * Context state provider, generates regions of interest
 *
 * @author Matthew Khouzam
 */
public class ContextStateProvider extends AbstractTmfStateProvider {

    private static final Set<String> ACCEPTED = ImmutableSet.of(TraceEventPhases.CONTEXT_START, TraceEventPhases.CONTEXT_END);

    /**
     * Constructor
     *
     * @param trace
     *            associated trace
     *
     */
    public ContextStateProvider(@NonNull ITmfTrace trace) {
        super(trace, ContextAnalysis.ID);
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new ContextStateProvider(getTrace());
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        ITmfStateSystemBuilder ss = getStateSystemBuilder();
        long t = event.getTimestamp().toNanos();
        String phase = event.getContent().getFieldValue(String.class, ITraceEventConstants.PHASE);
        if (ss == null || !ACCEPTED.contains(phase)) {
            return;
        }
        String catField = event.getContent().getFieldValue(String.class, ITraceEventConstants.CATEGORY);
        String nameField = event.getContent().getFieldValue(String.class, ITraceEventConstants.NAME);
        String idField = event.getContent().getFieldValue(String.class, ITraceEventConstants.ID);
        int quark = ss.getQuarkAbsoluteAndAdd(catField, nameField, idField);
        if (Objects.equals(phase, TraceEventPhases.CONTEXT_START)) {
            ss.modifyAttribute(t, 1, quark);
        }
        if (Objects.equals(phase, TraceEventPhases.CONTEXT_END)) {
            ss.modifyAttribute(t, (Object) null, quark);
        }
    }

}
