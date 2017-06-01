/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.handlers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.data.Attributes;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.module.StateValues;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * @author Cédric Biancheri
 */
public class FusedVMEventHandlerUtils {

    private FusedVMEventHandlerUtils() {
    }

    private static int getNodeCPUs(ITmfStateSystemBuilder ssb) {
        return ssb.getQuarkAbsoluteAndAdd(Attributes.CPUS);
    }

    private static int getNodeThreads(ITmfStateSystemBuilder ssb, String machineName) {
        return ssb.getQuarkAbsoluteAndAdd(Attributes.THREADS, machineName);
    }

    /**
     * Get the node Machines
     *
     * @param ssb
     *            the state system
     * @return the quark
     */
    public static int getNodeMachines(ITmfStateSystemBuilder ssb) {
        return ssb.getQuarkAbsoluteAndAdd(Attributes.MACHINES);
    }

    /**
     * Return the cpu quark
     *
     * @param cpuNumber
     *            number of the cpu
     * @param ss
     *            the state system
     * @return the quark
     */
    public static int getCurrentCPUNode(Integer cpuNumber, ITmfStateSystemBuilder ss) {
        return ss.getQuarkRelativeAndAdd(getNodeCPUs(ss), cpuNumber.toString());
    }

    /**
     * Get quark to current thread of a cpu
     *
     * @param cpuNumber
     *            number of the cpu
     * @param ss
     *            the state system
     * @return the quark
     */
    public static int getCurrentThreadNode(Integer cpuNumber, ITmfStateSystemBuilder ss) {
        /*
         * Shortcut for the "current thread" attribute node. It requires
         * querying the current CPU's current thread.
         */
        int quark = ss.getQuarkRelativeAndAdd(getCurrentCPUNode(cpuNumber, ss), Attributes.CURRENT_THREAD);
        ITmfStateValue value = ss.queryOngoingState(quark);
        int thread = value.isNull() ? -1 : value.unboxInt();
        quark = ss.getQuarkRelativeAndAdd(getCurrentCPUNode(cpuNumber, ss), Attributes.MACHINE_NAME);
        value = ss.queryOngoingState(quark);
        String machineName = value.unboxStr();
        return ss.getQuarkRelativeAndAdd(getNodeThreads(ss, machineName), buildThreadAttributeName(thread, cpuNumber));
    }

    /**
     * Build the thread attribute name.
     *
     * For all threads except "0" this is the string representation of the threadId.
     * For thread "0" which is the idle thread and can be running concurrently on multiple
     * CPUs, append "_cpuId".
     *
     * @param threadId
     *              the thread id
     * @param cpuId
     *              the cpu id
     *
     * @return the thread attribute name
     *         null if the threadId is zero and the cpuId is null
     */
    public static @Nullable String buildThreadAttributeName(int threadId, @Nullable Integer cpuId) {

        if (threadId == 0) {
            if (cpuId == null) {
                return null;
            }
            return Attributes.THREAD_0_PREFIX + String.valueOf(cpuId);
        }

        return String.valueOf(threadId);
    }

    /**
     * Get the IRQs node
     *
     * @param cpuNumber
     *            the cpu core
     * @param ss
     *            the state system
     * @return the IRQ node quark
     */
    public static int getNodeIRQs(int cpuNumber, ITmfStateSystemBuilder ss) {
        return ss.getQuarkAbsoluteAndAdd(Attributes.CPUS, Integer.toString(cpuNumber), Attributes.IRQS);
    }

    /**
     * Get the timestamp of the event
     *
     * @param event
     *            the event containing the timestamp
     *
     * @return the timestamp in long format
     */
    public static long getTimestamp(ITmfEvent event) {
        return event.getTimestamp().toNanos();
    }

