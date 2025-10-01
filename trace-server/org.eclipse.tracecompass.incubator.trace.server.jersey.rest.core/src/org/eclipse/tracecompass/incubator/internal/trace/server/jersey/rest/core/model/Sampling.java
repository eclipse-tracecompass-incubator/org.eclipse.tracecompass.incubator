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

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Describes alternative representations of sampling options.
 */
@Schema(oneOf = {
        Sampling.TimestampSampling.class,
        Sampling.CategorySampling.class,
        Sampling.RangeSampling.class
}, description = "Sampling options for xAxis")
public interface Sampling {

    /**
     * Sampling as list of timestamps.
     */
    @Schema(description = "Timestamp sampling")
    public static class TimestampSampling {
        /**
         * The sampling points as timestamp values.
         */
        @Schema(
            description = "Sampling as a flat list of timestamps. Example: [100, 200, 350]",
            requiredMode = RequiredMode.REQUIRED
        )
        public long[] xValues;
    }

    /**
     * Sampling as list of categories.
     */
    @Schema(description = "Categories sampling")
    public static class CategorySampling {
        /**
         * The sampling points as category names or labels.
         */
        @Schema(
            description = "Sampling as a flat list of categories. Example: [\"READ\", \"WRITE\"]",
            requiredMode = RequiredMode.REQUIRED
        )
        public String[] xCategories;
    }

    /**
     * Sampling as a list of start/end range objects.
     */
    @Schema(description = "Range sampling")
    public static class RangeSampling {
        /**
         * The list of sampling ranges, each with a start and end value.
         */
        @Schema(
            description = "Sampling as a list of start/end range objects. Example: [{\"start\": 10, \"end\": 20}, {\"start\": 50, \"end\": 75}]",
            requiredMode = RequiredMode.REQUIRED
        )
        public List<StartEndRange> xRanges;
    }
}
