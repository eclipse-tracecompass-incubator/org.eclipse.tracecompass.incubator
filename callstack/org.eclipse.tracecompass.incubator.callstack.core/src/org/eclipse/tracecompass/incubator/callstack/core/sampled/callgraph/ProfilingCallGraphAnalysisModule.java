/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.sampled.callgraph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.CallGraphGroupBy;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.ICallGraphProvider;
import org.eclipse.tracecompass.incubator.callstack.core.flamechart.IEventCallStackProvider;
import org.eclipse.tracecompass.incubator.callstack.core.symbol.CallStackSymbolFactory;
import org.eclipse.tracecompass.incubator.callstack.core.symbol.ICallStackSymbol;
import org.eclipse.tracecompass.incubator.internal.callstack.core.Activator;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

/**
 * The callgraph analysis module
 *
 * @author Geneviève Bastien
 */
public abstract class ProfilingCallGraphAnalysisModule extends TmfAbstractAnalysisModule implements ICallGraphProvider, IEventCallStackProvider {

    private @Nullable ITmfEventRequest fRequest;
    private final Set<ICallStackElement> fRootElements = new HashSet<>();
    private final Multimap<ICallStackElement, AggregatedCallSite> fCcts = HashMultimap.create();

    private @Nullable ICallStackGroupDescriptor fGroupBy = null;
    private @Nullable Set<ICallStackElement> fGroupedRootElements = null;
    private @Nullable Multimap<ICallStackElement, AggregatedCallSite> fGroupedCct = null;

    /**
     * Get the root elements from this call graph hierarchy
     *
     * @return The root elements
     */
    protected Collection<ICallStackElement> getRootElements() {
        return fRootElements;
    }

    /**
     * Add a root element to call graph
     *
     * @param element
     *            The root element to add
     */
    public void addRootElement(ICallStackElement element) {
        fRootElements.add(element);
    }

    @Override
    public Collection<ICallStackElement> getElements() {
        ICallStackGroupDescriptor groupBy = fGroupBy;
        Collection<ICallStackElement> elements = fRootElements;
        if (groupBy == null) {
            return ImmutableList.copyOf(elements);
        }

        // FIXME: The grouping part of this class is copy-pasted from
        // instrumented call graph. either have a base class or move this
        // functionnality to its own class
        Set<ICallStackElement> groupedElements = fGroupedRootElements;
        if (groupedElements == null) {
            Map<ICallStackElement, Collection<AggregatedCallSite>> groupCallGraphBy = CallGraphGroupBy.groupCallGraphBy(groupBy, elements, this);
            // Get the root elements that have no parent
            groupedElements = groupCallGraphBy.keySet().stream()
                    .filter(element -> element.getParentElement() == null)
                    .collect(Collectors.toSet());
            Multimap<ICallStackElement, AggregatedCallSite> groupedCct = HashMultimap.create();
            groupCallGraphBy.entrySet().forEach(entry -> {
                if (!entry.getValue().isEmpty()) {
                    groupedCct.putAll(entry.getKey(), entry.getValue());
                }
            });
            fGroupedRootElements = groupedElements;
            fGroupedCct = groupedCct;
        }

        return groupedElements;
    }

    @Override
    public void setGroupBy(@Nullable ICallStackGroupDescriptor descriptor) {
        fGroupBy = descriptor;
        fGroupedRootElements = null;
        fGroupedCct = null;
    }

    @Override
    public Collection<AggregatedCallSite> getCallingContextTree(ICallStackElement element) {
        Multimap<ICallStackElement, AggregatedCallSite> groupedCct = fGroupedCct;
        if (groupedCct != null) {
            return groupedCct.get(element);
        }
        return fCcts.get(element);
    }

    @Override
    public AggregatedCallSite createCallSite(ICallStackSymbol symbol) {
        return new AggregatedStackTraces(symbol);
    }

    @Override
    public void addAggregatedCallSite(ICallStackElement dstGroup, AggregatedCallSite callsite) {
        Collection<AggregatedCallSite> callsites = fCcts.get(dstGroup);
        for (AggregatedCallSite site : callsites) {
            if (site.getSymbol().equals(callsite.getSymbol())) {
                site.merge(callsite);
                return;
            }
        }
        fCcts.put(dstGroup, callsite);
    }

