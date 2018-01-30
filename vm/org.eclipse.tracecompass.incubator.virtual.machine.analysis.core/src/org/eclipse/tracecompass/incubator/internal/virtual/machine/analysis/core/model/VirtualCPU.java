/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model;

import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.StateValues;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * This class represents a virtual CPU, which is a CPU running on a guest. It
 * associates the guest CPU ID to a virtual machine of the model.
 *
 * @TODO Review this class's API with the new virtual machine environment
 *
 * @author Geneviève Bastien
 */
public final class VirtualCPU {

    private static final Table<VirtualMachine, Long, @Nullable VirtualCPU> VIRTUAL_CPU_TABLE = HashBasedTable.create();

    private final VirtualMachine fVm;
    private final Long fCpuId;
    /* Current state of the cpu. */
    private ITmfStateValue fCurrentState;
    /* Current thread of the cpu. */
    private ITmfStateValue fCurrentThread;
    private ITmfStateValue fStateBeforeIRQ;
    private @Nullable VirtualCPU fNextLayerVCPU;

    /**
     * Return the virtual CPU for to the virtual machine and requested CPU ID
     *
     * @param vm
     *            The virtual machine
     * @param cpu
     *            the CPU number
     * @return the virtual CPU
     */
    public static synchronized VirtualCPU getVirtualCPU(VirtualMachine vm, Long cpu) {
        VirtualCPU ht = VIRTUAL_CPU_TABLE.get(vm, cpu);
        if (ht == null) {
            ht = new VirtualCPU(vm, cpu);
            VIRTUAL_CPU_TABLE.put(vm, cpu, ht);
        }
        return ht;
    }

    /**
     * Get the virtual CPUs for a virtual machine
     *
     * @param machine
     *            The Virtual Machine to get the CPUs for
     * @return The map of virtual CPUs for this machine
     */
    public static synchronized @Nullable Map<Long, @Nullable VirtualCPU> getVirtualCPUs(VirtualMachine machine) {
        return VIRTUAL_CPU_TABLE.row(machine);
    }

    private VirtualCPU(VirtualMachine vm, Long cpu) {
        fVm = vm;
        fCpuId = cpu;
        fCurrentState = StateValues.CPU_STATUS_IDLE_VALUE;
        fCurrentThread = TmfStateValue.newValueInt(-1);
        fStateBeforeIRQ = StateValues.CPU_STATUS_IDLE_VALUE;
        fNextLayerVCPU = null;
    }

    /**
     * Get the CPU ID of this virtual CPU
     *
     * @return The zero-based CPU ID
     */
    public Long getCpuId() {
        return fCpuId;
    }

    /**
     * Get the virtual machine object this virtual CPU belongs to
     *
     * @return The guest Virtual Machine
     */
    public VirtualMachine getVm() {
        return fVm;
    }

    @Override
    public String toString() {
        return "VirtualCPU: [" + fVm + ',' + fCpuId + ']'; //$NON-NLS-1$
    }

    /**
     * Get the current state.
     *
     * @return the currentState
     */
    public ITmfStateValue getCurrentState() {
        return fCurrentState;
    }

    /**
     * Set the current state.
     *
     * @param currentState
     *            the currentState to set
     */
    public void setCurrentState(ITmfStateValue currentState) {
        this.fCurrentState = currentState;
    }

    /**
     * Get the current thread.
     *
     * @return the currentThread
     */
    public ITmfStateValue getCurrentThread() {
        return fCurrentThread;
    }

    /**
     * Set the current state.
     *
     * @param currentThread
     *            the currentThread to set
     */
    public void setCurrentThread(ITmfStateValue currentThread) {
        this.fCurrentThread = currentThread;
    }

    /**
     * @return the stateBeforeIRQ
     */
    public ITmfStateValue getStateBeforeIRQ() {
        return fStateBeforeIRQ;
    }

    /**
     * @param state
     *            the stateBeforeIRQ to set
     */
    public void setStateBeforeIRQ(ITmfStateValue state) {
        fStateBeforeIRQ = state;
    }

    public void setNextLayerVCPU(VirtualCPU vcpu) {
        fNextLayerVCPU = vcpu;
    }

    public @Nullable VirtualCPU getNextLayerVCPU() {
        return fNextLayerVCPU;
    }

}