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

import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackHostUtils;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackSeries;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;

/**
 * Interface that can be implemented by components who provide call stacks as
 * part of their data.
 *
 * @author Geneviève Bastien
 */
public interface IFlameChartProvider extends IAnalysisModule, ISegmentStoreProvider {

    /**
     * Get all the callstacks provided by this analysis.
     *
     * @return A collection of call stacks
     */
    Collection<CallStackSeries> getCallStackSeries();

    /**
     * Get the ID of the host this callstack provider is for
     *
     * TODO: Deprecate me, now using the interfaces from {@link CallStackHostUtils}
     *
     * @return The ID of the host
     */
    String getHostId();
}
