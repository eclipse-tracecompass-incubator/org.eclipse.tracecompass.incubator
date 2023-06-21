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

import java.util.Collection;
import java.util.Map;

import org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views.FilterQueryParameters.FilterQueryStrategy;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@Schema(description = "FilterQueryParameters is used to support search and filter expressions for timegraph views")
public interface RequestedFilterQueryParameters {

    /**
     * @return Strategy of search.
     */
    @JsonProperty("strategy")
    @Schema(description = "Optional parameter that enables the full search (deep search) or not", required = false)
    FilterQueryStrategy getStrategy();

    /**
     * @return Map of filter expressions.
     */
    @JsonProperty("filter_expressions_map")
    @Schema(description = "The key of this map can be \"1\" (means DIMMED) or \"4\" (means EXCLUDED) and the value is an array of the desired search query (e.g. {\"1\": [\"openat\", \"duration>10ms\"]})", required = true)
    Map<Integer, Collection<String>> getFilterExpressionsMap();
}
