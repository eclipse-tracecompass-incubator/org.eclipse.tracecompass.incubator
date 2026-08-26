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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.otlp.core.Activator;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Sorting job for OTLP protobuf binary traces. Parses the protobuf file using
 * {@link OtlpProtobufParser}, sorts spans by startTimeUnixNano, and writes the
 * same sorted JSON format as {@link OtlpSortingJob}.
 *
 * @author Matthew Khouzam
 */
public class OtlpProtobufSortingJob extends Job {

    private static final Gson G_SON = new Gson();

    private final ITmfTrace fTrace;
    private final String fPath;

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     * @param path
     *            path to the OTLP protobuf file
     */
    public OtlpProtobufSortingJob(ITmfTrace trace, String path) {
        super("Sorting OTLP protobuf trace"); //$NON-NLS-1$
        fTrace = trace;
        fPath = path;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            List<@NonNull JsonObject> allSpans = OtlpProtobufParser.parse(fPath);

            if (allSpans.isEmpty()) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No spans found in protobuf file"); //$NON-NLS-1$
            }

            // Extract span events as individual entries
            List<@NonNull JsonObject> allEntries = new java.util.ArrayList<>(allSpans);
            for (JsonObject span : allSpans) {
                OtlpSortingJob.extractSpanEvents(span, allEntries);
            }

            // Sort by startTimeUnixNano
            allEntries.sort(Comparator.comparingLong(span -> {
                JsonElement el = span.get("startTimeUnixNano"); //$NON-NLS-1$
                return el != null && !el.isJsonNull() ? parseLongSafe(el.getAsString()) : 0L;
            }));

            // Write sorted spans as a JSON array (one object per line)
            String dir = TmfTraceManager.getSupplementaryFileDir(fTrace);
            File outFile = new File(dir + new File(fPath).getName());
            outFile.getParentFile().mkdirs();
            try (PrintWriter writer = new PrintWriter(outFile)) {
                writer.println('[');
                for (int i = 0; i < allEntries.size(); i++) {
                    writer.print(G_SON.toJson(allEntries.get(i)));
                    if (i < allEntries.size() - 1) {
                        writer.println(',');
                    } else {
                        writer.println();
                    }
                }
                writer.println(']');
                if (writer.checkError()) {
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error writing sorted OTLP protobuf trace"); //$NON-NLS-1$
                }
            }

            return Status.OK_STATUS;
        } catch (IOException e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error parsing OTLP protobuf trace", e); //$NON-NLS-1$
        }
    }

    private static long parseLongSafe(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
