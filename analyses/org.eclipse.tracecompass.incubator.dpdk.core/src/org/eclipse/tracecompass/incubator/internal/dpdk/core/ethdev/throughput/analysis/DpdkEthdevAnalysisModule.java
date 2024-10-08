/**********************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.throughput.analysis;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Collections;

import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement.PriorityLevel;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAnalysisEventRequirement;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import com.google.common.collect.ImmutableList;

/**
 * This analysis calculates the reception and transmission throughput per
 * Ethernet NIC queue.
 *
 * @author Adel Belkhiri
 */
public class DpdkEthdevAnalysisModule extends TmfStateSystemAnalysisModule {

    /** The ID of this analysis module */
    public static final String ID = "org.eclipse.tracecompass.incubator.dpdk.ethdev.throughput.analysis"; //$NON-NLS-1$

    private final TmfAbstractAnalysisRequirement REQUIREMENT = new TmfAnalysisEventRequirement(ImmutableList.of(
            DpdkEthdevEventLayout.eventEthdevConfigure()), PriorityLevel.AT_LEAST_ONE);

    @Override
    protected ITmfStateProvider createStateProvider() {
        ITmfTrace trace = checkNotNull(getTrace());

        if (trace instanceof TmfTrace) {
            return new DpdkEthdevStateProvider(trace, ID);
        }

        throw new IllegalStateException();
    }

    @Override
    public Iterable<TmfAbstractAnalysisRequirement> getAnalysisRequirements() {
        return Collections.singleton(REQUIREMENT);
    }
}
