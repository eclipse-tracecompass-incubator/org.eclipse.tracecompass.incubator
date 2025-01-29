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
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Factory to create instances of the {@link DpdkEthdevSpinDataProvider}.
 *
 * @author Adel Belkhiri
 */
public class DpdkEthdevSpinDataProviderFactory implements IDataProviderFactory {
    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(DpdkEthdevSpinDataProvider.ID)
            .setName("Dpdk Ethernet RX Spins") //$NON-NLS-1$
            .setDescription("XY chart showing a rough estimate of PMD threads busyness based on the number of empy and full Rx spins") //$NON-NLS-1$
            .setProviderType(ProviderType.TREE_TIME_XY)
            .build();

    @Override
    public @Nullable ITmfTreeXYDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace) {
        DpdkEthdevSpinAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, DpdkEthdevSpinAnalysisModule.class, DpdkEthdevSpinAnalysisModule.ID);
        if (module == null) {
            return null;
        }
        module.schedule();
        return DpdkEthdevSpinDataProvider.create(trace);
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(ITmfTrace trace) {
        DpdkEthdevSpinAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, DpdkEthdevSpinAnalysisModule.class, DpdkEthdevSpinAnalysisModule.ID);
        return module != null ? Collections.singletonList(DESCRIPTOR) : Collections.emptyList();
    }

}
