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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.AbstractOtf2StateProvider;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Events;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Fields;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2GlobalDefinitions;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.Otf2Type;
import org.eclipse.tracecompass.incubator.internal.otf2.core.trace.AttributeDefinition;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.util.Pair;

/**
 * State provider for the OTF2 MPI messages flow analysis
 *
 * @author Yoann Heitz
 */

public class Otf2FlowsStateProvider extends AbstractOtf2StateProvider {

    private static final int VERSION_NUMBER = 1;
    private final FlowsNodeMap<Long, FlowsLocation> fMapLocation = new FlowsNodeMap<>();
    private final FlowsNodeMap<Long, FlowsLocationGroup> fMapLocationGroup = new FlowsNodeMap<>();
    private final FlowsNodeMap<Long, FlowsSystemTreeNode> fMapSystemTreeNode = new FlowsNodeMap<>();
    /** List of event attributes in pairs of attributeId and attributeValue */
    private List<Pair<Integer, Object>> fEventAttributes = new ArrayList<>();
    private Map<FlowsLocation, FlowsLocation> fRankToAcceleratorMap = new HashMap<>();
    private boolean fInitialized = false;

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
        case IOtf2GlobalDefinitions.OTF2_ATTRIBUTE: {
            processAttributeDefinition(event);
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

    private void initializeQuarks(ITmfStateSystemBuilder ssb) {
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
        fInitialized = true;
    }

    @Override
    protected void processOtf2Event(ITmfEvent event, String name, ITmfStateSystemBuilder ssb) {
        if (!fInitialized) {
            initializeQuarks(ssb);
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

    @Override
    protected void processOtf2EventAttribute(ITmfEvent event, String otf2EventName, ITmfStateSystemBuilder ssb) {
        Integer eventAttributeId = event.getContent().getFieldValue(Integer.class, IOtf2Fields.OTF2_ATTRIBUTE);
        Object eventAttributeValue = event.getContent().getFieldValue(Object.class, IOtf2Fields.OTF2_VALUE);
        if (eventAttributeId != null && eventAttributeValue != null) {
            fEventAttributes.add(new Pair<>(eventAttributeId, eventAttributeValue));
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
        FlowsLocation acceleratorLocation = fRankToAcceleratorMap.get(location);
        if (acceleratorLocation != null) {
            acceleratorLocation.setLatestEnteredTimestamp(location.getLatestEnteredTimestamp());
            acceleratorLocation.setInputMessageSize(location.getInputMessageSize());
            acceleratorLocation.setOutputMessageSize(location.getOutputMessageSize());
            acceleratorLocation.processLeave(ssb, event.getTimestamp().toNanos());
            location.setInputMessageSize(0L);
            location.setOutputMessageSize(0L);
        } else {
            location.processLeave(ssb, event.getTimestamp().toNanos());
        }
    }

    private void addFlowsLocationMapping(FlowsLocation location) {
        // Search for memory location in event attributes
        Long groupLocationId = null;
        int eventAttributeIndex = -1;
        for (Pair<Integer, Object> attribute : fEventAttributes) {
            eventAttributeIndex++;
            AttributeDefinition attributeDefinition = this.fAttributeDefinitions.get(attribute.getFirst());
            if (attributeDefinition != null && attributeDefinition.getType() == Otf2Type.OTF2_TYPE_LOCATION_GROUP) {
                groupLocationId = (Long) attribute.getSecond();
            }
        }
        if (groupLocationId != null) {
            // Rempve the event attribute because it has been mapped
            fEventAttributes.remove(eventAttributeIndex);
            for (FlowsLocation acceleratorLocation : fMapLocation.values()) {
                if (Objects.requireNonNull(acceleratorLocation).getLocationGroupId() == groupLocationId) {
                    fRankToAcceleratorMap.put(location, acceleratorLocation);
                }
            }
        }
    }

    /**
     * Function called when an Event_MpiSend event is encountered
     *
     * @param event
     *            an Event_MpiSend event
     */
    private void processMpiSend(ITmfEvent event) {
        ITmfEventField content = event.getContent();
        Long messageLength = content.getFieldValue(Long.class, IOtf2Fields.OTF2_MESSAGE_LENGTH);
        Long locationId = getLocationId(event);
        FlowsLocation location = fMapLocation.get(locationId);
        if (location != null && messageLength != null) {
            location.setOutputMessageSize(messageLength);
            addFlowsLocationMapping(location);
        }
    }

    /**
     * Function called when an Event_MpiRecv event is encountered
     *
     * @param event
     *            an Event_MpiRecv event
     */
    private void processMpiRecv(ITmfEvent event) {
        ITmfEventField content = event.getContent();
        Long messageLength = content.getFieldValue(Long.class, IOtf2Fields.OTF2_MESSAGE_LENGTH);
        Long locationId = getLocationId(event);
        FlowsLocation location = fMapLocation.get(locationId);
        if (location != null && messageLength != null) {
            location.setInputMessageSize(messageLength);
            addFlowsLocationMapping(location);
        }
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
        location.setInputMessageSize(receivedMessageLength);
        location.setOutputMessageSize(sentMessageLength);
    }
}
