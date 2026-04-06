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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a complete execution sequence in a virtualized environment:
 * <pre>
 * VM ENTRY → GUEST → VM EXIT → Hypervisor (Host) → VM Entry
 * </pre>
 * <p>
 * This class aggregates events occurring in both the guest and the hypervisor,
 * as well as the VM exit and entry transitions that connect them.
 * </p>
 *
 * @author Francois Belias
 */
public class ExecutionSequence {

    /**
     * List of events that occurred in the guest before the VM exit.
     */
    private final List<FlowEvent> guestEvents = new ArrayList<>();

    /**
     * List of events that occurred in the hypervisor (host)
     * between VM exit and VM entry.
     */
    private final List<FlowEvent> hypervisorEvents = new ArrayList<>();

    /**
     * Event representing the VM exit (transition from guest to host).
     */
    private FlowEvent vmExit;

    /**
     * Event representing the VM entry (transition from host back to guest).
     */
    private FlowEvent vmEntry;

    /**
     * Adds an event to the guest event list.
     *
     * @param event
     *            the guest event to add
     */
    void addGuestEvent(FlowEvent event) {
        guestEvents.add(event);
    }

    /**
     * Adds an event to the hypervisor event list.
     *
     * @param event
     *            the hypervisor event to add
     */
    void addHypervisorEvent(FlowEvent event) {
        hypervisorEvents.add(event);
    }

    /**
     * Sets the VM exit event.
     *
     * @param event
     *            the VM exit event
     */
    void setVmExit(FlowEvent event) {
        this.vmExit = event;
    }

    /**
     * Sets the VM entry event.
     *
     * @param event
     *            the VM entry event
     */
    void setVmEntry(FlowEvent event) {
        this.vmEntry = event;
    }

    /**
     * Indicates whether the execution sequence is complete.
     * <p>
     * A sequence is considered complete if it contains:
     * <ul>
     *   <li>At least one guest event</li>
     *   <li>A VM exit event</li>
     *   <li>A VM entry event</li>
     * </ul>
     * </p>
     *
     * @return {@code true} if the sequence is complete, {@code false} otherwise
     */
    boolean isComplete() {
        return !guestEvents.isEmpty() && vmExit != null && vmEntry != null;
    }
}