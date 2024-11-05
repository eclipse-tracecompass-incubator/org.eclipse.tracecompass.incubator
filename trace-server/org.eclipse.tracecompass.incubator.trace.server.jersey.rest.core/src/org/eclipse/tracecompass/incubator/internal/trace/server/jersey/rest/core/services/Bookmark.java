/*******************************************************************************
 * Copyright (c) 2024 Ericsson
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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Bookmark model for TSP
 *
 * @author Kaveh Shahedi
 * @since 10.1
 */
public class Bookmark implements Serializable {

    private static final long serialVersionUID = 6126770413230064175L;

    private final UUID fUUID;
    private final String fName;
    private final long fStart;
    private final long fEnd;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param uuid
     *            the stub's UUID
     * @param name
     *            bookmark name
     * @param start
     *            start time
     * @param end
     *            end time
     */
    @JsonCreator
    public Bookmark(
            @JsonProperty("uuid") UUID uuid,
            @JsonProperty("name") String name,
            @JsonProperty("start") long start,
            @JsonProperty("end") long end) {
        fUUID = uuid;
        fName = name;
        fStart = start;
        fEnd = end;
    }

    /**
     * Get the UUID
     *
     * @return the UUID
     */
    public UUID getUUID() {
        return fUUID;
    }

    /**
     * Get the bookmark name
     *
     * @return the bookmark name
     */
    public String getName() {
        return fName;
    }

    /**
     * Get the start time
     *
     * @return the start time
     */
    public long getStart() {
        return fStart;
    }

    /**
     * Get the end time
     *
     * @return the end time
     */
    public long getEnd() {
        return fEnd;
    }

    @Override
    public String toString() {
        return "Bookmark [fUUID=" + fUUID + ", fName=" + fName //$NON-NLS-1$ //$NON-NLS-2$
               + ", fStart=" + fStart + ", fEnd=" + fEnd + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}