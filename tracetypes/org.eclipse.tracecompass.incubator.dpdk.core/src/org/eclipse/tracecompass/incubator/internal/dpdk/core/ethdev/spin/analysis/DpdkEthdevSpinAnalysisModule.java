/**********************************************************************
 * Copyright (c) 2024 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.spin.analysis;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.tracecompass.incubator.dpdk.core.trace.DpdkTrace;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.Activator;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.analysis.DpdkEthdevEventLayout;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement.PriorityLevel;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAnalysisEventRequirement;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.ImmutableList;

/**
 * This analysis module estimates the percentage of time a PMD thread spends
 * performing actual work (e.g., fetching and processing packets). The obtained
 * results can be used to evaluate PMD thread efficiency. The analysis is based
 * on two events:
 *
 * 1- the "lib.ethdev.rx.burst.empty" event indicates an empty poll where no
 * packets were fetched.
 *
 * 2- the "lib.ethdev.rx.burst.nonempty" event indicates a successful poll where
 * one or more packets were fetched
 *
 * @author Adel Belkhiri
 */
public class DpdkEthdevSpinAnalysisModule extends TmfStateSystemAnalysisModule {

    /** The ID of this analysis module */
    public static final String ID = "org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.spin.analysis"; //$NON-NLS-1$
    private final DpdkEthdevEventLayout fLayout = new DpdkEthdevEventLayout();

    private final TmfAbstractAnalysisRequirement REQUIREMENT = new TmfAnalysisEventRequirement(ImmutableList.of(
            fLayout.eventEthdevRxBurstEmpty(), fLayout.eventEthdevRxBurstNonEmpty()), PriorityLevel.AT_LEAST_ONE);

    @Override
    protected ITmfStateProvider createStateProvider() {
        ITmfTrace trace = checkNotNull(getTrace());

        if (trace instanceof DpdkTrace) {
            return new DpdkEthdevSpinStateProvider(trace, fLayout, ID);
        }

        throw new IllegalStateException();
    }

    @Override
    public Iterable<TmfAbstractAnalysisRequirement> getAnalysisRequirements() {
        return Collections.singleton(REQUIREMENT);
    }

    /**
     * Computes the time spent in active and spin states for specified threads
     * within a given time range.
     *
     * @param threads
     *            A set of thread identifiers to analyze
     * @param start
     *            Start timestamp of the analysis interval
     * @param end
     *            End timestamp
     * @return A map where each key is a thread name, and the value is a pair
     *         containing time spent in active and spin states, respectively
     */
    public Map<String, Pair<Long, Long>> calculateThreadStateDurations(Set<Integer> threads, long start, long end) {
        Map<String, Pair<Long, Long>> map = new HashMap<>();

        ITmfTrace trace = getTrace();
        ITmfStateSystem threadSs = getStateSystem();
        if (trace == null || threadSs == null) {
            return map;
        }

        long startTime = Math.max(start, threadSs.getStartTime());
        long endTime = Math.min(end, threadSs.getCurrentEndTime());
        if (endTime < startTime) {
            return map;
        }

        try {
            int threadsQuark = threadSs.getQuarkAbsolute(DpdkEthdevSpinAttributes.POLL_THREADS);
            for (int threadQuark : threadSs.getSubAttributes(threadsQuark, false)) {
                if (!threads.contains(threadQuark)) {
                    continue;
                }

                String threadName = threadSs.getAttributeName(threadQuark);
                long countActive = 0;
                long countSpin = 0;
                for (int queueQuark : threadSs.getSubAttributes(threadQuark, false)) {
                    countActive += calculateStateCount(threadSs, queueQuark, startTime, endTime, DpdkEthdevSpinAttributes.ACTIVE_STATUS);
                    countSpin += calculateStateCount(threadSs, queueQuark, startTime, endTime, DpdkEthdevSpinAttributes.SPIN_STATUS);
                }

                map.put(threadName, new Pair<>(countActive, countSpin));
            }

        } catch (TimeRangeException | AttributeNotFoundException e) {
            Activator.getInstance().logError(e.getMessage());
        }

        return map;
    }

    /**
     * Computes the time a thread spent in a specific state within the given
     * time range.
     *
     * @param stateSystem
     *            State system
     * @param attributeNode
     *            The node representing the thread state.
     * @param startTime
     *            Start timestamp
     * @param endTime
     *            End timestamp
     * @param targetState
     *            The state to analyze (e.g., active or spin)
     * @return The total time spent in the target state
     */
    private static long calculateStateCount(ITmfStateSystem stateSystem, int attributeNode, long startTime, long endTime, String targetState) {
        long count = 0;
        long ts = startTime;

        try {
            while (ts < endTime) {
                ITmfStateInterval stateInterval = stateSystem.querySingleState(ts, attributeNode);
                Object stateValue = stateInterval.getStateValue().unboxValue();
                long stateStart = stateInterval.getStartTime();
                long stateEnd = stateInterval.getEndTime();

                if (stateValue != null && targetState.equals(stateValue)) {
                    count += interpolateCount(startTime, endTime, stateStart, stateEnd);
                }
                ts = Math.min(stateEnd, endTime) + 1;
            }
        } catch (TimeRangeException | StateSystemDisposedException e) {
            Activator.getInstance().logError(e.getMessage());
        }

        return count;
    }

    /**
     * Adjusts the time interval to ensure it fits within the specified range.
     *
     * @param startTime
     *            Start timestamp of the analysis interval
     * @param endTime
     *            End timestamp of the analysis interval
     * @param startInterval
     *            Start timestamp of the state interval
     * @param endInterval
     *            End timestamp of the state interval
     * @return
     */
    private static long interpolateCount(long startTime, long endTime, long startInterval, long endInterval) {

        long count = endInterval - startInterval;

        /* Sanity check */
        if (count > 0) {
            if (startTime > startInterval) {
                count -= (startTime - startInterval);
            }

            if (endTime < endInterval) {
                count -= (endInterval - endTime);
            }
        }
        return count;
    }
}
