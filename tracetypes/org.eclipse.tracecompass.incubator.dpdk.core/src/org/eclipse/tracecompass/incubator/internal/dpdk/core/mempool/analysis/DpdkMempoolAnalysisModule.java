/**********************************************************************
 * Copyright (c) 2025 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.dpdk.core.mempool.analysis;

import java.util.Collections;

import org.eclipse.tracecompass.incubator.dpdk.core.trace.DpdkTrace;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement.PriorityLevel;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAnalysisEventRequirement;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList;

/**
 * A DPDK mempool is a collection of pre-allocated, fixed-size memory objects.
 * By analyzing a subset of events from `lib.mempool.*`, this module analyzes
 * the way DPDK worker threads allocate and deallocate these objects.
 *
 * Key metrics that can be obtained through this analysis include:
 * <ul>
 * <li><strong>Object Allocation/Deallocation Rates</strong>: Measures the rate
 * at which mempool objects were allocated and freed.</li>
 * <li><strong>Mempool Usage</strong>: Monitors mempool utilization over
 * time.</li>
 * </ul>
 *
 * @author Adel Belkhiri
 */
public class DpdkMempoolAnalysisModule extends TmfStateSystemAnalysisModule {

    /** The ID of this analysis module */
    public static final String ID = "org.eclipse.tracecompass.incubator.dpdk.mempool.analysis"; //$NON-NLS-1$

    private final TmfAbstractAnalysisRequirement REQUIREMENT = new TmfAnalysisEventRequirement(ImmutableList.of(
            DpdkMempoolEventLayout.eventMempoolCreate(), DpdkMempoolEventLayout.eventMempoolCreateEmpty()), PriorityLevel.AT_LEAST_ONE);

    @Override
    protected ITmfStateProvider createStateProvider() {
        ITmfTrace trace = getTrace();
        if (trace instanceof DpdkTrace) {
            return new DpdkMempoolStateProvider(trace, ID);
        }
        throw new IllegalStateException("Trace " + trace + "(" + (trace == null ? "null" : trace.getClass().getCanonicalName()) + ")" + " is not of the type DpdkTrace."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    }

    @Override
    public Iterable<TmfAbstractAnalysisRequirement> getAnalysisRequirements() {
        return Collections.singleton(REQUIREMENT);
    }
}
