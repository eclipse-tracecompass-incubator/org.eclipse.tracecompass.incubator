/*******************************************************************************
 * Copyright (c) 2022 Ericsson and others
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.QueryParametersUtil;
import org.eclipse.tracecompass.tmf.core.model.annotations.Annotation;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * Test the {@link QueryParametersUtil} class
 */
public class QueryParametersUtilTest {

    private static final String NAME = "name";
    private static final String MISSING_NAME = "Missing query parameters: name";
    private static final String INVALID_NAME = "Invalid query parameters: name";
    private static final String REQUESTED_ELEMENT = "requested_element";
    private static final String MISSING_REQUESTED_ELEMENT = "Missing query parameters: requested_element";
    private static final String INVALID_REQUESTED_ELEMENT = "Invalid query parameters: requested_element";
    private static final String REQUESTED_ITEMS = "requested_items";
    private static final String MISSING_REQUESTED_ITEMS = "Missing query parameters: requested_items";
    private static final String INVALID_REQUESTED_ITEMS = "Invalid query parameters: requested_items";
    private static final String REQUESTED_TIMERANGE = "requested_timerange";
    private static final String MISSING_REQUESTED_TIMERANGE = "Missing query parameters: requested_timerange";
    private static final String INVALID_REQUESTED_TIMERANGE = "Invalid query parameters: requested_timerange";
    private static final String REQUESTED_TIMES = "requested_times";
    private static final String MISSING_REQUESTED_TIMES = "Missing query parameters: requested_times";
    private static final String INVALID_REQUESTED_TIMES = "Invalid query parameters: requested_times";
    private static final String TRACES = "traces";
    private static final String MISSING_TRACES = "Missing query parameters: traces";
    private static final String INVALID_TRACES = "Invalid query parameters: traces";
    private static final String URI = "uri";
    private static final String MISSING_URI = "Missing query parameters: uri";
    private static final String INVALID_URI = "Invalid query parameters: uri";
    private static final String REGEX_MAP_FILTERS = "regex_map_filters";
    private static final String FILTER_EXPRESSIONS_MAP = "filter_expressions_map";
    private static final String FILTER_QUERY_PARAMETERS = "filter_query_parameters";
    private static final String STRATEGY = "strategy";
    private static final String FULL_SEARCH = "full_search";
    private static final String DEEP_SEARCH = "DEEP";
    private static final String INVALID_FILTER_EXPRESSIONS_MAP = "Invalid query parameters: filter_expressions_map";
    private static final String INVALID_INTEGER_KEY = "b2";
    private static final String VALID_INTEGER_KEY = "1";

    private static final String DESTINATION_ID = "destinationId";
    private static final String DURATION = "duration";
    private static final String ELEMENT_TYPE = "elementType";
    private static final String END = "end";
    private static final String ENTRY_ID = "entryId";
    private static final String NB_TIMES = "nbTimes";
    private static final String START = "start";
    private static final String TIME = "time";

