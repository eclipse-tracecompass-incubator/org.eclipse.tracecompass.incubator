/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.callstack;

import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.trace.TraceEventTrace;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableSet;

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

    private @Nullable Set<@NonNull TmfAbstractAnalysisRequirement> fAnalysisRequirements = null;

    @Override
    public boolean setTrace(ITmfTrace trace) throws TmfAnalysisException {
        if (!(trace instanceof TraceEventTrace)) {
            return false;
        }
        return super.setTrace(trace);
    }

    @Override
    public TraceEventTrace getTrace() {
        return (TraceEventTrace) super.getTrace();
    }

    @Override
    protected ITmfStateProvider createStateProvider() {
        return new TraceEventCallStackProvider(Objects.requireNonNull(getTrace()), getSegmentStore());
    }

    @Override
    public @NonNull Iterable<@NonNull TmfAbstractAnalysisRequirement> getAnalysisRequirements() {

        Set<@NonNull TmfAbstractAnalysisRequirement> requirements = fAnalysisRequirements;
        if (requirements == null) {
            requirements = ImmutableSet.of(new TraceEventCallStackAnalysisRequirement());
            fAnalysisRequirements = requirements;
        }
        return requirements;
    }

}
