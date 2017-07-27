/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.perf.profiling.core.symbol;

import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * State provider for perf mmap analysis
 *
 * @author Geneviève Bastien
 */
public class PerfMmapStateProvider extends AbstractTmfStateProvider {

    private static final int VERSION = 1;
    private static final String MMAP_PREFIX = "perf_mmap"; //$NON-NLS-1$

    /**
     * Constructor
     *
     * @param trace The trace
     */
    public PerfMmapStateProvider(ITmfTrace trace) {
        super(trace, "Perf MMap Symbol Resolution"); //$NON-NLS-1$
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public ITmfStateProvider getNewInstance() {
        return new PerfMmapStateProvider(getTrace());
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        if (!event.getName().startsWith(MMAP_PREFIX)) {
            return;
        }
        ITmfStateSystemBuilder ss = getStateSystemBuilder();
        if (ss == null) {
            return;
        }
        Long pid = event.getContent().getFieldValue(Long.class, "pid"); //$NON-NLS-1$
        Long start = event.getContent().getFieldValue(Long.class, "start"); //$NON-NLS-1$
        String filename = event.getContent().getFieldValue(String.class, "filename"); //$NON-NLS-1$
        if (pid == null || start == null || filename == null) {
            return;
        }

        int startQuark = ss.getQuarkAbsoluteAndAdd(String.valueOf(pid), String.valueOf(start));
        ss.modifyAttribute(event.getTimestamp().toNanos(), filename, startQuark);
    }

}
