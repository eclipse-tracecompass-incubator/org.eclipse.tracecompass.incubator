/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.callgraph.profiling;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.callstack.core.callstack.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.internal.callstack.core.callgraph.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.internal.callstack.core.callgraph.LeafGroupNode;

/**
 * @author Geneviève Bastien
 */
public class ProfilingGroup extends LeafGroupNode {

    public ProfilingGroup(@NonNull String name, ICallStackGroupDescriptor descriptor) {
        super(name, descriptor);
    }

    /**
     * @param stackTrace
     */
    public void addStackTrace(Object[] stackTrace) {

    }

    /**
     * Add a stack trace to this group, such that the symbol at position 0 is the
     * top of the stack, ie the last symbol called.
     *
     * @param stackTrace The stack trace to add to the group
     */
    public void addStackTrace(long[] stackTrace) {
        if (stackTrace.length == 0) {
            return;
        }
        // Create the callsite for this stack trace
        AggregatedCallSite prevCallsite = new AggregatedStackTraces(stackTrace[stackTrace.length - 1]);
        for (int i = stackTrace.length - 2; i >= 0; i--) {
            AggregatedCallSite callsite = new AggregatedStackTraces(stackTrace[i]);
            callsite.addChild(prevCallsite);
            prevCallsite = callsite;
        }
        addAggregatedData(prevCallsite);
    }

}
