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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Describes alternative representations of sampling options.
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
    @ArraySchema(
        arraySchema = @Schema(
            description = "Sampling as a flat list of timestamps. Example: [100, 200, 350]"
        ),
        schema = @Schema(type = "integer", format = "int64")
    )
    class TimestampSampling implements Sampling {
        private final long[] timestamps;

        public TimestampSampling(long[] timestamps) {
            this.timestamps = timestamps;
        }

        @JsonValue
        public long[] getTimestamps() {
            return timestamps;
        }
    }

    /**
     * Sampling as list of categories.
     */
    @ArraySchema(
        arraySchema = @Schema(
            description = "Sampling as a flat list of categories. Example: [\"READ\", \"WRITE\"]"
        ),
        schema = @Schema(type = "string")
    )
    class CategorySampling implements Sampling {
        private final String[] categories;

        public CategorySampling(String[] categories) {
            this.categories = categories;
        }

        @JsonValue
        public String[] getCategories() {
            return categories;
        }
    }

    /**
     * Sampling as a list of start/end range objects.
     */
    @ArraySchema(
        arraySchema = @Schema(
            description = "Sampling as a list of start/end range objects. Example: [{\"start\": 10, \"end\": 20}, {\"start\": 50, \"end\": 75}]"
        ),
        // This will now correctly reference the StartEndRange object schema
        schema = @Schema(implementation = StartEndRange.class)
    )
    class RangeSampling implements Sampling {
        private final List<StartEndRange> ranges;

        public RangeSampling(List<StartEndRange> ranges) {
            this.ranges = ranges;
        }

        @JsonValue
        public List<StartEndRange> getRanges() {
            return ranges;
        }
    }
}
