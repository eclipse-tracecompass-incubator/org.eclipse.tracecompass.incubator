/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.perf.profiling.core.symbol;

import java.util.Collections;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement.PriorityLevel;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAnalysisEventRequirement;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList;

/**
 * Analysis for perf mmap events, linking a process to its libraries
 *
 * @author Geneviève Bastien
 */
public class PerfMmapAnalysisModule extends TmfStateSystemAnalysisModule {

    /**
     * The ID of this module
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.perf.profiling.core.symbol.mmap"; //$NON-NLS-1$

    private static final TmfAbstractAnalysisRequirement REQUIREMENT = new TmfAnalysisEventRequirement(ImmutableList.of("perf_mmap", "perf_mmap2"), PriorityLevel.AT_LEAST_ONE); //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * Constructor
     */
    public PerfMmapAnalysisModule() {

    }

    @Override
    protected ITmfStateProvider createStateProvider() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            throw new NullPointerException("Creating state provider: trace should not be null"); //$NON-NLS-1$
        }
        return new PerfMmapStateProvider(trace);
    }

    @Override
    public @NonNull Iterable<@NonNull TmfAbstractAnalysisRequirement> getAnalysisRequirements() {
        return Collections.singleton(REQUIREMENT);
    }

}
