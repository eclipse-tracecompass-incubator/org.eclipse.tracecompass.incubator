/**********************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
public interface ConfigurationQueryParameters {

    /**
     * @return The parameters.
     */
    @NonNull
    @Schema(required = true)
    ConfigurationParameters getParameters();

    /**
     * Configuration parameters as per current trace-server protocol.
     */
    interface ConfigurationParameters {
        /**
         * @return the name of the configuration
         */
        @Schema(required = true, description = "Unique name of the configuration. If omitted a unique name will be generated.")
        String getName();
        /**
         * @return the description of the configuration
         */
        @Schema(required = false, description = "Optional description of the configuration.")
        String getDescription();
        /**
         * @return the typeId of the configuration according to the {@link ConfigurationSourceType}
         */
        @Schema(required = false, description = "Optional typeId of the configuration according to the corresponding ConfigurationTypeDescriptor. Omit if it's part of the endpoint URI.")
        @JsonProperty("typeId")
        String getTypeId();
        /**
         * @return parameters map for custom parameters as defined in the corresponding {@link ConfigurationSourceType}
         */
        @Schema(required = true, description = "Parameters as specified in the schema or list of ConfigurationParameterDescriptor of the corresponding ConfigurationTypeDescriptor.")
        Map<String, Object> getParameters();
    }
}
