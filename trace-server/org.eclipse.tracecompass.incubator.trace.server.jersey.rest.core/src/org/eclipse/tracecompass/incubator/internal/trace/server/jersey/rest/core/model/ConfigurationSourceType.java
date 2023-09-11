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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@NonNullByDefault
public interface ConfigurationSourceType {

    /**
     * @return the name of the configuration source type
     */
    @Schema(description = "The human readable name")
    String getName();

    /**
     * @return the ID for of the configuration source type
     */
    @Schema(description = "The unique ID of the configuration source type")
    String getId();

    /**
     * @return a short description of this configuration source type
     */
    @Schema(description = "Describes the configuration source type")
    String getDescription();

    /**
     * @returns a list of query parameter keys to be passed when creating
     *          configuration instance of this type. Use 'path' key for file
     *          URIs.
     */
    @Schema(description = "A list of query parameter keys to be passed when creating configuration instance of this type. Use 'path' key for file URIs")
    List<String> getQueryParameterKeys();
}
