/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.handlers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxPidAspect;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.IVirtualMachineEventHandler;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualCPU;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualMachine;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.analysis.VirtualEnvironmentBuilder;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.analysis.VirtualMachineModelAnalysis;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.qemukvm.QemuKvmStrings;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.collect.ImmutableSet;

/**
 * Handle events coming from a qemu/kvm hypervisor
 *
 * @author Geneviève Bastien
 */
public class QemuKvmEventHandler implements IVirtualMachineModelBuilderEventHandler {

    private Map<IKernelAnalysisEventLayout, Set<String>> fRequiredEvents = new HashMap<>();

    private static final ImmutableSet<String> VMSYNC_EVENTS = ImmutableSet.of(
            QemuKvmStrings.VMSYNC_GH_GUEST,
            QemuKvmStrings.VMSYNC_GH_HOST,
            QemuKvmStrings.VMSYNC_HG_GUEST,
            QemuKvmStrings.VMSYNC_HG_HOST);

    /**
     * Constructor
     */
    public QemuKvmEventHandler() {
    }

    @Override
    public Set<String> getRequiredEvents(IKernelAnalysisEventLayout layout) {
        Set<String> events = fRequiredEvents.get(layout);
        if (events == null) {
            events = new HashSet<>();
            events.addAll(layout.eventsKVMEntry());
            events.addAll(layout.eventsKVMExit());
            events.addAll(VMSYNC_EVENTS);
            fRequiredEvents.put(layout, events);
        }
        return events;
    }

    /**
     * Handle the event for a Qemu/kvm model
     *
     * @param ss
     *            The state system builder to fill
     * @param event
     *            The event to handle
     */
    @Override
    public void handleBuilderEvent(ITmfStateSystemBuilder ss, ITmfEvent event, VirtualEnvironmentBuilder virtEnv, IKernelAnalysisEventLayout layout) {
        String eventName = event.getName();
        VirtualMachine machine = virtEnv.getCurrentMachineBuild(event);
        long ts = event.getTimestamp().toNanos();
        if (layout.eventsKVMEntry().contains(eventName)) {
            setMachineHost(ss, machine);
            handleKvmEntry(ss, ts, event, virtEnv, machine);
        } else if (layout.eventsKVMExit().contains(eventName)) {
            setMachineHost(ss, machine);
        } else if (eventName.equals(QemuKvmStrings.VMSYNC_GH_GUEST) || eventName.equals(QemuKvmStrings.VMSYNC_HG_GUEST)) {
            if (machine.isGuest()) {
                // There's nothing more we can learn from these events, return
                return;
            }
            handleGuestEvent(event, machine);
        } else if (eventName.equals(QemuKvmStrings.VMSYNC_GH_HOST)) {
            setMachineHost(ss, machine);
            handleHostEvent(ss, ts, event, virtEnv, machine);
        }
    }

