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
 * A class representing a location group. It is used to represent a MPI rank.
 * The parent of the group is a system tree node representing the node on which
 * is running this MPI rank.
 *
 * @author Yoann Heitz
 */
public class LocationGroup {

    /**
     * Unknown string
     */
    protected static final String UNKNOWN = "UNKNOWN"; //$NON-NLS-1$

    private final long fId;
    private final long fParentId;
    private final long fNameId;
    private final int fType;

    /**
     * Constructs an instance of a LocationGroup based on an ITmfEvent
     *
     * @param event
     *            the event that will be parsed
     */
    public LocationGroup(ITmfEvent event) {
        ITmfEventField content = event.getContent();
        Long id = content.getFieldValue(Long.class, IOtf2Fields.OTF2_SELF);
        fId = id == null ? IOtf2Constants.OTF2_UNKNOWN_LOCATION_GROUP : id;

        Long parentId = content.getFieldValue(Long.class, IOtf2Fields.OTF2_SYSTEM_TREE_PARENT);
        fParentId = parentId == null ? IOtf2Constants.OTF2_UNKNOWN_SYSTEM_TREE_NODE : parentId;

        Long nameId = content.getFieldValue(Long.class, IOtf2Fields.OTF2_NAME);
        fNameId = nameId == null ? IOtf2Constants.OTF2_UNKNOWN_STRING : nameId;

        Integer type = content.getFieldValue(Integer.class, IOtf2Fields.OTF2_LOCATION_GROUP_TYPE);
        fType = type == null ? IOtf2Constants.OTF2_UNKNOWN_LOCATION_GROUP_TYPE : type;
    }

    /**
     * Get the complete name of this LocationGroup
     *
     * @param stringIdMap
     *            a map with the different strings of the trace and their IDs
     * @return the full name of this LocationGroup
     */
    public String getFullName(Map<Integer, String> stringIdMap) {
        String name = stringIdMap.get((int) fNameId);
        if (fType == 1 && name != null) {
            return name;
        }
        return UNKNOWN;
    }

    /**
     * Gets the ID of this location group
     *
     * @return the ID of this LocationGroup
     */
    public long getId() {
        return fId;
    }

    /**
     * Gets the ID of the parent of this location group
     *
     * @return the ID of the parent of this LocationGroup
     */
    public long getParentId() {
        return fParentId;
    }
}
