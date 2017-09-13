/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.counter;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.counters.core.CounterAnalysis;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.trace.TraceEventTrace;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * This is a counter analysis. It handles the 'C' type events as specified in
 * https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU/
 * in the section "Counter Events"
 *
 * @author Matthew Khouzam
 */
public class TraceEventCounterAnalysis extends CounterAnalysis {

    @Override
    public boolean canExecute(@NonNull ITmfTrace trace) {
        return trace instanceof TraceEventTrace;
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return TraceEventCounterStateProvider.createTraceEventCounterStateProvider(getTrace());
    }
}
