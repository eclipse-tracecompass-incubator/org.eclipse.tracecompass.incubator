/**********************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Represents a range-based domain of values for an axis, typically numeric
 * (e.g., execution durations, time intervals).
 * <p>
 * Used in Swagger schema generation for chart axis descriptions.
 */
public class AxisDomainRange implements AxisDomain {

    private final long start;
    private final long end;

    /**
     * Constructor
     *
     * @param start
     *            The minimum value of the axis domain
     * @param end
     *            The maximum value of the axis domain
     */
    @JsonCreator
    public AxisDomainRange(
            @JsonProperty("start") long start,
            @JsonProperty("end") long end) {
        this.start = start;
        this.end = end;
    }

    @Override
    @Schema(description = "Type of axis domain", requiredMode = RequiredMode.REQUIRED)
    public String getType() {
        return "range";
    }

    /**
     * @return The start of the domain range
     */
    @Schema(description = "Start of the axis range", requiredMode = RequiredMode.REQUIRED)
    public long getStart() {
        return start;
    }

    /**
     * @return The end of the domain range
     */
    @Schema(description = "End of the axis range", requiredMode = RequiredMode.REQUIRED)
    public long getEnd() {
        return end;
    }
}
