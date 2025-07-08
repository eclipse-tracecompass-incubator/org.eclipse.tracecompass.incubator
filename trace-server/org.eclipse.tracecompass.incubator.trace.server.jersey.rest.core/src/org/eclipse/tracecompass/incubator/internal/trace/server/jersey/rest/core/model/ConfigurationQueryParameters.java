/**********************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AdditionalPropertiesValue;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
public interface ConfigurationQueryParameters {
    /**
     * @return the name of the configuration
     */
    @Schema(required = true, description = "Unique name of the configuration.")
    String getName();
    /**
     * @return the description of the configuration
     */
    @Schema(required = false, description = "Optional description of the configuration.")
    String getDescription();
    /**
     * @return parameters map for custom parameters as defined in the corresponding {@link ConfigurationSourceType}
     */
    @Schema(required = true, description = "Parameters as specified in the schema or list of ConfigurationParameterDescriptor of the corresponding ConfigurationTypeDescriptor."
            , additionalProperties = AdditionalPropertiesValue.TRUE)
    Object getParameters();
}
