/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.handlers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.IVirtualMachineEventHandler;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.data.VcpuStateValues;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.IVirtualEnvironmentModel;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualCPU;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualMachine;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.VmAttributes;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Handle sched switches
 *
 * @author Geneviève Bastien
 */
public class SchedSwitchEventHandler implements IVirtualMachineEventHandler {

    private Map<IKernelAnalysisEventLayout, Set<String>> fRequiredEvents = new HashMap<>();

    /**
     * Constructor
     */
    public SchedSwitchEventHandler() {
        // Nothing to do
    }

    @Override
    public Set<String> getRequiredEvents(IKernelAnalysisEventLayout layout) {
        Set<String> events = fRequiredEvents.get(layout);
        if (events == null) {
            events = new HashSet<>();
            events.add(layout.eventSchedSwitch());
            fRequiredEvents.put(layout, events);
        }
        return events;
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event, IVirtualEnvironmentModel virtEnv, IKernelAnalysisEventLayout eventLayout) {
        ITmfEventField content = event.getContent();
        long ts = event.getTimestamp().toNanos();
        Long prevTid = content.getFieldValue(Long.class, eventLayout.fieldPrevTid());
        Long nextTid = content.getFieldValue(Long.class, eventLayout.fieldNextTid());

        VirtualMachine currentMachine = virtEnv.getCurrentMachine(event);

        /*
         * If sched switch is from a guest, just update the status of the virtual CPU to
         * either idle or running
         */
        if (currentMachine.isGuest() && nextTid != null) {
            updateGuestStatus(ss, event, ts, currentMachine, nextTid);
        }

        /*
         * If previous thread is virtual CPU, update status of the virtual CPU to
         * preempted
         */
        if (currentMachine.isHost() && prevTid != null) {
            updatePreviousHostStatus(ss, event, ts, currentMachine, prevTid, virtEnv);
        }

        /*
         * If next thread is virtual CPU, update status of the virtual CPU the previous
         * status
         */
        if (currentMachine.isHost() && nextTid != null) {
            updateNextHostStatus(ss, event, ts, currentMachine, nextTid, virtEnv);
        }
    }

    private static void updateNextHostStatus(ITmfStateSystemBuilder ss, ITmfEvent event, long ts, VirtualMachine currentMachine, Long nextTid, IVirtualEnvironmentModel virtEnv) {
        HostThread ht = new HostThread(currentMachine.getHostId(), nextTid.intValue());
        VirtualCPU vcpu = virtEnv.getVirtualCpu(event, ht);

        if (vcpu == null) {
            return;
        }
        VirtualMachine vm = vcpu.getVm();

        int curStatusQuark = ss.getQuarkAbsoluteAndAdd(VmAttributes.VIRTUAL_MACHINES, vm.getHostId(),
                vcpu.getCpuId().toString(), VmAttributes.STATUS);

        /* Remove the preempted flag from the status */
        int prevStatus = Math.max(VcpuStateValues.VCPU_UNKNOWN, ss.queryOngoingState(curStatusQuark).unboxInt());
        int value = prevStatus & ~VcpuStateValues.VCPU_PREEMPT;
        ss.modifyAttribute(ts, value, curStatusQuark);
    }

    private static void updatePreviousHostStatus(ITmfStateSystemBuilder ss, ITmfEvent event, long ts, VirtualMachine currentMachine, Long prevTid, IVirtualEnvironmentModel virtEnv) {
        HostThread ht = new HostThread(currentMachine.getHostId(), prevTid.intValue());
        VirtualCPU vcpu = virtEnv.getVirtualCpu(event, ht);

        if (vcpu == null) {
            return;
        }

        VirtualMachine vm = vcpu.getVm();

        int curStatusQuark = ss.getQuarkAbsoluteAndAdd(VmAttributes.VIRTUAL_MACHINES, vm.getHostId(),
                vcpu.getCpuId().toString(), VmAttributes.STATUS);

        /* Add the preempted flag to the status */
        int prevStatus = Math.max(VcpuStateValues.VCPU_UNKNOWN, ss.queryOngoingState(curStatusQuark).unboxInt());
        int value = prevStatus | VcpuStateValues.VCPU_PREEMPT;
        ss.modifyAttribute(ts, value, curStatusQuark);
    }

    private static void updateGuestStatus(ITmfStateSystemBuilder ss, ITmfEvent event, long ts, VirtualMachine currentMachine, Long nextTid) {
        Integer cpu = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);
        if (cpu == null) {
            /* We couldn't find any CPU information, ignore */
            return;
        }

        int curStatusQuark = ss.getQuarkAbsoluteAndAdd(VmAttributes.VIRTUAL_MACHINES, currentMachine.getHostId(),
                cpu.toString(), VmAttributes.STATUS);
        // In theory, sched switches should not happen when vcpu is preempted or in vmm,
        // but in practice, synchronization is not perfect and it may.
        int currentStatus = ss.queryOngoingState(curStatusQuark).unboxInt();
        int curVmStatus = currentStatus & (VcpuStateValues.VCPU_PREEMPT | VcpuStateValues.VCPU_VMM);
        int value = (nextTid > 0) ? VcpuStateValues.VCPU_RUNNING | curVmStatus : VcpuStateValues.VCPU_IDLE | curVmStatus;
        ss.modifyAttribute(ts, value, curStatusQuark);
    }

}
