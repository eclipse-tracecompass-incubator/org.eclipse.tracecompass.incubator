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

package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.communicators;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.AbstractOtf2StateProvider;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Constants;
import org.eclipse.tracecompass.incubator.internal.otf2.core.mpi.CollectiveOperationIdentifiers;
import org.eclipse.tracecompass.incubator.internal.otf2.core.mpi.MessageIdentifiers;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;

import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * State provider for the OTF2 communicators analysis
 *
 * @author Yoann Heitz
 */

public class Otf2CommunicatorsStateProvider extends AbstractOtf2StateProvider {

    /**
     * Communicator
     */
    private static final String COMMUNICATOR = "Communicator "; //$NON-NLS-1$

    /**
     * Rank
     */
    private static final String RANK = "Rank "; //$NON-NLS-1$
    private static int VERSION_NUMBER = 1;

    /**
     * A class representing a location and its state in this analysis.
     *
     * @author Yoann Heitz
     */
    private class Location {

        private final long fId;
        private Communicator fCurrentCommunicator;
        private String fLatestEnteredRegion;
        private long fLatestEnteredTimestamp;
        private boolean fInCommunication;
        private int fRank;
        private final List<MessageIdentifiers> fSentMessages;
        private final Map<Long, Long> fRequestIdBeginTimestamp;

        public Location(long id) {
            fId = id;
            fCurrentCommunicator = new Communicator(UNKNOWN_RANK);
            fLatestEnteredRegion = IOtf2Constants.UNKNOWN_STRING;
            fLatestEnteredTimestamp = 0L;
            fInCommunication = false;
            fRank = UNKNOWN_RANK;
            fSentMessages = new ArrayList<>();
            fRequestIdBeginTimestamp = new HashMap<>();
        }

        /**
         * This method is called when a location enters a region and updates the
         * associated fields
         *
         * @param event
         *            the ITmfEvent associated to the OTF2 EnterRegion event
         */
        public void enter(ITmfEvent event) {
            ITmfEventField content = event.getContent();
            fLatestEnteredTimestamp = event.getTimestamp().toNanos();
            Integer regionId = content.getFieldValue(Integer.class, IOtf2Constants.OTF2_REGION_REFERENCE);
            if (regionId == null) {
                return;
            }
            String regionName = getRegionNameFromRegionId(regionId);
            fLatestEnteredRegion = regionName;
        }

        /**
         * This method is called when a location leaves a region associated to a
         * MPI Communication and updates state of the attribute associated to
         * the location
         *
         * @param event
         *            the ITmfEvent associated to the OTF2 LeaveRegion event
         * @param ssb
         *            the StateSystemBuilder used to update the state of the
         *            attribute
         */
        public void leaveMpiCommunication(ITmfEvent event, ITmfStateSystemBuilder ssb) {
            int communicatorQuark = fCurrentCommunicator.fQuark;
            int associatedLocationQuark = ssb.getQuarkRelativeAndAdd(communicatorQuark, RANK + String.valueOf(fRank));
            ssb.modifyAttribute(fLatestEnteredTimestamp, fLatestEnteredRegion, associatedLocationQuark);
            ssb.modifyAttribute(event.getTimestamp().toNanos(), null, associatedLocationQuark);
            fInCommunication = false;
        }

        public void mpiSend(ITmfEvent srcEvent, Communicator communicator) {
            /*
             * If an Mpi Send (point to point communication) routine is called
             * and finished, the informations about the communication are stored
             * into an object in order to link the corresponding Mpi_Recv
             * communication when encountered.
             */
            ITmfEventField content = srcEvent.getContent();
            Integer srcRank = getRank(fId, communicator.fId);
            Integer destRank = content.getFieldValue(Integer.class, IOtf2Constants.OTF2_RECEIVER);
            Integer messageTag = content.getFieldValue(Integer.class, IOtf2Constants.OTF2_MESSAGE_TAG);
            if (destRank == null || messageTag == null || srcRank == UNKNOWN_RANK) {
                return;
            }
            fSentMessages.add(new MessageIdentifiers(communicator.fId, srcRank, destRank, messageTag, srcEvent.getTimestamp().toNanos()));
            fCurrentCommunicator = communicator;
            fRank = srcRank;
        }

