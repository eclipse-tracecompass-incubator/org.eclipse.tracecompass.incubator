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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.ExperimentModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.OutputElementStyleStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.OutputStyleModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.StylesOutputResponseStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TgArrowsOutputResponseStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TgEntryModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TgStatesOutputResponseStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TgTooltipOutputResponseStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TgTreeOutputResponseStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TimeGraphArrowStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TimeGraphEntryStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TimeGraphModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TimeGraphRowStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TimeGraphStateStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.webapp.TestDataProviderService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;

/**
 * Test the {@link DataProviderService}
 *
 * @author Loic Prieur-Drevon
 * @author Geneviève Bastien
 * @author Bernd Hufmann
 */
@SuppressWarnings({"null", "restriction"})
public class TimeGraphDataProviderServiceTest extends RestServerTest {

    private static final String THREAD_STATUS_DP_ID = "org.eclipse.tracecompass.internal.analysis.os.linux.core.threadstatus.ThreadStatusDataProvider";
    private static final String DATA_PROVIDER_RESPONSE_FAILED_MSG = "There should be a positive response for the data provider";
    private static final String MODEL_NULL_MSG = "The model is null, maybe the analysis did not run long enough?";
    private static final int MAX_ITER = 40;
    private static final String REQUESTED_TIMERANGE_KEY = "requested_timerange";
    private static final String REQUESTED_ITEMS_KEY = "requested_items";
    private static final String REQUESTED_ELEMENT_KEY = "requested_element";
    private static final String ELEMENT_TYPE = "elementType";
    private static final String STATE = "state";
    private static final String ANNOTATION = "annotation";
    private static final String ARROW = "arrow";
    private static final String TIME = "time";
    private static final String DURATION = "duration";
    private static final String ENTRY_ID = "entryId";
    private static final String DESTINATION_ID = "destinationId";
    private static final String START = "start";
    private static final String END = "end";
    private static final String NB_TIMES = "nbTimes";
    private static final String FILTER_EXPRESSIONS_MAP = "filter_expressions_map";
    private static final String FILTER_QUERY_PARAMETERS = "filter_query_parameters";
    private static final String STRATEGY = "strategy";
    private static final String DEEP_SEARCH = "DEEP";
    private static final String SHALLOW_SEARCH = "SHALLOW";
    private static final String FILTER_QUERY = "test";
    private static final int DIMMED_FILTER_TAG = 1;

    /**
     * Ensure that the Call Stack data provider exists for the trace.
     */
    @Test
    public void testCallStackDataProvider() {
        ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);

        WebTarget callstackTree = getTimeGraphTreeEndpoint(exp.getUUID().toString(), CALL_STACK_DATAPROVIDER_ID);

