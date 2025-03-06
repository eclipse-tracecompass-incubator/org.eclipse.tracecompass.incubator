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
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tracecompass.incubator.internal.inandout.core.Activator;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.config.AbstractTmfDataProviderConfigurator;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.model.DataProviderCapabilities;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.osgi.framework.Bundle;

/**
 * Data provider factory for InAndOut analysis. It doesn't have any output but is able
 * to create custom InAndOut analyses.
 */
@NonNullByDefault
public class InAndOutDataProviderFactory extends AbstractTmfDataProviderConfigurator implements IDataProviderFactory {

    /** Data Provider factory ID */
    public static final String ID = "org.eclipse.tracecompass.incubator.inandout.core.analysis.inAndOutdataProviderFactory"; //$NON-NLS-1$

    private static final ITmfConfigurationSourceType CONFIG_SOURCE_TYPE;
    private static final String NAME = "In And Out Analysis"; //$NON-NLS-1$
    private static final String DESCRIPTION = "Configure In And Out analysis using file description"; //$NON-NLS-1$

    private static final String CONFIGURATOR_NAME = "InAndOut Configurator"; //$NON-NLS-1$
    private static final String CONFIGURATOR_DESCRIPTION = "Configure custom InAndOut analysis"; //$NON-NLS-1$

    private static final String CUSTOM_IN_AND_OUT_ANALYSIS_NAME = "InAndOut Analysis ({0})";  //$NON-NLS-1$
    private static final String CUSTOM_IN_AND_OUT_ANALYSIS_DESCRIPTION = "Custom InAndOut analysis configured by \" {0}\""; //$NON-NLS-1$

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

    // Data provider descriptor meant to be used to create new InAndOut configurations and hence new data providers
    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(ID)
            .setName(CONFIGURATOR_NAME)
            .setDescription(CONFIGURATOR_DESCRIPTION)
            .setProviderType(ProviderType.NONE)
            .setCapabilities(new DataProviderCapabilities.Builder().setCanCreate(true).build())
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

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(ITmfTrace trace) {
        List<IDataProviderDescriptor> list = new ArrayList<>();
        list.add(DESCRIPTOR);
        for (ITmfConfiguration config : getConfigurationTable().column(trace).values()) {
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
    @SuppressWarnings("null")
    protected IDataProviderDescriptor getDescriptorFromConfig(ITmfConfiguration config) {
        return new DataProviderDescriptor.Builder()
                .setParentId(ID)
                .setId(InAndOutAnalysisModule.ID + config.getId())
                .setName(NLS.bind(CUSTOM_IN_AND_OUT_ANALYSIS_NAME, config.getName()))
                .setDescription(NLS.bind(CUSTOM_IN_AND_OUT_ANALYSIS_DESCRIPTION, config.getName()))
                .setProviderType(ProviderType.NONE)
                .setConfiguration(config)
                .setCapabilities(new DataProviderCapabilities.Builder().setCanDelete(true).build())
                .build();
    }

    @Override
    protected void applyConfiguration(ITmfTrace trace, ITmfConfiguration config, boolean writeConfig) {
        if (trace instanceof TmfExperiment) {
            for (ITmfTrace tr : TmfTraceManager.getTraceSet(trace)) {
                applyConfiguration(tr, config, writeConfig);
            }
            // Only apply for traces in experiment
            return;
        }
        // Apply configuration to any trace (no need to check trace type here)
        try {
            create(config, trace, writeConfig, new InAndOutAnalysisModule());
        } catch (TmfConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    protected void removeConfiguration(ITmfTrace trace, ITmfConfiguration config) {
        if (trace instanceof TmfExperiment) {
            for (ITmfTrace tr : TmfTraceManager.getTraceSet(trace)) {
                removeConfiguration(tr, config);
            }
            // only remove for traces in experiment
            return;
        }
        try {
            remove(config, trace, InAndOutAnalysisModule.ID);
        } catch (TmfConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Removes configuration from trace:
     *  - delete configuration file
     *  - remove analysis module from trace object
     *
     * @param config
     *          the configuration to remove
     * @param trace
     *          the
     * @throws TmfConfigurationException if an error occurs
     */
    private void remove(ITmfConfiguration config, @NonNull ITmfTrace trace, String baseAnalysisId) throws TmfConfigurationException {
        // IPath traceConfig = getConfigurationRootFolder(trace, config.getSourceTypeId());
        IPath traceConfig = getConfigurationRootFolder(trace);
        traceConfig = traceConfig.append(File.separator).append(config.getId()).addFileExtension(JSON_EXTENSION);
        File configFile = traceConfig.toFile();
        if ((!configFile.exists()) || !configFile.delete()) {
            throw new TmfConfigurationException("Configuration file can't be deleted from trace: configId=" + config.getId()); //$NON-NLS-1$
        }

        // Remove and clear persistent data
        try {
            IAnalysisModule module = trace.removeAnalysisModule(baseAnalysisId + config.getId());
            if (module != null) {
                module.dispose();
                module.clearPersistentData();
            }
        } catch (TmfTraceException e) {
            throw new TmfConfigurationException("Error removing analysis module from trace: analysis ID=" + baseAnalysisId + config.getId(), e); //$NON-NLS-1$
        }
    }

    /**
     * Create the InAndOutAnalysisModule for a given configuration and trace
     *
     * @param config
     *            the input {@link ITmfConfiguration}
     * @param trace
     *            the trace to apply it to
     * @param writeConfig
     *            write the config (do only once)
     * @return InAndOutAnalysisModule
     * @throws TmfConfigurationException
     *             if an error occurs
     */
    public void create(@NonNull ITmfConfiguration config, @NonNull ITmfTrace trace, boolean writeConfig, IAnalysisModule module) throws TmfConfigurationException {
        /*
         * Apply configuration to each trace (no need to check trace type here)
         */
        module.setConfiguration(config);
        if (writeConfig) {
            // IPath traceConfigPath = getConfigurationRootFolder(trace, config.getSourceTypeId());
            IPath traceConfigPath = getConfigurationRootFolder(trace);
            writeConfiguration(config, traceConfigPath);
        }
        try {
            if (module.setTrace(trace)) {
                IAnalysisModule oldModule = trace.addAnalysisModule(module);
                if (oldModule != null) {
                    oldModule.dispose();
                    oldModule.clearPersistentData();
                }
            } else {
                module.dispose();
                throw new TmfConfigurationException("InAndOut analysis module can't be created"); //$NON-NLS-1$
            }
        } catch (TmfAnalysisException | TmfTraceException e) {
            module.dispose();
            throw new TmfConfigurationException("Exception when setting trace", e); //$NON-NLS-1$
        }
    }

//    protected IPath getConfigurationRootFolder(ITmfTrace trace, String subFolder) {
//        String supplFolder = TmfTraceManager.getSupplementaryFileDir(trace);
//        IPath supplPath = new Path(supplFolder);
//        supplPath = supplPath.addTrailingSeparator().append(subFolder);
//        return supplPath;
//    }

    @Override
    protected IPath getConfigurationRootFolder(ITmfTrace trace) {
        String supplFolder = TmfTraceManager.getSupplementaryFileDir(trace);
        IPath supplPath = new Path(supplFolder);
        supplPath = supplPath.addTrailingSeparator().append(ID);
        return supplPath;
    }

}
