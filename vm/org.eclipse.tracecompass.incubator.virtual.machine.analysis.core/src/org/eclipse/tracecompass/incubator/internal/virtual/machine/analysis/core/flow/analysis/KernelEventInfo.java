package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis;


/**
 *
 */
public class KernelEventInfo {
    /**
     *
     */
    public final String name;
    public final long timestamp;
    public final int pid;
    public final int tid;
    public final String processName;
    public final TraceType source;
    public final int cpuid;
    public final int vcpuid;
    public final String exitReason;

    /**
     * @param name
     * @param timestamp
     * @param pid
     * @param tid
     * @param processName
     * @param source
     * @param cpuid
     * @param vcpuid
     * @param exitReason
     */
    @SuppressWarnings("javadoc")
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
