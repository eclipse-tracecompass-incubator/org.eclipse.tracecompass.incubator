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

package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.poll.stats.analysis;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

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
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiDoubleNumber;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiLongNumber;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiTimeRange;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiTimestamp;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest.ExecutionType;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * The DPDK Polls Statistics Analysis is an on-demand analysis that computes
 * statistics related to the polling of receive queues of Ethernet ports by PMD
 * (Poll-Mode Driver) threads, through calls to `rte_eth_rx_burst()`. The
 * statistics include, per queue and per thread, the minimum, maximum, average,
 * and standard deviation of the number of packets retrieved in a single call to
 * the `rte_eth_rx_burst()` API function.
 *
 * @author Adel Belkhiri
 */
public class DpdkPollStatsAnalysis extends LamiAnalysis {

    private static final long PROGRESS_INTERVAL = (1 << 10) - 1L;
    private static final int MEMORY_SANITY_LIMIT = 40000;
    /* Events layout */
    private final DpdkEthdevEventLayout fLayout = new DpdkEthdevEventLayout();

    /**
     * Constructor
     */
    public DpdkPollStatsAnalysis() {
        super(Objects.requireNonNull(Messages.EthdevPollStats_AnalysisName), false, trace -> true, Collections.emptyList());
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
        Map<String, Map<String, List<Integer>>> pollCountMap = new HashMap<>();
        TmfTimeRange adjustedTimeRange = timeRange == null ? TmfTimeRange.ETERNITY : timeRange;
        SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.EthdevPollStats_AnalysisName, workRemaining(trace));

        // Create and send the event request
        TmfEventRequest eventRequest = createEventRequest(trace, adjustedTimeRange,
                pollCountMap, subMonitor, done);
        trace.sendRequest(eventRequest);

