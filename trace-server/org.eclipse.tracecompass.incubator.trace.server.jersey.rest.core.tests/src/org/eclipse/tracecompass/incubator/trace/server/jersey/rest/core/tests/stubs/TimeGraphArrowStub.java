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
import java.util.Objects;

import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphArrow;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Stub class for the time graph arrow elements. It matches the trace server
 * protocol's {@link TimeGraphArrow} schema
 *
 * @author Bernd Hufmann
 */
public class TimeGraphArrowStub implements Serializable {

    private static final long serialVersionUID = 7583805352991909215L;

    private final long fStartTime;
    private final long fEndTime;
    private final long fSourceId;
    private final long fTargetId;
    private final OutputElementStyleStub fStyle;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param startTime
     *            The start time of the state
     * @param endTime
     *            The end time of this state
     * @param sourceId
     *            The ID of the source entry
     * @param targetId
     *            The ID of the target entry
     * @param style
     *            The style of this state
     */
    @JsonCreator
    public TimeGraphArrowStub(@JsonProperty("start") Long startTime,
            @JsonProperty("end") Long endTime,
            @JsonProperty("sourceId") Long sourceId,
            @JsonProperty("targetId") Long targetId,
            @JsonProperty("style") OutputElementStyleStub style) {
        fStartTime = Objects.requireNonNull(startTime, "The 'start' json field was not set");
        fEndTime = Objects.requireNonNull(endTime, "The 'end' json field was not set");
        fSourceId = sourceId;
        fTargetId = targetId;
        fStyle = style;
    }

    /**
     * Get the ID of the source entry
     *
     * @return The ID of the source entry
     */
    public long getStartTime() {
        return fStartTime;
    }

    /**
     * Get the ID of the target entry
     *
     * @return The ID of the target entry
     */
    public long getEndTime() {
        return fEndTime;
    }

    /**
     * Get the label of this state
     *
     * @return The label of the state
     */
    public long getSourceId() {
        return fSourceId;
    }

    /**
     * Get the tags of this state
     *
     * @return The tags of the state
     */
    public long getTargetId() {
        return fTargetId;
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
