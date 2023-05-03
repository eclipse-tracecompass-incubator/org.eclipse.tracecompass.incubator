/**********************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.INVALID_PARAMETERS;
import static org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services.EndpointConstants.MISSING_PARAMETERS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.FilterQueryParameters;
import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.FilterQueryParameters.FilterQueryStrategy;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.annotations.Annotation;
import org.eclipse.tracecompass.tmf.core.model.annotations.IAnnotation.AnnotationType;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;

/**
 * Utility methods to validate and convert query parameters from the input Trace
 * Server Protocol to the output data provider interfaces.
 */
public class QueryParametersUtil {

    private interface ElementType {
        String STATE = "state"; //$NON-NLS-1$
        String ANNOTATION = "annotation"; //$NON-NLS-1$
        String ARROW = "arrow"; //$NON-NLS-1$
    }

    private static final String DESTINATION_ID = "destinationId"; //$NON-NLS-1$
    private static final String DURATION = "duration"; //$NON-NLS-1$
    private static final String ELEMENT_TYPE = "elementType"; //$NON-NLS-1$
    private static final String END = "end"; //$NON-NLS-1$
    private static final String ENTRY_ID = "entryId"; //$NON-NLS-1$
    private static final String FILTER_QUERY_PARAMETERS = "filter_query_parameters";  //$NON-NLS-1$
    private static final String FILTER_EXPRESSIONS_MAP = "filter_expressions_map";  //$NON-NLS-1$
    private static final String NAME = "name"; //$NON-NLS-1$
    private static final String NBTIMES = "nbTimes"; //$NON-NLS-1$
    private static final String REQUESTED_TIMERANGE_KEY = "requested_timerange"; //$NON-NLS-1$
    private static final String SEP = ": "; //$NON-NLS-1$
    private static final String START = "start"; //$NON-NLS-1$
    private static final String STRATEGY = "strategy"; //$NON-NLS-1$
    private static final String TIME = "time"; //$NON-NLS-1$
    private static final String TRACES = "traces"; //$NON-NLS-1$
    private static final String URI = "uri"; //$NON-NLS-1$

    private static final long MAX_NBTIMES = 1 << 16;
    private static final @NonNull OutputElementStyle EMPTY_STYLE = new OutputElementStyle(null, Collections.emptyMap());

    /**
     * Validate trace query parameters.
     *
     * @param params
     *            the map of query parameters
     * @return an error message if validation fails, or null otherwise
     */
    public static String validateTraceQueryParameters(Map<String, Object> params) {
        String errorMessage;
        if ((errorMessage = validateString(URI, params)) != null) {
            return errorMessage;
        }
        return null;
    }

    /**
     * Validate experiment query parameters.
     *
     * @param params
     *            the map of query parameters
     * @return an error message if validation fails, or null otherwise
     */
    public static String validateExperimentQueryParameters(Map<String, Object> params) {
        String errorMessage;
        if ((errorMessage = validateString(NAME, params)) != null) {
            return errorMessage;
        }
        if ((errorMessage = validateStringList(TRACES, params)) != null) {
            return errorMessage;
        }
        return null;
    }

    /**
     * Validate and convert tree query parameters.
     *
     * @param params
     *            the mutable map of query parameters
     * @return an error message if validation fails, or null otherwise
     */
    public static String validateTreeQueryParameters(Map<String, Object> params) {
        String errorMessage;
        if ((errorMessage = validateRequestedTimeRange(params, false, true)) != null) {
            return errorMessage;
        }
        return null;
    }

    /**
     * Validate and convert requested query parameters.
     *
     * @param params
     *            the mutable map of query parameters
     * @return an error message if validation fails, or null otherwise
     */
    public static String validateRequestedQueryParameters(Map<String, Object> params) {
        String errorMessage;
        if ((errorMessage = validateRequestedTimeRange(params, true, false)) != null) {
            return errorMessage;
        }
        if ((errorMessage = validateRequestedItems(params, true)) != null) {
            return errorMessage;
        }
        return null;
    }

