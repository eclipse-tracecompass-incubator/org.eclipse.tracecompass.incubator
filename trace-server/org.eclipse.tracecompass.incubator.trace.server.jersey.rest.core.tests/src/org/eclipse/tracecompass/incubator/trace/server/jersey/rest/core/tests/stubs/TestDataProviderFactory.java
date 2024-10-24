/*******************************************************************************
 * Copyright (c) 2018,
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.config.TestSchemaConfigurationSource;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.ITmfDataProviderConfigurator;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Test data provider factory
 */
public class TestDataProviderFactory implements IDataProviderFactory, ITmfDataProviderConfigurator{

    /** The ID of the factory */
    public static final String ID = "org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.TestDataProviderFactory";

    /** Name to test already exists */
    public static final String ALREADY_EXISTS_NAME = "ALREADY_EXISTS";

    private static final String CONFIGURATOR_NAME = "Test Configurator"; //$NON-NLS-1$
    private static final String CONFIGURATOR_DESCRIPTION = "Configure custom test analysis"; //$NON-NLS-1$

    private static final String DERIVED_CONFIGURATOR_NAME = "Derived Test DP"; //$NON-NLS-1$
    private static final String DERIVED_CONFIGURATOR_DESCRIPTION = "Derived custom test analysis"; //$NON-NLS-1$

    private IDataProviderDescriptor fDerivedDescriptor;

    /**
     * Default constructor
     */
    public TestDataProviderFactory() {
    }

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(@NonNull ITmfTrace trace) {
        return null;
    }

    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(ID)
            .setName(CONFIGURATOR_NAME)
            .setDescription(CONFIGURATOR_DESCRIPTION)
            .setProviderType(ProviderType.DATA_TREE)
            .build();

    @SuppressWarnings("null")
    @Override
    public @NonNull List<@NonNull ITmfConfigurationSourceType> getConfigurationSourceTypes() {
        return List.of(TestSchemaConfigurationSource.TYPE);
    }

    @Override
    public @NonNull IDataProviderDescriptor createDataProviderDescriptors(@NonNull ITmfTrace trace, @NonNull ITmfConfiguration configuration) throws TmfConfigurationException {
        if (configuration.getName().equals(TmfConfiguration.UNKNOWN)) {
            throw new TmfConfigurationException("Missing configuration name"); //$NON-NLS-1$
        }

        if (configuration.getSourceTypeId().equals(TmfConfiguration.UNKNOWN)) {
            throw new TmfConfigurationException("Missing configuration type"); //$NON-NLS-1$
        }
        fDerivedDescriptor = new DataProviderDescriptor.Builder()
                .setParentId(ID)
                .setId(ID + configuration.getId())
                .setConfiguration(configuration)
                .setName(DERIVED_CONFIGURATOR_NAME + ", " + configuration.getName())
                .setDescription(DERIVED_CONFIGURATOR_DESCRIPTION + ", " + configuration.getDescription())
                .setProviderType(ProviderType.TIME_GRAPH)
                .build();
        return fDerivedDescriptor;
    }

    @Override
    public void removeDataProviderDescriptor(@NonNull ITmfTrace trace, @NonNull IDataProviderDescriptor descriptor) throws TmfConfigurationException {
        ITmfConfiguration creationConfiguration = descriptor.getConfiguration();
        if (creationConfiguration == null) {
            throw new TmfConfigurationException("Data provider was not created by a configuration"); //$NON-NLS-1$
        }
        fDerivedDescriptor = null;
    }

    @Override
    public @NonNull Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
        List<IDataProviderDescriptor> descriptor = new ArrayList<>();
        descriptor.add(DESCRIPTOR);
        if (fDerivedDescriptor != null) {
            descriptor.add(fDerivedDescriptor);
        }
        return descriptor;
    }

}
