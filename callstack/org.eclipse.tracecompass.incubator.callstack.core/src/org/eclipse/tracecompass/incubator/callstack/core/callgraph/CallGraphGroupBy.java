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
import org.eclipse.tracecompass.incubator.internal.callstack.core.callgraph.CallGraphAllGroupDescriptor;
import org.eclipse.tracecompass.incubator.internal.callstack.core.callgraph.ICallGraphFactory;
import org.eclipse.tracecompass.incubator.internal.callstack.core.callgraph.LeafGroupNode;
import org.eclipse.tracecompass.incubator.internal.callstack.core.callstack.CallStackAllGroupDescriptor;

/**
 * A class containing helper methods to group aggregated callgraph data by the
 * different available groups
 *
 * @author Geneviève Bastien
 */
public final class CallGraphGroupBy {

    private CallGraphGroupBy() {
        // Nothing to do
    }

    /**
     * Group callgraph groups by one of the descriptor.
     *
     * @param groupBy
     *            The group descriptor by which to group the call graph elements.
     * @param groups
     *            The full expanded data from the groups
     * @param factory
     *            The factory to create individual groups.
     * @return A collection of data that is the result of the grouping by the
     *         descriptor
     */
    public static Collection<GroupNode> groupCallGraphBy(ICallStackGroupDescriptor groupBy, Collection<GroupNode> groups, ICallGraphFactory factory) {
        // Fast return: just aggregated all groups together
        if (groupBy instanceof CallStackAllGroupDescriptor) {
            return groupCallGraphByAll(groups, factory);
        }
        ICallStackGroupDescriptor nextGroup = groupBy.getNextGroup();
        if (nextGroup == null) {
            return groups;
        }
        // FIXME: a group's descriptor is the next one, when that is fixed, fix
        // this too
        List<GroupNode> grouped = new ArrayList<>();
        groups.forEach(g -> grouped.addAll(searchForGroups(g, nextGroup, factory)));
        return grouped;
    }

    private static void addGroupData(GroupNode srcGroup, LeafGroupNode dstGroup, ICallGraphFactory factory) {
        // Add a new
        srcGroup.getAggregatedData().forEach(ad -> {
            AggregatedCallSite callsite = factory.createAggregatedCallSite(ad.getSymbol());
            callsite.merge(ad);
            dstGroup.addAggregatedData(callsite);
        });
        srcGroup.getChildren().forEach(g -> addGroupData(g, dstGroup, factory));
    }

    private static Collection<GroupNode> groupCallGraphByAll(Collection<GroupNode> groups, ICallGraphFactory factory) {
        // Fast return: just aggregated all groups together
        LeafGroupNode allGroup = factory.createLeafGroup("all", CallGraphAllGroupDescriptor.getInstance());
        groups.forEach(g -> addGroupData(g, allGroup, factory));
        return Collections.singleton(allGroup);
    }

    private static Collection<? extends GroupNode> searchForGroups(GroupNode group, ICallStackGroupDescriptor descriptor, ICallGraphFactory factory) {
        if (group.getGroupDescriptor().equals(descriptor)) {
            LeafGroupNode leafGroup = factory.createLeafGroup(group.getName(), descriptor);
            addGroupData(group, leafGroup, factory);
            return Collections.singleton(leafGroup);
        }
        ICallStackGroupDescriptor nextGroup = descriptor.getNextGroup();
        if (nextGroup == null) {
            return Collections.singleton(group);
        }
        // FIXME: a group's descriptor is the next one, when that is fixed, fix
        // this too
        List<GroupNode> grouped = new ArrayList<>();
        group.getChildren().forEach(g -> grouped.addAll(searchForGroups(g, nextGroup, factory)));
        return grouped;
    }

}
