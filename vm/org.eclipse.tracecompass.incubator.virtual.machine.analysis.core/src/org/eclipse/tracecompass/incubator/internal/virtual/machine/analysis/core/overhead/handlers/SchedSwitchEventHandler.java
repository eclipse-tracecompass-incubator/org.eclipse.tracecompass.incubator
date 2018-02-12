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
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.overhead.VmOverheadAnalysis;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.overhead.VmOverheadStateProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Handle sched switches
 *
 * @author Geneviève Bastien
 */
public class SchedSwitchEventHandler implements IVirtualMachineEventHandler {

    private Map<IKernelAnalysisEventLayout, Set<String>> fRequiredEvents = new HashMap<>();

    private final Map<HostThread, OverheadStatus> fPrevStatus = new HashMap<>();

    private final VmOverheadStateProvider fProvider;

    private class OverheadStatus {
        private final @Nullable Object fLevel2;
        private final @Nullable Object fLevel3;

        public OverheadStatus(@Nullable Object level2, @Nullable  Object level3) {
            fLevel2 = level2;
            fLevel3 = level3;
        }
    }



    /**
     * Constructor
     * @param provider The state provider running the analysis
     */
    public SchedSwitchEventHandler(VmOverheadStateProvider provider) {
        fProvider = provider;
    }

    @Override
    public Set<String> getRequiredEvents(IKernelAnalysisEventLayout layout) {
        Set<String> events = fRequiredEvents.get(layout);
        if (events == null) {
            events = new HashSet<>();
            events.add(layout.eventSchedSwitch());
            fRequiredEvents.put(layout, events);
        }
        return events;
    }

