/**********************************************************************
 * Copyright (c) 2021, 2025 Ericsson
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

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
public interface TraceQueryParameters {

    /**
     * @return The parameters.
     */
    @NonNull
    @Schema(required = true)
    TraceParameters getParameters();

    /**
     * Detailed trace parameters.
     */
    interface TraceParameters {
        /**
         * @return The name.
         */
        @Schema(description = "The name of the trace in the server, to override the default name")
        String getName();

        /**
         * @return The type ID.
         */
        @Schema(description = "The trace type's ID, to force the use of a parser / disambiguate the trace type")
        String getTypeID();

        /**
         * @return The URI.
         */
        @JsonProperty("uri")
        @NonNull
        @Schema(description = "URI of the trace", required = true)
        String getUri();
    }
}
