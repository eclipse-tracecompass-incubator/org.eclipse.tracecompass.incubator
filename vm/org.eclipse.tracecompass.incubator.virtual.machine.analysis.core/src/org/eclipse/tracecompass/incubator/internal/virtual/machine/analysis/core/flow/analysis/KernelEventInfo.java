package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis;


/**
 * Represents information about a kernel or system trace event.
 * <p>
 * This class is immutable: all fields are {@code final} and initialized
 * through the constructor.
 * </p>
 *
 * @author Francois Belias
 */
public class KernelEventInfo {

    /**
     * Name of the event (e.g., sys_call, sched_switch, etc.).
     */
    public final String name;

    /**
     * Timestamp of the event (in nanoseconds or depending on the trace unit).
     */
    public final long timestamp;

    /**
     * Process ID (PID).
     */
    public final int pid;

    /**
     * Thread ID (TID).
     */
    public final int tid;

    /**
     * Name of the process associated with the event.
     */
    public final String processName;

    /**
     * Source of the trace (e.g., kernel, user space, hypervisor, etc.).
     */
    public final TraceType source;

    /**
     * Physical CPU identifier where the event occurred.
     */
    public final int cpuid;

    /**
     * Virtual CPU identifier (relevant in virtualized environments).
     */
    public final int vcpuid;

    /**
     * Exit reason, typically used for virtualization-related events (e.g., VM exit).
     * May be {@code null} if not applicable.
     */
    public final String exitReason;

    /**
     * Constructs a {@link KernelEventInfo} instance.
     *
     * @param name
     *            the event name
     * @param timestamp
     *            the event timestamp
     * @param pid
     *            the process ID
     * @param tid
     *            the thread ID
     * @param processName
     *            the process name
     * @param source
     *            the trace source
     * @param cpuid
     *            the physical CPU ID
     * @param vcpuid
     *            the virtual CPU ID
     * @param exitReason
     *            the exit reason (may be {@code null} if not applicable)
     */
    public KernelEventInfo(String name, long timestamp, int pid, int tid, String processName,
            TraceType source, int cpuid, int vcpuid, String exitReason) {
        this.name = name;
        this.timestamp = timestamp;
        this.pid = pid;
        this.tid = tid;
        this.processName = processName;
        this.source = source;
        this.cpuid = cpuid;
        this.vcpuid = vcpuid;
        this.exitReason = exitReason;
    }
}