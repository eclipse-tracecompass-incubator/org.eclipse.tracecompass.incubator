/**********************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@Schema(allOf = TreeDataModel.class)
public interface XYTreeEntry {

    /**
     * @return whether or not the entry is a default entry and its xy data
     *         should be fetched by default.
     */
    @JsonProperty("isDefault")
    @Schema(description = "Optional flag to indicate whether or not the entry is a default entry and its xy data should be fetched by default.")
    boolean isDefault();
}
