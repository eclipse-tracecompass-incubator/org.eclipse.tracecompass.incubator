/*******************************************************************************
 * Copyright (c) 2016-2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.module.StateValues;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;

/**
 * Utility methods to retrieve information from the virtual machine analysis
 *
 * @author Cédric Biancheri
 * @author Geneviève Bastien
 */
public final class FusedVMInformationProvider {

    private FusedVMInformationProvider() {
    }

    public static List<String> getMachinesTraced(ITmfStateSystem ssq) {
        List<String> list = new LinkedList<>();
        List<Integer> machinesQuarks = ssq.getQuarks(FusedAttributes.MACHINES, "*"); //$NON-NLS-1$
        for (Integer machineQuark : machinesQuarks) {
            String machineName = ssq.getAttributeName(machineQuark);
            list.add(machineName);
        }
        return list;
    }

    public static Integer getNbCPUs(ITmfStateSystem ssq, String machineName) {
        List<Integer> vCpuquarks = ssq.getQuarks(FusedAttributes.MACHINES, machineName, FusedAttributes.CPUS, "*"); //$NON-NLS-1$
        return vCpuquarks.size();
    }

    public static List<String> getMachineContainers(ITmfStateSystem ssq, String machineName) {
        List<String> containers = new LinkedList<>();
        List<Integer> containersQuark = ssq.getQuarks(FusedAttributes.MACHINES, machineName, FusedAttributes.CONTAINERS, "*");
        for (Integer containerQuark : containersQuark) {
            containers.add(ssq.getAttributeName(containerQuark));
        }
        return containers;
    }

    public static List<Integer> getMachineContainersQuarks(ITmfStateSystem ssq, String machineName) {
        return ssq.getQuarks(FusedAttributes.MACHINES, machineName, FusedAttributes.CONTAINERS, "*");
    }

