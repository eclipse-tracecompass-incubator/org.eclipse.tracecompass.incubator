/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model;

import java.util.Collection;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * Interface that represents the model of a virtual environment. The
 * implementations should be hypervisor-agnostic and model both virtual machines
 * and containers. All calls to this interface's methods should be blocking, ie
 * wait until the data is available before returning an answer.
 *
 * @author Geneviève Bastien
 */
public interface IVirtualEnvironmentModel {

    /**
     * This value corresponds to the root namespace ID, as defined in the linux
     * source code linux/include/linux/proc_ns.h
     *
     * <pre>
     * enum {
     *    [...]
     *    PROC_PID_INIT_INO   = 0xEFFFFFFCU,
     *    [...]
     * };
     * </pre>
     */
    long ROOT_NAMESPACE_LINUX = 4026531836L;

    /**
     * Get the machine that ran this event
     *
     * @param event
     *            The trace event
     * @return The machine this event was run on or {@code null} if the machine is
     *         not one belonging to this model.
     */
    VirtualMachine getCurrentMachine(ITmfEvent event);

    /**
     * Get all the machines, hosts, guests and unknowns, described by this virtual
     * environment. This method is not blocking.
     *
     * @return All the known machines
     */
    Collection<VirtualMachine> getMachines();

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
    @Nullable
    VirtualCPU getVirtualCpu(ITmfEvent event, HostThread ht);

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
    @Nullable
    VirtualMachine getGuestMachine(ITmfEvent event, HostThread ht);

    /**
     * Get the host thread corresponding to a virtual CPU
     *
     * @param vcpu
     *            The vcpu for which to get the thread
     * @return The HostThread corresponding to this CPU, or <code>null</code> if no
     *         such thread is found
     */
    // @Nullable
    // HostThread getVirtualCpuTid(VirtualCPU vcpu);

}
