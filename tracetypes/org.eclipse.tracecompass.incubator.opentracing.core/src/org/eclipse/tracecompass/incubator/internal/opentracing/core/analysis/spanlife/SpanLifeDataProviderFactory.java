/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.core.analysis.spanlife;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * {@link SpanLifeDataProviderFactory} factory, uses the data provider extension
 * point.
 *
 * @author Katherine Nadeau
 */
public class SpanLifeDataProviderFactory implements IDataProviderFactory {

    private static final Predicate<? super ITmfTrace> PREDICATE = t -> TmfTraceUtils.getAnalysisModuleOfClass(t, SpanLifeAnalysis.class, SpanLifeAnalysis.ID) != null;

    @SuppressWarnings("restriction")
    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(SpanLifeDataProvider.ID)
            .setName("Span Life") //$NON-NLS-1$
            .setDescription("Show spans over time") //$NON-NLS-1$
            .setProviderType(ProviderType.TIME_GRAPH)
            .build();

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(@NonNull ITmfTrace trace) {
        SpanLifeAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, SpanLifeAnalysis.class, SpanLifeAnalysis.ID);
        if (module != null) {
            module.schedule();
            return new SpanLifeDataProvider(trace, module);
        }

        return null;
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
        Collection<ITmfTrace> traces = TmfTraceManager.getTraceSet(trace);
        return Iterables.any(traces, PREDICATE) ? Collections.singletonList(DESCRIPTOR) : Collections.emptyList();
    }

}
