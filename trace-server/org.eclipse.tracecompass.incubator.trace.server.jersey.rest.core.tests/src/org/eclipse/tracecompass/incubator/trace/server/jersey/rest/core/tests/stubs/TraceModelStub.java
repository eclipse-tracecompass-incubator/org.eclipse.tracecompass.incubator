/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Basic Implementation of the serialized trace model object used by clients.
 * Equality of two stubs is determined by equality of names, paths and
 * {@link UUID}, as the start time, end time and number of events may be unknown
 * due to incomplete indexing.
 *
 * @author Loic Prieur-Drevon
 */
public class TraceModelStub extends AbstractModelStub {

    /**
     * Generated Serial Version UID
     */
    private static final long serialVersionUID = -1030854786688167776L;

    private final String fPath;

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
     */
    @JsonCreator
    public TraceModelStub(
            @JsonProperty("name") String name,
            @JsonProperty("path") String path,
            @JsonProperty("UUID") UUID uuid,
            @JsonProperty("nbEvents") long nbEvents,
            @JsonProperty("start") long start,
            @JsonProperty("end") long end,
            @JsonProperty("indexingStatus") String indexingStatus) {
        super(name, uuid, nbEvents, start, end, indexingStatus);
        fPath = path;
    }

    /**
     * Constructor for comparing equality
     *
     * @param name
     *            trace name
     * @param path
     *            path to trace on server file system
     */
    public TraceModelStub(String name, String path) {
        this(name, path, getUUID(path, name), 0, 0L, 0L, "RUNNING");
    }

    private static UUID getUUID(String path, String name) {
        return UUID.nameUUIDFromBytes(Objects.requireNonNull((path + name).getBytes(Charset.defaultCharset())));
    }

    /**
     * Getter for the path to the trace on the server's file system
     *
     * @return path
     */
    public String getPath() {
        return fPath;
    }

    @Override
    public String toString() {
        return getName() + ":<path=" + fPath + ", UUID=" + getUUID() + '>'; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fPath);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        } else if (obj instanceof TraceModelStub) {
            TraceModelStub other = (TraceModelStub) obj;
            return Objects.equals(fPath, other.fPath);
        }
        return false;
    }
}
