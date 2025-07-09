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

import org.eclipse.jdt.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@Schema(description = "Error response that includes an detailed description of the error occured")
public interface ErrorResponse {

    /**
     * @return The short, human-readable description of the error.
     */
    @Schema(requiredMode = RequiredMode.REQUIRED, description = "The short, human-readable description of the error")
    String getTitle();

    /**
     * @return The optional human-readable explanation of the error with details helping the client to correct the error.
     */
    @Nullable
    @Schema(description = "The optional human-readable explanation of the error with details helping the client to correct the error")
    String getDetail();
}
