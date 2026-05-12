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
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProviderFactory;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Registers {@link PerfDataMmapSymbolProvider} for any trace that has a
 * {@link PerfDataMmapAnalysisModule}.
 */
public class PerfDataMmapSymbolProviderFactory implements ISymbolProviderFactory {

    @Override
    public @Nullable ISymbolProvider createProvider(@NonNull ITmfTrace trace) {
        PerfDataMmapAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace,
                PerfDataMmapAnalysisModule.class, PerfDataMmapAnalysisModule.ID);
        if (module == null) {
            return null;
        }
        return new PerfDataMmapSymbolProvider(trace, module);
    }
}
