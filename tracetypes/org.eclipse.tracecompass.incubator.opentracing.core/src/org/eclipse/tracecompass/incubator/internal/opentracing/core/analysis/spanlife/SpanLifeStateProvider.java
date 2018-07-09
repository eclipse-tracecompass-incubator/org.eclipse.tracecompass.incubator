/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.core.analysis.spanlife;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    public SpanLifeStateProvider(ITmfTrace trace) {
        super(trace, SpanLifeAnalysis.ID);
        spanMap = new HashMap<>();
    }

    @Override
    public int getVersion() {
        return 2;
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

        Map<Long, Map<String, String>> logs = event.getContent().getFieldValue(Map.class, IOpenTracingConstants.LOGS);
        if (logs != null) {
            // We put all the logs in the state system under the LOGS attribute
            Integer logsQuark = ss.getQuarkAbsoluteAndAdd(IOpenTracingConstants.LOGS);
            for (Map.Entry<Long, Map<String, String>> log : logs.entrySet()) {
                List<String> logString = new ArrayList<>();
                for (Map.Entry<String, String> entry : log.getValue().entrySet()) {
                    logString.add(entry.getKey() + ':' + entry.getValue());
                }
                // One attribute for each span where each state value is the logs at the timestamp
                // corresponding to the start time of the state
                Integer logQuark = ss.getQuarkRelativeAndAdd(logsQuark, spanId);
                Long logTimestamp = log.getKey();
                ss.modifyAttribute(logTimestamp, String.join("~", logString), logQuark); //$NON-NLS-1$
                ss.modifyAttribute(logTimestamp + 1, (Object) null, logQuark);
            }
        }

        ss.modifyAttribute(timestamp + duration, (Object) null, quark);
        if (spanId != null) {
            spanMap.put(spanId, quark);
        }
    }

}
