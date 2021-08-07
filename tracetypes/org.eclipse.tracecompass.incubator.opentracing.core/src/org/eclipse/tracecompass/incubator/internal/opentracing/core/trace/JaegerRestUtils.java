/*******************************************************************************
 * Copyright (c) 2018, 2021 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.core.trace;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.eclipse.tracecompass.incubator.internal.opentracing.core.event.IOpenTracingConstants;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Jaeger REST utility class
 *
 * @author Simon Delisle
 */
public class JaegerRestUtils {

    private static final String SERVICES_ENDPOINT = "services"; //$NON-NLS-1$
    private static final String SERVICES_DATA_KEY = "data"; //$NON-NLS-1$
    private static final String TRACES_ENDPOINT = "traces"; //$NON-NLS-1$
    private static final String NANOSECONDS_PADDING = "000"; //$NON-NLS-1$
    private static final long HOUR_TO_SEC = 3600L;

    /*
     * Default and non-default query values
     */
    private static final String DEFAULT_LIMIT = "20"; //$NON-NLS-1$
    private static final String DEFAULT_SERVICE = "jaeger-query"; //$NON-NLS-1$
    private static final String DEFAULT_BASE_URL = "http://localhost:16686/api"; //$NON-NLS-1$
    private static final String DEFAULT_LOOKBACK = "1h"; //$NON-NLS-1$
    private static final String LOOKBACK_1_HOUR = "1h"; //$NON-NLS-1$
    private static final String LOOKBACK_2_HOUR = "2h"; //$NON-NLS-1$
    private static final String LOOKBACK_3_HOUR = "3h"; //$NON-NLS-1$
    private static final String LOOKBACK_6_HOUR = "6h"; //$NON-NLS-1$
    private static final String LOOKBACK_12_HOUR = "12h"; //$NON-NLS-1$


    /**
     * Parameters key for traces request
     */
    private static final String SEARCH_END_TIME = "end"; //$NON-NLS-1$
    private static final String NB_TRACES_LIMIT = "limit"; //$NON-NLS-1$
    private static final String LOOKBACK = "lookback"; //$NON-NLS-1$
    private static final String MAX_DURATION = "maxDuration"; //$NON-NLS-1$
    private static final String MIN_DURATION = "minDuration"; //$NON-NLS-1$
    private static final String SERVICE_NAME = "service"; //$NON-NLS-1$
    private static final String SEARCH_START_TIME = "start"; //$NON-NLS-1$
    private static final String TAGS = "tags"; //$NON-NLS-1$

    private JaegerRestUtils() {
    }

    /**
     * Fetch the available services from Jaeger, used to create the URL to fetch
     * traces
     *
     * @param baseUrl
     *            Base URL of Jaeger API
     * @return Array of service names
     */
    public static String[] fetchServices(String baseUrl) {
        URI uri = UriBuilder.fromUri(baseUrl).path(SERVICES_ENDPOINT).build();
        String response = jaegerGet(uri.toString());
        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
        JsonArray servicesArray = jsonResponse.get(SERVICES_DATA_KEY).getAsJsonArray();
        String[] services = new String[servicesArray.size()];
        for (int i = 0; i < servicesArray.size(); i++) {
            services[i] = servicesArray.get(i).getAsString();
        }
        return services;
    }

