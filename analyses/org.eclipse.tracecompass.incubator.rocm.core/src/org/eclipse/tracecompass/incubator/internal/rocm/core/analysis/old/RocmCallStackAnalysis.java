/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.rocm.core.analysis.old;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackSeries;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackSeries.IThreadIdResolver;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackStateProvider;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;

import com.google.common.collect.ImmutableList;

/**
 * This analysis is a callstack analysis which summarizes the execution of a
 * ROCm GPU program.
 *
 * It separates the execution into 3 categories: the GPU, the memory transfers,
 * and the system API calls. Furthermore, some dependencies were modeled using
 * arrows to show the user how these abstract layers relate to each other.
 *
 * @author Arnaud Fiorini
 */
public class RocmCallStackAnalysis extends InstrumentedCallStackAnalysis {
    /**
     * Call stack analysis ID
     */
    public static final @NonNull String ID = "org.eclipse.tracecompass.incubator.rocm.core.analysis.interval"; //$NON-NLS-1$

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new RocmCallStackStateProvider(Objects.requireNonNull(getTrace()));
    }

    @Override
    protected @Nullable IThreadIdResolver getCallStackTidResolver() {
        return new CallStackSeries.AttributeValueThreadResolver(2);
    }

    @Override
    protected @NonNull Collection<@NonNull Integer> getEdgeQuarks() {
        ITmfStateSystem ss = getStateSystem();
        if (ss == null) {
            return Collections.emptyList();
        }
        int edgeQuark = ss.optQuarkAbsolute(RocmStrings.EDGES);
        if (edgeQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return Collections.emptyList();
        }
        return ss.getSubAttributes(edgeQuark, false);
    }

    /**
     * Get the patterns for the GPUs, threads and categories
     *
     * @return The patterns for the different levels in the state system
     */
    @Override
    protected List<String[]> getPatterns() {
        return ImmutableList.of(
                new String[] { CallStackStateProvider.PROCESSES, "*" }, //$NON-NLS-1$
                new String[] { "*" }, //$NON-NLS-1$
                new String[] { "*" }); //$NON-NLS-1$
    }
}
