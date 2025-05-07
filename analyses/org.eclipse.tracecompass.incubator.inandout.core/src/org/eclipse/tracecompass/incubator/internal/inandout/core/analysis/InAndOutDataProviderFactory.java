/*******************************************************************************
 * Copyright (c) 2024, 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.inandout.core.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.DataProviderCapabilities;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Data provider factory for InAndOut analysis. It doesn't have any output but is able
 * to create custom InAndOut analyses.
 */
@NonNullByDefault
public class InAndOutDataProviderFactory implements IDataProviderFactory {

    /** Data Provider factory ID */
    public static final String ID = "org.eclipse.tracecompass.incubator.inandout.core.analysis.inAndOutdataProviderFactory"; //$NON-NLS-1$

    private static final String CONFIGURATOR_NAME = "InAndOut Configurator"; //$NON-NLS-1$
    private static final String CONFIGURATOR_DESCRIPTION = "Configure custom InAndOut analysis"; //$NON-NLS-1$

    // Data provider descriptor meant to be used to create new InAndOut configurations and hence new data providers
    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(ID)
            .setName(CONFIGURATOR_NAME)
            .setDescription(CONFIGURATOR_DESCRIPTION)
            .setProviderType(ProviderType.NONE)
            .setCapabilities(new DataProviderCapabilities.Builder().setCanCreate(true).build())
            .build();

    private InAndOutDataProviderConfigurator fConfigurator = new InAndOutDataProviderConfigurator();

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace) {
        return null;
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(ITmfTrace trace) {
        List<IDataProviderDescriptor> list = new ArrayList<>();
        list.add(DESCRIPTOR);
        list.addAll(fConfigurator.getDescriptors(trace));
        return list;
    }

    @Override
    public <T> @Nullable T getAdapter(@Nullable Class<T> adapter) {
        if (adapter != null && adapter.isAssignableFrom(InAndOutDataProviderConfigurator.class)) {
            return adapter.cast(fConfigurator);
        }
        return IDataProviderFactory.super.getAdapter(adapter);
    }

    @Override
    public void dispose() {
        fConfigurator.dispose();
    }
}
