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
 * A Stub class for the time graph row elements. It matches the trace server
 * protocol's <code>TimeGraphRow</code> schema
 *
 * @author Geneviève Bastien
 */
public class TimeGraphRowStub implements Serializable {

    private static final long serialVersionUID = -1699773811110874980L;
    private final long fId;
    private final List<TimeGraphStateStub> fStates;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param id
     *            The unique ID of the entry
     * @param states
     *            The unique id of the parent of this entry
     */
    @JsonCreator
    public TimeGraphRowStub(@JsonProperty("entryId") Long id,
            @JsonProperty("states") List<TimeGraphStateStub> states) {
        fId = Objects.requireNonNull(id, "The 'entryId' json field was not set");
        fStates = Objects.requireNonNull(states, "The 'states' json field was not set");
    }

    /**
     * Get the entry Id of this row
     *
     * @return The entryId
     */
    public long getEntryId() {
        return fId;
    }

    /**
     * Get the end time of this entry
     *
     * @return The end time of the entry
     */
    public List<TimeGraphStateStub> getStates() {
        return fStates;
    }

}
