/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.scripting.core.filters;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.antlr.runtime.RecognitionException;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.tracecompass.incubator.internal.filters.core.server.FilterValidation;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.TmfFilterAppliedSignal;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.TraceCompassFilter;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

/**
 * Scripting modules that adds filtering capabilities to Trace Compass scripting
 *
 * @author Geneviève Bastien
 */
@SuppressWarnings("restriction")
public class FiltersScriptingModule {

    /**
     * Apply a global filter to the currently active trace
     *
     * @param regex The filter to apply
     */
    @WrapToScript
    public void applyGlobalFilter(String regex) {
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace == null) {
            // No trace opened, can't filter
            return;
        }
        // Is the filter valid?
        try {
            List<Diagnostic> validate = FilterValidation.validate(regex);
            if (validate.isEmpty()) {
                trace.broadcast(new TmfFilterAppliedSignal(trace, trace, TraceCompassFilter.fromRegex(Collections.singleton(regex), trace)));
            }
        } catch (IOException | RecognitionException e) {
            // Nothing to do, simply don't filter
        }
    }

    /**
     * Remove all global filters
     */
    @WrapToScript
    public void clearGlobalFilters() {
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace == null) {
            // No trace opened, can't filter
            return;
        }
        trace.broadcast(new TmfFilterAppliedSignal(trace, trace, TraceCompassFilter.fromRegex(Collections.emptyList(), trace)));
    }

}
