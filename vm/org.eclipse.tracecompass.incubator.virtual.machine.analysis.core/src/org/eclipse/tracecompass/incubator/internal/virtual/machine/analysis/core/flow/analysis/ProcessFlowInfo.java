package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Flow analysis of a specific thread in a process. Tracks the complete execution
 * flow including guest events, VM transitions, and host events for virtualized systems.
 *
 * @author Francois Belias
 */
public class ProcessFlowInfo {
    final String phase;
    final String processName;
    final Map<Integer, ThreadFlowInfo> threadsByTid = new HashMap<>();

    // Thread-specific tracking
    private final Integer targetThreadId;  // The specific thread we want to track
    private Integer targetVcpuId = null;   // The vCPU associated with our target thread

    // Unified timeline containing both guest and hypervisor events
    final List<FlowEvent> unifiedFlow = new ArrayList<>();

    // Track virtualization context
    private final boolean trackHypervisor;
    private final Set<String> seenTransitions = new HashSet<>();
    private boolean currentlyInHost = false;

    // Store execution sequences for analysis
    private List<ExecutionSequence> executionSequences = new ArrayList<>();

    // Constructor for non-virtualized environments
    ProcessFlowInfo(String phase, String processName) {
        this(phase, processName, false, null);
    }

    // Constructor for tracking a specific thread in virtualized environment
    ProcessFlowInfo(String phase, String processName, boolean trackHypervisor, Integer targetThreadId) {
        this.phase = phase;
        this.processName = processName;
        this.trackHypervisor = trackHypervisor;
        this.targetThreadId = targetThreadId;
    }

    /**
     * Add a guest process event - only accepts events from our target thread
     */
    void addGuestEvent(KernelEventInfo evt) {
        if (!evt.processName.equals(this.processName)) {
            return;
        }

        // If we're tracking a specific thread, only accept events from that thread
        if (targetThreadId != null && !targetThreadId.equals(evt.tid)) {
            return;
        }

        // If this is the first event and we don't have a target thread, use this thread
        if (targetThreadId == null && trackHypervisor) {
            // This shouldn't happen with the new constructor, but just in case
            System.err.println("Warning: No target thread specified for hypervisor tracking"); //$NON-NLS-1$
            return;
        }

        ThreadFlowInfo threadInfo = threadsByTid.computeIfAbsent(evt.tid,
            k -> new ThreadFlowInfo(k, processName));
        threadInfo.addEvent(evt);

        // Establish vCPU mapping if we haven't already
        if (targetVcpuId == null && evt.cpuid >= 0) {
            targetVcpuId = evt.cpuid;
            System.out.printf("Established vCPU mapping: Thread %d -> vCPU %d\n", evt.tid, evt.cpuid); //$NON-NLS-1$
        }

        // Add to unified flow
        FlowEvent flowEvent = new FlowEvent(evt, FlowEventType.GUEST_EVENT);
        unifiedFlow.add(flowEvent);
    }

    /**
     * Add a VM exit/entry event - only accepts transitions for our target vCPU
     */
    boolean addVMTransition(KernelEventInfo evt, boolean isExit) {
        if (!trackHypervisor) {
            return false;
        }

        // Only accept VM transitions for our target vCPU
        if (targetVcpuId != null && evt.vcpuid != targetVcpuId) {
            return false;
        }

        // If we haven't established the vCPU mapping yet, we can't correlate
        if (targetVcpuId == null) {
            // Store this transition in case we get the vCPU mapping later
            // For now, we'll skip it
            return false;
        }

        String key = (isExit ? "EXIT" : "ENTRY") + "_" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                evt.timestamp + "_" + evt.cpuid + "_" + evt.vcpuid + "_" + evt.tid; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        if (seenTransitions.contains(key)) {
            return false;
        }
        seenTransitions.add(key);

        this.currentlyInHost = isExit;
        FlowEventType type = isExit ? FlowEventType.VM_EXIT : FlowEventType.VM_ENTRY;
        FlowEvent flowEvent = new FlowEvent(evt, type);
        unifiedFlow.add(flowEvent);

        System.out.printf("Added VM %s for vCPU %d at timestamp %d\n", //$NON-NLS-1$
                         isExit ? "EXIT" : "ENTRY", evt.vcpuid, evt.timestamp); //$NON-NLS-1$ //$NON-NLS-2$

        return true;
    }

