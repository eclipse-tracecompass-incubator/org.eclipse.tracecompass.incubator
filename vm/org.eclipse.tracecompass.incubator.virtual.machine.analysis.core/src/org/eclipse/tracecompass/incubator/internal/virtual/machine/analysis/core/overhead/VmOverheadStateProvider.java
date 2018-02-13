/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.overhead;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxTidAspect;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.DefaultEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ProcessStatusInterval;
import org.eclipse.tracecompass.incubator.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.incubator.analysis.core.model.ModelManager;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.IVirtualMachineModel;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualCPU;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualMachine;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.qemukvm.QemuKvmVmModel;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * State provider for VM overhead analysis. At the first level is the status of
 * a thread from a guest perspective, level 2 detail when the thread is in VMM
 * mode or its VCPU is preempted by the host and level 3 gives reasons or status
 * on that preemption.
 *
 * @author Geneviève Bastien
 */
public class VmOverheadStateProvider extends AbstractTmfStateProvider {

    /**
     * The threads attribute in the state system
     */
    public static final String TRACES = "Traces"; //$NON-NLS-1$

    /**
     * The threads attribute in the state system
     */
    public static final String THREADS = "Threads"; //$NON-NLS-1$

    /**
     * Version number of this state provider. Please bump this if you modify the
     * contents of the generated state history in some way.
     */
    private static final int VERSION = 3;

    private static final int SCHED_SWITCH_INDEX = 0;
    private static final int KVM_ENTRY_INDEX = 1;
    private static final int KVM_EXIT_INDEX = 2;

    private static final String LEVEL_1 = "1"; //$NON-NLS-1$
    private static final String LEVEL_2 = "2"; //$NON-NLS-1$
    private static final String LEVEL_3 = "3"; //$NON-NLS-1$
    private static final String STATUS_VCPU_PREEMPTED = "VCPU Preempted"; //$NON-NLS-1$
    private static final String STATUS_VMM_MODE = "VMM"; //$NON-NLS-1$
    private static final String STATUS_RUNNING = "Running"; //$NON-NLS-1$
    private static final String FIELD_EXIT_REASON = "exit_reason"; //$NON-NLS-1$

    /* TODO: An analysis should support many hypervisor models */
    private IVirtualMachineModel fModel;
    private final Table<ITmfTrace, String, @Nullable Integer> fEventNames;
    private final Map<ITmfTrace, IKernelAnalysisEventLayout> fLayouts;

    private final Map<HostThread, GuestKernelThreadStatuses> fGuestThreads = new HashMap<>();
    private final Map<HostThread, OverheadStatus> fPrevStatus = new HashMap<>();

    private class GuestKernelThreadStatuses {

        private final ITmfStateSystemBuilder fSs;
        private final int fQuark;
        private final Iterator<ProcessStatusInterval> fStatuses;
        private ProcessStatusInterval fCurrentStatus;

        public GuestKernelThreadStatuses(ITmfStateSystemBuilder ss, long start, Iterator<ProcessStatusInterval> statuses, int quark) {
            fSs = ss;
            fQuark = quark;
            fStatuses = statuses;
            fCurrentStatus = statuses.next();
            fSs.modifyAttribute(start, fCurrentStatus.getProcessStatus().name(), quark);
        }

        public boolean update(long start) {
            if (start <= fCurrentStatus.getEnd()) {
                return true;
            }
            if (fStatuses.hasNext()) {
                fCurrentStatus = fStatuses.next();
                fSs.modifyAttribute(fCurrentStatus.getStart(), fCurrentStatus.getProcessStatus().name(), fQuark);
                return true;
            }
            fSs.removeAttribute(fCurrentStatus.getEnd(), fQuark);
            return false;
        }

    }

    private class OverheadStatus {
        private final @Nullable Object fLevel2;
        private final @Nullable Object fLevel3;

        public OverheadStatus(@Nullable Object level2, @Nullable  Object level3) {
            fLevel2 = level2;
            fLevel3 = level3;
        }
    }

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Constructor
     *
     * @param experiment
     *            The virtual machine experiment
     */
    public VmOverheadStateProvider(TmfExperiment experiment) {
        super(experiment, "Vm Overhead State Provider"); //$NON-NLS-1$

        fModel = QemuKvmVmModel.get(experiment);
        Table<ITmfTrace, String, @Nullable Integer> table = HashBasedTable.create();
        fEventNames = table;
        fLayouts = new HashMap<>();
    }

    // ------------------------------------------------------------------------
    // Event names management
    // ------------------------------------------------------------------------

