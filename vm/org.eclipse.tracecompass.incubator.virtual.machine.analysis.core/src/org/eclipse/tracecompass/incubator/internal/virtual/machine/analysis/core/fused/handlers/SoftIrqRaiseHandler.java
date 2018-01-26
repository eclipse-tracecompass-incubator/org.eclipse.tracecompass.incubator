/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.handlers;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualMachine;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.StateValues;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * @author Cédric Biancheri
 */
public class SoftIrqRaiseHandler extends VMKernelEventHandler {

    public SoftIrqRaiseHandler(@NonNull IKernelAnalysisEventLayout layout, FusedVirtualMachineStateProvider sp) {
        super(layout, sp);
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event) {
        Integer softIrqId = ((Long) event.getContent().getField(getLayout().fieldVec()).getValue()).intValue();
        Integer cpu = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);
        if (cpu == null) {
            return;
        }
        FusedVirtualMachineStateProvider sp = getStateProvider();
        VirtualMachine host = sp.getCurrentMachine(event);
        if (host != null && host.isGuest()) {
            Integer physicalCPU = sp.getPhysicalCPU(host, cpu);
            if (physicalCPU != null) {
                cpu = physicalCPU;
            } else {
                return;
            }
        }
        /*
         * Mark this SoftIRQ as *raised* in the resource tree.
         */
        int quark = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getNodeSoftIRQs(cpu, ss), softIrqId.toString());

        ITmfStateValue value = (isInSoftirq(ss.queryOngoingState(quark)) ?
                StateValues.SOFT_IRQ_RAISED_RUNNING_VALUE :
                StateValues.SOFT_IRQ_RAISED_VALUE);
        ss.modifyAttribute(FusedVMEventHandlerUtils.getTimestamp(event), value, quark);

    }

    private static boolean isInSoftirq(@Nullable ITmfStateValue state) {
        return (state != null &&
                !state.isNull() &&
                (state.unboxInt() & StateValues.CPU_STATUS_SOFTIRQ) == StateValues.CPU_STATUS_SOFTIRQ);
    }
}
