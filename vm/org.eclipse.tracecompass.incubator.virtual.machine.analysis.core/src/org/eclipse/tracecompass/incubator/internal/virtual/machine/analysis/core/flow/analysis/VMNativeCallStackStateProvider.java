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

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.profiling.core.callstack.CallStackAnalysis;
import org.eclipse.tracecompass.analysis.profiling.core.callstack.CallStackStateProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Provider for VM Native CallStack analysis - structures native flow for flame graph
 * Based on LttngUstCallStackProvider pattern
 *
 * @author Francois Belias
 */
public class VMNativeCallStackStateProvider extends CallStackStateProvider {

    private static final int VERSION = 1;

    /**
     * Trace markers injected via user-space (e.g., echo) to delimit phases
     * and identify the execution environment within the trace.
     */
    private static final String EVENT_MARKER = "syscall_entry_openat"; //$NON-NLS-1$
    private static final String MARKER_VIRTUALIZED = "./VM_VIRTUALIZED_ANALYSIS.txt"; //$NON-NLS-1$
    private static final String MARKER_NATIVE = "./VM_NATIVE_ANALYSIS.txt"; //$NON-NLS-1$
    private static final String MARKER_WORKLOAD = "./VM_ANALYSIS.txt"; //$NON-NLS-1$

    /** Singleton providing VM event layout definitions */
    private static VMNativeEventLayout fLayout = VMNativeEventLayout.getInstance();

    /** Start and end synchronization points delimiting the analysis window */
    private SyncPoint fStart = null;
    private SyncPoint fEnd = null;

    /** Current process and thread identifiers associated with the trace */
    private String fProcessName = ""; //$NON-NLS-1$
    private String fThreadName = ""; //$NON-NLS-1$

    /** Maintains the hierarchy of processes observed in the trace */
    private ProcessTree fProcessTree = new ProcessTree();

    /** Tracks the current execution state of the VM (host vs guest, etc.) */
    private VMExecutionState fVmState = new VMExecutionState();

    /**
     * Detected execution environment (native, virtualized, workload),
     * inferred automatically from trace markers.
     */
    private @Nullable EnvironmentType fEnvironmentType = null;

