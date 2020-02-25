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

package org.eclipse.tracecompass.incubator.scripting.core.analysis;

import org.eclipse.ease.modules.ScriptParameter;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.scripting.core.analysis.TmfScriptAnalysis;
import org.eclipse.tracecompass.incubator.internal.scripting.core.trace.ScriptEventRequest;
import org.eclipse.tracecompass.incubator.scripting.core.trace.ScriptEventsIterator;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Analysis class that can be used with the scripts. It provides an event
 * iterator, as well as backends to store data. Scripts can thus parse events
 * and fill the backend appropriately.
 *
 * @author Geneviève Bastien
 */
public class ScriptedAnalysis {

    private final ITmfTrace fTrace;
    private final String fName;
    private @Nullable ITmfStateSystemBuilder fStateSystem;

    /**
     * Constructor
     *
     * package-private because it is only expected to be constructed by the
     * module.
     *
     * @param activeTrace
     *            The trace to associate with this analysis
     * @param name
     *            The name of the analysis
     */
    ScriptedAnalysis(ITmfTrace activeTrace, String name) {
        fTrace = activeTrace;
        fName = name;
    }

    /**
     * Get a state system to go with this analysis. If an analysis of the same
     * name already exists, it can be re-used instead of re-created.
     *
     * @param useExisting
     *            if <code>true</code>, any state system with the same name for
     *            the trace will be reused, otherwise, a new state system will
     *            be created
     * @return A state system builder
     */
    @WrapToScript
    public @Nullable ITmfStateSystemBuilder getStateSystem(@ScriptParameter(defaultValue = "false") boolean useExisting) {

        ITmfStateSystemBuilder stateSystem = fStateSystem;
        if (stateSystem != null) {
            return stateSystem;
        }
        TmfScriptAnalysis analysisModule = TmfTraceUtils.getAnalysisModuleOfClass(fTrace, TmfScriptAnalysis.class, TmfScriptAnalysis.ID);
        if (analysisModule == null) {
            return null;
        }
        analysisModule.schedule();
        analysisModule.waitForCompletion();

        stateSystem = (ITmfStateSystemBuilder) analysisModule.getStateSystem(fName, useExisting);
        fStateSystem = stateSystem;
        return stateSystem;
    }

    /**
     * Get an iterator to iterate chronologically through the events of the
     * trace. To reduce overhead of passing all events to/from the script when
     * only a subset of events is requested, the
     * {@link ScriptEventsIterator#addEvent(String)} method can be used to set
     * the events to filter.
     *
     * Thus, to iterate through a trace in a scripted analysis, one can just do
     * the following snippet (javascript)
     *
     * <pre>
     * var iter = analysis.getEventIterator();
     *
     * var event = null;
     * while (iter.hasNext()) {
     *
     *     event = iter.next();
     *
     *     // Do something with the event
     * }
     * </pre>
     *
     * @return The event iterator, starting from the first event
     */
    @WrapToScript
    public ScriptEventsIterator getEventIterator() {
        ScriptEventRequest scriptEventRequest = new ScriptEventRequest();
        fTrace.sendRequest(scriptEventRequest);
        return scriptEventRequest.getEventIterator();
    }

    /**
     * Get the trace, not to be used by scripts.
     *
     * @return The trace
     */
    public ITmfTrace getTrace() {
        return fTrace;
    }

    /**
     * Get the name of this analysis, not to be used by scripts
     *
     * @return The name of the analysis
     */
    public String getName() {
        return fName;
    }

    /**
     * Make sure the analysis is complete and the state system, if any, is
     * closed. It will close the state system at the end time of the trace if is
     * has not been closed previously. If no state system was requested through
     * the {@link #getStateSystem(boolean)} before hand, nothing will happen.
     */
    public void complete() {
        ITmfStateSystemBuilder stateSystem = fStateSystem;
        if (stateSystem == null) {
            return;
        }
        if (!stateSystem.waitUntilBuilt(0)) {
            stateSystem.closeHistory(getTrace().getEndTime().toNanos());
        }
    }

    /**
     * Get whether this analysis is complete, ie, if a state systemw as
     * requested by the {@link #getStateSystem(boolean)} method, then the state
     * system has been closed.
     *
     * @return Whether the analysis is complete and the state system was closed
     */
    public boolean isComplete() {
        ITmfStateSystemBuilder stateSystem = fStateSystem;
        if (stateSystem == null) {
            return true;
        }
        return stateSystem.waitUntilBuilt(0);
    }
}
