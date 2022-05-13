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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.incubator.callstack.core.base.EdgeStateValue;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.AbstractOtf2StateProvider;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Constants;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Events;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Fields;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2GlobalDefinitions;
import org.eclipse.tracecompass.incubator.internal.otf2.core.mpi.AllToRootIdentifiers;
import org.eclipse.tracecompass.incubator.internal.otf2.core.mpi.MessageIdentifiers;
import org.eclipse.tracecompass.incubator.internal.otf2.core.mpi.RootToAllIdentifiers;
import org.eclipse.tracecompass.incubator.internal.otf2.core.trace.Location;
import org.eclipse.tracecompass.incubator.internal.otf2.core.trace.LocationGroup;
import org.eclipse.tracecompass.incubator.internal.otf2.core.trace.SystemTreeNode;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

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
     * A class representing a node from the system tree. It is used to represent
     * how the different machines are distributed into a MPI cluster and how the
     * MPI ranks are distributed on each node.
     *
     * @author Yoann Heitz
     *
     */
    private class CallstackSystemTreeNode extends SystemTreeNode {
        private int fSystemTreeNodeQuark;

        public CallstackSystemTreeNode(ITmfEvent event) {
            super(event);
            fSystemTreeNodeQuark = UNKNOWN_ID;
        }

        /*
         * The method used to initialize the quark associated to this node. If
         * the node ID of the parent of this node is the maximum value for
         * unsigned integer, then this node is the root of a system tree. Else,
         * the parent node is usually a machine node. Its quark must be
         * retrieved and initialized if it was not done before.
         */
        public void initializeQuarks(ITmfStateSystemBuilder ssb) {
            String fullName = getFullName(getStringId());
            long parentId = getParentId();
            if (isRootNode()) {
                fSystemTreeNodeQuark = ssb.getQuarkAbsoluteAndAdd(PROCESSES, fullName);
            } else {
                int machineQuark = getSystemTreeNodeQuark(parentId);
                if (machineQuark == UNKNOWN_ID) {
                    CallstackSystemTreeNode parentNode = fMapSystemTreeNode.get(parentId);
                    if (parentNode == null) {
                        return;
                    }
                    parentNode.initializeQuarks(ssb);
                    machineQuark = getSystemTreeNodeQuark(parentId);
                }
                if (machineQuark != UNKNOWN_ID) {
                    fSystemTreeNodeQuark = ssb.getQuarkRelativeAndAdd(machineQuark, fullName);
                }
            }
        }

        public int getQuark() {
            return fSystemTreeNodeQuark;
        }
    }

    private int getSystemTreeNodeQuark(long systemTreeNodeId) {
        CallstackSystemTreeNode systemTreeNode = fMapSystemTreeNode.get(systemTreeNodeId);
        if (systemTreeNode == null) {
            return UNKNOWN_ID;
        }
        return systemTreeNode.getQuark();
    }

    /**
     * A class representing a location group for the callstack analysis. It is
     * used to represent a MPI rank.
     *
     * @author Yoann Heitz
     *
     */
    private class CallstackLocationGroup extends LocationGroup {
        private int fLocationGroupQuark;

        public CallstackLocationGroup(ITmfEvent event) {
            super(event);
            fLocationGroupQuark = UNKNOWN_ID;
        }

        public void initializeQuarks(ITmfStateSystemBuilder ssb) {
            long parentId = getParentId();
            String fullName = getFullName(getStringId());
            int nodeQuark = getSystemTreeNodeQuark(parentId);
            if (nodeQuark != UNKNOWN_ID) {
                fLocationGroupQuark = ssb.getQuarkRelativeAndAdd(nodeQuark, fullName);
            }
        }

        public int getQuark() {
            return fLocationGroupQuark;
        }
    }

    private int getLocationGroupQuark(long locationGroupId) {
        CallstackLocationGroup locationGroup = fMapLocationGroup.get(locationGroupId);
        if (locationGroup == null) {
            return UNKNOWN_ID;
        }
        return locationGroup.getQuark();
    }

    /**
     * A class representing a location. When an OTF2 event is encountered,
     * methods from this class will be used to modify the state of the
     * associated attributes.
     *
     * @author Yoann Heitz
     *
     */
    private class CallstackLocation extends Location {

        private long fCollectiveBeginTimestamp;
        private int fLocationQuark;
        private int fCallStackQuark;

        public CallstackLocation(ITmfEvent event) {
            super(event);
            fCollectiveBeginTimestamp = 0L;
            fLocationQuark = UNKNOWN_ID;
            fCallStackQuark = UNKNOWN_ID;
        }

        /**
         * This method will be called by each location when all definitions have
         * been read : the name of the location may be computed at this moment,
         * and the corresponding quark may be created.
         *
         * @param ssb
         *            The state system to write to
         */
        public void initializeQuarks(ITmfStateSystemBuilder ssb) {
            // Get the name of the location
            String locationName = getName(getStringId());

            // Create the associated quark
            int processQuark = getLocationGroupQuark(getLocationGroupId());
            fLocationQuark = ssb.getQuarkRelativeAndAdd(processQuark, locationName);
            fCallStackQuark = ssb.getQuarkRelativeAndAdd(fLocationQuark, InstrumentedCallStackAnalysis.CALL_STACK);
        }

        // This method is called when the location enters a code region
        public void enter(ITmfEvent event, ITmfStateSystemBuilder ssb) {
            ITmfEventField content = event.getContent();
            long timestamp = event.getTimestamp().toNanos();
            ssb.updateOngoingState(TmfStateValue.newValueLong(getId()), fLocationQuark);
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

        /**
         * This method is called when a location finished to send data through a
         * MPI routine (it does not mean the location exited the associated code
         * region). It stores the informations about the send event in order to
         * link it to the receive event when encountered.
         *
         * @param srcEvent
         *            The event generated when sending data
         */
        public void mpiSend(ITmfEvent srcEvent) {
            ITmfEventField content = srcEvent.getContent();
            Integer communicator = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_COMMUNICATOR);
            if (communicator == null) {
                return;
            }
            Integer srcRank = getRank(getId(), communicator);
            Integer destRank = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_RECEIVER);
            Integer messageTag = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_MESSAGE_TAG);
            if (destRank == null || messageTag == null || srcRank == UNKNOWN_RANK) {
                return;
            }
            fMsgDataEvent.put(new MessageIdentifiers(communicator, srcRank, destRank, messageTag), srcEvent);
        }

        /**
         * This method is called when a location finished to receive data
         * through a MPI routine (it does not mean the location exited the
         * associated code region). It searches for the associated send event
         * and draw an edge between the two events.
         *
         * @param destEvent
         *            The event generated when receiving data
         * @param ssb
         *            The state system to write to
         */
        public void mpiRecv(ITmfEvent destEvent, ITmfStateSystemBuilder ssb) {
            ITmfEventField content = destEvent.getContent();
            Integer communicator = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_COMMUNICATOR);
            if (communicator == null) {
                return;
            }
            Integer srcRank = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_SENDER);
            Integer destRank = getRank(getId(), communicator);
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
            HostThread dest = new HostThread(destEvent.getTrace().getHostId(), (int) getId());
            addArrow(ssb, srcEvent.getTimestamp().toNanos(), destEvent.getTimestamp().toNanos(), messageTag, src, dest);
        }

        // Called when a collective routine is called by this location
        public void mpiCollectiveBegin(ITmfEvent event) {
            fCollectiveBeginTimestamp = event.getTimestamp().toNanos();
        }

        /**
         * Called when a location finished an one to all MPI operation. This
         * location is necessarily a destination in an exchange of data. The
         * event corresponding to the root sending the data is retrieved in
         * order to draw an edge
         *
         * @param destEvent
         *            The event that waits for all MPI operations to finish
         * @param ssb
         *            The state system to write to
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
                if (operationProperties.isAssociatedOperation(operationCode, srcLocationId, communicator, getId())) {
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
            associatedOperation.locationCalledOperation(getId(), fCollectiveBeginTimestamp);

            if (associatedOperation.isOperationDone()) {
                fRootToAllQueue.remove(associatedOperation);
            }
            if (srcLocationId == getId()) {
                return;
            }
            if (srcEvent == null) {
                srcEvent = associatedOperation.getBeginEvent();
            }
            HostThread src = new HostThread(srcEvent.getTrace().getHostId(), (int) srcLocationId);
            HostThread dest = new HostThread(destEvent.getTrace().getHostId(), (int) getId());
            addArrow(ssb, srcEvent.getTimestamp().toNanos(), destEvent.getTimestamp().toNanos(), root, src, dest);

        }

        /**
         * Called when a location finished an all to one MPI operation. This
         * location is necessarily a source in an exchange of data. The
         * informations about the event are stored : if it is the root
         * (receiver) that call this method then the communication is done and
         * edges may be drawn.
         *
         * @param srcEvent
         *            The event that generates this operation
         * @param ssb
         *            The state system to write to
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
                if (operationProperties.isAssociatedOperation(operationCode, destLocationId, communicator, getId())) {
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
            associatedOperation.locationCalledOperation(getId(), srcEvent);

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
    private final Map<Long, CallstackSystemTreeNode> fMapSystemTreeNode = new HashMap<>();
    private final Map<Long, CallstackLocationGroup> fMapLocationGroup = new HashMap<>();
    private final Map<Long, CallstackLocation> fMapLocation = new HashMap<>();
    private final Map<MessageIdentifiers, ITmfEvent> fMsgDataEvent = new HashMap<>();
    private final Queue<RootToAllIdentifiers> fRootToAllQueue = new LinkedList<>();
    private final Queue<AllToRootIdentifiers> fAllToRootQueue = new LinkedList<>();
    private boolean fAllDefinitionsRead;

    /**
     * @param trace
     *            Trace to follow
     */
    public Otf2CallStackStateProvider(@Nullable ITmfTrace trace) {
        super(trace, ID);
        fAllDefinitionsRead = false;
    }

    @Override
    public int getVersion() {
        return 2;
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
        case IOtf2GlobalDefinitions.OTF2_SYSTEM_TREE_NODE: {
            processSystemTreeNodeDefinition(event);
            break;
        }
        default:
            return;
        }
    }

    private void processLocationDefinition(ITmfEvent event) {
        CallstackLocation location = new CallstackLocation(event);
        fMapLocation.put(location.getId(), location);
    }

    private void processLocationGroupDefinition(ITmfEvent event) {
        CallstackLocationGroup locationGroup = new CallstackLocationGroup(event);
        fMapLocationGroup.put(locationGroup.getId(), locationGroup);
    }

    private void processSystemTreeNodeDefinition(ITmfEvent event) {
        CallstackSystemTreeNode systemTreeNode = new CallstackSystemTreeNode(event);
        fMapSystemTreeNode.put(systemTreeNode.getId(), systemTreeNode);
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
        CallstackLocation location = fMapLocation.get(locationId);
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

    /**
     * Iterates over all the location and initializes the associated quarks
     */
    private void initializeQuarks(ITmfStateSystemBuilder ssb) {
        for (CallstackSystemTreeNode systemTreeNode : fMapSystemTreeNode.values()) {
            systemTreeNode.initializeQuarks(ssb);
        }
        for (CallstackLocationGroup locationGroup : fMapLocationGroup.values()) {
            locationGroup.initializeQuarks(ssb);
        }
        for (CallstackLocation location : fMapLocation.values()) {
            location.initializeQuarks(ssb);
        }
    }

    /**
     * Calls the corresponding method from the associated location given the
     * type of event
     */
    private static void processMpiCollectiveEnd(ITmfEvent event, ITmfStateSystemBuilder ssb, CallstackLocation location) {
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
