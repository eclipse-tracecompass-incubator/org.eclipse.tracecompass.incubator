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
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigParamDescriptor;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigParamDescriptor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Basic Implementation of the serialized {@link ITmfConfigParamDescriptor} model used by clients.
 *
 * @author Bernd Hufmann
 */
public class ConfigParamDescriptorStub implements Serializable, ITmfConfigParamDescriptor {

    /**
     * Generated Serial Version UID
     */
    private static final long serialVersionUID = -5864600863726070552L;
    private final ITmfConfigParamDescriptor fConfig;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param keyName
     *            the key name
     * @param dataType
     *            the dataType string
     * @param description
     *            the help text
     * @param isRequired
     *            the isRequired flag
     *
     */
    @SuppressWarnings("null")
    @JsonCreator
    public ConfigParamDescriptorStub(@JsonProperty("keyName") String keyName,
            @JsonProperty("dataType") String dataType,
            @JsonProperty("description") String description,
            @JsonProperty("required") Boolean isRequired) {
        super();

        TmfConfigParamDescriptor.Builder builder = new TmfConfigParamDescriptor.Builder()
                .setKeyName(keyName);
        if (dataType != null) {
            builder.setDataType(dataType);
        }
        if (description != null) {
            builder.setDescription(description);
        }
        if (isRequired != null) {
            builder.setIsRequired(isRequired);
        }
        fConfig = builder.build();
    }


    ITmfConfigParamDescriptor getConfig() {
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
        if (obj instanceof ConfigParamDescriptorStub) {
            return Objects.equals(this.getConfig(), ((ConfigParamDescriptorStub) obj).getConfig());
        }
        if (obj instanceof TmfConfigParamDescriptor) {
            return Objects.equals(this.getConfig(), obj);
        }
        return false;
    }

    @Override
    public @NonNull String getKeyName() {
        return fConfig.getKeyName();
    }

    @Override
    public @NonNull String getDataType() {
        return fConfig.getDataType();
    }

    @Override
    public @NonNull String getDescription() {
        return fConfig.getDescription();
    }

    @Override
    public boolean isRequired() {
        return fConfig.isRequired();
    }
}
