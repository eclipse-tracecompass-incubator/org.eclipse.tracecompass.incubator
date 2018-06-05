/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.core.analysis.spanlife;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.event.IOpenTracingConstants;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Span life state provider
 *
 * @author Katherine Nadeau
 *
 */
public class SpanLifeStateProvider extends AbstractTmfStateProvider {

    private final Map<String, Integer> spanMap;

    /**
     * Constructor
     *
     * @param trace
     *            the trace to follow
     */
    public SpanLifeStateProvider(@NonNull ITmfTrace trace) {
        super(trace, SpanLifeAnalysis.ID);
        spanMap = new HashMap<>();
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new SpanLifeStateProvider(getTrace());
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        ITmfStateSystemBuilder ss = getStateSystemBuilder();
        if (ss == null) {
            return;
        }

        long timestamp = event.getTimestamp().toNanos();
        Long duration = event.getContent().getFieldValue(Long.class, IOpenTracingConstants.DURATION);
        if (duration == null) {
            return;
        }

        int quark;
        String name = String.valueOf(TmfTraceUtils.resolveAspectOfNameForEvent(event.getTrace(), "Name", event)); //$NON-NLS-1$
        String spanId = event.getContent().getFieldValue(String.class, IOpenTracingConstants.SPAN_ID);
        String refId = event.getContent().getFieldValue(String.class, IOpenTracingConstants.REFERENCES + "/CHILD_OF"); //$NON-NLS-1$
        if (refId == null) {
            quark = ss.getQuarkAbsoluteAndAdd(name + '/' + spanId);
        } else {
            Integer parentQuark = spanMap.get(refId);
            if (parentQuark == null) {
                return;
            }
            quark = ss.getQuarkRelativeAndAdd(parentQuark, name + '/' + spanId);
        }
        ss.modifyAttribute(timestamp, name, quark);
        ss.modifyAttribute(timestamp + duration, (Object) null, quark);
        spanMap.put(event.getContent().getFieldValue(String.class, IOpenTracingConstants.SPAN_ID), quark);
    }

}
