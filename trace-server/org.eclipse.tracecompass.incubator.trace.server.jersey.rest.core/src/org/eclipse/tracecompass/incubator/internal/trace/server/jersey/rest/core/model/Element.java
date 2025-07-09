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

import org.eclipse.jdt.annotation.NonNull;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@Schema(description = "An element model to be identified")
public interface Element {

    /**
     * The element types.
     */
    enum ElementType {
        STATE, ANNOTATION, ARROW
    }

    /**
     * @return The element type.
     */
    @NonNull
    @Schema(description = "The type of element", requiredMode = RequiredMode.REQUIRED)
    ElementType getElementType();

    /**
     * @return The time.
     */
    @Schema(description = "Element's start time", requiredMode = RequiredMode.REQUIRED)
    long getTime();

    /**
     * @return The duration.
     */
    @Schema(description = "Element's duration", requiredMode = RequiredMode.REQUIRED)
    long getDuration();

    /**
     * @return The entry ID.
     */
    @Schema(description = "Entry's unique ID (annotation, arrow)")
    long getEntryId();

    /**
     * @return The destination ID.
     */
    @Schema(description = "Destination entry's unique ID (arrow)")
    long getDestinationId();
}
