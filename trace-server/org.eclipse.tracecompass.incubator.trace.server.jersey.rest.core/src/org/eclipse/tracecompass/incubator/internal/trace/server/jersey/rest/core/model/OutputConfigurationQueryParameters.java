/**********************************************************************
 * Copyright (c) 2023, 2024 Ericsson
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
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@Schema(allOf = ConfigurationQueryParameters.class)
public interface OutputConfigurationQueryParameters {
    /**
     * @return the typeId of the configuration according to the
     *         {@link ConfigurationSourceType}
     */
    @Schema(requiredMode = RequiredMode.REQUIRED, description = "The type ID of the corresponding ConfigurationSourceType defined by this output.")
    @JsonProperty("sourceTypeId")
    String getSourceTypeId();
}
