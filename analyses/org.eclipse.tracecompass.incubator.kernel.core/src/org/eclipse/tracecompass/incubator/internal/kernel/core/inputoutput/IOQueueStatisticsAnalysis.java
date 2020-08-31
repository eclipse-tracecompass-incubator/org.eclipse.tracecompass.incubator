/*******************************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.core.inputoutput;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.statistics.AbstractSegmentStatisticsAnalysis;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.segment.interfaces.INamedSegment;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Statistics analysis for the Input/Output queue segment stores.
 *
 * @author Geneviève Bastien
 */
public class IOQueueStatisticsAnalysis extends AbstractSegmentStatisticsAnalysis {

    /** The analysis module ID */
    private final ISegmentStoreProvider fSegmentStoreProvider;
    private final IAnalysisModule fAnalysis;

    /**
     * Constructor
     *
     * @param module The analysis module that is the source of the segment store
     * @param ss The segment store provider
     */
    public IOQueueStatisticsAnalysis(IAnalysisModule module, ISegmentStoreProvider ss) {
        fSegmentStoreProvider = ss;
        fAnalysis = module;
    }

    @Override
    protected boolean executeAnalysis(@NonNull IProgressMonitor monitor) throws TmfAnalysisException {
        fAnalysis.schedule();
        fAnalysis.waitForCompletion();
        return super.executeAnalysis(monitor);
    }

    @Override
    protected @Nullable ISegmentStoreProvider getSegmentProviderAnalysis(@NonNull ITmfTrace trace) {
        return fSegmentStoreProvider;
    }

    @Override
    protected @Nullable String getSegmentType(@NonNull ISegment segment) {
        if (segment instanceof INamedSegment) {
            INamedSegment named = (INamedSegment) segment;
            return String.valueOf(named.getName());
        }
        return null;
    }


}
