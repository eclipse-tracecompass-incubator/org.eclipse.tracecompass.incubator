/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.trace;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Model to store the trace and its name, while exposing only the necessary
 * fields
 *
 * @author Loic Prieur-Drevon
 */
@XmlRootElement
public class TraceModel {
    private final @NonNull ITmfTrace fTrace;
    private final String fName;

    /**
     * Construct a model object, giving the trace a name
     *
     * @param name
     *            name to give this object
     * @param trace
     *            {@link ITmfTrace} to encapsulate
     */
    public TraceModel(@NonNull String name, @NonNull ITmfTrace trace) {
        fName = name;
        fTrace = trace;
    }

    /**
     * Getter for this trace's name
     *
     * @return this trace's name
     */
    @XmlElement
    public String getName() {
        return fName;
    }

    /**
     * Getter for this trace's path
     *
     * @return this trace's path
     */
    @XmlElement
    public String getPath() {
        return fTrace.getPath();
    }

    /**
     * Getter for the current number of indexed events in this trace
     *
     * @return the current number of indexed events in this trace.
     */
    @XmlElement
    public long getNbEvents() {
        return fTrace.getNbEvents();
    }

    /**
     * Get the trace's start time
     *
     * @return the trace's start time
     */
    @XmlElement
    public long getStart() {
        return fTrace.getStartTime().toNanos();
    }

    /**
     * Get the trace's end time
     *
     * @return the trace's end time
     */
    @XmlElement
    public long getEnd() {
        return fTrace.getEndTime().toNanos();
    }

    /**
     * Getter for the underlying trace
     *
     * @return the backing trace
     */
    @XmlTransient
    public @NonNull ITmfTrace getTrace() {
        return fTrace;
    }
}
