/*******************************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which accompanies
 * this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.core.trace;

import java.util.Map;

import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Constants;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Fields;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;

/**
 * A class representing a location. When an OTF2 event is encountered, methods
 * from this class will be used to modify the state of the associated
 * attributes.
 *
 * @author Yoann Heitz
 */
public class Location {

    private final long fId;
    private final long fNameId;
    private final long fLocationGroupId;

    /**
     * Constructs an instance of a Location based on an ITmfEvent
     *
     * @param event
     *            the event that will be parsed
     */
    public Location(ITmfEvent event) {
        ITmfEventField content = event.getContent();
        Long id = content.getFieldValue(Long.class, IOtf2Fields.OTF2_SELF);
        fId = id == null ? IOtf2Constants.OTF2_UNKNOWN_LOCATION : id;

        Long nameId = content.getFieldValue(Long.class, IOtf2Fields.OTF2_NAME);
        fNameId = nameId == null ? IOtf2Constants.OTF2_UNKNOWN_STRING : nameId;

        Long locationGroupId = content.getFieldValue(Long.class, IOtf2Fields.OTF2_LOCATION_GROUP);
        fLocationGroupId = locationGroupId == null ? IOtf2Constants.OTF2_UNKNOWN_LOCATION_GROUP : locationGroupId;
    }

    /**
     * Gets the name of this location
     *
     * @param stringIdMap
     *            a map associating strings to their IDs
     * @return the name of this Location
     */
    public String getName(Map<Integer, String> stringIdMap) {
        String name = stringIdMap.get((int) fNameId);
        if (name == null) {
            return IOtf2Constants.UNKNOWN_STRING;
        }
        return name;
    }

    /**
     * Gets the ID of this location
     *
     * @return the ID of this location
     */
    public long getId() {
        return fId;
    }

    /**
     * Gets the ID of the location group containing this location
     *
     * @return the ID of the location group where this Location is
     */
    public long getLocationGroupId() {
        return fLocationGroupId;
    }
}
