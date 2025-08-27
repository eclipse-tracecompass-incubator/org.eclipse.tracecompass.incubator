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

import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * Represents a categorical domain of values for an axis, where values are
 * predefined strings such as labels or states (e.g., "read", "write", "idle").
 * <p>
 * Used in Swagger schema generation for chart axis descriptions.
 */
@Schema(description = "Domain of values supported on a categorical chart axis.")
public class AxisDomainCategorical implements AxisDomain {

    private final @NonNull Set<String> categories;

    /**
     * Constructor
     *
     * @param categories
     *            The set of category labels
     */
    @JsonCreator
    public AxisDomainCategorical(
            @JsonProperty("categories")
            @NonNull Set<String> categories) {
        this.categories = categories;
    }

    @Override
    @Schema(description = "Type of axis domain: 'categorical'", requiredMode = RequiredMode.REQUIRED)
    public String getType() {
        return "categorical"; //$NON-NLS-1$
    }

    /**
     * @return The set of category names
     */
    @ArraySchema(arraySchema = @Schema(description = "List of category labels on the axis"), schema = @Schema(requiredMode = RequiredMode.REQUIRED))
    public @NonNull Set<String> getCategories() {
        return categories;
    }
}
