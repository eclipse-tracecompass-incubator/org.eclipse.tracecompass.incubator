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
import org.eclipse.jdt.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 * Represents an axis description for XY charts.
 */
@Schema(description = "Describes a single axis in an XY chart, including label, unit, data type, and optional domain.")
public interface XYAxisDescription {

    /**
     * @return Axis label, e.g., "Time", "Duration", etc.
     */
    @NonNull
    @Schema(description = "Label for the axis", requiredMode = RequiredMode.REQUIRED)
    String getLabel();

    /**
     * @return Unit string, such as "ns", "ms", or "" (empty if not applicable).
     */
    @NonNull
    @Schema(description = "Unit associated with this axis (e.g., ns, ms)", requiredMode = RequiredMode.REQUIRED)
    String getUnit();

    /**
     * @return The data type of the axis values.
     */
    @NonNull
    @Schema(description = "The type of data this axis represents", requiredMode = RequiredMode.REQUIRED)
    DataType getDataType();

    /**
     * @return Optional domain of values that this axis can take.
     */
    @JsonProperty("axisDomain")
    @Schema(description = "Optional domain of values that this axis supports", requiredMode = RequiredMode.NOT_REQUIRED)
    @Nullable AxisDomain getAxisDomain();
}
