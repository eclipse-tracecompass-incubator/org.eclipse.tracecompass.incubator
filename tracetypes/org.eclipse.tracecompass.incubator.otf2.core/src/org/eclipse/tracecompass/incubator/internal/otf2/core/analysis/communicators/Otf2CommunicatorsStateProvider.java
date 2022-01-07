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
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.AbstractOtf2StateProvider;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Constants;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Events;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Fields;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2GlobalDefinitions;
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

    /*
     * A class representing an IRecvRequest. For each asynchronous receive
     * operation, an instance is partially constructed when the communication is
     * launched and completely filled with the communicator informations when
     * the request completion is checked.
     */
    private class IRecvRequest {
        private final String fOperationName;
        private final long fRequestBeginTimestamp;
        private final long fRequestID;
        private Communicator fCommunicator;
        private int fRank;
        private long fRequestEndTimestamp;
        private boolean fRequestResolved;

        public IRecvRequest(String operationName, long requestBeginTimestamp, long requestID) {
            fOperationName = operationName;
            fRequestBeginTimestamp = requestBeginTimestamp;
            fRequestID = requestID;
            fCommunicator = new Communicator(-1);
            fRank = UNKNOWN_RANK;
            fRequestEndTimestamp = 0;
            fRequestResolved = false;
        }

    }

    /*
     * A class implementing a triplet representing a change in the StateSystem.
     * It contains the quark on which this change occurs, the new string value
     * for this quark and the timestamp for this change
     */
    private class StateSystemUpdateTriplet {
        private final int fQuark;
        private final @Nullable String fValue;
        private final long fTimestamp;

        public StateSystemUpdateTriplet(int quark, @Nullable String value, long timestamp) {
            fQuark = quark;
            fValue = value;
            fTimestamp = timestamp;
        }
    }

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
        private boolean fInIRecvRequest;
        private int fRank;
        private final List<MessageIdentifiers> fSentMessages;
        private final Map<Long, Long> fRequestIdBeginTimestamp;
        private final Deque<IRecvRequest> fIRecvRequests;
        private final Deque<StateSystemUpdateTriplet> fPendingStateSystemUpdates;

        public Location(long id) {
            fId = id;
            fCurrentCommunicator = new Communicator(UNKNOWN_RANK);
            fLatestEnteredRegion = IOtf2Constants.UNKNOWN_STRING;
            fLatestEnteredTimestamp = 0L;
            fInCommunication = false;
            fInIRecvRequest = false;
            fRank = UNKNOWN_RANK;
            fSentMessages = new ArrayList<>();
            fRequestIdBeginTimestamp = new HashMap<>();
            fIRecvRequests = new LinkedList<>();
            fPendingStateSystemUpdates = new LinkedList<>();
        }

        /*
         * This method is called when an IRecvRequest was checked and resolved
         * (the corresponding IRecv event has been encountered). It updates the
         * IRecvRequest object created for this request
         */
        private void searchAndUpdateIRecvRequest(long requestID, Communicator communicator, int rank) {
            for (IRecvRequest request : fIRecvRequests) {
                if (request.fRequestID == requestID) {
                    request.fCommunicator = communicator;
                    request.fRank = rank;
                    request.fRequestResolved = true;
                    break;
                }
            }
        }

        /*
         * This method flushes the StateSystem updates when an IRecvRequest has
         * been completed. When an asynchronous Receive is encountered, no
         * information about the communicator is given at first. First an
         * IRecvRequest event is encountered without information about the
         * communicator. It is only when the request is resolved that the
         * information about the communicator can be processed. That is why it
         * is needed to store the StateSystem updates between the begin of the
         * IRecvRequest and its resolution and write all these updates in
         * monotonic order when the request has been resolved and all
         * information is available
         */
        private void flushUpdatesUntilUnresolvedRequest() {
            ITmfStateSystemBuilder ssb = getStateSystemBuilder();
            if (ssb == null) {
                return;
            }
            while (!fIRecvRequests.isEmpty() && fIRecvRequests.getFirst().fRequestResolved) {
                IRecvRequest firstRequest = fIRecvRequests.removeFirst();
                int requestCommunicatorQuark = firstRequest.fCommunicator.fQuark;
                int requestAssociatedLocationQuark = ssb.getQuarkRelativeAndAdd(requestCommunicatorQuark, RANK + String.valueOf(firstRequest.fRank));
                ssb.modifyAttribute(firstRequest.fRequestBeginTimestamp, firstRequest.fOperationName, requestAssociatedLocationQuark);
                ssb.modifyAttribute(firstRequest.fRequestEndTimestamp, null, requestAssociatedLocationQuark);

                long nextRequestTimestamp = fIRecvRequests.isEmpty() ? Long.MAX_VALUE : fIRecvRequests.getFirst().fRequestBeginTimestamp;
                while (!fPendingStateSystemUpdates.isEmpty() && fPendingStateSystemUpdates.peekFirst().fTimestamp <= nextRequestTimestamp) {
                    StateSystemUpdateTriplet triplet = fPendingStateSystemUpdates.removeFirst();
                    ssb.modifyAttribute(triplet.fTimestamp, triplet.fValue, triplet.fQuark);
                }
            }
        }

        /*
         * This method is called when all the trace has been read. It flushes
         * the StateSystem updates that were stored during an unresolved request
         */
        public void flushAllUpdates(ITmfStateSystemBuilder ssb) {
            /*
             * First, for each request that has been resolved, all other updates
             * that happens before these requests are flushed
             */
            for (IRecvRequest request : fIRecvRequests) {
                if (request.fRequestResolved) {
                    while (!fPendingStateSystemUpdates.isEmpty() && fPendingStateSystemUpdates.peekFirst().fTimestamp <= request.fRequestBeginTimestamp) {
                        StateSystemUpdateTriplet triplet = fPendingStateSystemUpdates.removeFirst();
                        ssb.modifyAttribute(triplet.fTimestamp, triplet.fValue, triplet.fQuark);
                    }
                    int requestCommunicatorQuark = request.fCommunicator.fQuark;
                    int requestAssociatedLocationQuark = ssb.getQuarkRelativeAndAdd(requestCommunicatorQuark, RANK + String.valueOf(request.fRank));
                    ssb.modifyAttribute(request.fRequestBeginTimestamp, request.fOperationName, requestAssociatedLocationQuark);
                    ssb.modifyAttribute(request.fRequestEndTimestamp, null, requestAssociatedLocationQuark);
                }
            }

            /*
             * When all the updates associated to resolved requests and events
             * occuring before them have been flushed, the remaining events are
             * flushed.
             */
            while (!fPendingStateSystemUpdates.isEmpty()) {
                StateSystemUpdateTriplet triplet = fPendingStateSystemUpdates.removeFirst();
                ssb.modifyAttribute(triplet.fTimestamp, triplet.fValue, triplet.fQuark);
            }
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
            Integer regionId = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_REGION);
            if (regionId == null) {
                return;
            }
            String regionName = getRegionNameFromRegionId(regionId);
            fLatestEnteredRegion = regionName;
        }

        /**
         * This method is called when an OTF2 IRecvRequest is encountered. It
         * means an asynchronous receive event has been encountered. However,
         * this event does not contain any information on the communicator and
         * the attributes can not be updated without this information. An
         * IRecvRequest object is partially constructed and will be filled with
         * the communicator information when the request is resolved.
         *
         * @param event
         *            the ITmfEvent associated to the OTF2 IRecvRequest event
         */

        public void mpiIRecvRequest(ITmfEvent event) {
            ITmfEventField content = event.getContent();
            Long requestID = content.getFieldValue(Long.class, IOtf2Fields.OTF2_REQUEST_ID);
            if (requestID == null) {
                return;
            }
            fInCommunication = true;
            fInIRecvRequest = true;
            fRequestIdBeginTimestamp.put(requestID, fLatestEnteredTimestamp);
            fIRecvRequests.add(new IRecvRequest(fLatestEnteredRegion, fLatestEnteredTimestamp, requestID));
        }

        /**
         * This method is called when a location leaves a region associated to a
         * MPI Communication and updates state of the attribute associated to
         * the location if there is no unresolved IRecvRequest ongoing for this
         * communicator, else, it stores the information for this update
         *
         * @param event
         *            the ITmfEvent associated to the OTF2 LeaveRegion event
         * @param ssb
         *            the StateSystemBuilder used to update the state of the
         *            attribute
         */
        public void leave(ITmfEvent event, ITmfStateSystemBuilder ssb) {
            if (fInIRecvRequest) {
                IRecvRequest iRecvRequest = fIRecvRequests.getLast();
                iRecvRequest.fRequestEndTimestamp = event.getTimestamp().toNanos();
                fInIRecvRequest = false;
                return;
            }
            if (fInCommunication) {
                int communicatorQuark = fCurrentCommunicator.fQuark;
                int associatedLocationQuark = ssb.getQuarkRelativeAndAdd(communicatorQuark, RANK + String.valueOf(fRank));
                long eventTimestamp = event.getTimestamp().toNanos();
                if (fIRecvRequests.isEmpty()) {
                    ssb.modifyAttribute(fLatestEnteredTimestamp, fLatestEnteredRegion, associatedLocationQuark);
                    ssb.modifyAttribute(eventTimestamp, null, associatedLocationQuark);
                } else {
                    fPendingStateSystemUpdates.add(new StateSystemUpdateTriplet(associatedLocationQuark, fLatestEnteredRegion, fLatestEnteredTimestamp));
                    fPendingStateSystemUpdates.add(new StateSystemUpdateTriplet(associatedLocationQuark, null, eventTimestamp));
                }
                fInCommunication = false;
            }
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
            Integer destRank = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_RECEIVER);
            Integer messageTag = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_MESSAGE_TAG);
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
            Integer srcRank = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_SENDER);
            Integer messageTag = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_MESSAGE_TAG);
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
                Long requestId = content.getFieldValue(Long.class, IOtf2Fields.OTF2_REQUEST_ID);
                if (requestId == null) {
                    return;
                }

                searchAndUpdateIRecvRequest(requestId, communicator, destRank);
                if (fIRecvRequests.getFirst().fRequestID == requestId) {
                    flushUpdatesUntilUnresolvedRequest();
                }
                Long requestBeginTimestamp = fRequestIdBeginTimestamp.remove(requestId);
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
            Integer operationCode = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_COLLECTIVE_OPERATION);
            if (operationCode == null) {
                return;
            }
            Integer root = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_ROOT);
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
         * changes may happen in the past for communicator attributes. In the
         * case were there were unresolved IRecv requests, stored updates for
         * each location are also flushed.
         */
        if (isLastEvent(event)) {
            ITmfStateSystemBuilder ssb = getStateSystemBuilder();
            if (ssb == null) {
                return;
            }
            processLocationsAttributes(ssb);
            processCommunicatorsAttributes(ssb);
        }
    }

    @Override
    protected void processGlobalDefinition(ITmfEvent event, String name) {
        switch (name) {
        case IOtf2GlobalDefinitions.OTF2_STRING: {
            processStringDefinition(event);
            break;
        }
        case IOtf2GlobalDefinitions.OTF2_REGION: {
            processRegionDefinition(event);
            break;
        }
        case IOtf2GlobalDefinitions.OTF2_LOCATION: {
            ITmfEventField content = event.getContent();
            Long locationReference = content.getFieldValue(Long.class, IOtf2Fields.OTF2_SELF);
            if (locationReference == null) {
                return;
            }
            fMapLocation.put(locationReference, new Location(locationReference));
            break;
        }
        case IOtf2GlobalDefinitions.OTF2_COMM: {
            processCommunicatorDefinition(event);
            ITmfEventField content = event.getContent();
            Integer communicatorReference = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_SELF);
            if (communicatorReference == null) {
                return;
            }
            fMapCommunicator.put(communicatorReference, new Communicator(communicatorReference));
            break;
        }
        case IOtf2GlobalDefinitions.OTF2_GROUP: {
            processGroupDefinition(event);
            break;
        }
        case IOtf2GlobalDefinitions.OTF2_GROUP_MEMBER: {
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
        case IOtf2Events.OTF2_ENTER: {
            location.enter(event);
            break;
        }
        case IOtf2Events.OTF2_LEAVE: {
            if (location.fInCommunication || location.fInIRecvRequest) {
                location.leave(event, ssb);
            }
            break;
        }
        case IOtf2Events.OTF2_MPI_IRECV_REQUEST: {
            location.mpiIRecvRequest(event);
            break;
        }
        case IOtf2Events.OTF2_MPI_SEND:
        case IOtf2Events.OTF2_MPI_ISEND:
        case IOtf2Events.OTF2_MPI_RECV:
        case IOtf2Events.OTF2_MPI_IRECV:
        case IOtf2Events.OTF2_MPI_COLLECTIVE_END: {
            ITmfEventField content = event.getContent();
            Integer communicatorId = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_COMMUNICATOR);
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
     * Updates the pending state changes of each locations in the case where
     * some IRecvRequests were not resolved
     */
    private void processLocationsAttributes(ITmfStateSystemBuilder ssb) {
        for (Map.Entry<Long, Location> locationEntry : fMapLocation.entrySet()) {
            Location location = locationEntry.getValue();
            location.flushAllUpdates(ssb);
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
        case IOtf2Events.OTF2_MPI_SEND:
        case IOtf2Events.OTF2_MPI_ISEND: {
            location.mpiSend(event, communicator);
            break;
        }
        case IOtf2Events.OTF2_MPI_RECV: {
            location.mpiRecv(event, communicator, true);
            break;
        }
        case IOtf2Events.OTF2_MPI_IRECV: {
            location.mpiRecv(event, communicator, false);
            break;
        }
        case IOtf2Events.OTF2_MPI_COLLECTIVE_END: {
            location.mpiCollective(event, communicator);
            break;
        }
        default:
            break;
        }
    }
}
