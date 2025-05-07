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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tracecompass.incubator.internal.inandout.core.Activator;
import org.eclipse.tracecompass.tmf.core.component.DataProviderConstants;
import org.eclipse.tracecompass.tmf.core.component.TmfComponent;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.ITmfDataProviderConfigurator;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.model.DataProviderCapabilities;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
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
@NonNullByDefault
public class InAndOutDataProviderConfigurator extends TmfComponent implements ITmfDataProviderConfigurator {

    private static final ITmfConfigurationSourceType CONFIG_SOURCE_TYPE;
    private static final String NAME = "In And Out Analysis"; //$NON-NLS-1$
    private static final String DESCRIPTION = "Configure In And Out analysis using file description"; //$NON-NLS-1$

    private static final String CUSTOM_IN_AND_OUT_ANALYSIS_NAME = "InAndOut Analysis ({0})";  //$NON-NLS-1$
    private static final String CUSTOM_IN_AND_OUT_ANALYSIS_DESCRIPTION = "Custom InAndOut analysis configured by \'\'{0}\'\'"; //$NON-NLS-1$

    private Table<String, ITmfTrace, ITmfConfiguration> fTmfConfigurationTable = HashBasedTable.create();

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

    /**
     * Default constructor
     */
    public InAndOutDataProviderConfigurator() {
        super("InAndOutDataProviderFactory"); //$NON-NLS-1$
    }

    /**
     * Gets the descriptors for this trace for all configurations for this trace
     * @param trace
     *            the trace
     * @return the descriptors for this trace for all configurations for this trace
     */
    public Collection<IDataProviderDescriptor> getDescriptors(ITmfTrace trace) {
        List<IDataProviderDescriptor> list = new ArrayList<>();
        for (ITmfConfiguration config : fTmfConfigurationTable.column(trace).values()) {
            list.add(getDescriptorFromConfig(config));
        }
        return list;
    }

    @SuppressWarnings("null")
    @Override
    public @NonNull List<@NonNull ITmfConfigurationSourceType> getConfigurationSourceTypes() {
        return List.of(CONFIG_SOURCE_TYPE);
    }

    @Override
    public @NonNull IDataProviderDescriptor createDataProviderDescriptors(ITmfTrace trace, ITmfConfiguration configuration) throws TmfConfigurationException {

        if (configuration.getName().equals(TmfConfiguration.UNKNOWN)) {
            throw new TmfConfigurationException("Missing configuration name of InAndOut analysis"); //$NON-NLS-1$
        }

        if (configuration.getSourceTypeId().equals(TmfConfiguration.UNKNOWN)) {
            throw new TmfConfigurationException("Missing configuration type for InAndOut analysis"); //$NON-NLS-1$
        }

        String description = configuration.getDescription();
        if (configuration.getDescription().equals(TmfConfiguration.UNKNOWN)) {
            description = "InAndOut Analysis defined by configuration " + configuration.getName(); //$NON-NLS-1$
        }

        TmfConfiguration.Builder builder = new TmfConfiguration.Builder();
        builder.setId(configuration.getId())
               .setSourceTypeId(configuration.getSourceTypeId())
               .setName(configuration.getName())
               .setDescription(description)
               .setParameters(configuration.getParameters())
               .build();

        ITmfConfiguration config = builder.build();

        applyConfiguration(trace, config, true);
        if (fTmfConfigurationTable.contains(config.getId(), trace)) {
            throw new TmfConfigurationException("Configuration already exists with label: " + config.getName()); //$NON-NLS-1$
        }
        fTmfConfigurationTable.put(config.getId(), trace, config);
        return getDescriptorFromConfig(config);
    }

    @Override
    public void removeDataProviderDescriptor(ITmfTrace trace, IDataProviderDescriptor descriptor) throws TmfConfigurationException {

        ITmfConfiguration creationConfiguration = descriptor.getConfiguration();
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
                    // Read configurations from sub-trace
                    List<ITmfConfiguration> configs = InAndOutAnalysisModule.readConfigurations(tr);
                    readAndApplyConfiguration(trace, configs);
                 }
             } else {
                 @SuppressWarnings("null")
                 // Read configurations trace
                 List<ITmfConfiguration> configs = InAndOutAnalysisModule.readConfigurations(trace);
                 readAndApplyConfiguration(trace, configs);
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

    private void readAndApplyConfiguration(ITmfTrace trace, List<ITmfConfiguration> configs) throws TmfConfigurationException {
        for (ITmfConfiguration config : configs) {
            if (!fTmfConfigurationTable.contains(config.getId(), trace)) {
                fTmfConfigurationTable.put(config.getId(), trace, config);
                applyConfiguration(trace, config, false);
            }
        }
    }

    private void applyConfiguration(ITmfTrace trace, ITmfConfiguration config, boolean writeConfig) throws TmfConfigurationException {
        if (trace instanceof TmfExperiment) {
            for (ITmfTrace tr : TmfTraceManager.getTraceSet(trace)) {
                applyConfiguration(tr, config, writeConfig);
            }
            // Only apply for traces in experiment
            return;
        }
         // Apply configuration to any trace (no need to check trace type here)
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

    @SuppressWarnings("null")
    private static IDataProviderDescriptor getDescriptorFromConfig(ITmfConfiguration config) {
        return new DataProviderDescriptor.Builder()
                .setParentId(InAndOutDataProviderFactory.ID)
                .setId(generateID(config.getId()))
                .setName(NLS.bind(CUSTOM_IN_AND_OUT_ANALYSIS_NAME, config.getName()))
                .setDescription(NLS.bind(CUSTOM_IN_AND_OUT_ANALYSIS_DESCRIPTION, config.getName()))
                .setProviderType(ProviderType.NONE)
                .setConfiguration(config)
                .setCapabilities(new DataProviderCapabilities.Builder().setCanDelete(true).build())
                .build();
    }

    /**
     * Generate data provider ID using a config ID.
     *
     * @param configId
     *          the config id
     * @return data provider ID using a config ID.
     */
    public static String generateID(String configId) {
        return InAndOutAnalysisModule.ID + DataProviderConstants.CONFIG_SEPARATOR + configId;
    }
}
