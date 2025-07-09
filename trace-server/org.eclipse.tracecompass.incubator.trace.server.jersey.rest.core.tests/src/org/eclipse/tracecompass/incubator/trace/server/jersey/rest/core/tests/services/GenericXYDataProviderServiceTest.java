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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.EntryStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.ExperimentModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.IAxisDomainStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.XyEntryModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.XyEntryStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.XyTreeOutputResponseStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.ISamplingStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TmfXYAxisDescriptionStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.XyModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.XyOutputResponseStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.XySeriesStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.eclipse.tracecompass.internal.tmf.core.Activator;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test {@link DataProviderService} with generic xy endpoints with non-time x-axis.
 *
 * @author Siwei Zhang
 */
@SuppressWarnings({"null"})
public class GenericXYDataProviderServiceTest extends RestServerTest {
    private static final String DATA_PROVIDER_RESPONSE_FAILED_MSG = "There should be a positive response for the data provider";
    private static final String MODEL_NULL_MSG = "The model is null, maybe the analysis did not run long enough?";
    private static final String REQUESTED_ITEMS_KEY = "requested_items";
    private static final String REQUESTED_TIMERANGE_KEY = "requested_timerange";
    private static final String START = "start";
    private static final String END = "end";
    private static final String NB_SAMPLES = "nbSamples";
    private static final int MAX_ITER = 40;
    private static final long TRACE_START_TIME = 1450193697034689597L;
    private static final long TRACE_END_TIME = 1450193745774189602L;
    private static final List<XyEntryStub> EXPECTED_ENTRIES = List.of(
            new XyEntryStub(Arrays.asList("ust"), 0, -1, true, null, true),
            new XyEntryStub(Arrays.asList("UNKNOWN_PID"), 1, 0, true, null, false));

    /**
     * Ensure that a generic xy data provider exists and returns correct data.
     * It does not test the data itself, simply that the serialized fields are
     * the expected ones according to the protocol. Tested using generic xy
     * provider for call stack.
     *
     * @throws InterruptedException
     *             Exception thrown while waiting to execute again
     */
    @Test
    public void testGenericXYDataProvider() throws InterruptedException {
        ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);

        WebTarget callstackTree = getGenericXYTreeEndpoint(exp.getUUID().toString(), CALL_STACK_FUNCTION_DENSITY_DATAPROVIDER_ID);