    /**
     * Constructor
     *
     * @param experiment the experiment containing all traces
     */
    public VMNativeCallStackStateProvider(@NonNull ITmfTrace experiment) {
        super(experiment);
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public VMNativeCallStackStateProvider getNewInstance() {
        return new VMNativeCallStackStateProvider(getTrace());
    }

    @Override
    protected boolean considerEvent(@NonNull ITmfEvent event) {
        // If the marker has not been seen yet, check the current event.
        if (this.fEnvironmentType == null) {
            detectEnvironmentFromMarker(event);
            return false; // Never process the marker itself as a call stack event
        }

        if (this.fEnvironmentType == EnvironmentType.VIRTUALIZED ||
                this.fEnvironmentType == EnvironmentType.HOST) {
            return this.considerVirtualizedEvent(event);
        } else if (this.fEnvironmentType == EnvironmentType.NATIVE) {
            return this.considerNativeEvent(event);
        }

        return false;
    }

    /**
     * Reads a single event and sets environmentType if it matches an environment marker.
     * The marker is a syscall_entry_openat injected by `echo ./VM_*_ANALYSIS.txt`
     * during the tracing session.
     */
    private void detectEnvironmentFromMarker(@NonNull ITmfEvent event) {
        String eventName = event.getName();
        if (!eventName.equals(EVENT_MARKER)) {
            return;
        }

        ITmfEventField content = event.getContent();
        String filename = content.getFieldValue(String.class, fLayout.fieldFilename());

        if (filename == null) {
            return;
        }

        if (filename.contains(MARKER_VIRTUALIZED)) {
            this.fEnvironmentType = EnvironmentType.VIRTUALIZED;
        } else if (filename.contains(MARKER_NATIVE)) {
            this.fEnvironmentType = EnvironmentType.NATIVE;
        }
    }

    @Override
    protected int getProcessId(@NonNull ITmfEvent event) {
        Integer pid = getIntField(event, fLayout.contextVPid());
        return pid != null ? pid : UNKNOWN_PID;
    }

    @Override
    protected long getThreadId(@NonNull ITmfEvent event) {
        Integer tid = getIntField(event, fLayout.contextVTid());
        return tid != null ? tid.longValue() : UNKNOWN_PID;
    }

    @Override
    public @Nullable String getThreadName(@NonNull ITmfEvent event) {
        // For host events, use guest thread name
        if (this.fEnvironmentType == EnvironmentType.HOST && fVmState.isInHypervisorOverhead()) {
            return fThreadName;
        }

        // For events in our process tree, show individual threads
        // but they'll be grouped under the same process name
        Integer pid = getIntField(event, fLayout.contextVPid());
        if (pid != null && fStart != null && fProcessTree.isInTree(pid)) {
            long tid = getThreadId(event);
            return getProcessName(event) + "-" + Long.toString(tid); //$NON-NLS-1$
        }

        // Fallback: standard naming
        String procName = getProcessName(event);
        long tid = getThreadId(event);
        return procName + "-" + Long.toString(tid); //$NON-NLS-1$
    }

    @Override
    public @Nullable ITmfStateValue functionEntry(@NonNull ITmfEvent event) {
        String eventName = event.getName();

        if (isSystemCallEntry(eventName)) {
            String syscallName = extractSyscallName(eventName);

            if (isSpecialSyscall(syscallName)) {
                return null;
            }

            return TmfStateValue.newValueString(syscallName);

        } else if (isInterruptEntry(eventName)) {
            String interruptName = fLayout.fieldIrq().toUpperCase() + "_" + extractInterruptInfo(event); //$NON-NLS-1$
            return TmfStateValue.newValueString(interruptName);
        }

        return null;
    }

    @Override
    public @Nullable ITmfStateValue functionExit(@NonNull ITmfEvent event) {
        String eventName = event.getName();

        if (isSystemCallExit(eventName)) {
            String syscallName = extractSyscallName(eventName.replace(fLayout.eventSyscallExitPrefix(), fLayout.eventSyscallEntryPrefix()));
            if (isSpecialSyscall(syscallName)) {
                return null;
            }
            return TmfStateValue.nullValue();

        } else if (isInterruptExit(eventName)) {
            return TmfStateValue.nullValue();
        }

        return null;
    }

    /**
     * Processes VM entry/exit events and updates the call stack accordingly.
     *
     * <p>
     * VM Exit → push hypervisor event<br>
     * VM Entry → pop (return to guest)
     */
    private void processHypervisorEvent(@NonNull ITmfEvent event) {
        String eventName = event.getName();
        long timestamp = event.getTimestamp().toNanos();
        Integer vcpuid = getIntField(event, fLayout.contextVcpuid());

        if (vcpuid == null || vcpuid != this.fVmState.getCurrentVirtualCpu()) {
            return;
        }

        ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());
        int processQuark = ss.getQuarkAbsoluteAndAdd(PROCESSES, this.fStart.procName);
        ss.updateOngoingState(TmfStateValue.newValueInt(this.fStart.pid), processQuark);
        int threadQuark = ss.getQuarkRelativeAndAdd(processQuark, fThreadName);
        ss.updateOngoingState(TmfStateValue.newValueLong(this.fStart.tid), threadQuark);
        int callStackQuark = ss.getQuarkRelativeAndAdd(threadQuark, CallStackAnalysis.CALL_STACK);

        if (isVMEntry(eventName)) {
            this.fVmState.enterGuest(vcpuid);
            ss.popAttribute(timestamp, callStackQuark);
        } else if (isVMExit(eventName)) {
            this.fVmState.exitGuest(vcpuid, timestamp);
            ss.pushAttribute(timestamp, eventName, callStackQuark);
        }
    }

    /**
     * Detects workload boundary markers.
     *
     * <p>
     * These markers define the region of interest.
     */
    private static boolean isWorkloadMarker(ITmfEvent event) {
        String eventName = event.getName();
        if (eventName.equals(EVENT_MARKER)) {
            ITmfEventField content = event.getContent();
            String filename = content.getFieldValue(String.class, fLayout.fieldFilename());
            if (filename != null) {
                return filename.contains(MARKER_WORKLOAD);
            }
        }
        return false;
    }

