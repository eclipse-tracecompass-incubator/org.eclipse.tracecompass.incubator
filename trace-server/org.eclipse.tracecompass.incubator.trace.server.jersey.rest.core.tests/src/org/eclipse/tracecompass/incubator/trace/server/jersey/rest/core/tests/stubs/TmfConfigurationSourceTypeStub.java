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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigParamDescriptor;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigurationSourceType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Basic Implementation of the serialized {@link ITmfConfigurationSourceType} model used by clients.
 *
 * @author Bernd Hufmann
 */
public class TmfConfigurationSourceTypeStub implements Serializable, ITmfConfigurationSourceType {

    /**
     * Generated Serial Version UID
     */
    private static final long serialVersionUID = 6934234848155424428L;
    private final ITmfConfigurationSourceType fConfig;
    private final JsonNode fSchema;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param id
     *            the id
     * @param name
     *            the name
     * @param description
     *            the help text
     * @param schema
     *            the JSON schema JsonNode
     * @param parameterDescriptors
     *            the list of keys
     *
     */
    @SuppressWarnings("null")
    @JsonCreator
    public TmfConfigurationSourceTypeStub(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("schema") JsonNode schema,
            @JsonProperty("parameterDescriptors") List<ConfigParamDescriptorStub> parameterDescriptors) {
        super();

        TmfConfigurationSourceType.Builder builder = new TmfConfigurationSourceType.Builder()
                .setDescription(description)
                .setName(name)
                .setId(id);
        if (parameterDescriptors != null) {
            builder.setConfigParamDescriptors(parameterDescriptors.stream().map(stub -> stub.getConfig()).collect(Collectors.toList()));
        }
        fConfig = builder.build();
        fSchema = schema;
    }

    /**
     * @return the JSON schema string
     */
    public JsonNode getSchema() {
        return fSchema;
    }

    ITmfConfigurationSourceType getConfig() {
        return fConfig;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fConfig, fSchema == null ? "null" : fSchema);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TmfConfigurationSourceTypeStub)) {
            return false;
        }
        TmfConfigurationSourceTypeStub other = (TmfConfigurationSourceTypeStub) obj;
        return Objects.equals(fConfig, other.fConfig) && Objects.equals(fSchema, other.fSchema);
    }

    @Override
    public @NonNull String getName() {
        return fConfig.getName();
    }

    @Override
    public @NonNull String getId() {
        return fConfig.getId();
    }

    @Override
    public @NonNull String getDescription() {
        return fConfig.getDescription();
    }

    @Override
    public @NonNull List<@NonNull ITmfConfigParamDescriptor> getConfigParamDescriptors() {
        return fConfig.getConfigParamDescriptors();
    }
}
