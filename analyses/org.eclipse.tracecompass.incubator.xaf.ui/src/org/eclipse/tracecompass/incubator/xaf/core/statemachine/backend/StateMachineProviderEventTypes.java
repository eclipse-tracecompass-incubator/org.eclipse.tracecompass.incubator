/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.xaf.core.statemachine.backend;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * The state provider used by our state machine state system analysis.
 *
 * <pre>
 * (root)
 *   `-- tid
 *        |-- (thread id 1)
 *        |    |-- (timer:cpu_usage)
 *        |    |-- (timer:wait_blocked)
 *        |    |-- (timer:wait_for_cpu)
 *        |    |-- (counter:syscalls)
 *        |    `-- (counter:preempt)
 *        |-- (thread id 2)
 *        |   ...
 *        |-- (thread id 3)
 *        |   ...
 *       ...
 * </pre>
 *
 * @author Raphaël Beamonte
 */
class StateMachineProviderEventTypes extends AbstractTmfStateProvider {

    /**
     * Version number of this input handler. Please bump this if you modify the
     * contents of the generated state history in some way.
     */
    private static final int VERSION = 2016070701;

    /** Forgotten string chains */
    private static final @NonNull String LAYOUT_CONTEXT_VTID = "context._vtid"; //$NON-NLS-1$
    private static final @NonNull String LAYOUT_PREV_PRIO = "prev_prio"; //$NON-NLS-1$
    private static final @NonNull String LAYOUT_STATE = "state"; //$NON-NLS-1$
    private static final @NonNull String LAYOUT_CPU_ID = "cpu_id"; //$NON-NLS-1$
    private static final @NonNull String EVENT_SCHED_WAKING = "sched_waking"; //$NON-NLS-1$
    private static final @NonNull String EVENT_IRQ_SOFTIRQ_ENTRY = "irq_softirq_entry"; //$NON-NLS-1$
    private static final @NonNull String EVENT_POWER_CPU_FREQUENCY = "power_cpu_frequency"; //$NON-NLS-1$

    /**
     * A map giving information about what process
     * is on what CPU
     */
    private Map<Integer, Long> threadByCPU = new TreeMap<>();

    /**
     * Save the information to compute the CPU Usage by TID
     */
    private Map<Long, ThreadInfo> info_by_tid = new HashMap<>();
    private @NonNull ThreadInfo getThreadInfoOrCreate(Long tid) {
        ThreadInfo threadInfo = info_by_tid.get(tid);
        if (threadInfo == null) {
            threadInfo = new ThreadInfo();
            info_by_tid.put(tid, threadInfo);
        }
        return threadInfo;
    }

