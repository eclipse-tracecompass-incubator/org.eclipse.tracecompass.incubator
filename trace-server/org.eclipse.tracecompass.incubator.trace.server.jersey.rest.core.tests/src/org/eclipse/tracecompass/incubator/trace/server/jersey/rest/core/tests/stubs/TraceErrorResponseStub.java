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
package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Error response class
 *
 * @author Bernd Hufmann
 */
public class TraceErrorResponseStub implements Serializable {

    private static final long serialVersionUID = -5823094821729001182L;

    private final String fTitle;
    private final String fDetail;
    private final TraceModelStub fTrace;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param title
     *            The error title
     * @param detail
     *            The error detail
     * @param trace
     *            The Trace
     */
    @JsonCreator
    public TraceErrorResponseStub(
            @JsonProperty("title") String title,
            @JsonProperty("detail") String detail,
            @JsonProperty("trace") TraceModelStub trace) {
        fTitle = title;
        fDetail = detail;
        fTrace = trace;
    }

    /**
     * Get the error title
     *
     * @return the error title
     */
    public String getTitle() {
        return fTitle;
    }

    /**
     * Get the error detail
     *
     * @return the error detail
     */
    public String getDetail() {
        return fDetail;
    }

    /**
     * Get the trace
     *
     * @return the trace
     */
    public TraceModelStub getTrace() {
        return fTrace;
    }
}