        /*
         * This method searches for the associated Send event, updates the state
         * of the location and stores the changes for the associated
         * communicator attribute and their timestamps.
         */
        public void mpiRecv(ITmfEvent srcEvent, Communicator communicator, boolean isBlocking) {
            ITmfEventField content = srcEvent.getContent();
            Integer destRank = getRank(fId, communicator.fId);
            Integer srcRank = content.getFieldValue(Integer.class, IOtf2Constants.OTF2_SENDER);
            Integer messageTag = content.getFieldValue(Integer.class, IOtf2Constants.OTF2_MESSAGE_TAG);
            if (srcRank == null || messageTag == null || destRank == UNKNOWN_RANK) {
                return;
            }

            Location srcLocation = fMapLocation.get(communicator.fLocations.get(srcRank));
            if (srcLocation == null) {
                return;
            }
            // Searches for the associated sent message
            MessageIdentifiers message = new MessageIdentifiers(communicator.fId, srcRank, destRank, messageTag);
            int messageIndex = srcLocation.fSentMessages.indexOf(message);
            MessageIdentifiers sentMessage = srcLocation.fSentMessages.remove(messageIndex);

            Long recvBeginTimestamp = fLatestEnteredTimestamp;
            /*
             * If this is a non blocking receive then the current code region is
             * the one associated to the MPI test and not to the Send routine.
             * The associated send begin timestamp must be retrieved
             */
            if (!isBlocking) {
                Long requestId = content.getFieldValue(Long.class, IOtf2Constants.OTF2_REQUEST_ID);
                if (requestId == null) {
                    return;
                }
                Long requestBeginTimestamp = fRequestIdBeginTimestamp.get(requestId);
                if (requestBeginTimestamp == null) {
                    return;
                }
                recvBeginTimestamp = requestBeginTimestamp;
            }

            // The changes for the communicator are stored
            Long beginCommunicatorUseTimestamp = Math.min(recvBeginTimestamp, sentMessage.getBeginTimestamp());
            // When the communication began, 2 locations were expected to use
            // the communicator
            communicator.fTimestampsPendingThreads.put(beginCommunicatorUseTimestamp, 2L);

            /*
             * When the send event was encountered, the sender finished to use
             * the communicator. It is the same for the receive event
             */
            communicator.fTimestampsPendingThreads.put(sentMessage.getBeginTimestamp(), -1L);
            communicator.fTimestampsPendingThreads.put(srcEvent.getTimestamp().toNanos(), -1L);
            fCurrentCommunicator = communicator;
            fRank = destRank;
        }

        /*
         * This methods is called when a location finished to use a communicator
         * for a collective routine
         */
        private void mpiCollective(ITmfEvent event, Communicator communicator) {
            ITmfEventField content = event.getContent();
            Integer operationCode = content.getFieldValue(Integer.class, IOtf2Constants.OTF2_COLLECTIVE_OPERATION);
            if (operationCode == null) {
                return;
            }
            Integer root = content.getFieldValue(Integer.class, IOtf2Constants.OTF2_ROOT);
            if (root == null) {
                return;
            }
            fCurrentCommunicator = communicator;
            fRank = getRank(fId, communicator.fId);
            /*
             * The associated collective operation is searched in the list of
             * ongoing collective operations in the associated communicator
             */
            for (CollectiveOperationIdentifiers collectiveOperation : communicator.fCollectiveOperations) {
                /*
                 * If the communication is found, the state of the communication
                 * and of the communicator are updated
                 */
                if (collectiveOperation.isAssociatedOperation(operationCode, root, fId)) {
                    collectiveOperation.locationCalledOperation(fId, fLatestEnteredTimestamp);
                    /*
                     * We store the change for the communicator : one less
                     * location is expected to use the communicator at this
                     * timestamp
                     */
                    communicator.fTimestampsPendingThreads.put(event.getTimestamp().toNanos(), -1L);
                    if (collectiveOperation.isOperationDone()) {
                        /*
                         * If the communication is done then we search for the
                         * moment were the first location started the
                         * communication : from this moment, the same number of
                         * locations as the number of locations in the
                         * communicator were expected to use this communicator
                         * and it is needed to store this change
                         */
                        communicator.fTimestampsPendingThreads.put(Collections.min(collectiveOperation.getEnterTimestamps()), (long) communicator.fLocations.size());
                        communicator.fCollectiveOperations.remove(collectiveOperation);
                    }
                    return;
                }

            }

            /*
             * This code is reached if no associated operation was found : it
             * means
             */
            CollectiveOperationIdentifiers collectiveOperation = new CollectiveOperationIdentifiers(operationCode, root, new ArrayList<>(communicator.fLocations));
            communicator.fCollectiveOperations.add(collectiveOperation);
            collectiveOperation.locationCalledOperation(fId, fLatestEnteredTimestamp);

            communicator.fTimestampsPendingThreads.put(event.getTimestamp().toNanos(), -1L);

            if (collectiveOperation.isOperationDone()) {
                communicator.fTimestampsPendingThreads.put(Collections.min(collectiveOperation.getEnterTimestamps()), (long) communicator.fLocations.size());
                communicator.fCollectiveOperations.remove(collectiveOperation);
            }
        }
    }

