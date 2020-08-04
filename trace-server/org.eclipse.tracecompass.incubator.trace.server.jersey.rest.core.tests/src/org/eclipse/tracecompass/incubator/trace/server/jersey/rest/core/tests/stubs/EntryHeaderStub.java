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
 * A Stub class for the entry headers elements. It matches the trace server
 * protocol's <code>EntryHeader</code> schema
 *
 * @author Geneviève Bastien
 */
public class EntryHeaderStub implements Serializable {

    private static final long serialVersionUID = -1255029411491940403L;

    private final String fName;
    private final String fTooltip;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param name
     *            the name of the header
     * @param tooltip
     *            The tooltip for this header
     */
    @JsonCreator
    public EntryHeaderStub(@JsonProperty("name") String name,
            @JsonProperty("tooltip") String tooltip) {
        fName = Objects.requireNonNull(name, "The 'name' json field was not set");
        fTooltip = tooltip;
    }

    /**
     * Get the name for this header element
     *
     * @return The name of this header
     */
    public String getName() {
        return fName;
    }

    /**
     * Get the tooltip for this header element
     *
     * @return The tooltip of this header
     */
    public String getTooltip() {
        return fTooltip;
    }
}
