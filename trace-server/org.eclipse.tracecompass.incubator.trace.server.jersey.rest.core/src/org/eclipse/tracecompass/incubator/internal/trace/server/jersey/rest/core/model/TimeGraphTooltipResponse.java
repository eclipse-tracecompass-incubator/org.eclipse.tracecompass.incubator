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

import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@Schema(allOf = GenericResponse.class)
public interface TimeGraphTooltipResponse {

    /**
     * @return The model.
     */
    @Nullable
    @Schema(description = "Tooltip map with key-value pairs, where the key is the tooltip name and the corresponding value is the tooltip value")
    Map<String, String> getModel();
}
