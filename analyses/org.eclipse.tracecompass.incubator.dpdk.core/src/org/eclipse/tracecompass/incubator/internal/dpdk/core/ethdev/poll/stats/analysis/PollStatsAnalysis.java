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
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
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
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiDoubleNumber;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiLongNumber;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiTimeRange;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.types.LamiTimestamp;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest.ExecutionType;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.util.Pair;

/**
 * The DPDK Polls Statistics Analysis is an on-demand analysis that computes
 * statistics related to the polling of RX queues of Ethernet devices by PMD
 * (Poll-Mode Driver) threads through calls to `rte_eth_rx_burst()`. The
 * statistics include, per queue and per thread, the minimum, maximum, average,
 * and standard deviation of the number of packets retrieved in a single call to
 * the `rte_eth_rx_burst()` function.
 *
 * @author Adel Belkhiri
 */
@SuppressWarnings("restriction")
public class PollStatsAnalysis extends LamiAnalysis {

    private static final long MASK = (1 << 10) - 1L;
    private static final int MEMORY_SANITY_LIMIT = 40000;

    /**
     * Constructor
     */
    public PollStatsAnalysis() {
        super("DPDK Polls Statistics (ethdev)", false, trace -> true, Collections.emptyList()); //$NON-NLS-1$
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

    @Override
    public List<LamiResultTable> execute(ITmfTrace trace, @Nullable TmfTimeRange timeRange, String extraParamsString, IProgressMonitor monitor) throws CoreException {
        List<LamiResultTable> results = new ArrayList<>();
        List<ITmfEventAspect<Pair<String, Integer>>> aspects = new ArrayList<>();
        TmfTimeRange tr = timeRange == null ? TmfTimeRange.ETERNITY : timeRange;

        /*
         * the queue name and the thread name are required aspects for this
         * analysis
         */
        aspects.add(PORT_QUEUE_NAME_ASPECT);
        aspects.add(THREAD_NAME_ASPECT);

        SubMonitor mon = SubMonitor.convert(monitor, "DPDK Polls Statistics Analysis", workRemaining(trace)); //$NON-NLS-1$
        AtomicLong done = new AtomicLong();

        // the event aspect/count map
        Map<String, Map<String, List<Integer>>> pollAspectCounts = new HashMap<>();

        /* create the event request */
        TmfEventRequest req = new TmfEventRequest(ITmfEvent.class, tr, 0, Integer.MAX_VALUE, ExecutionType.BACKGROUND) {
            @Override
            public void handleData(ITmfEvent event) {
                if (monitor.isCanceled()) {
                    cancel();
                }
                if (event.getName().equals(DpdkEthdevEventLayout.eventEthdevRxqBurstNonEmpty())) {
                    for (ITmfEventAspect<Pair<String, Integer>> aspect : aspects) {
                        Pair<String, Integer> resolved = aspect.resolve(event);
                        if (resolved != null) {
                            Map<String, List<Integer>> dataSet = pollAspectCounts.computeIfAbsent(aspect.getName(), unused -> new HashMap<>());
                            if (dataSet.size() < MEMORY_SANITY_LIMIT) {
                                List<Integer> data = dataSet.computeIfAbsent(resolved.getFirst(), unused -> new ArrayList<>());
                                data.add(resolved.getSecond());
                            }
                        }
                    }
                }
                if ((done.incrementAndGet() & MASK) == 0) {
                    mon.setWorkRemaining(workRemaining(trace));
                    mon.worked(1);

                    monitor.setTaskName("Dpdk Polls Statistics Analysis (" + NumberFormat.getInstance().format(done.get()) + " events read)"); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }

        };
        // send the request to the trace
        trace.sendRequest(req);

        // convert the result to Lami types
        try {
            req.waitForCompletion();
            for (Entry<String, Map<String, List<Integer>>> entry : pollAspectCounts.entrySet()) {

                Map<String, List<Integer>> dataSet = entry.getValue();
                List<LamiTableEntry> entries = new ArrayList<>();

                for (Entry<String, List<Integer>> element : dataSet.entrySet()) {
                    // Calculate the number of successful polls, along with the
                    // min and max polls values
                    int nbSuccessfulPolls = element.getValue().size();
                    int minPollValue = Collections.min(element.getValue());
                    int maxPollValue = Collections.max(element.getValue());

                    // Calculate the mean and the standard deviation
                    double avgPollValue = element.getValue().stream().mapToInt(i -> i).average().orElse(0);
                    double sd = element.getValue().stream().mapToDouble(val -> Math.pow(val - avgPollValue, 2)).sum();
                    double std = Math.sqrt(sd / element.getValue().size());

                    BigDecimal bd = new BigDecimal(std).setScale(2, RoundingMode.HALF_UP);
                    double rounded = bd.doubleValue();

                    List<LamiData> data = Arrays.asList(
                            new LamiString(element.getKey()),
                            new LamiLongNumber((long) minPollValue),
                            new LamiLongNumber((long) maxPollValue),
                            new LamiLongNumber((long) avgPollValue),
                            new LamiDoubleNumber(rounded),
                            new LamiLongNumber((long) nbSuccessfulPolls));

                    entries.add(new LamiTableEntry(data));
                }

                List<LamiTableEntryAspect> tableAspects = Arrays.asList(new LamiCategoryAspect(entry.getKey(), 0),
                        new LamiCountAspect("Minimum Value", 1),
                        new LamiCountAspect("Maximum Value", 2),
                        new LamiCountAspect("Average Value", 3),
                        new LamiCountAspect("Standard Deviation", 4),
                        new LamiCountAspect("Count", 5));
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
     * Todo, move to LAMI
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

    /**
     * Event aspect that resolves in the name of the queue targeted by the poll,
     * and the number of packets retrieved in a single call to the
     * rte_eth_rx_burst() function.
     */
    private static final ITmfEventAspect<Pair<String, Integer>> PORT_QUEUE_NAME_ASPECT = new ITmfEventAspect<Pair<String, Integer>>() {
        @Override
        public String getName() {
            return Messages.getMessage(Messages.AspectName_PortQueueName);
        }

        @Override
        public String getHelpText() {
            return Messages.getMessage(Messages.AspectHelpText_PortQueueName);
        }

        @Override
        public @Nullable Pair<String, Integer> resolve(ITmfEvent event) {
            Integer portId = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldPortId());
            Integer queueId = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldQueueId());
            Integer nbRxPkts = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldNbRxPkts());
            String queueName = "P" + Objects.requireNonNull(portId).toString() + "/Q" + Objects.requireNonNull(queueId).toString(); //$NON-NLS-1$//$NON-NLS-2$

            return new Pair<>(queueName, Objects.requireNonNull(nbRxPkts));
        }
    };

    /**
     * Event aspect that resolves in the name of the thread issuing the current
     * poll, along with the number of retrieved packets
     */
    private static final ITmfEventAspect<Pair<String, Integer>> THREAD_NAME_ASPECT = new ITmfEventAspect<Pair<String, Integer>>() {
        @Override
        public String getName() {
            return Messages.getMessage(Messages.AspectName_ThreadName);
        }

        @Override
        public String getHelpText() {
            return Messages.getMessage(Messages.AspectHelpText_ThreadName);
        }

        @Override
        public @Nullable Pair<String, Integer> resolve(ITmfEvent event) {
            String threadName = event.getContent().getFieldValue(String.class, DpdkEthdevEventLayout.fieldThreadName());
            Integer nbRxPkts = event.getContent().getFieldValue(Integer.class, DpdkEthdevEventLayout.fieldNbRxPkts());

            return new Pair<>(threadName, Objects.requireNonNull(nbRxPkts));
        }
    };
}
