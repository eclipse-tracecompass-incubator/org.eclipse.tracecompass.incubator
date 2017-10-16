/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.callgraph;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.model.ProcessStatus;
import org.eclipse.tracecompass.analysis.timing.core.statistics.IStatistics;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ICallStackSymbol;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ProcessStatusInterval;
import org.eclipse.tracecompass.incubator.analysis.core.model.IHostModel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * This class represents a function call in a certain level in the call stack.
 * It's used to build an aggregation segment tree (aggregated by depth and
 * callers). Per example,the two calls to the function A() in the call graph
 * below will be combined into one node in the generated tree:
 *
 * <pre>
 *   (Depth=0)      main              main
 *               ↓↑  ↓↑   ↓↑    =>   ↓↑   ↓↑
 *   (Depth=1)  A()  B()  A()       A()   B()
 * </pre>
 *
 * @author Sonia Farrah
 *
 */
public class AggregatedCalledFunction extends AggregatedCallSite {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private final AggregatedCalledFunctionStatistics fStatistics;
    private long fDuration = 0;
    private long fSelfTime = 0;
    private long fCpuTime = IHostModel.TIME_UNKNOWN;
    private int fProcessId;
    private Map<ProcessStatus, AggregatedThreadStatus> fProcessStatuses = new HashMap<>();

    /**
     * Constructor, parent is not null
     *
     * @param symbol
     *            The symbol of the function
     */
    public AggregatedCalledFunction(ICallStackSymbol symbol) {
        super(symbol);
        fStatistics = new AggregatedCalledFunctionStatistics();
        fProcessId = -1;
    }

    /**
     * copy constructor
     *
     * @param toCopy Object to copy
     */
    public AggregatedCalledFunction(AggregatedCalledFunction toCopy) {
        super(toCopy);
        fStatistics = new AggregatedCalledFunctionStatistics();
        fStatistics.merge(toCopy.fStatistics);
        fProcessId = toCopy.fProcessId;
        fDuration = toCopy.fDuration;
        fSelfTime = toCopy.fSelfTime;
        fCpuTime = toCopy.fCpuTime;
        mergeProcessStatuses(toCopy);
    }

    @Override
    public long getLength() {
        return fDuration;
    }

    @Override
    public AggregatedCalledFunction copyOf() {
        return new AggregatedCalledFunction(this);
    }

    @Override
    protected void mergeData(@NonNull AggregatedCallSite other) {
        if (!(other instanceof AggregatedCalledFunction)) {
            return;
        }
        AggregatedCalledFunction otherFct = (AggregatedCalledFunction) other;

        addToDuration(otherFct.getDuration());
        addToSelfTime(otherFct.getSelfTime());
        addToCpuTime(otherFct.getCpuTime());
        getFunctionStatistics().merge(otherFct.getFunctionStatistics(), true);
        mergeProcessStatuses(otherFct);
    }

    private void mergeProcessStatuses(AggregatedCalledFunction other) {
        Map<ProcessStatus, AggregatedThreadStatus> processStatuses = other.fProcessStatuses;
        for (Entry<ProcessStatus, AggregatedThreadStatus> entry : processStatuses.entrySet()) {
            AggregatedThreadStatus status = fProcessStatuses.get(entry.getKey());
            if (status == null) {
                status = new AggregatedThreadStatus(entry.getKey());
            }
            status.merge(entry.getValue());
            fProcessStatuses.put(entry.getKey(), status);
        }
    }

    @Override
    public Map<String, IStatistics<?>> getStatistics() {
        ImmutableMap.Builder<String, IStatistics<?>> builder = new ImmutableMap.Builder<>();
        builder.put(String.valueOf(Messages.CallGraphStats_Duration), getFunctionStatistics().getDurationStatistics());
        builder.put(String.valueOf(Messages.CallGraphStats_SelfTime), getFunctionStatistics().getSelfTimeStatistics());
        builder.put(String.valueOf(Messages.CallGraphStats_CpuTime), getFunctionStatistics().getCpuTimesStatistics());
        return builder.build();
    }

    /**
     * Add a new callee into the Callees list. If the function exists in the
     * callees list, the new callee's duration will be added to its duration and
     * it'll combine their callees.
     *
     * @param child
     *            The callee to add to this function
     * @param aggregatedChild
     *            The aggregated data of the callee
     */
    public synchronized void addChild(AbstractCalledFunction child, AggregatedCalledFunction aggregatedChild) {
        // Update the child's statistics with itself
        fSelfTime -= aggregatedChild.getDuration();
        aggregatedChild.addFunctionCall(child);
        super.addCallee(aggregatedChild);
    }

