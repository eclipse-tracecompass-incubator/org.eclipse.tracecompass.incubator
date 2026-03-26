package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
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

    /** State system attributes */
    public static final String NATIVE_ROOT = "Native"; //$NON-NLS-1$
    @SuppressWarnings("javadoc")
    public static final String VM_ROOT = "VM";  //$NON-NLS-1$
    @SuppressWarnings("javadoc")
    public static final String SYNC_POINTS = "SyncPoints"; //$NON-NLS-1$
    @SuppressWarnings("javadoc")
    public static final String PERFORMANCE_DELTA = "PerformanceDelta"; //$NON-NLS-1$

    private final static Map<String, TraceContext> traceContexts = new HashMap<>();

    private static final String WORKLOAD_UST_PROVIDER = "workload_provider"; //$NON-NLS-1$

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
    private SyncPoint nativeEnd = null;
    private SyncPoint vmStart = null;
    private SyncPoint vmEnd = null;
    private Map<TraceType, Map<Integer, ProcessFlowInfo>> flows = new HashMap<>();

    private VMExecutionState vmState = new VMExecutionState();

    /**
     * Constructor
     *
     * @param experiment : the experiment contains all the traces
     */
    public VMNativeStateProvider(@NonNull TmfExperiment experiment) {
        super(experiment, ID);
        initializeTraceContexts(experiment);
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    private static void initializeTraceContexts(TmfExperiment experiment) {
        for (ITmfTrace trace: experiment.getTraces()) {
            String traceName = trace.getName();
            TraceType type  = determineTraceType(traceName);

            TraceContext context = new TraceContext(trace, type);
            traceContexts.put(traceName, context);
        }
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

            long ts = event.getTimestamp().toNanos();

            if  (this.nativeEnd != null && this.nativeEnd.timestamp == ts) {
                Integer pid = getIntField(event, PID);
                Map<Integer, ProcessFlowInfo> typeFlows = flows.get(source);
                if (typeFlows != null) {
                    ProcessFlowInfo flow = typeFlows.get(pid);
                    if (flow != null) {
                        flow.finalizeFlow();
                        flow.printUnifiedFlow();
                    }
                }

            } else if (this.vmEnd != null && this.vmEnd.timestamp == ts) {
                Integer pid = getIntField(event, PID);
                Map<Integer, ProcessFlowInfo> typeFlows = flows.get(source);
                if (typeFlows != null) {
                    ProcessFlowInfo flow = typeFlows.get(pid);
                    if (flow != null) {
                        flow.finalizeFlow();
                        flow.printUnifiedFlow();
                    }
                }
            }


            if (isWorkloadEvent(event)) {
                handleWorkloadEvent(event, source, ss);
            } else {
                handleKernelEvent(event, source, ss);
            }

        } catch (Exception a) {
            System.err.println("Error processing event: " + a.getMessage()); //$NON-NLS-1$
        }
    }


    private static boolean isWorkloadEvent(ITmfEvent event) {
        String eventName = event.getType().getName();

        if (eventName.equals(EVENT_MARKER)) {
            Object filenameField = event.getContent().getField("filename"); //$NON-NLS-1$
            if (filenameField == null) {
                return false;
            }

            String value = filenameField.toString();
            String[] words = value.split("="); //$NON-NLS-1$
            if (words.length == 0 || words.length > 2) {
                return false;
            }

            if (words[1].contains(MARKER)) {
                return true;
            }

            return false;
        }

        return eventName.startsWith(WORKLOAD_UST_PROVIDER + ":"); //$NON-NLS-1$
    }


    private static String getExitReason(ITmfEvent event) {
        Object exitField = event.getContent().getField(EXIT_REASON);
        if (exitField == null) {
            return "UNKNOWN_EXIT_REASON"; //$NON-NLS-1$
        }

        String value = exitField.toString();
        String[] words = value.split("="); //$NON-NLS-1$
        if (words.length == 0 || words.length > 2) {
            return null;
        }
        int code = Integer.parseInt(words[1]);
        return ExitReasonMap.getExitReasonName(code);

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
        if (words.length == 0 || words.length > 2) {
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
        if (words.length == 0 || words.length > 2) {
            return "unknown"; //$NON-NLS-1$
        }

        return words[1];
    }

    private void handleWorkloadEvent(ITmfEvent event, TraceType source, ITmfStateSystemBuilder ss) {
        try {

            if (ss == null) {

            }

            String eventName = event.getType().getName();
            Integer pid = getIntField(event, PID);
            String procName = getProcessName(event);
            long timestamp = event.getTimestamp().toNanos();

            if (eventName.equals(EVENT_MARKER) && source.equals(TraceType.NATIVE_KERNEL)) {
                if (this.begin_native) {
                    this.nativeStart = new SyncPoint("workload_start", timestamp, //$NON-NLS-1$
                            source, 0,
                            pid != null ? pid : -1, procName);

                    this.begin_native = false;

                } else {
                    this.nativeEnd = new SyncPoint("workload_end", timestamp, //$NON-NLS-1$
                            source, 0,
                            pid != null ? pid : -1, procName);

                    String phase = "workload"; //$NON-NLS-1$
                    flows.computeIfAbsent(source, t -> new HashMap<>())
                            .computeIfAbsent(pid, p -> new ProcessFlowInfo(phase, procName));
                }
                return;

            } else if (eventName.equals(EVENT_MARKER) && source.equals(TraceType.VM_GUEST_KERNEL)) {
                if (this.begin_vm) {
                    this.vmStart = new SyncPoint("workload_start", timestamp, //$NON-NLS-1$
                            source, 0,
                            pid != null ? pid : -1, procName);

                    this.begin_vm = false;
                    String phase = "workload"; //$NON-NLS-1$
                    flows.computeIfAbsent(source, t -> new HashMap<>())
                            .computeIfAbsent(pid, p -> new ProcessFlowInfo(phase, procName));
                } else {
                    this.vmEnd = new SyncPoint("workload_end", timestamp, //$NON-NLS-1$
                            source, 0,
                            pid != null ? pid : -1, procName);
                }
                return;
            }


        } catch (Exception e) {
            System.err.println("Error handling workload event: " + e.getMessage()); //$NON-NLS-1$
        }
    }


    private void handleKernelEvent(ITmfEvent event, TraceType source, ITmfStateSystemBuilder ss) {
        try {

                if (ss == null ) {
                    return;
                }
                String name = event.getType().getName();
                @NonNull
                Integer pid = getIntField(event, PID);
                Integer tid = getIntField(event, TID);
                Integer cpuid = getIntField(event, CPUID);
                Integer vcpuid = getIntField(event, VCPUID);
                String processName = getProcessName(event);
                String exitReason = getExitReason(event);
                long ts = event.getTimestamp().toNanos();

                //System.out.println("DEBUG: source=" + source + " (class=" + (source == null ? "null" : source.getClass().getName()) + "), TraceType.VM_HOST=" + TraceType.VM_HOST + ", source==VM_HOST? " + (source == TraceType.VM_HOST));

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

                if (source == TraceType.NATIVE_KERNEL) {
                    if (this.nativeStart != null) {
                        if (pid == this.nativeStart.pid) {
                            Map<Integer, ProcessFlowInfo> typeFlows = flows.get(source);
                            if (typeFlows != null) {
                                ProcessFlowInfo flow = typeFlows.get(pid);
                                if (flow != null) {
                                    flow.addEvent(evt);
                                    int natffFlowQuark = ss.getQuarkAbsoluteAndAdd("natif_flow", pid.toString()); //$NON-NLS-1$
                                    int lastEventQuark = ss.getQuarkRelativeAndAdd(natffFlowQuark, "event"); //$NON-NLS-1$
                                    int eventCountQuark = ss.getQuarkRelativeAndAdd(natffFlowQuark, "event_count"); //$NON-NLS-1$

                                    ss.modifyAttribute(ts, name, lastEventQuark);

                                    // Keep track of event count
                                    Object currentCount = ss.queryOngoingState(eventCountQuark);
                                    int newCount = (currentCount instanceof Integer) ? ((Integer) currentCount) + 1 : 1;
                                    ss.modifyAttribute(ts, newCount, eventCountQuark);
                                }
                            }
                        }
                    }
                } else if (source == TraceType.VM_GUEST_KERNEL) {
                    if (this.vmStart != null) {
                        if (pid == this.vmStart.pid) {
                            Map<Integer, ProcessFlowInfo> typeFlows = flows.get(source);
                            if (typeFlows != null) {
                                ProcessFlowInfo flow = typeFlows.get(pid);
                                if (flow != null) {
                                    flow.addGuestEvent(evt);

                                    int vmFlowQuark = ss.getQuarkAbsoluteAndAdd("vm_flow", pid.toString()); //$NON-NLS-1$
                                    int lastEventQuark = ss.getQuarkRelativeAndAdd(vmFlowQuark, "event"); //$NON-NLS-1$
                                    int eventCountQuark = ss.getQuarkRelativeAndAdd(vmFlowQuark, "event_count"); //$NON-NLS-1$

                                    ss.modifyAttribute(ts, name, lastEventQuark);

                                    // Keep track of event count
                                    Object currentCount = ss.queryOngoingState(eventCountQuark);
                                    int newCount = (currentCount instanceof Integer) ? ((Integer) currentCount) + 1 : 1;
                                    ss.modifyAttribute(ts, newCount, eventCountQuark);
                                }
                            }
                        }
                    }
                } else if (source == TraceType.VM_HOST) {
                    if (this.vmStart != null && this.vmStart.timestamp < ts) {
                        Map<Integer, ProcessFlowInfo> typeFlows = flows.get(TraceType.VM_GUEST_KERNEL);
                        if (typeFlows != null) {
                            ProcessFlowInfo flow = typeFlows.get(this.vmStart.pid);
                            if (flow != null) {
                                EventType type = getEventType(evt);
                                processHypervisorEvents(flow, type, evt);


                                int vmFlowQuark = ss.getQuarkAbsoluteAndAdd("vm_flow", Integer.toString(this.vmStart.pid)); //$NON-NLS-1$
                                int lastEventQuark = ss.getQuarkRelativeAndAdd(vmFlowQuark, "event"); //$NON-NLS-1$
                                int eventCountQuark = ss.getQuarkRelativeAndAdd(vmFlowQuark, "event_count"); //$NON-NLS-1$

                                ss.modifyAttribute(ts, name, lastEventQuark);

                                // Keep track of event count
                                Object currentCount = ss.queryOngoingState(eventCountQuark);
                                int newCount = (currentCount instanceof Integer) ? ((Integer) currentCount) + 1 : 1;
                                ss.modifyAttribute(ts, newCount, eventCountQuark);
                        }
                       }
                    }
                }

        } catch (Exception e) {
            System.err.println("Error handling kernel event: " + e.getMessage()); //$NON-NLS-1$
        }
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
                vmState.enterGuest(event);
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
        if (hostEvent.cpuid == this.vmState.getLastExitCpuId()) {
            return true;
        }

        return false;
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
     * Context information for each trace
     */
    private static class TraceContext {
        final ITmfTrace trace;
        final TraceType type;

        TraceContext(ITmfTrace trace, TraceType type) {
            this.trace = trace;
            this.type =  type;
        }
    }

    /**
     * Represent a synchronization point in the trace
     */
    private static class SyncPoint {
        final String eventType;
        final long timestamp;
        final TraceType traceType;
        final Integer dataValue;
        final int pid;
        final String procName;

        SyncPoint(String eventType, long timestamp, TraceType traceType, Integer dataValue, int pid, String procName) {
            this.eventType = eventType;
            this.timestamp = timestamp;
            this.traceType = traceType;
            this.dataValue = dataValue;
            this.pid = pid;
            this.procName = procName;
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
        private int currentPhysicalCpu = -1;

        void enterGuest(KernelEventInfo vmEntry) {
            inGuest = true;
            currentPhysicalCpu = vmEntry.cpuid;
            // Note: Physical CPU may have changed since last exit
        }

        void exitGuest(KernelEventInfo vmExit) {
            inGuest = false;
            lastExitTimestamp = vmExit.timestamp;
            lastExitCpuId = vmExit.cpuid;
            currentPhysicalCpu = vmExit.cpuid;
        }

        boolean isInGuest() {
            return inGuest;
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