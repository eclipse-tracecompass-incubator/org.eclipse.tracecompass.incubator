/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.ui.views.vresources;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.StateValues;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;

/**
 * Represents a machine in the ui
 *
 * TODO: Move this class to the model
 *
 * @author Cedric Biancheri
 */
@NonNullByDefault
public class Machine {

    private final String fMachineName;
    private final String fHostId;
    private @Nullable Machine fHost = null;
    private Set<Processor> fCpus = new HashSet<>();
    private Set<Processor> fPcpus = new HashSet<>();
    private Set<Machine> fContainers = new HashSet<>();
    private Set<Machine> fVirtualMachines = new HashSet<>();
    private ITmfStateValue fMachineType;

    /**
     * Constructor
     *
     * @param name
     *            Name of the machine
     * @param hostId
     *            The host ID of the machine
     * @param type
     *            The type state value of the machine
     */
    public Machine(String name, String hostId, ITmfStateValue type) {
        this(name, hostId, type, Collections.emptyList());
    }

    /**
     * Constructor of a machine
     *
     * @param name
     *            The name of the machine
     * @param hostId
     *            The host ID of the machine
     * @param type
     *            The type of machine
     * @param pcpus
     *            The list of CPUs used by this machine
     */
    public Machine(String name, String hostId, ITmfStateValue type, Collection<String> pcpus) {
        fMachineName = name;
        fMachineType = type;
        fHostId = hostId;
        for (String pcpu : pcpus) {
            fPcpus.add(new Processor(pcpu, this));
        }
    }

    /**
     * Create a container in this machine with the CPUs sent in parameter. The
     * container will have the current machine as host and it will be added to
     * the container list.
     *
     * @param name
     *            The name of the container
     * @param hostId
     *            The host ID of the machine
     * @param physCpus
     *            The list of physical cpus IDs
     * @return The newly created container.
     */
    public Machine createContainer(String name, String hostId, List<String> physCpus) {
        Machine container = new Machine(name, hostId, StateValues.MACHINE_CONTAINER_VALUE, physCpus);
        container.setHost(this);
        fContainers.add(container);
        return container;
    }

    /**
     * Return whether this machine is a container
     *
     * @return whether the machine is a container
     */
    public boolean isContainer() {
        return fMachineType == StateValues.MACHINE_CONTAINER_VALUE;
    }

    /**
     * Get the name of this machine
     *
     * @return The name of the machine
     */
    public String getMachineName() {
        return fMachineName;
    }

    /**
     * Get the ID of the host trace
     *
     * @return The host ID of the trace this machine was described in
     */
    public String getHostId() {
        return fHostId;
    }

    /**
     * Set the machine on which this machine is running
     *
     * @param host
     *            the host
     */
    public void setHost(Machine host) {
        fHost = host;
    }

    /**
     * Return the machine on which this one runs, if it is not the physical
     * machine
     *
     * @return The host machine if it is not the physical machine
     */
    public @Nullable Machine getHost() {
        return fHost;
    }

    /**
     * Get the containers running on this machine
     *
     * @return The collection of containers
     */
    public Collection<Machine> getContainers() {
        return fContainers;
    }

    /**
     * Get the guest virtual machines running on this machine
     *
     * @return The collection of guest virtual machines
     */
    public Collection<Machine> getVirtualMachines() {
        return fVirtualMachines;
    }

    /**
     * Add a CPU
     *
     * @param cpu
     *            The CPU number
     */
    public void addCpu(String cpu) {
        fCpus.add(new Processor(cpu, this));
    }

    /**
     * Add a physical CPU
     *
     * @param pcpu
     *            The physical CPU number
     */
    public void addPCpu(String pcpu) {
        fPcpus.add(new Processor(pcpu, this));
    }

    /**
     * Add a virtual machine on this host
     *
     * @param machine
     *            The machine to add as a guest
     */
    public void addVirtualMachine(Machine machine) {
        if ((machine.fMachineType.unboxInt() & StateValues.MACHINE_GUEST) != StateValues.MACHINE_GUEST) {
            return;
        }
        fVirtualMachines.add(machine);
    }

    /**
     * Get the CPUs used by this machine, virtual or physical depending on which
     * type of machine this is
     *
     * @return The CPUs used by this machine
     */
    public Collection<Processor> getCpus() {
        return fCpus;
    }

    /**
     * Get the physical CPUs on the host used by this machine
     *
     * @return The physical CPUs of the host used by this machine
     */
    public Collection<Processor> getPhysicalCpus() {
        return fPcpus;
    }

    @Override
    public String toString() {
        return fMachineName + (fHost != null ? " in " + fHost : ""); //$NON-NLS-1$//$NON-NLS-2$
    }

}
