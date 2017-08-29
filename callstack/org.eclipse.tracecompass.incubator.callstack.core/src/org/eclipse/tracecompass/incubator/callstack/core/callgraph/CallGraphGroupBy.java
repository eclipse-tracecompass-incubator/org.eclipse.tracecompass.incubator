/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.callgraph;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.AggregatedCallSite;
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
     * @param callGraph
     *            The call graph data provider
     * @return A collection of data that is the result of the grouping by the
     *         descriptor
     */
    public static CallGraph groupCallGraphBy(ICallStackGroupDescriptor groupBy, CallGraph callGraph) {
        // Fast return: just aggregated all groups together
        if (groupBy.equals(AllGroupDescriptor.getInstance())) {
            return groupCallGraphByAll(callGraph);
        }

        CallGraph cg = new CallGraph();
        callGraph.getElements().forEach(g -> searchForGroups(g, groupBy, callGraph, null, cg));
        return cg;
    }

    private static void addGroupData(ICallStackElement srcGroup, CallGraph srcCg, ICallStackElement dstGroup, CallGraph callGraph) {
        srcCg.getCallingContextTree(srcGroup).forEach(acs -> {
            AggregatedCallSite acsCopy = acs.copyOf();
            callGraph.addAggregatedCallSite(dstGroup, acsCopy);
        });
        srcGroup.getChildren().forEach(group -> {
            addGroupData(group, srcCg, dstGroup, callGraph);
        });
    }

    private static CallGraph groupCallGraphByAll(CallGraph callGraph) {
        CallGraph cg = new CallGraph();
        // Fast return: just aggregate all groups together
        ICallStackElement allGroup = new CallStackElement("All", AllGroupDescriptor.getInstance(), null, null);
        callGraph.getElements().forEach(g -> {
            addGroupData(g, callGraph, allGroup, cg);
        });
        return cg;
    }

    private static void searchForGroups(ICallStackElement element, ICallStackGroupDescriptor groupBy, CallGraph callGraph, @Nullable ICallStackElement parentElement, CallGraph newCg) {
        if (element.getGroup().equals(groupBy)) {
            ICallStackElement groupedElement = new CallStackElement(element.getName(), groupBy, null, parentElement);
            if (parentElement != null) {
                parentElement.addChild(groupedElement);
            }
            addGroupData(element, callGraph, groupedElement, newCg);
            return;
        }
        // Maybe the children will be grouped, but this element goes will go in the map with no callsite
        ICallStackElement groupedElement = new CallStackElement(element.getName(), element.getGroup(), element.getNextGroup(), parentElement);
        if (parentElement != null) {
            parentElement.addChild(groupedElement);
        }
        element.getChildren().forEach(g -> {
            searchForGroups(g, groupBy, callGraph, groupedElement, newCg);
        });
    }

}
