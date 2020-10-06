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
 * A Stub class for a table line element. It matches the trace server protocol's
 * <code>LineModel</code> schema
 *
 * @author Geneviève Bastien
 */
public class LineModelStub implements Serializable {

    private static final long serialVersionUID = 7658220520705904830L;

    private final long fIndex;
    private final List<CellModelStub> fCells;



    private static class CellModelStub implements Serializable {

        private static final long serialVersionUID = -7600647881436449076L;

        /**
         * {@link JsonCreator} Constructor for final fields
         *
         * @param content
         *            The unique ID of the entry
         * @param tags
         *            The unique id of the parent of this entry
         */
        @JsonCreator
        public CellModelStub(@JsonProperty("content") String content,
                @JsonProperty("tags") int tags) {
            Objects.requireNonNull(content, "The 'index' json field was not set");
            if (tags < 0) {
                throw new IllegalArgumentException("tags should be positive");
            }
        }

    }

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param index
     *            The index of thie line
     * @param cells
     *            The cells
     */
    @JsonCreator
    public LineModelStub(@JsonProperty("index") Long index,
            @JsonProperty("cells") List<CellModelStub> cells) {
        fIndex = Objects.requireNonNull(index, "The 'index' json field was not set");
        fCells = Objects.requireNonNull(cells, "The 'cells' json field was not set");
    }

    /**
     * Get the index of this line in the model
     *
     * @return The index
     */
    public long getIndex() {
        return fIndex;
    }

    /**
     * Get the cells of this entry
     *
     * @return The cells
     */
    public List<CellModelStub> getCells() {
        return fCells;
    }

}
