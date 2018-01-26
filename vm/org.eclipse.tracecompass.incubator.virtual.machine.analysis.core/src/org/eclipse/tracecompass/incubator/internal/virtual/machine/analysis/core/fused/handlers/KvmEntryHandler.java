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
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * @author Cédric Biancheri
 */
public class KvmEntryHandler extends VMKernelEventHandler {

    public KvmEntryHandler(IKernelAnalysisEventLayout layout, FusedVirtualMachineStateProvider sp) {
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
        if (host == null) {
            return;
        }

        int currentCPUNode = FusedVMEventHandlerUtils.getCurrentCPUNode(cpu, ss);
        /*
         * Shortcut for the "current thread" attribute node. It requires
         * querying the current CPU's current thread.
         */
        int quark = ss.getQuarkRelativeAndAdd(currentCPUNode, FusedAttributes.CURRENT_THREAD);

        ITmfStateValue value = ss.queryOngoingState(quark);
        int thread = value.isNull() ? -1 : value.unboxInt();

        thread = VirtualCPU.getVirtualCPU(host, cpu.longValue()).getCurrentThread().unboxInt();
        if (thread == -1) {
            return;
        }

        /* Special case where host is also a guest. */
        if (host.isHost() && host.isGuest()) {
            /*
             * We are in L1. We are going to look for the vcpu of L2 we want to
             * launch and keep it for later.
             */
            /* We need our actual VM's vcpu. */
            VirtualCPU hostCpu = VirtualCPU.getVirtualCPU(host, cpu.longValue());

            /* The corresponding thread object. */
            HostThread ht = new HostThread(event.getTrace().getHostId(), thread);
            /* To get the vcpu of L2. */
            VirtualCPU nextLayerVCPU = sp.getVirtualCpu(ht);
            /* And keep it in the vcpu of L1. L0 will use it later. */
            hostCpu.setNextLayerVCPU(nextLayerVCPU);
            if (nextLayerVCPU != null) {
                /*
                 * Get the next layer vm to then remember which thread runs its
                 * vcpu.
                 */
                VirtualMachine nextLayerVM = nextLayerVCPU.getVm();

                /*
                 * If not already done, associate the TID in the host corresponding to
                 * the vCPU inside the state system.
                 */
                int quarkVCPUs = FusedVMEventHandlerUtils.getMachineCPUsNode(ss, nextLayerVM.getHostId());
                int quarkVCPU = ss.getQuarkRelativeAndAdd(quarkVCPUs, nextLayerVCPU.getCpuId().toString());
                if (ss.queryOngoingState(quarkVCPU).isNull()) {
                    ss.modifyAttribute(sp.getStartTime(), TmfStateValue.newValueInt(thread), quarkVCPU);
                }
            }

            /*
             * We also need to tell L0 that its thread running this vcpu of L1
             * wants to run L2, so that we are waiting for a kvm_mmu_get_page.
             */
            VirtualMachine parent = host.getParent();
            if (parent == null) {
                /* This should not happen. */
                System.err.println("Parent not found in KvmEntryHandler. This should never happen."); //$NON-NLS-1$
                return;
            }
            HostThread parentThread = sp.getHostThreadFromVCpu(hostCpu);
            parent.addThreadWaitingForNextLayer(parentThread);

            /* Nothing else to do, get out of here. */
            return;
        }



        /* Add the condition in_vm in the state system. */
        quark = ss.getQuarkRelativeAndAdd(currentCPUNode, FusedAttributes.CONDITION);
        value = StateValues.CONDITION_IN_VM_VALUE;
        long timestamp = FusedVMEventHandlerUtils.getTimestamp(event);
        ss.modifyAttribute(timestamp, value, quark);


        /* Get the host CPU doing the kvm_entry. */
        VirtualCPU hostCpu = VirtualCPU.getVirtualCPU(host, cpu.longValue());
        /*
         * Saves the state. Will be restored after a kvm_exit.
         */
        quark = ss.getQuarkRelativeAndAdd(currentCPUNode, FusedAttributes.STATUS);
        ITmfStateValue ongoingState = ss.queryOngoingState(quark);
        hostCpu.setCurrentState(ongoingState);
        /*
         * Get the host thread to get the right virtual machine.
         */
        HostThread ht = new HostThread(event.getTrace().getHostId(), thread);
        VirtualMachine virtualMachine = sp.getVmFromHostThread(ht);
        if (virtualMachine == null) {
            return;
        }

        VirtualCPU vcpu = sp.getVirtualCpu(ht);
        if (vcpu == null) {
            return;
        }

        /* Check if we need to jump to the next layer. */
        if (host.isThreadReadyForNextLayer(ht)) {
            /*
             * Then we need to go to the next layer by replacing the vcpu and
             * the vm by the one in the next layer.
             */
            vcpu = vcpu.getNextLayerVCPU();
            if (vcpu == null) {
                return;
            }
            virtualMachine = vcpu.getVm();
        } else {
            /*
             * If not already done, associate the TID in the host corresponding to
             * the vCPU inside the state system. We only do that if we are not going to the next layer.
             */
            int quarkVCPUs = FusedVMEventHandlerUtils.getMachineCPUsNode(ss, virtualMachine.getHostId());
            int quarkVCPU = ss.getQuarkRelativeAndAdd(quarkVCPUs, vcpu.getCpuId().toString());
            if (ss.queryOngoingState(quarkVCPU).isNull()) {
                ss.modifyAttribute(timestamp, TmfStateValue.newValueInt(thread), quarkVCPU);
            }
        }
        /* Now we put this vcpu on the pcpu. */

        /* Remember that this VM is using this pcpu. */
        int quarkPCPUs = FusedVMEventHandlerUtils.getMachinepCPUsNode(ss, virtualMachine.getHostId());
        ss.getQuarkRelativeAndAdd(quarkPCPUs, cpu.toString());

        Integer currentVCpu = vcpu.getCpuId().intValue();

        /* Set the value of the vcpu that is going to run. */
        int quarkVCpu = ss.getQuarkRelativeAndAdd(currentCPUNode, FusedAttributes.VIRTUAL_CPU);
        ITmfStateValue valueVCpu = TmfStateValue.newValueInt(currentVCpu);
        ss.modifyAttribute(timestamp, valueVCpu, quarkVCpu);

        /*
         * Set the name of the VM that will run just after the kvm_entry
         */
        int machineNameQuark = ss.getQuarkRelativeAndAdd(currentCPUNode, FusedAttributes.MACHINE_NAME);
        value = TmfStateValue.newValueString(virtualMachine.getHostId());
        ss.modifyAttribute(timestamp, value, machineNameQuark);

        /*
         * Then the current state of the vm is restored.
         */
        value = vcpu.getCurrentState();
        ss.modifyAttribute(timestamp, value, quark);

        /*
         * Save the current thread of the host that was running.
         */
        quark = ss.getQuarkRelativeAndAdd(currentCPUNode, FusedAttributes.CURRENT_THREAD);
        ongoingState = ss.queryOngoingState(quark);
        hostCpu.setCurrentThread(ongoingState);
        /* Restore the thread of the VM that was running. */
        value = vcpu.getCurrentThread();
        ss.modifyAttribute(timestamp, value, quark);
    }
}
