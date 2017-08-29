/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.trace;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList;

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
    private final List<String> fColumns;
    private final List<ITmfEventAspect<?>> fAspects;

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
        ImmutableList.Builder<String> stringBuilder = ImmutableList.<String> builder();
        ImmutableList.Builder<ITmfEventAspect<?>> aspectBuilder = ImmutableList.<ITmfEventAspect<?>> builder();
        for (ITmfEventAspect<?> aspect : fTrace.getEventAspects()) {
            stringBuilder.add(aspect.getName());
            aspectBuilder.add(aspect);
        }
        fColumns = stringBuilder.build();
        fAspects = aspectBuilder.build();
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
     * Getter for the columns available in this trace
     *
     * @return the aspect names
     */
    @XmlElement
    public List<String> getColumns() {
        return fColumns;
    }

    /**
     * Getter for the list of aspects in this trace
     *
     * @return the list of aspects
     */
    @XmlTransient
    public List<ITmfEventAspect<?>> getAspects() {
        return fAspects;
    }

    /**
     * Dispose of the underlying trace to avoid resource leakage
     */
    public void dispose() {
        fTrace.dispose();
    }

    /**
     * Expose the send request method
     *
     * @param request
     *            the request on the underlying trace
     */
    public void sendRequest(TmfEventRequest request) {
        fTrace.sendRequest(request);
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
