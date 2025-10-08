/*******************************************************************************
 * Copyright (c) 2018, 2025 Ericsson and others
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
import java.util.UUID;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.DataProviderService;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.webapp.TestDataProviderService;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.utils.RestServerTest;
import org.eclipse.tracecompass.incubator.tsp.client.core.ApiException;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.StylesApi;
import org.eclipse.tracecompass.incubator.tsp.client.core.api.TimeGraphApi;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Annotation;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Annotation.TypeEnum;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.AnnotationCategoriesResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.AnnotationModel;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.AnnotationResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.AnnotationsParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.AnnotationsQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ArrowsParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ArrowsQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.DataTreeResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Element;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Element.ElementTypeEnum;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.ErrorResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.Experiment;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.MetadataValue;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.OptionalQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.OutputElementStyle;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.OutputStyleModel;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.RequestedFilterQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.RequestedFilterQueryParameters.StrategyEnum;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.RequestedParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.RequestedQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.StylesResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeGraphArrow;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeGraphArrowsResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeGraphEntry;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeGraphModel;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeGraphRowModel;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeGraphState;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeGraphStatesResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeGraphStatesResponse.StatusEnum;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeGraphTooltipResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeGraphTreeModel;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeGraphTreeResponse;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeRange;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TooltipParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TooltipQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TreeParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.TreeQueryParameters;
import org.eclipse.tracecompass.incubator.tsp.client.core.model.XYTreeResponse;
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
public class TimeGraphDataProviderServiceTest extends RestServerTest {

    private static final String UST_CATEGORY_NAME = "ust";
    private static final String SLOT_CATEGORY_NAME = "Slot";
    private static final String CTX_SWITCH_EXPERIMENT = "ctxSwitchExperiment";
    private static final String THREAD_STATUS_DP_ID = "org.eclipse.tracecompass.internal.analysis.os.linux.core.threadstatus.ThreadStatusDataProvider";
    private static final String DATA_PROVIDER_RESPONSE_FAILED_MSG = "There should be a positive response for the data provider";
    private static final String MODEL_NULL_MSG = "The model is null, maybe the analysis did not run long enough?";
    private static final int MAX_ITER = 40;
    private static final String DEEP_SEARCH = "DEEP";
    private static final String SHALLOW_SEARCH = "SAMPLED";
    private static final String FILTER_QUERY = "test";
    private static final Integer DIMMED_FILTER_TAG = 1;

    private static final TimeGraphApi sfTgApi = new TimeGraphApi(sfApiClient);
    private static final StylesApi sfStylesApi = new StylesApi(sfApiClient);

    /**
     * Ensure that the Call Stack data provider exists for the trace.
     *
     * @throws ApiException
     *             if such exception occurs
     */
    @Test
    public void testCallStackDataProvider() throws ApiException {
        Experiment exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);

        TreeParameters params = new TreeParameters();
        params.requestedTimerange(new TimeRange().start(0L).end(Long.MAX_VALUE).nbTimes(2));
        TreeQueryParameters queryParams = new TreeQueryParameters().parameters(params);

        TimeGraphTreeResponse treeResponse = sfTgApi.getTimeGraphTree(exp.getUUID(), CALL_STACK_DATAPROVIDER_ID, queryParams);
        assertTrue(DATA_PROVIDER_RESPONSE_FAILED_MSG, !treeResponse.getStatus().equals(TimeGraphTreeResponse.StatusEnum.FAILED));

        treeResponse = sfTgApi.getTimeGraphTree(exp.getUUID(), CALL_STACK_DATAPROVIDER_ID, new TreeQueryParameters().parameters(params));
        assertTrue("Default values should return OK code", !treeResponse.getStatus().equals(TimeGraphTreeResponse.StatusEnum.FAILED));
    }

    /**
     * Ensure that a time graph data provider exists and returns correct data.
     * It does not test the data itself, simply that the serialized fields are
     * the expected ones.
     *
     * @throws InterruptedException
     *             Exception thrown while waiting to execute again
     * @throws ApiException
     *             if such exception occurs
     */
    @Test
    public void testTimeGraphDataProvider() throws InterruptedException, ApiException {
        testGetStates(null);
        testGetStates(DEEP_SEARCH);
        testGetStates(SHALLOW_SEARCH);
    }

    /**
     * Tests error cases when querying states for a time graph data provider
     */
    @Test
    public void testStatesErrors() {
        Experiment exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);
        executePostErrorTests(exp.getUUID(), RestServerTest::getArrowsEndpoint, CALL_STACK_DATAPROVIDER_ID, true);
    }

    /**
     * Tests error cases when querying states for a time graph data provider
     */
    @Test
    public void testTreeErrors() {
        Experiment exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);
        executePostErrorTests(exp.getUUID(), RestServerTest::getTimeGraphTreeEndpoint, CALL_STACK_DATAPROVIDER_ID, false);
    }

    /**
     * Tests querying styles for a time graph data provider
     *
     * @throws ApiException
     *             if such exception happens
     */
    @Test
    public void testStyles() throws ApiException {
        Experiment exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);

        StylesResponse outputResponse = sfStylesApi.getStyles(exp.getUUID(), CALL_STACK_DATAPROVIDER_ID, new OptionalQueryParameters());
        assertNotNull(outputResponse);
        assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, org.eclipse.tracecompass.incubator.tsp.client.core.model.StylesResponse.StatusEnum.COMPLETED, outputResponse.getStatus());
        OutputStyleModel model = outputResponse.getModel();
        assertNotNull(model);
        Map<String, OutputElementStyle> styles = model.getStyles();
        assertFalse(styles.isEmpty());
        OutputElementStyle elementStub = styles.get("0");
        assertNotNull(elementStub);
    }

    /**
     * Tests error cases when querying styles for a time graph data provider
     */
    @Test
    public void testStylesErrors() {
        Experiment exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);
        executePostErrorTests(exp.getUUID(), RestServerTest::getStylesEndpoint, CALL_STACK_DATAPROVIDER_ID, false);
    }

    /**
     * Tests querying arrows for a time graph data provider
     *
     * @throws InterruptedException
     *             if such exception happens
     * @throws ApiException
     *             if such exception happens
     */
    @Test
    public void testArrows() throws InterruptedException, ApiException {
        Experiment exp = assertPostExperiment(sfContextSwitchesKernelNotInitializedStub.getName(), sfContextSwitchesKernelNotInitializedStub);

        List<TimeGraphEntry> entries = loadDataProvider(exp, THREAD_STATUS_DP_ID);

        ArrowsParameters params = new ArrowsParameters().requestedTimerange(new TimeRange().start(1450193714978685130L).end(1450193715011015823L).nbTimes(1000));
        ArrowsQueryParameters queryParam = new ArrowsQueryParameters().parameters(params);
        TimeGraphArrowsResponse arrowsResponse = sfTgApi.getArrows(exp.getUUID(), THREAD_STATUS_DP_ID, queryParam);
        assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeGraphArrowsResponse.StatusEnum.COMPLETED, arrowsResponse.getStatus());

        List<TimeGraphArrow> tgModel = arrowsResponse.getModel();
        assertNotNull(tgModel);

        assertFalse(tgModel.isEmpty());

        TimeGraphArrow arrow = tgModel.get(0);
        // Verify first arrow in list
        verifyArrow(entries, arrow);
    }

    /**
     * Tests error cases when querying arrows for a time graph data provider
     */
    @Test
    public void testArrowsErrors() {
        Experiment exp = assertPostExperiment(sfContextSwitchesKernelNotInitializedStub.getName(), sfContextSwitchesKernelNotInitializedStub);
        executePostErrorTests(exp.getUUID(), RestServerTest::getArrowsEndpoint, THREAD_STATUS_DP_ID, true);
    }

    /**
     * Tests querying annotation categories for a time graph data provider
     *
     * @throws ApiException
     *             if such exception happen
     */
    @Test
    public void testAnnotationCategories() throws ApiException {
        Experiment exp = assertPostExperiment(CTX_SWITCH_EXPERIMENT, sfContextSwitchesKernelNotInitializedStub, sfContextSwitchesUstNotInitializedStub);

        // Get ust category
        AnnotationCategoriesResponse annotationCategoriesModel = sfAnnotationApi.getAnnotationCategories(exp.getUUID(), THREAD_STATUS_DP_ID, "unknown.annotation.cat.id");
        assertNotNull(annotationCategoriesModel);
        assertFalse(annotationCategoriesModel.getModel().getAnnotationCategories().isEmpty());
        List<String> categories = annotationCategoriesModel.getModel().getAnnotationCategories();
        assertFalse(categories.isEmpty());
        assertTrue(categories.contains(UST_CATEGORY_NAME));

        // get category from marker set
        annotationCategoriesModel = sfAnnotationApi.getAnnotationCategories(exp.getUUID(), THREAD_STATUS_DP_ID, "example.id");
        assertNotNull(annotationCategoriesModel);
        categories = annotationCategoriesModel.getModel().getAnnotationCategories();
        assertFalse(categories.isEmpty());
        List<String> expectedCategories = List.of("Frame", "Subframe", SLOT_CATEGORY_NAME);
        assertTrue(categories.containsAll(expectedCategories));
    }

    /**
     * Tests error cases when querying annotation categories for a time graph
     * data provider
     */
    @Test
    public void testAnnotationCategoriesErrors() {
        Experiment exp = assertPostExperiment(sfContextSwitchesKernelNotInitializedStub.getName(), sfContextSwitchesKernelNotInitializedStub);
        // Invalid UUID string
        WebTarget endpoint = getAnnotationCategoriesEndpoint(INVALID_EXP_UUID, THREAD_STATUS_DP_ID);
        try (Response response = endpoint.request(MediaType.APPLICATION_JSON).get()) {
            assertNotNull(response);
            assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }

        // Unknown experiment
        try {
            sfAnnotationApi.getAnnotationCategories(UUID.randomUUID(), THREAD_STATUS_DP_ID, "unknown.annotation.cat.id");
            fail();
        } catch (ApiException ex) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(EndpointConstants.NO_SUCH_TRACE, errorResponse.getTitle());
        }

        // Unknown data provider
        try {
            sfAnnotationApi.getAnnotationCategories(exp.getUUID(), UNKNOWN_DP_ID, "unknown.annotation.cat.id");
            fail();
        } catch (ApiException ex) {
            assertEquals(Status.METHOD_NOT_ALLOWED.getStatusCode(), ex.getCode());
            ErrorResponse errorResponse = deserializeErrorResponse(ex.getResponseBody(), ErrorResponse.class);
            assertNotNull(errorResponse);
            assertEquals(EndpointConstants.NO_PROVIDER, errorResponse.getTitle());
        }
    }

    /**
     * Tests querying annotation for a time graph data provider
     *
     * @throws InterruptedException
     *             if such exception occurred
     * @throws ApiException
     *             if such exception occurrs
     */
    @Test
    public void testAnnotations() throws InterruptedException, ApiException {
        Experiment exp = assertPostExperiment(CTX_SWITCH_EXPERIMENT, sfContextSwitchesKernelNotInitializedStub, sfContextSwitchesUstNotInitializedStub);
        List<TimeGraphEntry> entries = loadDataProvider(exp, THREAD_STATUS_DP_ID);

        List<String> categories = List.of("Frame", "Subframe", SLOT_CATEGORY_NAME, UST_CATEGORY_NAME);
        AnnotationsParameters params = new AnnotationsParameters()
                .requestedTimerange(new TimeRange().start(1450193722866679365L).end(1450193722881450790L).nbTimes(500))
                .requestedMarkerSet("example.id")
                .requestedMarkerCategories(categories);
        AnnotationsQueryParameters queryParam = new AnnotationsQueryParameters().parameters(params);

        // Find specific thread entry
        final String threadNameForTooltip = "lemon_server";
        Optional<TimeGraphEntry> threadOptional = entries.stream().filter(
                entry -> threadNameForTooltip.equals(entry.getLabels().get(0)) && entry.getLabels().get(1).equals("592")).findFirst();
        assertTrue(threadOptional.isPresent());
        params.requestedItems(List.of(Long.valueOf(threadOptional.get().getId()).intValue()));

        AnnotationResponse modelResponse = sfAnnotationApi.getAnnotations(exp.getUUID(), THREAD_STATUS_DP_ID, queryParam);

        assertEquals(org.eclipse.tracecompass.incubator.tsp.client.core.model.AnnotationResponse.StatusEnum.COMPLETED, modelResponse.getStatus());
        assertNotNull(modelResponse);

        AnnotationModel annotationModel = modelResponse.getModel();
        assertNotNull(annotationModel);
        Map<String, List<Annotation>> annotationsMap = annotationModel.getAnnotations();
        assertFalse(annotationsMap.isEmpty());
        for (String category : categories) {
            assertTrue(annotationsMap.containsKey(category));
            List<Annotation> annotations = annotationsMap.get(category);
            assertNotNull(annotations);
            assertFalse(annotations.isEmpty());
        }

        List<Annotation> annotations = annotationsMap.get(SLOT_CATEGORY_NAME);
        assertNotNull(annotations);
        Annotation annotation = annotations.iterator().next();

        // Verify first annotation created from marker set and category Slot
        assertEquals(Long.valueOf(1450193722866500000L), annotation.getTime());
        assertEquals(Long.valueOf(500000), annotation.getDuration());
        assertEquals(Long.valueOf(-1), annotation.getEntryId());
        assertEquals("1", annotation.getLabel());
        assertEquals(TypeEnum.CHART, annotation.getType());
        assertNotNull(annotation.getStyle());

        // Verify first annotation created from category ust for specific
        // thread
        annotations = annotationsMap.get(UST_CATEGORY_NAME);
        assertNotNull(annotations);
        annotation = annotations.iterator().next();
        assertEquals(Long.valueOf(1450193722867264709L), annotation.getTime());
        assertEquals(Long.valueOf(0), annotation.getDuration());
        assertEquals(threadOptional.get().getId(), annotation.getEntryId());
        assertEquals("lttng_ust_tracef:event", annotation.getLabel());
        assertEquals(TypeEnum.CHART, annotation.getType());
        assertNotNull(annotation.getStyle());
    }

    /**
     * Tests error cases when querying annotation for a time graph data provider
     */
    @Test
    public void testAnnotationErrors() {
        Experiment exp = assertPostExperiment(sfContextSwitchesKernelNotInitializedStub.getName(), sfContextSwitchesKernelNotInitializedStub);
        executePostErrorTests(exp.getUUID(), RestServerTest::getAnnotationEndpoint, THREAD_STATUS_DP_ID, true);
    }

    private static void testGetStates(String filterStrategy) throws InterruptedException, ApiException {
        try {
            Experiment exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);
            List<TimeGraphEntry> entries = loadDataProvider(exp, CALL_STACK_DATAPROVIDER_ID);

            // add entries for the states query, and make sure they don't have
            // extra time fields
            List<Integer> items = new ArrayList<>();
            // Find a specific call stack entry
            for (TimeGraphEntry entry : entries) {
                items.add(entry.getId().intValue()); // FIXME https://github.com/eclipse-cdt-cloud/trace-server-protocol/issues/140
            }

            long start = 1450193697034689597L;
            long end = 1450193697118480368L;
            RequestedParameters reqParams = new RequestedParameters()
                    .requestedTimerange(new TimeRange().start(start).end(end).nbTimes(10))
                    .requestedItems(items);
            if (filterStrategy != null) {
                RequestedFilterQueryParameters filterParams = new RequestedFilterQueryParameters()
                        .strategy(StrategyEnum.fromValue(filterStrategy))
                        .filterExpressionsMap(ImmutableMap.of(
                                Integer.toString(DIMMED_FILTER_TAG), Arrays.asList(FILTER_QUERY)));
                reqParams.filterQueryParameters(filterParams);
            }
            RequestedQueryParameters reqQueryParameters = new RequestedQueryParameters().parameters(reqParams);

            TimeGraphStatesResponse tgModelResponse = sfTgApi.getStates(exp.getUUID(), CALL_STACK_DATAPROVIDER_ID, reqQueryParameters);
            assertNotNull(tgModelResponse);
            assertEquals(StatusEnum.COMPLETED, tgModelResponse.getStatus());

            TimeGraphModel tgModel = tgModelResponse.getModel();
            List<TimeGraphRowModel> rows = tgModel.getRows();
            assertFalse(rows.isEmpty());

            // Test getting the time graph tooltip for a state
            int callstackEntryIdForTooltip = findCallStackEntry(entries);
            TimeGraphRowModel row = null;
            for (Iterator<TimeGraphRowModel> iterator = rows.iterator(); iterator.hasNext();) {
                TimeGraphRowModel timeGraphRowModel = iterator.next();
                if (timeGraphRowModel.getEntryId() == callstackEntryIdForTooltip) {
                    row = timeGraphRowModel;
                }
            }
            assertNotNull(row);
            TimeGraphState state = row.getStates().get(0);

            TooltipParameters tooltipParams = new TooltipParameters()
                    .requestedItems(Collections.singletonList(Long.valueOf(row.getEntryId()).intValue())) // FIXME https://github.com/eclipse-cdt-cloud/trace-server-protocol/issues/140
                    .requestedTimes(Collections.singletonList(state.getStart()))
                    .requestedElement(new Element()
                            .elementType(ElementTypeEnum.STATE)
                            .time(state.getStart())
                            .duration((state.getEnd() - state.getStart())));

            TooltipQueryParameters tooltipQueryParams = new TooltipQueryParameters().parameters(tooltipParams);
            TimeGraphTooltipResponse timegraphTooltipResponse = sfTgApi.getTimeGraphTooltip(exp.getUUID(), CALL_STACK_DATAPROVIDER_ID, tooltipQueryParams);

            assertNotNull(timegraphTooltipResponse);
            assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeGraphTooltipResponse.StatusEnum.COMPLETED, timegraphTooltipResponse.getStatus());
            Map<String, String> expectedTooltip = new HashMap<>();
            expectedTooltip.put("Address", "0x804a291");
            expectedTooltip.put("State", "000000000804a291");
            assertEquals(expectedTooltip, timegraphTooltipResponse.getModel());

            // Test getting the time graph tooltip for an annotation
            long time = state.getStart();
            tooltipParams = new TooltipParameters()
                    .requestedItems(Collections.singletonList(Long.valueOf(row.getEntryId()).intValue())) // FIXME https://github.com/eclipse-cdt-cloud/trace-server-protocol/issues/140
                    .requestedTimes(Collections.singletonList(time))
                    .requestedElement(new Element()
                            .elementType(ElementTypeEnum.ANNOTATION)
                            .time(time)
                            .duration(0L));

            tooltipQueryParams = new TooltipQueryParameters().parameters(tooltipParams);
            timegraphTooltipResponse = sfTgApi.getTimeGraphTooltip(exp.getUUID(), CALL_STACK_DATAPROVIDER_ID, tooltipQueryParams);

            assertNotNull(timegraphTooltipResponse);
            assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeGraphTooltipResponse.StatusEnum.COMPLETED, timegraphTooltipResponse.getStatus());

            // Test getting the time graph tooltip for an arrow
            TimeGraphRowModel destinationRow = Iterators.get(rows.iterator(), rows.size() - 2);

            tooltipParams = new TooltipParameters()
                    .requestedItems(Collections.singletonList(Long.valueOf(row.getEntryId()).intValue())) // FIXME https://github.com/eclipse-cdt-cloud/trace-server-protocol/issues/140
                    .requestedTimes(Collections.singletonList(time))
                    .requestedElement(new Element()
                            .elementType(ElementTypeEnum.ARROW)
                            .time(time)
                            .duration(0L)
                            .entryId(row.getEntryId())
                            .destinationId(destinationRow.getEntryId()));

            tooltipQueryParams = new TooltipQueryParameters().parameters(tooltipParams);
            timegraphTooltipResponse = sfTgApi.getTimeGraphTooltip(exp.getUUID(), CALL_STACK_DATAPROVIDER_ID, tooltipQueryParams);

            assertNotNull(timegraphTooltipResponse);
            assertEquals(DATA_PROVIDER_RESPONSE_FAILED_MSG, org.eclipse.tracecompass.incubator.tsp.client.core.model.TimeGraphTooltipResponse.StatusEnum.COMPLETED, timegraphTooltipResponse.getStatus());

            assertEquals(Collections.emptyMap(), timegraphTooltipResponse.getModel());

            if (filterStrategy != null) {
                // Test the tags of the state
                assertEquals(DIMMED_FILTER_TAG, state.getTags());
            }

        } catch (ProcessingException e) {
            // The failure from this exception alone is not helpful. Use the
            // suppressed exception's message be the failure message for more
            // help debugging failed tests.
            fail(e.getCause().getMessage());
        }
    }

    private static @NonNull List<TimeGraphEntry> loadDataProvider(Experiment exp, String dataProviderId) throws InterruptedException, ApiException {
        // Test getting the time graph tree
        TreeParameters params = new TreeParameters();
        TreeQueryParameters queryParams = new TreeQueryParameters().parameters(params);

        TimeGraphTreeResponse treeResponse = sfTgApi.getTimeGraphTree(exp.getUUID(), dataProviderId, queryParams);
        assertTrue(DATA_PROVIDER_RESPONSE_FAILED_MSG, !treeResponse.getStatus().equals(XYTreeResponse.StatusEnum.FAILED));
        TimeGraphTreeModel responseModel = treeResponse.getModel();

        // Make sure the analysis ran enough and we have a model
        int iteration = 0;
        while ((treeResponse.getStatus().equals(DataTreeResponse.StatusEnum.RUNNING)) || (responseModel == null) && (iteration < MAX_ITER)) {
            Thread.sleep(100);
            treeResponse = sfTgApi.getTimeGraphTree(exp.getUUID(), dataProviderId, queryParams);
            assertTrue(DATA_PROVIDER_RESPONSE_FAILED_MSG, !treeResponse.getStatus().equals(DataTreeResponse.StatusEnum.FAILED));
            responseModel = treeResponse.getModel();
            iteration++;
        }

        // Verify Entries
        assertNotNull(responseModel);
        List<TimeGraphEntry> entries = responseModel.getEntries();
        assertNotNull(MODEL_NULL_MSG, entries);
        assertFalse(entries.isEmpty());

        return entries;
    }

    private static int findCallStackEntry(List<TimeGraphEntry> entries) {
        // Find trace entry
        Optional<TimeGraphEntry> traceOptional = entries.stream().filter(entry -> entry.getParentId() == -1).findFirst();
        assertTrue(traceOptional.isPresent());
        long traceId = traceOptional.get().getId();

        // Find process
        final String processNameForTooltip = "UNKNOWN";
        Optional<TimeGraphEntry> processOptional = entries.stream().filter(
                entry -> processNameForTooltip.equals(entry.getLabels().get(0)) && entry.getParentId() == traceId).findFirst();
        assertTrue(processOptional.isPresent());
        long processEntryId = processOptional.get().getId();

        // Find specific thread entry
        final String threadNameForTooltip = "lemon_server-589";
        Optional<TimeGraphEntry> threadOptional = entries.stream().filter(
                entry -> threadNameForTooltip.equals(entry.getLabels().get(0)) && entry.getParentId() == processEntryId).findFirst();
        assertTrue(threadOptional.isPresent());
        long threadId = threadOptional.get().getId();

        // Find first callstack entry under the thread entry
        Optional<TimeGraphEntry> callstackOptional = entries.stream().filter(
                entry -> threadNameForTooltip.equals(entry.getLabels().get(0)) && entry.getParentId() == threadId).findFirst();
        assertTrue(callstackOptional.isPresent());
        return callstackOptional.get().getId().intValue(); // FIXME https://github.com/eclipse-cdt-cloud/trace-server-protocol/issues/140
    }

    /**
     * Using the custom data provider verify that only allowed types (Number,
     * String) are serialized in the metadata map of time graph entries.
     *
     * @throws ApiException
     *             if such exception happens
     * @throws InterruptedException
     *             if such exception happens
     */
    @Test
    public void testTimeGraphMetaDataSerializer() throws ApiException, InterruptedException {
        Experiment exp = assertPostExperiment(sfContextSwitchesUstNotInitializedStub.getName(), sfContextSwitchesUstNotInitializedStub);
        // Test getting the time graph tree
        List<TimeGraphEntry> entries = loadDataProvider(exp, TestDataProviderService.INVALID_ENTRY_METADATA);

        // Verify Time Graph Entries
        assertEquals(2, entries.size());
        for (TimeGraphEntry entry : entries) {
            verifyEntry(entry);
        }
    }

    private static void verifyMetadata(Map<String, List<MetadataValue>> metadata, String key, Class<?> clazz) {
        List<MetadataValue> col = metadata.get(key);
        assertNotNull(key, col);
        assertTrue(key, col.stream().allMatch((value) -> clazz.isInstance(value.getActualInstance())));
    }

    private static void verifyEntry(TimeGraphEntry entry) {
        assertFalse(entry.getLabels().isEmpty());
        if (entry.getLabels().get(0).equals(TestDataProviderService.ENTRY_NAME_WITH_METADATA)) {
            // Verify supported values for metadata in entry
            @Nullable
            Map<String, List<MetadataValue>> metadata = entry.getMetadata();
            assertNotNull(metadata);
            verifyMetadata(metadata, TestDataProviderService.VALID_TEST_KEY_BYTE, Number.class);
            verifyMetadata(metadata, TestDataProviderService.VALID_TEST_KEY_SHORT, Number.class);
            verifyMetadata(metadata, TestDataProviderService.VALID_TEST_KEY_INT, Number.class);
            verifyMetadata(metadata, TestDataProviderService.VALID_TEST_KEY_LONG, Number.class);
            verifyMetadata(metadata, TestDataProviderService.VALID_TEST_KEY_FLOAT, Number.class);
            verifyMetadata(metadata, TestDataProviderService.VALID_TEST_KEY_DOUBLE, Number.class);
            verifyMetadata(metadata, TestDataProviderService.VALID_TEST_KEY_STRING, String.class);

            // Verify unsupported object
            Collection<MetadataValue> col = metadata.get(TestDataProviderService.INVALID_TEST_KEY);
            assertNull(TestDataProviderService.INVALID_TEST_KEY, col);
        }

        if (entry.getLabels().get(0).equals(TestDataProviderService.ENTRY_NAME_WITHOUT_METADATA)) {
            // Verify that entry doesn't have metadata
            assertTrue(entry.getMetadata().isEmpty());
        }
    }

    private static void verifyArrow(List<TimeGraphEntry> entries, TimeGraphArrow arrow) {
        Optional<TimeGraphEntry> entryOptional = entries.stream().filter(entry -> entry.getId().equals(arrow.getSourceId())).findFirst();
        assertTrue(entryOptional.isPresent());
        TimeGraphEntry sourceEntry = entryOptional.get();
        assertEquals("lsmod", sourceEntry.getLabels().get(0));

        entryOptional = entries.stream().filter(entry -> entry.getId().equals(arrow.getTargetId())).findFirst();
        assertTrue(entryOptional.isPresent());
        TimeGraphEntry targetEntry = entryOptional.get();
        assertEquals("rcu_preempt", targetEntry.getLabels().get(0));

        assertNotNull(arrow.getStyle());
        assertTrue(arrow.getEnd() > arrow.getStart());
    }
}
