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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
public interface VirtualTableLine {

    /**
     * @return The index.
     */
    @Schema(description = "The index of this line in the virtual table")
    long getIndex();

    /**
     * @return The cells.
     */
    @ArraySchema(arraySchema = @Schema(description = "The content of the cells for this line. This array matches the column ids returned above"))
    List<VirtualTableCell> getCells();

    /**
     * @return The tags.
     */
    @Schema(description = "Tags for the entire line. " +
            "A bit mask to apply for tagging elements (e.g. table lines, states). " +
            "This can be used by the server to indicate if a filter matches and what action to apply. " +
            "Use 0 for no tags, 1 and 2 are reserved, 4 for 'BORDER' and 8 for 'HIGHLIGHT'.")
    int getTags();
}
