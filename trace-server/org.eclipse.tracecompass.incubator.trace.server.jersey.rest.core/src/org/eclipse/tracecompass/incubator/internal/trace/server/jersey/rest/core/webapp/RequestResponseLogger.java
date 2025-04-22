/*******************************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.webapp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.traceeventlogger.LogUtils;

/**
 * A filter that logs information about incoming HTTP requests and their
 * responses. This filter provides consolidated structured logs for each request
 * and response, including method, path, client IP, query parameters, headers,
 * body content, status code, and duration.
 *
 * @author Kaveh Shahedi
 */
@Provider
public class RequestResponseLogger implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = TraceCompassLog.getLogger(RequestResponseLogger.class);

    private static final String REQUEST_ID_PROPERTY = "request-id"; //$NON-NLS-1$
    private static final String START_TIME_PROPERTY = "request-start-time"; //$NON-NLS-1$
    private static final String CLIENT_IP_PROPERTY = "client-ip"; //$NON-NLS-1$
    private static final String REQUEST_BODY_PROPERTY = "request-body"; //$NON-NLS-1$
    private static final String METHOD_PROPERTY = "http-method"; //$NON-NLS-1$
    private static final String PATH_PROPERTY = "http-path"; //$NON-NLS-1$

    private static final AtomicLong REQUEST_COUNTER = new AtomicLong(0);

    private static final int MAX_BODY_LOG_LENGTH = 1000;

    // Constants for logging
    private static final String HTTP_RESPONSE_KEY = "HTTP Response"; //$NON-NLS-1$
    private static final String HTTP_REQUEST_KEY = "HTTP Request"; //$NON-NLS-1$
    private static final String REQUEST_ID_KEY = "requestId"; //$NON-NLS-1$
    private static final String METHOD_KEY = "method"; //$NON-NLS-1$
    private static final String PATH_KEY = "path"; //$NON-NLS-1$
    private static final String CLIENT_IP_KEY = "clientIp"; //$NON-NLS-1$
    private static final String QUERY_PARAMS_KEY = "queryParams"; //$NON-NLS-1$
    private static final String HEADERS_KEY = "headers"; //$NON-NLS-1$
    private static final String STATUS_KEY = "status"; //$NON-NLS-1$
    private static final String BODY_KEY = "body"; //$NON-NLS-1$
    private static final String CONTENT_LENGTH_KEY = "contentLength"; //$NON-NLS-1$
    private static final String CONTENT_TYPE_KEY = "contentType"; //$NON-NLS-1$
    private static final String DURATION_KEY = "duration"; //$NON-NLS-1$

    @Context
    private HttpServletRequest httpRequest;

    /**
     * Filter of incoming requests. This method is called before the request is
     * processed by the resource method. It logs the request details such as
     * method, path, client IP, query parameters, headers, and body content.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!LOGGER.isLoggable(Level.INFO)) {
            return;
        }

        long requestTimestamp = System.currentTimeMillis();
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();
        String clientIp = getClientIpAddress(requestContext);
        MultivaluedMap<String, String> queryParams = requestContext.getUriInfo().getQueryParameters();

        // Generate a unique request ID
        String requestId = generateRequestId(requestTimestamp, method, path, clientIp);

        // Store properties for response filter
        requestContext.setProperty(REQUEST_ID_PROPERTY, requestId);
        requestContext.setProperty(CLIENT_IP_PROPERTY, clientIp);
        requestContext.setProperty(METHOD_PROPERTY, method);
        requestContext.setProperty(PATH_PROPERTY, path);
        requestContext.setProperty(START_TIME_PROPERTY, requestTimestamp);

        // Capture body content
        String bodyContent = ""; //$NON-NLS-1$
        if (method.matches("POST|PUT|PATCH|DELETE") && requestContext.hasEntity()) { //$NON-NLS-1$
            List<String> contentTypeHeader = requestContext.getHeaders().get(HttpHeaders.CONTENT_TYPE);
            String contentType = contentTypeHeader != null && !contentTypeHeader.isEmpty() ? contentTypeHeader.get(0) : ""; //$NON-NLS-1$

            if (contentType.matches(".*(application/json|application/x-www-form-urlencoded|text/).*")) { //$NON-NLS-1$
                bodyContent = captureRequestBody(requestContext);
                bodyContent = (bodyContent.length() > MAX_BODY_LOG_LENGTH ? bodyContent.substring(0, MAX_BODY_LOG_LENGTH) + "..." : bodyContent); //$NON-NLS-1$
                requestContext.setProperty(REQUEST_BODY_PROPERTY, bodyContent);
            }
        }

        String relevantHeaders = getRelevantRequestHeaders(requestContext);

        LogUtils.traceInstant(
                LOGGER,
                Level.INFO,
                HTTP_REQUEST_KEY,
                REQUEST_ID_KEY, requestId,
                METHOD_KEY, method,
                PATH_KEY, path,
                CLIENT_IP_KEY, clientIp,
                QUERY_PARAMS_KEY, queryParams.isEmpty() ? null : queryParams,
                HEADERS_KEY, relevantHeaders.isEmpty() ? null : relevantHeaders,
                BODY_KEY, bodyContent.isEmpty() ? null : bodyContent);
    }

    /**
     * Filter of outgoing responses. This method is called after the resource
     * method has processed the request. It logs the response details such as
     * status code, duration, content type, and content length.
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (LOGGER.isLoggable(Level.INFO)) {
            // Retrieve stored request properties
            String requestId = (String) requestContext.getProperty(REQUEST_ID_PROPERTY);
            String method = (String) requestContext.getProperty(METHOD_PROPERTY);
            String path = (String) requestContext.getProperty(PATH_PROPERTY);
            String clientIp = (String) requestContext.getProperty(CLIENT_IP_PROPERTY);
            int status = responseContext.getStatus();

            Long startTime = (Long) requestContext.getProperty(START_TIME_PROPERTY);
            long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;

            // Get response content type and size
            Object contentTypeObj = responseContext.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
            String contentType = contentTypeObj != null ? contentTypeObj.toString() : null;

            // Get content length
            Object contentLengthObj = responseContext.getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH);
            Long contentLength = contentLengthObj != null ? Long.parseLong(contentLengthObj.toString()) : null;

            LogUtils.traceInstant(
                    LOGGER,
                    Level.INFO,
                    HTTP_RESPONSE_KEY,
                    REQUEST_ID_KEY, requestId,
                    METHOD_KEY, method,
                    PATH_KEY, path,
                    CLIENT_IP_KEY, clientIp,
                    STATUS_KEY, status,
                    DURATION_KEY, duration,
                    CONTENT_TYPE_KEY, contentType,
                    CONTENT_LENGTH_KEY, contentLength);
        }
    }

    /**
     * Extract the client IP address from the request context.
     *
     * @param requestContext
     *            The container request context
     * @return The client IP address
     */
    private String getClientIpAddress(ContainerRequestContext requestContext) {
        String clientIp = null;

        String header = httpRequest.getHeader("X-Forwarded-For"); //$NON-NLS-1$
        if (header != null && !header.isEmpty()) {
            clientIp = header.split(",")[0].trim(); //$NON-NLS-1$
        }

        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = httpRequest.getHeader("X-Real-IP"); //$NON-NLS-1$ "
        }

        if (clientIp == null || clientIp.isEmpty()) {
            String forwarded = httpRequest.getHeader("Forwarded"); //$NON-NLS-1$
            if (forwarded != null && forwarded.contains("for=")) { //$NON-NLS-1$
                int start = forwarded.indexOf("for=") + 4; //$NON-NLS-1$
                int end = forwarded.indexOf(";", start); //$NON-NLS-1$
                clientIp = (end > 0 ? forwarded.substring(start, end) : forwarded.substring(start)).replace("\"", "").trim(); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = httpRequest.getRemoteAddr();
        }

        if (clientIp == null || clientIp.isEmpty()) {
            return "unknown"; //$NON-NLS-1$
        }

        if (clientIp.startsWith("[") && clientIp.endsWith("]")) { //$NON-NLS-1$ //$NON-NLS-2$
            clientIp = clientIp.substring(1, clientIp.length() - 1);
        }

        return clientIp;
    }

    /**
     * Capture the request body content
     *
     * @param requestContext
     *            The container request context
     * @return The body content as a string
     * @throws IOException
     *             If an I/O error occurs
     */
    private static String captureRequestBody(ContainerRequestContext requestContext) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            InputStream entityStream = requestContext.getEntityStream();

            byte[] buffer = new byte[1024];
            int len;
            while ((len = entityStream.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }

            byte[] bytes = baos.toByteArray();
            requestContext.setEntityStream(new ByteArrayInputStream(bytes));
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    /**
     * Extract relevant headers from the request.
     *
     * @param requestContext
     *            The container request context
     * @return A string containing important headers
     */
    private static String getRelevantRequestHeaders(ContainerRequestContext requestContext) {
        MultivaluedMap<String, String> headers = requestContext.getHeaders();
        return headers.entrySet().stream()
                .filter(entry -> {
                    String headerName = entry.getKey().toLowerCase();
                    return headerName.startsWith("accept") || //$NON-NLS-1$
                            headerName.startsWith("content-") || //$NON-NLS-1$
                            headerName.equals("user-agent") || //$NON-NLS-1$
                            headerName.equals("host"); //$NON-NLS-1$
                })
                .map(entry -> entry.getKey() + "=" + String.join(",", entry.getValue())) //$NON-NLS-1$ //$NON-NLS-2$
                .collect(Collectors.joining(", ")); //$NON-NLS-1$
    }

    /**
     * Generate a unique request ID based on the request timestamp, method,
     * path, client IP, and a counter.
     *
     * @param timestamp
     *            The request timestamp
     * @param method
     *            The HTTP method
     * @param path
     *            The request path
     * @param clientIp
     *            The client IP address
     * @return A unique request ID
     */
    @SuppressWarnings("null")
    private static String generateRequestId(long timestamp, String method, String path, String clientIp) {
        String uniqueId = String.format("%d-%s-%s-%s-%d", timestamp, method, path, clientIp, REQUEST_COUNTER.incrementAndGet()); //$NON-NLS-1$
        UUID uuid = UUID.nameUUIDFromBytes(uniqueId.getBytes(StandardCharsets.UTF_8));
        return uuid.toString();
    }
}