    /**
     * A private class to represent the communicators states for this analysis.
     * The fields of this class are public but the class is private and should
     * not be used outside of this State Provider
     *
     * @author Yoann Heitz
     *
     */
    private class Communicator {
        protected final int fId;
        protected boolean fQuarkInitialized;
        protected int fQuark;
        protected List<Long> fLocations;
        protected final List<CollectiveOperationIdentifiers> fCollectiveOperations;
        /*
         * Sorted map that links timestamp with changes in the number of pending
         * locations. This sorted map will be iterated over at the end of the
         * trace to update the state of the attribute associated to this
         * communicator over the time.
         */
        protected final SortedMap<Long, Long> fTimestampsPendingThreads;

        public Communicator(int id) {
            fId = id;
            fQuarkInitialized = false;
            fLocations = new ArrayList<>();
            fCollectiveOperations = new ArrayList<>();
            fQuark = -1;
            fTimestampsPendingThreads = new TreeMap<>();
        }

        /**
         * Checks if the quark associated to this communicator was initialized
         *
         * @return a boolean
         */
        public boolean isInitialized() {
            return fQuarkInitialized;
        }

        /*
         * Initialize the quark for the communicator and the quarks for each
         * location in the communicator
         */
        public void initialize(ITmfStateSystemBuilder ssb) {
            ArrayList<Long> locations = getMembersFromCommunicatorReference(fId);
            fQuark = ssb.getQuarkAbsoluteAndAdd(COMMUNICATOR + String.valueOf(fId));
            fLocations = locations;
            for (int i = 0; i < fLocations.size(); i++) {
                ssb.getQuarkRelativeAndAdd(fQuark, RANK + String.valueOf(i));
            }
            fQuarkInitialized = true;
        }

        /*
         * Updates the number of pending locations in the communicator over the
         * time. This method should be called when the trace has completely been
         * read.
         */
        public void updatePendingLocations(ITmfStateSystemBuilder ssb) {
            if (isInitialized()) {
                Long pendingThreads = 0L;
                for (Map.Entry<Long, Long> entry : fTimestampsPendingThreads.entrySet()) {
                    Long timestamp = entry.getKey();
                    Long value = entry.getValue();
                    pendingThreads += value;
                    ssb.modifyAttribute(timestamp, pendingThreads, fQuark);
                }
                fTimestampsPendingThreads.clear();
            }
        }
    }

    private final Map<Long, Location> fMapLocation;
    private final Map<Integer, Communicator> fMapCommunicator;
    private final long fLastTimestamp;

    /**
     * Constructor : it also registers the last timestamp in the trace
     *
     * @param trace
     *            the trace
     */
    public Otf2CommunicatorsStateProvider(ITmfTrace trace) {
        super(trace, Otf2CommunicatorsAnalysis.getFullAnalysisId());
        fMapLocation = new HashMap<>();
        fMapCommunicator = new HashMap<>();
        fLastTimestamp = trace.readEnd().toNanos();
    }

    @Override
    public int getVersion() {
        return VERSION_NUMBER;
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        super.eventHandle(event);
        /*
         * If the event is the last one then we need to update the states
         * changes for each communicator. This has to be done at the end since
         * collective operations may overlap inside a communicator and state
         * changes may happen in the past for communicator attributes.
         */
        if (isLastEvent(event)) {
            ITmfStateSystemBuilder ssb = getStateSystemBuilder();
            if (ssb == null) {
                return;
            }
            processCommunicatorsAttributes(ssb);
        }
    }

