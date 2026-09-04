/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.sqlite.core.analysis;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.counters.core.CounterAnalysis;
import org.eclipse.tracecompass.incubator.internal.sqlite.core.trace.SQLiteTrace;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Counter analysis for SQLite traces. Every numeric column (integer, long or
 * floating point) of every event table is exposed as a counter series, so the
 * values can be plotted over time in the Counters view.
 *
 * @author Matthew Khouzam
 */
public class SqliteCounterAnalysis extends CounterAnalysis {

    /** The analysis id. */
    public static final @NonNull String ANALYSIS_ID = "org.eclipse.tracecompass.incubator.sqlite.core.analysis.counter"; //$NON-NLS-1$

    @Override
    public boolean canExecute(@NonNull ITmfTrace trace) {
        return trace instanceof SQLiteTrace;
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new SqliteCounterStateProvider(java.util.Objects.requireNonNull(getTrace()));
    }
}
