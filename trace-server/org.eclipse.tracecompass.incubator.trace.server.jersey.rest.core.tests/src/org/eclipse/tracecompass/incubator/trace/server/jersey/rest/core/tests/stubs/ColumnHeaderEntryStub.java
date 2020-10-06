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

import org.eclipse.jdt.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Stub class for the column headers elements. It matches the trace server
 * protocol's <code>ColumnHeaderEntry</code> schema
 *
 * @author Geneviève Bastien
 */
public class ColumnHeaderEntryStub implements Serializable {

    /**
     * Generated serial UID
     */
    private static final long serialVersionUID = -1730932469358135560L;

    private final long fId;
    private final @NonNull String fName;
    private final String fDescription;
    private final String fType;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param id
     *            The ID of this column entry
     *
     * @param name
     *            the name of the header
     * @param descriptor
     *            The description of the header
     * @param type
     *            The type of column
     */
    @JsonCreator
    public ColumnHeaderEntryStub(@JsonProperty("id") long id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String descriptor,
            @JsonProperty("type") String type) {
        fId = id;
        fName = Objects.requireNonNull(name, "The 'name' json field was not set");
        fDescription = descriptor;
        fType = type;
    }

    /**
     * Get the ID of this column header
     *
     * @return The ID of the header
     */
    public long getId() {
        return fId;
    }

    /**
     * Get the name of this column header
     *
     * @return The name of the header
     */
    public String getName() {
        return fName;
    }

    /**
     * Get the description of this column header
     *
     * @return The description of the header
     */
    public String getDescription() {
        return fDescription;
    }

    /**
     * Get the type of this column header
     *
     * @return The type of the header
     */
    public String getType() {
        return fType;
    }

}
