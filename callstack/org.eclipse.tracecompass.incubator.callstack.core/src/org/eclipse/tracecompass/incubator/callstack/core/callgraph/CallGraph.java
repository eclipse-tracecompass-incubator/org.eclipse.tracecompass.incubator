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
import java.util.HashSet;
import java.util.Set;

import org.eclipse.tracecompass.incubator.analysis.core.concepts.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackGroupDescriptor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * Represents a callgraph, ie the aggregation of callsites per elements.
 *
 * TODO: Have an interface and keep the add* method internal
 *
 * @author Geneviève Bastien
 */
public class CallGraph {

    /**
     * An empty graph that can be returned when there is no other call graph
     * available
     */
    public static final CallGraph EMPTY_GRAPH = new CallGraph();

    private Set<ICallStackElement> fRootElements = new HashSet<>();
    private final Multimap<ICallStackElement, AggregatedCallSite> fCcts = HashMultimap.create();

    /**
     * Constructor
     */
    public CallGraph() {

    }

    /**
     * Gets the calling context tree for an element.
     *
     * The calling context tree is the callgraph data aggregated by keeping the
     * context of each call.
     *
     * @param element
     *            The element for which to get the calling context tree
     * @return The aggregated data for the first level of the callgraph
     */
    public Collection<AggregatedCallSite> getCallingContextTree(ICallStackElement element) {
        return fCcts.get(element);
    }

    /**
     * Add an aggregated callsite toa callstack element.
     * @param dstGroup the destination group
     * @param callsite the callsite to add
     */
    public void addAggregatedCallSite(ICallStackElement dstGroup, AggregatedCallSite callsite) {
        // Make sure the root element is present
        ICallStackElement root = dstGroup;
        ICallStackElement parent = dstGroup.getParentElement();
        while (parent != null) {
            root = parent;
            parent = parent.getParentElement();
        }
        fRootElements.add(root);
        // Add the callsite to the appropriate group
        Collection<AggregatedCallSite> callsites = fCcts.get(dstGroup);
        for (AggregatedCallSite site : callsites) {
            if (site.getSymbol().equals(callsite.getSymbol())) {
                site.merge(callsite);
                return;
            }
        }
        fCcts.put(dstGroup, callsite);
    }

    /**
     * Get the root elements containing the call graph data. If a group by
     * descriptor was set using {@link #setGroupBy(ICallStackGroupDescriptor)}, the
     * elements are the root element of the hierarchy grouped per this descriptor.
     *
     * @return The root elements of the call graph
     */
    public Collection<ICallStackElement> getElements() {
        return ImmutableSet.copyOf(fRootElements);
    }

}