        Map<String, Object> parameters = FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0L, Long.MAX_VALUE, 2));
        try (Response tree = callstackTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, tree.getStatus());
        }
        parameters = new HashMap<>();
        try (Response defaults = callstackTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals("Default values should return OK code", 200, defaults.getStatus());
        }
    }

    /**
     * Ensure that a time graph data provider exists and returns correct data.
     * It does not test the data itself, simply that the serialized fields are
     * the expected ones.
     *
     * @throws InterruptedException
     *             Exception thrown while waiting to execute again
     */
    @Test
    public void testTimeGraphDataProvider() throws InterruptedException {
        testGetStates(null);
        testGetStates(DEEP_SEARCH);
        testGetStates(SHALLOW_SEARCH);
    }

    /**
     * Tests querying styles for a time graph data provider
     */
    @Test
    public void testStyles() {
        ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);

        WebTarget stylesEndpoint = getStylesEndpoint(exp.getUUID().toString(), CALL_STACK_DATAPROVIDER_ID);

        Map<String, Object> parameters = new HashMap<>();
        try (Response response = stylesEndpoint.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertNotNull(response);
            assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, Status.OK.getStatusCode(), response.getStatus());
            StylesOutputResponseStub outputResponse = response.readEntity(StylesOutputResponseStub.class);
            assertNotNull(outputResponse);
            OutputStyleModelStub model = outputResponse.getModel();
            assertNotNull(model);
            Map<String, OutputElementStyleStub> styles = model.getStyles();
            assertFalse(styles.isEmpty());
            OutputElementStyleStub elementStub = styles.get("0");
            assertNotNull(elementStub);
        }
    }

    /**
     * Tests error cases when querying styles for a time graph data provider
     */
    @Test
    public void testStylesErrors() {
        ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);
        executePostErrorTests(exp, RestServerTest::getStylesEndpoint, CALL_STACK_DATAPROVIDER_ID, false);
    }

    /**
     * Tests querying arrows for a time graph data provider
     *
     * @throws InterruptedException
     *             if such exception happens
     */
    @Test
    public void testArrows() throws InterruptedException {
        ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesKernelNotInitializedStub.getName(), sfContextSwitchesKernelNotInitializedStub);

        Set<TimeGraphEntryStub> entries = loadDataProvider(exp, THREAD_STATUS_DP_ID);
        WebTarget arrowsEndpoint = getArrowsEndpoint(exp.getUUID().toString(), THREAD_STATUS_DP_ID);

        Map<String, Object> parameters = new HashMap<>();
        parameters.remove(REQUESTED_TIMES_KEY);
        parameters.put(REQUESTED_TIMERANGE_KEY, ImmutableMap.of(START, 1450193714978685130L, END, 1450193715011015823L, NB_TIMES, 1000));

        try (Response arrowsResponse = arrowsEndpoint.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, Status.OK.getStatusCode(), arrowsResponse.getStatus());

            TgArrowsOutputResponseStub tgArrowsModelResponse = arrowsResponse.readEntity(TgArrowsOutputResponseStub.class);
            assertNotNull(tgArrowsModelResponse);

            List<TimeGraphArrowStub> tgModel = tgArrowsModelResponse.getModel();
            assertNotNull(tgModel);
            assertFalse(tgModel.isEmpty());

            TimeGraphArrowStub arrow = tgModel.get(0);
            // Verify first arrow in list
            verifyArrow(entries, arrow);
        }
    }

    /**
     * Tests error cases when querying arrows for a time graph data provider
     */
    @Test
    public void testArrowsErrors() {
        ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesKernelNotInitializedStub.getName(), sfContextSwitchesKernelNotInitializedStub);
        executePostErrorTests(exp, RestServerTest::getArrowsEndpoint, THREAD_STATUS_DP_ID, true);
    }

    private static void testGetStates(String filterStrategy) throws InterruptedException {
        try {
            ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);
            Set<TimeGraphEntryStub> entries = loadDataProvider(exp, CALL_STACK_DATAPROVIDER_ID);

            // add entries for the states query, and make sure they don't have
            // extra time fields
            List<Integer> items = new ArrayList<>();
            // Find a specific call stack entry
            for (TimeGraphEntryStub entry : entries) {
                items.add(entry.getId());
            }

            // Test getting the time graph row data
            WebTarget tgStatesEnpoint = getTimeGraphStatesEndpoint(exp.getUUID().toString(), CALL_STACK_DATAPROVIDER_ID);
            Map<String, Object> parameters = new HashMap<>();
            parameters.remove(REQUESTED_TIMES_KEY);
            parameters.put(REQUESTED_TIMERANGE_KEY, ImmutableMap.of(START, 1450193697034689597L, END, 1450193697118480368L, NB_TIMES, 10));
            parameters.put(REQUESTED_ITEMS_KEY, items);
            if (filterStrategy != null) {
                parameters.put(FILTER_QUERY_PARAMETERS, ImmutableMap.of(
                        STRATEGY, filterStrategy,
                        FILTER_EXPRESSIONS_MAP,
                        ImmutableMap.of(
                                Integer.toString(DIMMED_FILTER_TAG), Arrays.asList(FILTER_QUERY)
                                )
                        ));
            }
            try (Response statesResponse = tgStatesEnpoint.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
                assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, statesResponse.getStatus());

                TgStatesOutputResponseStub tgStateModelResponse = statesResponse.readEntity(TgStatesOutputResponseStub.class);
                assertNotNull(tgStateModelResponse);

                TimeGraphModelStub tgModel = tgStateModelResponse.getModel();
                Set<TimeGraphRowStub> rows = tgModel.getRows();
                assertFalse(rows.isEmpty());

                // Test getting the time graph tooltip for a state
                int callstackEntryIdForTooltip = findCallStackEntry(entries);
                TimeGraphRowStub row = null;
                for (Iterator<TimeGraphRowStub> iterator = rows.iterator(); iterator.hasNext();) {
                    TimeGraphRowStub timeGraphRowStub = iterator.next();
                    if (timeGraphRowStub.getEntryId() == callstackEntryIdForTooltip) {
                        row = timeGraphRowStub;
                    }
                }
                assertNotNull(row);
                TimeGraphStateStub state = row.getStates().get(0);
                WebTarget tgTooltipEnpoint = getTimeGraphTooltipEndpoint(exp.getUUID().toString(), CALL_STACK_DATAPROVIDER_ID);
                parameters.put(REQUESTED_ITEMS_KEY, Collections.singletonList(row.getEntryId()));
                parameters.put(REQUESTED_TIMES_KEY, Collections.singletonList(state.getStartTime()));
                parameters.put(REQUESTED_ELEMENT_KEY, ImmutableMap.of(
                        ELEMENT_TYPE, STATE,
                        TIME, state.getStartTime(),
                        DURATION, (state.getEndTime() - state.getStartTime())));
                try (Response tooltipResponse = tgTooltipEnpoint.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
                    assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, tooltipResponse.getStatus());

                    TgTooltipOutputResponseStub timegraphTooltipResponse = tooltipResponse.readEntity(TgTooltipOutputResponseStub.class);
                    assertNotNull(timegraphTooltipResponse);
                    Map<String, String> expectedTooltip = new HashMap<>();
                    expectedTooltip.put("Address", "0x804a291");
                    expectedTooltip.put("State", "000000000804a291");
                    assertEquals(expectedTooltip, timegraphTooltipResponse.getModel());
                }

                // Test getting the time graph tooltip for an annotation
                long time = state.getStartTime();
                parameters.put(REQUESTED_ITEMS_KEY, Collections.singletonList(row.getEntryId()));
                parameters.put(REQUESTED_TIMES_KEY, Collections.singletonList(state.getStartTime()));
                parameters.put(REQUESTED_ELEMENT_KEY, ImmutableMap.of(
                        ELEMENT_TYPE, ANNOTATION,
                        TIME, time,
                        DURATION, 0L,
                        ENTRY_ID, row.getEntryId()));
                try (Response tooltipResponse = tgTooltipEnpoint.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
                    assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, tooltipResponse.getStatus());

                    // Test getting the time graph tooltip for an arrow
                    TimeGraphRowStub destinationRow = Iterators.get(rows.iterator(), rows.size() - 2);
                    parameters.put(REQUESTED_ITEMS_KEY, Collections.singletonList(row.getEntryId()));
                    parameters.put(REQUESTED_TIMES_KEY, Collections.singletonList(state.getStartTime()));
                    parameters.put(REQUESTED_ELEMENT_KEY, ImmutableMap.of(
                            ELEMENT_TYPE, ARROW,
                            TIME, time,
                            DURATION, 0L,
                            ENTRY_ID, row.getEntryId(),
                            DESTINATION_ID, destinationRow.getEntryId()));
                }

                try (Response tooltipResponse = tgTooltipEnpoint.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
                    assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, tooltipResponse.getStatus());

                    TgTooltipOutputResponseStub timegraphTooltipResponse = tooltipResponse.readEntity(TgTooltipOutputResponseStub.class);
                    assertNotNull(timegraphTooltipResponse);
                    assertEquals(Collections.emptyMap(), timegraphTooltipResponse.getModel());
                }

                if (filterStrategy != null) {
                    // Test the tags of the state
                    assertEquals(DIMMED_FILTER_TAG, state.getTags());
                }
            }

        } catch (ProcessingException e) {
            // The failure from this exception alone is not helpful. Use the
            // suppressed exception's message be the failure message for more
            // help debugging failed tests.
            fail(e.getCause().getMessage());
        }
    }

    private static @NonNull Set<TimeGraphEntryStub> loadDataProvider(ExperimentModelStub exp, String dataProviderId) throws InterruptedException {
        // Test getting the time graph tree
        WebTarget callstackTree = getTimeGraphTreeEndpoint(exp.getUUID().toString(), dataProviderId);

        Map<String, Object> parameters = new HashMap<>();
        TgTreeOutputResponseStub responseModel;
        try (Response treeResponse = callstackTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, treeResponse.getStatus());
            responseModel = treeResponse.readEntity(TgTreeOutputResponseStub.class);
            assertNotNull(responseModel);
        }

        // Make sure the analysis ran enough and we have a model
        int iteration = 0;
        while ((responseModel.isRunning() || responseModel.getModel() == null) && iteration < MAX_ITER) {
            Thread.sleep(100);
            try (Response treeResponse = callstackTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
                assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, treeResponse.getStatus());
                responseModel = treeResponse.readEntity(TgTreeOutputResponseStub.class);
                assertNotNull(responseModel);
                iteration++;
            }
        }
        TgEntryModelStub model = responseModel.getModel();
        assertNotNull(MODEL_NULL_MSG + responseModel, model);
        Set<TimeGraphEntryStub> entries = model.getEntries();
        assertFalse(entries.isEmpty());
        return entries;
    }

    private static int findCallStackEntry(Set<TimeGraphEntryStub> entries) {
        // Find trace entry
        Optional<TimeGraphEntryStub> traceOptional = entries.stream().filter(entry -> entry.getParentId() == -1).findFirst();
        assertTrue(traceOptional.isPresent());
        int traceId = traceOptional.get().getId();

        // Find process
        final String processNameForTooltip = "UNKNOWN";
        Optional<TimeGraphEntryStub> processOptional = entries.stream().filter(
                entry -> processNameForTooltip.equals(entry.getLabels().get(0)) && entry.getParentId() == traceId).findFirst();
        assertTrue(processOptional.isPresent());
        int processEntryId = processOptional.get().getId();

        // Find specific thread entry
        final String threadNameForTooltip = "lemon_server-589";
        Optional<TimeGraphEntryStub> threadOptional = entries.stream().filter(
                entry -> threadNameForTooltip.equals(entry.getLabels().get(0)) && entry.getParentId() == processEntryId).findFirst();
        assertTrue(threadOptional.isPresent());
        int threadId = threadOptional.get().getId();

        // Find first callstack entry under the thread entry
        Optional<TimeGraphEntryStub> callstackOptional = entries.stream().filter(
                entry -> threadNameForTooltip.equals(entry.getLabels().get(0)) && entry.getParentId() == threadId).findFirst();
        assertTrue(callstackOptional.isPresent());
        return callstackOptional.get().getId();
    }

    /**
     * Using the custom data provider verify that only allowed types (Number,
     * String) are serialized in the metadata map of time graph entries.
     */
    @Test
    public void testTimeGraphMetaDataSerializer() {
        ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);

        // Test getting the time graph tree
        WebTarget callstackTree = getTimeGraphTreeEndpoint(exp.getUUID().toString(), TestDataProviderService.INVALID_ENTRY_METADATA);

        Map<String, Object> parameters = new HashMap<>();
        TgTreeOutputResponseStub responseModel;
        parameters.put(REQUESTED_TIMES_KEY, List.of(1450193697034689597L, 1450193745774189602L));
        try (Response treeResponse = callstackTree.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, 200, treeResponse.getStatus());
            responseModel = treeResponse.readEntity(TgTreeOutputResponseStub.class);
            assertNotNull(responseModel);

            // Verify Time Graph Entries
            Set<TimeGraphEntryStub> entries = responseModel.getModel().getEntries();
            assertEquals(2, entries.size());
            for (TimeGraphEntryStub entry : entries) {
                verifyEntry(entry);
            }
        } catch (ProcessingException e) {
            // The failure from this exception alone is not helpful. Use the
            // suppressed exception's message be the failure message for more
            // help debugging failed tests.
            fail(e.getCause().getMessage());
        }
    }

    private static void verifyMetadata(Map<String, Collection<Object>> metadata, String key, Class<?> clazz) {
        Collection<Object> col = metadata.get(key);
        assertNotNull(key, col);
        assertTrue(key, col.stream().allMatch(clazz::isInstance));
    }

    private static void verifyEntry(TimeGraphEntryStub entry) {
        assertFalse(entry.getLabels().isEmpty());
        if (entry.getLabels().get(0).equals(TestDataProviderService.ENTRY_NAME_WITH_METADATA)) {
            // Verify supported values for metadata in entry
            @Nullable Map<String, Collection<Object>> metadata = entry.getMetadata();
            assertNotNull(metadata);
            verifyMetadata(metadata, TestDataProviderService.VALID_TEST_KEY_BYTE, Number.class);
            verifyMetadata(metadata, TestDataProviderService.VALID_TEST_KEY_SHORT, Number.class);
            verifyMetadata(metadata, TestDataProviderService.VALID_TEST_KEY_INT, Number.class);
            verifyMetadata(metadata, TestDataProviderService.VALID_TEST_KEY_LONG, Number.class);
            verifyMetadata(metadata, TestDataProviderService.VALID_TEST_KEY_FLOAT, Number.class);
            verifyMetadata(metadata, TestDataProviderService.VALID_TEST_KEY_DOUBLE, Number.class);
            verifyMetadata(metadata, TestDataProviderService.VALID_TEST_KEY_STRING, String.class);

            // Verify unsupported object
            Collection<Object> col = metadata.get(TestDataProviderService.INVALID_TEST_KEY);
            assertNull(TestDataProviderService.INVALID_TEST_KEY, col);
        }

        if (entry.getLabels().get(0).equals(TestDataProviderService.ENTRY_NAME_WITHOUT_METADATA)) {
            // Verify that entry doesn't have metadata
            assertNull(entry.getMetadata());
        }
    }

    private static void verifyArrow(Set<TimeGraphEntryStub> entries, TimeGraphArrowStub arrow) {
        Optional<TimeGraphEntryStub> entryOptional = entries.stream().filter(entry -> entry.getId() == arrow.getSourceId()).findFirst();
        assertTrue(entryOptional.isPresent());
        TimeGraphEntryStub sourceEntry = entryOptional.get();
        assertEquals("lsmod", sourceEntry.getLabels().get(0));

        entryOptional = entries.stream().filter(entry -> entry.getId() == arrow.getTargetId()).findFirst();
        assertTrue(entryOptional.isPresent());
        TimeGraphEntryStub targetEntry = entryOptional.get();
        assertEquals("rcu_preempt", targetEntry.getLabels().get(0));

        assertNotNull(arrow.getStyle());
        assertTrue(arrow.getEndTime() > arrow.getStartTime());
    }
}
