/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.analysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.IVirtualEnvironmentModel;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualCPU;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualMachine;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

import com.google.common.primitives.Longs;

/**
 * The virtual environment model implementation based on a complete state
 * system. The methods in this class do not need to block as the analysis is
 * finished building
 *
 * @author Geneviève Bastien
 */
public class VirtualEnvironment implements IVirtualEnvironmentModel {

    /** Associate a host's thread to a virtual CPU */
    protected final Map<HostThread, VirtualCPU> fTidToVcpu = new HashMap<>();
    /** Associate a host's thread to a virtual machine */
    protected final Map<HostThread, VirtualMachine> fTidToVm = new HashMap<>();
    /** Maps a virtual machine name to a virtual machine */
    protected final Map<String, VirtualMachine> fKnownMachines = new HashMap<>();

    /**
     * Constructor
     *
     * @param stateSystem
     *            The state system
     */
    public VirtualEnvironment(ITmfStateSystem stateSystem) {
        loadModel(stateSystem);
    }

    private void loadModel(ITmfStateSystem stateSystem) {
        try {
            // Get the intervals at the end, where the model is full
            List<ITmfStateInterval> fullStates = stateSystem.queryFullState(stateSystem.getCurrentEndTime());
            // Create all machines
            List<Integer> quarks = stateSystem.getSubAttributes(ITmfStateSystem.ROOT_ATTRIBUTE, false);
            for (Integer machineQuark : quarks) {
                VirtualMachine machine = getOrCreateMachine(stateSystem, machineQuark, fullStates);
                int vmQuark = stateSystem.optQuarkRelative(machineQuark, VirtualMachineModelAnalysis.GUEST_VMS);
                if (vmQuark > 0) {
                    machine.setHost();
                    List<Integer> vmQuarks = stateSystem.getSubAttributes(vmQuark, false);
                    for (Integer guestQuark : vmQuarks) {
                        fillGuestMachine(stateSystem, guestQuark, machine, fullStates);
                    }
                }
            }
        } catch (StateSystemDisposedException e) {
            // State System disposed, the model will not be used, ignore
        }
    }

    private VirtualMachine getOrCreateMachine(ITmfStateSystem stateSystem, int quark, List<ITmfStateInterval> fullStates) {
        String hostId = stateSystem.getAttributeName(quark);
        VirtualMachine machine = fKnownMachines.get(hostId);
        if (machine == null) {
            machine = VirtualMachine.newUnknownMachine(hostId, String.valueOf(fullStates.get(quark).getValue()));
            fKnownMachines.put(hostId, machine);
        }
        return machine;
    }

    private void fillGuestMachine(ITmfStateSystem stateSystem, int guestQuark, VirtualMachine hostMachine, List<ITmfStateInterval> fullStates) {
        VirtualMachine guest = getOrCreateMachine(stateSystem, guestQuark, fullStates);
        // Set guest and add as a child of the host
        guest.setGuest(1L);
        hostMachine.addChild(guest);

        // Set the process ID
        int processQuark = stateSystem.optQuarkRelative(guestQuark, VirtualMachineModelAnalysis.PROCESS);
        if (processQuark >= 0) {
            Object pid = fullStates.get(processQuark).getValue();
            if (pid instanceof Integer) {
                fTidToVm.put(new HostThread(hostMachine.getHostId(), (Integer) pid), guest);
            }
        }

        for (Integer cpuQuark : stateSystem.getQuarks(guestQuark, VirtualMachineModelAnalysis.CPUS, "*")) { //$NON-NLS-1$
            String cpuStr = stateSystem.getAttributeName(cpuQuark);
            Long cpu = Longs.tryParse(cpuStr);
            if (cpu != null) {
                Object tid = fullStates.get(cpuQuark).getValue();
                if (tid instanceof Integer) {
                    HostThread ht = new HostThread(hostMachine.getHostId(), (Integer) tid);
                    fTidToVm.put(ht, guest);
                    VirtualCPU vcpu = VirtualCPU.getVirtualCPU(guest, cpu);
                    fTidToVcpu.put(ht, vcpu);
                }
            }
        }
    }

    /**
     * Method to retrieve the current machine at time of event. This method does not
     * block and does not create the machine if it does not exist. It will return a
     * <code>null</code> value if the machine does not exist yet.
     *
     * @param event
     *            The event for which to get the current machine
     * @return The current machine or <code>null</code> if the machine does not
     *         exist
     */
    protected @Nullable VirtualMachine innerGetCurrentMachine(ITmfEvent event) {
        String hostId = event.getTrace().getHostId();
        return fKnownMachines.get(hostId);
    }

    @Override
    public VirtualMachine getCurrentMachine(ITmfEvent event) {
        VirtualMachine machine = innerGetCurrentMachine(event);
        if (machine == null) {
            throw new NullPointerException("Machine should not be null"); //$NON-NLS-1$
        }
        return machine;
    }

    @Override
    public @Nullable VirtualCPU getVirtualCpu(ITmfEvent event, HostThread ht) {
        return fTidToVcpu.get(ht);
    }

    @Override
    public @Nullable VirtualMachine getGuestMachine(ITmfEvent event, HostThread ht) {
        return fTidToVm.get(ht);
    }

    @Override
    public Collection<VirtualMachine> getMachines() {
        return fKnownMachines.values();
    }

}
