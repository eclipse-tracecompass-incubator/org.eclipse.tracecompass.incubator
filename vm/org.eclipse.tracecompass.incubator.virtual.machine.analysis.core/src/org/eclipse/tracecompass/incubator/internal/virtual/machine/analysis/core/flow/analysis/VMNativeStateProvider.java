/*******************************************************************************
 * Copyright (c) 2026 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.Activator;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 * State provider for VM/Native comparison analysis
 *
 * @author Francois Belias
 *
 */
public class VMNativeStateProvider extends AbstractTmfStateProvider{
    private static final int VERSION = 1;
    private static final String ID = "org.eclipse.tracecompass.incubator.vm.state.provider"; //$NON-NLS-1$

    /** State system attribute */

    private static final String PID = "context._vpid"; //$NON-NLS-1$
    private static final String TID = "context._vtid"; //$NON-NLS-1$
    private static final String PROCESS_NAME = "context._procname"; //$NON-NLS-1$
    private static final String CPUID = "context.cpu_id"; //$NON-NLS-1$
    private static final String VCPUID = "vcpu_id"; //$NON-NLS-1$
    private static final String EXIT_REASON = "exit_reason";  //$NON-NLS-1$
    private static final String MARKER = "./VM_ANALYSIS.txt"; //$NON-NLS-1$
    private static final String EVENT_MARKER = "syscall_entry_openat"; //$NON-NLS-1$
    private boolean begin_native = true;
    private boolean begin_vm = true;

    private SyncPoint nativeStart = null;
    private SyncPoint vmStart = null;
    private Map<TraceType, Map<Integer, ProcessFlowInfo>> flows = new HashMap<>();

    private VMExecutionState vmState = new VMExecutionState();

    /**
     * Constructor
     *
     * @param experiment : the experiment contains all the traces
     */
    public VMNativeStateProvider(@NonNull TmfExperiment experiment) {
        super(experiment, ID);
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    private static TraceType determineTraceType(String traceName) {
        if (traceName.toLowerCase().contains("native/kernel")) { //$NON-NLS-1$
            return TraceType.NATIVE_KERNEL;
        } else if (traceName.toLowerCase().contains("native/ust")){ //$NON-NLS-1$
            return TraceType.NATIVE_UST;
        } else if (traceName.toLowerCase().contains("guest/kernel")) { //$NON-NLS-1$
            return TraceType.VM_GUEST_KERNEL;
        } else if (traceName.toLowerCase().contains("guest/ust")) { //$NON-NLS-1$
            return TraceType.VM_GUEST_UST;
        } else if (traceName.toLowerCase().contains("host")) { //$NON-NLS-1$
            return TraceType.VM_HOST;
        }
        return TraceType.UNKNOWN;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new VMNativeStateProvider((TmfExperiment) getTrace());
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        ITmfStateSystemBuilder ss = getStateSystemBuilder();
        if (ss == null) {
            return;
        }

        try {
            String traceName = event.getTrace().getName();
            TraceType source = determineTraceType(traceName);

            if (isWorkloadEvent(event)) {
                handleWorkloadEvent(event, source);
            } else {
                handleKernelEvent(event, source, ss);
            }

        } catch (RuntimeException e) {
            // Log or handle specific exceptions appropriately
            org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.Activator.getInstance().logError("Error processing event", e); //$NON-NLS-1$
        }
    }


    private static boolean isWorkloadEvent(ITmfEvent event) {
        if (!EVENT_MARKER.equals(event.getType().getName())) {
            return false;
        }

        Object filenameField = event.getContent().getField("filename"); //$NON-NLS-1$
        if (filenameField == null) {
            return false;
        }

        String value = filenameField.toString();
        int idx = value.indexOf('=');

        if (idx == -1 || idx == value.length() - 1) {
            return false;
        }

        String rightPart = value.substring(idx + 1);
        return rightPart.contains(MARKER);
    }


    private static String getExitReason(ITmfEvent event) {
        Object exitField = event.getContent().getField(EXIT_REASON);
        if (exitField == null) {
            return "UNKNOWN_EXIT_REASON"; //$NON-NLS-1$
        }

        String value = exitField.toString();
        String[] words = value.split("="); //$NON-NLS-1$
        if (words.length != 2) {
            return "UNKNOWN_EXIT_REASON"; //$NON-NLS-1$
        }

        try {
                int code = Integer.parseInt(words[1]);
                return ExitReasonMap.getExitReasonName(code);
            } catch (NumberFormatException e) {
                return "UNKNOWN_EXIT_REASON"; //$NON-NLS-1$
            }

    }

    private static Integer extractVcpuFromProcName(ITmfEvent event) {
        Object commField = event.getContent().getField(PROCESS_NAME);
        if (commField == null) {
            return null;
        }

        String procName = commField.toString();

        if (procName == null) {
            return null;
        }

        Pattern p = Pattern.compile("CPU (\\d+)/KVM"); //$NON-NLS-1$
        Matcher m = p.matcher(procName);

        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }

        return null;
    }


