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
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
public interface TimeGraphState {

    /**
     * @return The label.
     */
    @Nullable
    @Schema(description = "Text label to apply to this TimeGraphState if resolution permits. "
            + "Optional, no label is applied if absent")
    String getLabel();

    /**
     * @return The start time.
     */
    @Schema(description = "Start time for this state", requiredMode = RequiredMode.REQUIRED)
    long getStart();

    /**
     * @return The end time.
     */
    @Schema(description = "End time for this state", requiredMode = RequiredMode.REQUIRED)
    long getEnd();

    /**
     * @return The style.
     */
    @Nullable
    @Schema(description = "If state holds a single applicable value and no style(s), that value is this object's parent key")
    OutputElementStyle getStyle();

    /**
     * @return The tags.
     */
    @Schema(description = "Tags to apply on this state. A value of 0 should be handled as none (no tags)")
    int getTags();
}
