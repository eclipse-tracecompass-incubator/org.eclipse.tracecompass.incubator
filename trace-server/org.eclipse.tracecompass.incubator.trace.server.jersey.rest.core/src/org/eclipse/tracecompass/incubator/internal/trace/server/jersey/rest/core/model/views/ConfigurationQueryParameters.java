/*******************************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views;

import org.eclipse.jdt.annotation.NonNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import io.swagger.v3.oas.annotations.Hidden;

/**
 * Definition of a parameters object received by the server from a client for configurations.
 */
public class ConfigurationQueryParameters {
    private @NonNull JsonNode parameters;

    /**
     * Constructor for Jackson
     */
    @SuppressWarnings("null")
    public ConfigurationQueryParameters() {
        parameters = NullNode.instance;
    }

    /**
     * Constructor.
     *
     * @param parameters
     *            json object
     */
    @SuppressWarnings("null")
    public ConfigurationQueryParameters(JsonNode parameters) {
        this.parameters = parameters == null ? NullNode.instance : parameters;
    }

    /**
     * @return Map of parameters
     */
    @Hidden
    public @NonNull JsonNode getParameters() {
        return parameters;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString() {
        return "QueryParameters [parameters=" + parameters + ", json=" + parameters.toString() + "]";
    }
}
