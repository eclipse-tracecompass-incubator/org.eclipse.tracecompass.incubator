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

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.symbol.ICallStackSymbol;

/**
 * Interface that analyses who provide callgraph
 *
 * @author Geneviève Bastien
 */
public interface ICallGraphProvider {

    /**
     * Get the root elements containing the call graph data. If a group by
     * descriptor was set using {@link #setGroupBy(ICallStackGroupDescriptor)}, the
     * elements are the root element of the hierarchy grouped per this descriptor.
     *
     * @return The root elements of the call graph
     */
    Collection<ICallStackElement> getElements();

    /**
     * Get the group descriptors that describe how the elements are grouped in this
     * call graph hierarchy. This method will return the root group descriptor.
     * Children groups can be retrieved by the parent group. For call graph
     * providers who have only one series, this will be a singleton.
     *
     * @return The collection of group descriptors for this call graph
     */
    Collection<ICallStackGroupDescriptor> getGroupDescriptors();

    /**
     * Set the group descriptor by which to group the callgraph data. To aggregate
     * all data together, the {@link AllGroupDescriptor#getInstance()} can be used
     *
     * @param descriptor
     *            The descriptor by which to group the callgraph elements, or
     *            <code>null</code> will group them all together
     */
    void setGroupBy(@Nullable ICallStackGroupDescriptor descriptor);

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
    Collection<AggregatedCallSite> getCallingContextTree(ICallStackElement element);

    /**
     * Factory method to create an aggregated callsite for a symbol
     *
     * @param symbol
     *            The symbol
     * @return A new aggregated callsite
     */
    AggregatedCallSite createCallSite(ICallStackSymbol symbol);

    /**
     * @param dstGroup
     * @param callsite
     */
    void addAggregatedCallSite(ICallStackElement dstGroup, AggregatedCallSite callsite);

}
