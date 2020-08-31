/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.kernel.core.io.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.kernel.core.io.IoAnalysis;
import org.eclipse.tracecompass.incubator.internal.kernel.core.io.IoPerProcessDataProvider;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.tests.shared.TmfTestHelper;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Test the {@link IoPerProcessDataProvider} class
 *
 * @author Geneviève Bastien
 */
public class IoPerProcessDataProviderTest extends AbstractTestInputOutput {

    private static final String EXPECTED_FILE_PATH = "testfiles/dp/";

    private static final Map<String, Object> TREE_PARAMETERS = ImmutableMap.of(
            DataProviderParameterUtils.REQUESTED_TIME_KEY, ImmutableList.of(0, Long.MAX_VALUE));

    /**
     * Test the {@link IoPerProcessDataProvider} for the test callstack, with all
     * items separately
     *
     * @throws IOException
     *             if an I/O error occurs reading from the expected value file
     *             or a malformed or unmappable byte sequence is read
     */
    @Test
    public void testIoDataProvider() throws IOException {
        IoAnalysis module = getModule();
        assertTrue(TmfTestHelper.executeAnalysis(module));

        IoPerProcessDataProvider provider = new IoPerProcessDataProvider(getTrace(), module);

        Map<Long, TmfTreeDataModel> idsToEntries = assertAndGetTree(provider, "expectedIoTree", Collections.emptyMap());

        assertRowsRequests(provider, idsToEntries, "expectedDataTid2read");
        assertRowsRequests(provider, idsToEntries, "expectedDataTid2write");
        assertRowsRequests(provider, idsToEntries, "expectedDataTid2rw");
        assertRowsRequests(provider, idsToEntries, "expectedDataTid2rw2");
    }

    private static void assertRowsRequests(IoPerProcessDataProvider provider, Map<Long, TmfTreeDataModel> idsToEntries, String testFile) throws IOException {
        // Read the lines of the test file
        List<String> expectedStrings = Files.readAllLines(Paths.get(EXPECTED_FILE_PATH + testFile));
        // First line contains the data to test: start, end, increment
        assertTrue("test file has proper data", expectedStrings.size() > 2);
        String firstLine = expectedStrings.get(0);
        String[] testParameters = firstLine.split(",");
        assertTrue(testParameters.length == 3);
        long start = Long.parseLong(testParameters[0]);
        long end = Long.parseLong(testParameters[1]);
        long increment = Long.parseLong(testParameters[2]);

        ImmutableList.Builder<Long> timeBuilder = ImmutableList.builder();
        for (long i = start; i <= end; i = i + increment) {
            timeBuilder.add(i);
        }

        ImmutableSet.Builder<Long> idBuilder = ImmutableSet.builder();
        Map<Long, String> expectedData = new HashMap<>();
        for (int i = 1; i < expectedStrings.size(); i++) {
            String testString = expectedStrings.get(i);
            String[] split = testString.split(":");
            assertEquals("test data at line " + i, 2, split.length);
            TmfTreeDataModel entry = findEntryByPath(idsToEntries.values(), split[0]);
            assertNotNull("Getting entry " + split[0], entry);
            idBuilder.add(entry.getId());
            expectedData.put(entry.getId(), split[1]);
        }

        TmfModelResponse<ITmfXyModel> rowResponse = provider.fetchXY(prepareRowParameters(idBuilder.build(), timeBuilder.build()), null);
        assertNotNull(rowResponse);
        assertEquals(ITmfResponse.Status.COMPLETED, rowResponse.getStatus());
        ITmfXyModel model = rowResponse.getModel();
        assertNotNull(model);
        Collection<ISeriesModel> seriesData = model.getSeriesData();

        for (ISeriesModel series : seriesData) {
            String expectedValueStr = Objects.requireNonNull(expectedData.get(series.getId()));
            String[] expectedValues = expectedValueStr.split(",");
            int nbPoints = expectedValues.length / 2;
            long[] actualXValues = series.getXAxis();
            double[] actualYValues = series.getData();
            for (int i = 0; i < nbPoints; i++) {
                assertEquals("Expected X value at position " + i + " for series model " + series.getName(), Long.parseLong(expectedValues[i * 2]), actualXValues[i]);
                assertEquals("Expected Y value at time " + actualXValues[i] + " for series model " + series.getName(), Double.parseDouble(expectedValues[(i * 2) + 1]), actualYValues[i], 0.005);
            }
        }
    }

    private static Map<String, Object> prepareRowParameters(Set<Long> ids, List<Long> requestedTimes) {
      return ImmutableMap.of(DataProviderParameterUtils.REQUESTED_TIME_KEY, requestedTimes, DataProviderParameterUtils.REQUESTED_ITEMS_KEY, ids);
  }

    private static Map<Long, TmfTreeDataModel> assertAndGetTree(IoPerProcessDataProvider provider, String filePath, Map<String, Object> additionalParameters) throws IOException {
        Map<@NonNull String, @NonNull Object> parameters = new HashMap<>(TREE_PARAMETERS);
        parameters.putAll(additionalParameters);
        TmfModelResponse<TmfTreeModel<@NonNull TmfTreeDataModel>> treeResponse = provider.fetchTree(parameters, null);
        assertNotNull(treeResponse);
        assertEquals(ITmfResponse.Status.COMPLETED, treeResponse.getStatus());
        TmfTreeModel<@NonNull TmfTreeDataModel> treeModel = treeResponse.getModel();
        assertNotNull(treeModel);
        List<@NonNull TmfTreeDataModel> treeEntries = treeModel.getEntries();

        List<String> expectedStrings = Files.readAllLines(Paths.get(EXPECTED_FILE_PATH + filePath));
        // Make sure all entries that should exists are there
        for (int i = 0; i < expectedStrings.size(); i++) {
            String expectedString = expectedStrings.get(i);

            TmfTreeDataModel entry = findEntryByPath(treeEntries, expectedString);
            assertNotNull("Entry not found: " + expectedString, entry);
        }
        // Also make sure there are no extra entries
        assertEquals("Number of tree entries", expectedStrings.size(), treeEntries.size());
        Map<Long, TmfTreeDataModel> map = new HashMap<>();
        for (TmfTreeDataModel fgModel : treeEntries) {
            map.put(fgModel.getId(), fgModel);
        }
        return map;
    }

    private static @Nullable TmfTreeDataModel findEntryByPath(Collection<TmfTreeDataModel> list, String expectedString) {
        String[] entryPath = expectedString.split(",");
        long parentId = -1;
        TmfTreeDataModel entry = null;
        for (String path : entryPath) {
            entry = findEntryByNameWithParent(list, Objects.requireNonNull(path), parentId);
            if (entry == null) {
                return null;
            }
            parentId = entry.getId();
        }
        return entry;
    }

    private static @Nullable TmfTreeDataModel findEntryByNameWithParent(Collection<TmfTreeDataModel> list, String path, long parentId) {
        for (TmfTreeDataModel entry : list) {
            if (entry.getParentId() == parentId && entry.getName().equals(path)) {
                return entry;
            }
        }
        return null;
    }

}
