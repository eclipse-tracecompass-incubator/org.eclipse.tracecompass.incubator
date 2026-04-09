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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.Activator;

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
     * @param evt the event to add
     */
    public void addGuestEvent(KernelEventInfo evt) {
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
            Activator.getInstance().logWarning("No target thread specified for hypervisor tracking"); //$NON-NLS-1$
            return;
        }

        ThreadFlowInfo threadInfo = threadsByTid.computeIfAbsent(evt.tid,
            k -> new ThreadFlowInfo(k, processName));
        threadInfo.addEvent(evt);

        // Establish vCPU mapping if we haven't already
        if (targetVcpuId == null && evt.cpuid >= 0) {
            targetVcpuId = evt.cpuid;
        }

        // Add to unified flow
        FlowEvent flowEvent = new FlowEvent(evt, FlowEventType.GUEST_EVENT);
        unifiedFlow.add(flowEvent);
    }

    /**
     * Add a VM exit/entry event - only accepts transitions for our target vCPU
     * @param evt the event to add
     * @param isExit the type of transition
     * @return whether or not the transition was successfully added
     */
    public boolean addVMTransition(KernelEventInfo evt, boolean isExit) {
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

        return true;
    }

    /**
     * Add a hypervisor event - only accepts events on the CPU that received
     * the vCPU exit for our target thread
     * @param hypervisorEvt the hypervisor event
     * @param vmExitTimestamp the timestamp of the kvm exit
     * @return whether or not the hypervisor event was added
     */
    public boolean addHypervisorEvent(KernelEventInfo hypervisorEvt, long vmExitTimestamp) {
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
     * @param evt the event to add
     */
    public void addEvent(KernelEventInfo evt) {
        if (trackHypervisor) {
            return;  // we don't want to track the hypervisor because we are on the native system
        }

        if (!processName.equals(evt.processName)) {
            return;
        }

        // For native systems, if we have a target thread, only track that thread
        if (targetThreadId != null && !targetThreadId.equals(evt.tid)) {
            return;
        }

        ThreadFlowInfo threadInfo = threadsByTid.computeIfAbsent(evt.tid,
                k -> new ThreadFlowInfo(k, processName));
        threadInfo.addEvent(evt);

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
     * @param vmTransition the vm transition
     * @return whether or not the vm transition should be accepted
     */
    public boolean shouldAcceptVMTransition(KernelEventInfo vmTransition) {
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
     * @param threadId the Id of the thread
     * @return whether or not this thread is tracked
     */
    public boolean isTrackingThread(int threadId) {
        return targetThreadId == null || targetThreadId.equals(threadId);
    }

    /**
     * Get the target vCPU ID for this process flow
     * @return the target VCPU ID
     */
    public Integer getTargetVcpuId() {
        return targetVcpuId;
    }

    /**
     * Get the target thread ID for this process flow
     * @return the target thread ID
     */
    public Integer getTargetThreadId() {
        return targetThreadId;
    }

    /**
     * Set the target VCPUID
     * @param id the vcpu id
     */
    public void setTargetVcpuId(int id) {
        targetVcpuId = id;
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
    public void finalizeFlow() {
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
                    if (currentSequence != null) {
                        sequences.add(currentSequence);
                    }
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
        }

        // Add any remaining sequence
        if (currentSequence != null) {
            sequences.add(currentSequence);
        }
    }

    /**
     * @return whether or not the process has multiple threads
     */
    public boolean isMultiThreaded() {
        return threadsByTid.size() > 1;
    }

    /**
     * @return check whether the environment is virtualized or not
     */
    public boolean isVirtualized() {
        return trackHypervisor && unifiedFlow.stream()
            .anyMatch(fe -> fe.type == FlowEventType.HYPERVISOR_EVENT ||
                           fe.type == FlowEventType.VM_EXIT ||
                           fe.type == FlowEventType.VM_ENTRY);
    }
}