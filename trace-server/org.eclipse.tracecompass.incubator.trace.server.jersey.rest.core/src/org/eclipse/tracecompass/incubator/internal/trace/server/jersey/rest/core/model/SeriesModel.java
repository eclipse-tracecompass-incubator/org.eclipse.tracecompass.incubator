/*******************************************************************************
 * Copyright (c) 2021 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model;

import org.eclipse.jdt.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
public interface SeriesModel {

    /**
     * @return The series ID.
     */
    @Schema(description = "Series' ID", required = true)
    long getSeriesId();

    /**
     * @return The series name.
     */
    @NonNull
    @Schema(description = "Series' name", required = true)
    String getSeriesName();

    /**
     * @return The X values.
     */
    @JsonProperty("xValues")
    @ArraySchema(arraySchema = @Schema(description = "Series' X values", required = true))
    long[] getXValues();

    /**
     * @return The Y values.
     */
    @JsonProperty("yValues")
    @ArraySchema(arraySchema = @Schema(description = "Series' Y values", required = true))
    long[] getYValues();
}