    /**
     * Validate and convert arrows query parameters.
     *
     * @param params
     *            the mutable map of query parameters
     * @return an error message if validation fails, or null otherwise
     */
    public static String validateArrowsQueryParameters(Map<String, Object> params) {
        String errorMessage;
        if ((errorMessage = validateRequestedTimeRange(params, true, false)) != null) {
            return errorMessage;
        }
        return null;
    }

    /**
     * Validate and convert annotations query parameters.
     *
     * @param params
     *            the mutable map of query parameters
     * @return an error message if validation fails, or null otherwise
     */
    public static String validateAnnotationsQueryParameters(Map<String, Object> params) {
        String errorMessage;
        if ((errorMessage = validateRequestedTimeRange(params, true, false)) != null) {
            return errorMessage;
        }
        if ((errorMessage = validateRequestedItems(params, false)) != null) {
            return errorMessage;
        }
        return null;
    }

    /**
     * Validate and convert tooltip query parameters.
     *
     * @param params
     *            the mutable map of query parameters
     * @return an error message if validation fails, or null otherwise
     */
    public static String validateTooltipQueryParameters(Map<String, Object> params) {
        String errorMessage;
        if ((errorMessage = validateRequestedTimes(params, true)) != null) {
            return errorMessage;
        }
        if ((errorMessage = validateRequestedItems(params, true)) != null) {
            return errorMessage;
        }
        if ((errorMessage = validateRequestedElement(params)) != null) {
            return errorMessage;
        }
        return null;
    }

    /**
     * Validate and convert lines query parameters.
     *
     * @param params
     *            the mutable map of query parameters
     * @return an error message if validation fails, or null otherwise
     */
    public static String validateLinesQueryParameters(Map<String, Object> params) {
        String errorMessage;
        if ((errorMessage = validateRequestedTimes(params, false)) != null) {
            return errorMessage;
        }
        return null;
    }

