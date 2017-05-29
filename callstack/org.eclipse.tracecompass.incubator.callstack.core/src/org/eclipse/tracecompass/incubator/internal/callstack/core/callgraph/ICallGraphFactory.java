/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.callgraph;

import org.eclipse.tracecompass.incubator.callstack.core.callstack.ICallStackGroupDescriptor;

/**
 * @author Geneviève Bastien
 */
public interface ICallGraphFactory {

    GroupNode createGroupNode(String name, ICallStackGroupDescriptor descriptor);

    LeafGroupNode createLeafGroup(String name, ICallStackGroupDescriptor descriptor);

    AggregatedCallSite createAggregatedCallSite(Object symbol);

}
