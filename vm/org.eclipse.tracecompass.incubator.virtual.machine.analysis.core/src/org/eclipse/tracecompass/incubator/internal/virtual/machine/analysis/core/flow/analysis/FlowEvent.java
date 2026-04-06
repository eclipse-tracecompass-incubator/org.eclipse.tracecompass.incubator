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
 * Represents a single event within a unified execution flow.
 * <p>
 * A {@code FlowEvent} wraps a {@link KernelEventInfo} and classifies it
 * according to its role in the execution sequence (guest, hypervisor,
 * VM transition, etc.).
 * </p>
 *
 * @author Francois Belias
 */
public class FlowEvent {

    /**
     * The underlying kernel event associated with this flow event.
     */
    final KernelEventInfo fKernelEvent;

    /**
     * The type of this flow event within the execution flow.
     */
    final FlowEventType fType;

    /**
     * Timestamp of the corresponding guest event, used for correlating
     * hypervisor events back to guest execution.
     * <p>
     * This value is primarily used for {@link FlowEventType#HYPERVISOR_EVENT}
     * and is set to {@code -1} when not applicable.
     * </p>
     */
    long fCorrelatedGuestTimestamp = -1;

    /**
     * Constructs a {@code FlowEvent}.
     *
     * @param kernelEvent
     *            the underlying kernel event
     * @param type
     *            the type of the flow event
     */
    FlowEvent(KernelEventInfo kernelEvent, FlowEventType type) {
        this.fKernelEvent = kernelEvent;
        this.fType = type;
    }
}


/**
 * Enumerates the different types of events in a unified execution flow.
 * <p>
 * These types describe the role of each event in the interaction between
 * guest execution and hypervisor activity.
 * </p>
 */
enum FlowEventType {

    /**
     * A regular event occurring in the guest (virtual machine).
     */
    GUEST_EVENT,

    /**
     * A VM exit event, marking the transition from guest to hypervisor.
     */
    VM_EXIT,

    /**
     * An event occurring in the hypervisor (host) while handling a VM exit.
     */
    HYPERVISOR_EVENT,

    /**
     * A VM entry event, marking the transition from hypervisor back to guest.
     */
    VM_ENTRY,

    /**
     * An event occurring in a native (non-virtualized) environment.
     */
    NATIVE
}
