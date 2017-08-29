/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.perf.profiling.core.callgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.AggregatedCallSite;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ISamplingDataProvider;
import org.eclipse.tracecompass.incubator.callstack.core.base.CallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.base.CallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.sampled.callgraph.ProfilingCallGraphAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * An analysis module for the sampled callchains from a perf trace. It
 * aggregates the data from the sampling events per pid/tid.
 *
 * @author Geneviève Bastien
 */
public class PerfCallchainAnalysisModule extends ProfilingCallGraphAnalysisModule implements ISamplingDataProvider {

    /**
     * ID of this analysis
     */
    public static final String ID = "org.eclipse.tracecompass.extension.perf.profiling.core.callchain"; //$NON-NLS-1$
    private static final String EVENT_SAMPLING = "cycles"; //$NON-NLS-1$
    private static final String FIELD_PERF_CALLCHAIN = "perf_callchain"; //$NON-NLS-1$
    private static final String FIELD_PERF_PID = "perf_pid"; //$NON-NLS-1$
    private static final String FIELD_PERF_TID = "perf_tid"; //$NON-NLS-1$

    private final CallStackGroupDescriptor fThreadDescriptor;
    private final CallStackGroupDescriptor fProcessDescriptor;
    // private final ProfilingGroup fGroupNode = new ProfilingGroup("Data",
    // CallGraphAllGroupDescriptor.getInstance());

    /**
     * Constructor
     */
    public PerfCallchainAnalysisModule() {
        // Create group descriptors
        fThreadDescriptor = new CallStackGroupDescriptor("Threads", null, false);
        fProcessDescriptor = new CallStackGroupDescriptor("Process", fThreadDescriptor, true);
    }

    @Override
    protected @Nullable Pair<ICallStackElement, AggregatedCallSite> getProfiledStackTrace(@NonNull ITmfEvent event) {
        if (!event.getName().startsWith(EVENT_SAMPLING)) {
            return null;
        }
        // Get the callchain if available
        ITmfEventField field = event.getContent().getField(FIELD_PERF_CALLCHAIN);
        if (field == null) {
            return null;
        }
        long[] value = (long[]) field.getValue();
        int size = value.length;
        long tmp;
        // Reverse the stack so that element at position 0 is the bottom
        for (int i = 0, mid = size >> 1, j = size - 1; i < mid; i++, j--) {
            tmp = value[i];
            value[i] = value[j];
            value[j] = tmp;
        }
        ICallStackElement element = getElement(event);
        return new Pair<>(element, getCallSite(element, value, event.getTimestamp().getValue()));
    }

    /**
     * @param event
     */
    private ICallStackElement getElement(ITmfEvent event) {
        // Find a root elements with the same PID
        Collection<ICallStackElement> rootElements = getRootElements();
        Long pidField = event.getContent().getFieldValue(Long.class, FIELD_PERF_PID);
        Long pid = pidField == null ? -1 : pidField;
        Long tidField = event.getContent().getFieldValue(Long.class, FIELD_PERF_TID);
        Long tid = tidField == null ? -1 : tidField;
        Optional<ICallStackElement> process = rootElements.stream()
                .filter(e -> e.getName().equals(String.valueOf(pid)))
                .findFirst();
        if (!process.isPresent()) {
            // Process is null, create both process and thread elements and return
            ICallStackElement processEl = new CallStackElement(String.valueOf(pid), fProcessDescriptor, fThreadDescriptor, null) {

                @Override
                protected int retrieveSymbolKeyAt(long time) {
                    return pid.intValue();
                }

            };
            ICallStackElement threadEl = new CallStackElement(String.valueOf(tid), fThreadDescriptor, null, processEl);
            processEl.setSymbolKeyElement(processEl);
            threadEl.setSymbolKeyElement(processEl);
            processEl.addChild(threadEl);
            addRootElement(processEl);
            return threadEl;
        }
        ICallStackElement processEl = process.get();

        // Process exists, find a thread element under it or create it
        Optional<ICallStackElement> thread = processEl.getChildren().stream()
                .filter(e -> e.getName().equals(String.valueOf(tid)))
                .findFirst();

        if (thread.isPresent()) {
            return thread.get();
        }
        ICallStackElement threadEl = new CallStackElement(String.valueOf(tid), fThreadDescriptor, null, processEl);
        processEl.addChild(threadEl);
        return threadEl;

    }

