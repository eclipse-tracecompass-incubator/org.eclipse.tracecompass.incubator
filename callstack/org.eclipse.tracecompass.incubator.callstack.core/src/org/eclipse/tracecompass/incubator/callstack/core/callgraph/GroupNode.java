/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.callgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.tracecompass.incubator.callstack.core.callstack.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.internal.callstack.core.callgraph.AggregatedCallSite;

/**
 * A class that describes a node in the tree for callgraph grouping. Typically a
 * {@link ICallStackGroupDescriptor} will create the corresponding group nodes.
 * Leaf nodes will contain the actual callgraph data for an element.
 *
 * @author Geneviève Bastien
 */
public class GroupNode {

    private final String fName;
    private final ICallStackGroupDescriptor fDescriptor;
    private final List<GroupNode> fChildren = new ArrayList<>();

    public GroupNode(String name, ICallStackGroupDescriptor descriptor) {
        fName = name;
        fDescriptor = descriptor;
    }

    public String getName() {
        return fName;
    }

    public Collection<GroupNode> getChildren() {
        return fChildren;
    }

    public void addChild(GroupNode node) {
        fChildren.add(node);
    }

    public Collection<AggregatedCallSite> getAggregatedData() {
        return Collections.emptyList();
    }

    public boolean isLeaf() {
        return false;
    }

    public ICallStackGroupDescriptor getGroupDescriptor() {
        return fDescriptor;
    }

}
