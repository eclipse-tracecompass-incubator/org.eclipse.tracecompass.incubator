/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.rocm.core.trace;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.old.RocmMetadataAnalysis;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProviderFactory;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Factory for the ROCm Symbol provider
 *
 * @author Arnaud Fiorini
 */
public class RocmTraceSymbolProviderFactory implements ISymbolProviderFactory {

    @Override
    public @Nullable ISymbolProvider createProvider(@NonNull ITmfTrace trace) {
        RocmMetadataAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace,
                RocmMetadataAnalysis.class, RocmMetadataAnalysis.ID);
        if (module == null) {
            return null;
        }
        return new RocmTraceSymbolProvider(trace);
    }
}
