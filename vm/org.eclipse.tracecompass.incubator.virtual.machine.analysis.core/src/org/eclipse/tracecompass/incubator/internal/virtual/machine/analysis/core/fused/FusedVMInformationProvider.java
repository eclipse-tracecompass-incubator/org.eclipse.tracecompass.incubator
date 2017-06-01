/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.data.Attributes;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.module.StateValues;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;

/**
 * @author Cédric Biancheri
 */
public class FusedVMInformationProvider {

    private FusedVMInformationProvider() {
    }

    public static List<String> getMachinesTraced(ITmfStateSystem ssq) {
        List<String> list = new LinkedList<>();
        List<Integer> machinesQuarks = ssq.getQuarks(Attributes.MACHINES, "*"); //$NON-NLS-1$
        for (Integer machineQuark : machinesQuarks) {
            String machineName = ssq.getAttributeName(machineQuark);
            list.add(machineName);
        }
        return list;
    }

    public static Integer getNbCPUs(ITmfStateSystem ssq, String machineName) {
        List<Integer> vCpuquarks = ssq.getQuarks(Attributes.MACHINES, machineName, Attributes.CPUS, "*"); //$NON-NLS-1$
        return vCpuquarks.size();
    }

    public static List<String> getMachineContainers(ITmfStateSystem ssq, String machineName) {
        List<String> containers = new LinkedList<>();
        List<Integer> containersQuark = ssq.getQuarks(Attributes.MACHINES, machineName, Attributes.CONTAINERS, "*");
        for (Integer containerQuark : containersQuark) {
            containers.add(ssq.getAttributeName(containerQuark));
        }
        return containers;
    }

    public static List<Integer> getMachineContainersQuarks(ITmfStateSystem ssq, String machineName) {
        return ssq.getQuarks(Attributes.MACHINES, machineName, Attributes.CONTAINERS, "*");
    }

    public static int getContainerQuark(ITmfStateSystem ssq, String machineName, String containerID) {
        try {
            return ssq.getQuarkAbsolute(Attributes.MACHINES, machineName, Attributes.CONTAINERS, containerID);
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return -1;
    }

    public static int getNodeThreadsAndAdd(ITmfStateSystemBuilder ssq) {
        return ssq.getQuarkAbsoluteAndAdd(Attributes.THREADS);
    }

    public static int getNodeThreads(ITmfStateSystem ssq) {
        try {
            return ssq.getQuarkAbsolute(Attributes.THREADS);
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return -1;
    }

    public static @Nullable ITmfStateValue getTypeMachine(ITmfStateSystem ssq, String machineName) {
        int quark;
        try {
            quark = ssq.getQuarkAbsolute(Attributes.MACHINES, machineName);
            return ssq.querySingleState(ssq.getStartTime(), quark).getStateValue();
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (StateSystemDisposedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public static int saveContainerThreadID(ITmfStateSystemBuilder ss, int quark, int tid) {
        return ss.getQuarkRelativeAndAdd(quark, Attributes.THREADS, Integer.toString(tid));
    }

    public static int getMachineCPUsNode(ITmfStateSystemBuilder ssq, String machineName) {
        return ssq.getQuarkAbsoluteAndAdd(Attributes.MACHINES, machineName, Attributes.CPUS);
    }

    public static int getNodeIRQs(ITmfStateSystemBuilder ssq) {
        return ssq.getQuarkAbsoluteAndAdd(Attributes.IRQS);
    }

    public static int getNodeSoftIRQs(ITmfStateSystemBuilder ssq) {
        return ssq.getQuarkAbsoluteAndAdd(Attributes.SOFT_IRQS);
    }

    public static int getNodeNsInum(ITmfStateSystem ssq, long time, String machineName, int threadID) throws AttributeNotFoundException, StateSystemDisposedException {
        int quark = ssq.getQuarkRelative(FusedVMInformationProvider.getNodeThreads(ssq), machineName, Integer.toString(threadID), Attributes.NS_MAX_LEVEL);
        ITmfStateInterval interval = ssq.querySingleState(time, quark);
        quark = ssq.getQuarkRelative(FusedVMInformationProvider.getNodeThreads(ssq), machineName, Integer.toString(threadID));
        int nsMaxLevel = interval.getStateValue().unboxInt();
        for (int i = 1; i < nsMaxLevel; i++) {
            quark = ssq.getQuarkRelative(quark, Attributes.VTID);
        }
        return ssq.getQuarkRelative(quark, Attributes.NS_INUM);
    }

    public static Long getParentContainer(ITmfStateSystem ssq, int containerQuark) {
        int parentContainerIDQuark;
        Long parentContainerID = null;
        try {
            parentContainerIDQuark = ssq.getQuarkRelative(containerQuark, Attributes.PARENT);
            parentContainerID = ssq.querySingleState(ssq.getStartTime(), parentContainerIDQuark).getStateValue().unboxLong();

        } catch (AttributeNotFoundException | StateSystemDisposedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return parentContainerID;
    }

    public static String getParentMachineName(ITmfStateSystem ssq, String machineName) {
        String parentName = ""; //$NON-NLS-1$
        try {
            int parentNameQuark = ssq.getQuarkAbsolute(Attributes.MACHINES, machineName, Attributes.PARENT);
            parentName = ssq.querySingleState(ssq.getStartTime(), parentNameQuark).getStateValue().unboxStr();
        } catch (AttributeNotFoundException | StateSystemDisposedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return parentName;
    }

    public static List<String> getPCpusUsedByMachine(ITmfStateSystem ssq, String machineName) {
        List<String> pcpus = new LinkedList<>();
        List<Integer> pCpuquarks = new LinkedList<>();
        ITmfStateValue type = getTypeMachine(ssq, machineName);
        if (type == null) {
            return pcpus;
        }
        if ((type.unboxInt() & StateValues.MACHINE_GUEST) == StateValues.MACHINE_GUEST) {
            pCpuquarks = ssq.getQuarks(Attributes.MACHINES, machineName, Attributes.PCPUS, "*"); //$NON-NLS-1$
        } else if (type.unboxInt() == StateValues.MACHINE_HOST) {
            pCpuquarks = ssq.getQuarks(Attributes.MACHINES, machineName, Attributes.CPUS, "*"); //$NON-NLS-1$
        }
        for (Integer quark : pCpuquarks) {
            pcpus.add(ssq.getAttributeName(quark));
        }
        return pcpus;
    }

    public static List<String> getPCpusUsedByContainer(ITmfStateSystem ssq, int quark) {
        List<String> pcpus = new LinkedList<>();
        List<Integer> pCpusQuarks = ssq.getQuarks(quark, Attributes.PCPUS, "*");
        for (int pCpuqQuark : pCpusQuarks) {
            pcpus.add(ssq.getAttributeName(pCpuqQuark));
        }
        return pcpus;
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
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        String stime = timeFormat.format(new Date(time / 1000000));
        str.append(stime);
        str.append('.');
        // append the Milliseconds, MicroSeconds and NanoSeconds as specified in
        // the Resolution
        str.append(formatNs(time));
        return str.toString();
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
            return Attributes.THREAD_0_PREFIX + String.valueOf(cpuId);
        }

        return String.valueOf(threadId);
    }

}
