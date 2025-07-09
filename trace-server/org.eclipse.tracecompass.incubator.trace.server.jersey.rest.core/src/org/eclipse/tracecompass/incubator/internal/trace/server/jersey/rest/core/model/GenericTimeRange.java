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

/**
 * Contributes to the model used for TSP swagger-core annotations.
 *
 * This time range can optionally include a number of sampling points. The
 * sampling values are not restricted to timestamps—they can represent other
 * types of samples.
 */
@Schema(description = "A generic time range with optional sampling count. Sampling points may represent values other than timestamps.")
public interface GenericTimeRange {

    /**
     * @return The inclusive start of the range.
     */
    @Schema(description = "Start of the range", requiredMode = RequiredMode.REQUIRED)
    long getStart();

    /**
     * @return The inclusive end of the range.
     */
    @Schema(description = "End of the range", requiredMode = RequiredMode.REQUIRED)
    long getEnd();

    /**
     * @return The number of samples to compute within the range.
     */
    @Schema(description = "Optional number of samples (1–65536) to generate within the range", requiredMode = RequiredMode.NOT_REQUIRED)
    int getNbSamples();
}
