/*******************************************************************************
 * Copyright (c) 2023 Ericsson
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
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Basic Implementation of the serialized ITmfConfiguration model used by clients.
 *
 * @author Bernd Hufmann
 */
public class TmfConfigurationStub implements Serializable, ITmfConfiguration {

    /**
     * Generated Serial Version UID
     */
    private static final long serialVersionUID = 6934234848155424428L;
    private final String fId;
    private final String fName;
    private final String fDescription;
    private final String fSourceTypeId;
    private final Map<String, Object> fParameters;
    private final JsonNode fJsonParameters;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param id
     *            the id
     * @param name
     *            the name
     * @param description
     *            the help text
     * @param type
     *            the type id
     * @param parameters
     *            the parameters
     *
     */
    @JsonCreator
    public TmfConfigurationStub(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("sourceTypeId") String type,
            @JsonProperty("parameters") Map<String, Object> parameters,
            @JsonProperty("jsonParameters") JsonNode jsonParameters) {
        super();
        fId = id;
        fName = name;
        fDescription = description;
        fSourceTypeId = type;
        fParameters = parameters;
        fJsonParameters = jsonParameters;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fId, fDescription, fSourceTypeId, fJsonParameters);
    }

    @Override
    public boolean equals(Object arg0) {
        if (!(arg0 instanceof TmfConfigurationStub)) {
            return false;
        }
        TmfConfigurationStub other = (TmfConfigurationStub) arg0;
        return Objects.equals(fName, other.fName) && Objects.equals(fId, other.fId)
                && Objects.equals(fSourceTypeId, other.fSourceTypeId) && Objects.equals(fDescription, other.fDescription)
                && Objects.equals(fJsonParameters, other.fJsonParameters);
    }

    @Override
    public @NonNull String getName() {
        return fName;
    }

    @Override
    public @NonNull String getId() {
        return fId;
    }

    @Override
    public @NonNull String getDescription() {
        return fDescription;
    }

    @Override
    public @NonNull String getSourceTypeId() {
        return fSourceTypeId;
    }

    @Override
    public @NonNull Map<@NonNull String, @NonNull Object> getParameters() {
        return fParameters;
    }

    @Override
    public @NonNull String getJsonParameters() {
        return fJsonParameters == null ? "" :fJsonParameters.toString();
    }
}