    /**
     * Get CPU
     *
     * @param event
     *            The event containing the cpu
     *
     * @return the CPU number (null for not set)
     */
    public static @Nullable Integer getCpu(ITmfEvent event) {
        Integer cpuObj = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);
        if (cpuObj == null) {
            /* We couldn't find any CPU information, ignore this event */
            return null;
        }
        return cpuObj;
    }

    /**
     * When we want to set a process back to a "running" state, first check its
     * current System_call attribute. If there is a system call active, we put
     * the process back in the syscall state. If not, we put it back in user
     * mode state.
     *
     * @param timestamp
     *            the time in the state system of the change
     * @param currentThreadNode
     *            The current thread node
     * @param ssb
     *            the state system
     * @throws TimeRangeException
     *             the time is out of range
     * @throws StateValueTypeException
     *             the attribute was not set with int values
     */
    public static void setProcessToRunning(long timestamp, int currentThreadNode, ITmfStateSystemBuilder ssb)
            throws TimeRangeException, StateValueTypeException {
        int quark;
        ITmfStateValue value;

        quark = ssb.getQuarkRelativeAndAdd(currentThreadNode, Attributes.SYSTEM_CALL);
        if (ssb.queryOngoingState(quark).isNull()) {
            /* We were in user mode before the interruption */
            value = StateValues.PROCESS_STATUS_RUN_USERMODE_VALUE;
        } else {
            /* We were previously in kernel mode */
            value = StateValues.PROCESS_STATUS_RUN_SYSCALL_VALUE;
        }
        quark = ssb.getQuarkRelativeAndAdd(currentThreadNode, Attributes.STATUS);
        ssb.modifyAttribute(timestamp, value, quark);
    }

    /**
     * Reset the CPU's status when it's coming out of an interruption.
     *
     * @param timestamp
     *            the time when the status of the cpu is "leaving irq"
     * @param cpuNumber
     *            the cpu returning to its previous state
     *
     * @param ssb
     *            State system
     * @throws StateValueTypeException
     *             the attribute is not set as an int
     * @throws TimeRangeException
     *             the time is out of range
     */
    public static void cpuExitInterrupt(long timestamp, Integer cpuNumber, ITmfStateSystemBuilder ssb)
            throws StateValueTypeException, TimeRangeException {
        int quark;
        int currentCPUNode = getCurrentCPUNode(cpuNumber, ssb);

        quark = ssb.getQuarkRelativeAndAdd(currentCPUNode, Attributes.STATUS);
        ITmfStateValue value = getCpuStatus(ssb, currentCPUNode);
        ssb.modifyAttribute(timestamp, value, quark);
    }

    /**
     * Get the ongoing Status state of a CPU.
     *
     * This will look through the states of the
     *
     * <ul>
     * <li>IRQ</li>
     * <li>Soft IRQ</li>
     * <li>Process</li>
     * </ul>
     *
     * under the CPU, giving priority to states higher in the list. If the state
     * is a null value, we continue looking down the list.
     *
     * @param ssb
     *            The state system
     * @param cpuQuark
     *            The *quark* of the CPU we are looking for. Careful, this is
     *            NOT the CPU number (or attribute name)!
     * @return The state value that represents the status of the given CPU
     */
    private static ITmfStateValue getCpuStatus(ITmfStateSystemBuilder ssb, int cpuQuark) {

        /* Check if there is a IRQ running */
        int irqQuarks = ssb.getQuarkRelativeAndAdd(cpuQuark, Attributes.IRQS);
        List<Integer> irqs = ssb.getSubAttributes(irqQuarks, false);
        for (Integer quark : irqs) {
            final ITmfStateValue irqState = ssb.queryOngoingState(quark.intValue());
            if (!irqState.isNull()) {
                return irqState;
            }
        }

        /* Check if there is a soft IRQ running */
        int softIrqQuarks = ssb.getQuarkRelativeAndAdd(cpuQuark, Attributes.SOFT_IRQS);
        List<Integer> softIrqs = ssb.getSubAttributes(softIrqQuarks, false);
        for (Integer quark : softIrqs) {
            final ITmfStateValue softIrqState = ssb.queryOngoingState(quark.intValue());
            if (!softIrqState.isNull()) {
                return softIrqState;
            }
        }

        /*
         * Check if there is a thread running. If not, report IDLE. If there is,
         * report the running state of the thread (usermode or system call).
         */
        int currentThreadQuark = ssb.getQuarkRelativeAndAdd(cpuQuark, Attributes.CURRENT_THREAD);
        ITmfStateValue currentThreadState = ssb.queryOngoingState(currentThreadQuark);
        if (currentThreadState.isNull()) {
            return TmfStateValue.nullValue();
        }
        int tid = currentThreadState.unboxInt();
        if (tid == 0) {
            return StateValues.CPU_STATUS_IDLE_VALUE;
        }
        int currentMachineQuark = ssb.getQuarkRelativeAndAdd(cpuQuark, Attributes.MACHINE_NAME);
        String machineName = ssb.queryOngoingState(currentMachineQuark).unboxStr();
        int threadSystemCallQuark = ssb.getQuarkRelativeAndAdd(getNodeThreads(ssb, machineName), Integer.toString(tid), Attributes.SYSTEM_CALL);
        return (ssb.queryOngoingState(threadSystemCallQuark).isNull() ?
                StateValues.CPU_STATUS_RUN_USERMODE_VALUE :
                StateValues.CPU_STATUS_RUN_SYSCALL_VALUE);
    }

    /**
     * Get Machine CPUs node
     *
     * @param ssq
     *            the state system
     * @param machineName
     *            the machine's name
     * @return the quark
     */
    public static int getMachineCPUsNode(ITmfStateSystemBuilder ssq, String machineName) {
        return ssq.getQuarkAbsoluteAndAdd(Attributes.MACHINES, machineName, Attributes.CPUS);
    }

    /**
     * Get Machine pCPUs node
     *
     * @param ssq
     *            the state system
     * @param machineName
     *            the machine's name
     * @return the quark
     */
    public static int getMachinepCPUsNode(ITmfStateSystemBuilder ssq, String machineName) {
        return ssq.getQuarkAbsoluteAndAdd(Attributes.MACHINES, machineName, Attributes.PCPUS);
    }

    /**
     * Get the threads node
     *
     * @param ss
     *            the state system
     * @return the threads quark
     */
    public static int getNodeThreads(ITmfStateSystemBuilder ss) {
        return ss.getQuarkAbsoluteAndAdd(Attributes.THREADS);
    }

    public static int saveContainerThreadID(ITmfStateSystemBuilder ss, int quark,int tid) {
        return ss.getQuarkRelativeAndAdd(quark, Attributes.THREADS, Integer.toString(tid));
    }

    /**
     * Get the Soft IRQs node
     *
     * @param cpuNumber
     *            the cpu core
     * @param ss
     *            the state system
     * @return the Soft IRQ node quark
     */
    public static int getNodeSoftIRQs(int cpuNumber, ITmfStateSystemBuilder ss) {
        return ss.getQuarkAbsoluteAndAdd(Attributes.CPUS, Integer.toString(cpuNumber), Attributes.SOFT_IRQS);
    }

    public static List<Long> getProcessNSIDs(ITmfStateSystemBuilder ss, Integer processQuark, long timestamp) {
        List<Long> namespaces = new LinkedList<>();
        List<Integer> listQuarks = ss.getQuarks(processQuark, Attributes.NS_MAX_LEVEL);
        if (listQuarks.isEmpty()) {
            return namespaces;
        }
        int nsMaxLevelQuark = listQuarks.get(0);
        ITmfStateInterval interval;
        try {
            interval = ss.querySingleState(timestamp, nsMaxLevelQuark);
            int nsMaxLevel = interval.getStateValue().unboxInt();
            if (nsMaxLevel != 1) {
                int actualLevel = 1;
                int virtualTIDQuark = ss.getQuarkRelative(processQuark, Attributes.VTID);
                actualLevel++;
                int namespaceIDQuark = ss.getQuarkRelative(virtualTIDQuark, Attributes.NS_INUM);
                long namespaceID = ss.querySingleState(timestamp, namespaceIDQuark).getStateValue().unboxLong();
                namespaces.add(namespaceID);
                while (actualLevel < nsMaxLevel) {
                    virtualTIDQuark = ss.getQuarkRelative(virtualTIDQuark, Attributes.VTID);
                    actualLevel++;
                    namespaceIDQuark = ss.getQuarkRelative(virtualTIDQuark, Attributes.NS_INUM);
                    namespaceID = ss.querySingleState(timestamp, namespaceIDQuark).getStateValue().unboxLong();
                    namespaces.add(namespaceID);
                }

            }
        } catch (StateSystemDisposedException | AttributeNotFoundException e) {
            e.printStackTrace();
        }
        return namespaces;
    }


    // Method for debug purpose
    // Transform timestamp to something readable: hh:mm:ss
    public static String formatTime(long time) {

        return formatTimeAbs(time);
    }

    private static String formatNs(long srcTime) {
        StringBuffer str = new StringBuffer();
        long ns = Math.abs(srcTime % 1000000000);
        String nanos = Long.toString(ns);
        str.append("000000000".substring(nanos.length())); //$NON-NLS-1$
        str.append(nanos);
        return str.substring(0, 9);
    }

    private static String formatTimeAbs(long time) {
        StringBuffer str = new StringBuffer();

        // format time from nanoseconds to calendar time HH:MM:SS
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss"); //$NON-NLS-1$
        String stime = timeFormat.format(new Date(time / 1000000));
        str.append(stime);
        str.append('.');
        // append the Milliseconds, MicroSeconds and NanoSeconds as specified in
        // the Resolution
        str.append(formatNs(time));
        return str.toString();
    }



}
