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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.StateValues;
import org.eclipse.tracecompass.analysis.os.linux.core.model.ProcessStatus;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXContext;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;

/** Projects shared TraceX execution transitions into the stock kernel schema. */
final class TraceXKernelState {

    // The stock OS providers consume these persistent attribute names. Their
    // defining Attributes interface is internal to the OS Linux bundle.
    static final String CPUS = "CPUs"; //$NON-NLS-1$
    static final String CPU = "0"; //$NON-NLS-1$
    static final String CURRENT_THREAD = "Current_thread"; //$NON-NLS-1$
    private static final String THREADS = "Threads"; //$NON-NLS-1$
    private static final String EXEC_NAME = "Exec_name"; //$NON-NLS-1$
    private static final String PRIO = "Prio"; //$NON-NLS-1$
    private static final String IRQS = "IRQs"; //$NON-NLS-1$

    private final TraceXTrace fTrace;
    private final Map<Integer, Long> fInterrupts = new HashMap<>();
    private int fThread = -1;

    TraceXKernelState(TraceXTrace trace) {
        fTrace = trace;
    }

    void seed(ITmfStateSystemBuilder ss, long timestamp) {
        for (Map.Entry<Integer, String> thread : fTrace.getThreadNames().entrySet()) {
            int tid = thread.getKey();
            int quark = threadQuark(ss, tid);
            ss.modifyAttribute(timestamp, ProcessStatus.WAIT_UNKNOWN.getStateValue().unboxValue(), quark);
            ss.modifyAttribute(timestamp, thread.getValue(), ss.getQuarkRelativeAndAdd(quark, EXEC_NAME));
        }
        for (Map.Entry<Integer, Integer> priority : fTrace.getThreadPriorities().entrySet()) {
            ss.modifyAttribute(timestamp, priority.getValue(), priorityQuark(ss, priority.getKey()));
        }
        ss.getQuarkAbsoluteAndAdd(CPUS, CPU, CURRENT_THREAD);
    }

    void observePriority(ITmfStateSystemBuilder ss, long timestamp, TraceXContext context) {
        int tid = fTrace.getThreadId(context.getRawAddress());
        if (tid > 0) {
            ss.modifyAttribute(timestamp, context.getPriority(), priorityQuark(ss, tid));
        }
    }

    void enterInterrupt(ITmfStateSystemBuilder ss, long timestamp, int depth, long number) {
        fInterrupts.put(depth, number);
        ss.modifyAttribute(timestamp, Integer.valueOf(0), interruptQuark(ss, number));
    }

    void exitInterrupt(ITmfStateSystemBuilder ss, long timestamp, int depth) {
        Long number = fInterrupts.remove(depth);
        if (number != null && !fInterrupts.containsValue(number)) {
            ss.modifyAttribute(timestamp, (Object) null, interruptQuark(ss, number));
        }
    }

    void switchActive(ITmfStateSystemBuilder ss, long timestamp,
            @Nullable TraceXExecutionContext next, @Nullable TraceXExecutionContext scheduler) {
        @Nullable Integer cpuStatus = null;
        int tid = -1;
        boolean interrupted = false;
        if (next != null) {
            switch (next.getType()) {
            case THREAD:
                tid = fTrace.getThreadId(next.getAddress());
                if (tid > 0) {
                    cpuStatus = StateValues.CPU_STATUS_RUN_USERMODE;
                }
                break;
            case IDLE:
                tid = TraceXTrace.IDLE_TID;
                cpuStatus = StateValues.CPU_STATUS_IDLE;
                break;
            case ISR:
                // Scheduler selections inside an interrupt become active only
                // when the shared execution engine unwinds the interrupt.
                tid = fThread;
                if (tid < 0 && scheduler != null) {
                    tid = fTrace.getThreadId(scheduler.getAddress());
                }
                cpuStatus = StateValues.CPU_STATUS_IRQ;
                interrupted = true;
                break;
            case INITIALIZATION:
            case UNKNOWN:
            default:
                break;
            }
        }
        if (fThread > 0 && fThread != tid) {
            setThreadStatus(ss, timestamp, fThread, ProcessStatus.WAIT_CPU);
        }
        fThread = tid;
        if (tid > 0) {
            setThreadStatus(ss, timestamp, tid, interrupted ? ProcessStatus.INTERRUPTED : ProcessStatus.RUN);
        }
        ss.modifyAttribute(timestamp, tid < 0 ? null : Integer.valueOf(tid),
                ss.getQuarkAbsoluteAndAdd(CPUS, CPU, CURRENT_THREAD));
        ss.modifyAttribute(timestamp, cpuStatus, ss.getQuarkAbsoluteAndAdd(CPUS, CPU));
    }

    private static void setThreadStatus(ITmfStateSystemBuilder ss, long timestamp, int tid, ProcessStatus status) {
        ss.modifyAttribute(timestamp, status.getStateValue().unboxValue(), threadQuark(ss, tid));
    }

    private static int threadQuark(ITmfStateSystemBuilder ss, int tid) {
        return ss.getQuarkAbsoluteAndAdd(THREADS, Integer.toString(tid));
    }

    private static int priorityQuark(ITmfStateSystemBuilder ss, int tid) {
        return ss.getQuarkRelativeAndAdd(threadQuark(ss, tid), PRIO);
    }

    private static int interruptQuark(ITmfStateSystemBuilder ss, long number) {
        String name = Long.toString(number);
        if (number > Integer.MAX_VALUE) {
            // Resources parses the prefix as a signed int and preserves the
            // full attribute name as its label. Keep unsigned IDs distinct
            // from the unknown interrupt (-1).
            name = Integer.toString((int) number) + "/" + name; //$NON-NLS-1$
        }
        return ss.getQuarkAbsoluteAndAdd(CPUS, CPU, IRQS, name);
    }
}
