/*******************************************************************************
 * Copyright (c) 2021 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
public interface DataProvider {

    /**
     * The provider types.
     */
    enum ProviderType {
        TABLE, TREE_TIME_XY, TIME_GRAPH, DATA_TREE, NONE
    }

    /**
     * @return The name.
     */
    @Schema(description = "The human readable name")
    String getName();

    /**
     * @return The ID.
     */
    @Schema(description = "The output provider's ID")
    String getId();

    /**
     * @return The type.
     */
    @Schema(description = "Type of data returned by this output. " +
            "Serves as a hint to determine what kind of view should be used for this output (ex. XY, Time Graph, Table, etc..). " +
            "Providers of type TREE_TIME_XY and TIME_GRAPH can be grouped under the same time axis. " +
            "Providers of type DATA_TREE only provide a tree with columns and don't have any XY nor time graph data associated with it. " +
            "Providers of type NONE have no data to visualize. Can be used for grouping purposes and/or as data provider configurator.")
    ProviderType getType();

    /**
     * @return The description.
     */
    @Schema(description = "Describes the output provider's features")
    String getDescription();

    /**
     * @return optional parent Id
     */
    @Schema(required = false, description = "Optional parent Id for grouping purposes for example of derived data providers.")
    String getParentId();

    /**
     * @return the input configuration used to create this data provider.
     */
    @Schema(required = false, description = "Optional input configuration used to create this derived data provider.")
    Configuration getConfiguration();
}
