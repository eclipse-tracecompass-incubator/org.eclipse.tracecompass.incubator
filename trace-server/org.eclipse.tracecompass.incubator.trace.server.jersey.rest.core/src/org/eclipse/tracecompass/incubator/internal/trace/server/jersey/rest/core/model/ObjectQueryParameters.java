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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
public interface ObjectQueryParameters {

    /**
     * @return The parameters.
     */
    @NonNull
    @Schema(requiredMode = RequiredMode.REQUIRED)
    ObjectParameters getParameters();

    /**
     * Detailed object parameters
     */
    interface ObjectParameters {
        @JsonProperty("selection_range")
        @ArraySchema(arraySchema = @Schema( requiredMode = RequiredMode.NOT_REQUIRED, description = "Selection range included when the output has the selectionRange capability" ), minItems = 2, maxItems = 2, schema = @Schema( type = "integer", format = "int64" ))
        long[] getSelectionRange();

        @JsonProperty("next")
        @Schema(requiredMode = RequiredMode.NOT_REQUIRED, description = "Navigation object passed back from last response when browsing forward")
        Object getNext();

        @JsonProperty("previous")
        @Schema(requiredMode = RequiredMode.NOT_REQUIRED, description = "Navigation object passed back from last response when browsing backward")
        Object getPrevious();
    }
}
