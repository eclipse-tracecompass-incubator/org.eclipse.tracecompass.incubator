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

import java.util.HashMap;

import org.eclipse.jdt.annotation.Nullable;

/**
 * A generic class representing a map containing all "nodes" for a specific type
 * in the physical system. The types may be clusters, physical nodes, processes
 * or threads for example and what has been defined as a "node" above, is an
 * instance of such a type.
 *
 * @author Yoann Heitz
 */
final class FlowsNodeMap<NodeIdType, NodeType extends IFlowsNode> extends HashMap<NodeIdType, @Nullable NodeType> {

    /**
     * Magic number
     */
    private static final long serialVersionUID = 1L;
    private static final int UNKNOWN_ID = -1;

    /**
     * Constructor for this class
     */
    FlowsNodeMap() {
        super();
    }

    /**
     * Gets the correct quark depending on the given node ID and direction
     *
     * @param id
     *            the ID of the node
     * @param direction
     *            the direction associated to the quark
     * @return the quark associated to the node ID and the direction, -1 if the
     *         node is not present in the map.
     */
    public int getNodeQuark(NodeIdType id, String direction) {
        @Nullable NodeType node = get(id);
        if (node == null) {
            return UNKNOWN_ID;
        }
        return node.getQuark(direction);
    }
}
