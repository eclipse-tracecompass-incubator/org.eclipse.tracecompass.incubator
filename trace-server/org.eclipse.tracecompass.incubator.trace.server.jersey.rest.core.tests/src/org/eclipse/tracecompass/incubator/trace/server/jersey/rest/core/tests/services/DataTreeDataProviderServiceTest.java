/*******************************************************************************
 * Copyright (c) 2018, 2024 Ericsson and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.EntryHeaderStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.EntryModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.EntryStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.ExperimentModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TreeOutputResponseStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test the {@link DataProviderService} with focus on the data tree endpoint
 *
 * @author Bernd Hufmann
 */
@SuppressWarnings("null")
public class DataTreeDataProviderServiceTest extends RestServerTest {
    private static final String DATA_PROVIDER_RESPONSE_FAILED_MSG = "There should be a positive response for the data provider";
    private static final String MODEL_NULL_MSG = "The model is null, maybe the analysis did not run long enough?";
    private static final int MAX_ITER = 40;
    private static final String STATISTICS_DATAPROVIDER_ID = "org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreStatisticsDataProvider:org.eclipse.linuxtools.lttng2.ust.analysis.callstack";
    private static final String REQUESTED_TIMERANGE_KEY = "requested_timerange";
    private static final String IS_FILTERED_KEY = "isFiltered";
    private static final String START = "start";
    private static final String END = "end";

    private static final List<String> STATISTICS_TREE_HEADERS = List.of("Label", "Minimum", "Maximum", "Average", "Std Dev", "Count", "Total", "Min Time Range", "Max Time Range");
    private static final List<String> SAMPLE_TOTAL_STATS_LABELS = List.of("ust", "1 ns", "5.979 s", "10.845 ms", "196.299 ms", "1948", "21.127 s", "[1450193745774189602,1450193745774189603]", "[1450193722283061910,1450193728261604656]");
    private static final List<String> SAMPLE_SELECTION_STATS_LABELS = List.of("Selection", "49.665 µs", "5.979 s", "11.388 ms", "201.201 ms", "1854", "21.113 s", "[1450193730177271075,1450193730177320740]", "[1450193722283061910,1450193728261604656]");

    /**
     * Ensure that a data tree data provider exists and returns correct data.
     * It does not test the data itself, simply that the serialized fields are
     * the expected ones.
     *
     * @throws InterruptedException
     *             Exception thrown while waiting to execute again
     */
    @Test
    public void testDataTreeDataProvider() throws InterruptedException {
        long start = 1450193697034689597L;
        long end = 1450193745774189602L;
        try {
            ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);

            // Test getting the time graph tree
            WebTarget dataTree = getDataTreeEndpoint(exp.getUUID().toString(), STATISTICS_DATAPROVIDER_ID);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put(REQUESTED_TIMERANGE_KEY, ImmutableMap.of(START, start, END, end));
            EntryModelStub model = getDataTreeEntryModel(dataTree, parameters);
            List<EntryStub> totalEntries = model.getEntries();

            List<String> sampleLabels = totalEntries.get(0).getLabels();
            for(String sample : SAMPLE_TOTAL_STATS_LABELS) {
                assertTrue(sampleLabels.contains(sample));
            }

            // Query selection time range
            end = end - 100000000L;
            parameters.put(REQUESTED_TIMERANGE_KEY, ImmutableMap.of(START, start, END, end));
            parameters.put(IS_FILTERED_KEY, true);
            model = getDataTreeEntryModel(dataTree, parameters);
            List<EntryStub> selectionRangeEntries = model.getEntries();
            assertFalse(selectionRangeEntries.isEmpty());
            // the result model contains total and selection statistics
            assertTrue(selectionRangeEntries.size() > totalEntries.size());

            sampleLabels = selectionRangeEntries.get(totalEntries.size()).getLabels();
            for(String sample : SAMPLE_SELECTION_STATS_LABELS) {
                assertTrue(sampleLabels.contains(sample));
            }

        } catch (ProcessingException e) {
            // The failure from this exception alone is not helpful. Use the
            // suppressed exception's message be the failure message for more
            // help debugging failed tests.
            fail(e.getCause().getMessage());
        }
    }

    private static EntryModelStub getDataTreeEntryModel(WebTarget dataTree, Map<String, Object> parameters) throws InterruptedException {
        TreeOutputResponseStub responseModel;
        Response treeResponse = dataTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
        assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, treeResponse.getStatus());
        responseModel = treeResponse.readEntity(TreeOutputResponseStub.class);
        assertNotNull(responseModel);
        treeResponse.close();

        // Make sure the analysis ran enough and we have a model
        int iteration = 0;
        while ((responseModel.isRunning() || responseModel.getModel() == null) && iteration < MAX_ITER) {
            Thread.sleep(100);
            treeResponse = dataTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())));
            assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, treeResponse.getStatus());
            responseModel = treeResponse.readEntity(TreeOutputResponseStub.class);
            assertNotNull(responseModel);
            iteration++;
            treeResponse.close();
        }

        EntryModelStub model = responseModel.getModel();
        assertNotNull(MODEL_NULL_MSG + responseModel, model);
        List<EntryStub> totalEntries = model.getEntries();
        assertFalse(totalEntries.isEmpty());

        List<String> headerLabels = model.getHeaders().stream().map(EntryHeaderStub::getName).collect(Collectors.toList());
        for(String header : STATISTICS_TREE_HEADERS) {
            assertTrue(headerLabels.contains(header));
        }
        return model;
    }
}
