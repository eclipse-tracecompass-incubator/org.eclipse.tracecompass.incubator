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

package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.otf2.core.Activator;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.callstack.Otf2CallStackStateProvider;
import org.eclipse.tracecompass.incubator.internal.otf2.core.trace.AttributeDefinition;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Base state provider for the OTF2 analyses
 *
 * @author Yoann Heitz
 */
public abstract class AbstractOtf2StateProvider extends AbstractTmfStateProvider {

    /**
     * Unknown location ID
     */
    protected static final long UNKNOWN_LOCATION_ID = -1;

    /**
     * Unknown rank
     */
    protected static final int UNKNOWN_RANK = -1;

    /**
     * Unknown ID
     */
    protected static final int UNKNOWN_ID = -1;

    /**
     * Attribute id to attribute event definition
     */
    protected final Map<Integer, AttributeDefinition> fAttributeDefinitions;

    /**
     * String id to string value mapping
     */
    private final Map<Integer, String> fStringId;

    /**
     * Region id to string id mapping
     */
    private final Map<Integer, Integer> fRegionStringId;

    /**
     * Communicator id to group id mapping
     */
    private final Map<Integer, Integer> fCommunicatorGroup;

    /**
     * Group id to group members mapping
     */
    private final Map<Integer, @Nullable ArrayList<Long>> fGroupMembers;

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     * @param id
     *            the ID of the corresponding analysis
     */
    protected AbstractOtf2StateProvider(@Nullable ITmfTrace trace, String id) {
        super(Objects.requireNonNull(trace), id);
        fStringId = new HashMap<>();
        fRegionStringId = new HashMap<>();
        fCommunicatorGroup = new HashMap<>();
        fGroupMembers = new HashMap<>();
        fAttributeDefinitions = new HashMap<>();
    }

    @Override
    public ITmfStateProvider getNewInstance() {
        try {
            return Objects.requireNonNull((ITmfStateProvider) this.getClass().getConstructor(ITmfTrace.class).newInstance(getTrace()));
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            Activator.getInstance().logError("Error instantiating state provider"); //$NON-NLS-1$
            return new Otf2CallStackStateProvider(getTrace());
        }
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        ITmfEventField content = event.getContent();
        if (content == null) {
            return;
        }
        ITmfStateSystemBuilder ssb = getStateSystemBuilder();
        if (ssb == null) {
            return;
        }
        String ctfEventName = event.getName();
        Matcher matcher = IOtf2Constants.OTF2_EVENT_NAME_PATTERN.matcher(ctfEventName);
        if (!matcher.matches()) {
            return;
        }
        String eventType = matcher.group(IOtf2Constants.OTF2_TYPE_GROUP);
        String otf2EventName = matcher.group(IOtf2Constants.OTF2_NAME_GROUP);
        if (otf2EventName == null) {
            return;
        }
        switch (eventType) {
        case IOtf2Constants.OTF2_GLOBAL_DEFINITION: {
            processGlobalDefinition(event, otf2EventName);
            break;
        }
        case IOtf2Constants.OTF2_EVENT: {
            processOtf2Event(event, otf2EventName, ssb);
            break;
        }
        case IOtf2Constants.OTF2_ATTRIBUTE: {
            processOtf2EventAttribute(event, otf2EventName, ssb);
            break;
        }
        default:
            return;
        }
    }

    /**
     * Process OTF2 Event attribute
     *
     * @param event
     *            The event to process
     * @param otf2EventName
     *            The attribute name
     * @param ssb
     *            The state system builder
     */
    protected void processOtf2EventAttribute(ITmfEvent event, String otf2EventName, ITmfStateSystemBuilder ssb) {
        // Do nothing
    }

    /**
     * Process a global definition
     *
     * @param event
     *            The event to process
     * @param name
     *            The name of the global definition
     */
    protected abstract void processGlobalDefinition(ITmfEvent event, String name);

