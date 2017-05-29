/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.callgraph.profiling;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.callstack.core.callgraph.AggregatedCallSite;

/**
 * @author Geneviève Bastien
 */
public class AggregatedStackTraces extends AggregatedCallSite {

    public AggregatedStackTraces(Object symbol) {
        super(symbol);
    }

    private int fCount = 1;

    @Override
    public long getLength() {
        return fCount;
    }

    @Override
    protected void mergeData(@NonNull AggregatedCallSite child) {
        fCount += child.getLength();
    }

}
