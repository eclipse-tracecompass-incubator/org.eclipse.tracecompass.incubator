/**********************************************************************
 * Copyright (c) 2021, 2025 Ericsson
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

import org.eclipse.jdt.annotation.NonNull;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
public interface TreeEntryModel {

    /**
     * @return The entries.
     */
    @NonNull
    @Schema(requiredMode = RequiredMode.REQUIRED)
    List<@NonNull TreeDataModel> getEntries();

    /**
     * @return The headers.
     */
    @NonNull
    @Schema(requiredMode = RequiredMode.REQUIRED)
    List<@NonNull TreeColumnHeader> getHeaders();

    /**
     * @return The auto-expand level.
     */
    @Schema(description = "Optional auto-expand level to be used for the input of the tree. "
            + "If omitted or value -1 means that all subtrees should be expanded. The "
            + "value 0 means that there is no auto-expand; 1 means that top-level "
            + "elements are expanded, but not their children; 2 means that top-level "
            + "elements are expanded, and their children, but not grand-children; and so "
            + "on.")
    int getAutoExpandLevel();
}
