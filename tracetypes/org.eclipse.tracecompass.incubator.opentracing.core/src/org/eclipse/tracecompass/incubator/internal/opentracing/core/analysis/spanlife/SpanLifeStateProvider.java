/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.core.analysis.spanlife;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

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

    /**
     * Quark name for open tracing spans
     */
    public static final String OPEN_TRACING_ATTRIBUTE = "openTracingSpans"; //$NON-NLS-1$

    /**
     * Quark name for ust spans
     */
    public static final String UST_ATTRIBUTE = "ustSpans"; //$NON-NLS-1$

    private final Map<String, Integer> fSpanMap;

    private final Map<String, BiConsumer<ITmfEvent, ITmfStateSystemBuilder>> fHandlers;

    /**
     * Constructor
     *
     * @param trace
     *            the trace to follow
     */
    public SpanLifeStateProvider(ITmfTrace trace) {
        super(trace, SpanLifeAnalysis.ID);
        fSpanMap = new HashMap<>();
        fHandlers = new HashMap<>();
        fHandlers.put("OpenTracingSpan", this::handleSpan); //$NON-NLS-1$
        fHandlers.put("jaeger_ust:start_span", this::handleStart); //$NON-NLS-1$
        fHandlers.put("jaeger_ust:end_span", this::handleEnd); //$NON-NLS-1$
    }

    @Override
    public int getVersion() {
        return 3;
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
        BiConsumer<ITmfEvent, ITmfStateSystemBuilder> handler = fHandlers.get(event.getType().getName());
        if (handler != null) {
            handler.accept(event, ss);
        }
    }

    private void handleSpan(ITmfEvent event, ITmfStateSystemBuilder ss) {
        long timestamp = event.getTimestamp().toNanos();
        Long duration = event.getContent().getFieldValue(Long.class, IOpenTracingConstants.DURATION);
        if (duration == null) {
            return;
        }

        String traceId = event.getContent().getFieldValue(String.class, IOpenTracingConstants.TRACE_ID);
        int traceQuark = ss.getQuarkAbsoluteAndAdd(traceId);

        int openTracingSpansQuark = ss.getQuarkRelativeAndAdd(traceQuark, OPEN_TRACING_ATTRIBUTE);

        Boolean errorTag = Boolean.parseBoolean(event.getContent().getFieldValue(String.class, IOpenTracingConstants.TAGS + "/error")); //$NON-NLS-1$
        String processName = event.getContent().getFieldValue(String.class, IOpenTracingConstants.PROCESS_NAME);

        int spanQuark;
        String name = String.valueOf(TmfTraceUtils.resolveAspectOfNameForEvent(event.getTrace(), "Name", event)); //$NON-NLS-1$
        String spanId = event.getContent().getFieldValue(String.class, IOpenTracingConstants.SPAN_ID);
        String refId = event.getContent().getFieldValue(String.class, IOpenTracingConstants.REFERENCES + "/CHILD_OF"); //$NON-NLS-1$
        if (refId == null) {
            spanQuark = ss.getQuarkRelativeAndAdd(openTracingSpansQuark, name + '/' + spanId + '/' + errorTag + '/' + processName);
        } else {
            Integer parentQuark = fSpanMap.get(refId);
            if (parentQuark == null) {
                // We don't have the parent span, just start this span at root
                parentQuark = openTracingSpansQuark;
            }
            spanQuark = ss.getQuarkRelativeAndAdd(parentQuark, name + '/' + spanId + '/' + errorTag + '/' + processName);
        }

        ss.modifyAttribute(timestamp, name, spanQuark);

        Map<Long, Map<String, String>> logs = event.getContent().getFieldValue(Map.class, IOpenTracingConstants.LOGS);
        if (logs != null) {
            // We put all the logs in the state system under the LOGS attribute
            Integer logsQuark = ss.getQuarkRelativeAndAdd(traceQuark, IOpenTracingConstants.LOGS);
            for (Map.Entry<Long, Map<String, String>> log : logs.entrySet()) {
                List<String> logString = new ArrayList<>();
                for (Map.Entry<String, String> entry : log.getValue().entrySet()) {
                    logString.add(entry.getKey() + ':' + entry.getValue());
                }
                // One attribute for each span where each state value is the logs at the
                // timestamp
                // corresponding to the start time of the state
                Integer logQuark = ss.getQuarkRelativeAndAdd(logsQuark, spanId);
                Long logTimestamp = log.getKey();
                ss.modifyAttribute(logTimestamp, String.join("~", logString), logQuark); //$NON-NLS-1$
                ss.modifyAttribute(logTimestamp + 1, (Object) null, logQuark);
            }
        }

        ss.modifyAttribute(timestamp + duration, (Object) null, spanQuark);
        if (spanId != null) {
            fSpanMap.put(spanId, spanQuark);
        }
    }

    private void handleStart(ITmfEvent event, ITmfStateSystemBuilder ss) {
        String traceId = event.getContent().getFieldValue(String.class, "trace_id_low"); //$NON-NLS-1$
        traceId = Long.toHexString(Long.decode(traceId));
        int traceQuark = ss.getQuarkAbsoluteAndAdd(traceId);

        int ustSpansQuark = ss.getQuarkRelativeAndAdd(traceQuark, UST_ATTRIBUTE);

        String spanId = event.getContent().getFieldValue(String.class, "span_id"); //$NON-NLS-1$
        spanId = Long.toHexString(Long.decode(spanId));
        int spanQuark = ss.getQuarkRelativeAndAdd(ustSpansQuark, spanId);

        long timestamp = event.getTimestamp().toNanos();
        String name = event.getContent().getFieldValue(String.class, "op_name"); //$NON-NLS-1$
        ss.modifyAttribute(timestamp, name, spanQuark);
    }

    private void handleEnd(ITmfEvent event, ITmfStateSystemBuilder ss) {
        String traceId = event.getContent().getFieldValue(String.class, "trace_id_low"); //$NON-NLS-1$
        traceId = Long.toHexString(Long.decode(traceId));
        int traceQuark = ss.getQuarkAbsoluteAndAdd(traceId);

        int ustSpansQuark = ss.getQuarkRelativeAndAdd(traceQuark, UST_ATTRIBUTE);

        String spanId = event.getContent().getFieldValue(String.class, "span_id"); //$NON-NLS-1$
        spanId = Long.toHexString(Long.decode(spanId));
        int spanQuark = ss.getQuarkRelativeAndAdd(ustSpansQuark, spanId);

        long timestamp = event.getTimestamp().toNanos();
        ss.modifyAttribute(timestamp, (Object) null, spanQuark);
    }
}
