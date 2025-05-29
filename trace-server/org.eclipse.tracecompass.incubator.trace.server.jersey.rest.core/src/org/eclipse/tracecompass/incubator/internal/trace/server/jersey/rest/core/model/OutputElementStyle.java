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

import org.eclipse.jdt.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@Schema(description = "Represents the style on an element (ex. Entry, TimeGraphState, ...) returned by any output. " +
        "Supports style inheritance. " +
        "To avoid having too many styles, the element style can have a parent style and will have all the same style property values as the parent, and can add or override style properties.")
public interface OutputElementStyle {

    /**
     * @return The parent key.
     */
    @Nullable
    @Schema(description = "Parent style key or empty if there is no parent. " +
            "The parent key should match a style key defined in the style model and is used for style inheritance. " +
            "A comma-delimited list of parent style keys can be used for style composition, the last one taking precedence.")
    String getParentKey();

    /**
     * @return The style values.
     */
    @Schema(description = "Style values or empty map if there are no values. " +
            "Keys and values are defined in " +
            "https://github.com/eclipse-tracecompass/org.eclipse.tracecompass/blob/master/tmf/org.eclipse.tracecompass.tmf.core/src/org/eclipse/tracecompass/tmf/core/model/StyleProperties.java")
    Map<String, StyleValue> getValues();

    /**
     * Type for style values (String, Float, Integer or Boolean)
     */
    @Schema(description = "Supported types of a style value.", oneOf = { String.class, Double.class, Integer.class })
    interface StyleValue {
        // empty
    }
}
