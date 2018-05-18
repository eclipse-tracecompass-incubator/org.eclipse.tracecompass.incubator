/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.provider;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Provide arrows for a flame chart. These arrows may come from any other flame
 * chart analysis.
 *
 * @author Geneviève Bastien
 */
public class FlameChartArrowProvider {

    private final ITmfTrace fTrace;

    /**
     * Constructor
     *
     * @param trace
     *            The trace for which this data provider applies
     */
    public FlameChartArrowProvider(ITmfTrace trace) {
        fTrace = trace;
    }

    public List<ITmfStateInterval> fetchArrows(TimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        long start = filter.getStart();
        long end = filter.getEnd();

        InstrumentedCallStackAnalysis csModule = null;
        Iterable<InstrumentedCallStackAnalysis> modules = TmfTraceUtils.getAnalysisModulesOfClass(fTrace, InstrumentedCallStackAnalysis.class);
        // TODO Support many analysis modules, here we take only the first one
        Iterator<InstrumentedCallStackAnalysis> iterator = modules.iterator();
        if (!iterator.hasNext()) {
            return Collections.emptyList();
        }
        csModule = iterator.next();
        csModule.schedule();
        List<@NonNull ITmfStateInterval> edges = csModule.getLinks(start, end, monitor == null ? new NullProgressMonitor() : monitor);

        return edges;
    }
}
