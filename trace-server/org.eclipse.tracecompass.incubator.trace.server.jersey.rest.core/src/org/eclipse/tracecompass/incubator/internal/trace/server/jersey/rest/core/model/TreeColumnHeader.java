/**********************************************************************
 * Copyright (c) 2021, 2023 Ericsson
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

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
public interface TreeColumnHeader {

    /**
     * @return The name.
     */
    @NonNull
    @Schema(description = "Displayed name for this header", requiredMode = RequiredMode.REQUIRED)
    String getName();

    /**
     * @return The tooltip.
     */
    @Schema(description = "Displayed tooltip for this header. " +
            "Optional, no tooltip is applied if absent.")
    String getTooltip();

    /**
     * @return The data type of the column.
     */
    @Schema(description = "Data type of column. " +
            "Optional, data type STRING is applied if absent.")
    DataType getDataType();
}
