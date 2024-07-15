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

import org.eclipse.jdt.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Definition of a parameters object received by the server from a client for configurations to create
 * data providers.
 */
public class OutputConfigurationQueryParameters extends ConfigurationQueryParameters {

    private @Nullable String typeId;

    /**
     * Constructor for Jackson
     */
    public OutputConfigurationQueryParameters() {
        super();
    }

    /**
     * Constructor.
     *
     * @param parameters
     *            json object
     * @param typeId
     *            the configuration source type ID
     */
    public OutputConfigurationQueryParameters(JsonNode parameters, String typeId) {
        super(parameters);
        this.typeId = typeId;
    }

    /**
     * Gets the configuration source type ID.
     * @return the configuration source type ID
     */
    public String getTypeId() {
        return this.typeId;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString() {
        return "OutputConfigurationQueryParameters [parameters=" + super.getParameters().toString() + ", typeId=" + typeId + "]";
    }
}