    public static int getContainerQuark(ITmfStateSystem ssq, String machineName, String containerID) {
        try {
            return ssq.getQuarkAbsolute(FusedAttributes.MACHINES, machineName, FusedAttributes.CONTAINERS, containerID);
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return -1;
    }

    public static int getNodeThreadsAndAdd(ITmfStateSystemBuilder ssq) {
        return ssq.getQuarkAbsoluteAndAdd(FusedAttributes.THREADS);
    }

    public static int getNodeThreads(ITmfStateSystem ssq) {
        try {
            return ssq.getQuarkAbsolute(FusedAttributes.THREADS);
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return -1;
    }

    public static @Nullable ITmfStateValue getTypeMachine(ITmfStateSystem ssq, String machineName) {
        int quark;
        try {
            quark = ssq.getQuarkAbsolute(FusedAttributes.MACHINES, machineName);
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
        return ss.getQuarkRelativeAndAdd(quark, FusedAttributes.THREADS, Integer.toString(tid));
    }

    public static int getMachineCPUsNode(ITmfStateSystemBuilder ssq, String machineName) {
        return ssq.getQuarkAbsoluteAndAdd(FusedAttributes.MACHINES, machineName, FusedAttributes.CPUS);
    }

    public static int getNodeIRQs(ITmfStateSystemBuilder ssq) {
        return ssq.getQuarkAbsoluteAndAdd(FusedAttributes.IRQS);
    }

    public static int getNodeSoftIRQs(ITmfStateSystemBuilder ssq) {
        return ssq.getQuarkAbsoluteAndAdd(FusedAttributes.SOFT_IRQS);
    }

    public static int getNodeNsInum(ITmfStateSystem ssq, long time, String machineName, int threadID) throws AttributeNotFoundException, StateSystemDisposedException {
        int quark = ssq.getQuarkRelative(FusedVMInformationProvider.getNodeThreads(ssq), machineName, Integer.toString(threadID), FusedAttributes.NS_MAX_LEVEL);
        ITmfStateInterval interval = ssq.querySingleState(time, quark);
        quark = ssq.getQuarkRelative(FusedVMInformationProvider.getNodeThreads(ssq), machineName, Integer.toString(threadID));
        int nsMaxLevel = interval.getStateValue().unboxInt();
        for (int i = 1; i < nsMaxLevel; i++) {
            quark = ssq.getQuarkRelative(quark, FusedAttributes.VTID);
        }
        return ssq.getQuarkRelative(quark, FusedAttributes.NS_INUM);
    }

    public static Long getParentContainer(ITmfStateSystem ssq, int containerQuark) {
        int parentContainerIDQuark;
        Long parentContainerID = null;
        try {
            parentContainerIDQuark = ssq.getQuarkRelative(containerQuark, FusedAttributes.PARENT);
            parentContainerID = ssq.querySingleState(ssq.getStartTime(), parentContainerIDQuark).getStateValue().unboxLong();

        } catch (AttributeNotFoundException | StateSystemDisposedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return parentContainerID;
    }

    /**
     * Get the list of machine names, sorted from the closest to hardware to
     * most virtual, that were involved on a given CPU at a certain time
     *
     * @param ssq
     *            The state system used by this analysis
     * @param physicalCpu
     *            The number of the physical processor to query
     * @param time
     *            The time at which to query the machines
     * @return The list of machine names involved on the CPU at the requested
     *         time. The list of sorted from the physical machine to the most
     *         virtual layer.
     */
    public static List<String> getAllMachines(ITmfStateSystem ssq, int physicalCpu, long time) {
        List<String> machines = new ArrayList<>();
        // We'll need a few values, so query the full state at the time and get
        // the values from there
        try {
            List<ITmfStateInterval> states = ssq.queryFullState(time);
            // Get the thread on the CPU
            int quarkCurrentThread = ssq.optQuarkAbsolute(FusedAttributes.CPUS, String.valueOf(physicalCpu), FusedAttributes.CURRENT_THREAD);
            if (quarkCurrentThread == ITmfStateSystem.INVALID_ATTRIBUTE) {
                return machines;
            }
            int tid = states.get(quarkCurrentThread).getStateValue().unboxInt();
            if (tid < 0) {
                return machines;
            }
            int quarkCurrentMachine = ssq.optQuarkAbsolute(FusedAttributes.CPUS, String.valueOf(physicalCpu), FusedAttributes.MACHINE_NAME);
            if (quarkCurrentMachine == ITmfStateSystem.INVALID_ATTRIBUTE) {
                return machines;
            }
            ITmfStateValue stateValue = states.get(quarkCurrentMachine).getStateValue();
            if (stateValue.isNull()) {
                return machines;
            }
            machines.add(stateValue.unboxStr());

            // Follow this thread's namespaces
            machines.addAll(getContainersOf(ssq, stateValue.unboxStr(), tid, states));

            // Follow the CPU through virtual machines
            int quarkCpuState = ssq.optQuarkAbsolute(FusedAttributes.CPUS, String.valueOf(physicalCpu), FusedAttributes.CONDITION);
            int quarkVirtualCpu = ssq.optQuarkAbsolute(FusedAttributes.CPUS, String.valueOf(physicalCpu), FusedAttributes.VIRTUAL_CPU);

            if (quarkCpuState == ITmfStateSystem.INVALID_ATTRIBUTE || quarkVirtualCpu == ITmfStateSystem.INVALID_ATTRIBUTE) {
                return machines;
            }
            int vmCondition = states.get(quarkCpuState).getStateValue().unboxInt();
            if (vmCondition == StateValues.CONDITION_IN_VM) {
                machines.addAll(0, getParentMachines(ssq, stateValue.unboxStr(), states.get(quarkVirtualCpu).getStateValue().unboxInt(), states));
            }

        } catch (StateSystemDisposedException e) {
            // Nothing to do, about to be disposed
        }

        return machines;
    }

    private static Collection<String> getParentMachines(ITmfStateSystem ssq, String machine, int vcpu, List<ITmfStateInterval> states) {
        List<String> machines = new ArrayList<>();
        int machineQuark = ssq.optQuarkAbsolute(FusedAttributes.MACHINES, machine);
        if (machineQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return machines;
        }
        int quarkParent = ssq.optQuarkRelative(machineQuark, FusedAttributes.PARENT);
        int quarkVCpu = ssq.optQuarkRelative(machineQuark, FusedAttributes.CPUS, String.valueOf(vcpu));
        if (quarkParent == ITmfStateSystem.INVALID_ATTRIBUTE || quarkVCpu == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return machines;
        }
        ITmfStateValue parentValue = states.get(quarkParent).getStateValue();
        if (parentValue.isNull()) {
            return machines;
        }
        machines.add(parentValue.unboxStr());
        ITmfStateValue vcpuValue = states.get(quarkVCpu).getStateValue();
        if (vcpuValue.isNull()) {
            return machines;
        }
        machines.addAll(getContainersOf(ssq, parentValue.unboxStr(), vcpuValue.unboxInt(), states));
        return machines;
    }

    private static List<String> getContainersOf(ITmfStateSystem ssq, String machine, int tid, List<ITmfStateInterval> states) {
        List<String> containers = new ArrayList<>();
        int threadQuark = ssq.optQuarkAbsolute(FusedAttributes.THREADS, machine, String.valueOf(tid));
        if (threadQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return containers;
        }
        int quarkMaxLv = ssq.optQuarkRelative(threadQuark, FusedAttributes.NS_MAX_LEVEL);
        if (quarkMaxLv == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return containers;
        }
        int maxLv = states.get(quarkMaxLv).getStateValue().unboxInt();
        // Start at lv 1, as level 0 is the main host
        for (int i = 1; i < maxLv; i++) {
            threadQuark = ssq.optQuarkRelative(threadQuark, FusedAttributes.VTID);
            int inumQuark = ssq.optQuarkRelative(threadQuark, FusedAttributes.NS_INUM);
            if (threadQuark == ITmfStateSystem.INVALID_ATTRIBUTE || inumQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                break;
            }
            ITmfStateValue inumValue = states.get(inumQuark).getStateValue();
            if (inumValue.isNull()) {
                continue;
            }
            containers.add(String.valueOf(inumValue.unboxLong()));
        }

        return containers;
    }

    public static String getParentMachineName(ITmfStateSystem ssq, String machineName) {
        String parentName = ""; //$NON-NLS-1$
        try {
            int parentNameQuark = ssq.getQuarkAbsolute(FusedAttributes.MACHINES, machineName, FusedAttributes.PARENT);
            parentName = ssq.querySingleState(ssq.getStartTime(), parentNameQuark).getStateValue().unboxStr();
        } catch (AttributeNotFoundException | StateSystemDisposedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return parentName;
    }

    public static List<String> getPhysicalCpusUsedByMachine(ITmfStateSystem ssq, String machineName) {
        List<String> pcpus = new LinkedList<>();
        List<Integer> pCpuquarks = new LinkedList<>();
        ITmfStateValue type = getTypeMachine(ssq, machineName);
        if (type == null) {
            return pcpus;
        }
        if ((type.unboxInt() & StateValues.MACHINE_GUEST) == StateValues.MACHINE_GUEST) {
            pCpuquarks = ssq.getQuarks(FusedAttributes.MACHINES, machineName, FusedAttributes.PCPUS, "*"); //$NON-NLS-1$
        } else if (type.unboxInt() == StateValues.MACHINE_HOST) {
            pCpuquarks = ssq.getQuarks(FusedAttributes.MACHINES, machineName, FusedAttributes.CPUS, "*"); //$NON-NLS-1$
        }
        for (Integer quark : pCpuquarks) {
            pcpus.add(ssq.getAttributeName(quark));
        }
        return pcpus;
    }

    public static List<String> getCpusUsedByMachine(ITmfStateSystem ssq, String machineName) {
        List<String> cpus = new LinkedList<>();
        List<Integer> cpuQuarks = new LinkedList<>();
        ITmfStateValue type = getTypeMachine(ssq, machineName);
        if (type == null) {
            return cpus;
        }
        cpuQuarks = ssq.getQuarks(FusedAttributes.MACHINES, machineName, FusedAttributes.CPUS, "*"); //$NON-NLS-1$
        for (Integer quark : cpuQuarks) {
            cpus.add(ssq.getAttributeName(quark));
        }
        return cpus;
    }

    public static List<String> getPCpusUsedByContainer(ITmfStateSystem ssq, int quark) {
        List<String> pcpus = new LinkedList<>();
        List<Integer> pCpusQuarks = ssq.getQuarks(quark, FusedAttributes.PCPUS, "*");
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
        return String.valueOf(str);
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

}
