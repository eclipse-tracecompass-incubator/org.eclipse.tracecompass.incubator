/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.ui.project.wizards;

/**
 * Jaeger REST utility class
 *
 * @author Simon Delisle
 */
public class JaegerRestUtils {

//    private static final String SERVICES_ENDPOINT = "services"; //$NON-NLS-1$
//    private static final String TRACES_ENDPOINT = "traces"; //$NON-NLS-1$
//
//    private static final String SERVICES_DATA_KEY = "data"; //$NON-NLS-1$
//
//    /**
//     * Parameters key for traces request
//     */
//    private static final String SEARCH_END_TIME = "end"; //$NON-NLS-1$
//    private static final String NB_TRACES_LIMIT = "limit"; //$NON-NLS-1$
//    private static final String LOOKBACK = "lookback"; //$NON-NLS-1$
//    private static final String MAX_DURATION = "maxDuration"; //$NON-NLS-1$
//    private static final String MIN_DURATION = "minDuration"; //$NON-NLS-1$
//    private static final String SERVICE_NAME = "service"; //$NON-NLS-1$
//    private static final String SEARCH_START_TIME = "start"; //$NON-NLS-1$
//    private static final String TAGS = "tags"; //$NON-NLS-1$

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
        return new String[0];
//        URI uri = UriBuilder.fromUri(baseUrl).path(SERVICES_ENDPOINT).build();
//        String response = jaegerGet(uri.toString());
//        Gson gson = new Gson();
//        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
//        JsonArray servicesArray = jsonResponse.get(SERVICES_DATA_KEY).getAsJsonArray();
//        String[] services = new String[servicesArray.size()];
//        for (int i = 0; i < servicesArray.size(); i++) {
//            services[i] = servicesArray.get(i).getAsString();
//        }
//        return services;
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
        return "";
//        UriBuilder uriBuilder = UriBuilder.fromUri(baseUrl).path(TRACES_ENDPOINT)
//                .queryParam(SEARCH_END_TIME, endTime)
//                .queryParam(NB_TRACES_LIMIT, limit)
//                .queryParam(LOOKBACK, lookback)
//                .queryParam(SERVICE_NAME, service)
//                .queryParam(SEARCH_START_TIME, startTime);
//        if (!maxDuration.isEmpty()) {
//            uriBuilder.queryParam(MAX_DURATION, maxDuration);
//        }
//        if (!minDuration.isEmpty()) {
//            uriBuilder.queryParam(MIN_DURATION, minDuration);
//        }
//        if (!tags.isEmpty()) {
//            try {
//                uriBuilder.queryParam(TAGS, URLEncoder.encode(tags, StandardCharsets.UTF_8.name()));
//            } catch (UnsupportedEncodingException e) {
//                // We don't add the tags if the encoding fails
//            }
//        }
//
//        return uriBuilder.build().toString();
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

    /**
     * @param url
     */
    private static String jaegerGet(String url) {
        return null;
//        Client client = ClientBuilder.newClient();
//        WebTarget resource = client.target(url);
//        Builder request = resource.request();
//        request.accept(MediaType.APPLICATION_JSON);
//        try {
//            return request.get(String.class);
//        } catch (Exception e) {
//            return null;
//        }
    }

    /**
     * Verify the connection with the API URL
     *
     * @param url
     *            URL of Jaeger API
     * @return True if the connection can be establish
     */
    public static boolean jaegerCheckConnection(String url) {
        return false;
//        Client client = ClientBuilder.newClient();
//        WebTarget resource = client.target(url);
//        Builder request = resource.request();
//        try {
//            int status = request.get().getStatus();
//            return Response.Status.fromStatusCode(status) == Response.Status.OK;
//        } catch (Exception e) {
//            return false;
//        }
    }
}
