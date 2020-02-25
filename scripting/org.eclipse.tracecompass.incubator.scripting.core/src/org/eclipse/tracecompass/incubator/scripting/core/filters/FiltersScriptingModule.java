/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.scripting.core.filters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.runtime.RecognitionException;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelThreadInformationProvider;
import org.eclipse.tracecompass.analysis.os.linux.core.model.OsStrings;
import org.eclipse.tracecompass.incubator.internal.filters.core.server.FilterValidation;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filter.parser.IFilterStrings;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.TmfFilterAppliedSignal;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.TraceCompassFilter;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Scripting modules that adds filtering capabilities to Trace Compass
 * scripting.
 * <p>
 * With this module, a script can create a complex query from the trace data and
 * then call the {@link #applyGlobalFilter(String)} method to apply the filter
 * on all views of Trace Compass.
 * </p>
 * <p>
 * This module also contains helper methods that transform a simple query, but
 * that may not be easily understandable by Trace Compass, into a complex one
 * that will return a maximum of results.
 * </p>
 * <p>
 * This requires the <code>Trace Compass Filters (Incubation)</code> feature to
 * be installed.
 * </p>
 *
 * @author Geneviève Bastien
 */
@SuppressWarnings("restriction")
public class FiltersScriptingModule {

    /**
     * Apply a global filter to the currently active trace
     *
     * @param regex
     *            The filter to apply
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
     * Apply a global filter to all Trace Compass views on the threads whose
     * name contains the name in parameter.
     *
     * @param name
     *            The name of the thread, as can be seen in the Control Flow
     *            View.
     */
    @WrapToScript
    public void filterByThreadName(String name) {
        // iterate over names looking for a match. make a list with those match
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace == null) {
            return;
        }
        List<Integer> tids = findMatchName(name, trace);
        if (tids.size() == 0) {
            return;
        }
        // create the regex for tids
        String prefix = OsStrings.tid() + IFilterStrings.EQUAL;
        String separator = ' ' + IFilterStrings.OR + ' ' + prefix;
        StringJoiner regexJoiner = new StringJoiner(separator, prefix, ""); //$NON-NLS-1$
        for (Integer tid : tids) {
            regexJoiner.add(tid.toString());
        }
        // applyGlobalFilter
        String regexExpression = regexJoiner.toString();
        if (regexExpression != null) {
            applyGlobalFilter(regexExpression);
        }
    }

    private static List<Integer> findMatchName(String name, ITmfTrace trace) {
        Map<Integer, String> executableNames = getExecutableNames(trace);
        List<Integer> tids = new ArrayList<>();
        Pattern checkRegex = Pattern.compile(name); // regex expression
        for (Entry<Integer, String> entry : executableNames.entrySet()) {
            Matcher regexMatcher = checkRegex.matcher(entry.getValue());
            if (regexMatcher.find()) {
                tids.add(entry.getKey());
            }
        }
        return tids;
    }

    /*
     * FIXME TIDs can be reused for different traces, but HostId is not a field
     * that is well supported right now.
     *
     * FIXME if a tid is reused then it would need a filter that depends on
     * time, but time fields are not well supported right now.
     */
    private static Map<Integer, String> getExecutableNames(ITmfTrace trace) {
        Map<Integer, String> executableNames = new HashMap<>();
        KernelAnalysisModule kernelAnlysis = TmfTraceUtils.getAnalysisModuleOfClass(trace, KernelAnalysisModule.class, KernelAnalysisModule.ID);
        if (kernelAnlysis == null) {
            return Collections.emptyMap();
        }
        Collection<Integer> threadIds = KernelThreadInformationProvider.getThreadIds(kernelAnlysis);
        if (threadIds.isEmpty()) {
            return Collections.emptyMap();
        }
        for (Integer tid : threadIds) {
            String execName = KernelThreadInformationProvider.getExecutableName(kernelAnlysis, tid);
            if (execName != null) {
                executableNames.put(tid, execName);
            }
        }

        return executableNames;
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
