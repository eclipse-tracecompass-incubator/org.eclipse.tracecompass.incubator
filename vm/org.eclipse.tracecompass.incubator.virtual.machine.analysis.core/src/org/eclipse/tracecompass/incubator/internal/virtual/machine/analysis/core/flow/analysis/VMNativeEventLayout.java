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

import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.trace.layout.LttngEventLayout;

/**
 * This file defines the event and field names for LTTng kernel traces
 * generated in a virtual machine analysis context, covering both guest
 * and host traces produced with lttng-modules for VM native call stack
 * analysis.
 *
 * @author Francois Belias
 */
@SuppressWarnings("restriction")
public class VMNativeEventLayout extends LttngEventLayout {

    // Field names
    private static final String VPID = "context._vpid"; //$NON-NLS-1$
    private static final String VTID = "context._vtid"; //$NON-NLS-1$
    private static final String VCPUID = "vcpu_id"; //$NON-NLS-1$
    private static final String CPUID = "context.cpu_id"; //$NON-NLS-1$
    private static final String PROCESS_NAME = "context._procname"; //$NON-NLS-1$
    private static final String EXIT_REASON = "exit_reason"; //$NON-NLS-1$

    private static final VMNativeEventLayout INSTANCE = new VMNativeEventLayout();

    /**
     * Constructor
     */
    protected VMNativeEventLayout() {}

    public static VMNativeEventLayout getInstance() {
        return INSTANCE;
    }

    // Update the names of the events
    @Override
    public String eventSyscallEntryPrefix() {
        return "syscall_entry_"; //$NON-NLS-1$
    }

    @Override
    public String eventSyscallExitPrefix() {
        return "syscall_exit_"; //$NON-NLS-1$
    }

    @Override
    public @NonNull Collection<@NonNull String> eventsKVMEntry() {
        return Collections.singleton("kvm_x86_entry"); //$NON-NLS-1$
    }

    @Override
    public @NonNull Collection<@NonNull String> eventsKVMExit() {
        return Collections.singleton("kvm_x86_exit"); //$NON-NLS-1$;
    }

    /**
     * Get the VPID context event
     *
     * @return The VPID context event name
     */
    public @NonNull String contextVPid() {
        return VPID;
    }

    /**
     * Get the VTID context event
     *
     * @return The VTID context event name
     */
    public @NonNull String contextVTid() {
        return VTID;

    }

    /**
     * Get the VCPUID context event
     *
     * @return The VCPUID context event name
     */
    public @NonNull String contextVcpuid() {
        return VCPUID;
    }

    /**
     * Get the CPUID context event
     *
     * @return The CPUID context event name
     */
    public @NonNull String contextCpuid() {
        return CPUID;
    }

    /**
     * Get the process name context event
     *
     * @return The PROCESS_NAME context event name
     */
    public @NonNull String contextProcessName() {
        return PROCESS_NAME;
    }

    /**
     * Get the exit reason name context event
     *
     * @return the EXIT_REASON context event name
     */
    public @NonNull String contextExitReason() {
        return EXIT_REASON;
    }
}