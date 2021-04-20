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

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * A class representing an all to root MPI communication state : the
 * communication is defined by the communicator id, the id of the root rank and
 * the state of the communication depends on the pending ranks
 *
 * @author Yoann Heitz
 *
 */
public class AllToRootIdentifiers extends CollectiveOperationIdentifiers {

    private final Collection<ITmfEvent> fBeginEvents;

    /**
     * @param operationCode
     *            the identifier of the MPI routine
     * @param communicator
     *            reference of the communicator
     * @param rootLocationId
     *            location ID for the root
     * @param pendingLocations
     *            list of locations that need to call the routine
     */
    public AllToRootIdentifiers(int operationCode, int communicator, long rootLocationId, Collection<Long> pendingLocations) {
        super(operationCode, communicator, rootLocationId, pendingLocations);
        fBeginEvents = new ArrayList<>();
    }

    /**
     * Update the state of the communication when a pending location called the
     * expected MPI routine
     *
     * @param locationId
     *            the location that called the MPI routine
     * @param beginEvent
     *            the event representing the moment when the location entered
     *            the routine
     */
    public void locationCalledOperation(long locationId, ITmfEvent beginEvent) {
        super.locationCalledOperation(locationId, beginEvent.getTimestamp().toNanos());
        fBeginEvents.add(beginEvent);
    }

    /**
     * Get the enter into collective routine events for all the locations in the
     * communicator
     *
     * @return the list of the events
     */
    public Collection<ITmfEvent> getBeginEvents() {
        return fBeginEvents;
    }
}