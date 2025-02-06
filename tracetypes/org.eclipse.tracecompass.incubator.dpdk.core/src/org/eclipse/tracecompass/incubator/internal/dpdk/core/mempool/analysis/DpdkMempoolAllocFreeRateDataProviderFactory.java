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
 * Factory to create instances of the {@link DpdkMempoolAllocFreeRateDataProvider}.
 *
 * @author Adel Belkhiri
 */
public class DpdkMempoolAllocFreeRateDataProviderFactory implements IDataProviderFactory {
    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(DpdkMempoolAllocFreeRateDataProvider.ID)
            .setName("Dpdk Mempool Alloc/Free Rate") //$NON-NLS-1$
            .setDescription("XY chart showing the rate at which mempool objects were allocated and freed") //$NON-NLS-1$
            .setProviderType(ProviderType.TREE_TIME_XY)
            .build();

    @Override
    public @Nullable ITmfTreeXYDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace) {
        DpdkMempoolAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, DpdkMempoolAnalysisModule.class, DpdkMempoolAnalysisModule.ID);
        if (module == null) {
            return null;
        }
        module.schedule();
        return new DpdkMempoolAllocFreeRateDataProvider(trace, module);
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(ITmfTrace trace) {
        DpdkMempoolAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, DpdkMempoolAnalysisModule.class, DpdkMempoolAnalysisModule.ID);
        return module != null ? Collections.singletonList(DESCRIPTOR) : Collections.emptyList();
    }

}
