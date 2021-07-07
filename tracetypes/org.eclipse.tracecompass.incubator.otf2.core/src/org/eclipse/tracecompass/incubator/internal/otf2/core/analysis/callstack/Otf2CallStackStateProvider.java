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

package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.callstack;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.incubator.callstack.core.base.EdgeStateValue;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Constants;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Events;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Fields;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2GlobalDefinitions;
import org.eclipse.tracecompass.incubator.internal.otf2.core.mpi.AllToRootIdentifiers;
import org.eclipse.tracecompass.incubator.internal.otf2.core.mpi.MessageIdentifiers;
import org.eclipse.tracecompass.incubator.internal.otf2.core.mpi.RootToAllIdentifiers;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.AbstractOtf2StateProvider;

/**
 * Main state provider that defines the enter/leave states
 *
 * @author Yoann Heitz
 */
public class Otf2CallStackStateProvider extends AbstractOtf2StateProvider {

    private static final String ID = "org.eclipse.tracecompass.incubator.otf2.callstackstateprovider"; //$NON-NLS-1$

    /**
     * Processes attribute
     */
    public static final String PROCESSES = "Processes"; //$NON-NLS-1$

    /**
     * A class representing a location. When an OTF2 event is encountered,
     * methods from this class will be used to modify the state of the
     * associated attributes.
     *
     * @author Yoann Heitz
     *
     */
    private class Location {

        private final long fLocationId;
        private final int fLocationNameId;
        private final int fProcessId;
        private long fCollectiveBeginTimestamp;
        private int fLocationQuark;
        private int fCallStackQuark;

        public Location(long id, int nameId, int processId) {
            fLocationId = id;
            fLocationNameId = nameId;
            fProcessId = processId;
            fCollectiveBeginTimestamp = 0L;
            fLocationQuark = UNKNOWN_ID;
            fCallStackQuark = UNKNOWN_ID;
        }

        /*
         * This method will be called by each location when all definitions have
         * been read : the names of the location and of the parent process may
         * be computed at this moment, and the corresponding quarks may be
         * created.
         */
        public void initializeQuarks(ITmfStateSystemBuilder ssb) {
            // Get the names of the location and its parent process
            String locationName = getStringFromStringId(fLocationNameId);
            Integer processStringId = fLocationGroupStringId.get(fProcessId);
            if (processStringId == null) {
                processStringId = UNKNOWN_ID;
            }
            String processName = getStringFromStringId(processStringId);
            // Create the associated quarks
            int processQuark = ssb.getQuarkAbsoluteAndAdd(PROCESSES, processName);
            fLocationQuark = ssb.getQuarkRelativeAndAdd(processQuark, locationName);
            fCallStackQuark = ssb.getQuarkRelativeAndAdd(fLocationQuark, InstrumentedCallStackAnalysis.CALL_STACK);
        }

        // This method is called when the location enters a code region
        public void enter(ITmfEvent event, ITmfStateSystemBuilder ssb) {
            ITmfEventField content = event.getContent();
            long timestamp = event.getTimestamp().toNanos();
            ssb.updateOngoingState(TmfStateValue.newValueLong(fLocationId), fLocationQuark);
            Integer regionRef = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_REGION);
            if (regionRef == null) {
                ssb.modifyAttribute(timestamp, null, fCallStackQuark);
                return;
            }
            ssb.pushAttribute(timestamp, getRegionNameFromRegionId(regionRef), fCallStackQuark);
        }

        // This method is called when the location leaves a code region
        public void leave(ITmfEvent event, ITmfStateSystemBuilder ssb) {
            long timestamp = event.getTimestamp().toNanos();
            ssb.popAttribute(timestamp, fCallStackQuark);
        }