    private static void handleHostEvent(ITmfStateSystemBuilder ss, long ts, ITmfEvent event, VirtualEnvironmentBuilder virtEnv, VirtualMachine hostMachine) {
        HostThread ht = IVirtualMachineEventHandler.getCurrentHostThread(event, ts);
        if (ht == null) {
            return;
        }

        VirtualMachine vm = virtEnv.getGuestMachineBuild(event, ht);
        if (vm != null) {
            // Machine is already known, exit
            return;
        }

        Long vmUid = event.getContent().getFieldValue(Long.class, QemuKvmStrings.VM_UID_PAYLOAD);
        if (vmUid == null) {
            return;
        }

        for (VirtualMachine machine : virtEnv.getMachines()) {
            if (machine.getVmUid() == vmUid) {
                /*
                 * We found the VM being run, let's associate it with the
                 * thread ID and set its hypervisor
                 */
                hostMachine.addChild(machine);
                int guestQuark = ss.getQuarkAbsoluteAndAdd(hostMachine.getHostId(), VirtualMachineModelAnalysis.GUEST_VMS, machine.getHostId());
                ss.modifyAttribute(ts, machine.getTraceName(), guestQuark);

                virtEnv.setGuestMachine(machine, ht);

                int hypervisorQuark = ss.getQuarkRelativeAndAdd(guestQuark, VirtualMachineModelAnalysis.HYPERVISOR);
                ss.modifyAttribute(ts, "Qemu/KVM", hypervisorQuark); //$NON-NLS-1$

                // we have the thread running, its associated process represents the guest
                Integer pid = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), LinuxPidAspect.class, event);
                if (pid == null) {
                    return;
                }
                HostThread parentHt = new HostThread(ht.getHost(), pid);
                // Update guest process if not known
                if (virtEnv.getGuestMachineBuild(event, parentHt) == null) {
                    virtEnv.setGuestMachine(machine, parentHt);
                    int guestProcessQuark = ss.getQuarkRelativeAndAdd(guestQuark, VirtualMachineModelAnalysis.PROCESS);
                    ss.modifyAttribute(ts, pid, guestProcessQuark);
                }
            }
        }
    }

    private static void handleGuestEvent(ITmfEvent event, VirtualMachine machine) {
        // Set the machine as guest and add its uid
        Long uid = event.getContent().getFieldValue(Long.class, QemuKvmStrings.VM_UID_PAYLOAD);
        if (uid != null) {
            machine.setGuest(uid);
        }
    }

    private static void setMachineHost(ITmfStateSystemBuilder ss, VirtualMachine machine) {
        // Machine is already a host, ignore
        if (machine.isHost()) {
            return;
        }
        // Set the machine as host and add the quark for guests
        machine.setHost();
        ss.getQuarkAbsoluteAndAdd(machine.getHostId(), VirtualMachineModelAnalysis.GUEST_VMS);
    }

    private static void handleKvmEntry(ITmfStateSystemBuilder ss, long ts, ITmfEvent event, VirtualEnvironmentBuilder virtEnv, VirtualMachine machine) {
        HostThread ht = IVirtualMachineEventHandler.getCurrentHostThread(event, ts);
        if (ht == null) {
            return;
        }
        VirtualCPU vcpu = virtEnv.getVirtualCpuBuild(event, ht);
        if (vcpu != null) {
            // The current thread has a vcpu configured, ignore
            return;
        }
        // Try to find the guest that corresponds to this one
        VirtualMachine vm = virtEnv.getGuestMachineBuild(event, ht);
        if (vm == null) {
            vm = findVmFromProcess(event, ht, virtEnv);
            if (vm == null) {
                return;
            }
        }
        /* Associate this thread with the virtual CPU that is going to be run */
        final ITmfEventField content = event.getContent();
        Long vcpuId = content.getFieldValue(Long.class, QemuKvmStrings.VCPU_ID);
        if (vcpuId == null) {
            return;
        }
        VirtualCPU virtualCPU = VirtualCPU.getVirtualCPU(vm, vcpuId);
        virtEnv.setGuestCpu(virtualCPU, ht);
        int vcpuQuark = ss.getQuarkAbsoluteAndAdd(machine.getHostId(), VirtualMachineModelAnalysis.GUEST_VMS, vm.getHostId(), VirtualMachineModelAnalysis.CPUS, vcpuId.toString());
        ss.modifyAttribute(ts, ht.getTid(), vcpuQuark);
    }

    private @Nullable static VirtualMachine findVmFromProcess(ITmfEvent event, HostThread ht, VirtualEnvironmentBuilder virtEnv) {
        /*
         * Maybe the process of the current thread has a VM associated, see if we
         * can infer the VM for this thread
         */
        Integer pid = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), LinuxPidAspect.class, event);
        if (pid == null) {
            return null;
        }
        HostThread parentHt = new HostThread(ht.getHost(), pid);
        VirtualMachine vm = virtEnv.getGuestMachineBuild(event, parentHt);
        if (vm != null) {
            virtEnv.setGuestMachine(vm, ht);
        }

        return vm;
    }

}
