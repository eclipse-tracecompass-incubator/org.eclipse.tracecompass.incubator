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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * perf.data MMAP/MMAP2 state-system analysis; provides the per-process map
 * of loaded libraries used by {@link PerfDataMmapSymbolProvider}.
 */
public class PerfDataMmapAnalysisModule extends TmfStateSystemAnalysisModule {

    /** Analysis ID registered via plugin.xml. */
    public static final String ID = "org.eclipse.tracecompass.incubator.perf.core.symbol.mmap"; //$NON-NLS-1$

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            throw new IllegalStateException("Creating state provider: trace should not be null"); //$NON-NLS-1$
        }
        return new PerfDataMmapStateProvider(trace);
    }
}
