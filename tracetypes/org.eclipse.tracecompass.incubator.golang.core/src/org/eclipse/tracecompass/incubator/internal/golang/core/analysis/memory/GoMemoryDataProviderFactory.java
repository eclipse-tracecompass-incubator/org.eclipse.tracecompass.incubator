/**********************************************************************
 * Copyright (c) 2017, 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.golang.core.analysis.memory;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
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
 * Factory to create instances of the {@link GoMemoryUsageDataProvider}.
 * Uses the DataProviderFactory extension point.
 *
 * @since 2.4
 * @author Loic Prieur-Drevon
 */
public class GoMemoryDataProviderFactory implements IDataProviderFactory {

    private static final @NonNull String TITLE = Objects.requireNonNull("Heap Usage");
    private static final Predicate<? super ITmfTrace> PREDICATE = t -> TmfTraceUtils.getAnalysisModuleOfClass(t, GoMemoryAnalysisModule.class, GoMemoryAnalysisModule.ID) != null;
    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(GoMemoryUsageDataProvider.ID)
            .setName(TITLE)
            .setDescription(Objects.requireNonNull("Heap Usage"))
            .setProviderType(ProviderType.TREE_TIME_XY)
            .build();

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(@NonNull ITmfTrace trace) {
        Collection<ITmfTrace> traces = TmfTraceManager.getTraceSet(trace);
        if (traces.size() == 1) {
            return GoMemoryUsageDataProvider.create(trace);

        }
        return TmfTreeXYCompositeDataProvider.create(traces, TITLE, GoMemoryUsageDataProvider.ID);
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
        Collection<ITmfTrace> traces = TmfTraceManager.getTraceSet(trace);
        return traces.stream().anyMatch(PREDICATE) ? Collections.singletonList(DESCRIPTOR) : Collections.emptyList();
    }

}
