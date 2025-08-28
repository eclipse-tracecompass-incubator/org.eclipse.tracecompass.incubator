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
import org.eclipse.jdt.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@Schema(description = "Base entry returned by tree endpoints")
public interface TreeDataModel {

    /**
     * @return The ID.
     */
    @Schema(description = "Unique ID to identify this entry in the backend", requiredMode = RequiredMode.REQUIRED)
    long getId();

    /**
     * @return The parent ID.
     */
    @Schema(description = "Optional unique ID to identify this entry's parent. If the parent ID is -1 or omitted, this entry has no parent.")
    long getParentId();

    /**
     * @return The labels.
     */
    @ArraySchema(arraySchema = @Schema(description = "Array of cell labels to be displayed. " +
            "The length of the array and the index of each column need to correspond to the header array returned in the tree model."), schema = @Schema(requiredMode = RequiredMode.REQUIRED))
    @NonNull
    List<@NonNull String> getLabels();

    /**
     * @return The style.
     */
    @Nullable
    OutputElementStyle getStyle();

    /**
     * @return Has data (or not).
     */
    @Schema(description = "Whether or not this entry has data. false if absent.")
    boolean getHasData();
}
