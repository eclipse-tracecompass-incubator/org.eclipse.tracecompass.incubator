/**********************************************************************
 * Copyright (c) 2021 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.core.mpi;

import java.util.Collection;

import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * A class representing a root to all MPI communication state : the
 * communication is defined by thee communicator, the id of the root rank and
 * the state of the communication depends on the pending ranks
 *
 * @author Yoann Heitz
 *
 */
public class RootToAllIdentifiers extends CollectiveOperationIdentifiers{

    private final ITmfEvent fBeginEvent;


    /**
     * Constructor for this class
     * @param operationCode
     *            the identifier of the communication routine
     * @param communicator
     *            the id of the communicator
     * @param rootLocationId
     *            the id of the location that is the root for the communication
     * @param beginEvent
     *            the event where the root executed the MPI routine
     * @param pendingLocations
     *            the locations that did not call the MPI routine for the
     *            communication
     */
    public RootToAllIdentifiers(int operationCode, int communicator, long rootLocationId, ITmfEvent beginEvent, Collection<Long> pendingLocations) {
        super(operationCode, communicator, rootLocationId, pendingLocations);
        fBeginEvent = beginEvent;
    }

    /**
     * Get the event where the root finished the associated MPI routine
     *
     * @return the corresponding event
     */
    public ITmfEvent getBeginEvent() {
        return fBeginEvent;
    }

    /**
     * Get the timestamp when the root finished the associated MPI routine
     *
     * @return the corresponding timestamp
     */
    public long getBeginTimestamp() {
        return fBeginEvent.getTimestamp().toNanos();
    }
}