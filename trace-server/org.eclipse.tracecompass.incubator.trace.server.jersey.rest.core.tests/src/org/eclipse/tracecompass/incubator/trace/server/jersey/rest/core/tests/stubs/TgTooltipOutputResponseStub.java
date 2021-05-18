/**********************************************************************
 * Copyright (c) 2021 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A stub class for the response to a time graph tooltip's request. It contains
 * the generic response, as well as a {@link Map} model
 *
 * @author Patrick Tasse
 */
public class TgTooltipOutputResponseStub extends OutputResponseStub {

    private static final long serialVersionUID = -7459131077423626123L;
    private final @NonNull Map<@NonNull String, @NonNull String> fModel;

    /**
     * {@link JsonCreator} Constructor from json
     *
     * @param model
     *            The model for this response
     * @param status
     *            The status of the response
     * @param statusMessage
     *            The custom status message of the response
     */
    public TgTooltipOutputResponseStub(@JsonProperty("model") @NonNull Map<@NonNull String, @NonNull String> model,
            @JsonProperty("status") String status,
            @JsonProperty("statusMessage") String statusMessage) {
        super(status, statusMessage);
        fModel = model;
    }

    /**
     * Get the model for this response
     *
     * @return The model for the response
     */
    public @NonNull Map<@NonNull String, @NonNull String> getModel() {
        return fModel;
    }

}
