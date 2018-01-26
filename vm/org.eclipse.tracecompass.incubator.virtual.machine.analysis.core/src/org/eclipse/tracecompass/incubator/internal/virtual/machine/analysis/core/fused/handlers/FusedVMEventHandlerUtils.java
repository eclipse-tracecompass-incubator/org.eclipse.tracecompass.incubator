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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.FusedAttributes;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.StateValues;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Utility methods to retrieve information from either the events or the state
 * system
 *
 * @author Cédric Biancheri
 */
public class FusedVMEventHandlerUtils {

    private FusedVMEventHandlerUtils() {
    }

    private static int getNodeCPUs(ITmfStateSystemBuilder ssb) {
        return ssb.getQuarkAbsoluteAndAdd(FusedAttributes.CPUS);
    }

    private static int getNodeThreads(ITmfStateSystemBuilder ssb, String machineName) {
        return ssb.getQuarkAbsoluteAndAdd(FusedAttributes.THREADS, machineName);
    }

    /**
     * Get the node Machines
     *
     * @param ssb
     *            the state system
     * @return the quark
     */
    static int getMachinesNode(ITmfStateSystemBuilder ssb) {
        return ssb.getQuarkAbsoluteAndAdd(FusedAttributes.HOSTS);
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
    static int getCurrentCPUNode(Integer cpuNumber, ITmfStateSystemBuilder ss) {
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
        int quark = ss.getQuarkRelativeAndAdd(getCurrentCPUNode(cpuNumber, ss), FusedAttributes.CURRENT_THREAD);
        ITmfStateValue value = ss.queryOngoingState(quark);
        int thread = value.isNull() ? -1 : value.unboxInt();
        quark = ss.getQuarkRelativeAndAdd(getCurrentCPUNode(cpuNumber, ss), FusedAttributes.MACHINE_NAME);
        value = ss.queryOngoingState(quark);
        String machineName = value.unboxStr();
        return ss.getQuarkRelativeAndAdd(getNodeThreads(ss, machineName), buildThreadAttributeName(thread, cpuNumber));
    }

    /**
     * Build the thread attribute name.
     *
     * For all threads except "0" this is the string representation of the
     * threadId. For thread "0" which is the idle thread and can be running
     * concurrently on multiple CPUs, append "_cpuId".
     *
     * @param threadId
     *            the thread id
     * @param cpuId
     *            the cpu id
     *
     * @return the thread attribute name null if the threadId is zero and the
     *         cpuId is null
     */
    public static @Nullable String buildThreadAttributeName(int threadId, @Nullable Integer cpuId) {

        if (threadId == 0) {
            if (cpuId == null) {
                return null;
            }
            return FusedAttributes.THREAD_0_PREFIX + String.valueOf(cpuId);
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
        return ss.getQuarkAbsoluteAndAdd(FusedAttributes.CPUS, Integer.toString(cpuNumber), FusedAttributes.IRQS);
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

        quark = ssb.getQuarkRelativeAndAdd(currentThreadNode, FusedAttributes.SYSTEM_CALL);
        if (ssb.queryOngoingState(quark).isNull()) {
            /* We were in user mode before the interruption */
            value = StateValues.PROCESS_STATUS_RUN_USERMODE_VALUE;
        } else {
            /* We were previously in kernel mode */
            value = StateValues.PROCESS_STATUS_RUN_SYSCALL_VALUE;
        }
        quark = ssb.getQuarkRelativeAndAdd(currentThreadNode, FusedAttributes.STATUS);
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

        quark = ssb.getQuarkRelativeAndAdd(currentCPUNode, FusedAttributes.STATUS);
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
        int irqQuarks = ssb.getQuarkRelativeAndAdd(cpuQuark, FusedAttributes.IRQS);
        List<Integer> irqs = ssb.getSubAttributes(irqQuarks, false);
        for (Integer quark : irqs) {
            final ITmfStateValue irqState = ssb.queryOngoingState(quark.intValue());
            if (!irqState.isNull()) {
                return irqState;
            }
        }

        /* Check if there is a soft IRQ running */
        int softIrqQuarks = ssb.getQuarkRelativeAndAdd(cpuQuark, FusedAttributes.SOFT_IRQS);
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
        int currentThreadQuark = ssb.getQuarkRelativeAndAdd(cpuQuark, FusedAttributes.CURRENT_THREAD);
        ITmfStateValue currentThreadState = ssb.queryOngoingState(currentThreadQuark);
        if (currentThreadState.isNull()) {
            return TmfStateValue.nullValue();
        }
        int tid = currentThreadState.unboxInt();
        if (tid == 0) {
            return StateValues.CPU_STATUS_IDLE_VALUE;
        }
        int currentMachineQuark = ssb.getQuarkRelativeAndAdd(cpuQuark, FusedAttributes.MACHINE_NAME);
        String machineName = ssb.queryOngoingState(currentMachineQuark).unboxStr();
        int threadSystemCallQuark = ssb.getQuarkRelativeAndAdd(getNodeThreads(ssb, machineName), Integer.toString(tid), FusedAttributes.SYSTEM_CALL);
        return (ssb.queryOngoingState(threadSystemCallQuark).isNull() ? StateValues.CPU_STATUS_RUN_USERMODE_VALUE : StateValues.CPU_STATUS_RUN_SYSCALL_VALUE);
    }

    /**
     * Get a machine CPUs node. This node corresponds to the CPUs available to
     * the machine, ie in the case of a virtual machine, the virtual CPUs.
     *
     * @param ssq
     *            the state system
     * @param hostId
     *            the host ID of the machine
     * @return the quark
     */
    public static int getMachineCPUsNode(ITmfStateSystemBuilder ssq, String hostId) {
        return ssq.getQuarkAbsoluteAndAdd(FusedAttributes.HOSTS, hostId, FusedAttributes.CPUS);
    }

    /**
     * Get a machine pCPUs node. This node corresponds to the physical CPUs used
     * by the machine, ie, in the case of a virtual machine, the CPUs on the
     * host machine.
     *
     * @param ssq
     *            the state system
     * @param hostId
     *            the host ID of the machine
     * @return the quark
     */
    public static int getMachinepCPUsNode(ITmfStateSystemBuilder ssq, String hostId) {
        return ssq.getQuarkAbsoluteAndAdd(FusedAttributes.HOSTS, hostId, FusedAttributes.PCPUS);
    }

    /**
     * Get the threads node
     *
     * @param ss
     *            the state system
     * @return the threads quark
     */
    public static int getNodeThreads(ITmfStateSystemBuilder ss) {
        return ss.getQuarkAbsoluteAndAdd(FusedAttributes.THREADS);
    }

    public static int saveContainerThreadID(ITmfStateSystemBuilder ss, int quark, int tid) {
        return ss.getQuarkRelativeAndAdd(quark, FusedAttributes.THREADS, Integer.toString(tid));
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
        return ss.getQuarkAbsoluteAndAdd(FusedAttributes.CPUS, Integer.toString(cpuNumber), FusedAttributes.SOFT_IRQS);
    }

    /**
     * Get the namespaces for a thread
     *
     * @param ss
     *            The state system
     * @param threadQuark
     *            The quark of the thread
     * @return The list of namespaces the thread is part of
     */
    public static List<Long> getProcessNSIDs(ITmfStateSystemBuilder ss, int threadQuark) {
        List<Long> namespaces = new ArrayList<>();
        int maxLvQuark = ss.optQuarkRelative(threadQuark, FusedAttributes.NS_MAX_LEVEL);
        if (maxLvQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return namespaces;
        }
        ITmfStateValue value;
        value = ss.queryOngoingState(maxLvQuark);
        int nsMaxLevel = value.unboxInt();
        if (nsMaxLevel > 1) {
            int currentLevel = 1;
            int vtidQuark = threadQuark;
            while (currentLevel < nsMaxLevel) {
                vtidQuark = ss.optQuarkRelative(vtidQuark, FusedAttributes.VTID);
                if (vtidQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                    return namespaces;
                }
                int namespaceIDQuark = ss.optQuarkRelative(vtidQuark, FusedAttributes.NS_INUM);
                if (namespaceIDQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                    return namespaces;
                }
                currentLevel++;
                long namespaceID = ss.queryOngoingState(namespaceIDQuark).unboxLong();
                namespaces.add(namespaceID);
            }

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
