/*******************************************************************************
 * Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.tracex.core.analysis;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.cpuusage.KernelCpuUsageAnalysis;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.StateValues;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;

/** Stock CPU Usage adapter counting known thread execution, excluding interrupts. */
public class TraceXCpuUsageAnalysis extends KernelCpuUsageAnalysis {

    @Override
    public String getId() {
        return KernelCpuUsageAnalysis.ID;
    }

    @Override
    protected ITmfStateProvider createStateProvider() {
        return new TraceXStateProvider((TraceXTrace) Objects.requireNonNull(getTrace()));
    }

    @Override
    protected String getSsFileName() {
        return ID + ".v" + TraceXStateProvider.VERSION + ".ht"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public Iterable<TmfAbstractAnalysisRequirement> getAnalysisRequirements() {
        // TraceX retains its original event names and has no sched_switch event.
        return Collections.emptyList();
    }

    @Override
    public int getNumberOfCores() {
        return 1;
    }

    @Override
    public Map<String, Long> getCpuUsageInRange(Set<@NonNull Integer> cpus, long start, long end) {
        ITmfStateSystem ss = getStateSystem();
        if (ss == null || (!cpus.isEmpty() && !cpus.contains(0))) {
            return Collections.emptyMap();
        }
        long from = Math.max(start, ss.getStartTime());
        long to = Math.min(end, ss.getCurrentEndTime());
        int currentThread = ss.optQuarkAbsolute(TraceXKernelState.CPUS, TraceXKernelState.CPU,
                TraceXKernelState.CURRENT_THREAD);
        int cpu = ss.optQuarkAbsolute(TraceXKernelState.CPUS, TraceXKernelState.CPU);
        if (from >= to || currentThread == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return Collections.emptyMap();
        }
        Map<String, Long> result = new HashMap<>();
        long total = 0;
        try {
            long cursor = from;
            while (cursor < to) {
                ITmfStateInterval thread = ss.querySingleState(cursor, currentThread);
                ITmfStateInterval status = ss.querySingleState(cursor, cpu);
                long intervalEnd = Math.min(thread.getEndTime(), status.getEndTime());
                long next = intervalEnd >= to ? to : intervalEnd + 1;
                Object tid = thread.getValue();
                if (tid instanceof Integer && ((Integer) tid).intValue() > 0
                        && Integer.valueOf(StateValues.CPU_STATUS_RUN_USERMODE).equals(status.getValue())) {
                    long duration = next - cursor;
                    String suffix = SPLIT_STRING + tid;
                    result.merge(TraceXKernelState.CPU + suffix, duration, Long::sum);
                    result.merge(TOTAL + suffix, duration, Long::sum);
                    total += duration;
                }
                cursor = next;
            }
        } catch (StateSystemDisposedException e) {
            return Collections.emptyMap();
        }
        result.put(TraceXKernelState.CPU, total);
        result.put(TOTAL, total);
        return result;
    }
}
