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

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.ITmfDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Integration point for the shared TraceX execution data provider.
 */
public class TraceXDataProviderFactory implements IDataProviderFactory {

    /** Data-provider extension identifier. */
    public static final String ID = "org.eclipse.tracecompass.incubator.tracex.core.dataprovider"; //$NON-NLS-1$

    @Override
    public @Nullable ITmfDataProvider createDataProvider(ITmfTrace trace) {
        if (!(trace instanceof TraceXTrace)) {
            return null;
        }
        TraceXAnalysis analysis = TmfTraceUtils.getAnalysisModuleOfClass(trace,
                TraceXAnalysis.class, TraceXAnalysis.ID);
        if (analysis == null) {
            return null;
        }
        analysis.schedule();
        return new TraceXDataProvider(trace, analysis);
    }
}