    /**
     * Adds a function call to this aggregated called function data. The called
     * function must have the same symbol as this aggregate data and its
     * statistics will be added to this one. This should be a function at the
     * same level as this one.
     *
     * @param function
     *            The function that was called
     */
    public synchronized void addFunctionCall(AbstractCalledFunction function) {
        // FIXME: Aren't the statistics enough? Do we really need duration, self
        // time and cpu time here?
        addToDuration(function.getLength());
        addToSelfTime(function.getSelfTime());
        addToCpuTime(function.getCpuTime());
        fProcessId = function.getProcessId();
        getFunctionStatistics().update(function);
    }

    /**
     * Modify the function's duration
     *
     * @param duration
     *            The amount to increment the duration by
     */
    private void addToDuration(long duration) {
        fDuration += duration;
    }

    // /**
    // * Merge the callees of two functions.
    // *
    // * @param firstNode
    // * The first parent secondNode The second parent
    // */
    // private static void mergeChildren(AggregatedCalledFunction firstNode,
    // AggregatedCalledFunction secondNode) {
    // for (Map.Entry<Object, AggregatedCalledFunction> FunctionEntry :
    // secondNode.fChildren.entrySet()) {
    // Object childSymbol = NonNullUtils.checkNotNull(FunctionEntry.getKey());
    // AggregatedCalledFunction secondNodeChild =
    // NonNullUtils.checkNotNull(FunctionEntry.getValue());
    // AggregatedCalledFunction aggregatedCalledFunction =
    // firstNode.fChildren.get(childSymbol);
    // if (aggregatedCalledFunction == null) {
    // firstNode.fChildren.put(secondNodeChild.getSymbol(), secondNodeChild);
    // } else {
    // // combine children
    // AggregatedCalledFunction firstNodeChild = aggregatedCalledFunction;
    // merge(firstNodeChild, secondNodeChild, true);
    // firstNode.fChildren.replace(firstNodeChild.getSymbol(), firstNodeChild);
    // }
    // }
    // }
    //
    // /**
    // * Merge two functions, add durations, self times, increment the calls,
    // * update statistics and merge children.
    // *
    // * @param destination
    // * the node to merge to
    // * @param source
    // * the node to merge
    // */
    // private static void merge(AggregatedCalledFunction destination,
    // AggregatedCalledFunction source, boolean isGroup) {
    // long sourceDuration = source.getDuration();
    // long sourceSelfTime = source.getSelfTime();
    // destination.addToDuration(sourceDuration);
    // destination.addToSelfTime(sourceSelfTime);
    // destination.addToCpuTime(source.getCpuTime());
    // destination.getFunctionStatistics().merge(source.getFunctionStatistics(),
    // isGroup);
    // // merge the children callees.
    // mergeChildren(destination, source);
    // }

    private void addToCpuTime(long cpuTime) {
        if (cpuTime != IHostModel.TIME_UNKNOWN) {
            if (fCpuTime == IHostModel.TIME_UNKNOWN) {
                fCpuTime = 0;
            }
            fCpuTime += cpuTime;
        }
    }

    /**
     * The function's duration
     *
     * @return The duration of the function
     */
    public long getDuration() {
        return fDuration;
    }

    /**
     * The number of calls of a function
     *
     * @return The number of calls of a function
     */
    public long getNbCalls() {
        return fStatistics.getDurationStatistics().getNbElements();
    }

    /**
     * The self time of an aggregated function
     *
     * @return The self time
     */
    public long getSelfTime() {
        return fSelfTime;
    }

    /**
     * Add to the self time of an aggregated function
     *
     * @param selfTime
     *            The amount of self time to add
     */
    private void addToSelfTime(long selfTime) {
        fSelfTime += selfTime;
    }

    /**
     * The CPU time of an aggregated function
     *
     * @return The CPU time, or {@link IHostModel#TIME_UNKNOWN} if the CPU time
     *         is not known
     */
    public long getCpuTime() {
        return fCpuTime;
    }

    /**
     * The process ID of the trace application.
     *
     * @return The process Id
     */
    public int getProcessId() {
        return fProcessId;
    }

    /**
     * Add a process status interval tot his called function
     *
     * @param interval
     *            The process status interval
     */
    public void addKernelStatus(ProcessStatusInterval interval) {
        ProcessStatus processStatus = interval.getProcessStatus();
        AggregatedThreadStatus status = fProcessStatuses.get(processStatus);
        if (status == null) {
            status = new AggregatedThreadStatus(processStatus);
            fProcessStatuses.put(processStatus, status);
        }
        status.update(interval);
    }

    @Override
    public Iterable<AggregatedCallSite> getExtraChildrenSites() {
        return ImmutableList.copyOf(fProcessStatuses.values());
    }

    /**
     * The function's statistics
     *
     * @return The function's statistics
     */
    public AggregatedCalledFunctionStatistics getFunctionStatistics() {
        return fStatistics;
    }

    @Override
    public String toString() {
        return "Aggregate Function: " + getSymbol() + ", Duration: " + getDuration() + ", Self Time: " + fSelfTime + " on " + getNbCalls() + " calls"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    }

}
