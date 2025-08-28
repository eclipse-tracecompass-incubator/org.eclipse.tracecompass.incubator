/*******************************************************************************
 * Copyright (c) 2023, 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@NonNullByDefault
public interface ConfigurationParameterDescriptor {

    /**
     * @return the name of the configuration parameter
     */
    @Schema(description = "The unique key name of the configuration parameter", requiredMode = RequiredMode.REQUIRED)
    String getKeyName();

    /**
     * @return data type hint of the configuration parameter
     */
    @Schema(description = "Optional data type hint of the configuration parameter. For example, use NUMBER for numbers, or STRING as strings. If omitted assume the default value is STRING.")
    String getDataType();

    /**
     * @return a short description of this configuration parameter
     */
    @Schema(description = "Optional, describes the configuration parameter")
    String getDescription();

    /**
     * @return true if parameter is required else false.
     */
    @Schema(description = "Optional flag indicating whether the configuration parameter is required or not. If ommitted the default value is false.")
    Boolean isRequired();
}
