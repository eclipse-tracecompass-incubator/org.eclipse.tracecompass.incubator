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

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@Schema(description = "Experiment model")
public interface Experiment {

    /**
     * @return The name.
     */
    @Schema(description = "User defined name for the experiment")
    String getName();

    /**
     * @return The UUID.
     */
    @JsonProperty("UUID")
    @Schema(description = "The experiment's unique identifier")
    UUID getUUID();

    /**
     * @return The number of events.
     */
    @Schema(description = "Current number of indexed events in the experiment")
    long getNbEvents();

    /**
     * @return The start time.
     */
    @Schema(description = "The experiment's start time")
    long getStart();

    /**
     * @return The end time.
     */
    @Schema(description = "The experiment's end time")
    long getEnd();

    /**
     * @return The indexing status.
     */
    @Schema(description = "Status of the experiment indexing")
    IndexingStatus getIndexingStatus();

    /**
     * @return The traces.
     */
    @ArraySchema(arraySchema = @Schema(description = "The traces encapsulated by this experiment"))
    List<Trace> getTraces();
}
