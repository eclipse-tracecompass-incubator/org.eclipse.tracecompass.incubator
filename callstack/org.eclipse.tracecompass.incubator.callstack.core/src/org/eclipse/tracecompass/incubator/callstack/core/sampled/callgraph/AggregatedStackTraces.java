/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
@Deprecated(since="0.10.0", forRemoval=true)
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