    /**
     * Check if the system call name is a variant of sigreturn
     *
     * @param syscallName
     * @return
     */
    private static boolean isSpecialSyscall(String syscallName) {
        return syscallName.equals("rt_sigreturn") || //$NON-NLS-1$
                syscallName.equals("sigreturn"); //$NON-NLS-1$
    }

    /**
     * Handles workload markers to initialize start and end synchronization points.
     */
    private void handleWorkloadMarker(@NonNull ITmfEvent event) {
        Integer pid = getIntField(event, fLayout.contextVPid());
        long tid = getThreadId(event);
        fProcessName = getProcessName(event);
        fThreadName = getThreadName(event);
        long timestamp = event.getTimestamp().toNanos();

        if (fStart == null) {
            Integer vcpuid = getIntField(event, fLayout.contextCpuid());

            fStart = new SyncPoint(timestamp,
                    pid != null ? pid : -1, tid,
                    fProcessName);

            if (pid != null) {
                fProcessTree.SetRootPid(pid);
            }

            this.fVmState.enterGuest(vcpuid != null ? vcpuid : -1);
        } else {
            fEnd = new SyncPoint(timestamp,
                    pid != null ? pid : -1, tid,
                    fProcessName);
        }
    }

    /**
     * Check if the event is a syscall entry
     * @param eventName the name of the event
     * @return True if the event is a syscall entry
     */
    private static boolean isSystemCallEntry(String eventName) {
        return eventName.startsWith(fLayout.eventSyscallEntryPrefix());
    }

    /**
     * Check if the event is a syscall exit
     * @param eventName the name of the event
     * @return True if the event is a syscall exit
     */
    private static boolean isSystemCallExit(String eventName) {
        return eventName.startsWith(fLayout.eventSyscallExitPrefix());
    }

    /**
     * Checks whether an event corresponds to an interrupt entry.
     *
     * <p>
     * This includes both:
     * <ul>
     *   <li>Hardware IRQ entry</li>
     *   <li>Soft IRQ entry</li>
     * </ul>
     *
     * @param eventName The name of the event
     * @return True if the event represents an interrupt entry
     */
    private static boolean isInterruptEntry(String eventName) {
        return eventName.contains(fLayout.eventIrqEntry()) || eventName.contains(fLayout.eventSoftIrqEntry());
    }

    /**
     * Checks whether an event corresponds to an interrupt exit.
     *
     * @param eventName The name of the event
     * @return True if the event represents an interrupt exit
     */
    private static boolean isInterruptExit(String eventName) {
        return eventName.contains(fLayout.eventIrqExit()) || eventName.contains(fLayout.eventSoftIrqExit());
    }

    /**
     * Extracts the system call name from an event name.
     *
     * <p>
     * Example:
     * <pre>
     * syscall_entry_open → open
     * </pre>
     *
     * @param eventName The full event name
     * @return The extracted syscall name
     */
    private static String extractSyscallName(String eventName) {
        if (eventName.startsWith(fLayout.eventSyscallEntryPrefix())) {
            return eventName.substring(fLayout.eventSyscallEntryPrefix().length());
        }
        return eventName;
    }

    /**
     * Checks whether an event corresponds to a VM entry (guest resume).
     *
     * @param eventName The name of the event
     * @return True if it is a VM entry event
     */
    private static boolean isVMEntry(String eventName) {
        return eventName.contains(fLayout.eventsKVMEntry().iterator().next());
    }

    /**
     * Checks whether an event corresponds to a VM exit (transition to hypervisor).
     *
     * @param eventName The name of the event
     * @return True if it is a VM exit event
     */
    private static boolean isVMExit(String eventName) {
        return eventName.contains(fLayout.eventsKVMExit().iterator().next());
    }

    /**
     * Extracts interrupt information (IRQ number) from an event.
     *
     * <p>
     * If the information cannot be parsed, returns "unknown".
     *
     * @param event The event containing IRQ information
     * @return The IRQ identifier as a string
     */
    private static String extractInterruptInfo(ITmfEvent event) {
        ITmfEventField content = event.getContent();
        String value = content.getFieldValue(String.class, fLayout.fieldIrq());
        return value == null ? "unknown" : value; //$NON-NLS-1$
    }

