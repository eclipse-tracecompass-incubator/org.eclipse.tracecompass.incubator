/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.husdon.maven.core.analysis;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.statistics.AbstractSegmentStatisticsAnalysis;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Analysis module to calculate statistics of a latency analysis
 *
 * @author Marc-Andre Laperle
 */
public class MavenGoalsLatencyStatisticsAnalysisModule extends AbstractSegmentStatisticsAnalysis {

    @Override
    protected @Nullable String getSegmentType(@NonNull ISegment segment) {
        if (segment instanceof MavenGoal) {
            MavenGoal syscall = (MavenGoal) segment;
            return syscall.getName();
        }
        return null;
    }

    @Override
    protected @Nullable ISegmentStoreProvider getSegmentProviderAnalysis(@NonNull ITmfTrace trace) {
        return TmfTraceUtils.getAnalysisModuleOfClass(trace, MavenGoalsLatencyAnalysis.class, MavenGoalsLatencyAnalysis.ID);
    }

}