        // Test getting the tree endpoint with descriptors
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(REQUESTED_TIMES_KEY, List.of(0L, Long.MAX_VALUE));
        XyTreeOutputResponseStub responseModel;
        try (Response tree = callstackTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, tree.getStatus());
            responseModel = tree.readEntity(XyTreeOutputResponseStub.class);
            assertNotNull(responseModel);
        }
        // Make sure the analysis ran enough and we have a model
        int iteration = 0;
        while (responseModel.isRunning() && responseModel.getModel() == null && iteration < MAX_ITER) {
            Thread.sleep(100);
            try (Response treeResponse = callstackTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
                assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, treeResponse.getStatus());
                responseModel = treeResponse.readEntity(XyTreeOutputResponseStub.class);
                assertNotNull(responseModel);
                iteration++;
            }
        }

        // Validate model
        XyEntryModelStub model = responseModel.getModel();
        assertNotNull(MODEL_NULL_MSG + responseModel, model);

        List<XyEntryStub> entries = model.getEntries();
        assertFalse(entries.isEmpty());

        // Test getting the generic xy endpoint with descriptors for xy
        WebTarget xyEnpoint = getGenericXYSeriesEndpoint(exp.getUUID().toString(), CALL_STACK_FUNCTION_DENSITY_DATAPROVIDER_ID);
        parameters = new HashMap<>();
        List<Integer> items = new ArrayList<>();
        for (EntryStub entry : entries) {
            items.add(entry.getId());
        }
        parameters.put(REQUESTED_ITEMS_KEY, items);
        parameters.put(REQUESTED_TIMERANGE_KEY, ImmutableMap.of(START, TRACE_START_TIME, END, TRACE_END_TIME, NB_SAMPLES, 5));
        try (Response series = xyEnpoint.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, series.getStatus());
            XyOutputResponseStub xyModelResponse = series.readEntity(XyOutputResponseStub.class);
            assertNotNull(xyModelResponse);

            XyModelStub xyModel = xyModelResponse.getModel();
            Set<XySeriesStub> xySeries = xyModel.getSeries();
            assertFalse(xySeries.isEmpty());
        }
    }

    /**
     * Ensure that the inside data is correct for call stack function density
     * data provider for both tree end point and xy end point.
     *
     * @throws InterruptedException
     *             Exception thrown while waiting to execute again
     */
    @Test
    public void testCallStackFunctionDensityDataProvider() throws InterruptedException {
        ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);

        WebTarget callstackTree = getGenericXYTreeEndpoint(exp.getUUID().toString(), CALL_STACK_FUNCTION_DENSITY_DATAPROVIDER_ID);

        /*
         * Test the data in the tree end point with descriptors.
         */
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(REQUESTED_TIMES_KEY, List.of(0L, Long.MAX_VALUE));
        XyTreeOutputResponseStub responseModel;
        try (Response tree = callstackTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, tree.getStatus());
            responseModel = tree.readEntity(XyTreeOutputResponseStub.class);
            assertNotNull(responseModel);
        }
        // Make sure the analysis ran enough and we have a model
        int iteration = 0;
        while (responseModel.isRunning() && responseModel.getModel() == null && iteration < MAX_ITER) {
            Thread.sleep(100);
            try (Response treeResponse = callstackTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
                assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, treeResponse.getStatus());
                responseModel = treeResponse.readEntity(XyTreeOutputResponseStub.class);
                assertNotNull(responseModel);
                iteration++;
            }
        }

        // Validate model
        XyEntryModelStub model = responseModel.getModel();
        assertNotNull(MODEL_NULL_MSG + responseModel, model);

        int autoExpandLevel = model.getAutoExpandLevel();
        assertEquals("Auto-expand level mismatch", -1, autoExpandLevel);

        // Entries
        List<XyEntryStub> actualEntries = model.getEntries();
        assertEquals("Entry count mismatch", EXPECTED_ENTRIES.size(), actualEntries.size());

        for (int i = 0; i < EXPECTED_ENTRIES.size(); i++) {
            XyEntryStub expected = EXPECTED_ENTRIES.get(i);
            XyEntryStub actual = actualEntries.get(i);
            assertEquals("HasRowModel mismatch at index " + i, expected.hasRowModel(), actual.hasRowModel());
            assertEquals("Labels mismatch at index " + i, expected.getLabels(), actual.getLabels());
            assertEquals("Style mismatch at index " + i, expected.getStyle(), actual.getStyle());
        }
        assertEquals("Parent-child id mismatch.", actualEntries.get(0).getId(), actualEntries.get(1).getParentId());

        /*
         * Test the data in xy end point.
         */
        WebTarget xyEnpoint = getGenericXYSeriesEndpoint(exp.getUUID().toString(), CALL_STACK_FUNCTION_DENSITY_DATAPROVIDER_ID);
        parameters = new HashMap<>();
        List<Integer> items = new ArrayList<>();
        for (EntryStub entry : actualEntries) {
            items.add(entry.getId());
        }
        parameters.put(REQUESTED_ITEMS_KEY, items);
        parameters.put(REQUESTED_TIMERANGE_KEY, ImmutableMap.of(START, TRACE_START_TIME, END, TRACE_END_TIME, NB_SAMPLES, 5));
        XyOutputResponseStub xyModelResponse;
        try (Response series = xyEnpoint.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, series.getStatus());
            xyModelResponse = series.readEntity(XyOutputResponseStub.class);
        }
        assertNotNull(xyModelResponse);
        // Make sure the analysis ran enough and we have a fully executed model
        iteration = 0;
        while (xyModelResponse.isRunning() && iteration < MAX_ITER) {
            Thread.sleep(100);
            try (Response response = xyEnpoint.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
                Activator.logWarning("current status: " + xyModelResponse.isRunning());
                assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, response.getStatus());
                xyModelResponse = response.readEntity(XyOutputResponseStub.class);
                assertNotNull(xyModelResponse);
                iteration++;
            }
        }
        XyModelStub xyModel = xyModelResponse.getModel();
        Set<XySeriesStub> xySeries = xyModel.getSeries();
        assertEquals("Number of series mismatch", 1, xySeries.size());
        XySeriesStub seriesStub = xySeries.iterator().next();

        // Validate fields
        assertEquals("Name mismatch", "UNKNOWN_PID", seriesStub.getName());

        // Validate xValues
        ISamplingStub xValues = seriesStub.getXValues();
        assertTrue("xValues should be a RangesStub", xValues instanceof ISamplingStub.RangesStub);
        List<ISamplingStub.RangesStub.RangeStub> expectedRanges = Arrays.asList(
                new ISamplingStub.RangesStub.RangeStub(0L, 1195708549L),
                new ISamplingStub.RangesStub.RangeStub(1195708550L, 2391417098L),
                new ISamplingStub.RangesStub.RangeStub(2391417099L, 3587125647L),
                new ISamplingStub.RangesStub.RangeStub(3587125648L, 4782834196L),
                new ISamplingStub.RangesStub.RangeStub(4782834197L, 5978542746L)
            );
        List<ISamplingStub.RangesStub.RangeStub> actualRanges = ((ISamplingStub.RangesStub) xValues).getRanges();
        assertEquals("Range size mismatch", expectedRanges.size(), actualRanges.size());
        assertEquals("Range size mismatch", expectedRanges.size(), actualRanges.size());
        for (int i = 0; i < expectedRanges.size(); i++) {
            assertEquals("Range mismatch at index " + i, expectedRanges.get(i), actualRanges.get(i));
        }

        // Validate yValues
        List<Double> actualYValues = seriesStub.getYValues();
        List<Double> expectedYValues = Arrays.asList(1943.0, 1.0, 2.0, 1.0, 1.0);
        assertEquals("Y values size mismatch", expectedYValues.size(), actualYValues.size());
        assertEquals("Y values size mismatch", expectedYValues.size(), actualYValues.size());
        for (int i = 0; i < expectedYValues.size(); i++) {
            assertEquals("Y value mismatch at index " + i, expectedYValues.get(i), actualYValues.get(i), 0.000001);
        }

        // Validate axis descriptions (fully)
        TmfXYAxisDescriptionStub xAxis = seriesStub.getXAxisDescription();
        TmfXYAxisDescriptionStub yAxis = seriesStub.getYAxisDescription();

        assertNotNull("X axis description should not be null", xAxis);
        assertNotNull("Y axis description should not be null", yAxis);

        // X axis
        assertEquals("X axis label mismatch", "Execution Time", xAxis.getLabel());
        assertEquals("X axis unit mismatch", "ns", xAxis.getUnit());
        assertEquals("X axis data type mismatch", "DURATION", xAxis.getDataType());
        IAxisDomainStub xDomain = xAxis.getAxisDomain();
        assertNotNull("X axis domain should not be null", xDomain);
        assertTrue("X axis domain should be TimeRange", xDomain instanceof IAxisDomainStub.RangeStub);

        // Y axis
        assertEquals("Y axis label mismatch", "Number of Executions", yAxis.getLabel());
        assertEquals("Y axis unit mismatch", "", yAxis.getUnit());
        assertEquals("Y axis data type mismatch", "NUMBER", yAxis.getDataType());
        IAxisDomainStub yDomain = yAxis.getAxisDomain();
        assertNull("Y axis domain should be null", yDomain);

        // Validate style
        assertNotNull("Style should not be null", seriesStub.getStyle());
        assertEquals("Series type should be bar", "bar",
                seriesStub.getStyle().getStyleValues().get(StyleProperties.SERIES_TYPE));
    }
}
