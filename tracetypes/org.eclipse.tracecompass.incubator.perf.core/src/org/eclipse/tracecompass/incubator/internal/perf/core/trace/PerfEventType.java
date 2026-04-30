/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.perf.core.trace;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.tracecompass.incubator.internal.perf.core.PerfConstants;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;

/**
 * Static registry of {@link ITmfEventType} instances for the perf.data event
 * names we can emit. This is a trivial mapping from the kernel
 * {@code PERF_RECORD_*} identifiers to their canonical names.
 */
public final class PerfEventType {

    private static final Map<Integer, ITmfEventType> TYPES = new HashMap<>();

    static {
        put(PerfConstants.PERF_RECORD_MMAP, "MMAP"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_LOST, "LOST"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_COMM, "COMM"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_EXIT, "EXIT"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_THROTTLE, "THROTTLE"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_UNTHROTTLE, "UNTHROTTLE"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_FORK, "FORK"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_READ, "READ"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_SAMPLE, "SAMPLE"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_MMAP2, "MMAP2"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_AUX, "AUX"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_ITRACE_START, "ITRACE_START"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_LOST_SAMPLES, "LOST_SAMPLES"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_SWITCH, "SWITCH"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_SWITCH_CPU_WIDE, "SWITCH_CPU_WIDE"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_NAMESPACES, "NAMESPACES"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_CGROUP, "CGROUP"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_KSYMBOL, "KSYMBOL"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_BPF_EVENT, "BPF_EVENT"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_TEXT_POKE, "TEXT_POKE"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_AUX_OUTPUT_HW_ID, "AUX_OUTPUT_HW_ID"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_FINISHED_ROUND, "FINISHED_ROUND"); //$NON-NLS-1$
        put(PerfConstants.PERF_RECORD_FINISHED_INIT, "FINISHED_INIT"); //$NON-NLS-1$
    }

    private PerfEventType() {
        // utility class
    }

    private static void put(int type, String name) {
        TYPES.put(type, new TmfEventType(name, null));
    }

    /**
     * Look up the event type for a PERF_RECORD_* value.
     *
     * @param type
     *            the type value
     * @return the matching {@link ITmfEventType}
     */
    public static ITmfEventType lookup(int type) {
        ITmfEventType t = TYPES.get(type);
        if (t == null) {
            t = new TmfEventType("PERF_RECORD_" + type, null); //$NON-NLS-1$
            TYPES.put(type, t);
        }
        return t;
    }
}