    private void buildEventNames(ITmfTrace trace) {
        IKernelAnalysisEventLayout layout;
        if (trace instanceof IKernelTrace) {
            layout = ((IKernelTrace) trace).getKernelEventLayout();
        } else {
            /* Fall-back to the base LttngEventLayout */
            layout = DefaultEventLayout.getInstance();
        }
        fLayouts.put(trace, layout);
        fEventNames.put(trace, layout.eventSchedSwitch(), SCHED_SWITCH_INDEX);
        for (String kvmEntry : layout.eventsKVMEntry()) {
            fEventNames.put(trace, kvmEntry, KVM_ENTRY_INDEX);
        }
        for (String kvmExit : layout.eventsKVMExit()) {
            fEventNames.put(trace, kvmExit, KVM_EXIT_INDEX);
        }
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public TmfExperiment getTrace() {
        ITmfTrace trace = super.getTrace();
        if (trace instanceof TmfExperiment) {
            return (TmfExperiment) trace;
        }
        throw new IllegalStateException("VirtualMachineStateProvider: The associated trace should be an experiment"); //$NON-NLS-1$
    }

    @Override
    public ITmfStateProvider getNewInstance() {
        TmfExperiment trace = getTrace();
        return new VmOverheadStateProvider(trace);
    }

    @Override
    protected void eventHandle(ITmfEvent event) {

        Set<HostThread> toRemove = new HashSet<>();
        fGuestThreads.entrySet().forEach(t -> {
            if (!t.getValue().update(event.getTimestamp().toNanos())) {
                toRemove.add(t.getKey());
            }
        });
        toRemove.forEach(ht -> fGuestThreads.remove(ht));

        /* Is the event managed by this analysis */
        final String eventName = event.getName();
        IKernelAnalysisEventLayout eventLayout = fLayouts.get(event.getTrace());
        if (eventLayout == null) {
            buildEventNames(event.getTrace());
            eventLayout = fLayouts.get(event.getTrace());
            if (eventLayout == null) {
                return;
            }
        }

        if (!eventName.equals(eventLayout.eventSchedSwitch()) &&
                !fModel.getRequiredEvents(eventLayout).contains(eventName)) {
            return;
        }

        ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());

        /* Have the hypervisor models handle the event first */
        fModel.handleEvent(event, eventLayout);
        // The model should have been populated by the dependent analysis, we can just use it
        VirtualMachine host = fModel.getCurrentMachine(event);
        if (host == null) {
            return;
        }
        Integer idx = fEventNames.get(event.getTrace(), eventName);
        int intval = (idx == null ? -1 : idx.intValue());
        switch (intval) {
        case SCHED_SWITCH_INDEX: // "sched_switch":
            if (host.isHost()) {
                handleHostSchedSwitch(ss, event, eventLayout);
            }
            if (host.isGuest()) {
                handleGuestSchedSwitch(ss, event, eventLayout, host);
            }
            break;
        case KVM_ENTRY_INDEX:
            handleKvmEntry(ss, event);
            break;
        case KVM_EXIT_INDEX:
            handleKvmExit(ss, event);
            break;
        default:
            // Nothing to do
        }

    }

