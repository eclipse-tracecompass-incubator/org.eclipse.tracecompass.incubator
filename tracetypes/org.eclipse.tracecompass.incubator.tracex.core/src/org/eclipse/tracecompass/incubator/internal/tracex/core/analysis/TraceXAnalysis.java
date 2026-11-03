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

import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;

/**
 * TraceX single-core execution state-system analysis.
 */
public class TraceXAnalysis extends TmfStateSystemAnalysisModule {

    /** Analysis extension identifier. */
    public static final String ID = "org.eclipse.tracecompass.incubator.tracex.core.analysis"; //$NON-NLS-1$

    @Override
    protected ITmfStateProvider createStateProvider() {
        return new TraceXStateProvider((TraceXTrace) Objects.requireNonNull(getTrace()));
    }

    @Override
    protected String getSsFileName() {
        return ID + ".v" + TraceXStateProvider.VERSION + ".ht"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
