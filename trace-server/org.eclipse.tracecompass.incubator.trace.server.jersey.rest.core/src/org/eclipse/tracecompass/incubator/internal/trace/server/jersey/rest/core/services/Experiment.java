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
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Experiment model for TSP
 */
public final class Experiment implements Serializable {
    private static final long serialVersionUID = -3626414315455912960L;
    private final String fName;
    private final UUID fUUID;
    private final long fNbEvents;
    private final long fStart;
    private final long fEnd;
    private final String fIndexingStatus;
    private final Set<Trace> fTraces;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param name
     *            experiment name
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
     * @param traces
     *            traces
     */
    @JsonCreator
    public Experiment(@JsonProperty("name") String name,
            @JsonProperty("UUID") UUID uuid,
            @JsonProperty("nbEvents") long nbEvents,
            @JsonProperty("start") long start,
            @JsonProperty("end") long end,
            @JsonProperty("indexingStatus") String indexingStatus,
            @JsonProperty("traces") Set<Trace> traces) {
        fName = name;
        fUUID = uuid;
        fNbEvents = nbEvents;
        fStart = start;
        fEnd = end;
        fIndexingStatus = indexingStatus;
        fTraces = traces;
    }

    /**
     * Constructs an experiment model
     *
     * @param experiment
     *            experiment
     * @param expUUID
     *            experiment UUID
     * @return the experiment model
     */
    public static Experiment from(TmfExperiment experiment, UUID expUUID) {
        Iterator<UUID> iter = ExperimentManagerService.getTraceUUIDs(expUUID).iterator();
        Set<Trace> traces = Sets.newLinkedHashSet(Lists.transform(experiment.getTraces(),
                t -> Trace.from(t, iter.next())));
        return new Experiment(experiment.getName(),
                expUUID,
                experiment.getNbEvents(),
                experiment.getStartTime().toNanos(),
                experiment.getEndTime().toNanos(),
                experiment.isIndexing() ? "RUNNING" : "COMPLETED",
                traces);
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

    /**
     * Returns the traces
     * @return the traces
     */
    public Set<Trace> getTraces() {
        return fTraces;
    }

    @Override
    public String toString() {
        return "Experiment [fName=" + fName + ", fUUID=" + fUUID + ", fNbEvents=" + fNbEvents + ", fStart=" + fStart + ", fEnd=" + fEnd + ", fIndexingStatus=" + fIndexingStatus + ", fTraces=" + fTraces + "]";
    }
}