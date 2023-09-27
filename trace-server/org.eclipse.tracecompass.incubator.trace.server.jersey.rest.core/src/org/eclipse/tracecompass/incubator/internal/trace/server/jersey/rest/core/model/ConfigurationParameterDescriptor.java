/*******************************************************************************
 * Copyright (c) 2023 Ericsson
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

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@NonNullByDefault
public interface ConfigurationParameterDescriptor {

    /**
     * @return the name of the configuration parameter
     */
    @Schema(description = "The unique key name of the configuration parameter")
    String getKeyName();

    /**
     * @return the ID for of the configuration parameter
     */
    @Schema(description = "The data type hint of the configuration parameter")
    String getDataType();

    /**
     * @return a short description of this configuration parameter
     */
    @Schema(description = "Describes the configuration parameter")
    String getDescription();

    /**
     * @return true if parameter is required else false.
     */
    @Schema(description = "A flag indicating whether the configuration parameter is required or not")
    Boolean isRequired();
}
