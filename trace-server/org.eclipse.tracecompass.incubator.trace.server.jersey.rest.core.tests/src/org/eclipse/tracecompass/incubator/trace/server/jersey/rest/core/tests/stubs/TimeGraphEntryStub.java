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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Stub class for the entry elements. It matches the trace server protocol's
 * <code>TimeGraphEntry</code> schema
 *
 * @author Geneviève Bastien
 */
public class TimeGraphEntryStub extends EntryStub {

    private static final long serialVersionUID = 6834171793860138236L;

    private final Long fStartTime;
    private final Long fEndTime;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param labels
     *            The labels of this entry
     * @param id
     *            The unique ID of the entry
     * @param parentId
     *            The unique id of the parent of this entry
     * @param startTime
     *            The start time of the entry
     * @param endTime
     *            The end time of this entry
     * @param hasRowModel
     *            Whether has row model property
     * @param style
     *            The style of this entry
     */
    @JsonCreator
    public TimeGraphEntryStub(@JsonProperty("labels") List<String> labels,
            @JsonProperty("id") Integer id,
            @JsonProperty("parentId") Integer parentId,
            @JsonProperty("start") Long startTime,
            @JsonProperty("end") Long endTime,
            @JsonProperty("hasData") boolean hasRowModel,
            @JsonProperty("style") OutputElementStyleStub style) {
        super(labels, id, parentId, hasRowModel, style);
        fStartTime = startTime;
        fEndTime = endTime;
    }

    /**
     * Get the start time of this entry
     *
     * @return The start time of the entry
     */
    public Long getStartTime() {
        return fStartTime;
    }

    /**
     * Get the end time of this entry
     *
     * @return The end time of the entry
     */
    public Long getEndTime() {
        return fEndTime;
    }

}
