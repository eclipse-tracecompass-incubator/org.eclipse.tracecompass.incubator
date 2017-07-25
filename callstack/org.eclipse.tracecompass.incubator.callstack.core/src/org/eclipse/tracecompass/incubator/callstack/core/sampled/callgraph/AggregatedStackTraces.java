/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.sampled.callgraph;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.AggregatedCallSite;

/**
 * A data structure aggregating data from the callstack for sampled call stack
 * data. It counts the number of times each frame pointer was present in a given
 * stack
 *
 * @author Geneviève Bastien
 */
public class AggregatedStackTraces extends AggregatedCallSite {

    private int fCount = 1;

    /**
     * Constructor
     *
     * @param symbol
     *            The symbol for this frame pointer
     */
    public AggregatedStackTraces(Object symbol) {
        super(symbol);
    }

    @Override
    public long getLength() {
        return fCount;
    }

    @Override
    protected void mergeData(@NonNull AggregatedCallSite child) {
        fCount += child.getLength();
    }

}
