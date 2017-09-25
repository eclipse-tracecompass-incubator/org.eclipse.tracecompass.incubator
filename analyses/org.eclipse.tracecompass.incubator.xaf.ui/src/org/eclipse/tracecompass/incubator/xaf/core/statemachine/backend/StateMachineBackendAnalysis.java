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

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.incubator.internal.xaf.ui.statemachine.StateMachineUtils.TimestampInterval;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * TODO: StateMachineBackendAnalysis improvements list
 *  - wake_up latency (sched_waking -> sched_switch or sched_wakeup -> sched_switch for kernels < 4.3 but less precision)
 *  - preemptions (DONE)
 *  - IRQ/NMI preemption (irq_handler_entry / irq_handler_exit)
 *  - syscalls latency (syscall_entry / syscall_exit)
 *  - Add a "State" entry for the SS, that allows following what is the state of a process at a given time
 *
 * @author Raphaël Beamonte
 */
public class StateMachineBackendAnalysis extends TmfStateSystemAnalysisModule {

    /**
     * The ID of this analysis module (which is also the ID of the state system)
     */
    public static final @NonNull String ID = "org.eclipse.tracecompass.xaf.core.statemachine.backend"; //$NON-NLS-1$

    /**
     * The name of this analysis module
     */
    protected static final @NonNull String NAME = "State Machine Backend State System"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public StateMachineBackendAnalysis() {
        super();
        setId(ID);
        setName(NAME);
    }

    @Override
    protected ITmfStateProvider createStateProvider() {
        return new StateMachineProviderEventTypes(checkNotNull(getTrace()));
    }

    @Override
    protected String getSsFileName() {
        return "statemachine.ht"; //$NON-NLS-1$
    }

    /**
     * Allows to get the value of a timer for a thread at a
     * given timestamp
     *
     * @param tid The thread ID
     * @param ts The timestamp
     * @param timer The name of the timer (from {@link Attributes}
     * @return The value of the timer
     */
    public Long getTimer(long tid, long ts, String timer) {
        ITmfStateSystem ss = checkNotNull(getStateSystem());
        int quark;
        Long value;
        ITmfStateInterval intvl;

        long timestamp = ts;
        if (getTrace() != null && ts > checkNotNull(getTrace()).getEndTime().toNanos()) {
            timestamp = checkNotNull(getTrace()).getEndTime().toNanos();
        }

        try {
            quark = ss.getQuarkAbsolute(Attributes.TID, Long.toString(tid), timer);

            intvl = ss.querySingleState(timestamp, quark);
            value = intvl.getStateValue().unboxLong();
        } catch (AttributeNotFoundException|TimeRangeException e) {
            // If we end up here, it's that we don't have any run, we
            // can thus return 0 !
            return 0L;
        } catch (StateSystemDisposedException e) {
            e.printStackTrace();
            return null;
        }

        ITmfStateInterval intvlend;
        try {
            intvlend = ss.querySingleState(intvl.getEndTime()+1, quark);
        } catch (IndexOutOfBoundsException|TimeRangeException e) {
            // If we end up here, it's that we don't have any next run
            // we thus can return value
            return value;
        } catch (StateSystemDisposedException e) {
            e.printStackTrace();
            return null;
        }
        Long value2 = intvlend.getStateValue().unboxLong();
        long nextRunStart = intvlend.getStartTime()-(value2-value);

        if (nextRunStart > timestamp) {
            return value;
        }

        return value + (ts-nextRunStart);
    }

    /**
     * Allows to get the value evolution of a timer for a thread
     * during a given interval
     *
     * @param tid The thread ID
     * @param start The timestamp of the start of the interval
     * @param end The timestamp of the end of the interval
     * @param timer The name of the timer (from {@link Attributes}
     * @return The value difference of the timer for the interval
     */
    public Long getTimerIntvl(long tid, long start, long end, String timer) {
        Long valueStart = getTimer(tid, start, timer);
        Long valueEnd = getTimer(tid, end, timer);

        return valueEnd - valueStart;
    }

    /**
     * Allows to get the value of a WAIT_BLOCKED timer for a
     * thread at a given timestamp
     *
     * @param tid The thread ID
     * @param ts The timestamp
     * @return The value of the timer
     */
    public Long getWaitBlocked(long tid, long ts) {
        return getTimer(tid, ts, Attributes.TIMER_WAIT_BLOCKED);
    }

