/**********************************************************************
 * Copyright (c) 2024 École Polytechnique de Montréal
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

import org.eclipse.tracecompass.incubator.dpdk.core.trace.DpdkTrace;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis.DpdkEthdevEventLayout;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement.PriorityLevel;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAnalysisEventRequirement;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList;

/**
 * An analysis to calculate the traffic reception and transmission throughput
 * per Ethernet port queue.
 *
 * Note: To enable the computing of RX and TX throughput in bps, the DPDK
 * profiling library must be pre-loaded. This custom library emits events that
 * include the number of bytes representing the size of each burst. It achieves
 * this by hooking into the RX and TX burst operations through the registration
 * of appropriate callbacks, specifically via calls to rte_eth_add_rx_callback()
 * and rte_eth_add_tx_callback().
 *
 * @author Adel Belkhiri
 */
public class DpdkEthdevThroughputAnalysisModule extends TmfStateSystemAnalysisModule {

    /** The ID of this analysis module */
    public static final String ID = "org.eclipse.tracecompass.incubator.dpdk.ethdev.throughput.analysis"; //$NON-NLS-1$

    private final TmfAbstractAnalysisRequirement REQUIREMENT = new TmfAnalysisEventRequirement(ImmutableList.of(
            DpdkEthdevEventLayout.eventEthdevConfigure()), PriorityLevel.AT_LEAST_ONE);

    @Override
    protected ITmfStateProvider createStateProvider() {
        ITmfTrace trace = checkNotNull(getTrace());

        if (trace instanceof DpdkTrace) {
            return new DpdkEthdevThroughputStateProvider(trace, ID);
        }

        throw new IllegalStateException();
    }

    @Override
    public Iterable<TmfAbstractAnalysisRequirement> getAnalysisRequirements() {
        return Collections.singleton(REQUIREMENT);
    }
}
