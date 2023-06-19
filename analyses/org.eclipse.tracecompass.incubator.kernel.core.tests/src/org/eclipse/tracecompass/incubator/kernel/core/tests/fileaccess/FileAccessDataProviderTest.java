/*******************************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.kernel.core.tests.fileaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.tracecompass.incubator.internal.kernel.core.fileaccess.FileAccessDataProvider;
import org.eclipse.tracecompass.incubator.internal.kernel.core.io.IoAnalysis;
import org.eclipse.tracecompass.incubator.kernel.core.io.tests.AbstractTestInputOutput;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.tests.shared.TmfTestHelper;
import org.junit.Test;

/**
 * Test the {@link FileAccessDataProvider} class
 *
 * @author Arnaud Fiorini
 */
public class FileAccessDataProviderTest extends AbstractTestInputOutput {

    private static final String EXPECTED_FILE_PATH = "testfiles/dp/";

    /**
     * Test the {@link FileAccessDataProvider} for the test callstack, with all
     * items separately
     */
    @Test
    public void testFileAccessDataProvider() {
        IoAnalysis module = getModule();
        assertTrue(TmfTestHelper.executeAnalysis(module));

        FileAccessDataProvider provider = new FileAccessDataProvider(getTrace(), module);
        assertNotNull(provider);

        Map<Long, TmfTreeDataModel> entryIdToModel = assertAndGetTree(provider, "expectedFileAccessTree");
        assertRows(provider, entryIdToModel, "expectedFileAccessRows");
    }

    @Test
    public void testIncorrectParameters() {
        //TODO: test incorrect parameters
    }


    private static void assertRows(FileAccessDataProvider provider, Map<Long, TmfTreeDataModel> entryIdToModel, String filePath) {
        // Read the lines of the test file
        List<String> expectedStrings = Collections.emptyList();
        try {
            expectedStrings = Files.readAllLines(Paths.get(EXPECTED_FILE_PATH + filePath));
        } catch (IOException e) {
            fail("The file which stores the expected result was not found.");
        }
        // First line contains the data to test: start, end, increment
        assertTrue("test file has proper data", expectedStrings.size() > 2);
        String firstLine = expectedStrings.get(0);
        String[] testParameters = firstLine.split(",");
        assertTrue(testParameters.length == 3);
        long start = Long.parseLong(testParameters[0]);
        long end = Long.parseLong(testParameters[1]);
        long increment = Long.parseLong(testParameters[2]);

        Collection<Long> entriesToQuery = entryIdToModel.entrySet().stream().filter(entry -> entry.getValue().hasRowModel())
                .map(Entry::getKey).collect(Collectors.toList());
        TmfModelResponse<TimeGraphModel> rowModelResponse = provider.fetchRowModel(FetchParametersUtils.selectionTimeQueryToMap(
                new SelectionTimeQueryFilter(start, end, (int) ((end - start) / increment) + 1, entriesToQuery)), new NullProgressMonitor());

        assertNotNull(rowModelResponse);
        assertEquals(ITmfResponse.Status.COMPLETED, rowModelResponse.getStatus());
        TimeGraphModel rowModel = rowModelResponse.getModel();
        assertNotNull(rowModel);
        //TODO: compare row models with expected
    }

    @SuppressWarnings("null")
    private static Map<Long, TmfTreeDataModel> assertAndGetTree(FileAccessDataProvider provider, String filePath) {
        TmfModelResponse<TmfTreeModel<TimeGraphEntryModel>> treeResponse = provider.fetchTree(
                FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0, Long.MAX_VALUE, 2)), new NullProgressMonitor());

        assertNotNull(treeResponse);
        assertEquals(ITmfResponse.Status.COMPLETED, treeResponse.getStatus());
        TmfTreeModel<TimeGraphEntryModel> treeModel = treeResponse.getModel();
        assertNotNull(treeModel);
        List<TimeGraphEntryModel> treeEntries = treeModel.getEntries();

        List<String> expectedStrings = new ArrayList<>();
        try {
            expectedStrings = Files.readAllLines(Paths.get(EXPECTED_FILE_PATH + filePath));
        } catch (IOException e) {
            fail("The file which stores the expected result was not found.");
        }
        Map<Long, TmfTreeDataModel> map = new HashMap<>();
        for (TmfTreeDataModel fgModel : treeEntries) {
            map.put(fgModel.getId(), fgModel);
        }
        // Make sure all entries that should exists are there
        Map<Long, Long> receivedToExpectedIds = new HashMap<>();
        for (int i = 0; i < expectedStrings.size(); i++) {
            String expectedString = expectedStrings.get(i);
            String[] split = expectedString.split(",");
            Long expectedId = Long.parseLong(split[0]);
            Long expectedParentId = Long.parseLong(split[2]);
            String expectedName = split[1];
            boolean isPresent = false;
            for (Entry<Long, TmfTreeDataModel> entry : map.entrySet()) {
                if (entry.getValue().getName().equals(expectedName)) {
                    if (expectedParentId == -1) {
                        assertEquals("Parent of entry", expectedParentId, Long.valueOf(entry.getValue().getParentId()));
                    } else if (!receivedToExpectedIds.containsKey(entry.getValue().getParentId()) || receivedToExpectedIds.get(entry.getValue().getParentId()) != expectedParentId) {
                        continue;
                    }
                    receivedToExpectedIds.put(entry.getKey(), expectedId);
                    isPresent = true;
                    break;
                }
            }
            assertTrue("Entry is not present: " + expectedName + ", id = " + expectedId, isPresent);
        }
        // Make sure there are no extra entries
        assertEquals("Number of tree entries", expectedStrings.size(), receivedToExpectedIds.size());
        return map;
    }
}
