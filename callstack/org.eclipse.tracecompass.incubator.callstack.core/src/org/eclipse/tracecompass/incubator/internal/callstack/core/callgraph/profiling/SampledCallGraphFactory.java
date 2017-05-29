/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.callgraph.profiling;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.internal.callstack.core.callgraph.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.internal.callstack.core.callgraph.GroupNode;
import org.eclipse.tracecompass.incubator.internal.callstack.core.callgraph.ICallGraphFactory;
import org.eclipse.tracecompass.incubator.internal.callstack.core.callgraph.LeafGroupNode;

public class SampledCallGraphFactory implements ICallGraphFactory {

    private static @Nullable ICallGraphFactory sfInstance = null;

    public static synchronized ICallGraphFactory getInstance() {
        ICallGraphFactory instance = sfInstance;
        if (instance == null) {
            instance = new SampledCallGraphFactory();
            sfInstance = instance;
        }
        return instance;
    }

    @Override
    public GroupNode createGroupNode(String name, ICallStackGroupDescriptor descriptor) {
        // TODO Auto-generated method stub
        return new GroupNode(name, descriptor);
    }

    @Override
    public LeafGroupNode createLeafGroup(String name, ICallStackGroupDescriptor descriptor) {
        return new ProfilingGroup(name, descriptor);
    }

    @Override
    public AggregatedCallSite createAggregatedCallSite(Object symbol) {
        return new AggregatedStackTraces(symbol);
    }

}
