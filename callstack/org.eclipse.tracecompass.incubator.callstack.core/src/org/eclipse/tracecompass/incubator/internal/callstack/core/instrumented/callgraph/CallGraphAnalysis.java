/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.callgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tracecompass.common.core.StreamUtils;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ICallStackSymbol;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ProcessStatusInterval;
import org.eclipse.tracecompass.incubator.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.incubator.analysis.core.model.ModelManager;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.CallGraph;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.ICallGraphProvider;
import org.eclipse.tracecompass.incubator.callstack.core.flamechart.CallStack;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.ICalledFunction;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.IFlameChartProvider;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackSeries;
import org.eclipse.tracecompass.incubator.callstack.core.symbol.CallStackSymbolFactory;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.InstrumentedCallStackElement;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.annotations.VisibleForTesting;

/**
 * Call stack analysis used to create a segment for each call function from an
 * entry/exit event. It builds a segment tree from the state system. An example
 * taken from the Fibonacci trace's callStack shows the structure of the segment
 * tree given by this analysis:
 *
 * <pre>
 * (Caller)  main
 *            ↓↑
 * (Callee) Fibonacci
 *           ↓↑    ↓↑
 *      Fibonacci Fibonacci
 *         ↓↑         ↓↑
 *         ...        ...
 * </pre>
 *
 * @author Sonia Farrah
 */
public class CallGraphAnalysis extends TmfAbstractAnalysisModule implements ICallGraphProvider {

    /**
     * Public ID for this analysis
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.callstack.callgraph"; //$NON-NLS-1$

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private final IFlameChartProvider fCsProvider;
    private final CallGraph fCallGraph = new CallGraph();

    /**
     * Constructor
     *
     * @param csProvider
     *            The call stack provider to use with this analysis
     */
    public CallGraphAnalysis(IFlameChartProvider csProvider) {
        super();
        fCsProvider = csProvider;
        setName(NLS.bind(Messages.CallGraphAnalysis_NamePrefix, csProvider.getName()));
    }

    @Override
    public @NonNull String getHelpText() {
        String msg = Messages.CallGraphAnalysis_Description;
        return (msg != null) ? msg : super.getHelpText();
    }

    @Override
    public void setName(String name) {
        super.setName(NLS.bind(Messages.CallGraphAnalysis_NamePrefix, name));
    }

    @Override
    public @NonNull String getHelpText(@NonNull ITmfTrace trace) {
        return getHelpText();
    }

    @Override
    public boolean canExecute(ITmfTrace trace) {
        return true;
    }

    @Override
    protected Iterable<IAnalysisModule> getDependentAnalyses() {
        return Collections.singleton(fCsProvider);
    }

    @Override
    protected boolean executeAnalysis(@Nullable IProgressMonitor monitor) {
        return executeForRange(fCallGraph, TmfTimeRange.ETERNITY, monitor);
    }

    private boolean executeForRange(CallGraph callgraph, TmfTimeRange range, @Nullable IProgressMonitor monitor) {
        ITmfTrace trace = getTrace();
        if (monitor == null || trace == null) {
            return false;
        }
        Iterable<IAnalysisModule> dependentAnalyses = getDependentAnalyses();
        for (IAnalysisModule module : dependentAnalyses) {
            if (!(module instanceof IFlameChartProvider)) {
                return false;
            }
            module.schedule();
        }
        // TODO:Look at updates while the state system's being built
        dependentAnalyses.forEach((t) -> t.waitForCompletion(monitor));

        for (IAnalysisModule module : dependentAnalyses) {
            IFlameChartProvider callstackModule = (IFlameChartProvider) module;
            IHostModel model = ModelManager.getModelFor(callstackModule.getHostId());

            for (CallStackSeries callstack : callstackModule.getCallStackSeries()) {
                if (!iterateOverCallstackSerie(callstack, model, callgraph, range.getStartTime().toNanos(), range.getEndTime().toNanos(), monitor)) {
                    return false;
                }
            }
        }
        monitor.worked(1);
        monitor.done();
        return true;
    }

    /**
     * Iterate over a callstack series. It will do a depth-first search to create
     * the callgraph
     *
     * @param callstackSerie
     *            The series to iterate over
     * @param model
     *            The model of the host on which this callstack was running
     * @param callgraph
     *            The callgraph to fill
     * @param start
     *            the start time of the request
     * @param end
     *            The end time of the request
     * @param monitor
     *            A progress monitor
     * @return Whether the series was successfully iterated over
     */
    @VisibleForTesting
    protected boolean iterateOverCallstackSerie(CallStackSeries callstackSerie, IHostModel model, CallGraph callgraph, long start, long end, IProgressMonitor monitor) {
        // The root elements are the same as the one from the callstack series
        Collection<ICallStackElement> rootElements = callstackSerie.getRootElements();
        for (ICallStackElement element : rootElements) {
            if (monitor.isCanceled()) {
                return false;
            }
            iterateOverElement(element, model, callgraph, start, end, monitor);
        }
        return true;
    }

