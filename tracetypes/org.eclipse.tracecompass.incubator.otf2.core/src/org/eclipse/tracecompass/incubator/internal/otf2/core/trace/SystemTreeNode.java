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

import static org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Constants.OTF2_UNKNOWN_STRING;
import static org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Constants.OTF2_UNKNOWN_SYSTEM_TREE_NODE;

import java.util.Map;

import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Constants;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Fields;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;

/**
 * A generic class representing an OTF2 system tree node (it may be a cluster or
 * a physical node for example).
 *
 * @author Yoann Heitz
 */
public class SystemTreeNode {

    private static final String WHITESPACE = " "; //$NON-NLS-1$

    private final long fId;
    private final long fParentId;
    private final long fNameId;
    private final long fClassNameId;

    /**
     * Constructs an instance of a SystemTreeNode based on an ITmfEvent
     *
     * @param event
     *            the event that will be parsed
     */
    public SystemTreeNode(ITmfEvent event) {
        ITmfEventField content = event.getContent();
        Long id = content.getFieldValue(Long.class, IOtf2Fields.OTF2_SELF);
        fId = id == null ? OTF2_UNKNOWN_SYSTEM_TREE_NODE : id;

        Long parentId = content.getFieldValue(Long.class, IOtf2Fields.OTF2_SYSTEM_TREE_PARENT);
        fParentId = parentId == null ? OTF2_UNKNOWN_SYSTEM_TREE_NODE : parentId;

        Long nameId = content.getFieldValue(Long.class, IOtf2Fields.OTF2_NAME);
        fNameId = nameId == null ? OTF2_UNKNOWN_STRING : nameId;

        Long classNameId = content.getFieldValue(Long.class, IOtf2Fields.OTF2_CLASS_NAME);
        fClassNameId = classNameId == null ? OTF2_UNKNOWN_STRING : classNameId;
    }

    /**
     * A method to get the complete name for this system tree node
     *
     * @param stringIdMap
     *            a map containing the associations between the string IDs and
     *            their actual values
     * @return the full name of this system tree node
     */
    public String getFullName(Map<Integer, String> stringIdMap) {
        String name = stringIdMap.get((int) fNameId);
        String className = stringIdMap.get((int) fClassNameId);
        return className + WHITESPACE + name;
    }

    /**
     * Gets the ID of the system tree node
     *
     * @return the ID of this system tree node
     */
    public long getId() {
        return fId;
    }

    /**
     * Gets the ID of the parent of the system tree node
     *
     * @return the ID of the parent of this system tree node
     */
    public long getParentId() {
        return fParentId;
    }

    /**
     * Tests if this node is at the root of the system tree
     *
     * @return true if this node is root, false else.
     */
    public boolean isRootNode() {
        return getParentId() == IOtf2Constants.OTF2_UNDEFINED_SYSTEM_TREE_NODE;
    }
}
