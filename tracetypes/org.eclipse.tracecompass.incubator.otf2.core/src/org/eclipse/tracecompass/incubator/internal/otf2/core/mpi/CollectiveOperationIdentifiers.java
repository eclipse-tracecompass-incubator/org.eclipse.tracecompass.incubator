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

/**
 * A class representing a collective MPI communication and its state
 *
 * @author Yoann Heitz
 *
 */
public class CollectiveOperationIdentifiers {

    private static final int UNDEFINED_ROOT_ID = -1;
    private final int fOperationCode;
    private final int fCommunicator;
    private final long fRootLocationId;
    private final Collection<Long> fPendingLocations;
    private final Collection<Long> fEnterTimestamps;

    /**
     * @param operationCode
     *            the identifier of the communication routine
     * @param communicator
     *            the id of the communicator
     * @param rootLocationId
     *            the id of the location that is the root for the communication
     * @param pendingLocations
     *            the locations that did not call the MPI routine for the
     *            communication
     */
    public CollectiveOperationIdentifiers(int operationCode, int communicator, long rootLocationId, Collection<Long> pendingLocations) {
        fOperationCode = operationCode;
        fCommunicator = communicator;
        fRootLocationId = rootLocationId;
        fPendingLocations = pendingLocations;
        fEnterTimestamps = new ArrayList<>();
    }

    /**
     * This constructor should be used when there is not a particular root in
     * the communication
     *
     * @param operationCode
     *            the identifier of the communication routine
     * @param communicator
     *            the id of the communicator
     * @param pendingLocations
     *            the locations that did not call the MPI routine for the
     *            communication
     */
    public CollectiveOperationIdentifiers(int operationCode, int communicator, Collection<Long> pendingLocations) {
        this(operationCode, communicator, UNDEFINED_ROOT_ID, pendingLocations);
    }

    /**
     * Checks whether a communication identified by the arguments is part of
     * this collective communication
     *
     * @param operationCode
     *            the identifier of the MPI routine
     * @param rootLocationId
     *            the location ID of the communication root
     * @param communicator
     *            the communicator ID of the communication
     * @param srcLocationId
     *            the ID of the location that made this communication
     * @return a boolean indicating if the communication is associated
     */
    public boolean isAssociatedOperation(int operationCode, long rootLocationId, int communicator, long srcLocationId) {
        return operationCode == fOperationCode && rootLocationId ==  fRootLocationId && communicator ==  fCommunicator &&  fPendingLocations.contains(srcLocationId);
    }

    /**
     * Checks whether a communication identified by the arguments is part of
     * this collective communication. This method should be used for all to all
     * communications only
     *
     * @param operationCode
     *            the identifier of the MPI routine
     * @param communicator
     *            the communicator ID of the communication
     * @param srcLocationId
     *            the ID of the location that made this communication
     * @return a boolean indicating if the communication is associated
     */
    public boolean isAssociatedOperation(int operationCode, int communicator, long srcLocationId) {
        return isAssociatedOperation(operationCode, UNDEFINED_ROOT_ID, communicator, srcLocationId);
    }

    /**
     * Checks if the communication is finished (all the locations called the MPI
     * routine)
     *
     * @return a boolean
     */
    public boolean isOperationDone() {
        return fPendingLocations.isEmpty();
    }

    /**
     * Updates the state of the communication when a location called the routine
     *
     * @param locationId
     *            the ID of the location calling the routine
     * @param timestamp
     *            the timestamp when the call was done
     */
    public void locationCalledOperation(long locationId, long timestamp) {
        fPendingLocations.remove(locationId);
        fEnterTimestamps.add(timestamp);
    }

    /**
     * @return the list of timestamps when locations called the routine
     */
    public Collection<Long> getEnterTimestamps() {
        return fEnterTimestamps;
    }
}
