/*******************************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which accompanies
 * this distribution, and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.flows;

import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.otf2.core.trace.SystemTreeNode;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * A class representing a system tree node for the flows analysis
 *
 * @author Yoann Heitz
 */
class FlowsSystemTreeNode extends SystemTreeNode implements IFlowsNode {

    private int fInputQuark = UNKNOWN_ID;
    private int fOutputQuark = UNKNOWN_ID;

    /**
     * Constructor for this class
     *
     * @param event
     *            a GlobalDef_SystemTreeNode event
     */
    public FlowsSystemTreeNode(ITmfEvent event) {
        super(event);
    }

    /**
     * Initializes the quarks related to this system tree node. This method
     * should be called by the state provider after all definitions have been
     * read
     *
     * @param ssb
     *            the state system builder
     * @param systemTreeNodeMap
     *            a map containing the different system tree nodes and their IDs
     * @param stringIdMap
     *            a map containing the different strings and their IDs
     */
    public void initializeQuarks(ITmfStateSystemBuilder ssb, @Nullable FlowsNodeMap<Long, FlowsSystemTreeNode> systemTreeNodeMap, Map<Integer, String> stringIdMap) {
        String fullName = getFullName(stringIdMap);
        long parentId = getParentId();
        if (isRootNode()) {
            fInputQuark = ssb.getQuarkAbsoluteAndAdd(INPUT, fullName);
            fOutputQuark = ssb.getQuarkAbsoluteAndAdd(OUTPUT, fullName);
        } else if (systemTreeNodeMap != null) {
            int parentInputQuark = systemTreeNodeMap.getNodeQuark(parentId, INPUT);
            int parentOutputQuark = systemTreeNodeMap.getNodeQuark(parentId, OUTPUT);
            fInputQuark = ssb.getQuarkRelativeAndAdd(parentInputQuark, fullName);
            fOutputQuark = ssb.getQuarkRelativeAndAdd(parentOutputQuark, fullName);
        }
    }

    @Override
    public int getInputQuark() {
        return fInputQuark;
    }

    @Override
    public int getOutputQuark() {
        return fOutputQuark;
    }
}