    private void iterateOverElement(ICallStackElement element, IHostModel model, CallGraph callgraph, long start, long end, IProgressMonitor monitor) {
        // Iterator over the children of the element until we reach the leaves
        if (element.isLeaf()) {
            iterateOverLeafElement(element, model, callgraph, start, end, monitor);
            return;
        }
        for (ICallStackElement child : element.getChildren()) {
            iterateOverElement(child, model, callgraph, start, end, monitor);
        }
    }

    private void iterateOverLeafElement(ICallStackElement element, IHostModel model, CallGraph callgraph, long start, long end, IProgressMonitor monitor) {
        if (!(element instanceof InstrumentedCallStackElement)) {
            throw new IllegalStateException("Call Graph Analysis: The element does not have the right type"); //$NON-NLS-1$
        }
        InstrumentedCallStackElement insElement = (InstrumentedCallStackElement) element;
        CallStack callStack = insElement.getCallStack();

        // Create a root segment

        AbstractCalledFunction nextFunction = (AbstractCalledFunction) callStack.getNextFunction(callStack.getStartTime(), 1, null, model, start, end);
        while (nextFunction != null) {
            AggregatedCalledFunction aggregatedChild = createCallSite(CallStackSymbolFactory.createSymbol(nextFunction.getSymbol(), element, nextFunction.getStart()));
            iterateOverCallstack(element, callStack, nextFunction, 2, aggregatedChild, model, start, end, monitor);
            aggregatedChild.addFunctionCall(nextFunction);
            // Add the kernel statuses if available
            Iterator<ProcessStatusInterval> kernelStatuses = callStack.getKernelStatuses(nextFunction, -1);
            kernelStatuses.forEachRemaining(aggregatedChild::addKernelStatus);

            callgraph.addAggregatedCallSite(element, aggregatedChild);
            nextFunction = (AbstractCalledFunction) callStack.getNextFunction(nextFunction.getEnd(), 1, null, model, start, end);
        }
    }

    private void iterateOverCallstack(ICallStackElement element, CallStack callstack, ICalledFunction function, int nextLevel, AggregatedCalledFunction aggregatedCall, IHostModel model, long start, long end, IProgressMonitor monitor) {
        if (nextLevel > callstack.getMaxDepth()) {
            return;
        }
        int threadId = function.getThreadId();
        long lastSampleEnd = start;

        AbstractCalledFunction nextFunction = (AbstractCalledFunction) callstack.getNextFunction(function.getStart(), nextLevel, function, model, Math.max(function.getStart(), start), Math.min(function.getEnd(), end));
        while (nextFunction != null) {
            // Add sampling data of the time between next function and beginning of next
            // level
            if (threadId > 0) {
                Collection<AggregatedCallSite> samplingData = model.getSamplingData(threadId, lastSampleEnd, nextFunction.getStart());
                samplingData.forEach(aggregatedCall::addCallee);
                lastSampleEnd = nextFunction.getEnd();
            }
            AggregatedCalledFunction aggregatedChild = createCallSite(CallStackSymbolFactory.createSymbol(nextFunction.getSymbol(), element, nextFunction.getStart()));
            iterateOverCallstack(element, callstack, nextFunction, nextLevel + 1, aggregatedChild, model, start, end, monitor);
            aggregatedCall.addChild(nextFunction, aggregatedChild);
            nextFunction = (AbstractCalledFunction) callstack.getNextFunction(nextFunction.getEnd(), nextLevel, function, model, Math.max(function.getStart(), start), Math.min(function.getEnd(), end));
        }
        // Get the sampling to the end of the function
        if (threadId > 0) {
            Collection<AggregatedCallSite> samplingData = model.getSamplingData(threadId, lastSampleEnd, function.getEnd() - lastSampleEnd);
            samplingData.forEach(aggregatedCall::addCallee);
        }
    }

    /**
     * Get the callstack series of the providers of this analysis
     *
     * @return The collection of callstack series
     */
    public Collection<CallStackSeries> getSeries() {
        List<CallStackSeries> series = new ArrayList<>();
        for (IAnalysisModule dependent : getDependentAnalyses()) {
            if (!(dependent instanceof IFlameChartProvider)) {
                continue;
            }
            IFlameChartProvider csProvider = (IFlameChartProvider) dependent;
            series.addAll(csProvider.getCallStackSeries());
        }
        return series;
    }

    @Override
    protected void canceling() {
        // Do nothing
    }

    @Override
    public CallGraph getCallGraph(ITmfTimestamp start, ITmfTimestamp end) {
        CallGraph cg = new CallGraph();
        executeForRange(cg, new TmfTimeRange(start, end), new NullProgressMonitor());
        return cg;
    }

    @Override
    public CallGraph getCallGraph() {
        return fCallGraph;
    }

    @Override
    public Collection<ICallStackGroupDescriptor> getGroupDescriptors() {
        return StreamUtils.getStream(getDependentAnalyses())
                .flatMap(m -> StreamUtils.getStream(((IFlameChartProvider) m).getCallStackSeries()))
                .map(CallStackSeries::getRootGroup)
                .collect(Collectors.toList());
    }

    @Override
    public AggregatedCalledFunction createCallSite(ICallStackSymbol symbol) {
        return new AggregatedCalledFunction(symbol);
    }

}