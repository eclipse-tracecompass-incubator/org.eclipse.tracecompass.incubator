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

package org.eclipse.tracecompass.incubator.internal.perf.core.symbol;

import org.eclipse.tracecompass.incubator.internal.perf.core.PerfRecord;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * State provider that tracks the MMAP/MMAP2 regions of each process, so the
 * symbol provider can later resolve a given address to a mapped file.
 *
 * Attribute layout: {@code /<pid>/<baseAddr>} → filename, where
 * {@code baseAddr} is {@code addr - pgoff} (the effective ELF load base).
 */
public class PerfDataMmapStateProvider extends AbstractTmfStateProvider {

    private static final int VERSION = 1;

    private static final String MMAP_NAME = "MMAP"; //$NON-NLS-1$
    private static final String MMAP2_NAME = "MMAP2"; //$NON-NLS-1$

    /**
     * Constructor
     *
     * @param trace
     *            the trace to analyse
     */
    public PerfDataMmapStateProvider(ITmfTrace trace) {
        super(trace, "Perf Data MMap Symbol Resolution"); //$NON-NLS-1$
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public ITmfStateProvider getNewInstance() {
        return new PerfDataMmapStateProvider(getTrace());
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        String name = event.getType().getName();
        if (!MMAP_NAME.equals(name) && !MMAP2_NAME.equals(name)) {
            return;
        }
        ITmfStateSystemBuilder ss = getStateSystemBuilder();
        if (ss == null) {
            return;
        }
        Object value = event.getContent().getValue();
        if (!(value instanceof PerfRecord)) {
            return;
        }
        PerfRecord rec = (PerfRecord) value;
        Object pidObj = rec.getField("pid"); //$NON-NLS-1$
        Object addrObj = rec.getField("addr"); //$NON-NLS-1$
        Object pgoffObj = rec.getField("pgoff"); //$NON-NLS-1$
        Object filenameObj = rec.getField("filename"); //$NON-NLS-1$
        if (!(pidObj instanceof Integer) || !(addrObj instanceof Long)
                || !(pgoffObj instanceof Long) || !(filenameObj instanceof String)) {
            return;
        }
        int pid = (Integer) pidObj;
        if (pid < 0) {
            // Skip kernel-synthesized MMAPs with pid = -1 ([kernel.kallsyms], modules, ...)
            return;
        }
        long addr = (Long) addrObj;
        long pgoff = (Long) pgoffObj;
        String filename = (String) filenameObj;
        if (filename.isEmpty()) {
            return;
        }
        long loadBase = addr - pgoff;

        long ts = event.getTimestamp().toNanos();
        if (ts <= 0) {
            // Use the start of the trace for MMAPs that carry no timestamp.
            ts = getTrace().getStartTime().toNanos();
        }
        int quark = ss.getQuarkAbsoluteAndAdd(String.valueOf(pid), String.valueOf(loadBase));
        ss.modifyAttribute(ts, filename, quark);
    }
}
