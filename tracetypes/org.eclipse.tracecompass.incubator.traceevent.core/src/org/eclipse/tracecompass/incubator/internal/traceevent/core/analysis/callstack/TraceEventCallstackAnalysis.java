/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.callstack;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;

/**
 * Call-stack analysis to populate the TMF CallStack View from Trace Event
 * events
 *
 * @author Matthew khouzam
 */
public class TraceEventCallstackAnalysis extends InstrumentedCallStackAnalysis {

    /**
     * ID
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.traceevent.analysis.callstack"; //$NON-NLS-1$

//    private @Nullable Set<@NonNull TmfAbstractAnalysisRequirement> fAnalysisRequirements = null;

    @Override
    protected ITmfStateProvider createStateProvider() {
        return new TraceEventCallStackProvider(Objects.requireNonNull(getTrace()));
    }

    @Override
    public @NonNull Iterable<@NonNull TmfAbstractAnalysisRequirement> getAnalysisRequirements() {
        return Collections.emptyList();
        // TODO: Bring back this requirement
//        Set<@NonNull TmfAbstractAnalysisRequirement> requirements = fAnalysisRequirements;
//        if (requirements == null) {
//            requirements = ImmutableSet.of(new TraceEventCallStackAnalysisRequirement());
//            fAnalysisRequirements = requirements;
//        }
//        return requirements;
    }

    @Override
    protected @NonNull Collection<@NonNull Integer> getEdgeQuarks() {
        ITmfStateSystem ss = getStateSystem();
        if (ss == null) {
            return Collections.emptyList();
        }
        int edgeQuark = ss.optQuarkAbsolute(TraceEventCallStackProvider.EDGES);
        if (edgeQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return Collections.emptyList();
        }
        return ss.getSubAttributes(edgeQuark, false);
    }

}
