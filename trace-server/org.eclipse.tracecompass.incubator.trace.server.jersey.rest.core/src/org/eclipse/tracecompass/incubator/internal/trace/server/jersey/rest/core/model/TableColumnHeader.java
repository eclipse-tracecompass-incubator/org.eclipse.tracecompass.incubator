/*******************************************************************************
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

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
public interface TableColumnHeader {

    /**
     * @return The ID.
     */
    @Schema(description = "Unique id to identify this column in the backend")
    long getId();

    /**
     * @return The name.
     */
    @Schema(description = "Displayed name for this column")
    String getName();

    /**
     * @return The description.
     */
    @Schema(description = "Description of the column")
    String getDescription();

    /**
     * @return The type.
     */
    @Schema(description = "Type of data associated to this column")
    String getType();
}
