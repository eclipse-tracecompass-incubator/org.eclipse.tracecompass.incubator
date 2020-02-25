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
    private final String fId;
    private final String fName;
    private final String fDescription;
    private final String fTypeId;

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
     *
     */
    @JsonCreator
    public DataProviderDescriptorStub(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("type") String type) {
        fId = id;
        fName = name;
        fDescription = description;
        fTypeId = type;
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

    @Override
    public String toString() {
        return "DataProviderDescriptorStub[fId=" + getId() + ", fName=" + fName + ", fDescription=" + fDescription
                + ", fTypeId=" + fTypeId+ "]";
        }

    @Override
    public int hashCode() {
        return Objects.hash(fId, fName, fDescription, fTypeId);
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
            if (!Objects.equals(fId, other.fId)) {
                return false;
            }
            if (!Objects.equals(fName, other.fName)) {
                return false;
            }
            if (!Objects.equals(fDescription, other.fDescription)) {
                return false;
            }
            if (Objects.equals(fTypeId, other.fTypeId)) {
                return true;
            }
        }
        return false;
    }
}
