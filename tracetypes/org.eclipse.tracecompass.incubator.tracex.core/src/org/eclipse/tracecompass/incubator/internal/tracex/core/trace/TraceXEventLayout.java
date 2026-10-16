/*******************************************************************************
 * Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.tracex.core.trace;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.DefaultEventLayout;

/**
 * Kernel event layout for the TraceX (ThreadX) trace type.
 *
 * <p>
 * TraceX is not an LTTng-shaped trace: it has no native {@code sched_switch}
 * tracepoint. Instead, {@link TraceXTrace} synthesizes a single context-switch
 * event name and the {@code prev_tid}/{@code next_tid} fields that the stock
 * kernel CPU-usage analysis
 * ({@code org.eclipse.tracecompass.analysis.os.linux.core.cpuusage.KernelCpuUsageAnalysis})
 * consumes. This layout names those synthetic concepts so the shared analysis
 * applies to TraceX and the stock CPU Usage view populates with no bespoke view
 * code.
 *
 * <p>
 * All other kernel concepts keep their {@link DefaultEventLayout} definitions;
 * they are unused by TraceX but harmless.
 */
public final class TraceXEventLayout extends DefaultEventLayout {

    /** Synthetic context-switch event name emitted by {@link TraceXTrace}. */
    public static final String TRACEX_SCHED_SWITCH = "tracex_sched_switch"; //$NON-NLS-1$

    /** Synthetic field: TID scheduled out at a context switch. */
    public static final String TRACEX_PREV_TID = "prev_tid"; //$NON-NLS-1$
    /** Synthetic field: TID scheduled in at a context switch. */
    public static final String TRACEX_NEXT_TID = "next_tid"; //$NON-NLS-1$
    /** Synthetic field: name of the thread scheduled out. */
    public static final String TRACEX_PREV_COMM = "prev_comm"; //$NON-NLS-1$
    /** Synthetic field: name of the thread scheduled in. */
    public static final String TRACEX_NEXT_COMM = "next_comm"; //$NON-NLS-1$
    /** Synthetic field: priority of the thread scheduled out. */
    public static final String TRACEX_PREV_PRIO = "prev_prio"; //$NON-NLS-1$
    /** Synthetic field: priority of the thread scheduled in. */
    public static final String TRACEX_NEXT_PRIO = "next_prio"; //$NON-NLS-1$
    /** Synthetic field: previous thread state. */
    public static final String TRACEX_PREV_STATE = "prev_state"; //$NON-NLS-1$

    private static @Nullable TraceXEventLayout INSTANCE;

    private TraceXEventLayout() {
        // Singleton; use getInstance().
    }

    /**
     * Get the singleton instance of the TraceX kernel event layout.
     *
     * @return the layout instance
     */
    public static synchronized @NonNull TraceXEventLayout getInstance() {
        TraceXEventLayout inst = INSTANCE;
        if (inst == null) {
            inst = new TraceXEventLayout();
            INSTANCE = inst;
        }
        return inst;
    }

    // ------------------------------------------------------------------------
    // Context switch concepts (the only ones TraceX actually produces)
    // ------------------------------------------------------------------------

    @Override
    public String eventSchedSwitch() {
        return TRACEX_SCHED_SWITCH;
    }

    @Override
    public String fieldPrevTid() {
        return TRACEX_PREV_TID;
    }

    @Override
    public String fieldNextTid() {
        return TRACEX_NEXT_TID;
    }

    @Override
    public String fieldPrevComm() {
        return TRACEX_PREV_COMM;
    }

    @Override
    public String fieldNextComm() {
        return TRACEX_NEXT_COMM;
    }

    @Override
    public String fieldPrevPrio() {
        return TRACEX_PREV_PRIO;
    }

    @Override
    public String fieldNextPrio() {
        return TRACEX_NEXT_PRIO;
    }

    @Override
    public String fieldPrevState() {
        return TRACEX_PREV_STATE;
    }
}