    /**
     * Default method to process a string definition (add it to the String id to
     * String value mapping table)
     *
     * @param event
     *            The event to process
     */
    protected void processStringDefinition(ITmfEvent event) {
        ITmfEventField content = event.getContent();
        Integer stringReference = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_SELF);
        String stringValue = content.getFieldValue(String.class, IOtf2Fields.OTF2_STRING_VALUE);
        if (stringReference == null || stringValue == null) {
            return;
        }
        fStringId.put(stringReference, stringValue);
    }

    /**
     * Default method to process a region definition (add it to the Region to
     * String id mapping table)
     *
     * @param event
     *            The event to process
     */
    protected void processRegionDefinition(ITmfEvent event) {
        ITmfEventField content = event.getContent();
        Integer regionReference = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_SELF);
        Integer stringReference = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_NAME);
        if (regionReference == null || stringReference == null) {
            return;
        }
        fRegionStringId.put(regionReference, stringReference);
    }

    /**
     * Default method to process a communicator definition (add it to the
     * Communicator to Group mapping table)
     *
     * @param event
     *            The event to process
     */
    protected void processCommunicatorDefinition(ITmfEvent event) {
        ITmfEventField content = event.getContent();
        Integer communicatorReference = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_SELF);
        Integer groupReference = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_GROUP);
        if (communicatorReference == null || groupReference == null) {
            return;
        }
        fCommunicatorGroup.put(communicatorReference, groupReference);
    }

    /**
     * Default method to process a group definition (initialize the members
     * array)
     *
     * @param event
     *            The event to process
     */
    protected void processGroupDefinition(ITmfEvent event) {
        ITmfEventField content = event.getContent();
        Integer groupReference = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_SELF);
        Integer numberOfMembers = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_NUMBER_OF_MEMBERS);
        if (groupReference == null || numberOfMembers == null) {
            return;
        }
        if (numberOfMembers == 0) {
            fGroupMembers.put(groupReference, null);
            return;
        }
        ArrayList<Long> membersArray = new ArrayList<>(numberOfMembers);
        for (int i = 0; i < numberOfMembers; i++) {
            membersArray.add(UNKNOWN_LOCATION_ID);
        }
        fGroupMembers.put(groupReference, membersArray);
    }

    /**
     * Default method to process a group member definition (fill the
     * corresponding members array)
     *
     * @param event
     *            The event to process
     */
    protected void processGroupMemberDefinition(ITmfEvent event) {
        ITmfEventField content = event.getContent();
        Integer groupReference = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_GROUP);
        Integer rank = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_RANK);
        Long locationReference = content.getFieldValue(Long.class, IOtf2Fields.OTF2_LOCATION);
        if (groupReference == null || rank == null || locationReference == null) {
            return;
        }
        ArrayList<Long> membersArray = fGroupMembers.get(groupReference);
        if (membersArray == null) {
            return;
        }
        membersArray.set(rank, locationReference);
        fGroupMembers.put(groupReference, membersArray);
    }

    /**
     * Default method to process an attribute definition
     *
     * @param event
     *            The event to process
     */
    protected void processAttributeDefinition(ITmfEvent event) {
        Integer attributeId = event.getContent().getFieldValue(Integer.class, IOtf2Fields.OTF2_SELF);
        Integer nameId = event.getContent().getFieldValue(Integer.class, IOtf2Fields.OTF2_NAME);
        Integer descriptionId = event.getContent().getFieldValue(Integer.class, IOtf2Fields.OTF2_DESCRIPTION);
        Integer typeId = event.getContent().getFieldValue(Integer.class, IOtf2Fields.OTF2_TYPE);
        if (attributeId != null && nameId != null && descriptionId != null && typeId != null) {
            fAttributeDefinitions.put(attributeId, new AttributeDefinition(nameId, descriptionId, typeId));
        }
    }

    /**
     * Process an OTF2 event
     *
     * @param event
     *            The event to process
     * @param name
     *            The name of the event
     * @param ssb
     *            The state system builder
     */
    protected abstract void processOtf2Event(ITmfEvent event, String name, ITmfStateSystemBuilder ssb);

    /**
     * Check if an event is of a given type
     *
     * @param event
     *            the event
     * @param eventName
     *            the event name to check for
     * @return true if the event is of the given type, false otherwise
     */
    protected static boolean isEvent(ITmfEvent event, String eventName) {
        return event.getName().equals(eventName);
    }

    /**
     * Return the location id of an OTF2 event.
     *
     * @param event
     *            The event
     * @return The location id
     */
    protected long getLocationId(ITmfEvent event) {
        ITmfEventField content = event.getContent();
        Long locationId = content.getFieldValue(Long.class, IOtf2Fields.OTF2_LOCATION_ID);
        return (locationId != null) ? locationId : UNKNOWN_LOCATION_ID;
    }

    /**
     * Return the rank of a location into an MPI communicator.
     *
     * @param location
     *            The location id
     * @param communicatorReference
     *            The communicator id
     * @return The rank of the location into the communicator
     */
    protected int getRank(Long location, Integer communicatorReference) {
        Integer groupReference = fCommunicatorGroup.get(communicatorReference);
        ArrayList<Long> members = fGroupMembers.get(groupReference);
        if (members == null) {
            return UNKNOWN_RANK;
        }
        return members.indexOf(location);
    }

    /**
     * @param stringId
     *            the id of the string
     * @return the value of the string
     */
    protected String getStringFromStringId(int stringId) {
        String stringValue = fStringId.get(stringId);
        if (stringValue == null) {
            return IOtf2Constants.UNKNOWN_STRING;
        }
        return stringValue;
    }

    /**
     *
     * @param regionId
     *            the id of the region of code
     * @return the name of the region
     */
    protected String getRegionNameFromRegionId(int regionId) {
        Integer stringId = fRegionStringId.get(regionId);
        if (stringId == null) {
            return IOtf2Constants.UNKNOWN_STRING;
        }
        return getStringFromStringId(stringId);
    }

    /**
     *
     * @param communicatorReference
     *            the id of the communicator
     * @return the list of the members in the communicator
     */
    protected ArrayList<Long> getMembersFromCommunicatorReference(int communicatorReference) {
        Integer groupReference = fCommunicatorGroup.get(communicatorReference);
        ArrayList<Long> members = fGroupMembers.get(groupReference);
        if (members == null) {
            return new ArrayList<>();
        }
        return members;
    }

    /**
     * Getter for the fStringId field
     *
     * @return the fStringId field
     */
    protected Map<Integer, String> getStringId() {
        return fStringId;
    }

    /**
     * Return the location id of a process into an MPI communicator.
     *
     * @param rank
     *            The rank of the process in the communicator
     * @param communicatorReference
     *            The communicator id
     * @return The rank of the location into the communicator, unknown if the
     *         rank number is >= to the number of members defined.
     */
    protected long getLocationIdFromRank(Integer rank, Integer communicatorReference) {
        ArrayList<Long> members = getMembersFromCommunicatorReference(communicatorReference);
        if (rank >= members.size()) {
            return UNKNOWN_RANK;
        }
        return members.get(rank);
    }
}
