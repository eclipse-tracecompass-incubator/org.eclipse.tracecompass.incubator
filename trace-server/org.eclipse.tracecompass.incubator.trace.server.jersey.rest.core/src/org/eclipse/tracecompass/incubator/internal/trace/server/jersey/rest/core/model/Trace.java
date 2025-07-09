/**********************************************************************
 * Copyright (c) 2021, 2024 Ericsson
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
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@Schema(description = "Trace model")
public interface Trace {

    /**
     * @return The name.
     */
    @Schema(description = "User defined name for the trace")
    String getName();

    /**
     * @return The path.
     */
    @Schema(description = "Path to the trace on the server's file system")
    String getPath();

    /**
     * @return The UUID.
     */
    @JsonProperty("UUID")
    @Schema(description = "The trace's unique identifier")
    UUID getUUID();

    /**
     * @return The number of events.
     */
    @Schema(description = "Current number of indexed events in the trace")
    long getNbEvents();

    /**
     * @return The start time.
     */
    @Schema(description = "The trace's start time")
    long getStart();

    /**
     * @return The end time.
     */
    @Schema(description = "The trace's end time")
    long getEnd();

    /**
     * @return The properties.
     */
    @Schema(description = "The trace's properties")
    Map<String, String> getProperties();

    /**
     * @return The indexing status.
     */
    @Schema(description = "Status of the trace indexing")
    IndexingStatus getIndexingStatus();
}
