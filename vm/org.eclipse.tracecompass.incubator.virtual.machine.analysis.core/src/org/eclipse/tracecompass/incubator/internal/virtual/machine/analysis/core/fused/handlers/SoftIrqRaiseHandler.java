/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.handlers;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualMachine;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.StateValues;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * @author Cédric Biancheri
 */
public class SoftIrqRaiseHandler extends VMKernelEventHandler {

    /**
     * Constructor
     *
     * @param layout
     *            the layout
     * @param sp
     *            the state provider
     */
    public SoftIrqRaiseHandler(IKernelAnalysisEventLayout layout, FusedVirtualMachineStateProvider sp) {
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

        int value = (isInSoftirq(ss.queryOngoingState(quark)) ?
                StateValues.CPU_STATUS_SOFT_IRQ_RAISED_RUNNING :
                StateValues.CPU_STATUS_SOFT_IRQ_RAISED);
        ss.modifyAttribute(FusedVMEventHandlerUtils.getTimestamp(event), value, quark);

    }

    private static boolean isInSoftirq(@Nullable Object state) {
        return ((state instanceof Integer) &&
                ((int) state & StateValues.CPU_STATUS_SOFTIRQ) == StateValues.CPU_STATUS_SOFTIRQ);
    }
}
