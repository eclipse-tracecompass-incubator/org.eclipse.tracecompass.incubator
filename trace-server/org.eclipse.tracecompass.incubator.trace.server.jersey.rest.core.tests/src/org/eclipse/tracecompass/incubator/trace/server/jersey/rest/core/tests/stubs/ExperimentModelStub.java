/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Basic Implementation of the serialized experiment model object used by
 * clients. Equality of two stubs is determined by equality of names, paths,
 * {@link UUID}, and of the child tracesas the start time, end time and number
 * of events may be unknown due to incomplete indexing.
 *
 * @author Loic Prieur-Drevon
 */
public class ExperimentModelStub extends AbstractModelStub {
    /**
     * Generated Serial Version UID
     */
    private static final long serialVersionUID = -4143023822990481607L;
    private final Set<TraceModelStub> fTraces;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param name
     *            trace name
     * @param uuid
     *            the stub's UUID
     * @param nbEvents
     *            number of current indexed events
     * @param start
     *            start time
     * @param end
     *            end time
     * @param traces
     *            encapsulated traces
     */
    @JsonCreator
    public ExperimentModelStub(@JsonProperty("name") String name,
            @JsonProperty("UUID") UUID uuid,
            @JsonProperty("nbEvents") long nbEvents,
            @JsonProperty("start") long start,
            @JsonProperty("end") long end,
            @JsonProperty("traces") Set<TraceModelStub> traces) {
        super(name, uuid, nbEvents, start, end);
        fTraces = traces;
    }

    /**
     * Getter for the list of traces in the experiment
     *
     * @return list of traces encapsulated in the experiment
     */
    public Set<TraceModelStub> getTraces() {
        return fTraces;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fTraces);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        } else if (obj instanceof ExperimentModelStub) {
            ExperimentModelStub other = (ExperimentModelStub) obj;
            return fTraces.equals(other.fTraces);
        }
        return false;
    }

    @Override
    public String toString() {
        return getName() + ":<nbEvents=" + ", UUID=" + getUUID() + ", traces=" + fTraces + '>';
    }

}
