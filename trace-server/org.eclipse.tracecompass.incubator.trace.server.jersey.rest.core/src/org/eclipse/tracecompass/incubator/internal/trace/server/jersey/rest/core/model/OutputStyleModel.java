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

import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@Schema(description = "Map of styles specific to an output where values give hints on the style. " +
        "The keys are strings that can be used in OutputElementStyle")
public interface OutputStyleModel {

    /**
     * @return The styles.
     */
    @NonNull
    Map<@NonNull String, @NonNull OutputElementStyle> getStyles();
}
