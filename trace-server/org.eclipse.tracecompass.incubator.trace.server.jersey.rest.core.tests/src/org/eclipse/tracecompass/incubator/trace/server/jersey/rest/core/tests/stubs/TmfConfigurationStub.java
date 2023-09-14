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
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    private final ITmfConfiguration fConfig;

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
    @SuppressWarnings("null")
    @JsonCreator
    public TmfConfigurationStub(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("sourceTypeId") String type,
            @JsonProperty("parameters") Map<String, Object> parameters) {
        super();

        TmfConfiguration.Builder builder = new TmfConfiguration.Builder()
                .setDescription(description)
                .setName(name)
                .setId(id)
                .setSourceTypeId(type);
        if (parameters != null) {
            builder.setParameters(parameters);
        }
        fConfig = builder.build();
    }


    ITmfConfiguration getConfig() {
        return fConfig;
    }

    @Override
    public int hashCode() {
        return fConfig.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof TmfConfigurationStub) {
            return Objects.equals(this.getConfig(), ((TmfConfigurationStub) obj).getConfig());
        }
        if (obj instanceof TmfConfiguration) {
            return Objects.equals(this.getConfig(), obj);
        }
        return false;
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
    public @NonNull String getSourceTypeId() {
        return fConfig.getSourceTypeId();
    }

    @Override
    public @NonNull Map<@NonNull String, @NonNull Object> getParameters() {
        return fConfig.getParameters();
    }
}
