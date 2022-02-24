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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contributes to the model used for TSP swagger-core annotations.
 */
@Schema(description = "An annotation is used to mark an interesting area at a given time or time range")
public interface Annotation {

    /**
     * The annotation types.
     */
    enum AnnotationType {
        CHART, TREE
    }

    /**
     * @return The time.
     */
    @Schema(description = "Time of this annotation", required = true)
    long getTime();

    /**
     * @return The duration.
     */
    @Schema(description = "Duration of this annotation", required = true)
    long getDuration();

    /**
     * @return The entry ID.
     */
    @Schema(description = "Entry's unique ID or -1 if annotation not associated with an entry", required = true)
    long getEntryId();

    /**
     * @return The type.
     */
    @NonNull
    @Schema(description = "Type of annotation indicating its location", required = true)
    AnnotationType getType();

    /**
     * @return The label.
     */
    @Nullable
    @Schema(description = "Text label of this annotation")
    String getLabel();

    /**
     * @return The style.
     */
    @Nullable
    OutputElementStyle getStyle();
}
