/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.model;

import java.util.Objects;

/**
 * Event type record
 *
 * @param name
 *            the name of the event type
 */
public class EventType{
    private String fName;

    public EventType(String name) {
        setName(name);
}

    /**
     * @return the name
     */
    public String getName() {
        return fName;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        fName = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        EventType other = (EventType) obj;
        return Objects.equals(fName, other.fName);
    }
}
