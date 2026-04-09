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
 * Represents the execution flow of a thread.
 * <p>
 * This class collects and organizes kernel events associated with a specific
 * thread, and can be used for flow analysis in both native and virtualized systems.
 * </p>
 *
 * @author Francois Belias
 */
public class ThreadFlowInfo {

    /**
     * Thread ID (TID).
     */
    final int tid;

    /**
     * Name of the process to which the thread belongs.
     */
    final String processName;

    /**
     * List of kernel events associated with this thread.
     * Events are typically stored in chronological order.
     */
    final List<KernelEventInfo> events = new ArrayList<>();

    /**
     * Constructs a {@code ThreadFlowInfo} instance.
     *
     * @param tid
     *            the thread ID
     * @param processName
     *            the name of the process
     */
    ThreadFlowInfo(int tid, String processName) {
        this.tid = tid;
        this.processName = processName;
    }

    /**
     * Adds a kernel event to this thread's flow.
     *
     * @param evt
     *            the event to add
     */
    public void addEvent(KernelEventInfo evt) {
        events.add(evt);
    }
}