        /*
         * This method is called when a location finished to send data through a
         * MPI routine (it does not mean the location exited the associated code
         * region). It stores the informations about the send event in order to
         * link it to the receive event when encountered.
         *
         */
        public void mpiSend(ITmfEvent srcEvent) {
            ITmfEventField content = srcEvent.getContent();
            Integer communicator = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_COMMUNICATOR);
            if (communicator == null) {
                return;
            }
            Integer srcRank = getRank(fLocationId, communicator);
            Integer destRank = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_RECEIVER);
            Integer messageTag = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_MESSAGE_TAG);
            if (destRank == null || messageTag == null || srcRank == UNKNOWN_RANK) {
                return;
            }
            fMsgDataEvent.put(new MessageIdentifiers(communicator, srcRank, destRank, messageTag), srcEvent);
        }

        /*
         * This method is called when a location finished to receive data
         * through a MPI routine (it does not mean the location exited the
         * associated code region). It searches for the associated send event
         * and draw an edge between the two events.
         *
         */
        public void mpiRecv(ITmfEvent destEvent, ITmfStateSystemBuilder ssb) {
            ITmfEventField content = destEvent.getContent();
            Integer communicator = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_COMMUNICATOR);
            if (communicator == null) {
                return;
            }
            Integer srcRank = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_SENDER);
            Integer destRank = getRank(fLocationId, communicator);
            Integer messageTag = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_MESSAGE_TAG);
            if (srcRank == null || messageTag == null || destRank == UNKNOWN_RANK) {
                return;
            }
            ITmfEvent srcEvent = fMsgDataEvent.remove(new MessageIdentifiers(communicator, srcRank, destRank, messageTag));
            if (srcEvent == null) {
                return;
            }
            long srcLocationId = getLocationId(srcEvent);
            HostThread src = new HostThread(srcEvent.getTrace().getHostId(), (int) srcLocationId);
            HostThread dest = new HostThread(destEvent.getTrace().getHostId(), (int) fLocationId);
            addArrow(ssb, srcEvent.getTimestamp().toNanos(), destEvent.getTimestamp().toNanos(), messageTag, src, dest);
        }

        // Called when a collective routine is called by this location
        public void mpiCollectiveBegin(ITmfEvent event) {
            fCollectiveBeginTimestamp = event.getTimestamp().toNanos();
        }

        /*
         * Called when a location finished an one to all MPI operation. This
         * location is necessarily a destination in an exchange of data. The
         * event corresponding to the root sending the data is retrieved in
         * order to draw an edge
         */
        public void mpiRootToAll(ITmfEvent destEvent, ITmfStateSystemBuilder ssb) {
            ITmfEventField content = destEvent.getContent();
            // Get the informations about the communication from the event
            Integer root = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_ROOT);
            Integer communicator = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_COMMUNICATOR);
            Integer operationCode = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_COLLECTIVE_OPERATION);
            if (root == null || communicator == null || operationCode == null) {
                return;
            }
            long srcLocationId = getLocationIdFromRank(root, communicator);

            /*
             * Checks if the MPI routine for this communication has already been
             * called and finished by another location
             */
            RootToAllIdentifiers associatedOperation = null;
            for (RootToAllIdentifiers operationProperties : fRootToAllQueue) {
                if (operationProperties.isAssociatedOperation(operationCode, srcLocationId, communicator, fLocationId)) {
                    associatedOperation = operationProperties;
                }
            }

            /*
             * If this is the first location to end the routine, an object
             * representing the state of the communication is created, also it
             * means that the location where this event appeared is the root of
             * the communication (the root also sends data to itself)
             */
            ITmfEvent srcEvent = null;
            if (associatedOperation == null) {
                srcEvent = destEvent;
                ArrayList<Long> members = getMembersFromCommunicatorReference(communicator);
                ArrayList<Long> pendingLocations = new ArrayList<>(members);
                associatedOperation = new RootToAllIdentifiers(operationCode, communicator, srcLocationId, srcEvent, pendingLocations);
                fRootToAllQueue.add(associatedOperation);
            }

            // Update the state of the communication and draw edge
            associatedOperation.locationCalledOperation(fLocationId, fCollectiveBeginTimestamp);

            if (associatedOperation.isOperationDone()) {
                fRootToAllQueue.remove(associatedOperation);
            }
            if (srcLocationId == fLocationId) {
                return;
            }
            if (srcEvent == null) {
                srcEvent = associatedOperation.getBeginEvent();
            }
            HostThread src = new HostThread(srcEvent.getTrace().getHostId(), (int) srcLocationId);
            HostThread dest = new HostThread(destEvent.getTrace().getHostId(), (int) fLocationId);
            addArrow(ssb, srcEvent.getTimestamp().toNanos(), destEvent.getTimestamp().toNanos(), root, src, dest);

        }

        /*
         * Called when a location finished an all to one MPI operation. This
         * location is necessarily a source in an exchange of data. The
         * informations about the event are stored : if it is the root
         * (receiver) that call this method then the communication is done and
         * edges may be drawn.
         */
        public void mpiAllToRoot(ITmfEvent srcEvent, ITmfStateSystemBuilder ssb) {
            ITmfEventField content = srcEvent.getContent();
            // Gets the informations about the communication from the event
            Integer root = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_ROOT);
            Integer communicator = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_COMMUNICATOR);
            Integer operationCode = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_COLLECTIVE_OPERATION);
            if (root == null || communicator == null || operationCode == null) {
                return;
            }
            long destLocationId = getLocationIdFromRank(root, communicator);
            /*
             * Checks if the MPI routine for this communication has already been
             * called and finished by another location
             */
            AllToRootIdentifiers associatedOperation = null;
            for (AllToRootIdentifiers operationProperties : fAllToRootQueue) {
                if (operationProperties.isAssociatedOperation(operationCode, destLocationId, communicator, fLocationId)) {
                    associatedOperation = operationProperties;
                }
            }

            /*
             * If this is the first location to end the routine, an object
             * representing the state of the communication is created
             */
            if (associatedOperation == null) {
                ArrayList<Long> members = getMembersFromCommunicatorReference(communicator);
                ArrayList<Long> pendingLocations = new ArrayList<>(members);
                associatedOperation = new AllToRootIdentifiers(operationCode, communicator, destLocationId, pendingLocations);
                fAllToRootQueue.add(associatedOperation);
            }

            // Updates the state of the communication
            associatedOperation.locationCalledOperation(fLocationId, srcEvent);

            /*
             * If the communication is finished (routine called by all the
             * locations in communicator), then the location where this event
             * appeared is the root : edges are drawn from all the other
             * locations in the communicator, at the moment where they called
             * the routine to this location at the time of the event
             */
            if (associatedOperation.isOperationDone()) {
                fAllToRootQueue.remove(associatedOperation);
                Collection<ITmfEvent> sentEvents = associatedOperation.getBeginEvents();
                for (ITmfEvent sentEvent : sentEvents) {
                    HostThread src = new HostThread(sentEvent.getTrace().getHostId(), (int) getLocationId(sentEvent));
                    HostThread dest = new HostThread(srcEvent.getTrace().getHostId(), (int) getLocationId(srcEvent));
                    addArrow(ssb, sentEvent.getTimestamp().toNanos(), srcEvent.getTimestamp().toNanos(), 0, src, dest);
                }
            }

        }
    }

    /**
     * Mapping tables required for this analysis
     */
    private final Map<Integer, Integer> fLocationGroupStringId;
    private final Map<Long, Location> fMapLocation;
    private final Map<MessageIdentifiers, ITmfEvent> fMsgDataEvent;
    private final LinkedList<RootToAllIdentifiers> fRootToAllQueue;
    private final LinkedList<AllToRootIdentifiers> fAllToRootQueue;
    private boolean fAllDefinitionsRead;

    /**
     * @param trace
     *            Trace to follow
     */
    public Otf2CallStackStateProvider(@Nullable ITmfTrace trace) {
        super(trace, ID);
        fLocationGroupStringId = new HashMap<>();
        fMsgDataEvent = new HashMap<>();
        fMapLocation = new HashMap<>();
        fRootToAllQueue = new LinkedList<>();
        fAllToRootQueue = new LinkedList<>();
        fAllDefinitionsRead = false;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public ITmfStateProvider getNewInstance() {
        return new Otf2CallStackStateProvider(getTrace());
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
            processLocationDefinition(event);
            break;
        }
        case IOtf2GlobalDefinitions.OTF2_LOCATION_GROUP: {
            processLocationGroupDefinition(event);
            break;
        }
        case IOtf2GlobalDefinitions.OTF2_COMM: {
            processCommunicatorDefinition(event);
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

    private void processLocationDefinition(ITmfEvent event) {
        ITmfEventField content = event.getContent();
        Long locationReference = content.getFieldValue(Long.class, IOtf2Fields.OTF2_SELF);
        Integer locationGroupReference = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_LOCATION_GROUP);
        Integer stringReference = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_NAME);
        if (locationReference == null || locationGroupReference == null || stringReference == null) {
            return;
        }
        fMapLocation.put(locationReference, new Location(locationReference, stringReference, locationGroupReference));
    }

    private void processLocationGroupDefinition(ITmfEvent event) {
        ITmfEventField content = event.getContent();
        Integer locationGroupReference = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_SELF);
        Integer stringReference = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_NAME);
        if (locationGroupReference == null || stringReference == null) {
            return;
        }
        fLocationGroupStringId.put(locationGroupReference, stringReference);
    }

    @Override
    protected void processOtf2Event(ITmfEvent event, String name, ITmfStateSystemBuilder ssb) {
        /*
         * If an OTF2 event is encountered, it means all definitions were read :
         * if this is the first event to be read then quarks associated to the
         * locations need to be initialized
         */
        if (!fAllDefinitionsRead) {
            initializeQuarks(ssb);
            fAllDefinitionsRead = true;
        }

        long locationId = getLocationId(event);
        Location location = fMapLocation.get(locationId);
        if (location == null) {
            return;
        }

        switch (name) {
        case IOtf2Events.OTF2_ENTER: {
            location.enter(event, ssb);
            break;
        }
        case IOtf2Events.OTF2_LEAVE: {
            location.leave(event, ssb);
            break;
        }
        case IOtf2Events.OTF2_MPI_SEND:
        case IOtf2Events.OTF2_MPI_ISEND: {
            location.mpiSend(event);
            break;
        }
        case IOtf2Events.OTF2_MPI_RECV:
        case IOtf2Events.OTF2_MPI_IRECV: {
            location.mpiRecv(event, ssb);
            break;
        }
        case IOtf2Events.OTF2_MPI_COLLECTIVE_BEGIN: {
            location.mpiCollectiveBegin(event);
            break;
        }
        case IOtf2Events.OTF2_MPI_COLLECTIVE_END: {
            processMpiCollectiveEnd(event, ssb, location);
            break;
        }
        default:
            return;
        }
    }

    /*
     * Iterates over all the location and initializes the associated quarks
     */
    private void initializeQuarks(ITmfStateSystemBuilder ssb) {
        for (Location location : fMapLocation.values()) {
            location.initializeQuarks(ssb);
        }
    }

    /**
     * Return the location id of a process into an MPI communicator.
     *
     * @param rank
     *            The rank of the process in the communicator
     * @param communicatorReference
     *            The communicator id
     * @return The rank of the location into the communicator
     */
    protected long getLocationIdFromRank(Integer rank, Integer communicatorReference) {
        ArrayList<Long> members = getMembersFromCommunicatorReference(communicatorReference);
        if (rank >= members.size()) {
            return UNKNOWN_RANK;
        }
        return members.get(rank);
    }

    /*
     * Calls the corresponding method from the associated location given the
     * type of event
     */
    private static void processMpiCollectiveEnd(ITmfEvent event, ITmfStateSystemBuilder ssb, Location location) {
        ITmfEventField content = event.getContent();
        Integer operationCode = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_COLLECTIVE_OPERATION);
        if (operationCode == null) {
            return;
        }
        switch (IOtf2Constants.getOperation(operationCode)) {
        case BCAST:
        case SCATTER:
        case SCATTERV: {
            location.mpiRootToAll(event, ssb);
            return;
        }
        case GATHER:
        case GATHERV:
        case REDUCE: {
            location.mpiAllToRoot(event, ssb);
            return;
        }
        // $CASES-OMITTED$
        default:
            return;
        }
    }

    private static void addArrow(ITmfStateSystemBuilder ssb, Long startTime, Long endTime, int id,
            HostThread src, HostThread dest) {
        int edgeQuark = getAvailableEdgeQuark(ssb, startTime);
        Object edgeStateValue = new EdgeStateValue(id, src, dest);
        ssb.modifyAttribute(startTime, edgeStateValue, edgeQuark);
        ssb.modifyAttribute(endTime, (Object) null, edgeQuark);
    }

    private static int getAvailableEdgeQuark(ITmfStateSystemBuilder ssb, Long startTime) {
        int edgeRoot = ssb.getQuarkAbsoluteAndAdd(IOtf2Constants.EDGES);
        List<@NonNull Integer> subQuarks = ssb.getSubAttributes(edgeRoot, false);
        for (int quark : subQuarks) {
            long start = ssb.getOngoingStartTime(quark);
            Object value = ssb.queryOngoing(quark);
            if (value == null && start <= startTime) {
                return quark;
            }
        }
        return ssb.getQuarkRelativeAndAdd(edgeRoot, Integer.toString(subQuarks.size()));
    }
}
