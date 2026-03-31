package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Represents a complete execution sequence in a virtualized environment:
 * <pre>
 * Guest → VM Exit → Hypervisor (Host) → VM Entry
 * </pre>
 * <p>
 * This class aggregates events occurring in both the guest and the hypervisor,
 * as well as the VM exit and entry transitions that connect them.
 * </p>
 *
 * @author Francois Belias
 */
public class ExecutionSequence {

    /**
     * List of events that occurred in the guest before the VM exit.
     */
    private final List<FlowEvent> guestEvents = new ArrayList<>();

    /**
     * List of events that occurred in the hypervisor (host)
     * between VM exit and VM entry.
     */
    private final List<FlowEvent> hypervisorEvents = new ArrayList<>();

    /**
     * Event representing the VM exit (transition from guest to host).
     */
    private FlowEvent vmExit;

    /**
     * Event representing the VM entry (transition from host back to guest).
     */
    private FlowEvent vmEntry;

    /**
     * Adds an event to the guest event list.
     *
     * @param event
     *            the guest event to add
     */
    void addGuestEvent(FlowEvent event) {
        guestEvents.add(event);
    }

    /**
     * Adds an event to the hypervisor event list.
     *
     * @param event
     *            the hypervisor event to add
     */
    void addHypervisorEvent(FlowEvent event) {
        hypervisorEvents.add(event);
    }

    /**
     * Sets the VM exit event.
     *
     * @param event
     *            the VM exit event
     */
    void setVmExit(FlowEvent event) {
        this.vmExit = event;
    }

    /**
     * Sets the VM entry event.
     *
     * @param event
     *            the VM entry event
     */
    void setVmEntry(FlowEvent event) {
        this.vmEntry = event;
    }

    /**
     * Prints the execution sequence for debugging purposes.
     * <p>
     * The sequence is printed both to the standard output and appended
     * to a file on disk.
     * </p>
     *
     * @throws IOException
     *             if an I/O error occurs while writing to the file
     */
    void printSequence() throws IOException {
        try (
            FileOutputStream fos = new FileOutputStream(
                    new File("/home/philippe/Desktop/virtualized_flow.txt"), true); //$NON-NLS-1$
            PrintWriter writer = new PrintWriter(fos)) {

            // Print guest events
            for (FlowEvent guestEvent : guestEvents) {
                KernelEventInfo evt = guestEvent.kernelEvent;
                System.out.printf("    [GUEST] %s (TID:%d, CPU:%d)\n", evt.name, evt.tid, evt.cpuid); //$NON-NLS-1$
                writer.printf("    [GUEST] %s (TID:%d, CPU:%d)\n", evt.name, evt.tid, evt.cpuid); //$NON-NLS-1$
            }

            // Print VM exit
            if (vmExit != null) {
                System.out.printf("    ↓ [VM_EXIT] %s (CPU:%d, VCPU:%d, exit_reason:%s)\n", //$NON-NLS-1$
                        vmExit.kernelEvent.name,
                        vmExit.kernelEvent.cpuid,
                        vmExit.kernelEvent.vcpuid,
                        vmExit.kernelEvent.exitReason);

                writer.printf("    ↓ [VM_EXIT] %s (CPU:%d, VCPU:%d, exit_reason:%s)\n", //$NON-NLS-1$
                        vmExit.kernelEvent.name,
                        vmExit.kernelEvent.cpuid,
                        vmExit.kernelEvent.vcpuid,
                        vmExit.kernelEvent.exitReason);
            }

            // Print hypervisor events
            for (FlowEvent hypervisorEvent : hypervisorEvents) {
                KernelEventInfo evt = hypervisorEvent.kernelEvent;
                System.out.printf("      [HOST] %s (PID:%d, CPU:%d)\n", //$NON-NLS-1$
                        evt.name, evt.pid, evt.cpuid);

                writer.printf("      [HOST] %s (PID:%d, CPU:%d)\n", //$NON-NLS-1$
                        evt.name, evt.pid, evt.cpuid);
            }

            // Print VM entry
            if (vmEntry != null) {
                System.out.printf("    ↑ [VM_ENTRY] %s (CPU:%d, VCPU:%d)\n", //$NON-NLS-1$
                        vmEntry.kernelEvent.name,
                        vmEntry.kernelEvent.cpuid,
                        vmEntry.kernelEvent.vcpuid);

                writer.printf("    ↑ [VM_ENTRY] %s (CPU:%d, VCPU:%d)\n", //$NON-NLS-1$
                        vmEntry.kernelEvent.name,
                        vmEntry.kernelEvent.cpuid,
                        vmEntry.kernelEvent.vcpuid);
            }

            // Print timing summary
            if (!guestEvents.isEmpty() && vmEntry != null) {
                long totalDuration = vmEntry.kernelEvent.timestamp
                        - guestEvents.get(0).kernelEvent.timestamp;

                System.out.printf("    Total sequence duration: %d µs\n", totalDuration / 1000); //$NON-NLS-1$
                writer.printf("    Total sequence duration: %d µs\n", totalDuration / 1000); //$NON-NLS-1$
            }
        }
    }

    /**
     * Indicates whether the execution sequence is complete.
     * <p>
     * A sequence is considered complete if it contains:
     * <ul>
     *   <li>At least one guest event</li>
     *   <li>A VM exit event</li>
     *   <li>A VM entry event</li>
     * </ul>
     * </p>
     *
     * @return {@code true} if the sequence is complete, {@code false} otherwise
     */
    boolean isComplete() {
        return !guestEvents.isEmpty() && vmExit != null && vmEntry != null;
    }
}