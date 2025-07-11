/*******************************************************************************
 * Copyright (c) 2019 Ericsson
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
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Basic Implementation of the serialized data descriptor model used by clients.
 *
 * TODO Equality of two stubs is determined by equality of names, paths and
 * {@link UUID}, as the start time, end time and number of events may be unknown
 * due to incomplete indexing.
 *
 * @author Bernd Hufmann
 */
public class DataProviderDescriptorStub implements Serializable {

    /**
     * Generated Serial Version UID
     */
    private static final long serialVersionUID = -6380168132081665386L;
    private final String fParentId;
    private final String fId;
    private final String fName;
    private final String fDescription;
    private final String fTypeId;
    private final TmfConfigurationStub fConfiguration;
    private final TmfCapabilitiesStub fCapabilities;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param parentId
     *            the parentId
     * @param id
     *            the id
     * @param name
     *            the name
     * @param description
     *            the help text
     * @param type
     *            the type id
     * @param configuration
     *            the configuration
     * @param capabilities
     *            the data provider capabilities
     *
     */
    @JsonCreator
    public DataProviderDescriptorStub(@JsonProperty("parentId") String parentId,
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("type") String type,
            @JsonProperty("configuration") TmfConfigurationStub configuration,
            @JsonProperty("capabilities") TmfCapabilitiesStub capabilities) {
        fParentId = parentId;
        fId = id;
        fName = name;
        fDescription = description;
        fTypeId = type;
        fConfiguration = configuration;
        fCapabilities = capabilities;
    }

    /**
     * Gets the parent ID
     *
     * @return the parent ID
     */
    public String getParentId() {
        return fParentId;
    }

    /**
     * Gets the ID
     *
     * @return the ID
     */
    public String getId() {
        return fId;
    }

    /**
     * Gets the name
     *
     * @return the name
     */
    public String getName() {
        return fName;
    }

    /**
     * Gets the help text
     *
     * @return the help text
     */
    public String getDescription() {
        return fDescription;
    }

    /**
     * Gets the type ID
     *
     * @return the type ID
     */
    public String getTypeId() {
        return fTypeId;
    }

    /**
     * Gets the configuration
     *
     * @return the type ID
     */
    public TmfConfigurationStub getConfiguration() {
        return fConfiguration;
    }

    /**
     * Gets the capabilities
     *
     * @return the capabilities
     */
    public TmfCapabilitiesStub getCapabilities() {
        return fCapabilities;
    }

    @Override
    public String toString() {
        return "DataProviderDescriptorStub[fParentId=" + getParentId() + ", fId=" + getId() + ", fName=" + fName + ", fDescription=" + fDescription
                + ", fTypeId=" + fTypeId + ", fConfiguration=" + getConfiguration() + ", fCapabilities=" + getCapabilities() + "]";
        }

    @Override
    public int hashCode() {
        return Objects.hash(fParentId, fId, fName, fDescription, fTypeId, fConfiguration, fCapabilities);
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }

        if (obj instanceof DataProviderDescriptorStub) {
            DataProviderDescriptorStub other = (DataProviderDescriptorStub) obj;
            if (!Objects.equals(fParentId, other.fParentId)) {
                return false;
            }
            if (!Objects.equals(fId, other.fId)) {
                return false;
            }
            if (!Objects.equals(fName, other.fName)) {
                return false;
            }
            if (!Objects.equals(fDescription, other.fDescription)) {
                return false;
            }
            if (!Objects.equals(fTypeId, other.fTypeId)) {
                return false;
            }
            if (Objects.equals(fConfiguration, other.fConfiguration)) {
                return true;
            }
            if (!Objects.equals(fCapabilities, other.fCapabilities)) {
                return false;
            }
        }
        return false;
    }
}
