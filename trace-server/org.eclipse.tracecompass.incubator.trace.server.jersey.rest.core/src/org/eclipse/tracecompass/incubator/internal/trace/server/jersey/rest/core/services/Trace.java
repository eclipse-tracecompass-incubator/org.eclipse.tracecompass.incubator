/*******************************************************************************
 * Copyright (c) 2020 Ericsson
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
import java.util.UUID;

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
            @JsonProperty("indexingStatus") String indexingStatus) {
        fName = name;
        fUUID = uuid;
        fPath = path;
        fNbEvents = nbEvents;
        fStart = start;
        fEnd = end;
        fIndexingStatus = indexingStatus;
    }

    /**
     * Constructs a trace model
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
                trace.isIndexing() ? "RUNNING" : "COMPLETED");
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
     * Returns the indexing status
     * @return the indexing status
     */
    public String getIndexingStatus() {
        return fIndexingStatus;
    }

    @Override
    public String toString() {
        return "Trace [fName=" + fName + ", fUUID=" + fUUID + ", fPath=" + fPath + ", fNbEvents=" + fNbEvents + ", fStart=" + fStart + ", fEnd=" + fEnd + ", fIndexingStatus=" + fIndexingStatus + "]";
    }
}