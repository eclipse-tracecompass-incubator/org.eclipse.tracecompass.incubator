/*******************************************************************************
 * Copyright (c) 2025 Ericsson and others
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.NewRestServerTest;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.GenericXyApi;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.AxisDomainRange;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.DataTreeResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Experiment;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.GenericTimeRange;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.GenericXYQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.GenericXYRequestedParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.RangeSampling;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Sampling;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.SeriesModel;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.StartEndRange;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeRange;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TreeParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TreeQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.XYAxisDescription;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.XYAxisDescriptionAxisDomain;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.XYModel;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.XYResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.XYTreeEntry;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.XYTreeEntryModel;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.XYTreeResponse;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.junit.Test;

/**
 * Test {@link DataProviderService} with generic xy endpoints with non-time
 * x-axis.
 *
 * @author Siwei Zhang
 */
public class GenericXYDataProviderServiceTest extends NewRestServerTest {
    private static final String DATA_PROVIDER_RESPONSE_FAILED_MSG = "There should be a positive response for the data provider";
    private static final String MODEL_NULL_MSG = "The model is null, maybe the analysis did not run long enough?";
    private static final int MAX_ITER = 40;
    private static final long TRACE_START_TIME = 1450193697034689597L;
    private static final long TRACE_END_TIME = 1450193745774189602L;
    private static final List<XYTreeEntry> EXPECTED_ENTRIES = List.of(
            new XYTreeEntry().labels(Arrays.asList("ust")).id(0L).parentId(-1L).hasData(true).style(null).isDefault(false),
            new XYTreeEntry().labels(Arrays.asList("UNKNOWN_PID")).id(1L).parentId(0L).hasData(true).style(null).isDefault(false));

    private GenericXyApi sfXyApi = new GenericXyApi(sfApiClient);

    /**
     * Ensure that a generic xy data provider exists and returns correct data.
     * It does not test the data itself, simply that the serialized fields are
     * the expected ones according to the protocol. Tested using generic xy
     * provider for call stack.
     *
     * @throws InterruptedException
     *             Exception thrown while waiting to execute again
     * @throws ApiException
     *             if such exception occurs
     */
    @Test
    public void testGenericXYDataProvider() throws InterruptedException, ApiException {
        Experiment exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);

        TreeParameters params = new TreeParameters();
        params.requestedTimerange(new TimeRange().start(0L).end(Long.MAX_VALUE));
        TreeQueryParameters queryParams = new TreeQueryParameters().parameters(params);

        XYTreeResponse treeResponse = sfXyApi.getGenericXYChartTree(exp.getUUID(), CALL_STACK_FUNCTION_DENSITY_DATAPROVIDER_ID, queryParams);
        assertNotNull(treeResponse);
        assertTrue(DATA_PROVIDER_RESPONSE_FAILED_MSG, !treeResponse.getStatus().equals(XYTreeResponse.StatusEnum.FAILED));
        XYTreeEntryModel responseModel = treeResponse.getModel();

        // Make sure the analysis ran enough and we have a model
        int iteration = 0;
        while ((treeResponse.getStatus().equals(XYTreeResponse.StatusEnum.RUNNING)) || (responseModel == null) && (iteration < MAX_ITER)) {
            Thread.sleep(100);
            treeResponse = sfXyApi.getGenericXYChartTree(exp.getUUID(), CALL_STACK_FUNCTION_DENSITY_DATAPROVIDER_ID, queryParams);
            assertTrue(DATA_PROVIDER_RESPONSE_FAILED_MSG, !treeResponse.getStatus().equals(DataTreeResponse.StatusEnum.FAILED));
            responseModel = treeResponse.getModel();
            iteration++;
        }

        // Validate model
        assertNotNull(responseModel);

        // Verify Entries
        List<XYTreeEntry> entries = responseModel.getEntries();
        assertNotNull(MODEL_NULL_MSG, entries);
        assertFalse(entries.isEmpty());

        // Test getting the XY series endpoint
        List<Integer> items = new ArrayList<>();
        for (XYTreeEntry entry : entries) {
            items.add(entry.getId().intValue()); // FIXME long -> integer
                                                 // conversion
        }

        // Test getting the generic xy endpoint with descriptors for xy
        GenericXYRequestedParameters xyParams = new GenericXYRequestedParameters()
                .requestedItems(items)
                .requestedTimerange(new GenericTimeRange().start(TRACE_START_TIME).end(TRACE_END_TIME).nbSamples(5));

