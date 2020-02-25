/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.core.trace;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.eclipse.tracecompass.internal.jsontrace.core.job.SortingJob;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

/**
 * Sorting job particular to open tracing that has a list of processes after the
 * list of events
 *
 * @author Katherine Nadeau
 *
 */
public class OpenTracingSortingJob extends SortingJob {

    /**
     * Constructor
     *
     * @param trace
     *            the trace to be sort
     * @param path
     *            the path to the trace file
     */
    public OpenTracingSortingJob(ITmfTrace trace, String path) {
        super(trace, path, "\"startTime\":", 2); //$NON-NLS-1$
    }

    @Override
    protected void processMetadata(ITmfTrace trace, String dir) throws IOException {
        try (FileReader fileReader = new FileReader(getPath())) {
            try (JsonReader reader = new JsonReader(fileReader);) {
                Gson gson = new Gson();
                JsonObject object = gson.fromJson(reader, JsonObject.class);
                JsonElement jsonTrace = object.get("data").getAsJsonArray().get(0); //$NON-NLS-1$
                JsonObject jsonProcesses = jsonTrace.getAsJsonObject().get("processes").getAsJsonObject(); //$NON-NLS-1$
                JsonArray processes = new JsonArray();
                processes.add(jsonProcesses);

                String filePath = trace.getPath().replaceAll(".json", "Processes.json"); //$NON-NLS-1$//$NON-NLS-2$
                File processFile = new File(dir + File.separator + new File(filePath).getName());
                processFile.createNewFile();
                try (PrintWriter tempWriter = new PrintWriter(processFile)) {
                    tempWriter.println(gson.toJson(processes));
                }
            }
        } catch (IOException e) {
            // Nothing
        }
    }

}
