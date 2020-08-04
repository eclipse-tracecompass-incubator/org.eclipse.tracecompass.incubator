/**********************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
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
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Stub class for the time graph row elements. It matches the trace server
 * protocol's <code>TimeGraphState</code> schema
 *
 * @author Geneviève Bastien
 */
public class TimeGraphStateStub implements Serializable {

    private static final long serialVersionUID = -6695724477648258175L;

    private final long fStartTime;
    private final long fEndTime;
    private final String fLabel;
    private final Integer fTags;
    private final OutputElementStyleStub fStyle;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param startTime
     *            The start time of the state
     * @param endTime
     *            The end time of this state
     * @param label
     *            The label for this state
     * @param tags
     *            The additional tags for this state
     * @param style
     *            The style of this state
     */
    @JsonCreator
    public TimeGraphStateStub(@JsonProperty("start") Long startTime,
            @JsonProperty("end") Long endTime,
            @JsonProperty("label") String label,
            @JsonProperty("tags") Integer tags,
            @JsonProperty("style") OutputElementStyleStub style) {
        fStartTime = Objects.requireNonNull(startTime, "The 'start' json field was not set");
        fEndTime = Objects.requireNonNull(endTime, "The 'end' json field was not set");
        fLabel = label;
        fTags = tags;
        fStyle = style;
    }

    /**
     * Get the start time of this state
     *
     * @return The start time of the state
     */
    public long getStartTime() {
        return fStartTime;
    }

    /**
     * Get the end time of this state
     *
     * @return The end time of the state
     */
    public long getEndTime() {
        return fEndTime;
    }

    /**
     * Get the label of this state
     *
     * @return The label of the state
     */
    public String getLabel() {
        return fLabel;
    }

    /**
     * Get the tags of this state
     *
     * @return The tags of the state
     */
    public int getTags() {
        return fTags;
    }

    /**
     * Get the style of this state
     *
     * @return The style of the state
     */
    public OutputElementStyleStub getStyle() {
        return fStyle;
    }

}