    @Override
    protected void processGlobalDefinition(ITmfEvent event, String name) {
        switch (name) {
        case IOtf2Constants.OTF2_STRING: {
            processStringDefinition(event);
            break;
        }
        case IOtf2Constants.OTF2_REGION: {
            processRegionDefinition(event);
            break;
        }
        case IOtf2Constants.OTF2_LOCATION: {
            ITmfEventField content = event.getContent();
            Long locationReference = content.getFieldValue(Long.class, IOtf2Constants.OTF2_LOCATION_REFERENCE);
            if (locationReference == null) {
                return;
            }
            fMapLocation.put(locationReference, new Location(locationReference));
            break;
        }
        case IOtf2Constants.OTF2_COMM: {
            processCommunicatorDefinition(event);
            ITmfEventField content = event.getContent();
            Integer communicatorReference = content.getFieldValue(Integer.class, IOtf2Constants.OTF2_COMMUNICATOR_REFERENCE);
            if (communicatorReference == null) {
                return;
            }
            fMapCommunicator.put(communicatorReference, new Communicator(communicatorReference));
            break;
        }
        case IOtf2Constants.OTF2_GROUP: {
            processGroupDefinition(event);
            break;
        }
        case IOtf2Constants.OTF2_GROUP_MEMBER: {
            processGroupMemberDefinition(event);
            break;
        }
        default:
            return;
        }
    }

    @Override
    protected void processOtf2Event(ITmfEvent event, String name, ITmfStateSystemBuilder ssb) {
        Long locationId = getLocationId(event);
        Location location = fMapLocation.get(locationId);
        if (location == null) {
            return;
        }
        switch (name) {
        case IOtf2Constants.OTF2_ENTER: {
            location.enter(event);
            break;
        }
        case IOtf2Constants.OTF2_LEAVE: {
            if (location.fInCommunication) {
                location.leaveMpiCommunication(event, ssb);
            }
            break;
        }
        case IOtf2Constants.OTF2_MPI_SEND:
        case IOtf2Constants.OTF2_MPI_ISEND:
        case IOtf2Constants.OTF2_MPI_RECV:
        case IOtf2Constants.OTF2_MPI_IRECV:
        case IOtf2Constants.OTF2_MPI_COLLECTIVE_END: {
            ITmfEventField content = event.getContent();
            Integer communicatorId = content.getFieldValue(Integer.class, IOtf2Constants.OTF2_COMMUNICATOR);
            Communicator communicator = fMapCommunicator.get(communicatorId);
            if (communicator == null) {
                return;
            }
            location.fInCommunication = true;
            processMpiCommunication(event, ssb, name, location, communicator);
            break;
        }
        default:
            break;
        }
    }

    /*
     * Checks whether this event is the last one in the trace
     */
    private boolean isLastEvent(ITmfEvent event) {
        return fLastTimestamp == event.getTimestamp().toNanos();
    }

    /*
     * Updates the states of each communicators over the trace time range
     */
    private void processCommunicatorsAttributes(ITmfStateSystemBuilder ssb) {
        for (Map.Entry<Integer, Communicator> communicatorEntry : fMapCommunicator.entrySet()) {
            Communicator communicator = communicatorEntry.getValue();
            communicator.updatePendingLocations(ssb);
        }
    }

    /*
     * Calls the corresponding method from the associated location given the
     * type of event
     */
    private static void processMpiCommunication(ITmfEvent event, ITmfStateSystemBuilder ssb, String name, Location location, Communicator communicator) {
        if (!communicator.isInitialized()) {
            communicator.initialize(ssb);
        }
        switch (name) {
        case IOtf2Constants.OTF2_MPI_SEND:
        case IOtf2Constants.OTF2_MPI_ISEND: {
            location.mpiSend(event, communicator);
            break;
        }
        case IOtf2Constants.OTF2_MPI_RECV: {
            location.mpiRecv(event, communicator, true);
            break;
        }
        case IOtf2Constants.OTF2_MPI_IRECV: {
            location.mpiRecv(event, communicator, false);
            break;
        }
        case IOtf2Constants.OTF2_MPI_COLLECTIVE_END: {
            location.mpiCollective(event, communicator);
            break;
        }
        default:
            break;
        }
    }
}