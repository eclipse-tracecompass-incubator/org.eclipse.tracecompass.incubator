/*******************************************************************************
 * Copyright (c) 2018, 2021 Ericsson
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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.BiPredicate;

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

    private static final String DATA_KEY = "data"; //$NON-NLS-1$
    private static final String TRACE_ID = "traceID"; //$NON-NLS-1$
    private static final String TOTAL_KEY = "total"; //$NON-NLS-1$
    private static final String LIMIT_KEY = "limit"; //$NON-NLS-1$
    private static final String OFFSET_KEY = "offset"; //$NON-NLS-1$
    private static final String ERRORS_KEY = "errors"; //$NON-NLS-1$

    /**
     * Takes a JsonObject of multiple jaeger traces and import the selected
     * traces it contains
     *
     * @param jsonObject
     *            Complete json object of the traces file
     * @param selectedTracesId
     *            List of trace id's to import or null to import all
     * @param newFolderPath
     *            Absolute path indicating where the imported traces will be
     *            stored
     * @param predicate
     *            Function that should be executed for each jaeger trace after
     *            they have been downloaded
     * @return true if the operation has not found any issue, false if an
     *         exception arisen or the predicate failed
     */
    public static boolean splitAndImport(JsonObject jsonObject, List<String> selectedTracesId, String newFolderPath, BiPredicate<String, String> predicate) {

        Gson gson = new Gson();
        JsonArray array = jsonObject.get(DATA_KEY).getAsJsonArray();
        JsonElement total = jsonObject.get(TOTAL_KEY);
        JsonElement limit = jsonObject.get(LIMIT_KEY);
        JsonElement offset = jsonObject.get(OFFSET_KEY);
        JsonElement errors = jsonObject.get(ERRORS_KEY);
        boolean splitAndImportWithoutIssues = true;

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

                    /*
                     * Handle the file that has just been downloaded. Usually it
                     * is an import action.
                     */
                    splitAndImportWithoutIssues = predicate.test(newFolderPath, fileName) || splitAndImportWithoutIssues;

                } catch (IOException e) {
                    splitAndImportWithoutIssues = false;
                }
            }
        }
        return splitAndImportWithoutIssues;
    }

}
