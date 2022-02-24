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

import org.eclipse.jdt.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
public interface TimeGraphArrow {

    /**
     * @return The start time.
     */
    @Schema(description = "Start time for this arrow", required = true)
    long getStart();

    /**
     * @return The end time.
     */
    @Schema(description = "End time for this arrow", required = true)
    long getEnd();

    /**
     * @return The source ID.
     */
    @Schema(description = "Source entry's unique ID", required = true)
    long getSourceId();

    /**
     * @return The target ID.
     */
    @Schema(description = "Target entry's unique ID", required = true)
    long getTargetId();

    /**
     * @return The style.
     */
    @Nullable
    OutputElementStyle getStyle();
}
