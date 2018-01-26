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
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualMachine;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.StateValues;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * @author Cédric Biancheri
 */
public class SchedWakeupHandler extends VMKernelEventHandler {

    public SchedWakeupHandler(IKernelAnalysisEventLayout layout, FusedVirtualMachineStateProvider sp) {
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
        if (host != null && host.isGuest()) {
            Integer physicalCPU = sp.getPhysicalCPU(host, cpu);
            if (physicalCPU != null) {
                cpu = physicalCPU;
            } else {
                return;
            }
        }
        String machineHost = event.getTrace().getHostId();
        final int tid = ((Long) event.getContent().getField(getLayout().fieldTid()).getValue()).intValue();
        final int prio = ((Long) event.getContent().getField(getLayout().fieldPrio()).getValue()).intValue();

        String threadAttributeName = FusedVMEventHandlerUtils.buildThreadAttributeName(tid, cpu);
        if (threadAttributeName == null) {
            return;
        }

        final int threadNode = ss.getQuarkRelativeAndAdd(FusedVMEventHandlerUtils.getNodeThreads(ss), machineHost, threadAttributeName);

        /*
         * The process indicated in the event's payload is now ready to run.
         * Assign it to the "wait for cpu" state, but only if it was not already
         * running.
         */
        int quark = ss.getQuarkRelativeAndAdd(threadNode, FusedAttributes.STATUS);
        int status = ss.queryOngoingState(quark).unboxInt();
        ITmfStateValue value = null;
        long timestamp = FusedVMEventHandlerUtils.getTimestamp(event);
        if (status != StateValues.PROCESS_STATUS_RUN_SYSCALL &&
                status != StateValues.PROCESS_STATUS_RUN_USERMODE) {
            value = StateValues.PROCESS_STATUS_WAIT_FOR_CPU_VALUE;
            ss.modifyAttribute(timestamp, value, quark);
        }

        /*
         * When a user changes a threads prio (e.g. with pthread_setschedparam),
         * it shows in ftrace with a sched_wakeup.
         */
        quark = ss.getQuarkRelativeAndAdd(threadNode, FusedAttributes.PRIO);
        value = TmfStateValue.newValueInt(prio);
        ss.modifyAttribute(timestamp, value, quark);
    }
}
