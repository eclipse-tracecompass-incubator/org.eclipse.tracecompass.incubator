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
@Schema(description = "A marker set is used to represent a set of annotations that can be fetched")
public interface MarkerSet {

    /**
     * @return The name.
     */
    @Schema(description = "Name of this marker set")
    String getName();

    /**
     * @return The ID.
     */
    @Schema(description = "ID of this marker set")
    String getId();
}
