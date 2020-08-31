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
import static org.junit.Assert.fail;

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
import org.eclipse.tracecompass.incubator.internal.kernel.core.io.IoAccessDataProvider;
import org.eclipse.tracecompass.incubator.internal.kernel.core.io.IoAnalysis;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.tests.shared.TmfTestHelper;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;

/**
 * Test the {@link IoAccessDataProvider} class
 *
 * @author Geneviève Bastien
 */
public class IoAccessDataProviderTest extends AbstractTestInputOutput {

    private static final String EXPECTED_FILE_PATH = "testfiles/dp/";

    private static final Map<String, Object> TREE_PARAMETERS = ImmutableMap.of(
            DataProviderParameterUtils.REQUESTED_TIME_KEY, ImmutableList.of(0, Long.MAX_VALUE));

    /**
     * Test the {@link IoAccessDataProvider} for the test callstack, with all
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

        IoAccessDataProvider provider = new IoAccessDataProvider(getTrace(), module);

        Map<Long, TimeGraphEntryModel> idsToEntries = assertAndGetTree(provider, "expectedIoAccessTree", ImmutableMap.of(IoAccessDataProvider.SELECTED_TID_PARAM, Collections.singleton(4)));

        assertRowsRequests(provider, idsToEntries, "expectedIoAccessRows");
    }

    /**
     * Test the {@link IoAccessDataProvider} for the test callstack, with all
     * items separately
     *
     * @throws IOException
     *             if an I/O error occurs reading from the expected value file
     *             or a malformed or unmappable byte sequence is read
     */
    @Test
    public void testIoDataProviderMultipleTids() throws IOException {
        IoAnalysis module = getModule();
        assertTrue(TmfTestHelper.executeAnalysis(module));

        IoAccessDataProvider provider = new IoAccessDataProvider(getTrace(), module);

        Map<Long, TimeGraphEntryModel> idsToEntries = assertAndGetTree(provider, "expectedIoAccessMultiTree", ImmutableMap.of(IoAccessDataProvider.SELECTED_TID_PARAM, ImmutableList.of(2, 4)));

        assertRowsRequests(provider, idsToEntries, "expectedIoAccessMultiRows");
    }

    private static void assertRowsRequests(IoAccessDataProvider provider, Map<Long, TimeGraphEntryModel> idsToEntries, String testFile) throws IOException {
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
            TimeGraphEntryModel entry = findEntryByPath(idsToEntries.values(), split[0]);
            assertNotNull("Getting entry " + split[0], entry);
            idBuilder.add(entry.getId());
            expectedData.put(entry.getId(), split[1]);
        }

        TmfModelResponse<TimeGraphModel> rowResponse = provider.fetchRowModel(prepareRowParameters(idBuilder.build(), timeBuilder.build()), null);
        assertNotNull(rowResponse);
        assertEquals(ITmfResponse.Status.COMPLETED, rowResponse.getStatus());
        TimeGraphModel model = rowResponse.getModel();
        assertNotNull(model);
        List<ITimeGraphRowModel> rows = model.getRows();

        for (ITimeGraphRowModel row : rows) {
            TimeGraphEntryModel entry = idsToEntries.get(row.getEntryID());
            assertNotNull("Unexpected entry", entry);
            String data = expectedData.get(row.getEntryID());
            assertNotNull("Data for row: " + entry.getName(), data);

            assertEqualsStates(data, row.getStates(), entry.getName());
        }
        assertEquals(expectedStrings.size() - 1, rows.size());
    }

    private static void assertEqualsStates(String string, List<ITimeGraphState> states, String element) {
        String[] stringStates = string.split(",");
        for (int i = 0; i < stringStates.length / 4; i++) {
            ITimeGraphState state = states.get(i);
            assertEquals(element + ": start time at position " + i, Long.parseLong(stringStates[i * 4]), state.getStartTime());
            assertEquals(element + ": end time at position " + i, Long.parseLong(stringStates[i * 4 + 1]), state.getStartTime() + state.getDuration() - 1);
            OutputElementStyle style = state.getStyle();
            if (style == null) {
                // Expected a value of Long
                try {
                    assertEquals(element + ": null style at position " + i, stringStates[i * 4 + 2], "null");
                } catch (NumberFormatException e) {
                    fail(element + ": value at position " + i + ": did not expect a null style");
                }
            } else {
                assertEquals(element + ": value at position " + i, stringStates[i * 4 + 2], style.getParentKey());
            }
            assertEquals(element + ": label at position " + i, stringStates[i * 4 + 3], String.valueOf(state.getLabel()));
        }
    }

    private static Map<String, Object> prepareRowParameters(Set<Long> ids, List<Long> requestedTimes) {
        Builder<String, Object> builder = ImmutableMap.builder();
        builder.put(DataProviderParameterUtils.REQUESTED_TIME_KEY, requestedTimes);
        builder.put(DataProviderParameterUtils.REQUESTED_ITEMS_KEY, ids);
        return builder.build();
    }

    private static Map<Long, TimeGraphEntryModel> assertAndGetTree(IoAccessDataProvider provider, String filePath, Map<String, Object> additionalParameters) throws IOException {
        Map<@NonNull String, @NonNull Object> parameters = new HashMap<>(TREE_PARAMETERS);
        parameters.putAll(additionalParameters);
        TmfModelResponse<TmfTreeModel<@NonNull TimeGraphEntryModel>> treeResponse = provider.fetchTree(parameters, null);
        assertNotNull(treeResponse);
        assertEquals(ITmfResponse.Status.COMPLETED, treeResponse.getStatus());
        TmfTreeModel<@NonNull TimeGraphEntryModel> treeModel = treeResponse.getModel();
        assertNotNull(treeModel);
        List<@NonNull TimeGraphEntryModel> treeEntries = treeModel.getEntries();

        List<String> expectedStrings = Files.readAllLines(Paths.get(EXPECTED_FILE_PATH + filePath));
        // Make sure all entries that should exists are there
        for (int i = 0; i < expectedStrings.size(); i++) {
            String expectedString = expectedStrings.get(i);

            TimeGraphEntryModel entry = findEntryByPath(treeEntries, expectedString);
            assertNotNull("Entry not found: " + expectedString, entry);
        }
        // Also make sure there are no extra entries
        assertEquals("Number of tree entries", expectedStrings.size(), treeEntries.size());
        Map<Long, TimeGraphEntryModel> map = new HashMap<>();
        for (TimeGraphEntryModel fgModel : treeEntries) {
            map.put(fgModel.getId(), fgModel);
        }
        return map;
    }

    private static @Nullable TimeGraphEntryModel findEntryByPath(Collection<TimeGraphEntryModel> list, String expectedString) {
        String[] entryPath = expectedString.split(",");
        long parentId = -1;
        TimeGraphEntryModel entry = null;
        for (String path : entryPath) {
            entry = findEntryByNameWithParent(list, Objects.requireNonNull(path), parentId);
            if (entry == null) {
                return null;
            }
            parentId = entry.getId();
        }
        return entry;
    }

    private static @Nullable TimeGraphEntryModel findEntryByNameWithParent(Collection<TimeGraphEntryModel> list, String path, long parentId) {
        for (TimeGraphEntryModel entry : list) {
            if (entry.getParentId() == parentId && entry.getName().equals(path)) {
                return entry;
            }
        }
        return null;
    }

}
