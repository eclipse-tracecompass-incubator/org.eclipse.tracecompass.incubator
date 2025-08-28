/*******************************************************************************
 * Copyright (c) 2024, 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model;

import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 *
 * @author Kaveh Shahedi
 */
public interface Bookmark {

    /**
     * @return The bookmark UUID.
     */
    @JsonProperty("uuid")
    @Schema(description = "The bookmark's unique identifier", requiredMode = RequiredMode.REQUIRED)
    UUID getUUID();

    /**
     * @return The bookmark name.
     */
    @NonNull
    @Schema(description = "User defined name for the bookmark", requiredMode = RequiredMode.REQUIRED)
    String getName();

    /**
     * @return The start time.
     */
    @Schema(description = "The bookmark's start time", requiredMode = RequiredMode.REQUIRED)
    long getStart();

    /**
     * @return The end time.
     */
    @Schema(description = "The bookmark's end time", requiredMode = RequiredMode.REQUIRED)
    long getEnd();

}