    /**
     * Test the validateTraceQueryParameters method.
     */
    @Test
    public void testValidateTraceQueryParameters() {
        assertEquals(MISSING_URI,
                QueryParametersUtil.validateTraceQueryParameters(Maps.newHashMap(ImmutableMap.of())));
        assertEquals(INVALID_URI,
                QueryParametersUtil.validateTraceQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        URI, 0L))));
        assertNull(
                QueryParametersUtil.validateTraceQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        URI, "/uri"))));
    }

    /**
     * Test the validateExperimentQueryParameters method.
     */
    @Test
    public void testValidateExperimentQueryParameters() {
        assertEquals(MISSING_NAME,
                QueryParametersUtil.validateExperimentQueryParameters(Maps.newHashMap(ImmutableMap.of())));
        assertEquals(INVALID_NAME,
                QueryParametersUtil.validateExperimentQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        NAME, 0L))));
        assertEquals(MISSING_TRACES,
                QueryParametersUtil.validateExperimentQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        NAME, "trace"))));
        assertEquals(INVALID_TRACES,
                QueryParametersUtil.validateExperimentQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        NAME, "trace",
                        TRACES, 0L))));
        assertEquals(INVALID_TRACES,
                QueryParametersUtil.validateExperimentQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        NAME, "trace",
                        TRACES, Arrays.asList(0L)))));
        assertNull(
                QueryParametersUtil.validateExperimentQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        NAME, "trace",
                        TRACES, Arrays.asList()))));
        assertNull(
                QueryParametersUtil.validateExperimentQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        NAME, "trace",
                        TRACES, Arrays.asList("uuid")))));
    }

    /**
     * Test the validateTreeQueryParameters method.
     */
    @Test
    public void testValidateTreeQueryParameters() {
        assertNull(
                QueryParametersUtil.validateTreeQueryParameters(Maps.newHashMap(ImmutableMap.of())));
        assertEquals(INVALID_REQUESTED_TIMERANGE,
                QueryParametersUtil.validateTreeQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMERANGE, ImmutableMap.of()))));
        assertNull(
                QueryParametersUtil.validateTreeQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1000L)))));

        assertEquals(INVALID_REQUESTED_TIMES,
                QueryParametersUtil.validateTreeQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMES, Arrays.asList(0L)))));
        assertNull(
                QueryParametersUtil.validateTreeQueryParameters(Maps.newHashMap()));
        assertNull(
                QueryParametersUtil.validateTreeQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMES, Arrays.asList()))));
        assertNull(
                QueryParametersUtil.validateTreeQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMES, Arrays.asList(0L, 1000L)))));

        /* Test conversion of requested_timerange to requested_times */
        Map<String, Object> params;

        params = Maps.newHashMap(ImmutableMap.of(
                REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1000L)));
        assertNull(QueryParametersUtil.validateTreeQueryParameters(params));
        assertEquals(Arrays.asList(0L, 1000L), params.get(REQUESTED_TIMES));
        assertNull(params.get(REQUESTED_TIMERANGE));
    }

    /**
     * Test the validateRequestedQueryParameters method.
     */
    @Test
    public void testValidateRequestedQueryParameters() {
        assertEquals(MISSING_REQUESTED_ITEMS,
                QueryParametersUtil.validateRequestedQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1000L, NB_TIMES, 5)))));
        assertEquals(MISSING_REQUESTED_TIMERANGE,
                QueryParametersUtil.validateRequestedQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_ITEMS, Arrays.asList(0L, 1L, 2L)))));
        assertEquals(INVALID_REQUESTED_TIMERANGE,
                QueryParametersUtil.validateRequestedQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMERANGE, ImmutableMap.of(),
                        REQUESTED_ITEMS, Arrays.asList(0L, 1L, 2L)))));
        assertEquals(INVALID_REQUESTED_ITEMS,
                QueryParametersUtil.validateRequestedQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1000L, NB_TIMES, 5),
                        REQUESTED_ITEMS, 0L))));
        assertNull(
                QueryParametersUtil.validateRequestedQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1000L, NB_TIMES, 5),
                        REQUESTED_ITEMS, Arrays.asList(0L, 1L, 2L)))));
        assertEquals(INVALID_REQUESTED_TIMES,
                QueryParametersUtil.validateRequestedQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMES, Arrays.asList(),
                        REQUESTED_ITEMS, Arrays.asList(0L, 1L, 2L)))));
        assertNull(
                QueryParametersUtil.validateRequestedQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMES, Arrays.asList(0L, 250L, 500L, 750L, 1000L),
                        REQUESTED_ITEMS, Arrays.asList(0L, 1L, 2L)))));

        /* Test conversion of requested_timerange to requested_times */
        Map<String, Object> params;

        params = Maps.newHashMap(ImmutableMap.of(
                REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1000L, NB_TIMES, 5),
                REQUESTED_ITEMS, Arrays.asList(0L, 1L, 2L)));
        assertNull(QueryParametersUtil.validateRequestedQueryParameters(params));
        assertEquals(Arrays.asList(0L, 250L, 500L, 750L, 1000L), params.get(REQUESTED_TIMES));
        assertNull(params.get(REQUESTED_TIMERANGE));

        params = Maps.newHashMap(ImmutableMap.of(
                REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1000L, NB_TIMES, 0),
                REQUESTED_ITEMS, Arrays.asList(0L, 1L, 2L)));
        assertNull(QueryParametersUtil.validateRequestedQueryParameters(params));
        assertEquals(Arrays.asList(0L, 1000L), params.get(REQUESTED_TIMES));
        assertNull(params.get(REQUESTED_TIMERANGE));

        params = Maps.newHashMap(ImmutableMap.of(
                REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1048560L, NB_TIMES, Integer.MAX_VALUE),
                REQUESTED_ITEMS, Arrays.asList(0L, 1L, 2L)));
        assertNull(QueryParametersUtil.validateRequestedQueryParameters(params));
        assertEquals(65536, ((List<?>) params.getOrDefault(REQUESTED_TIMES, Collections.emptyList())).size());
        assertNull(params.get(REQUESTED_TIMERANGE));

        params = Maps.newHashMap(ImmutableMap.of(
                REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1000L),
                REQUESTED_ITEMS, Arrays.asList(0L, 1L, 2L)));
        assertNull(QueryParametersUtil.validateRequestedQueryParameters(params));
        assertEquals(Arrays.asList(0L, 1000L), params.get(REQUESTED_TIMES));
        assertNull(params.get(REQUESTED_TIMERANGE));
    }

    /**
     * Test the validateArrowsQueryParameters method.
     */
    @Test
    public void testValidateArrowsQueryParameters() {
        assertEquals(MISSING_REQUESTED_TIMERANGE,
                QueryParametersUtil.validateArrowsQueryParameters(Maps.newHashMap(ImmutableMap.of())));
        assertEquals(INVALID_REQUESTED_TIMERANGE,
                QueryParametersUtil.validateArrowsQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMERANGE, ImmutableMap.of()))));
        assertNull(
                QueryParametersUtil.validateArrowsQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1000L, NB_TIMES, 5)))));
        assertEquals(INVALID_REQUESTED_TIMES,
                QueryParametersUtil.validateArrowsQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMES, Arrays.asList()))));
        assertNull(
                QueryParametersUtil.validateArrowsQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMES, Arrays.asList(0L, 250L, 500L, 750L, 1000L)))));

        /* Test conversion of requested_timerange to requested_times */
        Map<String, Object> params;

        params = Maps.newHashMap(ImmutableMap.of(
                REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1000L, NB_TIMES, 5)));
        assertNull(QueryParametersUtil.validateArrowsQueryParameters(params));
        assertEquals(Arrays.asList(0L, 250L, 500L, 750L, 1000L), params.get(REQUESTED_TIMES));
        assertNull(params.get(REQUESTED_TIMERANGE));

        params = Maps.newHashMap(ImmutableMap.of(
                REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1000L, NB_TIMES, 0)));
        assertNull(QueryParametersUtil.validateArrowsQueryParameters(params));
        assertEquals(Arrays.asList(0L, 1000L), params.get(REQUESTED_TIMES));
        assertNull(params.get(REQUESTED_TIMERANGE));

        params = Maps.newHashMap(ImmutableMap.of(
                REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1048560L, NB_TIMES, Integer.MAX_VALUE)));
        assertNull(QueryParametersUtil.validateArrowsQueryParameters(params));
        assertEquals(65536, ((List<?>) params.getOrDefault(REQUESTED_TIMES, Collections.emptyList())).size());
        assertNull(params.get(REQUESTED_TIMERANGE));

        params = Maps.newHashMap(ImmutableMap.of(
                REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1000L)));
        assertNull(QueryParametersUtil.validateArrowsQueryParameters(params));
        assertEquals(Arrays.asList(0L, 1000L), params.get(REQUESTED_TIMES));
        assertNull(params.get(REQUESTED_TIMERANGE));
    }

    /**
     * Test the validateAnnotationsQueryParameters method.
     */
    @Test
    public void testValidateAnnotationsQueryParameters() {
        assertNull(
                QueryParametersUtil.validateAnnotationsQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1000L, NB_TIMES, 5)))));
        assertEquals(MISSING_REQUESTED_TIMERANGE,
                QueryParametersUtil.validateAnnotationsQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_ITEMS, Arrays.asList(0L, 1L, 2L)))));
        assertEquals(INVALID_REQUESTED_TIMERANGE,
                QueryParametersUtil.validateAnnotationsQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMERANGE, ImmutableMap.of(),
                        REQUESTED_ITEMS, Arrays.asList(0L, 1L, 2L)))));
        assertEquals(INVALID_REQUESTED_ITEMS,
                QueryParametersUtil.validateAnnotationsQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1000L, NB_TIMES, 5),
                        REQUESTED_ITEMS, 0L))));
        assertNull(
                QueryParametersUtil.validateAnnotationsQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1000L, NB_TIMES, 5),
                        REQUESTED_ITEMS, Arrays.asList(0L, 1L, 2L)))));
        assertEquals(INVALID_REQUESTED_TIMES,
                QueryParametersUtil.validateAnnotationsQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMES, Arrays.asList(),
                        REQUESTED_ITEMS, Arrays.asList(0L, 1L, 2L)))));
        assertNull(
                QueryParametersUtil.validateAnnotationsQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMES, Arrays.asList(0L, 250L, 500L, 750L, 1000L),
                        REQUESTED_ITEMS, Arrays.asList(0L, 1L, 2L)))));

        /* Test conversion of requested_timerange to requested_times */
        Map<String, Object> params;

        params = Maps.newHashMap(ImmutableMap.of(
                REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1000L, NB_TIMES, 5),
                REQUESTED_ITEMS, Arrays.asList(0L, 1L, 2L)));
        assertNull(QueryParametersUtil.validateAnnotationsQueryParameters(params));
        assertEquals(Arrays.asList(0L, 250L, 500L, 750L, 1000L), params.get(REQUESTED_TIMES));
        assertNull(params.get(REQUESTED_TIMERANGE));

        params = Maps.newHashMap(ImmutableMap.of(
                REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1000L, NB_TIMES, 0),
                REQUESTED_ITEMS, Arrays.asList(0L, 1L, 2L)));
        assertNull(QueryParametersUtil.validateAnnotationsQueryParameters(params));
        assertEquals(Arrays.asList(0L, 1000L), params.get(REQUESTED_TIMES));
        assertNull(params.get(REQUESTED_TIMERANGE));

        params = Maps.newHashMap(ImmutableMap.of(
                REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1048560L, NB_TIMES, Integer.MAX_VALUE),
                REQUESTED_ITEMS, Arrays.asList(0L, 1L, 2L)));
        assertNull(QueryParametersUtil.validateAnnotationsQueryParameters(params));
        assertEquals(65536, ((List<?>) params.getOrDefault(REQUESTED_TIMES, Collections.emptyList())).size());
        assertNull(params.get(REQUESTED_TIMERANGE));

        params = Maps.newHashMap(ImmutableMap.of(
                REQUESTED_TIMERANGE, ImmutableMap.of(START, 0L, END, 1000L),
                REQUESTED_ITEMS, Arrays.asList(0L, 1L, 2L)));
        assertNull(QueryParametersUtil.validateAnnotationsQueryParameters(params));
        assertEquals(Arrays.asList(0L, 1000L), params.get(REQUESTED_TIMES));
        assertNull(params.get(REQUESTED_TIMERANGE));
    }

    /**
     * Test the validateTooltipQueryParameters method.
     */
    @Test
    public void testValidateTooltipQueryParameters() {
        assertEquals(MISSING_REQUESTED_TIMES,
                QueryParametersUtil.validateTooltipQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_ITEMS, Arrays.asList(1L),
                        REQUESTED_ELEMENT, ImmutableMap.of(ELEMENT_TYPE, "state", TIME, 0L, DURATION, 10L)))));
        assertEquals(MISSING_REQUESTED_ITEMS,
                QueryParametersUtil.validateTooltipQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMES, Arrays.asList(0L),
                        REQUESTED_ELEMENT, ImmutableMap.of(ELEMENT_TYPE, "state", TIME, 0L, DURATION, 10L)))));
        assertEquals(MISSING_REQUESTED_ELEMENT,
                QueryParametersUtil.validateTooltipQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMES, Arrays.asList(0L),
                        REQUESTED_ITEMS, Arrays.asList(1L)))));
        assertEquals(INVALID_REQUESTED_TIMES,
                QueryParametersUtil.validateTooltipQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMES, Arrays.asList(),
                        REQUESTED_ITEMS, Arrays.asList(1L),
                        REQUESTED_ELEMENT, ImmutableMap.of(ELEMENT_TYPE, "state", TIME, 0L, DURATION, 10L)))));
        assertEquals(INVALID_REQUESTED_ITEMS,
                QueryParametersUtil.validateTooltipQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMES, Arrays.asList(0L),
                        REQUESTED_ITEMS, 0L,
                        REQUESTED_ELEMENT, ImmutableMap.of(ELEMENT_TYPE, "state", TIME, 0L, DURATION, 10L)))));
        assertEquals(INVALID_REQUESTED_ELEMENT,
                QueryParametersUtil.validateTooltipQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMES, Arrays.asList(0L),
                        REQUESTED_ITEMS, Arrays.asList(1L),
                        REQUESTED_ELEMENT, ImmutableMap.of(ELEMENT_TYPE, "", TIME, 0L, DURATION, 10L)))));
        assertNull(
                QueryParametersUtil.validateTooltipQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMES, Arrays.asList(0L),
                        REQUESTED_ITEMS, Arrays.asList(1L),
                        REQUESTED_ELEMENT, ImmutableMap.of(ELEMENT_TYPE, "state", TIME, 0L, DURATION, 10L)))));

        /* Test conversion of requested_element map to Object */
        Map<String, Object> params;

        params = Maps.newHashMap(ImmutableMap.of(
                REQUESTED_TIMES, Arrays.asList(0L),
                REQUESTED_ITEMS, Arrays.asList(1L),
                REQUESTED_ELEMENT, ImmutableMap.of(ELEMENT_TYPE, "state", TIME, 0L, DURATION, 10L)));
        assertNull(QueryParametersUtil.validateTooltipQueryParameters(params));
        assertTrue(params.get(REQUESTED_ELEMENT) instanceof TimeGraphState);

        params = Maps.newHashMap(ImmutableMap.of(
                REQUESTED_TIMES, Arrays.asList(0L),
                REQUESTED_ITEMS, Arrays.asList(1L),
                REQUESTED_ELEMENT, ImmutableMap.of(ELEMENT_TYPE, "annotation", TIME, 0L, DURATION, 10L, ENTRY_ID, 1L)));
        assertNull(QueryParametersUtil.validateTooltipQueryParameters(params));
        assertTrue(params.get(REQUESTED_ELEMENT) instanceof Annotation);

        params = Maps.newHashMap(ImmutableMap.of(
                REQUESTED_TIMES, Arrays.asList(0L),
                REQUESTED_ITEMS, Arrays.asList(1L),
                REQUESTED_ELEMENT, ImmutableMap.of(ELEMENT_TYPE, "arrow", TIME, 0L, DURATION, 10L, ENTRY_ID, 1L, DESTINATION_ID, 2L)));
        assertNull(QueryParametersUtil.validateTooltipQueryParameters(params));
        assertTrue(params.get(REQUESTED_ELEMENT) instanceof TimeGraphArrow);
    }

    /**
     * Test the validateLinesQueryParameters method.
     */
    @Test
    public void testValidateLinesQueryParameters() {
        assertNull(
                QueryParametersUtil.validateLinesQueryParameters(Maps.newHashMap(ImmutableMap.of())));
        assertEquals(INVALID_REQUESTED_TIMES,
                QueryParametersUtil.validateLinesQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMES, 0L))));
        assertNull(
                QueryParametersUtil.validateLinesQueryParameters(Maps.newHashMap(ImmutableMap.of(
                        REQUESTED_TIMES, Arrays.asList(0L)))));
    }

    /**
     * Test the validateFilterQueryParameters method.
     */
    @Test
    public void testValidateFilterQueryParameters() {
        assertNull(
                QueryParametersUtil.validateFilterQueryParameters(Maps.newHashMap()));

        /* Test conversion of filter_query_parameters to regex_map_filters*/
        Map<String, Object> params;

        params = Maps.newHashMap(ImmutableMap.of(FILTER_QUERY_PARAMETERS,
                ImmutableMap.of(
                    STRATEGY, DEEP_SEARCH,
                    FILTER_EXPRESSIONS_MAP,
                    ImmutableMap.of(
                        INVALID_INTEGER_KEY, Arrays.asList(REQUESTED_ELEMENT)
                    )
                )));
      assertEquals(INVALID_FILTER_EXPRESSIONS_MAP,
              QueryParametersUtil.validateFilterQueryParameters(params));

        params = Maps.newHashMap(ImmutableMap.of(FILTER_QUERY_PARAMETERS,
        ImmutableMap.of(
            STRATEGY, DEEP_SEARCH,
            FILTER_EXPRESSIONS_MAP,
            ImmutableMap.of(
                VALID_INTEGER_KEY, Arrays.asList(REQUESTED_ELEMENT)
            )
        )));
        assertNull(QueryParametersUtil.validateFilterQueryParameters(params));
        assertEquals(ImmutableMap.of(
                Integer.parseInt(VALID_INTEGER_KEY), Arrays.asList(REQUESTED_ELEMENT)
            ), params.get(REGEX_MAP_FILTERS));
        assertEquals(true, params.get(FULL_SEARCH));
        assertNull(params.get(FILTER_QUERY_PARAMETERS));

        params = Maps.newHashMap(ImmutableMap.of(FILTER_QUERY_PARAMETERS, ImmutableMap.of()));
        assertNull(QueryParametersUtil.validateFilterQueryParameters(params));
        assertNull(params.get(FILTER_QUERY_PARAMETERS));
        assertNull(params.get(REGEX_MAP_FILTERS));
    }
}
