/*******************************************************************************
 * Copyright (c) 2024 École Polytechnique de Montréal, Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.executioncomparison.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTree;
import org.eclipse.tracecompass.analysis.profiling.core.tree.WeightedTreeSet;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.ScopeLog;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTree;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTreeProvider;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.WeightedTreeUtils;
import org.eclipse.tracecompass.internal.analysis.profiling.core.callgraph2.AggregatedCalledFunction;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 * Builds a differential call graph using the differentialWeightedTreeSet from
 * two sets of call graphs.
 *
 * @author Fateme Faraji Daneshgar and Vlad Arama
 */
@SuppressWarnings("restriction")
public class DifferentialSeqCallGraphAnalysis extends TmfAbstractAnalysisModule {

    /**
     * The ID
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.executioncomparison.diffcallgraph"; //$NON-NLS-1$
    private static final Logger LOGGER = TraceCompassLog.getLogger(DifferentialSeqCallGraphAnalysis.class);
    private static final String MERGE = "Merge"; //$NON-NLS-1$
    private static Map<String, String> fCallStackAnalysisMap = new HashMap<>();
    private static Map<String, ICallGraphProvider2> fTraceCallGraphRegistry = new HashMap<>();

    private @Nullable DifferentialCallGraphProvider fDifferentialCallGraphProvider;
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
        fCallStackAnalysisMap.put("org.eclipse.tracecompass.incubator.traceevent.core.trace", "org.eclipse.tracecompass.incubator.traceevent.analysis.callstack"); //$NON-NLS-1$ //$NON-NLS-2$
        fCallStackAnalysisMap.put("org.eclipse.linuxtools.lttng2.ust.tracetype", "org.eclipse.tracecompass.lttng2.ust.core.analysis.callstack"); //$NON-NLS-1$ //$NON-NLS-2$
        fCallStackAnalysisMap.put("org.eclipse.tracecompass.incubator.uftrace.trace", "org.eclipse.tracecompass.incubator.uftrace.analysis.callstack"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Refresh differential call graph
     *
     * @param monitor
     *            the progress monitor
     * @return the differential call graph provider
     */
    public DifferentialCallGraphProvider refreshDiffCG(@Nullable IProgressMonitor monitor) {
        try (ScopeLog sl = new ScopeLog(LOGGER, Level.CONFIG, "DifferentialSequenceCGA::refresh()")) { //$NON-NLS-1$

            Collection<WeightedTree<ICallStackSymbol>> originalTree = new ArrayList<>();
            Collection<WeightedTree<ICallStackSymbol>> diffTree = new ArrayList<>();
            List<String> tla = getTraceListA();
            WeightedTreeSet<ICallStackSymbol, Object> callGraphA = mergeCallGraph(fStartA, fEndA, tla);

            Collection<@NonNull ?> processes = callGraphA.getTreesForNamed(MERGE);
            for (Object process : processes) {
                String label = ""; //$NON-NLS-1$
                for(String trace : tla) {
                    ICallGraphProvider2 reg = fTraceCallGraphRegistry.get(trace);
                    if (reg != null) {
                        label = reg.toDisplayString((AggregatedCallSite) process);
                        if (!label.startsWith("0x")) { //$NON-NLS-1$
                            break;
                        }
                    }
                }
                AggregatedCalledFunction p = new ResolvedFunction(label, (AggregatedCalledFunction)process);
                originalTree.add(p);
            }

            List<String> tlb = getTraceListB();
            WeightedTreeSet<ICallStackSymbol, Object> callGraphB = mergeCallGraph(fStartB, fEndB, tlb);
            processes = callGraphB.getTreesForNamed(MERGE);
            for (Object process : processes) {
                String label = ""; //$NON-NLS-1$
                for(String trace : tlb) {
                    ICallGraphProvider2 reg = fTraceCallGraphRegistry.get(trace);
                    if (reg != null) {
                        label = reg.toDisplayString((AggregatedCallSite) process);
                        if (!label.startsWith("0x")) { //$NON-NLS-1$
                            break;
                        }
                    }
                }
                AggregatedCalledFunction p = new ResolvedFunction(label, (AggregatedCalledFunction)process);
                diffTree.add(p);
            }

            Collection<DifferentialWeightedTree<ICallStackSymbol>> trees;
            trees = WeightedTreeUtils.diffTrees(originalTree, diffTree, fStatistic);

            Collection<ICallGraphProvider2> instrumentedCallStackAnalyses = fTraceCallGraphRegistry.values();
            fDifferentialCallGraphProvider = new DifferentialCallGraphProvider(instrumentedCallStackAnalyses, trees);
            return fDifferentialCallGraphProvider;
        }
    }

