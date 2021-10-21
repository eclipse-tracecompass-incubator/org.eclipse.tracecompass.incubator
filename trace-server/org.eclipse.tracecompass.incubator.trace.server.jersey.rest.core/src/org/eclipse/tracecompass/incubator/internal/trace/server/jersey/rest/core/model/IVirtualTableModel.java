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
public interface IVirtualTableModel {

    /**
     * @return The column IDs.
     */
    @ArraySchema(arraySchema = @Schema(description = "The array of column ids that are returned. They should match the content of the lines' content"))
    List<Long> getColumnIds();

    /**
     * @return The lines.
     */
    List<IVirtualTableLine> getLines();

    /**
     * @return The low index.
     */
    @Schema(description = "Index in the virtual table of the first returned event")
    long getLowIndex();

    /**
     * @return The size.
     */
    @Schema(description = "Number of events. If filtered, the size will be the number of events that match the filters")
    long getSize();
}
