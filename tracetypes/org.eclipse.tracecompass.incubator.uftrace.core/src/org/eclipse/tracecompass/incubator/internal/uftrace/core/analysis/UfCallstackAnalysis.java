/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.uftrace.core.analysis;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.uftrace.core.trace.Uftrace;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * UFTrace callstack analysis
 *
 * @author Matthew Khouzam
 *
 */
public class UfCallstackAnalysis extends InstrumentedCallStackAnalysis {

    private static final @NonNull String ID = "org.eclipse.tracecompass.incubator.uftrace.analysis.callstack"; //$NON-NLS-1$

    @Override
    public @NonNull String getId() {
        return ID;
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        ITmfTrace trace = Objects.requireNonNull(getTrace());
        if (trace instanceof Uftrace) {
            return new UfCallstackProvider(trace);
        }
        // placeholder for CTF UFTraces
        throw new IllegalStateException();
    }

}
