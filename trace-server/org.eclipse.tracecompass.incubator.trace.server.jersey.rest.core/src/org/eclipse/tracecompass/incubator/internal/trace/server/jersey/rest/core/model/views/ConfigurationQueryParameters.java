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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

import io.swagger.v3.oas.annotations.Hidden;

/**
 * Definition of a parameters object received by the server from a client for configurations.
 */
public class ConfigurationQueryParameters {
    private @NonNull Map<String, Object> parameters;

    /**
     * Constructor for Jackson
     */
    public ConfigurationQueryParameters() {
        // Default constructor for Jackson
        this.parameters = new HashMap<>();
    }

    /**
     * Constructor.
     *
     * @param parameters
     *            Map of parameters
     */
    public ConfigurationQueryParameters(Map<String, Object> parameters) {
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }

    /**
     * @return Map of parameters
     */
    @Hidden
    public @NonNull Map<String, Object> getParameters() {
        return parameters;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString() {
        return "ConfigurationQueryParameters [parameters=" + parameters + "]";
    }
}
