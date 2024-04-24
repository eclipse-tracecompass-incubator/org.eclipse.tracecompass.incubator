/*******************************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.executioncomparision.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackElement;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackSymbol;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.AggregatedCallSite;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.CallGraph;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.ICallGraphProvider2;
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.analysis.profiling.core.tree.ITree;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeProvider;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeSet;
import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTree;
import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTreeSet;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.ScopeLog;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTreeProvider;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTreeSet;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2.AggregatedCalledFunction;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.google.common.collect.Iterables;

/**
 * build a differential call graph using the differentialWeightedTreeSet from
 * two sets of call graphs.
 *
 *
 * @author Fateme Faraji Daneshgar
 */
@SuppressWarnings("restriction")
public class DifferentialSeqCallGraphAnalysis extends TmfAbstractAnalysisModule {

    /**
     * The ID
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.executioncomparision.diffcallgraph"; //$NON-NLS-1$
    private static final Logger LOGGER = TraceCompassLog.getLogger(DifferentialSeqCallGraphAnalysis.class);

    private static final String MERGE = "Merge"; //$NON-NLS-1$
    private @Nullable DifferentialCallGraphProvider fDifferentialCallGraphProvider;
    private static Map<String, String> fcallStackAnalysisMap = new HashMap<>();
    private static Map<String, ICallGraphProvider2> fTraceCallGraphRegistry = new HashMap<>();
    private ITmfTimestamp fStartA = TmfTimestamp.BIG_BANG;
    private ITmfTimestamp fEndA = TmfTimestamp.BIG_CRUNCH;
    private ITmfTimestamp fStartB = TmfTimestamp.BIG_BANG;
    private ITmfTimestamp fEndB = TmfTimestamp.BIG_CRUNCH;
    private String fStatistic = ""; //$NON-NLS-1$
    private List<String> fTraceListA = new ArrayList<>();
    private List<String> fTraceListB = new ArrayList<>();
    private @Nullable Job fDiffJob = null;

    /**
     * Constructor
     */
    public DifferentialSeqCallGraphAnalysis() {
        super();
        // TODO: Make a way to register tracetype->callstack IDs.
        fcallStackAnalysisMap.put("org.eclipse.tracecompass.incubator.traceevent.core.trace", "org.eclipse.tracecompass.incubator.traceevent.analysis.callstack"); //$NON-NLS-1$ //$NON-NLS-2$
        fcallStackAnalysisMap.put("org.eclipse.linuxtools.lttng2.ust.tracetype", "org.eclipse.tracecompass.incubator.callstack.core.lttng.ust"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Refresh differential call graph
     *
     * @param monitor
     *            the progress monitor
     */
    public void refreshDiffCG(@Nullable IProgressMonitor monitor) {
        try (ScopeLog sl = new ScopeLog(LOGGER, Level.CONFIG, "DifferentialSequenceCGA::refresh()")) { //$NON-NLS-1$
            Collection<WeightedTree<ICallStackSymbol>> originalTree = new ArrayList<>();
            Collection<WeightedTree<ICallStackSymbol>> diffTree = new ArrayList<>();
            /*
             * if (fEndA == null || fStartA == null) { fEndA =
             * TmfTimestamp.BIG_CRUNCH; } if (fStartA == null) { fStartA =
             * TmfTimestamp.BIG_BANG; } if (fEndB == null) { fEndB =
             * TmfTimestamp.BIG_CRUNCH; } if (fStartB == null) { fStartB =
             * TmfTimestamp.BIG_BANG; }
             */
            WeightedTreeSet<ICallStackSymbol, Object> callGraphA = mergeCallGraph(fStartA, fEndA, fTraceListA);
            Collection<@NonNull ?> processes = callGraphA.getTreesForNamed(MERGE);
            for (Object process : processes) {
                originalTree.add((AggregatedCalledFunction) process);
            }

            WeightedTreeSet<ICallStackSymbol, Object> callGraphB = mergeCallGraph(fStartB, fEndB, fTraceListB);
            processes = callGraphB.getTreesForNamed(MERGE);
            for (Object process : processes) {
                diffTree.add((AggregatedCalledFunction) process);
            }

            Collection<DifferentialWeightedTree<ICallStackSymbol>> trees;
            if (diffTree.isEmpty()) {
                trees = ParametricWeightedTreeUtils.diffTrees(diffTree, originalTree, fStatistic);
            } else {
                trees = ParametricWeightedTreeUtils.diffTrees(originalTree, diffTree, fStatistic);
            }

            IWeightedTreeProvider<ICallStackSymbol, ICallStackElement, AggregatedCallSite> instrumentedCallStackAnalysis = Iterables.get(fTraceCallGraphRegistry.values(), 0);
            setDifferentialCallGraphProvider(new DifferentialCallGraphProvider(instrumentedCallStackAnalysis, trees));
        }
    }

    /**
     * Merge callgraph
     *
     * @param start
     *            the start time
     * @param end
     *            the end time stamp
     * @param traceList
     *            the list of traces to merge
     * @return WeightedTreeSet<ICallStackSymbol, Object> the merged call graph
     *
     */
    public WeightedTreeSet<ICallStackSymbol, Object> mergeCallGraph(ITmfTimestamp start, ITmfTimestamp end, List<String> traceList) {
        try (ScopeLog sl = new ScopeLog(LOGGER, Level.FINE, "DifferenentialSequenceCGA::MergeCallGraph")) { //$NON-NLS-1$
            List<CallGraph> cGList = new ArrayList<>();
            WeightedTreeSet<ICallStackSymbol, Object> newTreeSet = new WeightedTreeSet<>();
            String mainGroup = MERGE;

            for (String traceName : traceList) {
                ICallGraphProvider2 instrumentedCallStackAnalysis = fTraceCallGraphRegistry.get(traceName);
                if (instrumentedCallStackAnalysis != null) {
                    cGList.add(instrumentedCallStackAnalysis.getCallGraph(start, end));
                }
            }

            for (CallGraph callGraph : cGList) {
                Collection<ICallStackElement> elements = getLeafElements(callGraph);
                for (ICallStackElement element : elements) {
                    recurseAddElementData(element, mainGroup, callGraph, newTreeSet);
                }
            }
            return newTreeSet;
        }
    }

    /**
     * get CallGraph
     *
     * @return DifferentialCallGraph
     */
    public IWeightedTreeSet<ICallStackSymbol, Object, DifferentialWeightedTree<ICallStackSymbol>> getCallGraph() {
        DifferentialCallGraphProvider differentialCallGraphProvider = getDifferentialCallGraphProvider();
        if (differentialCallGraphProvider != null) {
            return differentialCallGraphProvider.getTreeSet();
        }
        return new DifferentialWeightedTreeSet<>();
    }

    /**
     * Get the differential weighted tree provider
     *
     * @param monitor
     *            the monitor, can be null
     * @return the differential weighted provider or null
     */
    public @Nullable DifferentialWeightedTreeProvider<?> getDiffProvider(@Nullable IProgressMonitor monitor) {
        if (fTraceCallGraphRegistry.isEmpty()) {
            InstrumentedCallStackAnalysis callGraphModule;
            ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
            Collection<ITmfTrace> traceSet = TmfTraceManager.getTraceSet(trace);
            for (ITmfTrace traceMember : traceSet) {
                Iterable<InstrumentedCallStackAnalysis> modules = TmfTraceUtils.getAnalysisModulesOfClass(traceMember, InstrumentedCallStackAnalysis.class);
                for (InstrumentedCallStackAnalysis module : modules) {
                    if (module.getId().equals(fcallStackAnalysisMap.get(traceMember.getTraceTypeId()))) {
                        callGraphModule = module;
                        callGraphModule.schedule();
                        fTraceCallGraphRegistry.put(String.valueOf(traceMember.getName()), callGraphModule);
                        callGraphModule.waitForCompletion();
                        break;
                    }
                }
            }
            refreshDiffCG(monitor);
        }
        return getDifferentialCallGraphProvider();

    }

    /**
     * mergeCG merge two aggregated called functions
     *
     * @param cg1
     *            first aggregated called function
     * @param cg2
     *            second aggregated called function
     * @return AggregatedCalledFunction the resulting aggregated called function
     */
    public AggregatedCalledFunction mergeCG(AggregatedCalledFunction cg1, AggregatedCalledFunction cg2) {
        AggregatedCalledFunction merged = cg1.copyOf();
        merged.meanData(cg2);
        /// As the merge function in Weighted tree adds the values og two trees,
        /// we need to divide them by 2.
        return merged;

    }

    private static Collection<ICallStackElement> getLeafElements(CallGraph weightedTree) {
        Collection<ICallStackElement> elements = weightedTree.getElements();
        List<ICallStackElement> leafGroups = new ArrayList<>();
        for (ICallStackElement group : elements) {
            leafGroups.addAll(getLeafElement(group));
        }
        return leafGroups;

    }

    private static List<ICallStackElement> getLeafElement(ICallStackElement group) {
        if (group.isLeaf()) {
            return Collections.singletonList(group);
        }
        List<ICallStackElement> leafGroups = new ArrayList<>();
        group.getChildrenElements().forEach(g -> leafGroups.addAll(getLeafElement(g)));
        return leafGroups;
    }

    private static void recurseAddElementData(ICallStackElement element, String group, CallGraph callGraph, WeightedTreeSet<ICallStackSymbol, Object> newTreeSet) {

        // Add the current level of trees to the new tree set
        for (AggregatedCallSite tree : callGraph.getCallingContextTree(element)) {
            newTreeSet.addWeightedTree(group, tree.copyOf());
        }

        // Recursively add the next level of elements
        ICallStackElement treeEl = element;
        Collection<ITree> children = treeEl.getChildren();
        for (ITree child : children) {
            if (child instanceof ICallStackElement) {
                recurseAddElementData((ICallStackElement) child, group, callGraph, newTreeSet);
            }
        }
    }

    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) throws TmfAnalysisException {
        return true;
    }

    @Override
    public boolean waitForCompletion() {
        return true;

    }

    @Override
    protected void canceling() {

    }

    /**
     * @param signal
     *            TmfComparisonFilteringUpdatedSignal raised when filtering
     *            parameter are changed
     *
     */
    @TmfSignalHandler
    public void selectionRangeUpdated(TmfComparisonFilteringUpdatedSignal signal) {
        // tuning start and end times
        fStartA = TmfTimestamp.BIG_BANG.equals(signal.getBeginTimeA()) ? fStartA : signal.getBeginTimeA();
        fEndA = TmfTimestamp.BIG_CRUNCH.equals(signal.getEndTimeA()) ? fEndA : signal.getEndTimeA();
        fStartB = TmfTimestamp.BIG_BANG.equals(signal.getBeginTimeB()) ? fStartB : signal.getBeginTimeB();
        fEndB = TmfTimestamp.BIG_CRUNCH.equals(signal.getEndTimeB()) ? fEndB : signal.getEndTimeB();
        // tuning fStatistic
        String statistic = signal.getStatistic();
        fStatistic = (statistic == null) ? fStatistic : statistic;
        // tuning fTraceList
        List<String> traceListA = signal.getTraceListA();
        if (traceListA != null) {
            for (String name : traceListA) {
                if (fTraceListA.contains(name)) {
                    fTraceListA.remove(name);
                } else {
                    fTraceListA.add(name);
                }
            }
        }
        List<String> traceListB = signal.getTraceListB();
        if (traceListB != null) {
            for (String name : traceListB) {
                if (fTraceListB.contains(name)) {
                    fTraceListB.remove(name);
                } else {
                    fTraceListB.add(name);
                }
            }
        }
        if (!fTraceCallGraphRegistry.isEmpty()) {
            try (ScopeLog sl = new ScopeLog(LOGGER, Level.FINE, "MakeDiffCallGraph")) { //$NON-NLS-1$
                synchronized (this) {
                    if (fDiffJob != null) {
                        fDiffJob.join();
                    }
                    fDiffJob = new Job("Make differential Callgraph") { //$NON-NLS-1$
                        @Override
                        protected IStatus run(@Nullable IProgressMonitor monitor) {
                            refreshDiffCG(monitor);
                            if (monitor != null) {
                                monitor.done();
                            }
                            return Status.OK_STATUS;
                        }
                    };
                    fDiffJob.schedule();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

    @Override
    public boolean canExecute(ITmfTrace trace) {
        return (trace instanceof TmfExperiment);

    }

    @Override
    public void dispose() {
        super.dispose();
        TmfSignalManager.deregister(this);
    }

    /**
     * @return the differentialCallGraphProvider
     */
    private @Nullable DifferentialCallGraphProvider getDifferentialCallGraphProvider() {
        return fDifferentialCallGraphProvider;
    }

    /**
     * @param differentialCallGraphProvider
     *            the differentialCallGraphProvider to set
     */
    private void setDifferentialCallGraphProvider(@Nullable DifferentialCallGraphProvider differentialCallGraphProvider) {
        fDifferentialCallGraphProvider = Objects.requireNonNull(differentialCallGraphProvider);
    }

}