    /**
     * Validate and convert the filter_query_parameters query parameter
     *
     * @param params
     *            the mutable map of query parameters
     * @return an error message if validation fails, or null otherwise
     */
    public static String validateFilterQueryParameters(Map<String, Object> params) {
        Object value = params.get(FILTER_QUERY_PARAMETERS);

        if (value == null) {
            return null;
        }

        if (value instanceof Map) {
            Object filterExpressionsMapObj = ((Map<String, Object>) value).get(FILTER_EXPRESSIONS_MAP);
            if (filterExpressionsMapObj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) filterExpressionsMapObj;
                if (map.size() > 0) {
                    Map<Integer, Collection<String>> filterExpressionsMap = new HashMap<>();
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        try {
                            if (entry.getKey() instanceof String && entry.getValue() instanceof Collection<?>) {
                                if (((Collection<?>) entry.getValue()).stream().allMatch(item -> item instanceof String)) {
                                    filterExpressionsMap.put(Integer.parseInt((String) entry.getKey()), new ArrayList<>((Collection<String>) entry.getValue()));
                                }
                            }
                        } catch (NumberFormatException e) {
                            return INVALID_PARAMETERS + SEP + FILTER_EXPRESSIONS_MAP;
                        }
                    }

                    if (filterExpressionsMap.size() > 0) {
                        FilterQueryParameters filterQueryParameters = new FilterQueryParameters();
                        filterQueryParameters.setFilterExpressionsMap(filterExpressionsMap);

                        Object filterExpressionsStrategyObject = ((Map<String, Object>) value).get(STRATEGY);
                        if (FilterQueryStrategy.DEEP.name().equals(filterExpressionsStrategyObject)) {
                            filterQueryParameters.setStrategy(FilterQueryStrategy.DEEP);
                        }

                        params.put(DataProviderParameterUtils.REGEX_MAP_FILTERS_KEY, filterExpressionsMap);

                        Boolean isDeepSearch = filterQueryParameters.isDeepSearch();
                        if (isDeepSearch) {
                            params.put(DataProviderParameterUtils.FULL_SEARCH_KEY, isDeepSearch);
                        }
                    }
                }
            }
        }
        params.remove(FILTER_QUERY_PARAMETERS);
        return null;
    }

    /**
     * Validate a string query parameter.
     *
     * @param params
     *            the map of query parameters
     * @return an error message if validation fails, or null otherwise
     */
    private static String validateString(String param, Map<String, Object> params) {
        Object value = params.get(param);
        if (value == null) {
            return MISSING_PARAMETERS + SEP + param;
        }
        if (!(value instanceof String)) {
            return INVALID_PARAMETERS + SEP + param;
        }
        return null;
    }

    /**
     * Validate a string list query parameter.
     *
     * @param params
     *            the map of query parameters
     * @return an error message if validation fails, or null otherwise
     */
    private static String validateStringList(String param, Map<String, Object> params) {
        Object value = params.get(param);
        if (value == null) {
            return MISSING_PARAMETERS + SEP + param;
        }
        if (!(value instanceof List)) {
            return INVALID_PARAMETERS + SEP + param;
        }
        for (Object elem : (List<?>) value) {
            if (!(elem instanceof String)) {
                return INVALID_PARAMETERS + SEP + param;
            }
        }
        return null;
    }

    /**
     * Validate and convert the requested_timerange query parameter. For
     * backward compatibility, the requested_times query parameter is used as a
     * fall-back when requested_timerange is absent.
     *
     * @param params
     *            the mutable map of query parameters
     * @param required
     *            true if the parameter is required
     * @param isTree
     *            true if parameter requested_times is for a tree query
     * @return an error message if validation fails, or null otherwise
     */
    private static String validateRequestedTimeRange(Map<String, Object> params, boolean required, boolean isTree) {
        Object requestedTimeRange = params.get(REQUESTED_TIMERANGE_KEY);
        Object requestedTimes = params.get(DataProviderParameterUtils.REQUESTED_TIME_KEY);
        if (required && requestedTimeRange == null && requestedTimes == null) {
            return MISSING_PARAMETERS + SEP + REQUESTED_TIMERANGE_KEY;
        }
        if (requestedTimeRange != null) {
            /* Transform requested timerange to requested times array */
            requestedTimes = params.computeIfPresent(REQUESTED_TIMERANGE_KEY, (k, v) -> {
                if (v instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) v;
                    Object startObj = map.get(START);
                    Object endObj = map.get(END);
                    Object nbTimesObj = map.get(NBTIMES);
                    if (!(startObj instanceof Number && endObj instanceof Number)) {
                        return null;
                    }
                    long start = ((Number) startObj).longValue();
                    long end = ((Number) endObj).longValue();
                    if (!(nbTimesObj instanceof Number)) {
                        return Arrays.asList(start, end);
                    }
                    long nbTimes = Math.min(((Number) nbTimesObj).longValue(), MAX_NBTIMES);
                    if (nbTimes <= 1) {
                        return Arrays.asList(start, end);
                    }
                    long resolution = (end - start) / (nbTimes - 1);
                    return StateSystemUtils.getTimes(start, end, resolution);
                }
                return null;
            });
            if (requestedTimes == null) {
                return INVALID_PARAMETERS + SEP + REQUESTED_TIMERANGE_KEY;
            }
            params.put(DataProviderParameterUtils.REQUESTED_TIME_KEY, requestedTimes);
            params.remove(REQUESTED_TIMERANGE_KEY);
        } else if (requestedTimes != null) {
            List<@NonNull Long> timeRequested = DataProviderParameterUtils.extractTimeRequested(params);
            if ((timeRequested == null) || (isTree && timeRequested.size() == 1) || (!isTree && timeRequested.isEmpty())) {
                return INVALID_PARAMETERS + SEP + DataProviderParameterUtils.REQUESTED_TIME_KEY;
            }
            params.put(DataProviderParameterUtils.REQUESTED_TIME_KEY, timeRequested);
        }
        return null;
    }

    /**
     * Validate and convert the requested_times query parameter.
     *
     * @param params
     *            the mutable map of query parameters
     * @param required
     *            true if the parameter is required
     * @return true if validation was successful, false otherwise
     */
    private static String validateRequestedTimes(Map<String, Object> params, boolean required) {
        Object value = params.get(DataProviderParameterUtils.REQUESTED_TIME_KEY);
        if (required && value == null) {
            return MISSING_PARAMETERS + SEP + DataProviderParameterUtils.REQUESTED_TIME_KEY;
        }
        if (value != null) {
            List<@NonNull Long> timeRequested = DataProviderParameterUtils.extractTimeRequested(params);
            if (timeRequested == null || timeRequested.isEmpty()) {
                return INVALID_PARAMETERS + SEP + DataProviderParameterUtils.REQUESTED_TIME_KEY;
            }
            params.put(DataProviderParameterUtils.REQUESTED_TIME_KEY, value);
        }
        return null;
    }

    /**
     * Validate and convert the requested_items query parameter.
     *
     * @param params
     *            the mutable map of query parameters
     * @param required
     *            true if the parameter is required
     * @return an error message if validation fails, or null otherwise
     */
    private static String validateRequestedItems(Map<String, Object> params, boolean required) {
        Object value = params.get(DataProviderParameterUtils.REQUESTED_ITEMS_KEY);
        if (required && value == null) {
            return MISSING_PARAMETERS + SEP + DataProviderParameterUtils.REQUESTED_ITEMS_KEY;
        }
        if (value != null) {
            List<@NonNull Long> selectedItems = DataProviderParameterUtils.extractSelectedItems(params);
            if (selectedItems == null) {
                return INVALID_PARAMETERS + SEP + DataProviderParameterUtils.REQUESTED_ITEMS_KEY;
            }
            params.put(DataProviderParameterUtils.REQUESTED_ITEMS_KEY, value);
        }
        return null;
    }

    /**
     * Validate and convert the requested_element query parameter.
     *
     * @param params
     *            the mutable map of query parameters
     * @return an error message if validation fails, or null otherwise
     */
    private static String validateRequestedElement(Map<String, Object> params) {
        Object value = params.get(DataProviderParameterUtils.REQUESTED_ELEMENT_KEY);
        if (value == null) {
            return MISSING_PARAMETERS + SEP + DataProviderParameterUtils.REQUESTED_ELEMENT_KEY;
        }
        /* Replace default deserialized map with the correct element object */
        params.computeIfPresent(DataProviderParameterUtils.REQUESTED_ELEMENT_KEY, (k, v) -> {
            if (v instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) v;
                Object elementType = map.get(ELEMENT_TYPE);
                long time = ((Number) map.getOrDefault(TIME, 0L)).longValue();
                long duration = ((Number) map.getOrDefault(DURATION, 0L)).longValue();
                if (ElementType.STATE.equals(elementType)) {
                    return new TimeGraphState(time, duration, null, null);
                } else if (ElementType.ANNOTATION.equals(elementType)) {
                    long entryId = ((Number) map.getOrDefault(ENTRY_ID, -1L)).longValue();
                    return new Annotation(time, duration, entryId, AnnotationType.CHART, null, EMPTY_STYLE);
                } else if (ElementType.ARROW.equals(elementType)) {
                    long sourceId = ((Number) map.getOrDefault(ENTRY_ID, -1L)).longValue();
                    long destinationId = ((Number) map.getOrDefault(DESTINATION_ID, -1L)).longValue();
                    return new TimeGraphArrow(sourceId, destinationId, time, duration, EMPTY_STYLE);
                }
            }
            return null;
        });
        if (params.get(DataProviderParameterUtils.REQUESTED_ELEMENT_KEY) == null) {
            return INVALID_PARAMETERS + SEP + DataProviderParameterUtils.REQUESTED_ELEMENT_KEY;
        }
        return null;
    }
}