    /**
     * Constructor
     *
     * @param trace
     *            The trace for which we build this state system
     */
    public StateMachineProviderEventTypes(@NonNull ITmfTrace trace) {
        super(trace , StateMachineBackendAnalysis.NAME);
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public StateMachineProviderEventTypes getNewInstance() {
        return new StateMachineProviderEventTypes(this.getTrace());
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());
        int quark;

        /* Since this can be used for any trace types, normalize all the
         * timestamp values to nanoseconds. */
        final long ts = event.getTimestamp().toNanos();

        final String eventName = event.getType().getName();
        IKernelAnalysisEventLayout layout = ((IKernelTrace) getTrace()).getKernelEventLayout();

        if (eventName.equals(layout.eventSchedSwitch())) {
            ITmfEventField content = event.getContent();
            Long prevTid = (Long)content.getField(layout.fieldPrevTid()).getValue();
            Long nextTid = (Long)content.getField(layout.fieldNextTid()).getValue();
            Long prevState = (Long)content.getField(layout.fieldPrevState()).getValue();
            Integer nextPrio = ((Long)content.getField(layout.fieldNextPrio()).getValue()).intValue();
            Integer prevPrio = ((Long)content.getField(LAYOUT_PREV_PRIO).getValue()).intValue();

            Integer cpu = checkNotNull((Integer)TmfTraceUtils.resolveEventAspectOfClassForEvent(getTrace(), TmfCpuAspect.class, event));
            threadByCPU.put(cpu, nextTid);

            ThreadInfo prevThreadInfo = getThreadInfoOrCreate(prevTid);
            ThreadInfo nextThreadInfo = getThreadInfoOrCreate(nextTid);

            ////////////////////////////////
            // NEXT THREAD (BEING SCHEDULED)
            int quarkNextTid = ss.getQuarkAbsoluteAndAdd(Attributes.TID, nextTid.toString());

            // CPU: update the cpu number if needed
            if (nextThreadInfo.cpu_last != cpu) {
                try {
                    quark = ss.getQuarkRelativeAndAdd(quarkNextTid, Attributes.CPU);
                    TmfStateValue value = TmfStateValue.newValueInt(cpu);
                    ss.modifyAttribute(ts, value, quark);
                    nextThreadInfo.cpu_last = cpu;
                } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }

            // PRIO: update the priority if needed
            if (nextThreadInfo.prio_last != nextPrio) {
                try {
                    quark = ss.getQuarkRelativeAndAdd(quarkNextTid, Attributes.PRIO);
                    TmfStateValue value = TmfStateValue.newValueInt(nextPrio);
                    ss.modifyAttribute(ts, value, quark);
                    nextThreadInfo.prio_last = nextPrio;
                } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }

            // STATE: Change to RUNNING
            BackendState s = null;
            if (!nextThreadInfo.stack_state.isEmpty()) {
                nextThreadInfo.stack_state.pop();
                s = nextThreadInfo.stack_state.peek();
            }
            if (s == null) {
                s = new BackendState(BackendStateValue.RUNNING.getValue());
            }
            try {
                quark = ss.getQuarkRelativeAndAdd(quarkNextTid, Attributes.STATE);
                TmfStateValue value = TmfStateValue.newValueLong(s.getValue());
                ss.modifyAttribute(ts, value, quark);
            } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                e.printStackTrace();
            }

            // TIMER_WAIT_(BLOCKED|FOR_CPU)
            // TODO: find a way to update the time spent in WAIT_FOR_SOMETHING
            // for cases where the thread hasn't returned when finishing
            if (nextThreadInfo.last_state != null) {
                if (nextThreadInfo.last_state != 0 && nextThreadInfo.last_state != 1024) {
                    // TIMER_WAIT_BLOCKED
                    try {
                        quark = ss.getQuarkRelativeAndAdd(quarkNextTid, Attributes.TIMER_WAIT_BLOCKED);

                        if (nextThreadInfo.cumul_wait_blocked == 0L) {
                            TmfStateValue value = TmfStateValue.newValueLong(0L);
                            ss.modifyAttribute(getTrace().getStartTime().toNanos(), value, quark);
                        }

                        nextThreadInfo.cumul_wait_blocked += ts - nextThreadInfo.last_ts;

                        TmfStateValue value = TmfStateValue.newValueLong(nextThreadInfo.cumul_wait_blocked);
                        ss.modifyAttribute(ts, value, quark);
                    } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                        e.printStackTrace();
                    }
                } else {
                    // TIMER_WAIT_FOR_CPU
                    try {
                        quark = ss.getQuarkRelativeAndAdd(quarkNextTid, Attributes.TIMER_WAIT_FOR_CPU);

                        if (nextThreadInfo.cumul_wait_for_cpu == 0L) {
                            TmfStateValue value = TmfStateValue.newValueLong(0L);
                            ss.modifyAttribute(getTrace().getStartTime().toNanos(), value, quark);
                        }

                        nextThreadInfo.cumul_wait_for_cpu += ts - nextThreadInfo.last_ts;

                        TmfStateValue value = TmfStateValue.newValueLong(nextThreadInfo.cumul_wait_for_cpu);
                        ss.modifyAttribute(ts, value, quark);
                    } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                        e.printStackTrace();
                    }
                }
            }

            // TIMER_WAKEUP_LATENCY
            if (nextThreadInfo.last_wakeup != null) {
                try {
                    quark = ss.getQuarkRelativeAndAdd(quarkNextTid, Attributes.TIMER_WAKEUP_LATENCY);

                    if (nextThreadInfo.cumul_wakeup_latency == 0L) {
                        TmfStateValue value = TmfStateValue.newValueLong(0L);
                        ss.modifyAttribute(getTrace().getStartTime().toNanos(), value, quark);
                    }

                    nextThreadInfo.cumul_wakeup_latency += ts - nextThreadInfo.last_wakeup;

                    TmfStateValue value = TmfStateValue.newValueLong(nextThreadInfo.cumul_wakeup_latency);
                    ss.modifyAttribute(ts, value, quark);

                    // Reset last wakeup time to null, so that it will be used only next time we receive a sched_waking
                    nextThreadInfo.last_wakeup = null;
                } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }

            //////////////////////////////////////
            // PREVIOUS THREAD (BEING UNSCHEDULED)
            int quarkPrevTid = ss.getQuarkAbsoluteAndAdd(Attributes.TID, prevTid.toString());

            // PRIO: update the priority if needed
            if (prevThreadInfo.prio_last != prevPrio) {
                try {
                    quark = ss.getQuarkRelativeAndAdd(quarkPrevTid, Attributes.PRIO);
                    TmfStateValue value = TmfStateValue.newValueInt(prevPrio);
                    ss.modifyAttribute(ts, value, quark);
                    prevThreadInfo.prio_last = prevPrio;
                } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }

            // TIMER_CPU_USAGE
            // We chose here to consider the data from the trace, even if it's not the actual running duration
            //if (prevThreadInfo.last_ts != null) {
                try {
                    quark = ss.getQuarkRelativeAndAdd(quarkPrevTid, Attributes.TIMER_CPU_USAGE);

                    if (prevThreadInfo.cumul_cpu_usage == 0L) {
                        TmfStateValue value = TmfStateValue.newValueLong(0L);
                        ss.modifyAttribute(getTrace().getStartTime().toNanos(), value, quark);
                        //ss.modifyAttribute(prevThreadInfo.last_ts, value, quark);
                    }

                    long previousTs = (prevThreadInfo.last_ts != null)
                            ? prevThreadInfo.last_ts
                            : getTrace().getStartTime().toNanos();
                    prevThreadInfo.cumul_cpu_usage += ts - previousTs;

                    TmfStateValue value = TmfStateValue.newValueLong(prevThreadInfo.cumul_cpu_usage);
                    ss.modifyAttribute(ts, value, quark);
                } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            //}
            nextThreadInfo.last_ts = ts;

            // For wait for CPU and wait blocked for next time
            prevThreadInfo.last_ts = ts;
            prevThreadInfo.last_state = prevState;

            s = null;
            if (prevState == 0 || prevState == 1024) {
                // STATE: Change to PREEMPTED
                s = new BackendState(BackendStateValue.PREEMPTED.getValue());
                // COUNTER_PREEMPT: Increment
                try {
                    quark = ss.getQuarkRelativeAndAdd(quarkPrevTid, Attributes.COUNTER_PREEMPT);

                    if (prevThreadInfo.counter_preempt == 0) {
                        TmfStateValue value = TmfStateValue.newValueInt(0);
                        ss.modifyAttribute(getTrace().getStartTime().toNanos(), value, quark);
                    }

                    prevThreadInfo.counter_preempt++;

                    TmfStateValue value = TmfStateValue.newValueInt(prevThreadInfo.counter_preempt);
                    ss.modifyAttribute(ts, value, quark);
                } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            } else {
                // STATE: Change to BLOCKED
                s = new BackendState(BackendStateValue.BLOCKED.getValue());
            }

            prevThreadInfo.stack_state.push(s);
            try {
                quark = ss.getQuarkRelativeAndAdd(quarkPrevTid, Attributes.STATE);
                TmfStateValue value = TmfStateValue.newValueLong(s.getValue());
                ss.modifyAttribute(ts, value, quark);
            } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        } else if (eventName.equals(layout.eventSchedPiSetprio())) {
            ITmfEventField content = event.getContent();
            Long fromTid = (Long)content.getField(LAYOUT_CONTEXT_VTID).getValue();
            Long toTid = (Long)content.getField(layout.fieldTid()).getValue();
            Integer newPrio = ((Long)content.getField(layout.fieldNewPrio()).getValue()).intValue();

            // Get the threadInfo from the thread with raised priority
            ThreadInfo toThreadInfo = getThreadInfoOrCreate(toTid);

            // PRIO: update the priority if needed
            if (toThreadInfo.prio_last != newPrio) {
                try {
                    quark = ss.getQuarkAbsoluteAndAdd(Attributes.TID, toTid.toString(), Attributes.PRIO);
                    TmfStateValue value = TmfStateValue.newValueInt(newPrio);
                    ss.modifyAttribute(ts, value, quark);
                    toThreadInfo.prio_last = newPrio;
                } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }

            if (fromTid.equals(toTid)) {
                // Going back to normal
                // Then, for each thread that raised its priority, add to the cumulated time
                for (Long tid : toThreadInfo.sched_pi_fromTids) {
                    ThreadInfo fromThreadInfo = getThreadInfoOrCreate(tid);

                    boolean wasZero = (fromThreadInfo.sched_pi_cumul == 0);
                    fromThreadInfo.sched_pi_cumul += ts - fromThreadInfo.sched_pi_lastTs;
                    fromThreadInfo.sched_pi_lastTs = null;
                    //fromThreadInfo.sched_pi_toTid = null;

                    // Update SS
                    try {
                        quark = ss.getQuarkAbsoluteAndAdd(Attributes.TID, tid.toString());
                        quark = ss.getQuarkRelativeAndAdd(quark, Attributes.TIMER_SCHED_PI);

                        if (wasZero) {
                            TmfStateValue value = TmfStateValue.newValueLong(0);
                            ss.modifyAttribute(getTrace().getStartTime().toNanos(), value, quark);
                        }

                        TmfStateValue value = TmfStateValue.newValueLong(fromThreadInfo.sched_pi_cumul);
                        ss.modifyAttribute(ts, value, quark);
                    } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                        e.printStackTrace();
                    }
                }
                // Then clear the list of fromTids
                toThreadInfo.sched_pi_fromTids.clear();
            } else {
                // fromTid started giving its priority to toTid

                // Update the threadInfo for the fromTid
                ThreadInfo fromThreadInfo = getThreadInfoOrCreate(fromTid);
                fromThreadInfo.sched_pi_lastTs = ts;
                //fromThreadInfo.sched_pi_toTid = toTid;

                // Update the threadInfo for the toTid
                toThreadInfo.sched_pi_fromTids.add(fromTid);
            }
        } else if (eventName.equals(EVENT_SCHED_WAKING)) {
            // Get the TID we want to wake up
            ITmfEventField content = event.getContent();
            Long tid = (Long)content.getField(layout.fieldTid()).getValue();
            Integer prio = ((Long)content.getField(layout.fieldPrio()).getValue()).intValue();

            ThreadInfo threadInfo = getThreadInfoOrCreate(tid);

            threadInfo.last_wakeup = ts;

            // Thread quark
            int quarkTid = ss.getQuarkAbsoluteAndAdd(Attributes.TID, tid.toString());

            // PRIO: update the priority if needed
            if (threadInfo.prio_last != prio) {
                try {
                    quark = ss.getQuarkAbsoluteAndAdd(Attributes.TID, tid.toString(), Attributes.PRIO);
                    TmfStateValue value = TmfStateValue.newValueInt(prio);
                    ss.modifyAttribute(ts, value, quark);
                    threadInfo.prio_last = prio;
                } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }

            // STATE: Change to WAKING
            BackendState s = new BackendState(BackendStateValue.WAKING.getValue());
            if (!threadInfo.stack_state.isEmpty()) {
                // If not empty, the last status should either be PREEMPTED or BLOCKED,
                // we don't need any of them anymore in the stack as they are over so...
                threadInfo.stack_state.pop();
            }
            threadInfo.stack_state.push(s);
            try {
                quark = ss.getQuarkRelativeAndAdd(quarkTid, Attributes.STATE);
                TmfStateValue value = TmfStateValue.newValueLong(s.getValue());
                ss.modifyAttribute(ts, value, quark);
            } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        } else if (eventName.equals(EVENT_POWER_CPU_FREQUENCY)) {
            ITmfEventField content = event.getContent();
            Long cpu = (Long)content.getField(LAYOUT_CPU_ID).getValue();
            Long freq = (Long)content.getField(LAYOUT_STATE).getValue();

            try {
                quark = ss.getQuarkAbsoluteAndAdd(Attributes.CPU_FREQ, cpu.toString());
                TmfStateValue value = TmfStateValue.newValueLong(freq);
                ss.modifyAttribute(ts, value, quark);
            } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        } else if (eventName.equals(layout.eventSoftIrqEntry()) || eventName.equals(EVENT_IRQ_SOFTIRQ_ENTRY)) {
            ITmfEventField content = event.getContent();
            Long tid = (Long)content.getField(LAYOUT_CONTEXT_VTID).getValue();
            Long vec = (Long)content.getField("vec").getValue(); //$NON-NLS-1$

            // Thread quark
            int quarkTid = ss.getQuarkAbsoluteAndAdd(Attributes.TID, tid.toString());

            // Thread info
            ThreadInfo threadInfo = getThreadInfoOrCreate(tid);

            // STATE: Change to the right SOFTIRQ
            BackendState s = new BackendState(BackendState.TYPE_SOFTIRQ, vec);
            threadInfo.stack_state.push(s);
            try {
                quark = ss.getQuarkRelativeAndAdd(quarkTid, Attributes.STATE);
                TmfStateValue value = TmfStateValue.newValueLong(s.getValue());
                ss.modifyAttribute(ts, value, quark);
            } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        } else if (eventName.equals(layout.eventHRTimerExpireEntry()) || eventName.equals("timer_hrtimer_expire_entry")) { //$NON-NLS-1$
            ITmfEventField content = event.getContent();
            Long tid = (Long)content.getField(LAYOUT_CONTEXT_VTID).getValue();

            // Thread quark
            int quarkTid = ss.getQuarkAbsoluteAndAdd(Attributes.TID, tid.toString());

            // Thread info
            ThreadInfo threadInfo = getThreadInfoOrCreate(tid);

            // STATE: Change to HRTIMER
            BackendState s = new BackendState(BackendStateValue.HRTIMER.getValue());
            threadInfo.stack_state.push(s);
            try {
                quark = ss.getQuarkRelativeAndAdd(quarkTid, Attributes.STATE);
                TmfStateValue value = TmfStateValue.newValueLong(s.getValue());
                ss.modifyAttribute(ts, value, quark);
            } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        } else if (eventName.equals(layout.eventIrqHandlerEntry())) {
            ITmfEventField content = event.getContent();
            Long tid = (Long)content.getField(LAYOUT_CONTEXT_VTID).getValue();
            Long irq = (Long)content.getField("irq").getValue(); //$NON-NLS-1$

            // Thread quark
            int quarkTid = ss.getQuarkAbsoluteAndAdd(Attributes.TID, tid.toString());

            // Thread info
            ThreadInfo threadInfo = getThreadInfoOrCreate(tid);

            // STATE: Change to the right IRQ
            BackendState s = new BackendState(BackendState.TYPE_IRQ, irq);
            threadInfo.stack_state.push(s);
            try {
                quark = ss.getQuarkRelativeAndAdd(quarkTid, Attributes.STATE);
                TmfStateValue value = TmfStateValue.newValueLong(s.getValue());
                ss.modifyAttribute(ts, value, quark);
            } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            // TODO: démarrer le timer de IRQ preemption, qui sera stoppé lors du irq handler exit
            // TODO: augmenter le compteur de préemptions ? (probablement!)
            // TODO: stopper le timer de CPU Usage et démarrer le timer de preemption ?
            // TODO: mettre à jour l'attribute state pour "IRQ_PREEMPTED" ?
        } else if (eventName.startsWith(layout.eventSyscallEntryPrefix())) {
            ITmfEventField content = event.getContent();
            Long tid = (Long)content.getField(LAYOUT_CONTEXT_VTID).getValue();

            /*
            Integer cpu = checkNotNull((Integer)TmfTraceUtils.resolveEventAspectOfClassForEvent(getTrace(), TmfCpuAspect.class, event));
            Long tid = threadByCPU.get(cpu);

            if (tid == null) {
                KernelAnalysisModule kernelAnalysisModule = (KernelAnalysisModule)TmfTraceUtils.getAnalysisModuleOfClass(getTrace(), TmfStateSystemAnalysisModule.class, KernelAnalysisModule.ID);
                if (kernelAnalysisModule != null) {
                    Integer eventTid = KernelThreadInformationProvider.getThreadOnCpu(kernelAnalysisModule, cpu, ts);
                    if (eventTid != null) {
                        tid = new Long(eventTid);
                        threadByCPU.put(cpu, tid);
                    }
                }
            }
            */

            if (tid != null) {
                ThreadInfo threadInfo = getThreadInfoOrCreate(tid);
                threadInfo.counter_syscalls += 1;

                // Thread quark
                int quarkTid = ss.getQuarkAbsoluteAndAdd(Attributes.TID, tid.toString());

                // COUNTER_SYSCALLS
                quark = ss.getQuarkRelativeAndAdd(quarkTid, Attributes.COUNTER_SYSCALLS);

                if (threadInfo.counter_syscalls == 1) {
                    try {
                        TmfStateValue value = TmfStateValue.newValueInt(0);
                        ss.modifyAttribute(getTrace().getStartTime().toNanos(), value, quark);
                    } catch (StateValueTypeException | IndexOutOfBoundsException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    TmfStateValue value = TmfStateValue.newValueInt(threadInfo.counter_syscalls);
                    ss.modifyAttribute(ts, value, quark);
                } catch (StateValueTypeException | IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }

                // STATE: Change to SYSCALL
                BackendState s = new BackendState(BackendStateValue.SYSCALL.getValue());
                threadInfo.stack_state.push(s);
                try {
                    quark = ss.getQuarkRelativeAndAdd(quarkTid, Attributes.STATE);
                    TmfStateValue value = TmfStateValue.newValueLong(s.getValue());
                    ss.modifyAttribute(ts, value, quark);
                } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }
        } else if (eventName.equals(layout.eventSoftIrqExit()) || eventName.equals("irq_softirq_exit") //$NON-NLS-1$
                || eventName.equals(layout.eventHRTimerExpireExit()) || eventName.equals("timer_hrtimer_expire_exit") //$NON-NLS-1$
                || eventName.equals(layout.eventIrqHandlerExit())
                || eventName.startsWith(layout.eventSyscallExitPrefix())) {
            ITmfEventField content = event.getContent();
            Long tid = (Long)content.getField(LAYOUT_CONTEXT_VTID).getValue();

            // Thread quark
            int quarkTid = ss.getQuarkAbsoluteAndAdd(Attributes.TID, tid.toString());

            // Thread info
            ThreadInfo threadInfo = getThreadInfoOrCreate(tid);

            // STATE: Change to the last state value
            BackendState s = null;
            if (!threadInfo.stack_state.isEmpty()) {
                threadInfo.stack_state.pop();
                s = threadInfo.stack_state.peek();
            }
            if (s == null) {
                s = new BackendState(BackendStateValue.RUNNING.getValue());
            }
            try {
                quark = ss.getQuarkRelativeAndAdd(quarkTid, Attributes.STATE);
                TmfStateValue value = TmfStateValue.newValueLong(s.getValue());
                ss.modifyAttribute(ts, value, quark);
            } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        //} else if (eventName.equals(layout.eventIrqHandlerExit())) {
            // TODO: stopper le timer de IRQ preemption
            // TODO: stopper le timer de preemption et redémarrer le timer de CPU Usage ?
        }
    }

    /**
     * @since 2.0
     */
    @Override
    public void done() {
        int quark;
        TmfStateValue value;
        ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());
        long traceStartTime = getTrace().getStartTime().toNanos();
        long traceEndTime = getTrace().getEndTime().toNanos();

        // Update all the timers if necessary
        for (@NonNull Entry<Long, ThreadInfo> entry : info_by_tid.entrySet()) {
            Long tid = entry.getKey();
            ThreadInfo threadInfo = entry.getValue();

            int quarkTid = ss.getQuarkAbsoluteAndAdd(Attributes.TID, tid.toString());

            // TIMER_WAKEUP_LATENCY
            if (threadInfo.last_wakeup != null) {
                try {
                    quark = ss.getQuarkRelativeAndAdd(quarkTid, Attributes.TIMER_WAKEUP_LATENCY);

                    if (threadInfo.cumul_wakeup_latency == 0L) {
                        value = TmfStateValue.newValueLong(0L);
                        ss.modifyAttribute(traceStartTime, value, quark);
                    }

                    threadInfo.cumul_wakeup_latency += traceEndTime - threadInfo.last_wakeup;

                    value = TmfStateValue.newValueLong(threadInfo.cumul_wakeup_latency);
                    ss.modifyAttribute(traceEndTime, value, quark);

                    // Reset last wakeup time to null, so that it will be used only next time we receive a sched_waking
                    threadInfo.last_wakeup = null;
                } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }

            BackendState state = threadInfo.stack_state.peek();
            BackendStateValue stateValue = (state == null) ? null : BackendStateValue.getValue(state.getValue());

            if (stateValue == BackendStateValue.BLOCKED) {
                // TIMER_WAIT_BLOCKED
                try {
                    quark = ss.getQuarkRelativeAndAdd(quarkTid, Attributes.TIMER_WAIT_BLOCKED);

                    if (threadInfo.cumul_wait_blocked == 0L) {
                        value = TmfStateValue.newValueLong(0L);
                        ss.modifyAttribute(traceStartTime, value, quark);
                    }

                    threadInfo.cumul_wait_blocked += traceEndTime - threadInfo.last_ts;

                    value = TmfStateValue.newValueLong(threadInfo.cumul_wait_blocked);
                    ss.modifyAttribute(traceEndTime, value, quark);
                } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            } else if (stateValue == BackendStateValue.PREEMPTED) {
                // TIMER_WAIT_FOR_CPU
                try {
                    quark = ss.getQuarkRelativeAndAdd(quarkTid, Attributes.TIMER_WAIT_FOR_CPU);

                    if (threadInfo.cumul_wait_for_cpu == 0L) {
                        value = TmfStateValue.newValueLong(0L);
                        ss.modifyAttribute(traceStartTime, value, quark);
                    }

                    threadInfo.cumul_wait_for_cpu += traceEndTime - threadInfo.last_ts;

                    value = TmfStateValue.newValueLong(threadInfo.cumul_wait_for_cpu);
                    ss.modifyAttribute(traceEndTime, value, quark);
                } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            } else {
                // TIMER_CPU_USAGE
                try {
                    quark = ss.getQuarkRelativeAndAdd(quarkTid, Attributes.TIMER_CPU_USAGE);

                    if (threadInfo.cumul_cpu_usage == 0L) {
                        value = TmfStateValue.newValueLong(0L);
                        ss.modifyAttribute(traceStartTime, value, quark);
                    }

                    long previousTs = (threadInfo.last_ts != null)
                            ? threadInfo.last_ts
                            : traceStartTime;
                    threadInfo.cumul_cpu_usage += traceEndTime - previousTs;

                    value = TmfStateValue.newValueLong(threadInfo.cumul_cpu_usage);
                    ss.modifyAttribute(traceEndTime, value, quark);
                } catch (StateValueTypeException | TimeRangeException | IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }
            threadInfo.last_ts = traceEndTime;
        }
        info_by_tid.clear();
    }
}