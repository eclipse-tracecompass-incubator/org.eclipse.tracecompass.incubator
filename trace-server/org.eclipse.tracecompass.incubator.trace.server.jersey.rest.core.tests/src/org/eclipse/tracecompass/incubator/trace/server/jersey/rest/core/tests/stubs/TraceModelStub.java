/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

/**
 * Basic Implementation of the serialized trace model object used by clients
 *
 * @author Loic Prieur-Drevon
 */
public class TraceModelStub implements Serializable {

    /**
     * Generated Serial Version UID
     */
    private static final long serialVersionUID = -1030854786688167776L;

    private final String fName;
    private final String fPath;
    private final long fNbEvents;
    private final long fStart;
    private final long fEnd;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param name
     *            trace name
     * @param path
     *            path to trace on server file system
     * @param nbEvents
     *            number of current indexed events
     * @param start
     *            start time
     * @param end
     *            end time
     */
    @JsonCreator
    public TraceModelStub(@JsonProperty("name") String name,
            @JsonProperty("path") String path,
            @JsonProperty("nbEvents") long nbEvents,
            @JsonProperty("start") long start,
            @JsonProperty("end") long end) {
        fName = name;
        fPath = path;
        fNbEvents = nbEvents;
        fStart = start;
        fEnd = end;
    }

    /**
     * Getter for the trace's name
     *
     * @return trace name
     */
    public String getName() {
        return fName;
    }

    /**
     * Getter for the path to the trace on the server's file system
     *
     * @return path
     */
    public String getPath() {
        return fPath;
    }

    /**
     * Getter for the number of events currently indexed in the trace
     *
     * @return current number of indexed events
     */
    public long getNbEvents() {
        return fNbEvents;
    }

    /**
     * Getter for the trace start time
     *
     * @return trace start time
     */
    public long getStart() {
        return fStart;
    }

    /**
     * Getter for the current trace end time
     *
     * @return return the current maximum indexed timestamp in this trace.
     */
    public long getEnd() {
        return fEnd;
    }

    @Override
    public String toString() {
        return fName + ":<path=" + fPath + ", nbEvents=" + fNbEvents //$NON-NLS-1$ //$NON-NLS-2$
                + ", start=" + fStart + ", end=" + fEnd + '>'; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public int hashCode() {
        return Objects.hash(fName, fPath);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TraceModelStub other = (TraceModelStub) obj;
        return Objects.equals(fName, other.fName)
                && Objects.equals(fPath, other.fPath)
                && fNbEvents == other.fNbEvents
                && fStart == other.fStart
                && fEnd == other.fEnd;
    }
}
