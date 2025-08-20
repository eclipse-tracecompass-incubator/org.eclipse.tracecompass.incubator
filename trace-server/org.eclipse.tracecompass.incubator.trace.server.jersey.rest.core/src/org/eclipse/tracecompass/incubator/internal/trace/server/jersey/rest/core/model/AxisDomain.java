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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Represents the domain of values for a chart axis.
 * <p>
 * The domain can either be categorical (e.g., discrete string labels) or a
 * numeric range (e.g., time or duration values).
 * <p>
 * This interface is used for OpenAPI schema generation and supports polymorphic
 * serialization via {@code type} discriminator.
 */
@Schema(description = "Domain of values supported on a chart axis. Can be either categorical or numeric range.")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = AxisDomainCategorical.class, name = "categorical"),
    @JsonSubTypes.Type(value = AxisDomainRange.class, name = "range")
})
public interface AxisDomain {

    /**
     * Returns the type of axis domain.
     * <p>
     * This is used as a discriminator to identify the specific subtype
     * implementation (e.g., "categorical", "range").
     *
     * @return A string identifying the domain type
     */
    @Schema(description = "Type of axis domain (e.g., 'categorical' or 'range')", requiredMode = RequiredMode.REQUIRED)
    @JsonProperty("type")
    String getType();
}
