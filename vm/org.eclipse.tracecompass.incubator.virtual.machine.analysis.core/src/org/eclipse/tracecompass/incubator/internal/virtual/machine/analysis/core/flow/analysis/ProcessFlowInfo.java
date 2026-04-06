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
    /** The phase of the workload e.g begin, end */
    final String fPhase;

    /** The name of the process */
    final String fProcessName;

    /**Mapping between a tid and the threads associated to tid*/
    final Map<Integer, ThreadFlowInfo> fThreadsByTid = new HashMap<>();

    /** The specific thread we want to track **/
    private final Integer fTargetThreadId;

    /** The vCPU associated with our target thread **/
    private Integer fTargetVcpuId = null;

    /** Unified timeline containing both guest and hypervisor events **/
    final List<FlowEvent> fUnifiedFlow = new ArrayList<>();

    /** Whether hypervisor-level events should be tracked */
    private final boolean fTrackHypervisor;

    /** Tracks unique host–guest transitions to avoid duplicate processing */
    private final Set<String> fSeenTransitions = new HashSet<>();

    /** Tracks whether the current trace event is in host or guest context */
    private boolean fCurrentlyInHost = false;

    /** Constructor for non-virtualized environments **/
    ProcessFlowInfo(String phase, String processName) {
        this(phase, processName, false, null);
    }

    /**
     * Constructor
     *
     * @param phase the phase of the workload
     * @param processName the name of the process
     * @param trackHypervisor track the hypervisor or not
     * @param targetThreadId the thread to target
     */
    ProcessFlowInfo(String phase, String processName, boolean trackHypervisor, Integer targetThreadId) {
        this.fPhase = phase;
        this.fProcessName = processName;
        this.fTrackHypervisor = trackHypervisor;
        this.fTargetThreadId = targetThreadId;
    }

    /**
     * Add a guest process event - only accepts events from our target thread
     * @param evt the event to add
     */
    public void addGuestEvent(KernelEventInfo evt) {
        if (!evt.fProcessName.equals(this.fProcessName)) {
            return;
        }

        // If we're tracking a specific thread, only accept events from that thread
        if (fTargetThreadId != null && !fTargetThreadId.equals(evt.fTid)) {
            return;
        }

        // If this is the first event and we don't have a target thread, use this thread
        if (fTargetThreadId == null && fTrackHypervisor) {
            // This shouldn't happen with the new constructor, but just in case
            Activator.getInstance().logWarning("No target thread specified for hypervisor tracking"); //$NON-NLS-1$
            return;
        }

        ThreadFlowInfo threadInfo = fThreadsByTid.computeIfAbsent(evt.fTid,
            k -> new ThreadFlowInfo(k, fProcessName));
        threadInfo.addEvent(evt);

        // Establish vCPU mapping if we haven't already
        if (fTargetVcpuId == null && evt.fCpuid >= 0) {
            fTargetVcpuId = evt.fCpuid;
        }

        // Add to unified flow
        FlowEvent flowEvent = new FlowEvent(evt, FlowEventType.GUEST_EVENT);
        fUnifiedFlow.add(flowEvent);
    }

    /**
     * Add a VM exit/entry event - only accepts transitions for our target vCPU
     * @param evt the event to add
     * @param isExit the type of transition
     * @return whether or not the transition was successfully added
     */
    public boolean addVMTransition(KernelEventInfo evt, boolean isExit) {
        if (!fTrackHypervisor) {
            return false;
        }

        // Only accept VM transitions for our target vCPU
        if (fTargetVcpuId != null && evt.fVcpuid != fTargetVcpuId) {
            return false;
        }

        // If we haven't established the vCPU mapping yet, we can't correlate
        if (fTargetVcpuId == null) {
            // Store this transition in case we get the vCPU mapping later
            // For now, we'll skip it
            return false;
        }

        String key = (isExit ? "EXIT" : "ENTRY") + "_" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                evt.fTimestamp + "_" + evt.fCpuid + "_" + evt.fVcpuid + "_" + evt.fTid; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        if (fSeenTransitions.contains(key)) {
            return false;
        }
        fSeenTransitions.add(key);

        this.fCurrentlyInHost = isExit;
        FlowEventType type = isExit ? FlowEventType.VM_EXIT : FlowEventType.VM_ENTRY;
        FlowEvent flowEvent = new FlowEvent(evt, type);
        fUnifiedFlow.add(flowEvent);

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
        if (!fTrackHypervisor) {
            return false;
        }

        // We can only correlate hypervisor events if we know our target vCPU
        if (fTargetVcpuId == null) {
            return false;
        }

        // The hypervisor event should be on the same physical CPU that handled the VM exit
        // This is implicit in the chronological processing from the first solution

        String key = "HYPERVISOR" + "_" + //$NON-NLS-1$ //$NON-NLS-2$
                hypervisorEvt.fTimestamp + "_" + hypervisorEvt.fCpuid + "_" + hypervisorEvt.fTid; //$NON-NLS-1$ //$NON-NLS-2$

        if (fSeenTransitions.contains(key)) {
            return false;
        }
        fSeenTransitions.add(key);

        FlowEvent flowEvent = new FlowEvent(hypervisorEvt, FlowEventType.HYPERVISOR_EVENT);
        flowEvent.fCorrelatedGuestTimestamp = vmExitTimestamp;
        fUnifiedFlow.add(flowEvent);

        return true;
    }

    /**
     * Use this function when you want to add native events
     * @param evt the event to add
     */
    public void addEvent(KernelEventInfo evt) {
        if (fTrackHypervisor) {
            return;  // we don't want to track the hypervisor because we are on the native system
        }

        if (!fProcessName.equals(evt.fProcessName)) {
            return;
        }

        // For native systems, if we have a target thread, only track that thread
        if (fTargetThreadId != null && !fTargetThreadId.equals(evt.fTid)) {
            return;
        }

        ThreadFlowInfo threadInfo = fThreadsByTid.computeIfAbsent(evt.fTid,
                k -> new ThreadFlowInfo(k, fProcessName));
        threadInfo.addEvent(evt);

        String key = "NATIVE" + "_" + evt.fTimestamp + "_" + evt.fCpuid + "_" + evt.fTid; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        if (fSeenTransitions.contains(key)) {
            return;
        }
        fSeenTransitions.add(key);

        FlowEvent flowEvent = new FlowEvent(evt, FlowEventType.NATIVE);
        fUnifiedFlow.add(flowEvent);
    }

    /**
     * Check if this ProcessFlowInfo should accept a VM transition based on vCPU correlation
     * @param vmTransition the vm transition
     * @return whether or not the vm transition should be accepted
     */
    public boolean shouldAcceptVMTransition(KernelEventInfo vmTransition) {
        if (!fTrackHypervisor) {
            return false;
        }

        // If we haven't established a vCPU mapping yet, we can't determine correlation
        if (fTargetVcpuId == null) {
            return false;
        }

        // Accept if this transition is for our target vCPU
        return vmTransition.fVcpuid == fTargetVcpuId;
    }

    /**
     * Check if this ProcessFlowInfo is tracking the specified thread
     * @param threadId the Id of the thread
     * @return whether or not this thread is tracked
     */
    public boolean isTrackingThread(int threadId) {
        return fTargetThreadId == null || fTargetThreadId.equals(threadId);
    }

    /**
     * Get the target vCPU ID for this process flow
     * @return the target VCPU ID
     */
    public Integer getTargetVcpuId() {
        return fTargetVcpuId;
    }

    /**
     * Get the target thread ID for this process flow
     * @return the target thread ID
     */
    public Integer getTargetThreadId() {
        return fTargetThreadId;
    }

    /**
     * Set the target VCPUID
     * @param id the vcpu id
     */
    public void setTargetVcpuId(int id) {
        fTargetVcpuId = id;
    }

    /**
     * @return if we are currently inside the host
     */
    public boolean isCurrentlyInHost() {
        return fCurrentlyInHost;
    }

    /**
     * Finalize the flow analysis by sorting events chronologically
     */
    public void finalizeFlow() {
        // Sort unified flow by timestamp - this is crucial for correct chronological order
        fUnifiedFlow.sort(Comparator.comparing(fe -> fe.fKernelEvent.fTimestamp));

        // Build execution sequences
        buildExecutionSequences();
    }

    /**
     * Build execution sequences showing the flow between guest and hypervisor
     */
    private void buildExecutionSequences() {
        if (!fTrackHypervisor || fUnifiedFlow.isEmpty()) {
            return;
        }

        List<ExecutionSequence> sequences = new ArrayList<>();
        ExecutionSequence currentSequence = null;

        for (FlowEvent flowEvent : fUnifiedFlow) {
            switch (flowEvent.fType) {
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
        return fThreadsByTid.size() > 1;
    }

    /**
     * @return check whether the environment is virtualized or not
     */
    public boolean isVirtualized() {
        return fTrackHypervisor && fUnifiedFlow.stream()
            .anyMatch(fe -> fe.fType == FlowEventType.HYPERVISOR_EVENT ||
                           fe.fType == FlowEventType.VM_EXIT ||
                           fe.fType == FlowEventType.VM_ENTRY);
    }
}