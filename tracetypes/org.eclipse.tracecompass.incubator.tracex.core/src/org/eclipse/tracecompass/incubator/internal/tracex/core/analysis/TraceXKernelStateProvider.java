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

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.StateValues;
import org.eclipse.tracecompass.analysis.os.linux.core.model.ProcessStatus;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXContext;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXEvent;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;

/**
 * Builds the standard Linux kernel-shaped state system from TraceX (ThreadX)
 * events so the stock Control Flow View and Resources View populate directly.
 *
 * <p>
 * The attribute tree mirrors the one produced by the Trace Compass
 * {@code KernelAnalysisModule}, which both views read:
 *
 * <pre>
 *   Threads/&lt;tid&gt;            -&gt; {@link ProcessStatus} value (thread status)
 *   Threads/&lt;tid&gt;/Exec_name   -&gt; thread name
 *   Threads/&lt;tid&gt;/Prio        -&gt; ThreadX priority
 *   CPUs/0/Current_thread      -&gt; running tid
 *   CPUs/0                     -&gt; CPU status ({@link StateValues})
 *   CPUs/0/IRQs/&lt;irq&gt;        -&gt; CPU id while an ISR is active
 * </pre>
 *
 * The attribute names are the stable on-disk names used by the kernel analysis;
 * they are intentionally plain string literals because the defining
 * {@code Attributes} interface is internal to the OS-Linux plug-in.
 */
public final class TraceXKernelStateProvider extends AbstractTmfStateProvider {

    /** First-level attribute: CPUs. */
    private static final String CPUS = "CPUs"; //$NON-NLS-1$
    /** First-level attribute: Threads. */
    private static final String THREADS = "Threads"; //$NON-NLS-1$
    /** CPU sub-attribute: current thread. */
    private static final String CURRENT_THREAD = "Current_thread"; //$NON-NLS-1$
    /** CPU sub-attribute: IRQ lanes. */
    private static final String IRQS = "IRQs"; //$NON-NLS-1$
    /** Thread sub-attribute: executable/thread name. */
    private static final String EXEC_NAME = "Exec_name"; //$NON-NLS-1$
    /** Thread sub-attribute: priority. */
    private static final String PRIO = "Prio"; //$NON-NLS-1$

    /** TraceX is single-core; everything runs on CPU 0. */
    private static final String CPU = "0"; //$NON-NLS-1$

    /** State-system version; bump when the attribute layout changes. */
    public static final int VERSION = 1;

    private static final int THREAD_RESUME = 1;
    private static final int THREAD_SUSPEND = 2;
    private static final int ISR_ENTER = 3;
    private static final int ISR_EXIT = 4;
    private static final int TIME_SLICE = 5;
    private static final int RUNNING = 6;
    private static final int THREAD_RELINQUISH = 109;

    private final TraceXTrace fTrace;
    private final Deque<Integer> fIsrStack = new ArrayDeque<>();
    private int fCurrentThread = TraceXTrace.IDLE_TID;
    private boolean fSeeded;

