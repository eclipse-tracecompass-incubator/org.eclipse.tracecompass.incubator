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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.rocm.core.Activator;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.RocmMetadataAnalysis;
import org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.RocmMetadataStateProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.TmfResolvedSymbol;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Symbol provider to resolve api function ids to their names
 *
 * @author Arnaud Fiorini
 */
public class RocmTraceSymbolProvider implements ISymbolProvider {

    private @NonNull ITmfTrace fTrace;

    /**
     * Constructor
     *
     * @param trace
     *            The trace this provider is for
     */
    public RocmTraceSymbolProvider(@NonNull ITmfTrace trace) {
        fTrace = trace;
    }

    @Override
    public @NonNull ITmfTrace getTrace() {
        return fTrace;
    }

    @Override
    public void loadConfiguration(@Nullable IProgressMonitor monitor) {
        // No configuration
    }

    @Override
    public @Nullable TmfResolvedSymbol getSymbol(long address) {
        RocmMetadataAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(getTrace(),
                RocmMetadataAnalysis.class, RocmMetadataAnalysis.ID);
        if (module == null) {
            /*
             * The analysis is not available for this trace, we won't be able to
             * find the information.
             */
            return null;
        }
        ITmfStateSystem ss = module.getStateSystem();
        if (ss == null || address == -1) {
            return new TmfResolvedSymbol(address, StringUtils.EMPTY);
        }
        String functionName;
        try {
            RocmTrace trace = (RocmTrace) getTrace();
            int nApi = trace.getNApi();
            Integer apiId = (int) (address % nApi);
            int cid = (int) ((address - apiId) / nApi);
            int functionNameQuark = ss.getQuarkAbsolute(RocmMetadataStateProvider.FUNCTION_NAMES);
            int apiQuark = ss.getQuarkRelative(functionNameQuark, apiId.toString());
            functionName = ss.querySingleState(ss.getStartTime() + cid, apiQuark).getValueString();
            if (functionName == null) {
                functionName = StringUtils.EMPTY;
                Activator.getInstance().logWarning("The function name with cid " + cid + //$NON-NLS-1$
                        " and the apiId " + apiId + " was not found in the state system"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        } catch (AttributeNotFoundException | StateSystemDisposedException e) {
            Activator.getInstance().logError(e.getMessage());
            return new TmfResolvedSymbol(address, StringUtils.EMPTY);
        }
        return new TmfResolvedSymbol(address, functionName);
    }

    @Override
    public @Nullable TmfResolvedSymbol getSymbol(int pid, long timestamp, long address) {
        return getSymbol(address);
    }
}
