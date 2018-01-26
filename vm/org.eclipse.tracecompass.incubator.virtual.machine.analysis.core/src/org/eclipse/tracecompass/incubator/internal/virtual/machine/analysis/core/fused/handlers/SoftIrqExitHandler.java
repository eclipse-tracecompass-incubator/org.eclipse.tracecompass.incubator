/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.handlers;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.FusedAttributes;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualCPU;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualMachine;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.StateValues;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * @author Cédric Biancheri
 */
public class SoftIrqExitHandler extends VMKernelEventHandler {

    public SoftIrqExitHandler(IKernelAnalysisEventLayout layout, FusedVirtualMachineStateProvider sp) {
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

        int currentCPUNode = FusedVMEventHandlerUtils.getCurrentCPUNode(cpu, ss);
        Integer softIrqId = ((Long) event.getContent().getField(getLayout().fieldVec()).getValue()).intValue();
        int currentThreadNode = FusedVMEventHandlerUtils.getCurrentThreadNode(cpu, ss);
        /* Put this SoftIRQ back to inactive (= -1) in the resource tree */
        int quark = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getNodeSoftIRQs(cpu, ss), softIrqId.toString());
        long timestamp = FusedVMEventHandlerUtils.getTimestamp(event);

        if (isSoftIrqRaised(ss.queryOngoingState(quark))) {
            ss.modifyAttribute(timestamp, StateValues.SOFT_IRQ_RAISED_VALUE, quark);
        } else {
            ss.modifyAttribute(timestamp, TmfStateValue.nullValue(), quark);
        }
        List<Integer> softIrqs = ss.getSubAttributes(ss.getParentAttributeQuark(quark), false);
        /* Only set status to running and no exit if ALL softirqs are exited. */
        for (Integer softIrq : softIrqs) {
            @NonNull ITmfStateValue irqStateValue = ss.queryOngoingState(softIrq);
            if (!irqStateValue.isNull()) { // && !(irqStateValue.unboxInt() == StateValues.CPU_STATUS_IRQ)) {
                return;
            }
        }

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

        /* Set the previous process back to running */
        FusedVMEventHandlerUtils.setProcessToRunning(timestamp, currentThreadNode, ss);

        /* Set the CPU status back to "busy" or "idle" */
        quark = ss.getQuarkRelativeAndAdd(currentCPUNode, FusedAttributes.STATUS);
        ITmfStateValue value = cpuObject.getStateBeforeIRQ();
        cpuObject.setCurrentState(value);
        if (modify) {
            ss.modifyAttribute(timestamp, value, quark);
        }

    }

    /**
     * This checks if the running <stong>bit</strong> is set
     *
     * @param state
     *            the state to check
     * @return true if in a softirq. The softirq may be pre-empted by an irq
     */
    private static boolean isSoftIrqRaised(@Nullable ITmfStateValue state) {
        return (state != null &&
                !state.isNull() &&
                (state.unboxInt() & StateValues.CPU_STATUS_SOFT_IRQ_RAISED) == StateValues.CPU_STATUS_SOFT_IRQ_RAISED);
    }

}
