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

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

import java.util.List;

/**
 * Describes alternative representations of sampling options.
 * Used for OpenAPI generation.
 */
@Schema(oneOf = {
        Sampling.TimestampSampling.class,
        Sampling.CategorySampling.class,
        Sampling.RangeSampling.class
})
public interface Sampling {

    /**
     * Sampling as list of timestamps.
     */
    public static class TimestampSampling {
        /**
         * The sampling points as timestamp values.
         */
        @Schema(
            description = "Sampling as list of timestamps",
            requiredMode = RequiredMode.REQUIRED
        )
        public long[] sampling;
    }

    /**
     * Sampling as list of categories.
     */
    public static class CategorySampling {
        /**
         * The sampling points as category names or labels.
         */
        @Schema(
            description = "Sampling as list of categories",
            requiredMode = RequiredMode.REQUIRED
        )
        public String[] sampling;
    }

    /**
     * Sampling as a list of [start, end] ranges.
     */
    public static class RangeSampling {
        /**
         * The list of sampling ranges, each with a start and end value.
         */
        @Schema(
            description = "Sampling as list of [start, end] timestamp ranges",
            requiredMode = RequiredMode.REQUIRED
        )
        public List<StartEndRange> sampling;
    }

    /**
     * Represents a closed interval [start, end] for a sampling range.
     */
    public static class StartEndRange {
        /**
         * Start timestamp of the range (inclusive).
         */
        @Schema(description = "Start timestamp of the range", requiredMode = RequiredMode.REQUIRED)
        public long start;

        /**
         * End timestamp of the range (inclusive).
         */
        @Schema(description = "End timestamp of the range", requiredMode = RequiredMode.REQUIRED)
        public long end;
    }
}
