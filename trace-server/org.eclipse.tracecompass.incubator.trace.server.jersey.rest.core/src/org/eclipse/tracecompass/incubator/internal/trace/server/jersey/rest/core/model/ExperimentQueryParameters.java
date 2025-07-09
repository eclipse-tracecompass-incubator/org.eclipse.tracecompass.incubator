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

import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
public interface ExperimentQueryParameters {

    /**
     * @return The parameters.
     */
    @NonNull
    @Schema(requiredMode = RequiredMode.REQUIRED)
    ExperimentParameters getParameters();

    /**
     * Detailed experiment parameters.
     */
    interface ExperimentParameters {

        /**
         * @return The name.
         */
        @NonNull
        @Schema(description = "The name to give this experiment", requiredMode = RequiredMode.REQUIRED)
        String getName();

        /**
         * @return The traces.
         */
        @NonNull
        @ArraySchema(arraySchema = @Schema(description = "The unique identifiers of the traces to encapsulate in this experiment", requiredMode = RequiredMode.REQUIRED))
        List<@NonNull UUID> getTraces();
    }
}
