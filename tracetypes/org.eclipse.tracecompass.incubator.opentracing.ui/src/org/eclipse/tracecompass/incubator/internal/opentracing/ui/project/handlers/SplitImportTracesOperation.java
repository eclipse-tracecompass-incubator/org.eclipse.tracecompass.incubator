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

package org.eclipse.tracecompass.incubator.internal.opentracing.ui.project.handlers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.tracecompass.tmf.core.project.model.TmfTraceType;
import org.eclipse.tracecompass.tmf.core.project.model.TraceTypeHelper;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceTypeUIUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Operator to split jaeger traces into multiple files and import them
 *
 * @author Katherine Nadeau
 *
 */
public class SplitImportTracesOperation {

    private static final String TRACE_TYPE_ID = "org.eclipse.tracecompass.incubator.opentracing.core"; //$NON-NLS-1$

    private static final String DATA_KEY = "data"; //$NON-NLS-1$
    private static final String TRACE_ID = "traceID"; //$NON-NLS-1$
    private static final String TOTAL_KEY = "total"; //$NON-NLS-1$
    private static final String LIMIT_KEY = "limit"; //$NON-NLS-1$
    private static final String OFFSET_KEY = "offset"; //$NON-NLS-1$
    private static final String ERRORS_KEY = "errors"; //$NON-NLS-1$

    /**
     * Takes a JsonObject of multiple jaeger traces and import the selected traces
     * it contains
     *
     * @param jsonObject
     *            Complete json object of the traces file
     * @param selectedTracesId
     *            List of trace id's to import or null to import all
     * @param newTracesDestination
     *            Path of the new traces folder relative to the destination folder
     * @param tracesDestinationFolder
     *            "Traces" folder of the trace to split
     */
    public static void splitAndImport(JsonObject jsonObject, List<String> selectedTracesId, String newTracesDestination, TmfTraceFolder tracesDestinationFolder) {
        TraceTypeHelper traceTypeHelper = TmfTraceType.getTraceType(TRACE_TYPE_ID);

        Gson gson = new Gson();
        JsonArray array = jsonObject.get(DATA_KEY).getAsJsonArray();
        JsonElement total = jsonObject.get(TOTAL_KEY);
        JsonElement limit = jsonObject.get(LIMIT_KEY);
        JsonElement offset = jsonObject.get(OFFSET_KEY);
        JsonElement errors = jsonObject.get(ERRORS_KEY);

        String newFolderPath = tracesDestinationFolder.getLocation().getPath() + newTracesDestination;
        new File(newFolderPath).mkdir();

        // We create a new file for every selected trace in the traces file
        for (int i = 0; i < array.size(); i++) {
            String traceId = array.get(i).getAsJsonObject().get(TRACE_ID).getAsString();

            if (selectedTracesId == null || selectedTracesId.contains(traceId)) {
                String fileName = traceId + ".json"; //$NON-NLS-1$
                File subTrace = new File(newFolderPath + '/' + fileName);

                try {
                    subTrace.createNewFile();

                    JsonObject newObject = new JsonObject();
                    JsonArray data = new JsonArray();
                    data.add(array.get(i));
                    newObject.add(DATA_KEY, data);
                    newObject.add(TOTAL_KEY, total);
                    newObject.add(LIMIT_KEY, limit);
                    newObject.add(OFFSET_KEY, offset);
                    newObject.add(ERRORS_KEY, errors);
                    try (PrintWriter tempWriter = new PrintWriter(subTrace)) {
                        tempWriter.print(gson.toJson(newObject));
                    }

                    IFolder resource = tracesDestinationFolder.getResource();
                    resource.refreshLocal(IResource.DEPTH_INFINITE, null);

                    IFile file = resource.getFile(newTracesDestination + '/' + fileName);

                    TmfTraceTypeUIUtils.setTraceType(file, traceTypeHelper);
                } catch (IOException | CoreException e) {
                }
            }
        }
    }

}
