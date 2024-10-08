/*******************************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.poll.distribution.analysis;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.dpdk.core.trace.DpdkTrace;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.aspect.LamiGenericAspect;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.aspect.LamiTableEntryAspect;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiAnalysis;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiResultTable;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiTableClass;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiTableEntry;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiData;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiLongNumber;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiTimeRange;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiTimestamp;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfContentFieldAspect;
import org.eclipse.tracecompass.tmf.core.filter.model.TmfFilterMatchesNode;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest.ExecutionType;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Dpdk polls distribution analysis is an on-demand analysis that calculates the
 * number of packets retrieved in a single call to rte_eth_rx_burst(). The poll
 * distribution is calculated per port queue only.
 *
 * @author Adel Belkhiri
 *
 */
public class PollDistributionAnalysis extends LamiAnalysis {

    private static final long MASK = (1 << 10) - 1L;
    private static final int MEMORY_SANITY_LIMIT = 40000;

    /**
     * Constructor
     */
    @SuppressWarnings("restriction")
    public PollDistributionAnalysis() {
        super("DPDK Polls Distribution (ethdev)", false, trace -> true, Collections.emptyList()); //$NON-NLS-1$
    }

    @Override
    protected synchronized void initialize() {
        // do nothing
    }

    @Override
    public boolean canExecute(ITmfTrace trace) {
        if (trace instanceof DpdkTrace) {
            return ((DpdkTrace) trace).validate(null, trace.getPath()).isOK()? true : false;
        }
        return false;
    }

    private static int workRemaining(ITmfTrace trace) {
        return (int) Math.min(trace.getNbEvents() / (MASK + 1), Integer.MAX_VALUE);
    }

    @SuppressWarnings("restriction")
    @Override
    public List<LamiResultTable> execute(ITmfTrace trace, @Nullable TmfTimeRange timeRange, String extraParamsString, IProgressMonitor monitor) throws CoreException {
        List<LamiResultTable> results = new ArrayList<>();
        TmfTimeRange tr = timeRange == null ? TmfTimeRange.ETERNITY : timeRange;

        /* handle the filter in case the user set one */
        TmfFilterMatchesNode filter = new TmfFilterMatchesNode(null);
        filter.setEventAspect(new TmfContentFieldAspect(Messages.getMessage(Messages.AspectName_FieldPortName), DpdkEthdevEventLayout.fieldPortId()));
        filter.setRegex(extraParamsString);

        Predicate<ITmfEvent> filterPred = (event -> extraParamsString.isEmpty() || filter.matches(event));
        SubMonitor mon = SubMonitor.convert(monitor, "Ethdev Polls Distribution", workRemaining(trace)); //$NON-NLS-1$
        AtomicLong done = new AtomicLong();

        // the poll aspect/count map
        Map<String, Map<Integer, Long>> pollAspectCounts = new TreeMap<>();

        /* create the event request */
        TmfEventRequest req = new TmfEventRequest(ITmfEvent.class, tr, 0, Integer.MAX_VALUE, ExecutionType.BACKGROUND) {
            @Override
            public void handleData(ITmfEvent event) {
                if (monitor.isCanceled()) {
                    cancel();
                }
                if (event.getName().equals(DpdkEthdevEventLayout.eventEthdevRxqBurstNonEmpty()) && filterPred.test(event)) {
                    TmfEtherPollAspect aspect = TmfEtherPollAspect.getInstance(event);
                    Object resolved = aspect.resolve(event);
                    if (resolved != null) {
                        Map<Integer, Long> dataSet = pollAspectCounts.computeIfAbsent(aspect.getName(), unused -> new HashMap<>());
                        if (dataSet.size() < MEMORY_SANITY_LIMIT) {
                            int pollValue = (Integer) resolved;
                            dataSet.put(pollValue, dataSet.getOrDefault(pollValue, 0L) + 1);
                        }
                    }
                }
                if ((done.incrementAndGet() & MASK) == 0) {
                    mon.setWorkRemaining(workRemaining(trace));
                    mon.worked(1);

                    monitor.setTaskName("DPDK Polls Distribution Analysis (" + NumberFormat.getInstance().format(done.get()) + " events read)"); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }

        };
        // send the request to the trace
        trace.sendRequest(req);

        // convert the result to Lami types
        try {
            req.waitForCompletion();
            for (Entry<String, Map<Integer, Long>> entry : pollAspectCounts.entrySet()) {
                List<LamiTableEntry> entries = new ArrayList<>();
                Map<Integer, Long> dataSet = entry.getValue();
                for (Entry<Integer, Long> element : dataSet.entrySet()) {
                    List<LamiData> data = Arrays.asList(
                            new LamiString(element.getKey().toString()),
                            new LamiLongNumber(element.getValue()));

                    entries.add(new LamiTableEntry(data));
                }

                List<LamiTableEntryAspect> tableAspects = Arrays.asList(new LamiCategoryAspect("Number of retrieved packets", 0), //$NON-NLS-1$
                        new LamiCountAspect("Count", 1));
                LamiTableClass tableClass = new LamiTableClass(entry.getKey(), entry.getKey(), tableAspects, Collections.emptySet());
                LamiResultTable lrt = new LamiResultTable(createTimeRange(tr), tableClass, entries);
                results.add(lrt);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return results;
    }

    /**
     * Todo, move to LAMI
     */
    private static LamiTimeRange createTimeRange(TmfTimeRange timeRange) {
        return new LamiTimeRange(new LamiTimestamp(timeRange.getStartTime().toNanos()), new LamiTimestamp(timeRange.getEndTime().toNanos()));
    }

    /**
     * Todo, LamiString in LAMI is private
     */
    private final class LamiString extends LamiData {
        private final String fElement;

        private LamiString(String element) {
            fElement = element;
        }

        @Override
        public @NonNull String toString() {
            return fElement;
        }
    }

    /**
     * Count aspect, generic
     *
     */
    private final class LamiCountAspect extends LamiGenericAspect {
        private LamiCountAspect(String name, int column) {
            super(name, null, column, true, false);
        }
    }

    /**
     * Category aspect, generic
     *
     */
    private final class LamiCategoryAspect extends LamiGenericAspect {
        private LamiCategoryAspect(String name, int column) {
            super(name, null, column, false, false);
        }
    }
}
