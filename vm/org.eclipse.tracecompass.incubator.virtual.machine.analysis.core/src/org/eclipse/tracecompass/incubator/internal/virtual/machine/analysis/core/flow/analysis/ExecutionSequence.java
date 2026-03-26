package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Represents a complete execution sequence: Guest → VM Exit → Host → VM Entry
 *
 * @author philippe
 */
public class ExecutionSequence {
    private final List<FlowEvent> guestEvents = new ArrayList<>();
    private final List<FlowEvent> hypervisorEvents = new ArrayList<>();
    private FlowEvent vmExit;
    private FlowEvent vmEntry;

    void addGuestEvent(FlowEvent event) {
        guestEvents.add(event);
    }

    void addHypervisorEvent(FlowEvent event) {
        hypervisorEvents.add(event);
    }

    void setVmExit(FlowEvent event) {
        this.vmExit = event;
    }

    void setVmEntry(FlowEvent event) {
        this.vmEntry = event;
    }

    void printSequence() throws IOException {
        try (

            FileOutputStream fos = new FileOutputStream(new File("/home/philippe/Desktop/virtualized_flow.txt"), true); //$NON-NLS-1$
            PrintWriter writer = new PrintWriter(fos)) {
            for (FlowEvent guestEvent : guestEvents) {
                KernelEventInfo evt = guestEvent.kernelEvent;
                System.out.printf("    [GUEST] %s (TID:%d, CPU:%d)\n", evt.name, evt.tid, evt.cpuid); //$NON-NLS-1$
                writer.printf("    [GUEST] %s (TID:%d, CPU:%d)\n", evt.name, evt.tid, evt.cpuid); //$NON-NLS-1$
            }

            // Print VM exit
            if (vmExit != null) {
                System.out.printf("    ↓ [VM_EXIT] %s (CPU:%d, VCPU:%d, exit_reason:%s)\n", vmExit.kernelEvent.name,  //$NON-NLS-1$
                        vmExit.kernelEvent.cpuid, vmExit.kernelEvent.vcpuid, vmExit.kernelEvent.exitReason);

                writer.printf("    ↓ [VM_EXIT] %s (CPU:%d, VCPU:%d, exit_reason:%s)\n", vmExit.kernelEvent.name,  //$NON-NLS-1$
                        vmExit.kernelEvent.cpuid, vmExit.kernelEvent.vcpuid, vmExit.kernelEvent.exitReason);
            }

            // Print hypervisor events
            for (FlowEvent hypervisorEvent : hypervisorEvents) {
                KernelEventInfo evt = hypervisorEvent.kernelEvent;
                System.out.printf("      [HOST] %s (PID:%d, CPU:%d)\n", evt.name, evt.pid, evt.cpuid); //$NON-NLS-1$

                writer.printf("      [HOST] %s (PID:%d, CPU:%d)\n", evt.name, evt.pid, evt.cpuid); //$NON-NLS-1$
            }

            // Print VM entry
            if (vmEntry != null) {
                System.out.printf("    ↑ [VM_ENTRY] %s (CPU:%d, VCPU:%d)\n", vmEntry.kernelEvent.name, //$NON-NLS-1$
                        vmEntry.kernelEvent.cpuid, vmEntry.kernelEvent.vcpuid);

                writer.printf("    ↑ [VM_ENTRY] %s (CPU:%d, VCPU:%d)\n", vmEntry.kernelEvent.name, //$NON-NLS-1$
                        vmEntry.kernelEvent.cpuid, vmEntry.kernelEvent.vcpuid);
            }

            // Print timing summary
            if (!guestEvents.isEmpty() && vmEntry != null) {
                long totalDuration = vmEntry.kernelEvent.timestamp -
                    guestEvents.get(0).kernelEvent.timestamp;
                System.out.printf("    Total sequence duration: %d µs\n", totalDuration / 1000); //$NON-NLS-1$

                writer.printf("    Total sequence duration: %d µs\n", totalDuration / 1000); //$NON-NLS-1$
            }
        }
    }

    boolean isComplete() {
        return !guestEvents.isEmpty() && vmExit != null && vmEntry != null;
    }
}