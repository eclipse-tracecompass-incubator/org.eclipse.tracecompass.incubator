/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.callgraph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.base.CallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackGroupDescriptor;

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
     * @param elements
     *            The full expanded data from the groups
     * @param cgProvider
     *            The call graph data provider
     * @return A collection of data that is the result of the grouping by the
     *         descriptor
     */
    public static Map<ICallStackElement, Collection<AggregatedCallSite>> groupCallGraphBy(ICallStackGroupDescriptor groupBy, Collection<ICallStackElement> elements, ICallGraphProvider cgProvider) {
        // Fast return: just aggregated all groups together
        if (groupBy.equals(AllGroupDescriptor.getInstance())) {
            return groupCallGraphByAll(elements, cgProvider);
        }

        Map<ICallStackElement, Collection<AggregatedCallSite>> grouped = new HashMap<>();
        elements.forEach(g -> grouped.putAll(searchForGroups(g, groupBy, cgProvider, null)));
        return grouped;
    }

    private static void mergeCallsites(
            Map<Object, AggregatedCallSite> map, Collection<AggregatedCallSite> toMerge, ICallGraphProvider cgProvider) {
        toMerge.forEach(acs -> {
            AggregatedCallSite mergeTo = map.get(acs.getSymbol());
            if (mergeTo == null) {
                mergeTo = cgProvider.createCallSite(acs.getSymbol());
                map.put(mergeTo.getSymbol(), mergeTo);

            }
            mergeTo.merge(acs);
        });
    }

    private static Collection<AggregatedCallSite> addGroupData(ICallStackElement srcGroup, ICallStackElement dstGroup, ICallGraphProvider cgProvider) {
        Map<Object, AggregatedCallSite> callsiteMap = new HashMap<>();
        mergeCallsites(callsiteMap, cgProvider.getCallingContextTree(srcGroup), cgProvider);
        srcGroup.getChildren().forEach(group -> {
            Collection<AggregatedCallSite> groupData = addGroupData(group, dstGroup, cgProvider);
            mergeCallsites(callsiteMap, groupData, cgProvider);
        });
        return callsiteMap.values();
    }

    private static Map<ICallStackElement, Collection<AggregatedCallSite>> groupCallGraphByAll(Collection<ICallStackElement> groups, ICallGraphProvider cgProvider) {
        // Fast return: just aggregate all groups together
        ICallStackElement allGroup = new CallStackElement("All", AllGroupDescriptor.getInstance(), null, null);
        Map<Object, AggregatedCallSite> callsiteMap = new HashMap<>();
        groups.forEach(g -> {
            Collection<AggregatedCallSite> groupData = addGroupData(g, allGroup, cgProvider);
            mergeCallsites(callsiteMap, groupData, cgProvider);
        });
        Map<ICallStackElement, Collection<AggregatedCallSite>> map = new HashMap<>();
        map.put(allGroup, callsiteMap.values());
        return map;
    }

    private static Map<ICallStackElement, Collection<AggregatedCallSite>> searchForGroups(ICallStackElement element, ICallStackGroupDescriptor groupBy, ICallGraphProvider cgProvider, @Nullable ICallStackElement parentElement) {
        Map<ICallStackElement, Collection<AggregatedCallSite>> map = new HashMap<>();
        if (element.getGroup().equals(groupBy)) {
            ICallStackElement groupedElement = new CallStackElement(element.getName(), groupBy, null, parentElement);
            map.put(groupedElement, addGroupData(element, groupedElement, cgProvider));
            return map;
        }
        // Maybe the children will be grouped, but this element goes will go in the map with no callsite
        ICallStackElement groupedElement = new CallStackElement(element.getName(), element.getGroup(), element.getNextGroup(), parentElement);
        map.put(groupedElement, Collections.emptyList());
        element.getChildren().forEach(g -> {
            Map<ICallStackElement, Collection<AggregatedCallSite>> subGroups = searchForGroups(g, groupBy, cgProvider, groupedElement);
            subGroups.keySet().forEach(e -> groupedElement.addChild(e));
            map.putAll(subGroups);
        });
        return map;
    }

}
