/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.sampled.callgraph;

import org.eclipse.tracecompass.incubator.analysis.core.concepts.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ICallStackSymbol;

/**
 * A data structure aggregating data from the callstack for sampled call stack
 * data. It counts the number of times each frame pointer was present in a given
 * stack
 *
 * @author Geneviève Bastien
 */
public class AggregatedStackTraces extends AggregatedCallSite {

    /**
     * Constructor
     *
     * @param symbol
     *            The symbol for this frame pointer
     */
    public AggregatedStackTraces(ICallStackSymbol symbol) {
        super(symbol, 1);
    }

    private AggregatedStackTraces(AggregatedStackTraces toCopy) {
        super(toCopy);
    }

    @Override
    public AggregatedStackTraces copyOf() {
        return new AggregatedStackTraces(this);
    }

}