    /**
     * Extracts the vCPU ID from the process name.
     *
     * <p>
     * Expected format:
     * <pre>
     * "CPU X/KVM"
     * </pre>
     *
     * @param event The event containing process name
     * @return The vCPU ID, or null if not found
     */
    private static @Nullable Integer extractVcpuFromProcName(ITmfEvent event) {
        Object commField = event.getContent().getField(fLayout.contextProcessName());
        if (commField == null) {
            return null;
        }

        String procName = commField.toString();
        Pattern p = Pattern.compile("CPU (\\d+)/KVM"); //$NON-NLS-1$
        Matcher m = p.matcher(procName);

        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }

        return null;
    }

    /**
     * Extracts an integer field from an event.
     *
     * <p>
     * Special case:
     * For KVM entry events, the vCPU ID is extracted from the process name
     * instead of the standard field.
     *
     * @param event The event
     * @param fieldName The field name to extract
     * @return The integer value, or null if parsing fails
     */
    private static @Nullable Integer getIntField(ITmfEvent event, @NonNull String fieldName) {
        if (event.getType().getName().equals(fLayout.eventsKVMEntry().iterator().next()) && fieldName.equals(fLayout.contextVcpuid())) {
            return extractVcpuFromProcName(event);
        }

        ITmfEventField content = event.getContent();
        Integer value = content.getFieldValue(Integer.class, fieldName);
        return value;
    }

    /**
     * Extracts the process name from an event.
     *
     * <p>
     * Returns "unknown" if the field is missing or malformed.
     *
     * @param event The event
     * @return The process name
     */
    private static String getProcessNameFromEvent(ITmfEvent event) {
        ITmfEventField content = event.getContent();
        String value = content.getFieldValue(String.class, fLayout.contextProcessName());
        return value == null ? "unknown" : value; //$NON-NLS-1$
    }

    /**
     * Determines whether a host event is relevant to the current VM execution.
     *
     * <p>
     * Only events from the correct KVM vCPU thread are considered.
     *
     * @param event The event
     * @return True if the event should be considered
     */
    private boolean isRelevantHostEvent(@NonNull ITmfEvent event) {
        String procName = getProcessNameFromEvent(event);

        procName = procName.trim().replaceAll("\"", ""); //$NON-NLS-1$ //$NON-NLS-2$

        if (procName.matches("CPU \\d+/KVM")) { //$NON-NLS-1$
            Integer vcpu = extractVcpuFromProcName(event);
            return vcpu != null && vcpu == fVmState.getCurrentVirtualCpu();
        }

        return false;
    }

    /**
     * Filters events in virtualized or host environments.
     *
     * <p>
     * Behavior depends on environment:
     * <ul>
     *   <li><b>Guest (VIRTUALIZED)</b>: track events from the process tree</li>
     *   <li><b>Host</b>: track only hypervisor overhead related to the VM</li>
     * </ul>
     *
     * @param event The event to evaluate
     * @return True if the event should be included in the call stack
     */
    private boolean considerVirtualizedEvent(@NonNull ITmfEvent event) {
        if (isWorkloadMarker(event)) {
            handleWorkloadMarker(event);
            return false;
        }

        if (fStart != null) {
            Integer pid = getIntField(event, fLayout.contextVPid());
            long timestamp = event.getTimestamp().toNanos();

            if (this.fEnvironmentType == EnvironmentType.VIRTUALIZED) {
                trackCloneEvent(event);
                if (pid != null && fProcessTree.isInTree(pid)) {
                    Integer guestCpu = getIntField(event, fLayout.contextCpuid());
                    if (guestCpu != null) {
                        fVmState.enterGuest(guestCpu);
                    }
                    return (fEnd == null || timestamp <= fEnd.timestamp);
                }
            } else if (this.fEnvironmentType == EnvironmentType.HOST) {
                if (fEnd == null || timestamp <= fEnd.timestamp) {
                    String eventName = event.getName();
                    if (isVMEntry(eventName) || isVMExit(eventName)) {
                        this.processHypervisorEvent(event);
                        return false;
                    }

                    if (fVmState.isInHypervisorOverhead() && isRelevantHostEvent(event)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Filters events in native execution mode.
     *
     * <p>
     * Only events from the tracked process tree are kept,
     * within the workload time window.
     *
     * @param event The event
     * @return True if the event should be included
     */
    private boolean considerNativeEvent(@NonNull ITmfEvent event) {
        if (isWorkloadMarker(event)) {
            handleWorkloadMarker(event);
            return false;
        }

        if (fStart != null) {
            trackCloneEvent(event);

            Integer pid = getIntField(event, fLayout.contextVPid());
            long timestamp = event.getTimestamp().toNanos();

            if (pid != null && fProcessTree.isInTree(pid)) {
                return (fEnd == null || timestamp <= fEnd.timestamp);
            }
        }

        return false;
    }

    @Override
    protected String getProcessName(ITmfEvent event) {
        if (this.fEnvironmentType == EnvironmentType.HOST && fVmState.isInHypervisorOverhead()) {
            return fProcessName;
        }

        Integer pid = getIntField(event, fLayout.contextVcpuid());
        if (pid != null && fStart != null && fProcessTree.isInTree(pid)) {
            return fStart.procName;
        }

        return getProcessNameFromEvent(event);
    }

    /**
     * Tracks process hierarchy using clone/fork system calls.
     *
     * <p>
     * Only children of tracked processes are included.
     */
    private void trackCloneEvent(@NonNull ITmfEvent event) {
        String eventName = event.getName();

        if (eventName.equals("syscall_exit_clone") || //$NON-NLS-1$
            eventName.equals("syscall_exit_clone3") || //$NON-NLS-1$
            eventName.equals("syscall_exit_fork") || //$NON-NLS-1$
            eventName.equals("syscall_exit_vfork")) { //$NON-NLS-1$

            Integer parentPid = getIntField(event, fLayout.contextVPid());
            Integer childPid = getIntField(event, fLayout.fieldSyscallRet());

            if (parentPid != null && childPid != null && childPid > 0) {
                if (fProcessTree.isInTree(parentPid)) {
                    fProcessTree.registerClone(parentPid, childPid);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Inner classes
    // -------------------------------------------------------------------------

    private static class SyncPoint {
        final long timestamp;
        final int pid;
        final String procName;
        final long tid;

        SyncPoint(long timestamp, int pid, long tid, String procName) {
            this.timestamp = timestamp;
            this.pid = pid;
            this.tid = tid;
            this.procName = procName;
        }
    }

    /** Tracks VM execution state with migration awareness */
    private static class VMExecutionState {
        private boolean inGuest = false;
        private long lastExitTimestamp = -1;
        private int currentVirtualCpu = -1;

        void enterGuest(int vcpuid) {
            inGuest = true;
            this.currentVirtualCpu = vcpuid;
        }

        void exitGuest(int cpuid, long timestamp) {
            this.inGuest = false;
            this.lastExitTimestamp = timestamp;
            this.currentVirtualCpu = cpuid;
        }

        boolean isInHypervisorOverhead() {
            return !inGuest && this.lastExitTimestamp != -1;
        }

        int getCurrentVirtualCpu() {
            return this.currentVirtualCpu;
        }
    }

    private enum EnvironmentType {
        VIRTUALIZED,
        HOST,
        NATIVE
    }

    /** Tracks process hierarchy through clone/fork events */
    private static class ProcessTree {
        private final Map<Integer, Integer> childToParent = new HashMap<>();
        private final Map<Integer, Set<Integer>> parentToChildren = new HashMap<>();
        private int rootPid = -1;

        void registerClone(int parentPid, int childPid) {
            childToParent.put(childPid, parentPid);
            parentToChildren.computeIfAbsent(parentPid, k -> new HashSet<>()).add(childPid);
        }

        void SetRootPid(int pid) {
            this.rootPid = pid;
        }

        boolean isInTree(int pid) {
            if (rootPid == -1) {
                return false;
            }
            if (pid == rootPid) {
                return true;
            }
            return isDescendant(pid, rootPid);
        }

        private boolean isDescendant(int pid, int ancestorPid) {
            Integer current = childToParent.get(pid);
            while (current != null) {
                if (current == ancestorPid) {
                    return true;
                }
                current = childToParent.get(current);
            }
            return false;
        }
    }
}