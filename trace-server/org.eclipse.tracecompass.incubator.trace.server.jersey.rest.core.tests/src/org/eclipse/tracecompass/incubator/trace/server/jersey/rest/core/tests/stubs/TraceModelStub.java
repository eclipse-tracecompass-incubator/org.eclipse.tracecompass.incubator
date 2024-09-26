/*******************************************************************************
 * Copyright (c) 2018, 2024 Ericsson
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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

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
    private final Map<String, String> fProperties;

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
    public TraceModelStub(
            @JsonProperty("name") String name,
            @JsonProperty("path") String path,
            @JsonProperty("UUID") UUID uuid,
            @JsonProperty("nbEvents") long nbEvents,
            @JsonProperty("start") long start,
            @JsonProperty("end") long end,
            @JsonProperty("properties") Map<String, String> properties,
            @JsonProperty("indexingStatus") String indexingStatus) {
        super(name, uuid, nbEvents, start, end, indexingStatus);
        fPath = path;
        fProperties = properties;
    }

    /**
     * Constructor for comparing equality
     *
     * @param name
     *            trace name
     * @param path
     *            path to trace on server file system
     * @param properties
     *            properties of the trace
     */
    public TraceModelStub(String name, String path, Map<String, String> properties) {
        this(name, path, getUUID(path, name), 0, 0L, 0L, properties, "RUNNING");
    }

    private static UUID getUUID(String path, String name) {
        IPath tracePath = new Path(path).append(name);
        return UUID.nameUUIDFromBytes(Objects.requireNonNull(tracePath.toString().getBytes(Charset.defaultCharset())));
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
     * Returns the trace's properties
     * @return the trace's properties
     */
    public Map<String, String> getProperties() {
        return fProperties;
    }

    @Override
    public String toString() {
        return getName() + ": <UUID=" + getUUID() + ", path=" + fPath + '>'; //$NON-NLS-1$ //$NON-NLS-2$
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
            return Objects.equals(fPath, other.fPath) && Objects.equals(fProperties, other.fProperties);
        }
        return false;
    }
}
