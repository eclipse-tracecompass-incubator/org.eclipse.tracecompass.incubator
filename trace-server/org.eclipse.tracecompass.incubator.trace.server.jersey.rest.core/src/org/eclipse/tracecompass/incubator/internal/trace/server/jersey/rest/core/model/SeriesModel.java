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
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@Schema(description = "This model includes the series output style values.")
public interface SeriesModel {

    /**
     * @return The series ID.
     */
    @Schema(description = "Series' ID", requiredMode = RequiredMode.REQUIRED)
    long getSeriesId();

    /**
     * @return The series name.
     */
    @NonNull
    @Schema(description = "Series' name", requiredMode = RequiredMode.REQUIRED)
    String getSeriesName();

    /**
     * @return The X values.
     */
    @JsonProperty("xValues")
    @ArraySchema(arraySchema = @Schema(description = "Series' X values"), schema = @Schema(requiredMode = RequiredMode.REQUIRED))
    long[] getXValues();

    /**
     * @return The Y values.
     */
    @JsonProperty("yValues")
    @ArraySchema(arraySchema = @Schema(description = "Series' Y values"), schema = @Schema(requiredMode = RequiredMode.REQUIRED))
    double[] getYValues();

    /**
     * @return The series style.
     */
    @NonNull
    @Schema(requiredMode = RequiredMode.REQUIRED)
    OutputElementStyle getStyle();
}
