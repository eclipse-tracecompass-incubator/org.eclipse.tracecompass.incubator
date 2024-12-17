/**********************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs;

import java.io.Serializable;

import org.eclipse.jdt.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Basic Implementation of the serialized annotation used by clients.
 *
 * @author Bernd Hufmann
 */
public class AnnotationStub implements Serializable{

    private static final long serialVersionUID = -1493443168095328737L;
    private final @Nullable String fLabel;
    private final long fTime;
    private final long fDuration;
    private final long fEntryId;
    private final String fType;
    private final OutputElementStyleStub fStyle;

    /**
     * Constructor
     *
     * @param time
     *            Annotation start time
     * @param duration
     *            Annotation duration
     * @param entryId
     *            EntryId to position the annotation or -1 if it is not attached
     *            to a single entry
     * @param type
     *            Annotation type
     * @param label
     *            Annotation label for display purposes
     * @param style
     *            Style to use for this annotation
     */
    public AnnotationStub(@JsonProperty("time") long time,
            @JsonProperty("duration") long duration,
            @JsonProperty("entryId") long entryId,
            @JsonProperty("type") String type,
            @JsonProperty("label") @Nullable String label,
            @JsonProperty("style") OutputElementStyleStub style) {
        fTime = time;
        fDuration = duration;
        fEntryId = entryId;
        fType = type;
        fLabel = label;
        fStyle = style;
    }

    /**
     * Get the annotation time, for chart annotations.
     *
     * @return Annotation time
     */
    public long getTime() {
        return fTime;
    }

    /**
     * Get the duration
     *
     * @return Duration
     */
    public long getDuration() {
        return fDuration;
    }

    /**
     * Get the entry model ID.
     *
     * @return Entry model ID associated to this annotation or -1 if this
     *         annotation is not attached to a single entry
     */
    public long getEntryId() {
        return fEntryId;
    }

    /**
     * Get the annotation type.
     *
     * @return Annotation type
     */
    public String getType() {
        return fType;
    }

    /**
     * Get the annotation label.
     *
     * @return Annotation label or null
     */
    public @Nullable String getLabel() {
        return fLabel;
    }

    /**
     * Get the style associated with this element
     *
     * @return {@link OutputElementStyleStub} describing the style of this element
     */
    public @Nullable OutputElementStyleStub getStyle() {
        return fStyle;
    }
}
