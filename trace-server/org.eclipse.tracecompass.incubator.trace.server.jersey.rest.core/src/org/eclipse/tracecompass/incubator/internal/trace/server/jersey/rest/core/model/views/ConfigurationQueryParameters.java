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
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;

/**
 * Definition of a parameters object received by the server from a client for configurations.
 */
public class ConfigurationQueryParameters {
    private @NonNull String name;
    private @NonNull String description;
    private @NonNull Map<String, Object> parameters;

    /**
     * Constructor for Jackson
     */
    public ConfigurationQueryParameters() {

        // Default constructor for Jackson
        this.parameters = new HashMap<>();
        this.name = TmfConfiguration.UNKNOWN;
        this.description = TmfConfiguration.UNKNOWN;
    }

    /**
     * Constructor.
     *
     * @param name
     *            the name of the configuration
     * @param description
     *            the description of the configuration
     * @param typeId
     *            the typeId of the configuration
     *
     * @param parameters
     *            Map of parameters
     */
    public ConfigurationQueryParameters(String name, String description, Map<String, Object> parameters) {
        this.parameters = parameters != null ? parameters : new HashMap<>();
        this.name = name == null ? TmfConfiguration.UNKNOWN : name;
        this.description = description == null ? TmfConfiguration.UNKNOWN : description;
    }

    /**
     * @return the name of configuration or {@link TmfConfiguration#UNKNOWN} if not provided
     */
    @NonNull public String getName() {
        return name;
    }

    /**
     * @return the description of configuration or {@link TmfConfiguration#UNKNOWN} if not provided
     */
    @NonNull public String getDescription() {
        return description;
    }

    /**
     * @return Map of parameters or empty map if not provided
     */
    @NonNull public Map<String, Object> getParameters() {
        return parameters;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString() {
        return "ConfigurationQueryParameters [name=" + getName() + ", description=" + getDescription() + ", parameters=" + getParameters() + "]";
    }
}
