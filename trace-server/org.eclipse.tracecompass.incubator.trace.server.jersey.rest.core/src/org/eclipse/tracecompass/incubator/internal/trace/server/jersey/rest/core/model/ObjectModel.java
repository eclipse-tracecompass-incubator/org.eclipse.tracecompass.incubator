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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@Schema(description = "A generic object with optional navigation parameters. " +
        "If the next and/or previous navigation parameters are present, one of them can be returned as fetch parameter in a subsequent request to get the following or preceding object.")
public interface ObjectModel {

    /**
     * @return The object
     */
    @NonNull
    @Schema(requiredMode = RequiredMode.REQUIRED)
    Object getObject();

    /**
     * @return The optional next navigation parameter object
     */
    @Nullable
    @Schema(requiredMode = RequiredMode.NOT_REQUIRED, description = "Navigation object to pass back in next request when browsing forward")
    Object getNext();

    /**
     * @return The optional previous navigation parameter object
     */
    @Nullable
    @Schema(requiredMode = RequiredMode.NOT_REQUIRED, description = "Navigation object to pass back in next request when browsing backward")
    Object getPrevious();
}
