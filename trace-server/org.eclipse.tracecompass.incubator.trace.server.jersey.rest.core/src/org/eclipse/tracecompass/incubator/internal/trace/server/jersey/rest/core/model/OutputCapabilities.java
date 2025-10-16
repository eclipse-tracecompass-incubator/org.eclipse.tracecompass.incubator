/*******************************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
public interface OutputCapabilities {

    /**
     * @return whether this output (data provider) can create derived outputs (data providers). 'false' if absent.
     */
    @JsonProperty("canCreate")
    @Schema(description = "Optional, whether this output (data provider) can create derived outputs (data providers). 'false' if absent.")
    boolean canCreate();

    /**
     * @return whether this output (data provider) can be deleted. 'false' if absent.
     */
    @Schema(description = "Optional, whether this output (data provider) can be deleted. 'false' if absent.")
    @JsonProperty("canDelete")
    boolean canDelete();

    /**
     * @return whether this output (data provider) uses the selection range. 'false' if absent.
     */
    @Schema(description = "Optional, whether this output (data provider) uses the selection range. 'false' if absent.")
    @JsonProperty("selectionRange")
    boolean selectionRange();
}
