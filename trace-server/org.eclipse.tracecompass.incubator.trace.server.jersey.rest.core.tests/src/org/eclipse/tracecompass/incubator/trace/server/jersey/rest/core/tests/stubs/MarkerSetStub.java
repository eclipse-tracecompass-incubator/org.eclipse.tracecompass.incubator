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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Basic Implementation of the serialized marker set model used by clients.
 *
 * @author Bernd Hufmann
 */
public class MarkerSetStub implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * Generated Serial Version UID
     */
    private final String fId;
    private final String fName;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param id
     *            the id
     * @param name
     *            the name
     */
    @JsonCreator
    public MarkerSetStub(@JsonProperty("id") String id,
            @JsonProperty("name") String name) {
        fId = id;
        fName = name;
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

    @Override
    public String toString() {
        return "MarkerSetStub[fId=" + getId() + ", fName=" + fName + "]";
        }

    @Override
    public int hashCode() {
        return Objects.hash(fId, fName);
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }

        if (obj instanceof MarkerSetStub) {
            MarkerSetStub other = (MarkerSetStub) obj;
            if (!Objects.equals(fId, other.fId)) {
                return false;
            }
            if (!Objects.equals(fName, other.fName)) {
                return false;
            }
        }
        return false;
    }
}
