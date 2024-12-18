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
import java.util.UUID;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.QueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.AnnotationCategoriesOutputResponseStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.AnnotationModelStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.AnnotationResponseStub;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.AnnotationStub;
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
import org.eclipse.tracecompass.tmf.core.model.annotations.IAnnotation.AnnotationType;
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

    private static final String UST_CATEGORY_NAME = "ust";
    private static final String SLOT_CATEGORY_NAME = "Slot";
    private static final String CTX_SWITCH_EXPERIMENT = "ctxSwitchExperiment";
    private static final String THREAD_STATUS_DP_ID = "org.eclipse.tracecompass.internal.analysis.os.linux.core.threadstatus.ThreadStatusDataProvider";
    private static final String DATA_PROVIDER_RESPONSE_FAILED_MSG = "There should be a positive response for the data provider";
    private static final String MODEL_NULL_MSG = "The model is null, maybe the analysis did not run long enough?";
    private static final int MAX_ITER = 40;
    private static final String REQUESTED_TIMERANGE_KEY = "requested_timerange";
    private static final String REQUESTED_CATAGORIES_KEY = "requested_marker_categories";
    private static final String REQUESTED_MARKERSET_ID_KEY = "requested_marker_set";
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
     * Tests error cases when querying states for a time graph data provider
     */
    @Test
    public void testStatesErrors() {
        ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);
        executePostErrorTests(exp, RestServerTest::getArrowsEndpoint, CALL_STACK_DATAPROVIDER_ID, true);
    }

    /**
     * Tests error cases when querying states for a time graph data provider
     */
    @Test
    public void testTreeErrors() {
        ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);
        executePostErrorTests(exp, RestServerTest::getTimeGraphTreeEndpoint, CALL_STACK_DATAPROVIDER_ID, false);
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

    /**
     * Tests querying annotation categories for a time graph data provider
     */
    @Test
    public void testAnnotationCategories() {
        ExperimentModelStub exp = assertPostExperiment(CTX_SWITCH_EXPERIMENT, sfContextSwitchesKernelNotInitializedStub, sfContextSwitchesUstNotInitializedStub);

        // Get ust category
        WebTarget categoriesEndpoint = getAnnotationCategoriesEndpoint(exp.getUUID().toString(), THREAD_STATUS_DP_ID, "unknown.annotation.cat.id");
        AnnotationCategoriesOutputResponseStub annotationCategoriesModel = categoriesEndpoint.request(MediaType.APPLICATION_JSON).get(AnnotationCategoriesOutputResponseStub.class);
        assertNotNull(annotationCategoriesModel);
        assertFalse(annotationCategoriesModel.getModel().getAnnotationCategories().isEmpty());
        List<String> categories = annotationCategoriesModel.getModel().getAnnotationCategories();
        assertFalse(categories.isEmpty());
        assertTrue(categories.contains(UST_CATEGORY_NAME));

        // get category from marker set
        categoriesEndpoint = getAnnotationCategoriesEndpoint(exp.getUUID().toString(), THREAD_STATUS_DP_ID, "example.id");
        annotationCategoriesModel = categoriesEndpoint.request(MediaType.APPLICATION_JSON).get(AnnotationCategoriesOutputResponseStub.class);

        assertNotNull(annotationCategoriesModel);
        categories = annotationCategoriesModel.getModel().getAnnotationCategories();
        assertFalse(categories.isEmpty());
        List<String> expectedCategories = List.of("Frame", "Subframe", SLOT_CATEGORY_NAME);
        assertTrue(categories.containsAll(expectedCategories));
    }

    /**
     * Tests error cases when querying annotation categories for a time graph data provider
     */
    @Test
    public void testAnnotationCategoriesErrors() {
        ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesKernelNotInitializedStub.getName(), sfContextSwitchesKernelNotInitializedStub);
        // Invalid UUID string
        WebTarget endpoint = getAnnotationCategoriesEndpoint(INVALID_EXP_UUID, THREAD_STATUS_DP_ID);
        try (Response response = endpoint.request(MediaType.APPLICATION_JSON).get()) {
            assertNotNull(response);
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }

        // Unknown experiment
        endpoint = getAnnotationCategoriesEndpoint(UUID.randomUUID().toString(), THREAD_STATUS_DP_ID);
        try (Response response = endpoint.request(MediaType.APPLICATION_JSON).get()) {
            assertNotNull(response);
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
            assertEquals(EndpointConstants.NO_SUCH_TRACE, response.readEntity(String.class));
        }

        // Unknown data provider
        endpoint = getAnnotationCategoriesEndpoint(exp.getUUID().toString(), UNKNOWN_DP_ID);
        try (Response response = endpoint.request(MediaType.APPLICATION_JSON).get()) {
            assertNotNull(response);
            assertEquals(Status.METHOD_NOT_ALLOWED.getStatusCode(), response.getStatus());
            assertEquals(EndpointConstants.NO_PROVIDER, response.readEntity(String.class));
        }
    }

    /**
     * Tests querying annotation for a time graph data provider
     *
     * @throws InterruptedException
     *             if such exception occurred
     */
    @Test
    public void testAnnotations() throws InterruptedException {
        ExperimentModelStub exp = assertPostExperiment(CTX_SWITCH_EXPERIMENT, sfContextSwitchesKernelNotInitializedStub, sfContextSwitchesUstNotInitializedStub);
        Set<TimeGraphEntryStub> entries =  loadDataProvider(exp, THREAD_STATUS_DP_ID);

        Map<String, Object> parameters = new HashMap<>();
        parameters.remove(REQUESTED_TIMES_KEY);
        parameters.put(REQUESTED_TIMERANGE_KEY, ImmutableMap.of(START, 1450193722866679365L, END, 1450193722881450790L, NB_TIMES, 500));
        parameters.put(REQUESTED_MARKERSET_ID_KEY, "example.id");
        List<String> categories = List.of("Frame", "Subframe", SLOT_CATEGORY_NAME, UST_CATEGORY_NAME);
        parameters.put(REQUESTED_CATAGORIES_KEY, categories);

        // Find specific thread entry
        final String threadNameForTooltip = "lemon_server";
        Optional<TimeGraphEntryStub> threadOptional = entries.stream().filter(
                entry -> threadNameForTooltip.equals(entry.getLabels().get(0)) && entry.getLabels().get(1).equals("592")).findFirst();
        assertTrue(threadOptional.isPresent());
        parameters.put(REQUESTED_ITEMS_KEY, List.of(threadOptional.get().getId()));

        WebTarget annoationEndpoint = getAnnotationEndpoint(exp.getUUID().toString(), THREAD_STATUS_DP_ID);
        try (Response response = annoationEndpoint.request().post(Entity.json(new QueryParameters(parameters, Collections.emptyList())))) {
            assertNotNull(response);
            assertEquals(Status.OK.getStatusCode(), response.getStatus());
            AnnotationResponseStub modelResponse = response.readEntity(AnnotationResponseStub.class);
            assertNotNull(modelResponse);

            AnnotationModelStub annotationModel = modelResponse.getModel();
            assertNotNull(annotationModel);
            Map<String, Collection<AnnotationStub>> annotationsMap = annotationModel.getAnnotations();
            assertFalse(annotationsMap.isEmpty());
            for (String category : categories) {
                assertTrue(annotationsMap.containsKey(category));
                Collection<AnnotationStub> annotations = annotationsMap.get(category);
                assertNotNull(annotations);
                assertFalse(annotations.isEmpty());
            }

            Collection<AnnotationStub> annotations = annotationsMap.get(SLOT_CATEGORY_NAME);
            assertNotNull(annotations);
            AnnotationStub annotation = annotations.iterator().next();

            // Verify first annotation created from marker set and category Slot
            assertEquals(1450193722866500000L, annotation.getTime());
            assertEquals(500000, annotation.getDuration());
            assertEquals(-1, annotation.getEntryId());
            assertEquals("1", annotation.getLabel());
            assertEquals(AnnotationType.CHART.name(), annotation.getType());
            assertNotNull(annotation.getStyle());

            // Verify first annotation created from category ust for specific thread
            annotations = annotationsMap.get(UST_CATEGORY_NAME);
            assertNotNull(annotations);
            annotation = annotations.iterator().next();
            assertEquals(1450193722867264709L, annotation.getTime());
            assertEquals(0, annotation.getDuration());
            assertEquals(threadOptional.get().getId(), annotation.getEntryId());
            assertEquals("lttng_ust_tracef:event", annotation.getLabel());
            assertEquals(AnnotationType.CHART.name(), annotation.getType());
            assertNotNull(annotation.getStyle());
        }
    }

    /**
     * Tests error cases when querying annotation for a time graph data provider
     */
    @Test
    public void testAnnotationErrors() {
        ExperimentModelStub exp = assertPostExperiment(sfContextSwitchesKernelNotInitializedStub.getName(), sfContextSwitchesKernelNotInitializedStub);
        executePostErrorTests(exp, RestServerTest::getAnnotationEndpoint, THREAD_STATUS_DP_ID, true);
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