        // Convert the results to LAMI tables
        try {
            eventRequest.waitForCompletion();
            return convertToLamiTables(adjustedTimeRange, pollCountMap);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }
    }

    private TmfEventRequest createEventRequest(ITmfTrace trace, TmfTimeRange timeRange, Map<String, Map<String, List<Integer>>> pollAspectCounts, SubMonitor monitor, AtomicLong nbProcessevents) {
        return new TmfEventRequest(ITmfEvent.class, timeRange, 0, Integer.MAX_VALUE, ExecutionType.BACKGROUND) {
            @Override
            public void handleData(ITmfEvent event) {
                if (monitor.isCanceled()) {
                    cancel();
                    return;
                }

                // Process events to compute RX polls statistics
                processEvent(event, pollAspectCounts);

                if ((nbProcessevents.incrementAndGet() & PROGRESS_INTERVAL) == 0) {
                    monitor.setWorkRemaining(workRemaining(trace));
                    monitor.worked(1);
                    monitor.setTaskName(String.format("Dpdk Polls Statistics Analysis (%s events processed)", //$NON-NLS-1$
                            NumberFormat.getInstance().format(nbProcessevents.get())));
                }
            }
        };
    }

    private void processEvent(ITmfEvent event, Map<String, Map<String, List<Integer>>> pollCountsMap) {
        if (!event.getName().equals(fLayout.eventEthdevRxBurstNonEmpty())) {
            return;
        }

        Integer nbRxPkts = event.getContent().getFieldValue(Integer.class, fLayout.fieldNbRxPkts());
        Integer portId = event.getContent().getFieldValue(Integer.class, fLayout.fieldPortId());
        Integer queueId = event.getContent().getFieldValue(Integer.class, fLayout.fieldQueueId());
        String threadName = event.getContent().getFieldValue(String.class, fLayout.fieldThreadName());

        if (nbRxPkts == null || portId == null || queueId == null || threadName == null) {
            return;
        }

        // Update the poll count for queues
        String queueName = "P" + portId + "/Q" + queueId; //$NON-NLS-1$ //$NON-NLS-2$
        updatePollCountsMap(pollCountsMap, Objects.requireNonNull(Messages.EthdevPollStats_QueueLabel), queueName, nbRxPkts);

        // Update the poll count for threads
        updatePollCountsMap(pollCountsMap, Objects.requireNonNull(Messages.EthdevPollStats_ThreadLabel), threadName, nbRxPkts);
    }

    private static void updatePollCountsMap(Map<String, Map<String, List<Integer>>> pollCountsMap, String aspectName, String key, Integer nbRxPkts) {
        Map<String, List<Integer>> dataSet = pollCountsMap.computeIfAbsent(aspectName, unused -> new HashMap<>());
        if (dataSet.size() < MEMORY_SANITY_LIMIT) {
            List<Integer> data = dataSet.computeIfAbsent(key, unused -> new ArrayList<>());
            data.add(nbRxPkts);
        }
    }

    private List<LamiResultTable> convertToLamiTables(TmfTimeRange timeRange,
            Map<String, Map<String, List<Integer>>> pollAspectCounts) {
        List<LamiResultTable> results = new ArrayList<>();
        for (Entry<String, Map<String, List<Integer>>> entry : pollAspectCounts.entrySet()) {

            Map<String, List<Integer>> dataSet = Objects.requireNonNull(entry.getValue());
            List<LamiTableEntry> entries = new ArrayList<>();

            for (Entry<String, List<Integer>> element : dataSet.entrySet()) {

                List<Integer> pollValues = Objects.requireNonNull(element.getValue());
                /*
                 * Calculate the number of successful polls, along with the
                 * minimum and maximum polls values
                 */
                int nbSuccessfulPolls = pollValues.size();
                int minPollValue = Collections.min(element.getValue());
                int maxPollValue = Collections.max(element.getValue());

                /*
                 * Calculate the mean and the standard deviation
                 */
                double avgPollValue = pollValues.stream().mapToInt(i -> i).average().orElse(0);
                double sd = pollValues.stream().mapToDouble(val -> Math.pow(val - avgPollValue, 2)).sum();
                double std = Math.sqrt(sd / pollValues.size());
                double stdRounded = Math.round(std * 100.0) / 100.0;

                List<LamiData> data = Arrays.asList(
                        new LamiString(element.getKey()),
                        new LamiLongNumber((long) minPollValue),
                        new LamiLongNumber((long) maxPollValue),
                        new LamiLongNumber((long) avgPollValue),
                        new LamiDoubleNumber(stdRounded),
                        new LamiLongNumber((long) nbSuccessfulPolls));

                entries.add(new LamiTableEntry(data));
            }

            List<LamiTableEntryAspect> tableAspects = Arrays.asList(new LamiCategoryAspect(entry.getKey(), 0),
                    new LamiCountAspect(Objects.requireNonNull(Messages.EthdevPollStats_MinimumValueLabel), 1),
                    new LamiCountAspect(Objects.requireNonNull(Messages.EthdevPollStats_MaximumValueLabel), 2),
                    new LamiCountAspect(Objects.requireNonNull(Messages.EthdevPollStats_AverageValueLabel), 3),
                    new LamiCountAspect(Objects.requireNonNull(Messages.EthdevPollStats_StandardDeviationLabel), 4),
                    new LamiCountAspect(Objects.requireNonNull(Messages.EthdevPollStats_CountLabel), 5));
            LamiTableClass tableClass = new LamiTableClass(entry.getKey(), entry.getKey(), tableAspects, Collections.emptySet());
            LamiResultTable lrt = new LamiResultTable(createTimeRange(timeRange), tableClass, entries);
            results.add(lrt);
        }
        return results;
    }

    /**
     * TODO: move to LAMI
     */
    private static LamiTimeRange createTimeRange(TmfTimeRange timeRange) {
        return new LamiTimeRange(new LamiTimestamp(timeRange.getStartTime().toNanos()), new LamiTimestamp(timeRange.getEndTime().toNanos()));
    }

    /**
     * TODO: move to LAMI
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

    /**
     * Count aspect, generic
     *
     * TODO: move to LAMI
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
     * TODO: move to LAMI
     *
     */
    private final class LamiCategoryAspect extends LamiGenericAspect {

        private LamiCategoryAspect(String name, int column) {
            super(name, null, column, false, false);
        }
    }
}
