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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;

/**
 * Placeholder for the TraceX execution analysis.
 */
public class TraceXAnalysis extends TmfAbstractAnalysisModule {

    /** Analysis extension identifier. */
    public static final String ID = "org.eclipse.tracecompass.incubator.tracex.core.analysis"; //$NON-NLS-1$

    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) {
        monitor.done();
        return !monitor.isCanceled();
    }

    @Override
    protected void canceling() {
        // There is no analysis work to cancel while the implementation is pending.
    }
}
