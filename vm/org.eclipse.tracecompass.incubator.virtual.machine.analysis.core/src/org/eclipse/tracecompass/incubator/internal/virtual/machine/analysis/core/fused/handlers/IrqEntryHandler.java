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
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Irq Entry Handler
 */
public class IrqEntryHandler extends VMKernelEventHandler {

    /**
     * Constructor
     *
     * @param layout
     *            event layout
     * @param sp
     *            state provider
     */
    public IrqEntryHandler(IKernelAnalysisEventLayout layout, FusedVirtualMachineStateProvider sp) {
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
        Integer irqId = ((Long) event.getContent().getField(getLayout().fieldIrq()).getValue()).intValue();

        /*
         * Mark this IRQ as active in the resource tree. The state value = the
         * CPU on which this IRQ is sitting
         */
        int quark = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getNodeIRQs(cpu, ss), irqId.toString());

        ITmfStateValue value = TmfStateValue.newValueInt(cpu.intValue());
        long timestamp = FusedVMEventHandlerUtils.getTimestamp(event);
        ss.modifyAttribute(timestamp, value, quark);

        /* Change the status of the running process to interrupted */
        quark = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getCurrentThreadNode(cpu, ss), FusedAttributes.STATUS);
        value = StateValues.PROCESS_STATUS_INTERRUPTED_VALUE;
        ss.modifyAttribute(timestamp, value, quark);

        /* Change the status of the CPU to be interrupted */
        quark = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getCurrentCPUNode(cpu, ss), FusedAttributes.STATUS);
        value = ss.queryOngoingState(quark);
        cpuObject.setStateBeforeIRQ(value);
        value = StateValues.CPU_STATUS_IRQ_VALUE;
        ss.modifyAttribute(timestamp, value, quark);
    }

}
