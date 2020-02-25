/*******************************************************************************
 * Copyright (c) 2018 Ericsson
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

/**
 * Abstract Stub to let the traces and experiments share code.
 *
 * @author Loic Prieur-Drevon
 */
public abstract class AbstractModelStub implements Serializable {
    /**
     * Generated Serial Version UID
     */
    private static final long serialVersionUID = 8456397901233666237L;
    private final String fName;
    private final UUID fUUID;
    private final long fNbEvents;
    private final long fStart;
    private final long fEnd;
    private final String fIndexingStatus;

    /**
     * Constructor
     *
     * @param name
     *            this model's name
     * @param uuid
     *            this model's unique ID
     * @param nbEvents
     *            the current number of events in the model
     * @param start
     *            the current start time
     * @param end
     *            the current end time
     */
    public AbstractModelStub(String name, UUID uuid, long nbEvents, long start, long end, String indexingStatus) {
        fName = name;
        fUUID = uuid;
        fNbEvents = nbEvents;
        fStart = start;
        fEnd = end;
        fIndexingStatus = indexingStatus;
    }

    /**
     * Getter for this model's name
     *
     * @return this model's name
     */
    public String getName() {
        return fName;
    }

    /**
     * Getter for this model's {@link UUID}
     *
     * @return get this model's UUID
     */
    public UUID getUUID() {
        return fUUID;
    }

    /**
     * Getter for the current number of indexed events in the model
     *
     * @return the current number of indexed events in this model
     */
    public long getNbEvents() {
        return fNbEvents;
    }

    /**
     * Getter for the current start time of the model
     *
     * @return this model's current start time
     */
    public long getStart() {
        return fStart;
    }

    /**
     * Getter for the current end time for this model
     *
     * @return this model's current end time
     */
    public long getEnd() {
        return fEnd;
    }

    public String getIndexingStatus() {
        return fIndexingStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fName, fUUID);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj instanceof AbstractModelStub) {
            AbstractModelStub other = (AbstractModelStub) obj;
            return fName.equals(other.fName) && fUUID.equals(other.fUUID);
        }
        return false;
    }

}