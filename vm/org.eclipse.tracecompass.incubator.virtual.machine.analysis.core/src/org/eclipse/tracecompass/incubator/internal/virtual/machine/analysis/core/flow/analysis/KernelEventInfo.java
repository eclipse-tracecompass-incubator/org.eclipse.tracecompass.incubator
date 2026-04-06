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
    public final String fName;

    /**
     * Timestamp of the event (in nanoseconds or depending on the trace unit).
     */
    public final long fTimestamp;

    /**
     * Process ID (PID).
     */
    public final int fPid;

    /**
     * Thread ID (TID).
     */
    public final int fTid;

    /**
     * Name of the process associated with the event.
     */
    public final String fProcessName;

    /**
     * Source of the trace (e.g., kernel, user space, hypervisor, etc.).
     */
    public final TraceType fSource;

    /**
     * Physical CPU identifier where the event occurred.
     */
    public final int fCpuid;

    /**
     * Virtual CPU identifier (relevant in virtualized environments).
     */
    public final int fVcpuid;

    /**
     * Exit reason, typically used for virtualization-related events (e.g., VM exit).
     * May be {@code null} if not applicable.
     */
    public final String fExitReason;

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
        this.fName = java.util.Objects.requireNonNull(name, "name cannot be null"); //$NON-NLS-1$
        this.fTimestamp = timestamp;
        this.fPid = pid;
        this.fTid = tid;
        this.fProcessName = java.util.Objects.requireNonNull(processName, "processName cannot be null"); //$NON-NLS-1$
        this.fSource = java.util.Objects.requireNonNull(source, "source cannot be null"); //$NON-NLS-1$
        this.fCpuid = cpuid;
        this.fVcpuid = vcpuid;
        this.fExitReason = exitReason;
    }
}