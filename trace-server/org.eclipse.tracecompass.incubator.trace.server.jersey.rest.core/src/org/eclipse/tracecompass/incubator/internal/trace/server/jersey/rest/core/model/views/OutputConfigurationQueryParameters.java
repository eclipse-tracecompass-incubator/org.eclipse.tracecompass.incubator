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

import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;

/**
 * Definition of a parameters object received by the server from a client for configurations.
 */
public class OutputConfigurationQueryParameters extends ConfigurationQueryParameters {
    private @NonNull String typeId;

    /**
     * Constructor for Jackson
     */
    public OutputConfigurationQueryParameters() {
        // Default constructor for Jackson
        super();
        this.typeId = TmfConfiguration.UNKNOWN;
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
    public OutputConfigurationQueryParameters(String name, String description, String typeId, Map<String, Object> parameters) {
        super(name, description, parameters);
        this.typeId = typeId == null ? TmfConfiguration.UNKNOWN : typeId;
    }

    /**
     * @return the type ID of configuration or {@link TmfConfiguration#UNKNOWN} if not provided
     */
    @NonNull public String getTypeId() {
        return typeId;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString() {
        return "OutputConfigurationQueryParameters [name=" + getName() + ", description=" + getDescription()
           +", typeId=" + getTypeId() + ", parameters=" + getParameters() + "]";
    }
}
