/**********************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model;

import org.eclipse.jdt.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 *
 * Uses generic time range for requested time range, which allows sampling
 * values are not restricted to timestamps.
 */
public interface GenericXYQueryParameters {

    /**
     * @return The parameters.
     */
    @NonNull
    @Schema(requiredMode = RequiredMode.REQUIRED)
    GenericXYRequestedParameters getParameters();

    /**
     * Property names below use underscores as per trace-server protocol.
     */
    interface GenericXYRequestedParameters {

        @JsonProperty("requested_timerange")
        @Schema(requiredMode = RequiredMode.REQUIRED)
        GenericTimeRange getRequestedTimeRange();

        @JsonProperty("requested_items")
        @Schema(requiredMode = RequiredMode.REQUIRED)
        int[] getRequestedItems();

        @JsonProperty("filter_query_parameters")
        @Schema(requiredMode = RequiredMode.NOT_REQUIRED)
        RequestedFilterQueryParameters getFilterQueryParameters();
    }
}
