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

import org.eclipse.jdt.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
public interface IRequestedQueryParameters {

    /**
     * @return The parameters.
     */
    @NonNull
    @Schema(required = true)
    IRequestedParameters getParameters();

    /**
     * Property names below use underscores as per trace-server protocol.
     */
    interface IRequestedParameters {

        @JsonProperty("requested_times")
        @Schema(required = true)
        long[] getRequestedTimes();

        @JsonProperty("requested_items")
        @Schema(required = true)
        int[] getRequestedItems();
    }
}