    /**
     * Add a hypervisor event - only accepts events on the CPU that received
     * the vCPU exit for our target thread
     */
    boolean addHypervisorEvent(KernelEventInfo hypervisorEvt, long vmExitTimestamp) {
        if (!trackHypervisor) {
            return false;
        }

        // We can only correlate hypervisor events if we know our target vCPU
        if (targetVcpuId == null) {
            return false;
        }

        // The hypervisor event should be on the same physical CPU that handled the VM exit
        // This is implicit in the chronological processing from the first solution

        String key = "HYPERVISOR" + "_" + //$NON-NLS-1$ //$NON-NLS-2$
                hypervisorEvt.timestamp + "_" + hypervisorEvt.cpuid + "_" + hypervisorEvt.tid; //$NON-NLS-1$ //$NON-NLS-2$

        if (seenTransitions.contains(key)) {
            return false;
        }
        seenTransitions.add(key);

        FlowEvent flowEvent = new FlowEvent(hypervisorEvt, FlowEventType.HYPERVISOR_EVENT);
        flowEvent.correlatedGuestTimestamp = vmExitTimestamp;
        unifiedFlow.add(flowEvent);

        return true;
    }

    /**
     * Use this function when you want to add native events
     */
    public void addEvent(KernelEventInfo evt) {
        if (trackHypervisor) {
            return;  // we don't want to track the hypervisor because we are on the native system
        }

        // For native systems, if we have a target thread, only track that thread
        if (targetThreadId != null && !targetThreadId.equals(evt.tid)) {
            return;
        }

        String key = "NATIVE" + "_" + evt.timestamp + "_" + evt.cpuid + "_" + evt.tid; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        if (seenTransitions.contains(key)) {
            return;
        }
        seenTransitions.add(key);

        FlowEvent flowEvent = new FlowEvent(evt, FlowEventType.NATIVE);
        unifiedFlow.add(flowEvent);
    }

    /**
     * Check if this ProcessFlowInfo should accept a VM transition based on vCPU correlation
     */
    boolean shouldAcceptVMTransition(KernelEventInfo vmTransition) {
        if (!trackHypervisor) {
            return false;
        }

        // If we haven't established a vCPU mapping yet, we can't determine correlation
        if (targetVcpuId == null) {
            return false;
        }

        // Accept if this transition is for our target vCPU
        return vmTransition.vcpuid == targetVcpuId;
    }

    /**
     * Check if this ProcessFlowInfo is tracking the specified thread
     */
    boolean isTrackingThread(int threadId) {
        return targetThreadId == null || targetThreadId.equals(threadId);
    }

    /**
     * Get the target vCPU ID for this process flow
     */
    Integer getTargetVcpuId() {
        return targetVcpuId;
    }

    /**
     * Get the target thread ID for this process flow
     */
    Integer getTargetThreadId() {
        return targetThreadId;
    }

    void SetTargetVcpuId(int value) {
        targetVcpuId = value;
    }

    /**
     * @return if we are currently inside the host
     */
    public boolean isCurrentlyInHost() {
        return currentlyInHost;
    }

    /**
     * Finalize the flow analysis by sorting events chronologically
     */
    void finalizeFlow() {
        // Sort unified flow by timestamp - this is crucial for correct chronological order
        unifiedFlow.sort(Comparator.comparing(fe -> fe.kernelEvent.timestamp));

        // Build execution sequences
        buildExecutionSequences();
    }

    /**
     * Build execution sequences showing the flow between guest and hypervisor
     */
    private void buildExecutionSequences() {
        if (!trackHypervisor || unifiedFlow.isEmpty()) {
            return;
        }

        List<ExecutionSequence> sequences = new ArrayList<>();
        ExecutionSequence currentSequence = null;

        for (FlowEvent flowEvent : unifiedFlow) {
            switch (flowEvent.type) {
                case VM_ENTRY:
                    currentSequence = new ExecutionSequence();
                    currentSequence.setVmEntry(flowEvent);
                    break;

                case GUEST_EVENT:
                    if (currentSequence != null) {
                        currentSequence.addGuestEvent(flowEvent);
                    }
                    break;

                case VM_EXIT:
                    if (currentSequence != null) {
                        currentSequence.setVmExit(flowEvent);
                    }
                    break;

                case HYPERVISOR_EVENT:
                    if (currentSequence != null) {
                        currentSequence.addHypervisorEvent(flowEvent);
                    }
                    break;

                case NATIVE:
                    if (currentSequence == null) {
                        currentSequence = new ExecutionSequence();
                    }
                    currentSequence.addHypervisorEvent(flowEvent);
                    break;

                default:
                    break;
            }

            // Close sequence after VM_ENTRY (which comes after hypervisor events)
            // Actually, let's close it when we see the next VM_ENTRY or at the end
        }

        // Add any remaining sequence
        if (currentSequence != null) {
            sequences.add(currentSequence);
        }

        this.executionSequences = sequences;
    }

