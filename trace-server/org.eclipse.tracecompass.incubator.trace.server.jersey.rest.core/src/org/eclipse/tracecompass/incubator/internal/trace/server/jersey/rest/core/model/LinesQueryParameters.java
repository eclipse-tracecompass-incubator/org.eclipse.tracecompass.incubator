/**********************************************************************
 * Copyright (c) 2021 Ericsson
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
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
public interface LinesQueryParameters {

    /**
     * @return The parameters.
     */
    @NonNull
    @Schema(requiredMode = RequiredMode.REQUIRED)
    LinesParameters getParameters();

    /**
     * Property names below use underscores as per trace-server protocol.
     */
    interface LinesParameters {

        enum Direction {
            NEXT, PREVIOUS
        }

        @JsonProperty("requested_table_index")
        long getRequestedTableIndex();

        @JsonProperty("requested_times")
        long[] getRequestedTimes();

        @JsonProperty("requested_table_count")
        @Schema(requiredMode = RequiredMode.REQUIRED)
        int getRequestedTableCount();

        @JsonProperty("requested_table_column_ids")
        long[] getRequestedTableColumnIds();

        @JsonProperty("table_search_expressions")
        Map<Object, String> getTableSearchExpressions();

        @JsonProperty("table_search_direction")
        @Schema(description = "Search next or previous item (e.g. event, state etc.)")
        Direction getTableSearchDirection();
    }
}