    /**
     * Allows to get the value evolution of a WAIT_BLOCKED timer
     * for a thread during a given interval
     *
     * @param tid The thread ID
     * @param start The timestamp of the start of the interval
     * @param end The timestamp of the end of the interval
     * @return The value difference of the timer for the interval
     */
    public Long getWaitBlockedIntvl(long tid, long start, long end) {
        return getTimerIntvl(tid, start, end, Attributes.TIMER_WAIT_BLOCKED);
    }

    /**
     * Allows to get the value of a WAIT_FOR_CPU timer for a
     * thread at a given timestamp
     *
     * @param tid The thread ID
     * @param ts The timestamp
     * @return The value of the timer
     */
    public Long getWaitForCPU(long tid, long ts) {
        return getTimer(tid, ts, Attributes.TIMER_WAIT_FOR_CPU);
    }

    /**
     * Allows to get the value evolution of a WAIT_FOR_CPU timer
     * for a thread during a given interval
     *
     * @param tid The thread ID
     * @param start The timestamp of the start of the interval
     * @param end The timestamp of the end of the interval
     * @return The value difference of the timer for the interval
     */
    public Long getWaitForCPUIntvl(long tid, long start, long end) {
        return getTimerIntvl(tid, start, end, Attributes.TIMER_WAIT_FOR_CPU);
    }

    /**
     * Allows to get the value of a CPU_USAGE timer for a
     * thread at a given timestamp
     *
     * @param tid The thread ID
     * @param ts The timestamp
     * @return The value of the timer
     */
    public Long getCpuUsage(long tid, long ts) {
        return getTimer(tid, ts, Attributes.TIMER_CPU_USAGE);
    }

    /**
     * Allows to get the value evolution of a CPU_USAGE timer
     * for a thread during a given interval
     *
     * @param tid The thread ID
     * @param start The timestamp of the start of the interval
     * @param end The timestamp of the end of the interval
     * @return The value difference of the timer for the interval
     */
    public Long getCpuUsageIntvl(long tid, long start, long end) {
        return getTimerIntvl(tid, start, end, Attributes.TIMER_CPU_USAGE);
    }

    /**
     * Allows to get the value of a SCHED_PI timer for a
     * thread at a given timestamp
     *
     * @param tid The thread ID
     * @param ts The timestamp
     * @return The value of the timer
     */
    public Long getSchedPi(long tid, long ts) {
        return getTimer(tid, ts, Attributes.TIMER_SCHED_PI);
    }

    /**
     * Allows to get the value evolution of a SCHED_PI timer
     * for a thread during a given interval
     *
     * @param tid The thread ID
     * @param start The timestamp of the start of the interval
     * @param end The timestamp of the end of the interval
     * @return The value difference of the timer for the interval
     */
    public Long getSchedPiIntvl(long tid, long start, long end) {
        return getTimerIntvl(tid, start, end, Attributes.TIMER_SCHED_PI);
    }

