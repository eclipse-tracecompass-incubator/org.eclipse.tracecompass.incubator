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
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

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

/**
 * Handle events coming from a qemu/kvm hypervisor
 *
 * @author Geneviève Bastien
 */
public class QemuKvmEventHandler implements IVirtualMachineEventHandler {

    private Map<IKernelAnalysisEventLayout, Set<String>> fRequiredEvents = new HashMap<>();

    /**
     * Constructor
     */
    public QemuKvmEventHandler() {
        // Nothing to do
    }

    @Override
    public Set<String> getRequiredEvents(IKernelAnalysisEventLayout layout) {
        Set<String> events = fRequiredEvents.get(layout);
        if (events == null) {
            events = new HashSet<>();
            events.addAll(layout.eventsKVMEntry());
            events.addAll(layout.eventsKVMExit());
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
    public void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event, IVirtualEnvironmentModel virtEnv, IKernelAnalysisEventLayout layout) {
        String eventName = event.getName();
        long ts = event.getTimestamp().toNanos();
        if (layout.eventsKVMEntry().contains(eventName)) {
            // The vcpu is exiting hypervisor mode
            handleKvmEvent(ss, ts, event, virtEnv, i -> i & ~VcpuStateValues.VCPU_VMM);
        } else if (layout.eventsKVMExit().contains(eventName)) {
            // The vcpu is entering hypervisor mode
            handleKvmEvent(ss, ts, event, virtEnv, i -> i | VcpuStateValues.VCPU_VMM);
        }
    }

    private static void handleKvmEvent(ITmfStateSystemBuilder ss, long ts, ITmfEvent event, IVirtualEnvironmentModel virtEnv, Function<Integer, Integer> fctNewVal) {
        HostThread ht = IVirtualMachineEventHandler.getCurrentHostThread(event, ts);
        if (ht == null) {
            return;
        }
        VirtualCPU vcpu = virtEnv.getVirtualCpu(event, ht);
        if (vcpu == null) {
            // The current thread has a vcpu configured, ignore
            return;
        }
        /* Add the hypervisor flag to the status */
        VirtualMachine vm = vcpu.getVm();
        int curStatusQuark = ss.getQuarkAbsoluteAndAdd(VmAttributes.VIRTUAL_MACHINES, vm.getHostId(),
                vcpu.getCpuId().toString(), VmAttributes.STATUS);

        int prevStatus = Math.max(VcpuStateValues.VCPU_UNKNOWN, ss.queryOngoingState(curStatusQuark).unboxInt());
        int value = Objects.requireNonNull(fctNewVal.apply(prevStatus));
        ss.modifyAttribute(ts, value, curStatusQuark);
    }

}
