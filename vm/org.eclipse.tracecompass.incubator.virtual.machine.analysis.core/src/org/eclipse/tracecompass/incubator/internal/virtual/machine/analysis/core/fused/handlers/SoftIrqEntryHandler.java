/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.handlers;

import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.FusedAttributes;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualCPU;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualMachine;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.StateValues;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * @author Cédric Biancheri
 */
public class SoftIrqEntryHandler extends VMKernelEventHandler {

    public SoftIrqEntryHandler(IKernelAnalysisEventLayout layout, FusedVirtualMachineStateProvider sp) {
        super(layout, sp);
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event) {
        Integer cpu = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);
        if (cpu == null) {
            return;
        }
        FusedVirtualMachineStateProvider sp = getStateProvider();
        VirtualMachine host = sp.getCurrentMachine(event);
        VirtualCPU cpuObject = VirtualCPU.getVirtualCPU(host, cpu.longValue());
        if (host != null && host.isGuest()) {
            Integer physicalCPU = sp.getPhysicalCPU(host, cpu);
            if (physicalCPU != null) {
                cpu = physicalCPU;
            } else {
                return;
            }
        }

        long timestamp = FusedVMEventHandlerUtils.getTimestamp(event);
        Integer softIrqId = ((Long) event.getContent().getField(getLayout().fieldVec()).getValue()).intValue();
        int currentCPUNode = FusedVMEventHandlerUtils.getCurrentCPUNode(cpu, ss);
        int currentThreadNode = FusedVMEventHandlerUtils.getCurrentThreadNode(cpu, ss);

        /*
         * If the trace that generates the event doesn't match the currently
         * running machine on this pcpu then we do not modify the state system.
         */
        boolean modify = true;
        if (host != null) {
            int machineNameQuark = ss.getQuarkRelativeAndAdd(currentCPUNode, FusedAttributes.MACHINE_NAME);
            try {
                modify = ss.querySingleState(timestamp, machineNameQuark).getStateValue().unboxStr().equals(host.getHostId());
            } catch (StateSystemDisposedException e) {
                e.printStackTrace();
            }
        }

        /*
         * Mark this SoftIRQ as active in the resource tree. The state value =
         * the CPU on which this SoftIRQ is processed
         */
        int quark = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getNodeSoftIRQs(cpu, ss), softIrqId.toString());
        ITmfStateValue value = StateValues.CPU_STATUS_SOFTIRQ_VALUE;
        ss.modifyAttribute(timestamp, value, quark);

        /* Change the status of the running process to interrupted */
        quark = ss.getQuarkRelativeAndAdd(currentThreadNode, FusedAttributes.STATUS);
        value = StateValues.PROCESS_STATUS_INTERRUPTED_VALUE;
        ss.modifyAttribute(timestamp, value, quark);

        /* Change the status of the CPU to interrupted */
        quark = ss.getQuarkRelativeAndAdd(currentCPUNode, FusedAttributes.STATUS);
        value = ss.queryOngoingState(quark);
        value = cpuObject.getCurrentState();
//        cpuObject.setCurrentState(value);
        if (value != StateValues.CPU_STATUS_SOFTIRQ_VALUE && value != StateValues.SOFT_IRQ_RAISED_VALUE) {
            /* Save only if we are not doing multiple soft irqs */
            cpuObject.setStateBeforeIRQ(value);
        }
        value = StateValues.CPU_STATUS_SOFTIRQ_VALUE;
        cpuObject.setCurrentState(value);
        if (modify) {
            ss.modifyAttribute(timestamp, value, quark);
        }

    }
}
