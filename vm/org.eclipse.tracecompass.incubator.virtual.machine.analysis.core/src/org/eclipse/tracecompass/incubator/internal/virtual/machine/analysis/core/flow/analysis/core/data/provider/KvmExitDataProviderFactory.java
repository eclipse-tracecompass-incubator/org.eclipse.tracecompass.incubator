/*******************************************************************************
 * Copyright (c) 2026 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.core.data.provider;


import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.KvmExitAnalysisModule;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfTreeXYCompositeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;


/**
 * Factory for the KVM exit data provider
 *
 * @author Francois Belias
 */
public class KvmExitDataProviderFactory implements IDataProviderFactory {

    private static final String TITLE = "KVM Exit Density"; //$NON-NLS-1$
    private static final String DESCRIPTION = "Shows the density of KVM exit events per CPU over time"; //$NON-NLS-1$

    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(KvmExitRateDataProvider.ID)
            .setName(TITLE)
            .setDescription(DESCRIPTION)
            .setProviderType(ProviderType.TREE_TIME_XY)
            .build();

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createDataProvider(ITmfTrace trace) {
        Collection<@NonNull ITmfTrace> traces = TmfTraceManager.getTraceSet(trace);
        if (traces.size() == 1) {
            KvmExitAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, KvmExitAnalysisModule.class, KvmExitAnalysisModule.ID);
            if (module == null) {
                return null;
            }
            module.schedule();
            return new KvmExitRateDataProvider(trace, module);
        }
        return TmfTreeXYCompositeDataProvider.create(traces, "KVM Exit time graph", KvmExitRateDataProvider.ID); //$NON-NLS-1$
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(ITmfTrace trace) {
        Collection<@NonNull ITmfTrace> traces = TmfTraceManager.getTraceSet(trace);
        if (traces.size() > 1) {
            return Collections.singletonList(DESCRIPTOR);
        }

        KvmExitAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, KvmExitAnalysisModule.class, KvmExitAnalysisModule.ID);
        return module != null ? Collections.singletonList(DESCRIPTOR) : Collections.emptyList();
    }
}