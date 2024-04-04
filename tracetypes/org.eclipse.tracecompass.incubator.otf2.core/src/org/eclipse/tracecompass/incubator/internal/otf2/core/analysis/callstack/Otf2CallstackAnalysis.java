/**********************************************************************
 * Copyright (c) 2021 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.callstack;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.profiling.core.callstack.CallStackStateProvider;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackSeries;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackSeries.IThreadIdResolver;
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.AbstractOtf2Analysis;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.IOtf2Constants;
import org.eclipse.tracecompass.incubator.otf2.core.trace.Otf2Trace;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList;

/**
 * OTF2 callstack analysis
 *
 * @author Yoann Heitz
 *
 */
public class Otf2CallstackAnalysis extends InstrumentedCallStackAnalysis {

    private static final String ID_SUFFIX = ".callstack"; //$NON-NLS-1$
    private static final int THREAD_DEPTH = 3;

    @Override
    public String getId() {
        return AbstractOtf2Analysis.getAnalysisIdFromSuffix(ID_SUFFIX);
    }

    @Override
    protected @NonNull IThreadIdResolver getCallStackTidResolver() {
        return new CallStackSeries.AttributeValueThreadResolver(THREAD_DEPTH);
    }

    @Override
    protected ITmfStateProvider createStateProvider() {
        ITmfTrace trace = Objects.requireNonNull(getTrace());
        if (trace instanceof Otf2Trace) {
            return new Otf2CallStackStateProvider(trace);
        }
        throw new IllegalStateException("Trace is not an OTF2 trace"); //$NON-NLS-1$
    }

    @Override
    protected Collection<Integer> getEdgeQuarks() {
        ITmfStateSystem ss = getStateSystem();
        if (ss == null) {
            return Collections.emptyList();
        }
        int edgeQuark = ss.optQuarkAbsolute(IOtf2Constants.EDGES);
        if (edgeQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return Collections.emptyList();
        }
        return ss.getSubAttributes(edgeQuark, false);
    }

    /**
     * Get the patterns for the clusters, nodes, processes and threads
     *
     * @return The patterns for the different levels in the state system
     */
    @Override
    protected List<String[]> getPatterns() {
        return ImmutableList.of(
                new String[] { CallStackStateProvider.PROCESSES, "*" }, //$NON-NLS-1$
                new String[] { "*" }, //$NON-NLS-1$
                new String[] { "*" }, //$NON-NLS-1$
                new String[] { "*" }); //$NON-NLS-1$
    }
}