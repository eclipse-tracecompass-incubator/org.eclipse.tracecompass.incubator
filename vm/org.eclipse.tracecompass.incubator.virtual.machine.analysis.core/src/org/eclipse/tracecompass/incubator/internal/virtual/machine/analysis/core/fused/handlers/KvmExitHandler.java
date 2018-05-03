/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.handlers;

import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.FusedAttributes;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualCPU;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualMachine;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.StateValues;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * @author Cédric Biancheri
 */
public class KvmExitHandler extends VMKernelEventHandler {

    public KvmExitHandler(IKernelAnalysisEventLayout layout, FusedVirtualMachineStateProvider sp) {
        super(layout, sp);
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event) {
        Integer cpu = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);
        if (cpu == null) {
            return;
        }
        FusedVirtualMachineStateProvider sp = getStateProvider();
        int currentCPUNode = FusedVMEventHandlerUtils.getCurrentCPUNode(cpu, ss);
        int quark;

        VirtualMachine host = sp.getCurrentMachine(event);
        if (host == null) {
            return;
        }

        if (host.isHost() && host.isGuest()) {
            /*
             * This exit is not relevant, it was already done by the real host a
             * long time ago.
             */
            return;
        }

        /* Get the host CPU doing the kvm_exit. */
        VirtualCPU hostCpu = VirtualCPU.getVirtualCPU(host, cpu.longValue());
        /*
         * Get the host thread to get the right virtual machine.
         */
        long timestamp = FusedVMEventHandlerUtils.getTimestamp(event);
        Integer currentThread = hostCpu.getCurrentThread();
        HostThread ht = new HostThread(host.getHostId(), currentThread);
        VirtualCPU vcpu = sp.getVirtualCpu(ht);
        if (vcpu == null) {
            return;
        }


        /* Check if we are getting out of an higher layer. */
        if (host.isThreadReadyForNextLayer(ht)) {
            /* If so, get the vcpu of this higher layer. */
            vcpu = vcpu.getNextLayerVCPU();
            if (vcpu == null) {
                return;
            }
        }

        /* Save the state of the VCpu. */
        quark = ss.getQuarkRelativeAndAdd(currentCPUNode, FusedAttributes.STATUS);
        Object status = ss.queryOngoing(quark);
        if (!(status instanceof Integer)) {
            return;
        }
        vcpu.setCurrentState((Integer) status);

        /* Then the current state of the host is restored. */
        ss.modifyAttribute(timestamp, hostCpu.getCurrentState(), quark);

        /*
         * Save the current thread of the vm that was running.
         */
        quark = ss.getQuarkRelativeAndAdd(currentCPUNode, FusedAttributes.CURRENT_THREAD);
        status = ss.queryOngoing(quark);
        if (!(status instanceof Integer)) {
            return;
        }
        vcpu.setCurrentThread((Integer) status);

        /* Restore the thread of the host that was running. */
        ss.modifyAttribute(timestamp, hostCpu.getCurrentThread(), quark);

        /* Add the condition out_vm in the state system. */
        quark = ss.getQuarkRelativeAndAdd(currentCPUNode, FusedAttributes.CONDITION);
        ss.modifyAttribute(timestamp, StateValues.CONDITION_OUT_VM, quark);

        /*
         * Set the name of the VM that will run just after the kvm_entry
         */
        int machineNameQuark = ss.getQuarkRelativeAndAdd(currentCPUNode, FusedAttributes.MACHINE_NAME);
        ss.modifyAttribute(timestamp, event.getTrace().getHostId(), machineNameQuark);
    }

}
