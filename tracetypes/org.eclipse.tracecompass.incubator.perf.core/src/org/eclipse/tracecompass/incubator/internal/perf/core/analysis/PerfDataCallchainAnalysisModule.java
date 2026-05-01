/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.perf.core.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.base.ICallStackElement;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.AggregatedCallSite;
import org.eclipse.tracecompass.analysis.profiling.core.callstack2.CallStackElement;
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.CallStackGroupDescriptor;
import org.eclipse.tracecompass.analysis.profiling.core.model.ISamplingDataProvider;
import org.eclipse.tracecompass.analysis.profiling.core.sampled.callgraph.ProfilingCallGraphAnalysisModule;
import org.eclipse.tracecompass.analysis.profiling.core.tree.IWeightedTreeGroupDescriptor;
import org.eclipse.tracecompass.incubator.internal.perf.core.PerfConstants;
import org.eclipse.tracecompass.incubator.internal.perf.core.PerfRecord;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
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
 * Sampled-callchain analysis for {@code perf.data} traces.
 *
 * Iterates {@code PERF_RECORD_SAMPLE} events, reads the {@code callchain}
 * {@code long[]} produced by the low-level reader, and aggregates the chains
 * per process/thread for the Flame Graph and Flame Chart views.
 *
 * This module is the perf.data-native counterpart of the CTF-based
 * {@code PerfCallchainAnalysisModule} in the
 * {@code org.eclipse.tracecompass.incubator.perf.profiling.core} bundle. The
 * only differences are:
 * <ul>
 * <li>We filter samples by record type ({@link PerfConstants#PERF_RECORD_SAMPLE}
 * via the matching {@code PerfEventType} name, {@code "SAMPLE"}).</li>
 * <li>The callchain is read directly off the {@link PerfRecord} embedded in
 * the event's root field, so we do not have to go through string conversion.</li>
 * </ul>
 *
 * @author Matthew Khouzam
 */
public class PerfDataCallchainAnalysisModule extends ProfilingCallGraphAnalysisModule
        implements ISamplingDataProvider {

    /** Analysis ID registered via plugin.xml. */
    public static final String ID = "org.eclipse.tracecompass.incubator.perf.core.callchain"; //$NON-NLS-1$

    private static final String EVENT_SAMPLE_NAME = "SAMPLE"; //$NON-NLS-1$
    private static final String FIELD_CALLCHAIN = "callchain"; //$NON-NLS-1$
    private static final String FIELD_PID = "pid"; //$NON-NLS-1$
    private static final String FIELD_TID = "tid"; //$NON-NLS-1$

    private final CallStackGroupDescriptor fThreadDescriptor;
    private final CallStackGroupDescriptor fProcessDescriptor;

    /**
     * Constructor
     */
    public PerfDataCallchainAnalysisModule() {
        fThreadDescriptor = new CallStackGroupDescriptor("Threads", null, false); //$NON-NLS-1$
        fProcessDescriptor = new CallStackGroupDescriptor("Process", fThreadDescriptor, true); //$NON-NLS-1$
    }

    // ---------------------------------------------------------------------
    // Core stack extraction
    // ---------------------------------------------------------------------

    @Override
    protected Pair<ICallStackElement, AggregatedCallSite> getProfiledStackTrace(@NonNull ITmfEvent event) {
        if (!EVENT_SAMPLE_NAME.equals(event.getType().getName())) {
            return null;
        }
        long[] chain = extractCallchain(event);
        if (chain == null || chain.length == 0) {
            return null;
        }
        long[] reversed = reverseAndFilter(chain);
        if (reversed.length == 0) {
            return null;
        }
        ICallStackElement element = getElement(event);
        return new Pair<>(element, getCallSite(element, reversed, event.getTimestamp().getValue()));
    }

    /**
     * perf fills the callchain array with synthetic sentinel IPs
     * ({@code PERF_CONTEXT_*}) such as {@code 0xffffffffffffff80} to delimit
     * privilege-level segments. Filter them out and reverse so that the
     * oldest frame is at index 0 (matches what the framework expects).
     */
    private static long[] reverseAndFilter(long[] chain) {
        // The PERF_CONTEXT_* sentinels are all in [0xffffffffffffff80,
        // 0xffffffffffffffff], i.e. the top of the address space. Drop them.
        final long sentinelLo = 0xffffffffffffff80L;
        int kept = 0;
        for (long v : chain) {
            if (Long.compareUnsigned(v, sentinelLo) < 0) {
                kept++;
            }
        }
        long[] out = new long[kept];
        int j = kept - 1;
        for (long v : chain) {
            if (Long.compareUnsigned(v, sentinelLo) < 0) {
                out[j--] = v;
            }
        }
        return out;
    }

    private static long @Nullable [] extractCallchain(ITmfEvent event) {
        Object value = event.getContent().getValue();
        if (value instanceof PerfRecord) {
            Object cc = ((PerfRecord) value).getField(FIELD_CALLCHAIN);
            if (cc instanceof long[]) {
                return (long[]) cc;
            }
        }
        // Fallback through the field map
        Object fallback = event.getContent().getFieldValue(Object.class, FIELD_CALLCHAIN);
        if (fallback instanceof long[]) {
            return (long[]) fallback;
        }
        return null;
    }

    private static int extractInt(ITmfEvent event, String field, int defaultValue) {
        Object value = event.getContent().getValue();
        if (value instanceof PerfRecord) {
            Object v = ((PerfRecord) value).getField(field);
            if (v instanceof Integer) {
                return (Integer) v;
            }
            if (v instanceof Long) {
                return ((Long) v).intValue();
            }
        }
        Object v = event.getContent().getFieldValue(Object.class, field);
        if (v instanceof Integer) {
            return (Integer) v;
        }
        if (v instanceof Long) {
            return ((Long) v).intValue();
        }
        return defaultValue;
    }

    private ICallStackElement getElement(ITmfEvent event) {
        Collection<ICallStackElement> rootElements = getRootElements();
        final int pid = extractInt(event, FIELD_PID, -1);
        final int tid = extractInt(event, FIELD_TID, -1);

        Optional<ICallStackElement> process = rootElements.stream()
                .filter(e -> e.getName().equals(String.valueOf(pid)))
                .findFirst();
        if (!process.isPresent()) {
            ICallStackElement processEl = new CallStackElement(String.valueOf(pid),
                    fProcessDescriptor, fThreadDescriptor, null) {
                @Override
                protected int retrieveSymbolKeyAt(long time) {
                    return pid;
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
        Optional<ICallStackElement> thread = processEl.getChildrenElements().stream()
                .filter(e -> e.getName().equals(String.valueOf(tid)))
                .findFirst();
        if (thread.isPresent()) {
            return thread.get();
        }
        ICallStackElement threadEl = new CallStackElement(String.valueOf(tid), fThreadDescriptor, null, processEl);
        processEl.addChild(threadEl);
        return threadEl;
    }

    // ---------------------------------------------------------------------
    // ISamplingDataProvider
    // ---------------------------------------------------------------------

    @Override
    public Collection<IWeightedTreeGroupDescriptor> getGroupDescriptors() {
        return ImmutableList.of(fProcessDescriptor);
    }

    @Override
    public Map<String, Collection<Object>> getCallStack(@NonNull ITmfEvent event) {
        long[] chain = extractCallchain(event);
        if (chain == null) {
            return Collections.emptyMap();
        }
        long[] reversed = reverseAndFilter(chain);
        List<Object> list = new ArrayList<>(reversed.length);
        for (long ip : reversed) {
            list.add(ip);
        }
        return ImmutableMap.of("Callchain", list); //$NON-NLS-1$
    }

    @Override
    public Collection<AggregatedCallSite> getSamplingData(int tid, long start, long end) {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return Collections.emptyList();
        }
        List<AggregatedCallSite> sites = new ArrayList<>();
        TmfEventRequest request = new PerfSamplingRequest(trace, start, end, tid, sites);
        trace.sendRequest(request);
        try {
            request.waitForCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return sites;
    }

    @Override
    public Collection<String> getHostIds() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return Collections.emptySet();
        }
        return Collections.singleton(trace.getHostId());
    }

    /** Event request that aggregates one thread's samples into a list of callsites. */
    private final class PerfSamplingRequest extends TmfEventRequest {

        private final int fTid;
        private final ITmfTrace fTrace;
        private final List<AggregatedCallSite> fSites;

        PerfSamplingRequest(ITmfTrace trace, long start, long end, int tid, List<AggregatedCallSite> sites) {
            super(org.eclipse.tracecompass.tmf.core.event.TmfEvent.class,
                    new TmfTimeRange(TmfTimestamp.fromNanos(start), TmfTimestamp.fromNanos(end)),
                    0,
                    ITmfEventRequest.ALL_DATA,
                    ITmfEventRequest.ExecutionType.BACKGROUND);
            fTid = tid;
            fTrace = trace;
            fSites = sites;
        }

        @Override
        public void handleData(final ITmfEvent event) {
            super.handleData(event);
            if (event.getTrace() == fTrace) {
                handleEvent(event);
            } else if (fTrace instanceof TmfExperiment) {
                for (ITmfTrace child : ((TmfExperiment) fTrace).getTraces()) {
                    if (child == event.getTrace()) {
                        handleEvent(event);
                        break;
                    }
                }
            }
        }

        private void handleEvent(ITmfEvent event) {
            if (extractInt(event, FIELD_TID, -2) != fTid) {
                return;
            }
            Pair<ICallStackElement, AggregatedCallSite> stackTrace = getProfiledStackTrace(event);
            if (stackTrace == null) {
                return;
            }
            AggregatedCallSite sample = stackTrace.getSecond();
            for (AggregatedCallSite existing : fSites) {
                if (existing.getObject().equals(sample.getObject())) {
                    existing.merge(sample);
                    return;
                }
            }
            fSites.add(sample);
        }
    }
}
