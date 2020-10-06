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
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Stub class for the table lines. It matches the trace server
 * protocol's <code>TableModel</code> schema
 *
 * @author Geneviève Bastien
 */
public class TableModelStub implements Serializable {

    private static final long serialVersionUID = 5896455708422239772L;

    private final long fLowIndex;
    private final long fSize;
    private final List<Long> fColumnIds;
    private final List<LineModelStub> fLines;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param lowIndex
     *            The start time of the state
     * @param size
     *            The end time of this state
     * @param columnIds
     *            The column IDs returned
     * @param lines
     *            The array of lines
     */
    @JsonCreator
    public TableModelStub(@JsonProperty("lowIndex") Long lowIndex,
            @JsonProperty("size") Integer size,
            @JsonProperty("columnIds") List<Long> columnIds,
            @JsonProperty("lines") List<LineModelStub> lines) {
        fLowIndex = Objects.requireNonNull(lowIndex, "The 'lowIndex' json field was not set");
        fSize = Objects.requireNonNull(size, "The 'size' json field was not set");
        fColumnIds = columnIds;
        fLines = lines;
    }

    /**
     * Get the low index of this model
     *
     * @return The start time of the mode
     */
    public long getLowIndex() {
        return fLowIndex;
    }

    /**
     * Get the number of lines in this model
     *
     * @return The size of this model
     */
    public long getSize() {
        return fSize;
    }

    /**
     * Get the column IDs returned by the model
     *
     * @return The column IDs of this model
     */
    public List<Long> getColumnIds() {
        return fColumnIds;
    }

    /**
     * Get the lines in this model
     *
     * @return The lines in this model
     */
    public List<LineModelStub> getLines() {
        return fLines;
    }

}
