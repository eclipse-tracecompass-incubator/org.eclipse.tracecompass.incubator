/*******************************************************************************
 * Copyright (c) 2020, 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.services;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.tracecompass.tmf.core.io.ResourceUtil;
import org.eclipse.tracecompass.tmf.core.project.model.ITmfPropertiesProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Trace model for TSP
 */
public final class Trace implements Serializable {
    private static final long serialVersionUID = 1194829124866484394L;
    private final String fName;
    private final UUID fUUID;
    private final String fPath;
    private final long fNbEvents;
    private final long fStart;
    private final long fEnd;
    private final Map<String, String> fProperties;
    private final String fIndexingStatus;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param name
     *            trace name
     * @param path
     *            path to trace on server file system
     * @param uuid
     *            the stub's UUID
     * @param nbEvents
     *            number of current indexed events
     * @param start
     *            start time
     * @param end
     *            end time
     * @param properties
     *            the properties of the trace
     * @param indexingStatus
     *            indexing status
     */
    @JsonCreator
    public Trace(@JsonProperty("name") String name,
            @JsonProperty("UUID") UUID uuid,
            @JsonProperty("path") String path,
            @JsonProperty("nbEvents") long nbEvents,
            @JsonProperty("start") long start,
            @JsonProperty("end") long end,
            @JsonProperty("properties") Map<String, String> properties,
            @JsonProperty("indexingStatus") String indexingStatus) {
        fName = name;
        fUUID = uuid;
        fPath = path;
        fNbEvents = nbEvents;
        fStart = start;
        fEnd = end;
        fProperties = properties;
        fIndexingStatus = indexingStatus;
    }

    /**
     * Constructs a trace model from its instance
     *
     * @param trace
     *            trace
     * @param uuid
     *            UUID
     * @return the trace model
     */
    public static Trace from(ITmfTrace trace, UUID uuid) {
        return new Trace(trace.getName(),
                uuid,
                trace.getPath(),
                trace.getNbEvents(),
                trace.getStartTime().toNanos(),
                trace.getEndTime().toNanos(),
                trace instanceof ITmfPropertiesProvider ? ((ITmfPropertiesProvider) trace).getProperties() : new HashMap<>(),
                trace.isIndexing() ? "RUNNING" : "COMPLETED"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Constructs a trace model from its resource
     *
     * @param traceResource
     *            trace resource
     * @param uuid
     *            UUID
     * @return the trace model
     */
    public static Trace from(IResource traceResource, UUID uuid) {
        IPath location = ResourceUtil.getLocation(traceResource);
        if (location == null) {
            location = traceResource.getProjectRelativePath();
        }
        String path = location.removeTrailingSeparator().toOSString();
        return new Trace(traceResource.getName(),
                uuid,
                path,
                0L,
                0L,
                0L,
                new HashMap<>(),
                "CLOSED"); //$NON-NLS-1$
    }

    /**
     * Returns the name
     * @return the name
     */
    public String getName() {
        return fName;
    }

    /**
     * Returns the UUID
     * @return the UUID
     */
    public UUID getUUID() {
        return fUUID;
    }

    /**
     * Returns the path
     * @return the path
     */
    public String getPath() {
        return fPath;
    }

    /**
     * Returns the number of events
     * @return the number of events
     */
    public long getNbEvents() {
        return fNbEvents;
    }

    /**
     * Returns the start time
     * @return the start time
     */
    public long getStart() {
        return fStart;
    }

    /**
     * Returns the end time
     * @return the end time
     */
    public long getEnd() {
        return fEnd;
    }

    /**
     * Returns the properties
     * @return the properties
     */
    public Map<String, String> getProperties() {
        return fProperties;
    }

    /**
     * Returns the indexing status
     * @return the indexing status
     */
    public String getIndexingStatus() {
        return fIndexingStatus;
    }

    @Override
    public String toString() {
        return "Trace [fName=" + fName + ", fUUID=" + fUUID + ", fPath=" + fPath + ", fNbEvents=" + fNbEvents + ", fStart=" + fStart + ", fEnd=" + fEnd + ", fIndexingStatus=" + fIndexingStatus + ", fProperties" + fProperties.toString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
    }
}