    /**
     * Create a provider.
     *
     * @param trace
     *            the initialized TraceX trace
     */
    public TraceXKernelStateProvider(TraceXTrace trace) {
        super(trace, TraceXKernelAnalysis.TRACEX_KERNEL_ID);
        fTrace = trace;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public ITmfStateProvider getNewInstance() {
        return new TraceXKernelStateProvider(fTrace);
    }

    @Override
    protected void eventHandle(ITmfEvent tmfEvent) {
        ITmfStateSystemBuilder ss = getStateSystemBuilder();
        Object value = tmfEvent.getContent().getValue();
        if (ss == null || !(value instanceof TraceXEvent)) {
            return;
        }
        TraceXEvent event = (TraceXEvent) value;
        long ts = tmfEvent.getTimestamp().toNanos();
        if (!fSeeded) {
            seedThreads(ss, ts);
            fSeeded = true;
        }

        switch (event.getNormalizedEventId()) {
        case THREAD_RESUME:
        case THREAD_SUSPEND:
            schedule(ss, ts, threadId(event.getInformationWord(4)));
            break;
        case TIME_SLICE:
            schedule(ss, ts, threadId(event.getInformationWord(1)));
            break;
        case THREAD_RELINQUISH:
            schedule(ss, ts, threadId(event.getInformationWord(2)));
            break;
        case RUNNING:
            TraceXContext context = event.getContext();
            if (context.getKind() == TraceXContext.Kind.THREAD
                    || context.getKind() == TraceXContext.Kind.IDLE) {
                schedule(ss, ts, fTrace.getThreadId(context.getRawAddress()));
            }
            break;
        case ISR_ENTER:
            enterIsr(ss, ts, (int) event.getInformationWord(2));
            break;
        case ISR_EXIT:
            exitIsr(ss, ts);
            break;
        default:
            break;
        }
    }

    /** Create a Threads/&lt;tid&gt; subtree for every known thread up front. */
    private void seedThreads(ITmfStateSystemBuilder ss, long ts) {
        for (Map.Entry<Integer, String> thread : fTrace.getThreadNames().entrySet()) {
            int tid = thread.getKey().intValue();
            int threadQuark = ss.getQuarkAbsoluteAndAdd(THREADS, Integer.toString(tid));
            ss.modifyAttribute(ts, ProcessStatus.WAIT_UNKNOWN.getStateValue().unboxValue(), threadQuark);
            int nameQuark = ss.getQuarkRelativeAndAdd(threadQuark, EXEC_NAME);
            ss.modifyAttribute(ts, thread.getValue(), nameQuark);
        }
        // Ensure the CPU and its current-thread attribute exist from the start.
        ss.getQuarkAbsoluteAndAdd(CPUS, CPU, CURRENT_THREAD);
    }

    private int threadId(long address) {
        return fTrace.getThreadId(address);
    }

    /**
     * Switch the running thread: mark the previous thread as waiting for CPU and
     * the new one as running, and update the CPU current-thread and status.
     */
    private void schedule(ITmfStateSystemBuilder ss, long ts, int tid) {
        if (tid < 0 || tid == fCurrentThread) {
            return;
        }
        if (!fIsrStack.isEmpty()) {
            // Defer the thread switch display until interrupts unwind, but still
            // record the pending running thread.
            fCurrentThread = tid;
            setCurrentThread(ss, ts, tid);
            return;
        }
        if (fCurrentThread > TraceXTrace.IDLE_TID) {
            setThreadStatus(ss, ts, fCurrentThread, ProcessStatus.WAIT_CPU);
        }
        fCurrentThread = tid;
        setCurrentThread(ss, ts, tid);
        if (tid > TraceXTrace.IDLE_TID) {
            setThreadStatus(ss, ts, tid, ProcessStatus.RUN);
            setCpuStatus(ss, ts, StateValues.CPU_STATUS_RUN_USERMODE);
        } else {
            setCpuStatus(ss, ts, StateValues.CPU_STATUS_IDLE);
        }
    }

    private void enterIsr(ITmfStateSystemBuilder ss, long ts, int irq) {
        fIsrStack.push(Integer.valueOf(irq));
        int irqQuark = ss.getQuarkAbsoluteAndAdd(CPUS, CPU, IRQS, Integer.toString(irq));
        ss.modifyAttribute(ts, Integer.valueOf(0), irqQuark); // value = CPU id (0)
        if (fCurrentThread > TraceXTrace.IDLE_TID) {
            setThreadStatus(ss, ts, fCurrentThread, ProcessStatus.INTERRUPTED);
        }
        setCpuStatus(ss, ts, StateValues.CPU_STATUS_IRQ);
    }

    private void exitIsr(ITmfStateSystemBuilder ss, long ts) {
        Integer irq = fIsrStack.poll();
        if (irq != null) {
            int irqQuark = ss.getQuarkAbsoluteAndAdd(CPUS, CPU, IRQS, Integer.toString(irq.intValue()));
            ss.modifyAttribute(ts, (Object) null, irqQuark);
        }
        if (!fIsrStack.isEmpty()) {
            setCpuStatus(ss, ts, StateValues.CPU_STATUS_IRQ);
            return;
        }
        // Interrupts fully unwound: restore the running thread.
        if (fCurrentThread > TraceXTrace.IDLE_TID) {
            setThreadStatus(ss, ts, fCurrentThread, ProcessStatus.RUN);
            setCpuStatus(ss, ts, StateValues.CPU_STATUS_RUN_USERMODE);
        } else {
            setCpuStatus(ss, ts, StateValues.CPU_STATUS_IDLE);
        }
    }

    private static void setCurrentThread(ITmfStateSystemBuilder ss, long ts, int tid) {
        int quark = ss.getQuarkAbsoluteAndAdd(CPUS, CPU, CURRENT_THREAD);
        ss.modifyAttribute(ts, Integer.valueOf(tid), quark);
    }

    private static void setCpuStatus(ITmfStateSystemBuilder ss, long ts, int status) {
        int quark = ss.getQuarkAbsoluteAndAdd(CPUS, CPU);
        ss.modifyAttribute(ts, Integer.valueOf(status), quark);
    }

    private void setThreadStatus(ITmfStateSystemBuilder ss, long ts, int tid,
            ProcessStatus status) {
        int threadQuark = ss.getQuarkAbsoluteAndAdd(THREADS, Integer.toString(tid));
        ss.modifyAttribute(ts, status.getStateValue().unboxValue(), threadQuark);
        ensurePriority(ss, ts, tid);
    }

    /** Populate the Prio sub-attribute once from the registry initial priority. */
    private void ensurePriority(ITmfStateSystemBuilder ss, long ts, int tid) {
        int prioQuark = ss.getQuarkAbsoluteAndAdd(THREADS, Integer.toString(tid), PRIO);
        if (ss.queryOngoing(prioQuark) != null) {
            return;
        }
        Integer prio = priorityFor(tid);
        if (prio != null) {
            ss.modifyAttribute(ts, prio, prioQuark);
        }
    }

    private @Nullable Integer priorityFor(int tid) {
        try {
            String name = fTrace.getThreadName(tid);
            if (name == null) {
                return null;
            }
            return fTrace.getRegistry().getRecords().stream()
                    .filter(r -> r.isValid() && name.equals(r.getName()))
                    .map(r -> r.getInitialPriority())
                    .filter(p -> p != null)
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }
}
