/*******************************************************************************
 * Copyright (c) 2013, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.ICallGraphProvider;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.IFlameChartProvider;
import org.eclipse.tracecompass.incubator.internal.callstack.core.Activator;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.callgraph.CallGraphAnalysis;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.segmentstore.core.SegmentStoreFactory;
import org.eclipse.tracecompass.segmentstore.core.SegmentStoreFactory.SegmentStoreType;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.callstack.CallStackStateProvider;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

/**
 * The base classes for analyses who want to populate the CallStack state
 * system.
 *
 * If the elements in the callstack are grouped by process ID / thread ID, the
 * default state provider {@link CallStackStateProvider} can be extended, and
 * implement how to retrieve function entry and exit and process/thread IDs.
 *
 * @author Matthew Khouzam
 * @author Geneviève Bastien
 */
public abstract class InstrumentedCallStackAnalysis extends TmfStateSystemAnalysisModule implements IFlameChartProvider, ICallGraphProvider {

    /** CallStack stack-attribute */
    public static final String CALL_STACK = "CallStack"; //$NON-NLS-1$

    private static final String[] DEFAULT_PROCESSES_PATTERN = new String[] { CallStackStateProvider.PROCESSES, "*" }; //$NON-NLS-1$
    private static final String[] DEFAULT_THREADS_PATTERN = new String[] { "*" }; //$NON-NLS-1$

    private static final List<String[]> PATTERNS = ImmutableList.of(DEFAULT_PROCESSES_PATTERN, DEFAULT_THREADS_PATTERN);

    private @Nullable Collection<CallStackSeries> fCallStacks;

    private CallGraphAnalysis fCallGraph;

    private static final String LINKS_SUFFIX = ".links.ht"; //$NON-NLS-1$
    /**
     * Arrows, not really nullable, but lazy initialized
     */
    private @Nullable ISegmentStore<CallStackEdge> fLinks = null;

    /**
     * Abstract constructor (should only be called via the sub-classes'
     * constructors.
     */
    protected InstrumentedCallStackAnalysis() {
        super();
        fCallGraph = new CallGraphAnalysis(this);
    }

    private @Nullable ISegmentStore<org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackEdge> buildOnDiskSegmentStore(String fileName) {
        ITmfTrace trace = checkNotNull(getTrace());

        /* See if the data file already exists on disk */
        String dir = TmfTraceManager.getSupplementaryFileDir(trace);
        final Path file = Paths.get(dir, fileName);

        ISegmentStore<CallStackEdge> segmentStore;
        try {
            segmentStore = SegmentStoreFactory.createOnDiskSegmentStore(Objects.requireNonNull(file), CallStackEdge.READER);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e1) {
                // Ignore
            }
            Activator.getInstance().logError("Error creating segment store", e); //$NON-NLS-1$
            return null;
        }
        return segmentStore;
    }

    @Override
    public boolean setTrace(@NonNull ITmfTrace trace) throws TmfAnalysisException {
        if (!super.setTrace(trace)) {
            return false;
        }
        return fCallGraph.setTrace(trace);
    }

    @Override
    public Collection<CallStackSeries> getCallStackSeries() {
        Collection<CallStackSeries> callstacks = fCallStacks;
        if (callstacks == null) {
            ITmfStateSystem ss = getStateSystem();
            if (ss == null) {
                return Collections.emptySet();
            }
            callstacks = Collections.singleton(new CallStackSeries(ss, PATTERNS, 0, "", getHostId(), new CallStackSeries.AttributeNameThreadResolver(1))); //$NON-NLS-1$
            fCallStacks = callstacks;
        }
        return callstacks;
    }

    @Override
    protected boolean executeAnalysis(@Nullable IProgressMonitor monitor) {
        fCallGraph.setId(getId());
        boolean ret = super.executeAnalysis(monitor);
        if (!ret) {
            return ret;
        }
        fCallGraph.schedule();
        return true;
    }

    /**
     * Get the patterns for the process, threads and callstack levels in the state
     * system
     *
     * @return The patterns for the different levels in the state system
     */
    @VisibleForTesting
    protected List<String[]> getPatterns() {
        return PATTERNS;
    }

    @Override
    public @NonNull String getHostId() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return ""; //$NON-NLS-1$
        }
        return trace.getHostId();
    }

    /**
     * Get the edges (links) of the callstack
     *
     * @return a list of the edges
     */
    public ISegmentStore<CallStackEdge> getSegmentStore() {
        ISegmentStore<CallStackEdge> links = fLinks;
        if (fLinks == null) {
            links = buildOnDiskSegmentStore(String.valueOf(getId()) + LINKS_SUFFIX);
            links = links != null ? links : SegmentStoreFactory.createSegmentStore(SegmentStoreType.Fast);
            fLinks = links;
        }
        return Objects.requireNonNull(links);
    }

    /**
     * Get the groups
     *
     * @return
     */
    @Override
    public Collection<ICallStackElement> getElements() {
        return fCallGraph.getElements();
    }

    @Override
    public Collection<ICallStackGroupDescriptor> getGroupDescriptors() {
        fCallGraph.schedule();
        fCallGraph.waitForCompletion();
        return fCallGraph.getGroupDescriptors();
    }

    @Override
    public void dispose() {
        super.dispose();
        fCallGraph.dispose();
    }

    @Override
    public void setGroupBy(@Nullable ICallStackGroupDescriptor descriptor) {
        fCallGraph.setGroupBy(descriptor);
    }

    @Override
    public Collection<AggregatedCallSite> getCallingContextTree(ICallStackElement element) {
        return fCallGraph.getCallingContextTree(element);
    }

    @Override
    public AggregatedCallSite createCallSite(Object symbol) {
        return fCallGraph.createCallSite(symbol);
    }

    @Override
    public void addAggregatedCallSite(ICallStackElement dstGroup, AggregatedCallSite callsite) {
        fCallGraph.addAggregatedCallSite(dstGroup, callsite);
    }



}