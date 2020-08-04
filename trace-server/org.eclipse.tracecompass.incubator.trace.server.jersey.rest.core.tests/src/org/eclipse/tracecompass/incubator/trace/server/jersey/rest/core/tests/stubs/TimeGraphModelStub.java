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
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Stub class for the entry model. It matches the trace server protocol's
 * <code>TimeGraphModel</code> schema.
 *
 * @author Geneviève Bastien
 */
public class TimeGraphModelStub implements Serializable {

    private static final long serialVersionUID = -7171791006527715964L;
    private final Set<TimeGraphRowStub> fRows;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param rows
     *            The set of rows for this model
     */
    @JsonCreator
    public TimeGraphModelStub(@JsonProperty("rows") Set<TimeGraphRowStub> rows) {
        fRows = Objects.requireNonNull(rows, "The 'rows' json field was not set");
    }

    /**
     * Get the entries described by this model
     *
     * @return The entries in this model
     */
    public Set<TimeGraphRowStub> getRows() {
        return fRows;
    }

}
