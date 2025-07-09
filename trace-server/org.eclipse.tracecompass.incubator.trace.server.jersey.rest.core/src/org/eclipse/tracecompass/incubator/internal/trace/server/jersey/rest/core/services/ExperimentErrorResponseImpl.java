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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Error response class with experiment
 *
 * @author Bernd Hufmann
 */
public class ExperimentErrorResponseImpl extends ErrorResponseImpl {

    private static final long serialVersionUID = -733846016160687714L;
    private final Experiment fExperiment;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param title
     *            The short, human-readable description of the error
     * @param detail
     *            The human-readable explanation of the error with details helping the client to correct the error
     * @param experiment
     *            The experiment this error corresponds to
     */
    @JsonCreator
    public ExperimentErrorResponseImpl(
            @JsonProperty("title") String title,
            @JsonProperty("detail") String detail,
            @JsonProperty("experiment") Experiment experiment) {
        super(title, detail);
        fExperiment = experiment;
    }

    /**
     * The trace this error corresponds to
     *
     * @return The trace this error corresponds to
     */
    @JsonProperty("experiment")
    public Experiment getExperiment() {
        return fExperiment;
    }
}
