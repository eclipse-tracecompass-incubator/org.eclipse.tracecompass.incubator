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

import org.eclipse.jdt.annotation.NonNull;
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
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.ImmutableList;

/**
 * This analysis provides an estimation for the percentage of time a PMD thread
 * was doing a real work (e.g., fetching packets, processing them, etc.). It is
 * based on two events: the "lib.ethdev.rx.burst.empty" event denotes an empty
 * poll, and the "lib.ethdev.rx.burst.nonempty" event denotes a successful poll,
 * where one or many packets were retrieved.
 *
 * @author Adel Belkhiri
 */
public class DpdkEthdevSpinAnalysisModule extends TmfStateSystemAnalysisModule {

    /** The ID of this analysis module */
    public static final String ID = "org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.spin.analysis"; //$NON-NLS-1$

    private final TmfAbstractAnalysisRequirement REQUIREMENT = new TmfAnalysisEventRequirement(ImmutableList.of(
            DpdkEthdevEventLayout.eventEthdevRxqBurstEmpty(), DpdkEthdevEventLayout.eventEthdevRxqBurstNonEmpty()), PriorityLevel.AT_LEAST_ONE);

    @Override
    protected ITmfStateProvider createStateProvider() {
        ITmfTrace trace = checkNotNull(getTrace());

        if (trace instanceof TmfTrace) {
            return new DpdkEthdevSpinStateProvider(trace, ID);
        }

        throw new IllegalStateException();
    }

    @Override
    public Iterable<TmfAbstractAnalysisRequirement> getAnalysisRequirements() {
        return Collections.singleton(REQUIREMENT);
    }

    /**
     * Calculates thread usage witin a specific time interval
     *
     * @param start
     *            start timestamp
     * @param end
     *            end timestamp
     * @return A map of Thread names -> time spent in empty or active iterations
     */
    public Map<String, Pair<Long, Long>> getThreadUsageInRange(Set<@NonNull Integer> threads, long start, long end) {
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
            int threadsNode = threadSs.getQuarkAbsolute(Attributes.POLL_THREADS);
            for (int threadNode : threadSs.getSubAttributes(threadsNode, false)) {
                if (!threads.contains(threadNode)) {
                    continue;
                }

                String curThreadName = threadSs.getAttributeName(threadNode);
                long countActive = 0;
                long countSpin = 0;
                for (int queueNode : threadSs.getSubAttributes(threadNode, false)) {
                    long ts = startTime;
                    do {
                        ITmfStateInterval stateInterval = threadSs.querySingleState(ts, queueNode);
                        Object stateValue = stateInterval.getStateValue().unboxValue();
                        long stateStart = stateInterval.getStartTime();
                        long stateEnd = stateInterval.getEndTime();

                        if (stateValue != null) {
                            String threadState = (String) stateValue;
                            if (threadState.equals(Attributes.ACTIVE_STATUS)) {
                                countActive += interpolateCount(startTime, endTime, stateEnd, stateStart);
                            } else if (threadState.equals(Attributes.SPIN_STATUS)) {
                                countSpin += interpolateCount(startTime, endTime, stateEnd, stateStart);
                            }
                        }
                        ts = Math.min(stateEnd, endTime) + 1;
                    } while (ts < endTime);
                }

                map.put(curThreadName, new Pair<>(countActive, countSpin));

            }
        } catch (TimeRangeException | AttributeNotFoundException | StateSystemDisposedException e) {
        }

        return map;
    }

    private static long interpolateCount(long startTime, long endTime, long spinningEnd, long spinningStart) {

        long count = spinningEnd - spinningStart;

        /* sanity check */
        if (count > 0) {
            if (startTime > spinningStart) {
                count -= (startTime - spinningStart);
            }

            if (endTime < spinningEnd) {
                count -= (spinningEnd - endTime);
            }
        }
        return count;
    }
}
