/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.otlp.core.trace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.otlp.core.Activator;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

/**
 * Sorting job for OTLP traces. Flattens the nested resourceSpans/scopeSpans
 * structure into individual span JSON objects sorted by startTimeUnixNano.
 * Supports both single JSON file and JSONL (one ExportTraceServiceRequest per
 * line) formats.
 *
 * @author Matthew Khouzam
 */
public class OtlpSortingJob extends Job {

    private static final Gson G_SON = new Gson();

    private final ITmfTrace fTrace;
    private final String fPath;

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     * @param path
     *            path to the OTLP JSON file
     */
    public OtlpSortingJob(ITmfTrace trace, String path) {
        super("Sorting OTLP trace"); //$NON-NLS-1$
        fTrace = trace;
        fPath = path;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            List<JsonObject> allSpans = new ArrayList<>();

            // Try single JSON parse first
            boolean singleJsonParsed = tryParseSingleJson(allSpans);
            if (!singleJsonParsed) {
                // Fall back to JSONL line-by-line parsing
                allSpans.clear();
                tryParseJsonl(allSpans);
            }

            if (allSpans.isEmpty()) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No spans found in OTLP trace"); //$NON-NLS-1$
            }

            // Sort by startTimeUnixNano
            allSpans.sort(Comparator.comparingLong(span -> {
                JsonElement el = span.get("startTimeUnixNano"); //$NON-NLS-1$
                return el != null ? Long.parseLong(el.getAsString()) : 0L;
            }));

            // Write sorted spans as a JSON array (one object per line)
            String dir = TmfTraceManager.getSupplementaryFileDir(fTrace);
            File outFile = new File(dir + new File(fPath).getName());
            outFile.getParentFile().mkdirs();
            try (PrintWriter writer = new PrintWriter(outFile)) {
                writer.println('[');
                for (int i = 0; i < allSpans.size(); i++) {
                    writer.print(G_SON.toJson(allSpans.get(i)));
                    if (i < allSpans.size() - 1) {
                        writer.println(',');
                    } else {
                        writer.println();
                    }
                }
                writer.println(']');
            }

