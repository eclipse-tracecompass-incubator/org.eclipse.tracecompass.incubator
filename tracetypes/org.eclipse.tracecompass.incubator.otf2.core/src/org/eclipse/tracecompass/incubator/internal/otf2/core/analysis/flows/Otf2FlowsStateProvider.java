/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.flows;

import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.AbstractOtf2StateProvider;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Events;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Fields;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2GlobalDefinitions;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * State provider for the OTF2 MPI messages flow analysis
 *
 * @author Yoann Heitz
 */

public class Otf2FlowsStateProvider extends AbstractOtf2StateProvider {

    private static int VERSION_NUMBER = 1;
    private final FlowsNodeMap<Long, FlowsLocation> fMapLocation = new FlowsNodeMap<>();
    private final FlowsNodeMap<Long, FlowsLocationGroup> fMapLocationGroup = new FlowsNodeMap<>();
    private final FlowsNodeMap<Long, FlowsSystemTreeNode> fMapSystemTreeNode = new FlowsNodeMap<>();
    private boolean initialized = false;

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     */
    public Otf2FlowsStateProvider(ITmfTrace trace) {
        super(trace, Otf2FlowsAnalysis.getFullAnalysisId());
    }

    @Override
    public int getVersion() {
        return VERSION_NUMBER;
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

    /**
     * Function called when a GlobalDef_Location event is encountered
     *
     * @param event
     *            an GlobalDef_Location event
     */
    private void processLocationDefinition(ITmfEvent event) {
        FlowsLocation location = new FlowsLocation(event);
        fMapLocation.put(location.getId(), location);
    }

    /**
     * Function called when a GlobalDef_LocationGroup event is encountered
     *
     * @param event
     *            an GlobalDef_LocationGroup event
     */
    private void processLocationGroupDefinition(ITmfEvent event) {
        FlowsLocationGroup locationGroup = new FlowsLocationGroup(event);
        fMapLocationGroup.put(locationGroup.getId(), locationGroup);
    }

    /**
     * Function called when a GlobalDef_SystemTreeNode event is encountered
     *
     * @param event
     *            an GlobalDef_SystemTreeNode event
     */
    private void processSystemTreeNodeDefinition(ITmfEvent event) {
        FlowsSystemTreeNode systemTreeNode = new FlowsSystemTreeNode(event);
        fMapSystemTreeNode.put(systemTreeNode.getId(), systemTreeNode);
    }

    @Override
    protected void processOtf2Event(ITmfEvent event, String name, ITmfStateSystemBuilder ssb) {
        if (!initialized) {
            for (FlowsSystemTreeNode systemTreeNode : fMapSystemTreeNode.values()) {
                if (systemTreeNode != null) {
                    systemTreeNode.initializeQuarks(ssb, fMapSystemTreeNode, getStringId());
                }
            }
            for (FlowsLocationGroup locationGroup : fMapLocationGroup.values()) {
                if (locationGroup != null) {
                    locationGroup.initializeQuarks(ssb, fMapSystemTreeNode, getStringId());
                }

            }
            for (FlowsLocation location : fMapLocation.values()) {
                if (location != null) {
                    location.initializeQuarks(ssb, fMapLocationGroup, getStringId());
                }
            }
            initialized = true;
        }
        switch (name) {
        case IOtf2Events.OTF2_ENTER: {
            processEnter(event);
            break;
        }
        case IOtf2Events.OTF2_LEAVE: {
            processLeave(ssb, event);
            break;
        }
        case IOtf2Events.OTF2_MPI_SEND: {
            processMpiSend(event);
            break;
        }
        case IOtf2Events.OTF2_MPI_RECV: {
            processMpiRecv(event);
            break;
        }
        case IOtf2Events.OTF2_MPI_COLLECTIVE_END: {
            processMpiCollectiveCommunication(event);
            break;
        }
        default:
            return;
        }
    }

    /**
     * Function called when an Event_Enter event is encountered
     *
     * @param event
     *            an Event_Enter event
     */
    private void processEnter(ITmfEvent event) {
        Long locationId = getLocationId(event);
        FlowsLocation location = fMapLocation.get(locationId);
        if (location == null) {
            return;
        }
        location.setLatestEnteredTimestamp(event.getTimestamp().toNanos());
    }

    /**
     * Function called when an Event_Leave event is encountered
     *
     * @param event
     *            an Event_Leave event
     */
    private void processLeave(ITmfStateSystemBuilder ssb, ITmfEvent event) {
        Long locationId = getLocationId(event);
        FlowsLocation location = fMapLocation.get(locationId);
        if (location == null) {
            return;
        }
        location.processLeave(ssb, event.getTimestamp().toNanos());
    }

    /**
     * Function called when an Event_MpiSend event is encountered
     *
     * @param event
     *            an Event_MpiSend event
     */
    private void processMpiSend(ITmfEvent event) {
        Long locationId = getLocationId(event);
        ITmfEventField content = event.getContent();
        Long messageLength = content.getFieldValue(Long.class, IOtf2Fields.OTF2_MESSAGE_LENGTH);
        FlowsLocation location = fMapLocation.get(locationId);
        if (location == null || messageLength == null) {
            return;
        }
        location.updateOutputMessageSize(messageLength);
    }

    /**
     * Function called when an Event_MpiRecv event is encountered
     *
     * @param event
     *            an Event_MpiRecv event
     */
    private void processMpiRecv(ITmfEvent event) {
        Long locationId = getLocationId(event);
        ITmfEventField content = event.getContent();
        Long messageLength = content.getFieldValue(Long.class, IOtf2Fields.OTF2_MESSAGE_LENGTH);
        FlowsLocation location = fMapLocation.get(locationId);
        if (location == null || messageLength == null) {
            return;
        }
        location.updateInputMessageSize(messageLength);
    }

    /**
     * Function called when an Event_MpiCollectiveEnd event is encountered
     *
     * @param event
     *            an Event_MpiCollectiveEnd event
     */
    private void processMpiCollectiveCommunication(ITmfEvent event) {
        Long locationId = getLocationId(event);
        ITmfEventField content = event.getContent();
        Long receivedMessageLength = content.getFieldValue(Long.class, IOtf2Fields.OTF2_SIZE_RECEIVED);
        Long sentMessageLength = content.getFieldValue(Long.class, IOtf2Fields.OTF2_SIZE_SENT);
        FlowsLocation location = fMapLocation.get(locationId);
        if (location == null || receivedMessageLength == null || sentMessageLength == null) {
            return;
        }
        location.updateInputMessageSize(receivedMessageLength);
        location.updateOutputMessageSize(sentMessageLength);
    }
}
