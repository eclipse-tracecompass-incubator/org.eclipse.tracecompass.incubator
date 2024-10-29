/*******************************************************************************
 * Copyright (c) 2023, 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@NonNullByDefault
@Schema(description = "Configuration instance describing user provided configuration parameters.")
public interface Configuration {
    /**
     * @return the name of configuration instance
     */
    @Schema(description = "The human readable name")
    String getName();

    /**
     * @return the ID for of the configuration instance.
     */
    @Schema(description = "The unique ID of the configuration instance")
    String getId();

    /**
     * @return a short description of this configuration instance.
     */
    @Schema(description = "Describes the configuration instance")
    String getDescription();

    /**
     * @return the configuration source type
     */
    @Schema(description = "The ID of the configuration source type")
    String getSourceTypeId();

    /**
     * @return parameters to return. Can be used to show
     *         more details to users of the configuration instance
     */
    @Schema(description = "Optional parameters representing the configuration parameters used to create this configuration.")
    Map<String, Object> getParameters();
}
