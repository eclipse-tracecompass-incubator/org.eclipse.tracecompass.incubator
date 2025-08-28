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

import java.util.Collection;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@Schema(allOf = TreeDataModel.class)
public interface TimeGraphEntry {

    /**
     * @return The start time.
     */
    @Schema(description = "Beginning of the range for which this entry exists", requiredMode = RequiredMode.REQUIRED)
    long getStart();

    /**
     * @return The end time.
     */
    @Schema(description = "End of the range for which this entry exists", requiredMode = RequiredMode.REQUIRED)
    long getEnd();

    /**
     * @return The entry's metadata map.
     */
    @Schema(description = "Optional metadata map for domain specific data for matching data across data providers. Keys for the same data shall be the same across data providers."
            + " For each key all values shall have the same type.")
    Map<String, Collection<MetadataValue>> getMetadata();

    /**
     * Type for metadata values (String or Number)
     */
    @Schema(description = "Supported types of a metadata value. Only values of type Number or String are allowed.", oneOf = { String.class, Number.class })
    interface MetadataValue {
        // empty
    }
}
