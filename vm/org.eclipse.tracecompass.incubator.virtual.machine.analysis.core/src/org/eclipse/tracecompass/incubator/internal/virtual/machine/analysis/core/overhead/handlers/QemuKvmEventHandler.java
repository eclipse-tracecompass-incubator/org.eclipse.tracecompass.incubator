/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.overhead.handlers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.incubator.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.incubator.analysis.core.model.ModelManager;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.IVirtualMachineEventHandler;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.IVirtualEnvironmentModel;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualCPU;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualMachine;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.overhead.VmOverheadStateProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Handle events coming from a qemu/kvm hypervisor
 *
 * @author Geneviève Bastien
 */
public class QemuKvmEventHandler implements IVirtualMachineEventHandler {

    private static final String FIELD_EXIT_REASON = "exit_reason"; //$NON-NLS-1$

    private final Map<IKernelAnalysisEventLayout, Set<String>> fRequiredEvents = new HashMap<>();

    private final VmOverheadStateProvider fProvider;

    /**
     * Constructor
     *
     * @param provider
     *            The state provider
     */
    public QemuKvmEventHandler(VmOverheadStateProvider provider) {
        fProvider = provider;
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
            handleKvmEvent(ss, ts, event, virtEnv, null, null);
        } else if (layout.eventsKVMExit().contains(eventName)) {
            // The vcpu is entering hypervisor mode
            Long exitReason = event.getContent().getFieldValue(Long.class, FIELD_EXIT_REASON);
            handleKvmEvent(ss, ts, event, virtEnv, VmOverheadStateProvider.STATUS_VMM_MODE, String.valueOf(exitReason));
        }
    }

    private void handleKvmEvent(ITmfStateSystemBuilder ss, long ts, ITmfEvent event, IVirtualEnvironmentModel virtEnv, @Nullable Object level2, @Nullable Object level3) {
        HostThread ht = IVirtualMachineEventHandler.getCurrentHostThread(event, ts);
        if (ht == null) {
            return;
        }
        VirtualCPU vcpu = virtEnv.getVirtualCpu(event, ht);
        if (vcpu == null) {
            // The current thread has a vcpu configured, ignore
            return;
        }

        VirtualMachine vm = vcpu.getVm();
        IHostModel model = ModelManager.getModelFor(vm.getHostId());
        int guestTid = model.getThreadOnCpu(vcpu.getCpuId().intValue(), ts);
        if (guestTid != IHostModel.UNKNOWN_TID) {
            int quark = ss.getQuarkAbsoluteAndAdd(VmOverheadStateProvider.TRACES, vm.getTraceName(), VmOverheadStateProvider.THREADS, VmOverheadStateProvider.buildThreadAttributeName(guestTid, vcpu.getCpuId().intValue()), InstrumentedCallStackAnalysis.CALL_STACK);
            // Just make sure this attribute exists, at the beginning of trace or if lost
            // events, it may not
            ss.getQuarkRelativeAndAdd(quark, VmOverheadStateProvider.LEVEL_1);
            int tidQuark = ss.getQuarkRelativeAndAdd(quark, VmOverheadStateProvider.LEVEL_1);
            if (ss.queryOngoing(tidQuark) == null) {
                HostThread hostThread = new HostThread(event.getTrace().getHostId(), guestTid);
                fProvider.createGuestThreadStatus(ss, hostThread, ts, tidQuark);
            }
            int preemptQuark = ss.getQuarkRelativeAndAdd(quark, VmOverheadStateProvider.LEVEL_2);
            ss.modifyAttribute(ts, level2, preemptQuark);
            int statusQuark = ss.getQuarkRelativeAndAdd(quark, VmOverheadStateProvider.LEVEL_3);
            ss.modifyAttribute(ts, level3, statusQuark);
        }

    }

}
