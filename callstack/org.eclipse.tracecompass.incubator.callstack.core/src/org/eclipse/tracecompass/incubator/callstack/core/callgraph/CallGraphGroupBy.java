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

import org.eclipse.tracecompass.incubator.callstack.core.base.CallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.internal.callstack.core.base.AllGroupDescriptor;

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
    public static Collection<ICallStackElement> groupCallGraphBy(ICallStackGroupDescriptor groupBy, Collection<ICallStackElement> elements, ICallGraphProvider cgProvider) {
        // Fast return: just aggregated all groups together
        if (groupBy.equals(AllGroupDescriptor.getInstance())) {
            return groupCallGraphByAll(elements, cgProvider);
        }

        List<ICallStackElement> grouped = new ArrayList<>();
        elements.forEach(g -> grouped.addAll(searchForGroups(g, groupBy, cgProvider)));
        return grouped;
    }

    private static void addGroupData(ICallStackElement srcGroup, ICallStackElement dstGroup, ICallGraphProvider cgProvider) {
        cgProvider.getCallingContextTree(srcGroup).forEach(ad -> {
            AggregatedCallSite callsite = cgProvider.createCallSite(ad.getSymbol());
            callsite.merge(ad);
            cgProvider.addAggregatedCallSite(dstGroup, callsite);
        });
        srcGroup.getChildren().forEach(g -> addGroupData(g, dstGroup, cgProvider));
    }

    private static Collection<ICallStackElement> groupCallGraphByAll(Collection<ICallStackElement> groups, ICallGraphProvider cgProvider) {
        // Fast return: just aggregate all groups together
        ICallStackElement allGroup = new CallStackElement("All", AllGroupDescriptor.getInstance(), null, null);
        groups.forEach(g -> addGroupData(g, allGroup, cgProvider));
        return Collections.singleton(allGroup);
    }

    private static Collection<? extends ICallStackElement> searchForGroups(ICallStackElement element, ICallStackGroupDescriptor descriptor, ICallGraphProvider cgProvider) {
        if (element.getGroup().equals(descriptor)) {
            ICallStackElement groupedElement = new CallStackElement(element.getName(), descriptor);
            addGroupData(element, groupedElement, cgProvider);
            return Collections.singleton(groupedElement);
        }
        ICallStackGroupDescriptor nextGroup = descriptor.getNextGroup();
        if (nextGroup == null) {
            return Collections.singleton(element);
        }
        List<ICallStackElement> grouped = new ArrayList<>();
        element.getChildren().forEach(g -> grouped.addAll(searchForGroups(g, nextGroup, cgProvider)));
        return grouped;
    }

}
