/*******************************************************************************
 * Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.tracex.core.analysis;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;

/**
 * Kernel-shaped execution analysis for TraceX.
 *
 * <p>
 * This subclasses the stock {@link KernelAnalysisModule} and reuses its
 * identifier so the globally registered Control Flow View
 * ({@code ThreadStatusDataProvider}) and Resources View
 * ({@code ResourcesStatusDataProvider}) data providers—which look the module up
 * by {@code KernelAnalysisModule.class} and {@link KernelAnalysisModule#ID}—find
 * it and render TraceX execution with no bespoke view code.
 *
 * <p>
 * Only the state provider is replaced: TraceX (ThreadX) events are not
 * LTTng-shaped, so a dedicated {@link TraceXKernelStateProvider} fills the same
 * kernel attribute tree the two views read.
 */
public class TraceXKernelAnalysis extends KernelAnalysisModule {

    /**
     * Identifier of the TraceX kernel state system. The analysis itself is
     * registered under {@link KernelAnalysisModule#ID} (see
     * {@link #getId()}) so the stock providers resolve it; this separate id is
     * used only for the state-system backend file.
     */
    public static final @NonNull String TRACEX_KERNEL_ID = "org.eclipse.tracecompass.incubator.tracex.core.kernel"; //$NON-NLS-1$

    @Override
    public @NonNull String getId() {
        // Reuse the stock kernel analysis id so the Control Flow and Resources
        // data provider factories find this module.
        return KernelAnalysisModule.ID;
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new TraceXKernelStateProvider(
                (TraceXTrace) Objects.requireNonNull(getTrace()));
    }
}
