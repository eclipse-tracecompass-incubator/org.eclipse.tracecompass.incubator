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
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Error response utility class
 *
 * @author Bernd Hufmann
 */
public class ErrorResponseUtil {

    private ErrorResponseUtil() {
    }

    /**
     * Create a new error response
     *
     * @param status
     *            the http status
     * @param title
     *            the short-human-readable description of the errors
     * @return the error response
     */
    public static Response newErrorResponse(Status status, String title) {
        return Response.status(status).entity(new ErrorResponseImpl(title)).build();
    }

    /**
     * Create a new error response
     *
     * @param status
     *            the http status
     * @param title
     *            the short-human-readable description of the errors
     * @param detail
     *            the human-readable explanation of the error helping clients to
     *            correct the error
     * @return the error response
     */
    public static Response newErrorResponse(Status status, String title, String detail) {
        return Response.status(status).entity(new ErrorResponseImpl(title, detail)).build();
    }

    /**
     * Create a new error response with a trace
     *
     * @param status
     *            the http status
     * @param title
     *            the short-human-readable description of the errors
     * @param detail
     *            the human-readable explanation of the error helping clients to
     *            correct the error
     * @param trace
     *            the trace this error corresponds to
     * @return the error response
     */
    public static Response newErrorResponse(Status status, String title, String detail, Trace trace) {
        return Response.status(status).entity(new TraceErrorResponseImpl(title, detail, trace)).build();
    }

    /**
     * Create a new error response with an experiment
     *
     * @param status
     *            the http status
     * @param title
     *            the short-human-readable description of the errors
     * @param detail
     *            the human-readable explanation of the error helping clients to
     *            correct the error
     * @param experiment
     *            the experiment this error corresponds to
     * @return the error response
     */
    public static Response newErrorResponse(Status status, String title, String detail, Experiment experiment) {
        return Response.status(status).entity(new ExperimentErrorResponseImpl(title, detail, experiment)).build();
    }
}