    // get the field PID/TID
    private static Integer getIntField(ITmfEvent event, String fieldName) {

        if (event.getType().getName().equals("kvm_x86_entry") && fieldName.equals(VCPUID)) { //$NON-NLS-1$
            return extractVcpuFromProcName(event);
        }



        Object obj = event.getContent().getField(fieldName);
        if (obj == null) {
            return null;
        }

        String value = obj.toString();
        String[] words = value.split("="); //$NON-NLS-1$
        if (words.length != 2) {
            return null;
        }

        try {
            return Integer.parseInt(words[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // get the process name
    private static String getProcessName(ITmfEvent event) {
        Object commField = event.getContent().getField(PROCESS_NAME);

        if (commField == null) {
            return "unknown"; //$NON-NLS-1$
        }

        String value = commField.toString();
        String[] words = value.split("="); //$NON-NLS-1$
        if (words.length != 2) {
            return "unknown"; //$NON-NLS-1$
        }

        return words[1];
    }

    private void handleWorkloadEvent(ITmfEvent event, TraceType source) {
        try {
            String eventName = event.getType().getName();
            Integer pid = getIntField(event, PID);
            String procName = getProcessName(event);
            long timestamp = event.getTimestamp().toNanos();

            if (eventName.equals(EVENT_MARKER) && source.equals(TraceType.NATIVE_KERNEL)) {
                if (this.begin_native) {
                    this.nativeStart = new SyncPoint(timestamp,
                            pid != null ? pid : -1);
                    this.begin_native = false;
                    String phase = "workload"; //$NON-NLS-1$
                    flows.computeIfAbsent(source, t -> new HashMap<>())
                            .computeIfAbsent(pid, p -> new ProcessFlowInfo(phase, procName));

                } else {

                    // finalizeFlow()
                    Map<Integer, ProcessFlowInfo> typeFlows = flows.get(source);
                    if (typeFlows != null && pid != null) {
                        ProcessFlowInfo flow = typeFlows.get(pid);
                        if (flow != null) {
                            flow.finalizeFlow();
                        }
                    }
                    this.nativeStart = null;
                }

            } else if (eventName.equals(EVENT_MARKER) && source.equals(TraceType.VM_GUEST_KERNEL)) {
                if (this.begin_vm) {
                    this.vmStart = new SyncPoint(timestamp,
                            pid != null ? pid : -1);
                    this.begin_vm = false;

                    String phase = "workload"; //$NON-NLS-1$
                    flows.computeIfAbsent(source, t -> new HashMap<>())
                            .computeIfAbsent(pid, p -> new ProcessFlowInfo(phase, procName));

                } else {

                    // finalizeFlow()
                    Map<Integer, ProcessFlowInfo> typeFlows = flows.get(source);
                    if (typeFlows != null && pid != null) {
                        ProcessFlowInfo flow = typeFlows.get(pid);
                        if (flow != null) {
                            flow.finalizeFlow();
                        }
                    }
                    this.vmStart = null;
                }
            }

        } catch (Exception e) {
            Activator.getInstance().logError("Error handling workload event", e); //$NON-NLS-1$
        }
    }


    private void handleKernelEvent(ITmfEvent event, TraceType source, ITmfStateSystemBuilder ss) {
        try {
            if (ss == null) {
                return;
            }

            String name = event.getType().getName();
            Integer pid = getIntField(event, PID);
            Integer tid = getIntField(event, TID);
            Integer cpuid = getIntField(event, CPUID);
            Integer vcpuid = getIntField(event, VCPUID);
            String processName = getProcessName(event);
            String exitReason = getExitReason(event);
            long ts = event.getTimestamp().toNanos();

            KernelEventInfo evt = new KernelEventInfo(
                    name,
                    ts,
                    pid != null ? pid : -1,
                    tid != null ? tid : -1,
                    processName,
                    source,
                    cpuid != null ? cpuid : -1,
                    vcpuid != null ? vcpuid : -1,
                    exitReason
            );

            if (pid == null) {
                return;
            }

            if (source == TraceType.NATIVE_KERNEL && this.nativeStart != null && pid == this.nativeStart.pid) {
                Map<Integer, ProcessFlowInfo> typeFlows = flows.get(source);
                if (typeFlows != null) {
                    ProcessFlowInfo flow = typeFlows.get(pid);
                    if (flow != null) {
                        flow.addEvent(evt);
                        updateFlowStateSystem(ss, "native_flow", pid, ts, name); //$NON-NLS-1$
                    }
                }

            } else if (source == TraceType.VM_GUEST_KERNEL && this.vmStart != null && pid == this.vmStart.pid) {
                Map<Integer, ProcessFlowInfo> typeFlows = flows.get(source);
                if (typeFlows != null) {
                    ProcessFlowInfo flow = typeFlows.get(pid);
                    if (flow != null) {
                        flow.addGuestEvent(evt);
                        updateFlowStateSystem(ss, "vm_flow", pid, ts, name); //$NON-NLS-1$
                    }
                }

            } else if (source == TraceType.VM_HOST && this.vmStart != null && this.vmStart.timestamp < ts) {
                Map<Integer, ProcessFlowInfo> typeFlows = flows.get(TraceType.VM_GUEST_KERNEL);
                if (typeFlows != null) {
                    ProcessFlowInfo flow = typeFlows.get(this.vmStart.pid);
                    if (flow != null) {
                        EventType type = getEventType(evt);
                        if (processHypervisorEvents(flow, type, evt)) {
                            updateFlowStateSystem(ss, "vm_flow", this.vmStart.pid, ts, name); //$NON-NLS-1$
                        }
                    }
                }
            }

        } catch (Exception e) {
            Activator.getInstance().logError("Error handling workload event", e); //$NON-NLS-1$
        }
    }

    private static void updateFlowStateSystem(ITmfStateSystemBuilder ss, String flowRoot, int pid, long ts, String eventName) {
        int flowQuark = ss.getQuarkAbsoluteAndAdd(flowRoot, Integer.toString(pid));
        int lastEventQuark = ss.getQuarkRelativeAndAdd(flowQuark, "event"); //$NON-NLS-1$
        int eventCountQuark = ss.getQuarkRelativeAndAdd(flowQuark, "event_count"); //$NON-NLS-1$

        ss.modifyAttribute(ts, eventName, lastEventQuark);

        Object currentCount = ss.queryOngoingState(eventCountQuark);
        int newCount = (currentCount instanceof Integer) ? ((Integer) currentCount) + 1 : 1;
        ss.modifyAttribute(ts, newCount, eventCountQuark);
    }

    private static EventType getEventType(KernelEventInfo event) {
        if (isVMExit(event)) {
            return EventType.VM_EXIT;
        } else if (isVMEntry(event)) {
            return EventType.VM_ENTRY;
        }

        return EventType.HOST;
    }

    private boolean processHypervisorEvents(ProcessFlowInfo processFlow, EventType evtType, KernelEventInfo event) {

            switch (evtType) {
            case VM_ENTRY:
                vmState.enterGuest();
                return processFlow.addVMTransition(event, false);

            case VM_EXIT:
                vmState.exitGuest(event);
                return processFlow.addVMTransition(event, true);

            case GUEST:
                return false;

            case HOST:
                if (vmState.isInHypervisorOverhead()
                        && isHostEventRelevant(event)) {
                       return processFlow.addHypervisorEvent(event, vmState.getLastExitTimestamp());
                       }
                return false;
        default:
            return false;
        }
    }


    @Override
    public void done() {
        super.done();
    }



    // Check if a host event is relevant during hypervisor overhead
    private boolean isHostEventRelevant(KernelEventInfo hostEvent) {
        // Accept events on the CPU that handled the VM exit
        return hostEvent.cpuid == this.vmState.getLastExitCpuId();

    }

    /**
     * Check if an event is a VM exit
     */
    private static boolean isVMExit(KernelEventInfo event) {
        return event.name.contains("kvm_x86_exit"); //$NON-NLS-1$

    }

    /**
     * Check if an event is a VM entry
     */
    private static boolean isVMEntry(KernelEventInfo event) {
        return event.name.contains("kvm_x86_entry"); //$NON-NLS-1$

    }

    /**
     * Represent a synchronization point in the trace
     */
    private static class SyncPoint {
        final long timestamp;
        final int pid;
        SyncPoint(long timestamp, int pid) {
            this.timestamp = timestamp;
            this.pid = pid;
        }
    }


    private enum EventType {
        VM_ENTRY, VM_EXIT, GUEST, HOST
    }

    // Helper class to track VM execution state with migration awareness
    private static class VMExecutionState {
        private boolean inGuest = false;
        private long lastExitTimestamp = -1;
        private int lastExitCpuId = -1;
        void enterGuest() {
            inGuest = true;
        }

        void exitGuest(KernelEventInfo vmExit) {
            inGuest = false;
            lastExitTimestamp = vmExit.timestamp;
            lastExitCpuId = vmExit.cpuid;
        }

        boolean isInHypervisorOverhead() {
            return !inGuest && lastExitTimestamp != -1;
        }

        long getLastExitTimestamp() {
            return lastExitTimestamp;
        }

        int getLastExitCpuId() {
            return lastExitCpuId;
        }
    }

}