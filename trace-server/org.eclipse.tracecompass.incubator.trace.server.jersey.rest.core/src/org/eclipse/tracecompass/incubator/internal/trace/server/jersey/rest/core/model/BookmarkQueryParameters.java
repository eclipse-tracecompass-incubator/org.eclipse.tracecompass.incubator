/*******************************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Parameters for bookmark creation and update operations
 *
 * @author Kaveh Shahedi
 */
public interface BookmarkQueryParameters {

    /**
     * @return The bookmark parameters
     */
    @JsonProperty("parameters")
    @Schema(description = "The bookmark parameters", requiredMode = RequiredMode.REQUIRED)
    BookmarkParameters getParameters();


    /**
     * Bookmark parameters
     */
    interface BookmarkParameters {
        /**
         * @return The bookmark name
         */
        @JsonProperty("name")
        @Schema(description = "The name to give this bookmark", requiredMode = RequiredMode.REQUIRED)
        String getName();

        /**
         * @return The start time
         */
        @JsonProperty("start")
        @Schema(description = "The bookmark's start time", requiredMode = RequiredMode.REQUIRED)
        long getStart();

        /**
         * @return The end time
         */
        @JsonProperty("end")
        @Schema(description = "The bookmark's end time", requiredMode = RequiredMode.REQUIRED)
        long getEnd();
    }
}