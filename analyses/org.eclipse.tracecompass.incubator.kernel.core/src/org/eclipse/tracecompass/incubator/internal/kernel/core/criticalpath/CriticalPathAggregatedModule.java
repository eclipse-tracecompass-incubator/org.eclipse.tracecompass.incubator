/*******************************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.core.criticalpath;

import java.util.Collection;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.criticalpath.CriticalPathModule;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IDataPalette;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeProvider;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeSet;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.WeightedTree;
import org.eclipse.tracecompass.tmf.core.TmfStrings;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfStartAnalysisSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

/**
 * Analysis modules that aggregates the states of the critical path into a
 * weighted tree structure, to display in flame graphs or compare with other
 * graphs.
 *
 * @author Geneviève Bastien
 */
public class CriticalPathAggregatedModule extends TmfAbstractAnalysisModule implements IWeightedTreeProvider<Object, String, WeightedTree<Object>> {

    private static final MetricType DURATION_METRIC = new MetricType(Objects.requireNonNull(TmfStrings.duration()), DataType.NANOSECONDS, null);

    private @Nullable CriticalPathModule fModule = null;
    private @Nullable CriticalPathWeighted fCritPathCg = null;

    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) throws TmfAnalysisException {
        CriticalPathModule module = fModule;
        if (module == null) {
            return false;
        }
        if (!module.waitForCompletion(Objects.requireNonNull(monitor))) {
            return false;
        }
        fCritPathCg = CriticalPathWeighted.create(module.getCriticalPath());
        return true;
    }

    /**
     * Signal handler for analysis started, we need to rebuilt the entry list
     * with updated statistics values for the current graph worker of the
     * critical path module.
     *
     * @param signal
     *            The signal
     */
    @TmfSignalHandler
    public void analysisStarted(TmfStartAnalysisSignal signal) {
        IAnalysisModule analysis = signal.getAnalysisModule();
        if (analysis instanceof CriticalPathModule) {
            CriticalPathModule criticalPath = (CriticalPathModule) analysis;
            Collection<ITmfTrace> traces = TmfTraceManager.getTraceSetWithExperiment(getTrace());
            if (traces.contains(criticalPath.getTrace())) {
                cancel();
                fModule = criticalPath;
                fCritPathCg = null;
                resetAnalysis();
                schedule();
            }
        }
    }

    @Override
    protected void canceling() {
        // Nothing to do
    }

    @Override
    public MetricType getWeightType() {
        return DURATION_METRIC;
    }

    @Override
    public String getTitle() {
        return "What the process is waiting for"; //$NON-NLS-1$
    }

    @Override
    public IWeightedTreeSet<Object, String, WeightedTree<Object>> getTreeSet() {
        CriticalPathWeighted critPathCg = fCritPathCg;
        if (critPathCg != null) {
            return critPathCg;
        }
        CriticalPathModule module = fModule;
        if (module == null) {
            return CriticalPathWeighted.create(null);
        }
        return CriticalPathWeighted.create(module.getCriticalPath());
    }

    @Override
    public IDataPalette getPalette() {
        return CriticalPathAggregatedPalette.getInstance();
    }

}