        GenericXYQueryParameters xyQueryParams = new GenericXYQueryParameters().parameters(xyParams);
        /*
         * FIXME: Remove try/catch after fixing issue:
         * https://github.com/eclipse-tracecompass-incubator/org.eclipse.tracecompass.incubator/issues/236
         */
        try {
            XYResponse xyModelResponse = sfXyApi.getGenericXY(exp.getUUID(), CALL_STACK_FUNCTION_DENSITY_DATAPROVIDER_ID, xyQueryParams);

            assertNotNull(xyModelResponse);

            XYModel xyModel = xyModelResponse.getModel();
            List<SeriesModel> xySeries = xyModel.getSeries();
            assertFalse(xySeries.isEmpty());
        } catch (Exception ex) {
        }
    }

    /**
     * Ensure that the inside data is correct for call stack function density
     * data provider for both tree end point and xy end point.
     *
     * @throws InterruptedException
     *             Exception thrown while waiting to execute again
     * @throws ApiException
     *             if such exception occurs
     */
    @Test
    public void testCallStackFunctionDensityDataProvider() throws InterruptedException, ApiException {
        Experiment exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);

        TreeParameters params = new TreeParameters();
        params.requestedTimerange(new TimeRange().start(0L).end(Long.MAX_VALUE));
        TreeQueryParameters queryParams = new TreeQueryParameters().parameters(params);

        XYTreeResponse treeResponse = sfXyApi.getGenericXYChartTree(exp.getUUID(), CALL_STACK_FUNCTION_DENSITY_DATAPROVIDER_ID, queryParams);
        assertNotNull(treeResponse);
        assertTrue(DATA_PROVIDER_RESPONSE_FAILED_MSG, !treeResponse.getStatus().equals(XYTreeResponse.StatusEnum.FAILED));
        XYTreeEntryModel responseModel = treeResponse.getModel();

        // Make sure the analysis ran enough and we have a model
        int iteration = 0;
        while ((treeResponse.getStatus().equals(XYTreeResponse.StatusEnum.RUNNING)) || (responseModel == null) && (iteration < MAX_ITER)) {
            Thread.sleep(100);
            treeResponse = sfXyApi.getGenericXYChartTree(exp.getUUID(), CALL_STACK_FUNCTION_DENSITY_DATAPROVIDER_ID, queryParams);
            assertTrue(DATA_PROVIDER_RESPONSE_FAILED_MSG, !treeResponse.getStatus().equals(DataTreeResponse.StatusEnum.FAILED));
            responseModel = treeResponse.getModel();
            iteration++;
        }

        // Validate model
        assertNotNull(responseModel);

        int autoExpandLevel = responseModel.getAutoExpandLevel();
        assertEquals("Auto-expand level mismatch", -1, autoExpandLevel);

        // Entries
        List<XYTreeEntry> actualEntries = responseModel.getEntries();
        assertEquals("Entry count mismatch", EXPECTED_ENTRIES.size(), actualEntries.size());

        for (int i = 0; i < EXPECTED_ENTRIES.size(); i++) {
            XYTreeEntry expected = EXPECTED_ENTRIES.get(i);
            XYTreeEntry actual = actualEntries.get(i);
            assertEquals("HasRowModel mismatch at index " + i, expected.getHasData(), actual.getHasData());
            assertEquals("Labels mismatch at index " + i, expected.getLabels(), actual.getLabels());
            assertEquals("Style mismatch at index " + i, expected.getStyle(), actual.getStyle());
        }
        assertEquals("Parent-child id mismatch.", actualEntries.get(0).getId(), actualEntries.get(1).getParentId());

        /*
         * Test the data in xy end point.
         */
        List<Integer> items = new ArrayList<>();
        for (XYTreeEntry entry : actualEntries) {
            items.add(entry.getId().intValue()); // FIXME long -> integer
            // conversion
        }

        GenericXYRequestedParameters xyParams = new GenericXYRequestedParameters()
                .requestedItems(items)
                .requestedTimerange(new GenericTimeRange().start(TRACE_START_TIME).end(TRACE_END_TIME).nbSamples(5));

        GenericXYQueryParameters xyQueryParams = new GenericXYQueryParameters().parameters(xyParams);
        /*
         * FIXME: Remove try/catch after fixing issue:
         * https://github.com/eclipse-tracecompass-incubator/org.eclipse.tracecompass.incubator/issues/236
         */
        try {
            XYResponse xyModelResponse = sfXyApi.getGenericXY(exp.getUUID(), CALL_STACK_FUNCTION_DENSITY_DATAPROVIDER_ID, xyQueryParams);
            assertNotNull(xyModelResponse);
            // Make sure the analysis ran enough and we have a fully executed model
            iteration = 0;
            while (xyModelResponse.getStatus().equals(XYResponse.StatusEnum.RUNNING) && iteration < MAX_ITER) {
                Thread.sleep(100);
                xyModelResponse = sfXyApi.getGenericXY(exp.getUUID(), CALL_STACK_FUNCTION_DENSITY_DATAPROVIDER_ID, xyQueryParams);
                assertNotNull(xyModelResponse);
                iteration++;
            }

            XYModel xyModel = xyModelResponse.getModel();
            List<SeriesModel> xySeries = xyModel.getSeries();
            assertFalse(xySeries.isEmpty());
            assertEquals("Number of series mismatch", 1, xySeries.size());
            SeriesModel seriesStub = xySeries.iterator().next();

            // Validate fields
            assertEquals("Name mismatch", "UNKNOWN_PID", seriesStub.getSeriesName());

            // Validate xValues
            Sampling xValues = seriesStub.getxValues();
            assertTrue("xValues should be a RangesStub", xValues.getActualInstance() instanceof RangeSampling);
            List<StartEndRange> expectedRanges = Arrays.asList(
                    new StartEndRange().start(0L).end(1195708549L),
                    new StartEndRange().start(1195708550L).end(2391417098L),
                    new StartEndRange().start(2391417099L).end(3587125647L),
                    new StartEndRange().start(3587125648L).end(4782834196L),
                    new StartEndRange().start(4782834197L).end(5978542746L));
            List<StartEndRange> actualRanges = ((RangeSampling) xValues.getActualInstance()).getSampling();
            assertEquals("Range size mismatch", expectedRanges.size(), actualRanges.size());
            assertEquals("Range size mismatch", expectedRanges.size(), actualRanges.size());
            for (int i = 0; i < expectedRanges.size(); i++) {
                assertEquals("Range mismatch at index " + i, expectedRanges.get(i), actualRanges.get(i));
            }

            // Validate yValues
            List<Double> actualYValues = seriesStub.getyValues();
            List<Double> expectedYValues = Arrays.asList(1943.0, 1.0, 2.0, 1.0, 1.0);
            assertEquals("Y values size mismatch", expectedYValues.size(), actualYValues.size());
            assertEquals("Y values size mismatch", expectedYValues.size(), actualYValues.size());
            for (int i = 0; i < expectedYValues.size(); i++) {
                assertEquals("Y value mismatch at index " + i, expectedYValues.get(i), actualYValues.get(i), 0.000001);
            }

            // Validate axis descriptions (fully)
            XYAxisDescription xAxis = seriesStub.getxValuesDescription();
            XYAxisDescription yAxis = seriesStub.getyValuesDescription();

            assertNotNull("X axis description should not be null", xAxis);
            assertNotNull("Y axis description should not be null", yAxis);

            // X axis
            assertEquals("X axis label mismatch", "Execution Time", xAxis.getLabel());
            assertEquals("X axis unit mismatch", "ns", xAxis.getUnit());
            assertEquals("X axis data type mismatch", "DURATION", xAxis.getDataType());
            XYAxisDescriptionAxisDomain xDomain = xAxis.getAxisDomain();
            assertNotNull("X axis domain should not be null", xDomain);
            assertTrue("X axis domain should be TimeRange", xDomain.getActualInstance() instanceof AxisDomainRange);

            // Y axis
            assertEquals("Y axis label mismatch", "Number of Executions", yAxis.getLabel());
            assertEquals("Y axis unit mismatch", "", yAxis.getUnit());
            assertEquals("Y axis data type mismatch", "NUMBER", yAxis.getDataType());
            XYAxisDescriptionAxisDomain yDomain = yAxis.getAxisDomain();
            assertNull("Y axis domain should be null", yDomain);

            // Validate style
            assertNotNull("Style should not be null", seriesStub.getStyle());
            assertEquals("Series type should be bar", "bar",
                    seriesStub.getStyle().getValues().get(StyleProperties.SERIES_TYPE));
        } catch (Exception ex) {
        }
    }
    /**
     * Tests error cases when querying arrows for a time graph data provider
     */
    @Test
    public void testXYErrors() {
        Experiment exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);
        executePostErrorTests(exp.getUUID(), NewRestServerTest::getGenericXYTreeEndpoint, CALL_STACK_FUNCTION_DENSITY_DATAPROVIDER_ID, false);
        executePostErrorTests(exp.getUUID(), NewRestServerTest::getGenericXYSeriesEndpoint, CALL_STACK_FUNCTION_DENSITY_DATAPROVIDER_ID, true);
    }
}