    /**
     * /** Add a stack trace to this group, such that the symbol at position 0
     * is the top of the stack, ie the last symbol called.
     *
     * @param dstGroup
     *            The element to which to add this stack trace
     * @param stackTrace
     *            The stack trace to add to the group
     * @param ts
     *            The timestamp at which this stack trace is added
     */
    public void addStackTrace(ICallStackElement dstGroup, @NonNull Object [] stackTrace, long ts) {
        if (stackTrace.length == 0) {
            return;
        }
        // Create the callsite for this stack trace
        AggregatedCallSite prevCallsite = createCallSite(CallStackSymbolFactory.createSymbol(stackTrace[stackTrace.length - 1], dstGroup, ts));
        for (int i = stackTrace.length - 2; i >= 0; i--) {
            AggregatedCallSite callsite = createCallSite(CallStackSymbolFactory.createSymbol(stackTrace[i], dstGroup, ts));
            callsite.addCallee(prevCallsite);
            prevCallsite = callsite;
        }
        addAggregatedCallSite(dstGroup, prevCallsite);
    }

    /**
     * Add a stack trace to this group, such that the symbol at position 0 is
     * the top of the stack, ie the last symbol called.
     *
     * @param dstGroup
     *            The element to which to add this stack trace
     * @param stackTrace
     *            The stack trace to add to the group
     * @param ts
     *            The timestamp at which this stack trace is added
     */
    public void addStackTrace(ICallStackElement dstGroup, long[] stackTrace, long ts) {
        if (stackTrace.length == 0) {
            return;
        }
        // Create the callsite for this stack trace
        AggregatedCallSite prevCallsite = createCallSite(CallStackSymbolFactory.createSymbol(stackTrace[stackTrace.length - 1], dstGroup, ts));
        for (int i = stackTrace.length - 2; i >= 0; i--) {
            AggregatedCallSite callsite = createCallSite(CallStackSymbolFactory.createSymbol(stackTrace[i], dstGroup, ts));
            callsite.addCallee(prevCallsite);
            prevCallsite = callsite;
        }
        addAggregatedCallSite(dstGroup, prevCallsite);
    }

    /**
     * Method to implement to process a callstack event. If this event contains
     * a stack trace to add to some element, the implementation first needs to
     * find the element to which to add the stack trace. The root elements of
     * the hierarchy should be kept in this class. A root element can be added
     * by calling {@link #addRootElement(ICallStackElement)} and they can be
     * retrieved with {@link #getRootElements()}.
     *
     * Then from, the event, when the stack trace is retrieve, it can be
     * aggregated to the element by calling
     * {@link #addStackTrace(ICallStackElement, long[], long)} or
     * {@link #addStackTrace(ICallStackElement, Object[], long)}. These methods will
     * take care of creating the callsite objects and add the resulting callsite
     * to the element. Refer to the documentation of those method for the order
     * of the stack.
     *
     * @param event
     *            The trace event to process
     */
    protected abstract void processEvent(ITmfEvent event);

    @Override
    protected boolean executeAnalysis(@NonNull IProgressMonitor monitor) throws TmfAnalysisException {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            throw new NullPointerException("Trace has not been set, yet the analysis is being run!"); //$NON-NLS-1$
        }
        /* Cancel any previous request */
        ITmfEventRequest request = fRequest;
        if ((request != null) && (!request.isCompleted())) {
            request.cancel();
        }

        try {
            request = new ProfilingEventRequest(trace);
            fRequest = request;
            trace.sendRequest(request);

            request.waitForCompletion();
        } catch (InterruptedException e) {
            Activator.getInstance().logError("Request interrupted", e); //$NON-NLS-1$
        }
        return request.isCompleted();
    }

    @Override
    protected void canceling() {
        ITmfEventRequest req = fRequest;
        if ((req != null) && (!req.isCompleted())) {
            req.cancel();
        }
    }

    private class ProfilingEventRequest extends TmfEventRequest {

        private final ITmfTrace fTrace;

        /**
         * Constructor
         *
         * @param trace
         *            The trace
         */
        public ProfilingEventRequest(ITmfTrace trace) {
            super(TmfEvent.class,
                    TmfTimeRange.ETERNITY,
                    0,
                    ITmfEventRequest.ALL_DATA,
                    ITmfEventRequest.ExecutionType.BACKGROUND);
            fTrace = trace;
        }

        @Override
        public void handleData(final ITmfEvent event) {
            super.handleData(event);
            if (event.getTrace() == fTrace) {
                processEvent(event);
            } else if (fTrace instanceof TmfExperiment) {
                /*
                 * If the request is for an experiment, check if the event is
                 * from one of the child trace
                 */
                for (ITmfTrace childTrace : ((TmfExperiment) fTrace).getTraces()) {
                    if (childTrace == event.getTrace()) {
                        processEvent(event);
                    }
                }
            }
        }
    }

}
