package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Represents a closed interval [start, end] for a sampling range.
 */
@Schema(
     name = "StartEndRange",
     description = "An object representing a closed interval with a start and end."
)
class StartEndRange {
    @Schema(description = "Start of the range (inclusive).", requiredMode = RequiredMode.REQUIRED)
    private final long start;

    @Schema(description = "End of the range (inclusive).", requiredMode = RequiredMode.REQUIRED)
    private final long end;

    public StartEndRange(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }
}