    @Override
    public void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event, IVirtualEnvironmentModel virtEnv, IKernelAnalysisEventLayout eventLayout) {

        VirtualMachine currentMachine = virtEnv.getCurrentMachine(event);

        /*
         * If sched switch is from a guest, just update the status of the virtual CPU to
         * either idle or running
         */
        if (currentMachine.isHost()) {
            handleHostSchedSwitch(ss, event, virtEnv, eventLayout);
        }
        if (currentMachine.isGuest()) {
            handleGuestSchedSwitch(ss, event, virtEnv, eventLayout, currentMachine);
        }
    }

    /*
     * If the previous or next TID is one of a vcpu, update the preemption status of
     * the thread running on that CPU in the guest
     */
    private void handleHostSchedSwitch(ITmfStateSystemBuilder ss, ITmfEvent event, IVirtualEnvironmentModel virtEnv, IKernelAnalysisEventLayout eventLayout) {

        final ITmfEventField content = event.getContent();
        final String hostId = event.getTrace().getHostId();
        final long ts = event.getTimestamp().getValue();
        int prevTid = ((Long) content.getField(eventLayout.fieldPrevTid()).getValue()).intValue();
        int nextTid = ((Long) content.getField(eventLayout.fieldNextTid()).getValue()).intValue();
        Long prevState = content.getFieldValue(Long.class, eventLayout.fieldPrevState());

        /* Verify if the previous thread corresponds to a virtual CPU */
        /*
         * If previous thread is virtual CPU, update status of the virtual CPU to
         * preempted
         */
        VirtualCPU vcpu = prevTid == 0 ? null : virtEnv.getVirtualCpu(event, new HostThread(hostId, prevTid));
        if (vcpu != null) {
            VirtualMachine vm = vcpu.getVm();
            IHostModel model = ModelManager.getModelFor(vm.getHostId());
            int guestTid = model.getThreadOnCpu(vcpu.getCpuId().intValue(), ts);
            if (guestTid != IHostModel.UNKNOWN_TID) {

                int quark = ss.getQuarkAbsoluteAndAdd(VmOverheadStateProvider.TRACES, vm.getTraceName(), VmOverheadStateProvider.THREADS, VmOverheadStateProvider.buildThreadAttributeName(guestTid, vcpu.getCpuId().intValue()),
                        InstrumentedCallStackAnalysis.CALL_STACK);
                // Just make sure this attribute exists, at the beginning of trace or if lost
                // events, it may not
                int tidQuark = ss.getQuarkRelativeAndAdd(quark, VmOverheadStateProvider.LEVEL_1);
                if (ss.queryOngoing(tidQuark) == null) {
                    HostThread hostThread = new HostThread(event.getTrace().getHostId(), guestTid);
                    fProvider.createGuestThreadStatus(ss, hostThread, ts, tidQuark);
                }

                int preemptQuark = ss.getQuarkRelativeAndAdd(quark, VmOverheadStateProvider.LEVEL_2);
                int statusQuark = ss.getQuarkRelativeAndAdd(quark, VmOverheadStateProvider.LEVEL_3);
                // Save the previous statuses for this thread
                fPrevStatus.put(new HostThread(vm.getHostId(), guestTid), new OverheadStatus(ss.queryOngoing(preemptQuark), ss.queryOngoing(statusQuark)));

                ss.modifyAttribute(ts, VmOverheadStateProvider.STATUS_VCPU_PREEMPTED, preemptQuark);
                if (prevState != null) {
                    ss.modifyAttribute(ts, String.valueOf(prevState), statusQuark);
                }

            }
        }

        vcpu = nextTid == 0 ? null : virtEnv.getVirtualCpu(event, new HostThread(hostId, nextTid));
        if (vcpu != null) {
            VirtualMachine vm = vcpu.getVm();
            IHostModel model = ModelManager.getModelFor(vm.getHostId());
            int guestTid = model.getThreadOnCpu(vcpu.getCpuId().intValue(), ts);
            if (guestTid != IHostModel.UNKNOWN_TID) {
                int quark = ss.getQuarkAbsoluteAndAdd(VmOverheadStateProvider.TRACES, vm.getTraceName(), VmOverheadStateProvider.THREADS, VmOverheadStateProvider.buildThreadAttributeName(guestTid, vcpu.getCpuId().intValue()),
                        InstrumentedCallStackAnalysis.CALL_STACK);
                int tidQuark = ss.getQuarkRelativeAndAdd(quark, VmOverheadStateProvider.LEVEL_1);
                if (ss.queryOngoing(tidQuark) == null) {
                    ss.modifyAttribute(ts, VmOverheadStateProvider.STATUS_RUNNING, tidQuark);
                }
                int preemptQuark = ss.getQuarkRelativeAndAdd(quark, VmOverheadStateProvider.LEVEL_2);
                int statusQuark = ss.getQuarkRelativeAndAdd(quark, VmOverheadStateProvider.LEVEL_3);
                OverheadStatus overheadStatus = fPrevStatus.remove(new HostThread(vm.getHostId(), guestTid));
                if (overheadStatus == null) {
                    ss.removeAttribute(ts, preemptQuark);
                    ss.removeAttribute(ts, statusQuark);
                } else {
                    ss.modifyAttribute(ts, overheadStatus.fLevel2, preemptQuark);
                    ss.modifyAttribute(ts, overheadStatus.fLevel3, statusQuark);
                }
            }
        }
    }

    private void handleGuestSchedSwitch(ITmfStateSystemBuilder ss, ITmfEvent event, IVirtualEnvironmentModel virtEnv, IKernelAnalysisEventLayout eventLayout, VirtualMachine host) {
        final ITmfEventField content = event.getContent();
        final long ts = event.getTimestamp().getValue();
        int prevTid = ((Long) content.getField(eventLayout.fieldPrevTid()).getValue()).intValue();
        int nextTid = ((Long) content.getField(eventLayout.fieldNextTid()).getValue()).intValue();
        Object cpuObj = TmfTraceUtils.resolveEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);
        int cpu = (cpuObj == null ? -1 : (Integer) cpuObj);

        // Handle the process being scheduled out
        int baseQuark = ss.getQuarkAbsoluteAndAdd(VmOverheadStateProvider.TRACES, event.getTrace().getName(), VmOverheadStateProvider.THREADS, VmOverheadStateProvider.buildThreadAttributeName(prevTid, cpu));
        // Remove host tid running the previous thread
        int quark = ss.getQuarkRelativeAndAdd(baseQuark, VmOverheadAnalysis.HOST_CPU_TID);
        ss.removeAttribute(ts, quark);

        // The thread is not running anymore, remove all stack
        quark = ss.getQuarkRelativeAndAdd(baseQuark, InstrumentedCallStackAnalysis.CALL_STACK);
        int tidQuark = ss.getQuarkRelativeAndAdd(quark, VmOverheadStateProvider.LEVEL_1);
        ss.removeAttribute(ts, tidQuark);
        int preemptQuark = ss.getQuarkRelativeAndAdd(quark, VmOverheadStateProvider.LEVEL_2);
        ss.removeAttribute(ts, preemptQuark);
        int statusQuark = ss.getQuarkRelativeAndAdd(quark, VmOverheadStateProvider.LEVEL_3);
        ss.removeAttribute(ts, statusQuark);

        // Stop handling the guest thread status for previous thread
        fProvider.removeGuestThreadStatus(new HostThread(event.getTrace().getHostId(), prevTid));

        // Handle the next thread
        baseQuark = ss.getQuarkAbsoluteAndAdd(VmOverheadStateProvider.TRACES, event.getTrace().getName(), VmOverheadStateProvider.THREADS, VmOverheadStateProvider.buildThreadAttributeName(nextTid, cpu));
        if (cpu >= 0) {
            // Find the host thread of the current cpu and set it as host thread
            VirtualCPU virtualCPU = VirtualCPU.getVirtualCPU(host, Long.valueOf(cpu));
            HostThread vcpuTid = virtEnv.getVirtualCpuTid(virtualCPU);
            if (vcpuTid != null) {
                quark = ss.getQuarkRelativeAndAdd(baseQuark, VmOverheadAnalysis.HOST_CPU_TID);
                ss.modifyAttribute(ts, vcpuTid.getTid(), quark);
            }
        }

        quark = ss.getQuarkRelativeAndAdd(baseQuark, InstrumentedCallStackAnalysis.CALL_STACK);
        tidQuark = ss.getQuarkRelativeAndAdd(quark, VmOverheadStateProvider.LEVEL_1);
        if (ss.queryOngoing(tidQuark) == null) {
            ss.modifyAttribute(ts, VmOverheadStateProvider.STATUS_RUNNING, tidQuark);
        }
        preemptQuark = ss.getQuarkRelativeAndAdd(quark, VmOverheadStateProvider.LEVEL_2);
        ss.removeAttribute(ts, preemptQuark);
        statusQuark = ss.getQuarkRelativeAndAdd(quark, VmOverheadStateProvider.LEVEL_3);
        ss.removeAttribute(ts, statusQuark);

        // Create the iterator to update guest thread statuses
        HostThread hostThread = new HostThread(event.getTrace().getHostId(), nextTid);
        fProvider.createGuestThreadStatus(ss, hostThread, ts, tidQuark);

    }

}
