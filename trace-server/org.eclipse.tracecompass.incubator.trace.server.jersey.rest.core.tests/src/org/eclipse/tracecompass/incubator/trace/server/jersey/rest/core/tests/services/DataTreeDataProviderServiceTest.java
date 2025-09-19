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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.ProcessingException;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.NewRestServerTest;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.DataTreeApi;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.DataTreeEntry;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.DataTreeEntryModel;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.DataTreeResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Experiment;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeRange;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TreeColumnHeader;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TreeParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TreeQueryParameters;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Test the {@link DataProviderService} with focus on the data tree endpoint
 *
 * @author Bernd Hufmann
 */
public class DataTreeDataProviderServiceTest extends NewRestServerTest {
    private static final String DATA_PROVIDER_RESPONSE_FAILED_MSG = "There should be a positive response for the data provider";
    private static final String MODEL_NULL_MSG = "The model is null, maybe the analysis did not run long enough?";
    private static final int MAX_ITER = 40;
    private static final String STATISTICS_DATAPROVIDER_ID = "org.eclipse.tracecompass.analysis.timing.core.segmentstore.SegmentStoreStatisticsDataProvider:org.eclipse.linuxtools.lttng2.ust.analysis.callstack";
    private static final String IS_FILTERED_KEY = "isFiltered";

    private static final List<String> STATISTICS_TREE_HEADERS = List.of("Label", "Minimum", "Maximum", "Average", "Std Dev", "Count", "Total", "Min Time Range", "Max Time Range");
    private static final List<String> SAMPLE_TOTAL_STATS_LABELS = List.of("ust", "1 ns", "5.979 s", "10.845 ms", "196.299 ms", "1948", "21.127 s", "[1450193745774189602,1450193745774189603]", "[1450193722283061910,1450193728261604656]");
    private static final List<String> SAMPLE_SELECTION_STATS_LABELS = List.of("Selection", "49.665 µs", "5.979 s", "11.388 ms", "201.201 ms", "1854", "21.113 s", "[1450193730177271075,1450193730177320740]", "[1450193722283061910,1450193728261604656]");

    /**
     * Data provider API
     */
    protected static DataTreeApi sfDataTreeApi = new DataTreeApi(sfApiClient);

    /**
     * Ensure that a data tree data provider exists and returns correct data.
     * It does not test the data itself, simply that the serialized fields are
     * the expected ones.
     *
     * @throws InterruptedException
     *             Exception thrown while waiting to execute again
     * @throws ApiException
     *             if such exception occurs
     */
    @Test
    public void testDataTreeDataProvider() throws InterruptedException, ApiException {
        long start = 1450193697034689597L;
        long end = 1450193745774189602L;
        try {
            Experiment exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);

            // Test getting the time graph tree
            List<DataTreeEntry> totalEntries = getDataTreeEntryModel(exp.getUUID(), STATISTICS_DATAPROVIDER_ID, start, end, false);

            List<String> sampleLabels = totalEntries.get(0).getLabels();
            for(String sample : SAMPLE_TOTAL_STATS_LABELS) {
                assertTrue(sampleLabels.contains(sample));
            }

            // Query selection time range (total and appended selection stats)
            end = end - 100000000L;
            List<DataTreeEntry> selectionRangeEntries = getDataTreeEntryModel(exp.getUUID(), STATISTICS_DATAPROVIDER_ID, start, end, true);
            assertFalse(selectionRangeEntries.isEmpty());
            // the result model contains total and selection statistics
            assertTrue(selectionRangeEntries.size() > totalEntries.size());

            sampleLabels = selectionRangeEntries.get(totalEntries.size()).getLabels();
            assertEquals(SAMPLE_SELECTION_STATS_LABELS, sampleLabels);
            for (String sample : SAMPLE_SELECTION_STATS_LABELS) {
                assertTrue(sampleLabels.contains(sample));
            }
        } catch (ProcessingException e) {
            // The failure from this exception alone is not helpful. Use the
            // suppressed exception's message be the failure message for more
            // help debugging failed tests.
            fail(e.getCause().getMessage());
        }
    }

    private static List<DataTreeEntry> getDataTreeEntryModel(UUID uuid, String id, long start, long end, boolean isFiltered) throws InterruptedException, ApiException {
        TreeParameters params = new TreeParameters();
        if (isFiltered) {
            params = new FilteredTreeParameter();
            ((FilteredTreeParameter) params).isFiltered(isFiltered);
        }
        params.requestedTimerange(new TimeRange().start(start).end(end));
        TreeQueryParameters queryParams = new TreeQueryParameters().parameters(params);

        DataTreeResponse treeResponse = sfDataTreeApi.getDataTree(uuid, id, queryParams);
        assertTrue(DATA_PROVIDER_RESPONSE_FAILED_MSG, !treeResponse.getStatus().equals(DataTreeResponse.StatusEnum.FAILED));
        DataTreeEntryModel responseModel = treeResponse.getModel();

        // Make sure the analysis ran enough and we have a model
        int iteration = 0;
        while ((treeResponse.getStatus().equals(DataTreeResponse.StatusEnum.RUNNING)) || (responseModel == null) && (iteration < MAX_ITER)) {
            Thread.sleep(100);
            treeResponse = sfDataTreeApi.getDataTree(uuid, id, queryParams);
            assertTrue(DATA_PROVIDER_RESPONSE_FAILED_MSG, !treeResponse.getStatus().equals(DataTreeResponse.StatusEnum.FAILED));
            responseModel = treeResponse.getModel();
            assertNotNull(responseModel);
            iteration++;
        }

        assertNotNull(responseModel);
        List<DataTreeEntry> model = responseModel.getEntries();
        assertNotNull(MODEL_NULL_MSG + responseModel, model);

        List<TreeColumnHeader> headers = responseModel.getHeaders();
        assertNotNull(headers);

        List<String> headerLabels = headers.stream().map(TreeColumnHeader::getName).collect(Collectors.toList());
        for(String header : STATISTICS_TREE_HEADERS) {
            assertTrue(headerLabels.contains(header));
        }
        return model;
    }

    // FIXME: isFiltered should be part of the TSP
    private static class FilteredTreeParameter extends TreeParameters {
        private boolean isFiltered;
        public FilteredTreeParameter isFiltered(boolean flag) {
            this.isFiltered = flag;
            return this;
          }
        /**
         * Get isFiltered flag
         * @return isFiltered
         */
        @JsonProperty(IS_FILTERED_KEY)
        @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
        public boolean isFiltered() {
          return isFiltered;
        }

        @JsonProperty(IS_FILTERED_KEY)
        @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
        public void setFiltered(boolean isFiltered) {
          this.isFiltered = isFiltered;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            FilteredTreeParameter treeParameters = (FilteredTreeParameter) o;
            return super.equals(o) && isFiltered == treeParameters.isFiltered;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + Boolean.hashCode(isFiltered);
            return result;
        }
    }
}
