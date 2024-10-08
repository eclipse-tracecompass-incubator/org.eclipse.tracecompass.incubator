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

package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.spin.analysis;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfTreeXYCompositeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * Factory to create instances of the {@link DpdkEtherSpinDataProvider}.
 *
 * @author Adel Belkhiri
 */
public class DpdkEtherSpinDataProviderFactory implements IDataProviderFactory {
    private static final Predicate<? super ITmfTrace> PREDICATE = t -> TmfTraceUtils.getAnalysisModuleOfClass(t, DpdkEthdevSpinAnalysisModule.class, DpdkEthdevSpinAnalysisModule.ID) != null;
    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(DpdkEtherSpinDataProvider.ID)
            .setName("Dpdk Ethernet RX Spins") //$NON-NLS-1$
            .setDescription("XY chart showing a rough estimate of PMD threads busyness based on the number of empy and full Rx spins") //$NON-NLS-1$
            .setProviderType(ProviderType.TREE_TIME_XY)
            .build();

    @Override
    public @Nullable ITmfTreeXYDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace) {
        Collection<ITmfTrace> traces = TmfTraceManager.getTraceSet(trace);
        if (traces.size() == 1) {
            return DpdkEtherSpinDataProvider.create(trace);
        }
        return TmfTreeXYCompositeDataProvider.create(traces, DpdkEtherSpinDataProvider.PROVIDER_TITLE, DpdkEtherSpinDataProvider.ID);
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(ITmfTrace trace) {
        Collection<ITmfTrace> traces = TmfTraceManager.getTraceSet(trace);
        return Iterables.any(traces, PREDICATE) ? Collections.singletonList(DESCRIPTOR) : Collections.emptyList();
    }

}
