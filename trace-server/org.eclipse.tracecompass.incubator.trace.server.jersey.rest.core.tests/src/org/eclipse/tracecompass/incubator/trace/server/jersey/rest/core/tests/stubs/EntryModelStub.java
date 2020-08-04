/**********************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs;

import java.io.Serializable;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Stub class for the entry model. It matches the trace server protocol's
 * <code>EntryModel</code> schema
 *
 * FIXME: Cannot remove the JsonIgnoreProperties because the method used to
 * custom serialize does not allow to serialize generic types, so we use the
 * default serialization which adds extra fields
 *
 * @author Geneviève Bastien
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntryModelStub implements Serializable {

    private static final long serialVersionUID = 6027193074532379770L;

    private final Set<EntryStub> fEntries;
    private final Set<EntryHeaderStub> fHeaders;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param entries
     *            The set of entries for this model
     * @param headers
     *            The set of headers for this model
     */
    @JsonCreator
    public EntryModelStub(@JsonProperty("entries") Set<EntryStub> entries,
            @JsonProperty("headers") Set<EntryHeaderStub> headers) {
        fEntries = Objects.requireNonNull(entries, "The 'entries' json field was not set");
        fHeaders = headers == null ? Collections.emptySet() : headers;
    }

    /**
     * Get the entries described by this model
     *
     * @return The entries in this model
     */
    public Set<EntryStub> getEntries() {
        return fEntries;
    }

    /**
     * Get the headers that describe this model
     *
     * @return The headers in this model
     */
    public Set<EntryHeaderStub> getHeaders() {
        return fHeaders;
    }

}