    /**
     * Build the correct URL with the given parameters that will be use to fetch
     * traces
     *
     * @param baseUrl
     *            Base URL of Jaeger API
     * @param service
     *            Service name
     * @param startTime
     *            Search start time
     * @param endTime
     *            Search end time
     * @param limit
     *            Limit on the number of traces returned
     * @param lookback
     *            Amount of time to go look back for traces
     * @param maxDuration
     *            Trace maximum duration
     * @param minDuration
     *            Trace minimum duration
     * @param tags
     *            Span tags filter
     * @return The built URL
     */
    public static String buildTracesUrl(String baseUrl, String endTime, String limit, String lookback, String maxDuration, String minDuration, String service, String startTime, String tags) {
        UriBuilder uriBuilder = UriBuilder.fromUri(baseUrl).path(TRACES_ENDPOINT)
                .queryParam(SEARCH_END_TIME, endTime)
                .queryParam(NB_TRACES_LIMIT, limit)
                .queryParam(LOOKBACK, lookback)
                .queryParam(SERVICE_NAME, service)
                .queryParam(SEARCH_START_TIME, startTime);
        if (!maxDuration.isEmpty()) {
            uriBuilder.queryParam(MAX_DURATION, maxDuration);
        }
        if (!minDuration.isEmpty()) {
            uriBuilder.queryParam(MIN_DURATION, minDuration);
        }
        if (!tags.isEmpty()) {
            try {
                uriBuilder.queryParam(TAGS, URLEncoder.encode(tags, StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException e) {
                // We don't add the tags if the encoding fails
            }
        }

        return uriBuilder.build().toString();
    }

    /**
     * Fetch the traces from Jaeger
     *
     * @param url
     *            The complete URL (built from buildTracesUrl)
     * @return A JSon string that contains all the traces or null if there is a
     *         problem with the connection
     */
    public static String fetchJaegerTraces(String url) {
        return jaegerGet(url);
    }

    private static String jaegerGet(String url) {
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target(url);
        Builder request = resource.request();
        request.accept(MediaType.APPLICATION_JSON);
        try {
            return request.get(String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Verify the connection with the API URL
     *
     * @param url
     *            URL of Jaeger API
     * @return True if the connection can be establish
     */
    public static boolean jaegerCheckConnection(String url) {
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target(url);
        Builder request = resource.request();
        try {
            int status = request.get().getStatus();
            return Response.Status.fromStatusCode(status) == Response.Status.OK;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Fetch the traces from Jaeger
     *
     * @param parameters
     *            Map of parameters used when querying the Jaeger Query Service
     *            via HTTP JSON API
     * @return A JSON string that contains all the traces or null if there is a
     *         problem with the connection
     */
    public static String fetchJaegerTraces(Map<String, Object> parameters) {
        Object targetUrlObject = parameters.get("targetUrl"); //$NON-NLS-1$
        String targetUrlText = targetUrlObject != null ? (String) targetUrlObject : DEFAULT_BASE_URL;
        Object serviceObject = parameters.get(SERVICE_NAME);
        String service = serviceObject != null ? (String) serviceObject : DEFAULT_SERVICE;
        Object tagsObject = parameters.get(TAGS);
        String tags = tagsObject != null ? (String) tagsObject : ""; //$NON-NLS-1$
        Object limitResultsObject = parameters.get(NB_TRACES_LIMIT);
        String limitResults = limitResultsObject != null ? (String) limitResultsObject : DEFAULT_LIMIT;
        Object lookBackObject = parameters.get(LOOKBACK);
        String lookBackText = lookBackObject != null ? (String) lookBackObject : DEFAULT_LOOKBACK;
        long lookBack = HOUR_TO_SEC;
        switch (lookBackText) {
        case LOOKBACK_1_HOUR:
            lookBack = HOUR_TO_SEC;
            break;
        case LOOKBACK_2_HOUR:
            lookBack = HOUR_TO_SEC * 2;
            break;
        case LOOKBACK_3_HOUR:
            lookBack = HOUR_TO_SEC * 3;
            break;
        case LOOKBACK_6_HOUR:
            lookBack = HOUR_TO_SEC * 6;
            break;
        case LOOKBACK_12_HOUR:
            lookBack = HOUR_TO_SEC * 12;
            break;
        default:
            lookBack = HOUR_TO_SEC;
            break;
        }
        Object maxDurationObject = parameters.get(MAX_DURATION);
        String targetMaxDurationText = maxDurationObject != null ? (String) maxDurationObject : ""; //$NON-NLS-1$
        Object minDurationObject = parameters.get(MIN_DURATION);
        String targetMinDurationText = minDurationObject != null ? (String) minDurationObject : ""; //$NON-NLS-1$

        long startTime = Instant.now().minusSeconds(lookBack).toEpochMilli();
        long endTime = Instant.now().toEpochMilli();

        String requestUrl = JaegerRestUtils.buildTracesUrl(targetUrlText, Long.toString(endTime) + NANOSECONDS_PADDING, limitResults, lookBackText,
                targetMaxDurationText, targetMinDurationText, service, Long.toString(startTime) + NANOSECONDS_PADDING, tags);
        return JaegerRestUtils.fetchJaegerTraces(requestUrl);
    }

    /**
     * Get a list of traceIDs from a JSON Object containing all traces
     *
     * @param tracesObject
     *            a {@link JsonObject} hat contains all the traces
     * @return a list of traceIDs identified in the tracesObject. If the list is
     *         empty, no traceIDs have been found
     */
    public static List<String> getTraceIDs(JsonObject tracesObject) {
        JsonArray tracesArray = tracesObject.get(SERVICES_DATA_KEY).getAsJsonArray();
        // Get all traceIDs and put in a list
        List<String> traceIDs = new ArrayList<>();
        if (tracesArray.size() > 0) {
            for (int i = 0; i < tracesArray.size(); i++) {
                JsonObject trace = tracesArray.get(i).getAsJsonObject();
                traceIDs.add(trace.get(IOpenTracingConstants.TRACE_ID).getAsString());
            }
        }
        return traceIDs;
    }
}
