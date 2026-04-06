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


import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemBuilderUtils;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * State provider for the KVM exit analysis module.
 * This provider processes KVM exit events from a trace and updates the state system
 * with information about KVM exits per CPU.
 *
 * @author Francois Belias
 */
public class KvmExitStateProvider extends AbstractTmfStateProvider {

    private static final int VERSION = 1;

    /**
     * The ID of this analysis module
     */
    private static final String ID = "org.eclipse.tracecompass.incubator.internal.overhead.core.analysis"; //$NON-NLS-1$

    /** Singleton providing VM (e.g., KVM) event layout definitions */
    private static final VMNativeEventLayout fLayout = VMNativeEventLayout.getInstance();

    /**
     * Constructor
     *
     * @param trace
     *            The trace to analyze
     */
    public KvmExitStateProvider(@NonNull ITmfTrace trace) {
        super(trace, ID);
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new KvmExitStateProvider(getTrace());
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        ITmfStateSystemBuilder ss = getStateSystemBuilder();
        if (ss == null) {
            return;
        }

        final String eventName = event.getName();
        final long timestamp = event.getTimestamp().toNanos();
        final ITmfEventField content = event.getContent();

        // Process KVM exit events
        if (eventName.equals(fLayout.eventsKVMExit().iterator().next())) {
            // Get the CPU ID and exit reason
            Integer cpuId = content.getFieldValue(Integer.class, fLayout.contextCpuid());
            Integer exitReason = content.getFieldValue(Integer.class, fLayout.contextExitReason());
            Integer vcpuId = content.getFieldValue(Integer.class, fLayout.contextVcpuid());

            if (cpuId == null) {
                return;
            }

            // Create the CPU attribute if it doesn't exist
            int cpuQuark = ss.getQuarkAbsoluteAndAdd("CPUs", String.valueOf(cpuId)); //$NON-NLS-1$

            // Create and increment the KVM exits counter for this CPU
            int exitCountQuark = ss.getQuarkRelativeAndAdd(cpuQuark, "kvm_exits"); //$NON-NLS-1$
            StateSystemBuilderUtils.incrementAttributeLong(ss, timestamp, exitCountQuark, 1);

            // Track the VCPU if available
            if (vcpuId != null) {
                int vcpuQuark = ss.getQuarkRelativeAndAdd(cpuQuark, "vcpu"); //$NON-NLS-1$
                ss.modifyAttribute(timestamp, vcpuId, vcpuQuark);

                // Also track per-VCPU exit information
                int vcpusQuark = ss.getQuarkAbsoluteAndAdd("VCPUs"); //$NON-NLS-1$
                int specificVcpuQuark = ss.getQuarkRelativeAndAdd(vcpusQuark, String.valueOf(vcpuId));
                int vcpuExitCountQuark = ss.getQuarkRelativeAndAdd(specificVcpuQuark, "kvm_exits"); //$NON-NLS-1$
                StateSystemBuilderUtils.incrementAttributeLong(ss, timestamp, vcpuExitCountQuark, 1);

                // If we have exit reason information, track it
                if (exitReason != null) {
                    int reasonQuark = ss.getQuarkRelativeAndAdd(specificVcpuQuark, "exit_reasons", ExitReasonMap.getExitReasonName(exitReason)); //$NON-NLS-1$
                    StateSystemBuilderUtils.incrementAttributeLong(ss, timestamp, reasonQuark, 1);
                }


                // Track which physical CPU this VCPU is running on
                int vcpuOnCpuQuark = ss.getQuarkRelativeAndAdd(specificVcpuQuark, "on_cpu"); //$NON-NLS-1$
                ss.modifyAttribute(timestamp, cpuId, vcpuOnCpuQuark);
            }

            // Track that the CPU is currently in KVM exit mode
            int stateQuark = ss.getQuarkRelativeAndAdd(cpuQuark, "kvm_state"); //$NON-NLS-1$
            ss.modifyAttribute(timestamp, "exit", stateQuark); //$NON-NLS-1$

        } else if (eventName.equals(fLayout.eventsKVMEntry().iterator().next())) {
            // Get the CPU ID
            Integer cpuId = content.getFieldValue(Integer.class, fLayout.contextCpuid());

            // Update the CPU state to show it's back in guest mode
            int cpuQuark = ss.getQuarkAbsoluteAndAdd("CPUs", String.valueOf(cpuId)); //$NON-NLS-1$
            int stateQuark = ss.getQuarkRelativeAndAdd(cpuQuark, "kvm_state"); //$NON-NLS-1$
            ss.modifyAttribute(timestamp, "entry", stateQuark); //$NON-NLS-1$
        }
    }
}