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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.dpdk.core.trace.DpdkTrace;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis.DpdkEthdevEventLayout;
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

    /**
     * Constructor
     */
    public DpdkPollStatsAnalysis() {
        super(Messages.getMessage(Messages.EthdevPollStats_AnalysisName), false, trace -> true, Collections.emptyList());
    }

    @Override
    protected synchronized void initialize() {
        // do nothing
    }

    @Override
    public boolean canExecute(ITmfTrace trace) {
        if (trace instanceof DpdkTrace) {
            return ((DpdkTrace) trace).validate(null, trace.getPath()).isOK() ? true : false;
        }
        return false;
    }

    private static int workRemaining(ITmfTrace trace) {
        return (int) Math.min(trace.getNbEvents() / (PROGRESS_INTERVAL + 1), Integer.MAX_VALUE);
    }

    @Override
    public List<LamiResultTable> execute(ITmfTrace trace, @Nullable TmfTimeRange timeRange, String extraParamsString, IProgressMonitor monitor) throws CoreException {
        AtomicLong done = new AtomicLong();
        Map<@NonNull String, Map<@NonNull String, List<Integer>>> pollCountMap = new HashMap<>();
        TmfTimeRange adjustedTimeRange = timeRange == null ? TmfTimeRange.ETERNITY : timeRange;
        SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.EthdevPollStats_AnalysisName, workRemaining(trace));

        // create and send the event request
        TmfEventRequest eventRequest = createEventRequest(trace, adjustedTimeRange,
                pollCountMap, subMonitor, done);
        trace.sendRequest(eventRequest);

        // convert the results to LAMI tables
        try {
            eventRequest.waitForCompletion();
            return convertToLamiTables(adjustedTimeRange, pollCountMap);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }
    }

    private static TmfEventRequest createEventRequest(ITmfTrace trace, TmfTimeRange timeRange, Map<@NonNull String, Map<@NonNull String, List<Integer>>> pollAspectCounts, SubMonitor monitor, AtomicLong nbProcessevents) {
        return new TmfEventRequest(ITmfEvent.class, timeRange, 0, Integer.MAX_VALUE, ExecutionType.BACKGROUND) {
            @Override
            public void handleData(ITmfEvent event) {
                if (monitor.isCanceled()) {
                    cancel();
                    return;
                }

                // process events to compute RX polls statistics
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

    private static void processEvent(ITmfEvent event, Map<@NonNull String, Map<@NonNull String, List<Integer>>> pollCountsMap) {
        if (!event.getName().equals(DpdkEthdevEventLayout.eventEthdevRxBurstNonEmpty())) {
            return;
        }

        Integer nbRxPkts = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldNbRxPkts());
        Integer portId = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldPortId());
        Integer queueId = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldQueueId());
        String threadName = event.getContent().getFieldValue(String.class, DpdkEthdevEventLayout.fieldThreadName());

        if (nbRxPkts == null || portId == null || queueId == null || threadName == null) {
            return;
        }

        // update the poll count from a queue perspective
        String queueName = "P" + portId + "/Q" + queueId; //$NON-NLS-1$ //$NON-NLS-2$
        updatePollCountsMap(pollCountsMap, Messages.getMessage(Messages.EthdevPollStats_QueueLabel), queueName, nbRxPkts);

        // update the poll count from a thread perspective
        updatePollCountsMap(pollCountsMap, Messages.getMessage(Messages.EthdevPollStats_ThreadLabel), threadName, nbRxPkts);
    }

    private static void updatePollCountsMap(Map<@NonNull String, Map<@NonNull String, List<Integer>>> pollCountsMap, @NonNull String aspectName, @NonNull String key, Integer nbRxPkts) {
        Map<@NonNull String, List<Integer>> dataSet = pollCountsMap.computeIfAbsent(aspectName, unused -> new HashMap<>());
        if (dataSet.size() < MEMORY_SANITY_LIMIT) {
            List<Integer> data = dataSet.computeIfAbsent(key, unused -> new ArrayList<>());
            data.add(nbRxPkts);
        }
    }

    private @NonNull List<LamiResultTable> convertToLamiTables(TmfTimeRange timeRange,
            Map<@NonNull String, Map<@NonNull String, List<Integer>>> pollAspectCounts) {
        List<LamiResultTable> results = new ArrayList<>();
        for (Entry<@NonNull String, Map<@NonNull String, List<Integer>>> entry : pollAspectCounts.entrySet()) {

            Map<@NonNull String, List<Integer>> dataSet = entry.getValue();
            List<LamiTableEntry> entries = new ArrayList<>();

            for (Entry<@NonNull String, List<Integer>> element : dataSet.entrySet()) {
                /*
                 * Calculate the number of successful polls, along with the
                 * minimum and maximum polls values
                 */
                int nbSuccessfulPolls = element.getValue().size();
                int minPollValue = Collections.min(element.getValue());
                int maxPollValue = Collections.max(element.getValue());

                // calculate the mean and the standard deviation
                double avgPollValue = element.getValue().stream().mapToInt(i -> i).average().orElse(0);
                double sd = element.getValue().stream().mapToDouble(val -> Math.pow(val - avgPollValue, 2)).sum();
                double std = Math.sqrt(sd / element.getValue().size());

                BigDecimal bd = new BigDecimal(std).setScale(2, RoundingMode.HALF_UP);
                double rounded = bd.doubleValue();

                List<@NonNull LamiData> data = Arrays.asList(
                        new LamiString(element.getKey()),
                        new LamiLongNumber((long) minPollValue),
                        new LamiLongNumber((long) maxPollValue),
                        new LamiLongNumber((long) avgPollValue),
                        new LamiDoubleNumber(rounded),
                        new LamiLongNumber((long) nbSuccessfulPolls));

                entries.add(new LamiTableEntry(data));
            }

            List<@NonNull LamiTableEntryAspect> tableAspects = Arrays.asList(new LamiCategoryAspect(entry.getKey(), 0),
                    new LamiCountAspect(Messages.EthdevPollStats_MinimumValueLabel, 1),
                    new LamiCountAspect(Messages.EthdevPollStats_MaximumValueLabel, 2),
                    new LamiCountAspect(Messages.EthdevPollStats_AverageValueLabel, 3),
                    new LamiCountAspect(Messages.EthdevPollStats_StandardDeviationLabel, 4),
                    new LamiCountAspect(Messages.EthdevPollStats_CountLabel, 5));
            LamiTableClass tableClass = new LamiTableClass(entry.getKey(), entry.getKey(), tableAspects, Collections.emptySet());
            LamiResultTable lrt = new LamiResultTable(createTimeRange(timeRange), tableClass, entries);
            results.add(lrt);
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
     * Todo, move to LAMI
     */
    private final class LamiString extends LamiData {
        private final String fElement;

        private LamiString(@NonNull String element) {
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
