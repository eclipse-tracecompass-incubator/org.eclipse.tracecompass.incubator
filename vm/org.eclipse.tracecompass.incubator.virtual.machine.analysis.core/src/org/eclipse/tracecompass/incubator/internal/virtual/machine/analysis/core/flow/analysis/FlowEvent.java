package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis;

/**
 * Represents a single event in the unified flow
 */
public class FlowEvent {
    final KernelEventInfo kernelEvent;
    final FlowEventType type;
    long correlatedGuestTimestamp = -1; // For hypervisor events

    FlowEvent(KernelEventInfo kernelEvent, FlowEventType type) {
        this.kernelEvent = kernelEvent;
        this.type = type;
    }
}


/**
 * Types of events in the unified flow
 */
enum FlowEventType {
    GUEST_EVENT,     // Normal guest process event
    VM_EXIT,         // VM exit to hypervisor
    HYPERVISOR_EVENT, // Host/hypervisor processing
    VM_ENTRY,         // VM entry back to guest
    NATIVE // Native system
}
