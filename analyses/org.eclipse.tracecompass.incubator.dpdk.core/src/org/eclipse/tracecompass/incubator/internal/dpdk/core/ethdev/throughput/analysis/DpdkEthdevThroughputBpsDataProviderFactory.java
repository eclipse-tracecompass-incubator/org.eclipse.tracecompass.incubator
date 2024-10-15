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
 * Factory to create instances of the
 * {@link DpdkEthdevThroughputBpsDataProvider}.
 *
 * @author Adel Belkhiri
 */
public class DpdkEthdevThroughputBpsDataProviderFactory implements IDataProviderFactory {
    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(DpdkEthdevThroughputBpsDataProvider.ID)
            .setName("Dpdk Ethernet Throughput BPS") //$NON-NLS-1$
            .setDescription("XY chart illustrating the throughput of DPDK Ethernet NIC queues in bits per seconds") //$NON-NLS-1$
            .setProviderType(ProviderType.TREE_TIME_XY)
            .build();

    @Override
    public @Nullable ITmfTreeXYDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace) {
        DpdkEthdevThroughputAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, DpdkEthdevThroughputAnalysisModule.class, DpdkEthdevThroughputAnalysisModule.ID);
        if (module == null) {
            return null;
        }
        module.schedule();
        return DpdkEthdevThroughputBpsDataProvider.create(trace);
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(ITmfTrace trace) {
        DpdkEthdevThroughputAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, DpdkEthdevThroughputAnalysisModule.class, DpdkEthdevThroughputAnalysisModule.ID);
        return module != null ? Collections.singletonList(DESCRIPTOR) : Collections.emptyList();
    }

}