            return Status.OK_STATUS;
        } catch (IOException e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error sorting OTLP trace", e); //$NON-NLS-1$
        }
    }

    /**
     * Try to parse the file as a single JSON ExportTraceServiceRequest.
     *
     * @return true if parsing succeeded, false otherwise
     */
    private boolean tryParseSingleJson(List<JsonObject> allSpans) {
        try (FileReader fileReader = new FileReader(fPath);
             JsonReader reader = new JsonReader(fileReader)) {
            JsonObject root = G_SON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return false;
            }
            JsonArray resourceSpans = root.getAsJsonArray("resourceSpans"); //$NON-NLS-1$
            if (resourceSpans == null) {
                return false;
            }
            extractSpansFromResourceSpans(resourceSpans, allSpans);
            return true;
        } catch (JsonSyntaxException | IOException e) {
            return false;
        }
    }

    /**
     * Parse the file as JSONL (one ExportTraceServiceRequest per line).
     */
    private void tryParseJsonl(List<JsonObject> allSpans) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(fPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                try {
                    JsonObject root = G_SON.fromJson(line, JsonObject.class);
                    if (root == null) {
                        continue;
                    }
                    JsonArray resourceSpans = root.getAsJsonArray("resourceSpans"); //$NON-NLS-1$
                    if (resourceSpans == null) {
                        continue;
                    }
                    extractSpansFromResourceSpans(resourceSpans, allSpans);
                } catch (JsonSyntaxException e) {
                    // Skip malformed lines
                }
            }
        }
    }

    /**
     * Extract individual spans and span events from a resourceSpans array.
     * Each OTLP span becomes one entry. Each OTLP span event (log) also
     * becomes its own entry so it appears as a separate row in the events
     * table. Span events are represented as zero-duration spans at the
     * event's timestamp, inheriting the parent span's context.
     */
    private static void extractSpansFromResourceSpans(JsonArray resourceSpans, List<JsonObject> allSpans) {
        for (JsonElement rsElement : resourceSpans) {
            JsonObject rs = rsElement.getAsJsonObject();
            String serviceName = extractServiceName(rs);
            JsonArray resourceAttributes = extractResourceAttributes(rs);
            JsonArray scopeSpans = rs.getAsJsonArray("scopeSpans"); //$NON-NLS-1$
            if (scopeSpans == null) {
                continue;
            }
            for (JsonElement ssElement : scopeSpans) {
                JsonObject ss = ssElement.getAsJsonObject();
                JsonArray spans = ss.getAsJsonArray("spans"); //$NON-NLS-1$
                if (spans == null) {
                    continue;
                }
                for (JsonElement spanElement : spans) {
                    JsonObject span = spanElement.getAsJsonObject();
                    // Inject service name into the span for later use
                    span.addProperty("serviceName", serviceName); //$NON-NLS-1$
                    // Inject all resource attributes
                    if (resourceAttributes != null && resourceAttributes.size() > 0) {
                        span.add("resourceAttributes", resourceAttributes); //$NON-NLS-1$
                    }
                    allSpans.add(span);

                    // Extract span events as individual entries
                    extractSpanEvents(span, allSpans);
                }
            }
        }
    }

    /**
     * Extract OTLP span events (logs) from a span and add them as
     * individual zero-duration span entries. Each event inherits the
     * parent span's traceId, spanId, and service context. The span must
     * already have {@code serviceName} and optionally
     * {@code resourceAttributes} injected.
     *
     * @param span
     *            the span to extract events from
     * @param allSpans
     *            the list to add extracted events to
     */
    static void extractSpanEvents(JsonObject span, List<JsonObject> allSpans) {
        JsonArray events = span.getAsJsonArray("events"); //$NON-NLS-1$
        if (events == null || events.size() == 0) {
            return;
        }
        String traceId = ""; //$NON-NLS-1$
        JsonElement traceIdEl = span.get("traceId"); //$NON-NLS-1$
        if (traceIdEl != null && !traceIdEl.isJsonNull()) {
            traceId = traceIdEl.getAsString();
        }
        String spanId = ""; //$NON-NLS-1$
        JsonElement spanIdEl = span.get("spanId"); //$NON-NLS-1$
        if (spanIdEl != null && !spanIdEl.isJsonNull()) {
            spanId = spanIdEl.getAsString();
        }
        String serviceName = ""; //$NON-NLS-1$
        JsonElement svcEl = span.get("serviceName"); //$NON-NLS-1$
        if (svcEl != null && !svcEl.isJsonNull()) {
            serviceName = svcEl.getAsString();
        }
        @Nullable JsonArray resourceAttributes = span.getAsJsonArray("resourceAttributes"); //$NON-NLS-1$

        for (int i = 0; i < events.size(); i++) {
            JsonObject event = events.get(i).getAsJsonObject();
            String timeStr = "0"; //$NON-NLS-1$
            JsonElement timeEl = event.get("timeUnixNano"); //$NON-NLS-1$
            if (timeEl != null && !timeEl.isJsonNull()) {
                timeStr = timeEl.getAsString();
            }
            String eventName = ""; //$NON-NLS-1$
            JsonElement nameEl = event.get("name"); //$NON-NLS-1$
            if (nameEl != null && !nameEl.isJsonNull()) {
                eventName = nameEl.getAsString();
            }

            // Build a synthetic span entry for this event
            JsonObject synth = new JsonObject();
            synth.addProperty("traceId", traceId); //$NON-NLS-1$
            synth.addProperty("spanId", spanId); //$NON-NLS-1$
            synth.addProperty("name", eventName); //$NON-NLS-1$
            synth.addProperty("startTimeUnixNano", timeStr); //$NON-NLS-1$
            synth.addProperty("endTimeUnixNano", timeStr); //$NON-NLS-1$
            synth.addProperty("serviceName", serviceName); //$NON-NLS-1$
            // Carry the event's own attributes as span attributes
            JsonArray eventAttrs = event.getAsJsonArray("attributes"); //$NON-NLS-1$
            if (eventAttrs != null && eventAttrs.size() > 0) {
                synth.add("attributes", eventAttrs); //$NON-NLS-1$
            }
            if (resourceAttributes != null && resourceAttributes.size() > 0) {
                synth.add("resourceAttributes", resourceAttributes); //$NON-NLS-1$
            }
            allSpans.add(synth);
        }
    }

    /**
     * Extract OTLP span events (logs) from a span and add them as
     * individual zero-duration span entries. Each event inherits the
     * parent span's traceId, spanId, and service context.
     */
    private static void extractSpanEvents(JsonObject span, String serviceName,
            @Nullable JsonArray resourceAttributes, List<JsonObject> allSpans) {
        JsonArray events = span.getAsJsonArray("events"); //$NON-NLS-1$
        if (events == null || events.size() == 0) {
            return;
        }
        String traceId = ""; //$NON-NLS-1$
        JsonElement traceIdEl = span.get("traceId"); //$NON-NLS-1$
        if (traceIdEl != null && !traceIdEl.isJsonNull()) {
            traceId = traceIdEl.getAsString();
        }
        String spanId = ""; //$NON-NLS-1$
        JsonElement spanIdEl = span.get("spanId"); //$NON-NLS-1$
        if (spanIdEl != null && !spanIdEl.isJsonNull()) {
            spanId = spanIdEl.getAsString();
        }

        for (int i = 0; i < events.size(); i++) {
            JsonObject event = events.get(i).getAsJsonObject();
            String timeStr = "0"; //$NON-NLS-1$
            JsonElement timeEl = event.get("timeUnixNano"); //$NON-NLS-1$
            if (timeEl != null && !timeEl.isJsonNull()) {
                timeStr = timeEl.getAsString();
            }
            String eventName = ""; //$NON-NLS-1$
            JsonElement nameEl = event.get("name"); //$NON-NLS-1$
            if (nameEl != null && !nameEl.isJsonNull()) {
                eventName = nameEl.getAsString();
            }

            // Build a synthetic span entry for this event
            JsonObject synth = new JsonObject();
            synth.addProperty("traceId", traceId); //$NON-NLS-1$
            synth.addProperty("spanId", spanId); //$NON-NLS-1$
            synth.addProperty("name", eventName); //$NON-NLS-1$
            synth.addProperty("startTimeUnixNano", timeStr); //$NON-NLS-1$
            synth.addProperty("endTimeUnixNano", timeStr); //$NON-NLS-1$
            synth.addProperty("serviceName", serviceName); //$NON-NLS-1$
            // Carry the event's own attributes as span attributes
            JsonArray eventAttrs = event.getAsJsonArray("attributes"); //$NON-NLS-1$
            if (eventAttrs != null && eventAttrs.size() > 0) {
                synth.add("attributes", eventAttrs); //$NON-NLS-1$
            }
            if (resourceAttributes != null && resourceAttributes.size() > 0) {
                synth.add("resourceAttributes", resourceAttributes); //$NON-NLS-1$
            }
            allSpans.add(synth);
        }
    }

    private static String extractServiceName(JsonObject resourceSpan) {
        JsonObject resource = resourceSpan.getAsJsonObject("resource"); //$NON-NLS-1$
        if (resource == null) {
            return ""; //$NON-NLS-1$
        }
        JsonArray attributes = resource.getAsJsonArray("attributes"); //$NON-NLS-1$
        if (attributes == null) {
            return ""; //$NON-NLS-1$
        }
        for (JsonElement attrElement : attributes) {
            JsonObject attr = attrElement.getAsJsonObject();
            if ("service.name".equals(attr.get("key").getAsString())) { //$NON-NLS-1$ //$NON-NLS-2$
                JsonObject value = attr.getAsJsonObject("value"); //$NON-NLS-1$
                if (value != null && value.has("stringValue")) { //$NON-NLS-1$
                    return value.get("stringValue").getAsString(); //$NON-NLS-1$
                }
            }
        }
        return ""; //$NON-NLS-1$
    }

    private static @Nullable JsonArray extractResourceAttributes(JsonObject resourceSpan) {
        JsonObject resource = resourceSpan.getAsJsonObject("resource"); //$NON-NLS-1$
        if (resource == null) {
            return null;
        }
        return resource.getAsJsonArray("attributes"); //$NON-NLS-1$
    }
}
