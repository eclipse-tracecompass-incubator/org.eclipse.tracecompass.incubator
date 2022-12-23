/**********************************************************************
 * Copyright (c) 2023 Ericsson
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
 * <code>XY Entry</code> schema
 *
 * @author Bernd Hufmann
 */
public class XyEntryStub extends EntryStub {

    private static final long serialVersionUID = 8369475974133990978L;

    private final boolean fIsDefault;

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
     * @param isDefault
     *            whether the entry is a default entry (default selection)
     */
    @JsonCreator
    public XyEntryStub(@JsonProperty("labels") List<String> labels,
            @JsonProperty("id") Integer id,
            @JsonProperty("parentId") Integer parentId,
            @JsonProperty("hasData") boolean hasRowModel,
            @JsonProperty("style") OutputElementStyleStub style,
            @JsonProperty("isDefault") boolean isDefault) {
        super(labels, id, parentId, hasRowModel, style);
        fIsDefault = isDefault;
    }

    /**
     * @return whether the entry is a default entry and should be selected by
     *         default
     */
    public boolean isDefault() {
        return fIsDefault;
    }
}
