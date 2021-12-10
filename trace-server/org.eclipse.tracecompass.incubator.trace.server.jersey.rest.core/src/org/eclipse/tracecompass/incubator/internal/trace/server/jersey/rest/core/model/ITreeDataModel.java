/**********************************************************************
 * Copyright (c) 2021 Ericsson
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

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@Schema(description = "Base entry returned by tree endpoints")
public interface ITreeDataModel {

    /**
     * @return The ID.
     */
    @Schema(description = "Unique id to identify this entry in the backend", required = true)
    long getId();

    /**
     * @return The parent ID.
     */
    @Schema(description = "Unique id to identify this parent's entry, " +
            "optional if this entry does not have a parent.")
    long getParentId();

    /**
     * @return The labels.
     */
    @ArraySchema(arraySchema = @Schema(description = "Array of cell labels to be displayed. " +
            "The length of the array and the index of each column need to correspond to the header array returned in the tree model.", required = true))
    @NonNull
    List<@NonNull String> getLabels();

    /**
     * @return The style.
     */
    @Nullable
    IOutputElementStyle getStyle();

    /**
     * @return Has data (or not).
     */
    @Schema(description = "Whether or not this entry has data")
    boolean getHasData();
}
