/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.perf.profiling.core.symbol;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProviderFactory;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Get the symbol provider factory for the perf mmap module
 *
 * @author Geneviève Bastien
 */
public class PerfMmapSymbolProviderFactory implements ISymbolProviderFactory {

    @Override
    public @Nullable ISymbolProvider createProvider(@NonNull ITmfTrace trace) {
        /*
         * This applies only to traces that have the PerfMmap module
         */
        PerfMmapAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, PerfMmapAnalysisModule.class, PerfMmapAnalysisModule.ID);
        if (module == null) {
            return null;
        }
        return new PerfMmapSymbolProvider(trace, module);
    }

}
