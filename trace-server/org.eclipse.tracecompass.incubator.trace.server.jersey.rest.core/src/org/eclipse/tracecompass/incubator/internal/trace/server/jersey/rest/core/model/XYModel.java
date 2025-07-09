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

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNull;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
public interface XYModel {

    /**
     * @return The title.
     */
    @NonNull
    @Schema(description = "Title of the model", requiredMode = RequiredMode.REQUIRED)
    String getTitle();

    /**
     * @return The series.
     */
    @NonNull
    @ArraySchema(arraySchema = @Schema(description = "The collection of series"), schema = @Schema(requiredMode = RequiredMode.REQUIRED))
    Collection<@NonNull SeriesModel> getSeries();
}
