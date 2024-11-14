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

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Stub class for the bookmark model. It matches the trace server protocol's
 * <code>BookmarkModel</code> schema
 *
 * @author Kaveh Shahedi
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookmarkModelStub implements Serializable {
    private static final long serialVersionUID = -1945923534635091200L;

    private final UUID fUUID;
    private final String fName;
    private final long fStart;
    private final long fEnd;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param uuid
     *            The bookmark's UUID
     * @param name
     *            The bookmark name
     * @param start
     *            The start time
     * @param end
     *            The end time
     */
    @JsonCreator
    public BookmarkModelStub(
            @JsonProperty("uuid") UUID uuid,
            @JsonProperty("name") String name,
            @JsonProperty("start") long start,
            @JsonProperty("end") long end) {
        fUUID = Objects.requireNonNull(uuid, "The 'UUID' json field was not set");
        fName = Objects.requireNonNull(name, "The 'name' json field was not set");
        fStart = start;
        fEnd = end;
    }

    /**
     * Constructor for comparing equality
     *
     * @param name
     *            bookmark name
     * @param start
     *           start time
     * @param end
     *          end time
     */
    public BookmarkModelStub(String name, long start, long end) {
        this(UUID.randomUUID(), name, start, end);
    }

    /**
     * Get the UUID
     *
     * @return The UUID
     */
    public UUID getUUID() {
        return fUUID;
    }

    /**
     * Get the bookmark name
     *
     * @return The bookmark name
     */
    public String getName() {
        return fName;
    }

    /**
     * Get the start time
     *
     * @return The start time
     */
    public long getStart() {
        return fStart;
    }

    /**
     * Get the end time
     *
     * @return The end time
     */
    public long getEnd() {
        return fEnd;
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
        BookmarkModelStub other = (BookmarkModelStub) obj;
        if (fEnd != other.fEnd) {
            return false;
        }
        if (fName == null) {
            if (other.fName != null) {
                return false;
            }
        } else if (!fName.equals(other.fName)) {
            return false;
        }
        if (fStart != other.fStart) {
            return false;
        }
        if (fUUID == null) {
            if (other.fUUID != null) {
                return false;
            }
        } else if (!fUUID.equals(other.fUUID)) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        return "BookmarkModelStub [fUUID=" + fUUID + ", fName=" + fName + ", fStart=" + fStart + ", fEnd=" + fEnd + "]";
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
