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

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Error response class
 *
 * @author Bernd Hufmann
 */
public class ErrorResponseImpl implements Serializable {

    private static final long serialVersionUID = -5823094821729001182L;

    private final String fTitle;
    private final String fDetail;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param title
     *            The short, human-readable description of the error
     * @param detail
     *            The human-readable explanation of the error with details helping the client to correct the error
     */
    @JsonCreator
    public ErrorResponseImpl(
            @JsonProperty("title") String title,
            @JsonProperty("detail") String detail) {
        fTitle = title;
        fDetail = detail;
    }

    /**
     * Constructor with error title only
     *
     * @param title
     *            The short, human-readable description of the error
     */
    public ErrorResponseImpl(String title) {
        this(title, null);
    }

    /**
     * Get the short, human-readable description of the error
     *
     * @return the error title
     */
    @JsonProperty("title")
    public String getTitle() {
        return fTitle;
    }

    /**
     * Get human-readable explanation of the error with details helping the client to correct the error
     *
     * @return the error detail
     */
    @JsonProperty("detail")
    public String getDetail() {
        return fDetail;
    }
}