    private void handleKvmEntry(ITmfStateSystemBuilder ss, ITmfEvent event) {
        Integer currentTid = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), LinuxTidAspect.class, event);
        if (currentTid == null || currentTid <= 0) {
            return;
        }
        HostThread ht = new HostThread(event.getTrace().getHostId(), currentTid);
        VirtualCPU vcpu = fModel.getVirtualCpu(ht);

        if (vcpu != null) {
            final long ts = event.getTimestamp().getValue();
            VirtualMachine vm = vcpu.getVm();
            IHostModel model = ModelManager.getModelFor(vm.getHostId());
            int guestTid = model.getThreadOnCpu(vcpu.getCpuId().intValue(), ts);
            if (guestTid != IHostModel.UNKNOWN_TID) {
                int quark = ss.getQuarkAbsoluteAndAdd(TRACES, vm.getTraceName(), THREADS, buildThreadAttributeName(guestTid, vcpu.getCpuId().intValue()), InstrumentedCallStackAnalysis.CALL_STACK);
                // Just make sure this attribute exists, at the beginning of trace or if lost
                // events, it may not
                int tidQuark = ss.getQuarkRelativeAndAdd(quark, LEVEL_1);
                if (ss.queryOngoing(tidQuark) == null) {
                    HostThread hostThread = new HostThread(event.getTrace().getHostId(), guestTid);
                    createGuestThreadStatus(ss, hostThread, ts, tidQuark);
                }
                int preemptQuark = ss.getQuarkRelativeAndAdd(quark, LEVEL_2);
                ss.removeAttribute(ts, preemptQuark);
                int statusQuark = ss.getQuarkRelativeAndAdd(quark, LEVEL_3);
                ss.removeAttribute(ts, statusQuark);
            }
        }
    }

    private void handleKvmExit(ITmfStateSystemBuilder ss, ITmfEvent event) {
        Integer currentTid = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), LinuxTidAspect.class, event);
        if (currentTid == null || currentTid <= 0) {
            return;
        }
        HostThread ht = new HostThread(event.getTrace().getHostId(), currentTid);
        VirtualCPU vcpu = fModel.getVirtualCpu(ht);
        if (vcpu != null) {
            final long ts = event.getTimestamp().getValue();
            Long exitReason = event.getContent().getFieldValue(Long.class, FIELD_EXIT_REASON);
            VirtualMachine vm = vcpu.getVm();
            IHostModel model = ModelManager.getModelFor(vm.getHostId());
            int guestTid = model.getThreadOnCpu(vcpu.getCpuId().intValue(), ts);
            if (guestTid != IHostModel.UNKNOWN_TID) {
                int quark = ss.getQuarkAbsoluteAndAdd(TRACES, vm.getTraceName(), THREADS, buildThreadAttributeName(guestTid, vcpu.getCpuId().intValue()), InstrumentedCallStackAnalysis.CALL_STACK);
                // Just make sure this attribute exists, at the beginning of trace or if lost
                // events, it may not
                int tidQuark = ss.getQuarkRelativeAndAdd(quark, LEVEL_1);
                if (ss.queryOngoing(tidQuark) == null) {
                    HostThread hostThread = new HostThread(event.getTrace().getHostId(), guestTid);
                    createGuestThreadStatus(ss, hostThread, ts, tidQuark);
                }
                int preemptQuark = ss.getQuarkRelativeAndAdd(quark, LEVEL_2);
                ss.modifyAttribute(ts, STATUS_VMM_MODE, preemptQuark);
                int statusQuark = ss.getQuarkRelativeAndAdd(quark, LEVEL_3);
                ss.modifyAttribute(ts, String.valueOf(exitReason), statusQuark);
            }
        }
    }

    private static String buildThreadAttributeName(int threadId, int cpuId) {
        if (threadId == 0) {
            if (cpuId < 0) {
                return String.valueOf(threadId);
            }
            return String.valueOf(threadId) + '_' + String.valueOf(cpuId);
        }

        return String.valueOf(threadId);
    }

    private void createGuestThreadStatus(ITmfStateSystemBuilder ss, HostThread ht, long start, int tidQuark) {
        IHostModel model = ModelManager.getModelFor(ht.getHost());
        Iterator<ProcessStatusInterval> statuses = model.getThreadStatusIntervals(ht.getTid(), start, Long.MAX_VALUE, 1);
        if (statuses.hasNext()) {
            GuestKernelThreadStatuses threadStatus = new GuestKernelThreadStatuses(ss, start, statuses, tidQuark);
            fGuestThreads.put(ht, threadStatus);
        }
    }

    /**
     * For guest sched_switch, update the status of the previous and next TIDs
     * @param host
     */
    private void handleGuestSchedSwitch(ITmfStateSystemBuilder ss, ITmfEvent event, IKernelAnalysisEventLayout eventLayout, VirtualMachine host) {
        final ITmfEventField content = event.getContent();
        final long ts = event.getTimestamp().getValue();
        int prevTid = ((Long) content.getField(eventLayout.fieldPrevTid()).getValue()).intValue();
        int nextTid = ((Long) content.getField(eventLayout.fieldNextTid()).getValue()).intValue();
        Object cpuObj = TmfTraceUtils.resolveEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);
        int cpu = (cpuObj == null ? -1 : (Integer) cpuObj);

        int baseQuark = ss.getQuarkAbsoluteAndAdd(TRACES, event.getTrace().getName(), THREADS, buildThreadAttributeName(prevTid, cpu));
        // Remove host tid
        int quark = ss.getQuarkRelativeAndAdd(baseQuark, VmOverheadAnalysis.HOST_CPU_TID);
        ss.removeAttribute(ts, quark);

        quark = ss.getQuarkRelativeAndAdd(baseQuark, InstrumentedCallStackAnalysis.CALL_STACK);
        int tidQuark = ss.getQuarkRelativeAndAdd(quark, LEVEL_1);
        ss.removeAttribute(ts, tidQuark);
        int preemptQuark = ss.getQuarkRelativeAndAdd(quark, LEVEL_2);
        ss.removeAttribute(ts, preemptQuark);
        int statusQuark = ss.getQuarkRelativeAndAdd(quark, LEVEL_3);
        ss.removeAttribute(ts, statusQuark);

        fGuestThreads.remove(new HostThread(event.getTrace().getHostId(), prevTid));

        baseQuark = ss.getQuarkAbsoluteAndAdd(TRACES, event.getTrace().getName(), THREADS, buildThreadAttributeName(nextTid, cpu));
        if (cpuObj instanceof Integer) {
            // Find the host thread of the current cpu
            VirtualCPU virtualCPU = VirtualCPU.getVirtualCPU(host, ((Integer) cpuObj).longValue());
            HostThread vcpuTid = fModel.getVirtualCpuTid(virtualCPU);
            if (vcpuTid != null) {
                quark = ss.getQuarkRelativeAndAdd(baseQuark, VmOverheadAnalysis.HOST_CPU_TID);
                ss.modifyAttribute(ts, vcpuTid.getTid(), quark);
            }
        }

        quark = ss.getQuarkRelativeAndAdd(baseQuark, InstrumentedCallStackAnalysis.CALL_STACK);
        tidQuark = ss.getQuarkRelativeAndAdd(quark, LEVEL_1);
        if (ss.queryOngoing(tidQuark) == null) {
            ss.modifyAttribute(ts, STATUS_RUNNING, tidQuark);
        }
        preemptQuark = ss.getQuarkRelativeAndAdd(quark, LEVEL_2);
        ss.removeAttribute(ts, preemptQuark);
        statusQuark = ss.getQuarkRelativeAndAdd(quark, LEVEL_3);
        ss.removeAttribute(ts, statusQuark);

        HostThread hostThread = new HostThread(event.getTrace().getHostId(), nextTid);
        createGuestThreadStatus(ss, hostThread, ts, tidQuark);
    }

    /**
     * If the previous or next TID is one of a vcpu, update the preemption status of
     * the thread running on that CPU in the guest
     */
    private void handleHostSchedSwitch(ITmfStateSystemBuilder ss, ITmfEvent event, IKernelAnalysisEventLayout eventLayout) {

        final ITmfEventField content = event.getContent();
        final String hostId = event.getTrace().getHostId();
        final long ts = event.getTimestamp().getValue();
        int prevTid = ((Long) content.getField(eventLayout.fieldPrevTid()).getValue()).intValue();
        int nextTid = ((Long) content.getField(eventLayout.fieldNextTid()).getValue()).intValue();
        Long prevState = content.getFieldValue(Long.class, eventLayout.fieldPrevState());

        /* Verify if the previous thread corresponds to a virtual CPU */
        /*
         * If previous thread is virtual CPU, update status of the
         * virtual CPU to preempted
         */
        VirtualCPU vcpu = prevTid == 0 ? null : fModel.getVirtualCpu(new HostThread(hostId, prevTid));
        if (vcpu != null) {
            VirtualMachine vm = vcpu.getVm();
            IHostModel model = ModelManager.getModelFor(vm.getHostId());
            int guestTid = model.getThreadOnCpu(vcpu.getCpuId().intValue(), ts);
            if (guestTid != IHostModel.UNKNOWN_TID) {

                int quark = ss.getQuarkAbsoluteAndAdd(TRACES, vm.getTraceName(), THREADS, buildThreadAttributeName(guestTid, vcpu.getCpuId().intValue()), InstrumentedCallStackAnalysis.CALL_STACK);
                // Just make sure this attribute exists, at the beginning of trace or if lost events, it may not
                ss.getQuarkRelativeAndAdd(quark, LEVEL_1);
                int preemptQuark = ss.getQuarkRelativeAndAdd(quark, LEVEL_2);
                int statusQuark = ss.getQuarkRelativeAndAdd(quark, LEVEL_3);
                // Save the previous statuses for this thread
                fPrevStatus.put(new HostThread(vm.getHostId(), guestTid), new OverheadStatus(ss.queryOngoing(preemptQuark), ss.queryOngoing(statusQuark)));

                // Set the overhead as vcpu preempted
                ss.modifyAttribute(ts, STATUS_VCPU_PREEMPTED, preemptQuark);
                if (prevState != null) {
                    ss.modifyAttribute(ts, String.valueOf(prevState), statusQuark);
                }
            }
        }

        vcpu = nextTid == 0 ? null : fModel.getVirtualCpu(new HostThread(hostId, nextTid));
        if (vcpu != null) {
            VirtualMachine vm = vcpu.getVm();
            IHostModel model = ModelManager.getModelFor(vm.getHostId());
            int guestTid = model.getThreadOnCpu(vcpu.getCpuId().intValue(), ts);
            if (guestTid != IHostModel.UNKNOWN_TID) {
                int quark = ss.getQuarkAbsoluteAndAdd(TRACES, vm.getTraceName(), THREADS, buildThreadAttributeName(guestTid, vcpu.getCpuId().intValue()), InstrumentedCallStackAnalysis.CALL_STACK);
                int tidQuark = ss.getQuarkRelativeAndAdd(quark, LEVEL_1);
                if (ss.queryOngoing(tidQuark) == null) {
                    ss.modifyAttribute(ts, STATUS_RUNNING, tidQuark);
                }
                // Reset to the previous overhead status
                int preemptQuark = ss.getQuarkRelativeAndAdd(quark, LEVEL_2);
                int statusQuark = ss.getQuarkRelativeAndAdd(quark, LEVEL_3);
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
}