    /**
     * Print the unified execution flow
     */
    void printUnifiedFlow() throws IOException {
        System.out.printf("\n=== Unified Flow for Process %s (Phase: %s) ===\n", //$NON-NLS-1$
            processName, phase);

        if (targetThreadId != null) {
            System.out.printf("Tracking Thread: %d", targetThreadId); //$NON-NLS-1$
            if (targetVcpuId != null) {
                System.out.printf(" (vCPU: %d)", targetVcpuId); //$NON-NLS-1$
            }
            System.out.println();
        }

        if (!trackHypervisor) {
            printSimpleFlow();
            return;
        }

        printVirtualizedFlow();
    }

    private void printSimpleFlow() throws IOException {
        System.out.printf("Events: %d, Threads: %d\n", unifiedFlow.size(), threadsByTid.size()); //$NON-NLS-1$

        FileOutputStream fos = new FileOutputStream(new File("/home/philippe/Desktop/natif_flow.txt"), true); //$NON-NLS-1$
        try (PrintWriter writer = new PrintWriter(fos)) {
            for (FlowEvent flowEvent : unifiedFlow) {
                KernelEventInfo evt = flowEvent.kernelEvent;
                String output = String.format("  [%d] %s (TID:%d)\n", //$NON-NLS-1$
                    evt.timestamp, evt.name, evt.tid);
                System.out.print(output);
                writer.print(output);
            }
        }
    }

    private void printVirtualizedFlow() throws IOException {
        System.out.printf("Execution Sequences: %d\n", executionSequences.size()); //$NON-NLS-1$

        int sequenceNum = 1;
        for (ExecutionSequence seq : executionSequences) {
            System.out.printf("\n--- Sequence %d ---\n", sequenceNum++); //$NON-NLS-1$
            seq.printSequence();
        }

        // Also print raw chronological flow
        System.out.println("\n--- Raw Chronological Flow ---"); //$NON-NLS-1$
        FileOutputStream fos = new FileOutputStream(new File("/home/philippe/Desktop/virtualized_flow_raw.txt"), true); //$NON-NLS-1$
        try (PrintWriter writer = new PrintWriter(fos)) {
            for (FlowEvent flowEvent : unifiedFlow) {
                printFlowEvent(flowEvent, writer);
            }
        }
    }

    private static void printFlowEvent(FlowEvent flowEvent, PrintWriter writer) {
        KernelEventInfo evt = flowEvent.kernelEvent;
        String prefix = getEventPrefix(flowEvent.type);

        String output = String.format("  [%d] %s%s", evt.timestamp, prefix, evt.name); //$NON-NLS-1$

        if (evt.tid >= 0) {
            output += String.format(" (TID:%d", evt.tid); //$NON-NLS-1$
            if (evt.cpuid >= 0) {
                output += String.format(", CPU:%d", evt.cpuid); //$NON-NLS-1$
            }
            if (evt.vcpuid >= 0) {
                output += String.format(", VCPU:%d", evt.vcpuid); //$NON-NLS-1$
            }
            if (!evt.exitReason.equals("UNKNOWN_EXIT_REASON")) { //$NON-NLS-1$
                output += String.format(", exit_reason:%s", evt.exitReason); //$NON-NLS-1$
            }
            output += ")"; //$NON-NLS-1$
        }
        output += "\n"; //$NON-NLS-1$

        System.out.print(output);
        if (writer != null) {
            writer.print(output);
        }
    }

    private static String getEventPrefix(FlowEventType type) {
        switch (type) {
            case GUEST_EVENT: return "[GUEST] "; //$NON-NLS-1$
            case VM_EXIT: return "[VM_EXIT] "; //$NON-NLS-1$
            case HYPERVISOR_EVENT: return "[HOST] "; //$NON-NLS-1$
            case VM_ENTRY: return "[VM_ENTRY] "; //$NON-NLS-1$
            case NATIVE: return "[NATIVE] "; //$NON-NLS-1$
            default: return ""; //$NON-NLS-1$
        }
    }

    boolean isMultiThreaded() {
        return threadsByTid.size() > 1;
    }

    boolean isVirtualized() {
        return trackHypervisor && unifiedFlow.stream()
            .anyMatch(fe -> fe.type == FlowEventType.HYPERVISOR_EVENT ||
                           fe.type == FlowEventType.VM_EXIT ||
                           fe.type == FlowEventType.VM_ENTRY);
    }
}