    @Override
    public Collection<ICallStackGroupDescriptor> getGroupDescriptors() {
        return ImmutableList.of(fProcessDescriptor);
    }

    @Override
    public Map<String, Collection<Object>> getCallStack(@NonNull ITmfEvent event) {
        ITmfEventField field = event.getContent().getField(FIELD_PERF_CALLCHAIN);
        if (field == null) {
            return Collections.emptyMap();
        }
        Object value = field.getValue();
        if (!(value instanceof long[])) {
            return Collections.emptyMap();
        }
        long[] callstack = (long[]) value;
        List<Object> longList = new ArrayList<>();
        for (long callsite : callstack) {
            longList.add(callsite);
        }
        Collections.reverse(longList);
        return ImmutableMap.of("Callchain", longList);

    }

    @Override
    public Collection<AggregatedCallSite> getSamplingData(int tid, long start, long end) {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return Collections.emptyList();
        }
        List<AggregatedCallSite> callsites = new ArrayList<>();
        TmfEventRequest request = new PerfProfilingEventRequest(trace, start, end, tid, callsites);
        trace.sendRequest(request);
        try {
            request.waitForCompletion();
        } catch (InterruptedException e) {

        }
        return callsites;
    }

    @Override
    public Collection<String> getHostIds() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return Collections.emptySet();
        }
        return Collections.singleton(trace.getHostId());
    }

    private class PerfProfilingEventRequest extends TmfEventRequest {

        private final int fTid;
        private final ITmfTrace fTrace;
        private final List<AggregatedCallSite> fSites;

        /**
         * Constructor
         *
         * @param trace
         *            The trace
         * @param start
         *            The start time of the request
         * @param end
         *            The end time of the request
         * @param tid
         *            The tid for which to get the samples
         * @param callsites
         *            A list of callsites to fill
         */
        public PerfProfilingEventRequest(ITmfTrace trace, long start, long end, int tid, List<AggregatedCallSite> callsites) {
            super(TmfEvent.class,
                    new TmfTimeRange(TmfTimestamp.fromNanos(start), TmfTimestamp.fromNanos(end)),
                    0,
                    ITmfEventRequest.ALL_DATA,
                    ITmfEventRequest.ExecutionType.BACKGROUND);
            fTid = tid;
            fTrace = trace;
            fSites = callsites;
        }

        @Override
        public void handleData(final ITmfEvent event) {
            super.handleData(event);
            if (event.getTrace() == fTrace) {
                handleEvent(event);
            } else if (fTrace instanceof TmfExperiment) {
                /*
                 * If the request is for an experiment, check if the event is
                 * from one of the child trace
                 */
                for (ITmfTrace childTrace : ((TmfExperiment) fTrace).getTraces()) {
                    if (childTrace == event.getTrace()) {
                        handleEvent(event);
                    }
                }
            }
        }

        private void handleEvent(ITmfEvent event) {
            Long tidField = event.getContent().getFieldValue(Long.class, FIELD_PERF_TID);
            Long tid = tidField == null ? -1 : tidField;
            if (tid.intValue() != fTid) {
                return;
            }
            Pair<ICallStackElement, AggregatedCallSite> stackTrace = getProfiledStackTrace(event);
            if (stackTrace == null) {
                return;
            }
            AggregatedCallSite perfCallSite = stackTrace.getSecond();
            for (AggregatedCallSite site : fSites) {
                if (site.getSymbol().equals(perfCallSite.getSymbol())) {
                    site.merge(perfCallSite);
                    return;
                }
            }
            fSites.add(perfCallSite);
        }
    }

}
