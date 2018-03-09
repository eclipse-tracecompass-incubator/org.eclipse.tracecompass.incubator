/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.instrumented;

import java.util.Collection;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackHostUtils;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackSeries;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Interface that can be implemented by components who provide call stacks as
 * part of their data.
 *
 * @author Geneviève Bastien
 */
public interface IFlameChartProvider extends IAnalysisModule, ISegmentStoreProvider {

    /**
     * Get the callstacks series provided by this analysis.
     *
     * @return The callstack series or null if it is not available yet
     */
    @Nullable CallStackSeries getCallStackSeries();

    /**
     * Get the ID of the host this callstack provider is for
     *
     * TODO: Deprecate me, now using the interfaces from {@link CallStackHostUtils}
     *
     * @return The ID of the host
     */
    String getHostId();

    /**
     * Return whether this analysis is complete
     *
     * @return <code>true</code> if the analysis is completed, whether failed or
     *         not, <code>false</code> if it is currently running
     */
    boolean isComplete();

    /**
     * Query the requested callstacks and return the segments for the sampled times.
     * The returned segments will be simply {@link ISegment} when there is no
     * function at a given depth, or {@link ICalledFunction} when there is an actual
     * function.
     *
     * @param collection
     *            The callstack entries to query
     * @param times
     *            The complete list of times to query, they may not all be within
     *            this series's range
     * @return A map of callstack depths to a list of segments.
     */
    default Multimap<CallStackDepth, ISegment> queryCallStacks(Collection<CallStackDepth> collection, Collection<Long> times) {
        CallStackSeries callStackSeries = getCallStackSeries();
        if (callStackSeries == null) {
            return Objects.requireNonNull(ArrayListMultimap.create());
        }
        return callStackSeries.queryCallStacks(collection, times);
    }
}
