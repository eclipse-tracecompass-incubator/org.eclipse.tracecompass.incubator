/*******************************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.rocm.core.analysis;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.provisional.analysis.profiling.core.instrumented.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;

/**
 * This analysis is a callstack analysis which summarizes the execution of a
 * ROCm GPU program.
 *
 * It separates the execution into two categories: the GPU queues and the system
 * API calls. Furthermore, some dependencies were modeled using arrows to show
 * the user how these abstract layers relate to each other.
 *
 * @author Arnaud Fiorini
 */
@SuppressWarnings("restriction")
public class RocmCallStackAnalysis extends InstrumentedCallStackAnalysis {

    private static final String EDGES = "EDGES"; //$NON-NLS-1$

    @Override
    protected @NonNull Collection<Integer> getEdgeQuarks() {
        ITmfStateSystem ss = getStateSystem();
        if (ss == null) {
            return Collections.emptyList();
        }
        int edgeQuark = ss.optQuarkAbsolute(EDGES);
        if (edgeQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return Collections.emptyList();
        }
        return ss.getSubAttributes(edgeQuark, false);
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new RocmCallStackStateProvider(Objects.requireNonNull(getTrace()), new RocmEventLayout());
    }
}
