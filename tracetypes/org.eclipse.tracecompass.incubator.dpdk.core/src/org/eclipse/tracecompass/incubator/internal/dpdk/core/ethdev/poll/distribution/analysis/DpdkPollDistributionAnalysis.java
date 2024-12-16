/*******************************************************************************
 * Copyright (c) 2024 École Polytechnique de Montréal
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
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.dpdk.core.trace.DpdkTrace;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.analysis.DpdkEthdevEventLayout;
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
public class DpdkPollDistributionAnalysis extends LamiAnalysis {

    private static final long PROGRESS_INTERVAL = (1 << 10) - 1L;
    private static final int MEMORY_SANITY_LIMIT = 40000;
    /* Events layout */
    private final DpdkEthdevEventLayout fLayout = new DpdkEthdevEventLayout();

    /**
     * Constructor
     */
    public DpdkPollDistributionAnalysis() {
        super(Objects.requireNonNull(Messages.EthdevPollDistribution_AnalysisName), false, trace -> true, Collections.emptyList());
    }

    @Override
    protected synchronized void initialize() {
        // do nothing
    }

    @Override
    public boolean canExecute(ITmfTrace trace) {
        if (trace instanceof DpdkTrace) {
            return ((DpdkTrace) trace).validate(null, trace.getPath()).isOK();
        }
        return false;
    }

    private static int workRemaining(ITmfTrace trace) {
        return (int) Math.min(trace.getNbEvents() / (PROGRESS_INTERVAL + 1), Integer.MAX_VALUE);
    }

    @Override
    public List<LamiResultTable> execute(ITmfTrace trace, @Nullable TmfTimeRange timeRange, String extraParamsString, IProgressMonitor monitor) throws CoreException {
        AtomicLong done = new AtomicLong();
        Map<String, Map<Integer, Long>> pollCountPerQueue = new TreeMap<>();
        TmfTimeRange adjustedTimeRange = timeRange == null ? TmfTimeRange.ETERNITY : timeRange;
        SubMonitor subMonitor = SubMonitor.convert(monitor, Objects.requireNonNull(Messages.EthdevPollDistribution_AnalysisName), workRemaining(trace));

        /*
         * Handle the filter in case the user indicates a specific port to
         * process its events
         */
        TmfFilterMatchesNode filter = new TmfFilterMatchesNode(null);
        filter.setEventAspect(new TmfContentFieldAspect(Objects.requireNonNull(Messages.EthdevPollDistribution_CountLabel), fLayout.fieldPortId()));
        filter.setRegex(extraParamsString);
        Predicate<ITmfEvent> filterPred = (event -> extraParamsString.isEmpty() || filter.matches(event));

        // Create and send the event request
        TmfEventRequest eventRequest = createEventRequest(trace, adjustedTimeRange, filterPred,
                pollCountPerQueue, subMonitor, done);
        trace.sendRequest(eventRequest);

        try {
            eventRequest.waitForCompletion();
            return convertToLamiTables(adjustedTimeRange, pollCountPerQueue);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }
    }

    private TmfEventRequest createEventRequest(ITmfTrace trace, TmfTimeRange timeRange, Predicate<ITmfEvent> filterPredicate, Map<String, Map<Integer, Long>> pollAspectCounts, SubMonitor monitor, AtomicLong nbProcessevents) {
        return new TmfEventRequest(ITmfEvent.class, timeRange, 0, Integer.MAX_VALUE, ExecutionType.BACKGROUND) {
            @Override
            public void handleData(ITmfEvent event) {
                if (monitor.isCanceled()) {
                    cancel();
                    return;
                }

                // Process events to compute RX polls distribution
                processEvent(event, filterPredicate, pollAspectCounts);

                if ((nbProcessevents.incrementAndGet() & PROGRESS_INTERVAL) == 0) {
                    monitor.setWorkRemaining(workRemaining(trace));
                    monitor.worked(1);
                    monitor.setTaskName(String.format("DPDK Polls Distribution Analysis (%s events processed)", //$NON-NLS-1$
                            NumberFormat.getInstance().format(nbProcessevents.get())));
                }
            }
        };
    }

    private void processEvent(ITmfEvent event, Predicate<ITmfEvent> filterPredicate,
            Map<String, Map<Integer, Long>> pollAspectCounts) {

        if (event.getName().equals(fLayout.eventEthdevRxBurstNonEmpty())
                && filterPredicate.test(event)) {
            Integer nbRxPkts = event.getContent().getFieldValue(Integer.class, fLayout.fieldNbRxPkts());
            Integer portId = event.getContent().getFieldValue(Integer.class, fLayout.fieldPortId());
            Integer queueId = event.getContent().getFieldValue(Integer.class, fLayout.fieldQueueId());

            if (nbRxPkts != null && portId != null && queueId != null) {
                String queueName = "P" + portId + "/Q" + queueId; //$NON-NLS-1$ //$NON-NLS-2$
                Map<Integer, Long> dataSet = pollAspectCounts.computeIfAbsent(queueName, k -> new HashMap<>());
                if (dataSet.size() < MEMORY_SANITY_LIMIT) {
                    dataSet.merge(nbRxPkts, 1L, (v1, v2) -> v1 + v2);
                }
            }
        }
    }

    private List<LamiResultTable> convertToLamiTables(TmfTimeRange timeRange,
            Map<String, Map<Integer, Long>> pollCountPerQueue) {
        List<LamiResultTable> results = new ArrayList<>();
        for (Map.Entry<String, Map<Integer, Long>> entry : pollCountPerQueue.entrySet()) {
            String queueName = Objects.requireNonNull(entry.getKey());
            Map<Integer, Long> dataSet = Objects.requireNonNull(entry.getValue());

            List<LamiTableEntry> tableEntries = dataSet.entrySet().stream()
                    .map(e -> new LamiTableEntry(Arrays.asList(
                            new LamiString(Objects.requireNonNull(e.getKey()).toString()),
                            new LamiLongNumber(Objects.requireNonNull(e.getValue())))))
                    .collect(Collectors.toList());

            List<LamiTableEntryAspect> tableAspects = Arrays.asList(
                    new LamiCategoryAspect(Objects.requireNonNull(Messages.EthdevPollDistribution_NumberOfPacketsLabel), 0),
                    new LamiCountAspect(Objects.requireNonNull(Messages.EthdevPollDistribution_CountLabel), 1));

            LamiTableClass tableClass = new LamiTableClass(queueName, queueName, tableAspects, Collections.emptySet());
            results.add(new LamiResultTable(createTimeRange(timeRange), tableClass, tableEntries));
        }
        return results;
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

    /**
     * TODO: move to LAMI
     */
    private static LamiTimeRange createTimeRange(TmfTimeRange timeRange) {
        return new LamiTimeRange(new LamiTimestamp(timeRange.getStartTime().toNanos()), new LamiTimestamp(timeRange.getEndTime().toNanos()));
    }

    /**
     * TODO: LamiString in LAMI is private
     */
    private final class LamiString extends LamiData {
        private final String fElement;

        private LamiString(String element) {
            fElement = element;
        }

        @Override
        public @Nullable String toString() {
            return fElement;
        }
    }
}