    /**
     * Allows to get the stateInterval of an attribute for a thread at a
     * given timestamp
     *
     * @param tid The thread ID
     * @param ts The timestamp
     * @param attribute The name of the attribute (from {@link Attributes}
     * @return The state interval of the attribute
     */
    public ITmfStateInterval getStateInterval(long tid, long ts, String attribute) {
        ITmfStateSystem ss = checkNotNull(getStateSystem());
        int quark;
        ITmfStateInterval intvl;

        try {
            quark = ss.getQuarkAbsolute(Attributes.TID, Long.toString(tid), attribute);

            intvl = ss.querySingleState(ts, quark);

            return intvl;
        } catch (AttributeNotFoundException e) {
            return null;
        } catch (StateSystemDisposedException|TimeRangeException e) {
            //e.printStackTrace();
            return null;
        } catch (RuntimeException e) { // For debugging purposes...
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Allows to get all the stateIntervals of an attribute for a thread
     * between two given timestamps
     *
     * @param tid The thread ID
     * @param start The start timestamp
     * @param end The end timestamp
     * @param attribute The name of the attribute (from {@link Attributes}
     * @return The list of state intervals of the attribute
     * @throws TimeRangeException If timestamp are out of range
     */
    public List<ITmfStateInterval> getAllStateIntervalInPeriod(long tid, long start, long end, String attribute) throws TimeRangeException {
        return getAllStateIntervalInPeriod(start, end, Attributes.TID, Long.toString(tid), attribute);
    }

    /**
     * Allows to get all the stateIntervals of an attribute for a thread
     * between two given timestamps
     *
     * @param start The start timestamp
     * @param end The end timestamp
     * @param attributePath The path of the attribute (from {@link Attributes}
     * @return The list of state intervals of the attribute
     * @throws TimeRangeException If timestamp are out of range
     */
    public List<ITmfStateInterval> getAllStateIntervalInPeriod(long start, long end, String... attributePath) throws TimeRangeException {
        //List<ITmfStateInterval> stateIntervalList = new LinkedList<>();

        ITmfStateSystem ss = checkNotNull(getStateSystem());
        int quark;
        //ITmfStateInterval intvl;

        try {
            quark = ss.getQuarkAbsolute(attributePath);
            return StateSystemUtils.queryHistoryRange(ss, quark, Math.max(ss.getStartTime(), start), Math.min(ss.getCurrentEndTime(), end));
            //intvl = ss.querySingleState(start, quark);
        } catch (AttributeNotFoundException|TimeRangeException e) {
            return new LinkedList<>();
            //return stateIntervalList;
        } catch (StateSystemDisposedException e) {
            e.printStackTrace();
            return new LinkedList<>();
            //return stateIntervalList;
        } catch (RuntimeException e) { // For debugging purposes...
            e.printStackTrace();
            return new LinkedList<>();
            //return stateIntervalList;
        }

        /*while (intvl != null && intvl.getStartTime() < end) {
            stateIntervalList.add(intvl);

            try {
                intvl = ss.querySingleState(intvl.getEndTime() + 1, quark);
            } catch (AttributeNotFoundException|TimeRangeException e) {
                intvl = null;
            } catch (StateSystemDisposedException e) {
                e.printStackTrace();
                intvl = null;
            } catch (RuntimeException e) { // For debugging purposes...
                e.printStackTrace();
                intvl = null;
            }
        }

        return stateIntervalList;*/
    }

    /**
     * @param tiCollection The collection of time intervals in which to search
     * @param quarkPath The path to the quark
     * @return a list of all the state interval which have a value in at least one of the given time interval
     */
    public List<ITmfStateInterval> getAllKernelStateIntervalInPeriods(Collection<TimestampInterval> tiCollection, String... quarkPath) {
        List<ITmfStateInterval> stateIntervalList = new LinkedList<>();
        TimestampInterval largerInterval = TimestampInterval.maxTsInterval(tiCollection);

        if (getKernelTrace().getStartTime().compareTo(largerInterval.getEndTime()) > 0 || getKernelTrace().getEndTime().compareTo(largerInterval.getStartTime()) < 0) {
            return stateIntervalList;
        }

        @SuppressWarnings("null")
        KernelAnalysisModule kernelAnalysisModule = TmfTraceUtils.getAnalysisModuleOfClass(getKernelTrace(), KernelAnalysisModule.class, KernelAnalysisModule.ID);
        if (kernelAnalysisModule == null) {
            return stateIntervalList;
        }

        // Just in case it wasn't finished yet
        kernelAnalysisModule.waitForCompletion();

        ITmfStateSystem ss = kernelAnalysisModule.getStateSystem();
        if (ss == null) {
            return stateIntervalList;
        }

        try {
            Integer attributeQuark = ss.getQuarkAbsolute(quarkPath);

            for (TimestampInterval ti : tiCollection) {
                stateIntervalList.addAll(StateSystemUtils.queryHistoryRange(ss, attributeQuark, ti.getStartTime().getValue(), ti.getEndTime().getValue()));
            }
        } catch (AttributeNotFoundException | StateSystemDisposedException | TimeRangeException e) {
            e.printStackTrace();
        }

        stateIntervalList.sort(new Comparator<ITmfStateInterval>() {
            @Override
            public int compare(ITmfStateInterval itsi1, ITmfStateInterval itsi2) {
                int cmp = Long.valueOf(itsi1.getStartTime()).compareTo(itsi2.getStartTime());
                if (cmp == 0) {
                    cmp = Long.valueOf(itsi1.getEndTime()).compareTo(itsi2.getEndTime());
                }
                return cmp;
            }

        });
        return stateIntervalList;
    }

    /**
     * @param tiCollection The collection of time intervals in which to search
     * @param quarkPath The path to the quark
     * @return a unique set of all the state value which have a value in at least one of the given time interval
     */
    public Collection<ITmfStateValue> getAllKernelStateValueInPeriods(Collection<TimestampInterval> tiCollection, String... quarkPath) {
        Collection<ITmfStateValue> stateValueCollection = new TreeSet<>();

        for (ITmfStateInterval itsi : getAllKernelStateIntervalInPeriods(tiCollection, quarkPath)) {
            ITmfStateValue itsv = itsi.getStateValue();
            if (!itsv.isNull()) {
                stateValueCollection.add(itsv);
            }
        }

        return stateValueCollection;
    }

    /**
     * Allows to get the value of a counter for a thread at a
     * given timestamp
     *
     * @param tid The thread ID
     * @param ts The timestamp
     * @param counter The name of the counter (from {@link Attributes}
     * @return The value of the counter
     */
    public Integer getCounter(long tid, long ts, String counter) {
        /*
        ITmfStateSystem ss = checkNotNull(getStateSystem());
        int quark, value;
        ITmfStateInterval intvl;

        try {
            quark = ss.getQuarkAbsolute(Attributes.TID, Long.toString(tid), counter);

            intvl = ss.querySingleState(ts, quark);
            value = intvl.getStateValue().unboxInt();

            return value;
        } catch (AttributeNotFoundException e) {
            return 0;
        } catch (StateSystemDisposedException e) {
            e.printStackTrace();
            return null;
        } catch (RuntimeException e) { // For debugging purposes...
            e.printStackTrace();
            return null;
        }
        */

        long timestamp = ts;
        if (getTrace() != null && ts > checkNotNull(getTrace()).getEndTime().toNanos()) {
            timestamp = checkNotNull(getTrace()).getEndTime().toNanos();
        }

        ITmfStateInterval intvl = getStateInterval(tid, timestamp, counter);
        if (intvl == null) {
            return 0;
        }

        int value = intvl.getStateValue().unboxInt();

        return value;
    }

    /**
     * Allows to get the value evolution of a counter for a thread
     * during a given interval
     *
     * @param tid The thread ID
     * @param start The timestamp of the start of the interval
     * @param end The timestamp of the end of the interval
     * @param counter The name of the counter (from {@link Attributes}
     * @return The value difference of the counter for the interval
     */
    public Integer getCounterIntvl(long tid, long start, long end, String counter) {
        Integer valueStart = getCounter(tid, start, counter);
        Integer valueEnd = getCounter(tid, end, counter);

        return valueEnd - valueStart;
    }

    /**
     * Allows to get the value of a SYSCALLS counter for a
     * thread at a given timestamp
     *
     * @param tid The thread ID
     * @param ts The timestamp
     * @return The value of the counter
     */
    public Integer getSyscalls(long tid, long ts) {
        return getCounter(tid, ts, Attributes.COUNTER_SYSCALLS);
    }

    /**
     * Allows to get the value evolution of a SYSCALLS counter
     * for a thread during a given interval
     *
     * @param tid The thread ID
     * @param start The timestamp of the start of the interval
     * @param end The timestamp of the end of the interval
     * @return The value difference of the counter for the interval
     */
    public Integer getSyscallsIntvl(long tid, long start, long end) {
        return getCounterIntvl(tid, start, end, Attributes.COUNTER_SYSCALLS);
    }

    /**
     * Allows to get the value of a PREEMPT counter for a
     * thread at a given timestamp
     *
     * @param tid The thread ID
     * @param ts The timestamp
     * @return The value of the counter
     */
    public Integer getPreempt(long tid, long ts) {
        return getCounter(tid, ts, Attributes.COUNTER_PREEMPT);
    }

    /**
     * Allows to get the value evolution of a PREEMPT counter
     * for a thread during a given interval
     *
     * @param tid The thread ID
     * @param start The timestamp of the start of the interval
     * @param end The timestamp of the end of the interval
     * @return The value difference of the counter for the interval
     */
    public Integer getPreemptIntvl(long tid, long start, long end) {
        return getCounterIntvl(tid, start, end, Attributes.COUNTER_PREEMPT);
    }

    /**
     * To get the kernel trace from the module
     * @return The kernel trace on which this module applies
     */
    public ITmfTrace getKernelTrace() {
        return getTrace();
    }

}
