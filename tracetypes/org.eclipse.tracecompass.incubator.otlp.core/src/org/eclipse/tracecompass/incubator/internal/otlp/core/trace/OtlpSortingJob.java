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
import org.eclipse.tracecompass.incubator.internal.otlp.core.Activator;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

/**
 * Sorting job for OTLP traces. Flattens the nested resourceSpans/scopeSpans
 * structure into individual span JSON objects sorted by startTimeUnixNano.
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

            try (FileReader fileReader = new FileReader(fPath);
                 JsonReader reader = new JsonReader(fileReader)) {
                JsonObject root = G_SON.fromJson(reader, JsonObject.class);
                JsonArray resourceSpans = root.getAsJsonArray("resourceSpans"); //$NON-NLS-1$
                if (resourceSpans == null) {
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No resourceSpans found"); //$NON-NLS-1$
                }

                for (JsonElement rsElement : resourceSpans) {
                    JsonObject rs = rsElement.getAsJsonObject();
                    String serviceName = extractServiceName(rs);
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
                            allSpans.add(span);
                        }
                    }
                }
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
}
