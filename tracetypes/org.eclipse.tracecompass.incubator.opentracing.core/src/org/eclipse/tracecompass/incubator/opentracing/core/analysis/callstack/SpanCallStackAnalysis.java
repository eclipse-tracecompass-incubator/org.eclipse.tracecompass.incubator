package org.eclipse.tracecompass.incubator.opentracing.core.analysis.callstack;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IDataPalette;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.IWeightedTreeGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.CallGraph;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.SymbolAspect;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackSeries;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackStateProvider;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList;

/**
 *
 */
public abstract class SpanCallStackAnalysis extends InstrumentedCallStackAnalysis {

    private static final String[] DEFAULT_PROCESSES_PATTERN = new String[] { CallStackStateProvider.PROCESSES, "*" }; //$NON-NLS-1$

    private static final List<String[]> PATTERNS = ImmutableList.of(DEFAULT_PROCESSES_PATTERN);

    private @Nullable CallStackSeries fCallStacks;

    private final SpanCallGraphAnalysis fCallGraph;

    private boolean fAutomaticCallgraph=true;


    protected SpanCallStackAnalysis() {
        super();
        fCallGraph = new SpanCallGraphAnalysis(this);
    }
    @Override
    public boolean setTrace(@NonNull ITmfTrace trace) throws TmfAnalysisException {
        if (!super.setTrace(trace)) {
            return false;
        }
        return fCallGraph.setTrace(trace);
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        fCallGraph.setName(name);
    }

    @Override
    public synchronized @Nullable CallStackSeries getCallStackSeries() {
        CallStackSeries callstacks = fCallStacks;
        if (callstacks == null) {
            ITmfStateSystem ss = getStateSystem();
            ITmfTrace trace = getTrace();
            if (ss == null || trace == null) {
                return null;
            }
            callstacks = new CallStackSeries(ss, getPatterns(), 0, "", getCallStackHostResolver(trace), null); //$NON-NLS-1$
            fCallStacks = callstacks;
        }
        return callstacks;
    }


    /**
     * Get the patterns for the process, threads and callstack levels in the
     * state system
     *
     * @return The patterns for the different levels in the state system
     */
    @Override
    protected List<String[]> getPatterns() {
        return PATTERNS;
    }
    @Override
    protected boolean executeAnalysis(@Nullable IProgressMonitor monitor) {
        fCallGraph.setId(getId());
        boolean ret = super.executeAnalysis(monitor);
        if (!ret) {
            return ret;
        }
        ISegmentStore<ISegment> segmentStore = getSegmentStore();
        if (segmentStore != null) {
            sendUpdate(segmentStore);
        }
        if (fAutomaticCallgraph) {
            fCallGraph.schedule();
        }
        return true;
    }

    @Override
    public void dispose() {
        fCallGraph.dispose();
        super.dispose();
    }

    @Override
    public CallGraph getCallGraph(ITmfTimestamp start, ITmfTimestamp end) {
        fCallGraph.schedule();
        fCallGraph.waitForCompletion();
        return fCallGraph.getCallGraph(start, end);
    }

    @Override
    public CallGraph getCallGraph() {
        fCallGraph.schedule();
        fCallGraph.waitForCompletion();
        return fCallGraph.getCallGraph();
    }

    @Override
    public Collection<IWeightedTreeGroupDescriptor> getGroupDescriptors() {
        fCallGraph.schedule();
        fCallGraph.waitForCompletion();
        return fCallGraph.getGroupDescriptors();
    }

    @Override
    public String getTitle() {
        return fCallGraph.getTitle();
    }

    @Override
    public AggregatedCallSite createCallSite(Object symbol) {
        return fCallGraph.createCallSite(symbol);
    }
    @Override
    public @NonNull List<@NonNull String> getExtraDataSets() {
        return fCallGraph.getExtraDataSets();
    }

    @Override
    public MetricType getWeightType() {
        return fCallGraph.getWeightType();
    }

    @Override
    public List<MetricType> getAdditionalMetrics() {
        return fCallGraph.getAdditionalMetrics();
    }

    @Override
    public String toDisplayString(AggregatedCallSite object) {
        return fCallGraph.toDisplayString(object);
    }

    @Override
    public Object getAdditionalMetric(AggregatedCallSite object, int metricIndex) {
        return fCallGraph.getAdditionalMetric(object, metricIndex);
    }

    @Override
    public IDataPalette getPalette() {
        // Schedule the analysis (it will likely be needed) but don't wait for
        // completion as this should be a fast return.
        fCallGraph.schedule();
        return fCallGraph.getPalette();
    }


    @Override

    public Iterable<ISegmentAspect> getSegmentAspects() {

        return Collections.singletonList(SymbolAspect.SYMBOL_ASPECT);
    }
}