    /**
     * Merges call graph
     *
     * @param start
     *            the start time stamp
     * @param end
     *            the end time stamp
     * @param traceList
     *            the list of traces to merge
     * @return WeightedTreeSet<ICallStackSymbol, Object> the merged call graph
     *
     */
    public WeightedTreeSet<ICallStackSymbol, Object> mergeCallGraph(ITmfTimestamp start, ITmfTimestamp end, List<String> traceList) {
        try (ScopeLog sl = new ScopeLog(LOGGER, Level.FINE, "DifferentialSequenceCGA::MergeCallGraph")) { //$NON-NLS-1$
            WeightedTreeSet<ICallStackSymbol, Object> newTreeSet = new WeightedTreeSet<>();
            String mainGroup = MERGE;

            List<CallGraph> cGList = addToCallGraph(start, end, traceList);

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
     * Helper function that adds the call graphs for the specified time range
     * and trace list to the call graph list.
     *
     * @param start
     *            the start time of the time range
     * @param end
     *            the end time of the time range
     * @param traceList
     *            the list of trace names
     * @return the list of call graphs
     */
    private static List<CallGraph> addToCallGraph(ITmfTimestamp start, ITmfTimestamp end, List<String> traceList) {
        List<CallGraph> cGList = new ArrayList<>();
        for (String traceName : traceList) {
            ICallGraphProvider2 instrumentedCallStackAnalysis = fTraceCallGraphRegistry.get(traceName);
            if (instrumentedCallStackAnalysis != null) {
                ITmfTrace trace = getTrace(traceName);
                if (trace != null) {
                    ITmfTimestamp traceStart = start;
                    ITmfTimestamp traceEnd = end;

                    if (traceStart.getValue() < trace.getStartTime().getValue()) {
                        traceStart = trace.getStartTime();
                    }
                    if (traceEnd.getValue() > trace.getEndTime().getValue()) {
                        traceEnd = trace.getEndTime();
                    }
                    cGList.add(instrumentedCallStackAnalysis.getCallGraph(traceStart, traceEnd));
                }
            }
        }
        return cGList;
    }

    /**
     * Gets the differential weighted tree provider
     *
     * @param monitor
     *            the progress monitor, can be null
     * @return the differential weighted provider or null
     */
    public @Nullable DifferentialWeightedTreeProvider<?> getDifferentialTreeProvider(IProgressMonitor monitor) {
        if (fTraceCallGraphRegistry.isEmpty()) {
            InstrumentedCallStackAnalysis callGraphModule;
            ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
            Collection<ITmfTrace> traceSet = TmfTraceManager.getTraceSet(trace);
            for (ITmfTrace traceMember : traceSet) {
                Iterable<InstrumentedCallStackAnalysis> modules = TmfTraceUtils.getAnalysisModulesOfClass(traceMember, InstrumentedCallStackAnalysis.class);
                for (InstrumentedCallStackAnalysis module : modules) {
                    if (module.getId().equals(fCallStackAnalysisMap.get(traceMember.getTraceTypeId()))) {
                        callGraphModule = module;
                        callGraphModule.schedule();
                        fTraceCallGraphRegistry.put(String.valueOf(traceMember.getName()), callGraphModule);
                        while (!callGraphModule.waitForCompletion()) {
                            if (monitor.isCanceled()) {
                                callGraphModule.cancel();
                                return null;
                            }
                        }
                        break;
                    }
                }
            }
        }
        refreshDiffCG(monitor);
        return fDifferentialCallGraphProvider;

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
    protected boolean executeAnalysis(IProgressMonitor monitor) {
        synchronized (this) {
            if (fDiffJob != null) {
                fDiffJob.cancel();
            }
            fDiffJob = new Job("Make differential Callgraph") { //$NON-NLS-1$
                @Override
                protected IStatus run(@Nullable IProgressMonitor progressMonitor) {
                    refreshDiffCG(progressMonitor);
                    if (progressMonitor != null) {
                        progressMonitor.done();
                    }
                    return Status.OK_STATUS;
                }
            };
            fDiffJob.schedule();
        }
        return true;
    }

    @Override
    public boolean waitForCompletion() {
        if (fDiffJob != null) {
            try {
                fDiffJob.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    protected void canceling() {
        if (fDiffJob != null) {
            fDiffJob.cancel();
        }
        super.cancel();
    }

    /**
     * Update the selection range
     *
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
            synchronizedListAdd(traceListA, fTraceListA);

        }
        List<String> traceListB = signal.getTraceListB();
        if (traceListB != null) {
            synchronizedListAdd(traceListB, fTraceListB);

        }
        if (!fTraceCallGraphRegistry.isEmpty()) {
            try (ScopeLog sl = new ScopeLog(LOGGER, Level.FINE, "MakeDiffCallGraph")) { //$NON-NLS-1$
                IProgressMonitor monitor = new NullProgressMonitor();
                executeAnalysis(monitor);
            }
        }
    }

    /**
     * Helper function that adds all elements from the given traceList to the
     * given fTraceList in a thread-safe manner.
     *
     * @param traceList
     *            the list of strings to be added
     * @param fTraceList
     *            the list to which the strings will be added
     * @return the synchronized list after adding the elements
     */
    private static List<String> synchronizedListAdd(List<String> traceList, List<String> fTraceList) {
        List<String> synchronizedList = Collections.synchronizedList(fTraceList);
        synchronized (synchronizedList) {
            synchronizedList.clear();
            for (String name : traceList) {
                synchronizedList.add(name);
            }
        }
        return synchronizedList;
    }

    @Override
    public boolean canExecute(ITmfTrace trace) {
        if (trace instanceof TmfExperiment) {
            for (ITmfTrace individualTrace : ((TmfExperiment) trace).getTraces()) {
                Iterable<IAnalysisModule> modules = individualTrace.getAnalysisModules();
                for (IAnalysisModule module : modules) {
                    if (fCallStackAnalysisMap.containsValue(module.getId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void dispose() {
        try {
            cancel();
            synchronized (this) {
                if (fDiffJob != null) {
                    fDiffJob.cancel();
                    fDiffJob = null;
                }

                // Clear all data structures and fields
                fTraceCallGraphRegistry.clear();
                fTraceListA.clear();
                fTraceListB.clear();
                fDifferentialCallGraphProvider = null;

                fStartA = TmfTimestamp.BIG_BANG;
                fEndA = TmfTimestamp.BIG_CRUNCH;
                fStartB = TmfTimestamp.BIG_BANG;
                fEndB = TmfTimestamp.BIG_CRUNCH;
            }
            TmfSignalManager.deregister(this);

        } finally {
            super.dispose();
        }
    }

    private static @Nullable ITmfTrace getTrace(String traceName) {
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        Collection<ITmfTrace> traceSet = TmfTraceManager.getTraceSet(trace);
        for (ITmfTrace traceMember : traceSet) {
            if (traceMember.getName().equals(traceName)) {
                return traceMember;
            }
        }
        return null;
    }

    private List<String> getTraceListA() {
        return new ArrayList<>(fTraceListA);
    }

    private List<String> getTraceListB() {
        return new ArrayList<>(fTraceListB);
    }

}
