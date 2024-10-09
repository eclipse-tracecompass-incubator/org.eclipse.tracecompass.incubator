/*******************************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.inandout.core.analysis;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.inandout.core.Activator;
import org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented.FlameChartDataProvider;
import org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.SegmentStoreScatterDataProvider;
import org.eclipse.tracecompass.internal.analysis.timing.core.segmentstore.SegmentStoreStatisticsDataProvider;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.ITmfDataProviderSource;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.TmfJsonConfiguration;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.osgi.framework.Bundle;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Data provider factory for InAndOut analysis. It doesn't have any output but is able
 * to create custom InAndOut analyses.
 */
@SuppressWarnings("restriction")
@NonNullByDefault
public class InAndOutDataProviderFactory implements IDataProviderFactory, ITmfDataProviderSource {

    /** Data Provider factory ID */
    public static final String ID = "org.eclipse.tracecompass.incubator.inandout.core.analysis.inAndOutdataProviderFactory"; //$NON-NLS-1$

    private static final ITmfConfigurationSourceType CONFIG_SOURCE_TYPE;
    private static final String NAME = "In And Out Analysis"; //$NON-NLS-1$
    private static final String DESCRIPTION = "Configure In And Out analysis using file description"; //$NON-NLS-1$

    private static final Table<String, ITmfTrace, ITmfConfiguration> fTmfConfigurationTable = HashBasedTable.create();

    static {
        Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
        IPath defaultPath = new Path(SegmentSpecifierConfiguration.IN_AND_OUT_CONFIG_SOURCE_SCHEMA);
        URL url = FileLocator.find(bundle, defaultPath, null);
        File schemaFile = null;
        try {
            schemaFile = new File(FileLocator.toFileURL(url).toURI());
        } catch (URISyntaxException | IOException e) {
            Activator.getInstance().logError("Failed to read schema file: " + SegmentSpecifierConfiguration.IN_AND_OUT_CONFIG_SOURCE_SCHEMA, e); //$NON-NLS-1$
        }
        CONFIG_SOURCE_TYPE = new TmfConfigurationSourceType.Builder()
                .setId(SegmentSpecifierConfiguration.IN_AND_OUT_CONFIG_SOURCE_TYPE_ID)
                .setDescription(DESCRIPTION)
                .setName(NAME)
                .setSchemaFile(schemaFile)
                .build();
    }

    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(ID)
            .setName("InAndOut Configurator") //$NON-NLS-1$
            .setDescription("Configure custom InAndOut analysis") //$NON-NLS-1$
            .setProviderType(ProviderType.NONE)
            .build();

    /**
     * Default constructor
     */
    public InAndOutDataProviderFactory() {
        TmfSignalManager.register(this);
    }

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace) {
        return null;
    }

    @SuppressWarnings("null")
    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(ITmfTrace trace) {
        return List.of(DESCRIPTOR);
    }

    @SuppressWarnings("null")
    @Override
    public @NonNull List<@NonNull ITmfConfigurationSourceType> getConfigurationSourceTypes() {
        return List.of(CONFIG_SOURCE_TYPE);
    }

    @SuppressWarnings( "null")
    @Override
    public @NonNull List<@NonNull IDataProviderDescriptor> createDataProviderDescriptors(String typeId, ITmfTrace trace, String jsonParameters) throws TmfConfigurationException {
        /*
         * Note only one config type is available for this.
         */
        ITmfConfiguration defaultConfig = new TmfConfiguration.Builder()
                .setId("Dummy ID") // not used //$NON-NLS-1$
                .setSourceTypeId(typeId)
                .setName("InAndOut config") //$NON-NLS-1$
                .setDescription("Custom In And Out").build(); //$NON-NLS-1$

        ITmfConfiguration config = TmfJsonConfiguration.fromJsonString(jsonParameters, defaultConfig);
        if (fTmfConfigurationTable.contains(config.getId(), trace)) {
            throw new TmfConfigurationException("Configuration already existis with label: " + config.getName()); //$NON-NLS-1$
        }
        fTmfConfigurationTable.put(config.getId(), trace, config);
        applyConfiguration(trace, config, true);

        // TODO add a parameter to the parameters to specify if it applies for the experiment or its traces
        return DataProviderManager.getInstance().getAvailableProviders(trace,
                config.getId(),
                FlameChartDataProvider.ID,
                SegmentStoreStatisticsDataProvider.ID,
                SegmentStoreScatterDataProvider.ID,
                SegmentStoreScatterDataProvider.ID);
        }

    @Override
    public void removeDataProviderDescriptor(ITmfTrace trace, IDataProviderDescriptor descriptor) throws TmfConfigurationException {

        ITmfConfiguration creationConfiguration = descriptor.getCreationConfiguration();
        if (creationConfiguration == null) {
            throw new TmfConfigurationException("Data provider was not created by a configuration"); //$NON-NLS-1$
        }

        String configId = creationConfiguration.getId();
        ITmfConfiguration config = fTmfConfigurationTable.get(configId, trace);
        if (config == null) {
            return;
        }
        config = fTmfConfigurationTable.remove(configId, trace);
        removeConfiguration(trace, config);
    }

    /**
     * Signal handler for opened trace signal. Will populate trace
     * configurations
     *
     * @param signal
     *            the signal to handle
     */
    @TmfSignalHandler
    public void traceOpened(TmfTraceOpenedSignal signal) {
        ITmfTrace trace = signal.getTrace();
        if (trace == null) {
            return;
        }
        try {
             if (trace instanceof TmfExperiment) {
                 for (ITmfTrace tr : TmfTraceManager.getTraceSet(trace)) {
                     @SuppressWarnings("null")
                    List<ITmfConfiguration> configs = InAndOutAnalysisModule.readConfigurations(tr);
                     for (ITmfConfiguration config : configs) {
                         if (!fTmfConfigurationTable.contains(config.getId(), trace)) {
                             fTmfConfigurationTable.put(config.getId(), trace, config);
                             applyConfiguration(trace, config, false);
                         }
                     }
                 }
             }
        } catch (TmfConfigurationException e) {
            Activator.getInstance().logError("Error applying configurations for trace " + trace.getName(), e); //$NON-NLS-1$
        }
    }

    /**
     * Handles trace closed signal
     *
     * @param signal
     *            the close signal to handle
     */
    @TmfSignalHandler
    public void traceClosed(TmfTraceClosedSignal signal) {
        ITmfTrace trace = signal.getTrace();
        fTmfConfigurationTable.column(trace).clear();
    }

    private void applyConfiguration(ITmfTrace trace, ITmfConfiguration config, boolean writeConfig) throws TmfConfigurationException {
        if (trace instanceof TmfExperiment) {
            for (ITmfTrace tr : TmfTraceManager.getTraceSet(trace)) {
                applyConfiguration(tr, config, writeConfig);
            }
            // only apply for traces in experiment
            return;
        }
        /*
         * Apply configuration to any trace (no need to check trace type here)
         */
        InAndOutAnalysisModule.create(config, trace, writeConfig);
    }

    private void removeConfiguration(ITmfTrace trace, ITmfConfiguration config) throws TmfConfigurationException {
        if (trace instanceof TmfExperiment) {
            for (ITmfTrace tr : TmfTraceManager.getTraceSet(trace)) {
                removeConfiguration(tr, config);
            }
            // only remove for traces in experiment
            return;
        }
        InAndOutAnalysisModule.remove(config, trace);
    }

    @Override
    public <T> @Nullable T getAdapter(@Nullable Class<T> adapter) {
        if (adapter == ITmfDataProviderSource.class) {
            return adapter.cast(this);
        }
        return null;
    }
}
