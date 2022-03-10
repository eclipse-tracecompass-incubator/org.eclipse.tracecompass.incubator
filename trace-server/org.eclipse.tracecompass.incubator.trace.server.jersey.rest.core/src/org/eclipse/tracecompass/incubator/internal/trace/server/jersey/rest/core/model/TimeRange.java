/**********************************************************************
 * Copyright (c) 2022 Ericsson
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
@Schema(description = "A time range with optional number of timestamps to be sampled")
public interface TimeRange {

    /**
     * @return The start time.
     */
    @Schema(description = "The start of the time range", required = true)
    long getStart();

    /**
     * @return The end time.
     */
    @Schema(description = "The end of the time range", required = true)
    long getEnd();

    /**
     * @return The number of times to be sampled.
     */
    @Schema(description = "The number of timestamps to be sampled (1-65536) in the given range", required = false)
    int getNbTimes();
}
