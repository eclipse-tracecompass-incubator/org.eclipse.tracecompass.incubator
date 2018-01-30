/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.analysis;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualCPU;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualMachine;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * The virtual environment model implementation based on a state system
 * currently being build. The methods of the interface need to block until the
 * analysis has reached the timestamp of the request. It also provides
 * non-blocking methods for the building state provider to use.
 *
 * @author Geneviève Bastien
 */
public class VirtualEnvironmentBuilder extends VirtualEnvironment {

    /**
     * Give the model analysis a headstart on the other analyses, so later events
     * can enhance the model (no need to wait until the end of the trace, this
     * buffer is enough)
     */
    private static final long ANALYSIS_BUFFER = 100000000L;

    private final ITmfStateSystemBuilder fStateSystem;
    private final VirtualMachineModelAnalysis fAnalysis;

    /**
     * Constructor
     *
     * @param stateSystem
     *            The state system containing the model
     * @param analysis
     *            The model analysis this builder is associated to
     */
    public VirtualEnvironmentBuilder(ITmfStateSystemBuilder stateSystem, VirtualMachineModelAnalysis analysis) {
        super(stateSystem);
        fStateSystem = stateSystem;
        fAnalysis = analysis;
    }

    private void waitAnalysis(ITmfEvent event) {
        long ts = event.getTimestamp().toNanos() + ANALYSIS_BUFFER;
        while (!fAnalysis.isQueryable(ts)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Just return, don't try to wait again
                return;
            }
        }
    }

    @Override
    public VirtualMachine getCurrentMachine(ITmfEvent event) {
        waitAnalysis(event);
        return super.getCurrentMachine(event);
    }

    @Override
    public @Nullable VirtualCPU getVirtualCpu(@NonNull ITmfEvent event, @NonNull HostThread ht) {
        waitAnalysis(event);
        return super.getVirtualCpu(event, ht);
    }

    @Override
    public @Nullable VirtualMachine getGuestMachine(@NonNull ITmfEvent event, @NonNull HostThread ht) {
        waitAnalysis(event);
        return super.getGuestMachine(event, ht);
    }

    //---------------------------------------
    // Methods for building the model, non-blocking
    //---------------------------------------

    /**
     * Get the machine this event is for
     *
     * @param event The event to get the machine for
     * @return The machine for this event
     */
    public VirtualMachine getCurrentMachineBuild(ITmfEvent event) {
        VirtualMachine machine = innerGetCurrentMachine(event);
        if (machine == null) {
            synchronized (fStateSystem) {
                String hostId = event.getTrace().getHostId();
                machine = createMachine(fStateSystem, event.getTimestamp().toNanos(), hostId, String.valueOf(event.getTrace().getName()));
                fKnownMachines.put(hostId, machine);
            }

        }
        return machine;
    }

    /**
     * Get the virtual CPU from a guest that corresponds to a specific thread from a
     * host
     *
     * @param event
     *            The event being handled
     * @param ht
     *            The current thread this event belongs to. This thread should be
     *            running on the host.
     * @return The virtual CPU corresponding to this thread or {@code null} if no
     *         virtual CPU corresponds to the thread
     */
    public @Nullable VirtualCPU getVirtualCpuBuild(ITmfEvent event, HostThread ht) {
        return super.getVirtualCpu(event, ht);
    }

    /**
     * Get the guest that corresponds to a specific thread from a host
     *
     * @param event
     *            The event being handled
     * @param ht
     *            The current thread this event belongs to. This thread should be
     *            running on the host.
     * @return The guest machine corresponding to this thread or {@code null} if no
     *         guest corresponds to the thread
     */
    public @Nullable VirtualMachine getGuestMachineBuild(ITmfEvent event, HostThread ht) {
        return super.getGuestMachine(event, ht);
    }

    private static VirtualMachine createMachine(ITmfStateSystemBuilder ss, long ts, String hostId, String traceName) {
        int quark = ss.getQuarkAbsoluteAndAdd(hostId);
        ss.modifyAttribute(ts, traceName, quark);
        return VirtualMachine.newUnknownMachine(hostId, traceName);
    }

    /**
     * Associate a guest machine with a thread on the machine that hosts it
     *
     * @param vm
     *            The Guest virtual machine
     * @param ht
     *            The thread on the host machine
     */
    public void setGuestMachine(VirtualMachine vm, HostThread ht) {
        fTidToVm.put(ht, vm);
    }

    /**
     * Associate a virtual CPU to a thread on the machine that hosts it
     *
     * @param virtualCPU
     *            The virtual CPU
     * @param ht
     *            The thread on the host machine
     */
    public void setGuestCpu(VirtualCPU virtualCPU, HostThread ht) {
        fTidToVcpu.put(ht, virtualCPU);
    }

}
