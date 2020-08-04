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
 * A Stub class for the entry elements. It matches the trace server protocol's
 * <code>Entry</code> schema
 *
 * @author Geneviève Bastien
 */
public class EntryStub implements Serializable {

    private static final long serialVersionUID = 3428838268294534414L;

    private final List<String> fLabels;
    private final int fId;
    private final int fParentId;
    private final boolean fHasRowModel;
    private final OutputElementStyleStub fStyle;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param labels
     *            The labels of this entry
     * @param id
     *            The unique ID of the entry
     * @param parentId
     *            The unique id of the parent of this entry
     * @param hasRowModel
     *            Whether this entry has data
     * @param style
     *            The style of this entry
     */
    @JsonCreator
    public EntryStub(@JsonProperty("labels") List<String> labels,
            @JsonProperty("id") Integer id,
            @JsonProperty("parentId") Integer parentId,
            @JsonProperty("hasData") boolean hasRowModel,
            @JsonProperty("style") OutputElementStyleStub style) {
        fLabels = Objects.requireNonNull(labels, "The 'labels' json field was not set");
        fId = Objects.requireNonNull(id, "The 'id' json field was not set");
        fParentId = parentId;
        fHasRowModel = hasRowModel;
        fStyle = style;
    }

    /**
     * Get the labels of this entry
     *
     * @return The labels of the entry
     */
    public List<String> getLabels() {
        return fLabels;
    }

    /**
     * Get the ID of this entry
     *
     * @return The ID of the entry
     */
    public int getId() {
        return fId;
    }

    /**
     * Get the parent ID of the entry
     *
     * @return The parent ID
     */
    public int getParentId() {
        return fParentId;
    }

    /**
     * Get whether this entry has row models
     *
     * @return Whether the entry has row model
     */
    public boolean hasRowModel() {
        return fHasRowModel;
    }

    /**
     * Get the style for this entry
     *
     * @return The entry's style
     */
    public OutputElementStyleStub getStyle() {
        return fStyle;
    }

}
