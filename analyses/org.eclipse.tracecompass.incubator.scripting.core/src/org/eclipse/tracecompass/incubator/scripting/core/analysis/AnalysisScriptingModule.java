/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.scripting.core.analysis;

import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Provide an API to create an analysis. Using an analysis links the outputs of
 * the analysis with the trace. For example, the backends (state systems,
 * segment store) filled by the script will be available under the trace.
 * <p>
 * Typical use of this module is for scripts who wish to parse the events of a
 * trace or experiment and save data in a backend to be displayed later.
 * </p>
 * <p>
 * Example scripts with data providers can be found here:
 * <ul>
 * <li><a href="../doc-files/scriptedDataProvider.js">A scripted time graph data
 * provider</a> with script-defined entries and arrows (javascript)</li>
 * <li><a href="../doc-files/basicAnalysis.js">A basic analysis</a> building an
 * state system and showing its data in a time graph (javascript)</li>
 * <li><a href="../doc-files/basicAnalysis.py">Same basic analysis as before,</a>
 * building an state system and showing its data in a time graph, in python</li>
 * </ul>
 *
 * @author Geneviève Bastien
 */
public class AnalysisScriptingModule {

    /**
     * Create an analysis with the given name. It will be associated with the
     * currently active trace. If there is no active trace, it will return
     * <code>null</code>.
     *
     * @param name
     *            The name of the analysis
     * @return The new analysis, for the active trace, or <code>null</code> if
     *         there is no active trace.
     */
    @WrapToScript
    public @Nullable ScriptedAnalysis getAnalysis(String name) {
        ITmfTrace activeTrace = TmfTraceManager.getInstance().getActiveTrace();
        if (activeTrace == null) {
            return null;
        }
        return new ScriptedAnalysis(activeTrace, name);
    }

    /**
     * A wrapper method to get the value of an event field. If the field itself
     * does not exist, it will try to resolve an aspect from the trace the event
     * is from.
     *
     * @param event
     *            The event for which to get the field
     * @param fieldName
     *            The name of the field to fetch
     * @return The field value object, or <code>null</code> if the field is not
     *         found
     */
    @WrapToScript
    public @Nullable Object getFieldValue(ITmfEvent event, String fieldName) {

        final ITmfEventField field = event.getContent().getField(fieldName);

        /* If the field does not exist, see if it's a special case */
        if (field == null) {
            // This will allow to use any column as input
            return TmfTraceUtils.resolveAspectOfNameForEvent(event.getTrace(), fieldName, event);
        }
        return field.getValue();

    }

    /**
     * A wrapper method to get a specified analysis of a trace. It returns an
     * existing analysis, whether builtin or data-driven, for the trace. These
     * analyses cannot create state systems or anything else, but their results
     * can be queried and visualized in a script.
     * <p>
     * The analyses can be queried by ID, or by name, ie the text that is
     * displayed in the Trace Compass UI.
     *
     * @param trace
     *            The trace being analyzed.
     * @param analysisName
     *            The analysis module name or ID to get.
     * @return The module
     */
    @WrapToScript
    public @Nullable IAnalysisModule getTraceAnalysis(ITmfTrace trace, String analysisName) {
        for (ITmfTrace childTrace : TmfTraceManager.getTraceSetWithExperiment(trace)) {
            for (IAnalysisModule module : childTrace.getAnalysisModules()) {
                if (module.getName().equals(analysisName) || module.getId().equals(analysisName)) {
                    return module;
                }
            }
        }
        return null;
    }

}