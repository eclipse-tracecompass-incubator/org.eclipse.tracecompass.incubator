/*******************************************************************************
 * Copyright (c) 2025 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.analysis.core.reports;

import static org.eclipse.tracecompass.common.core.NonNullUtils.nullToEmptyString;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigParamDescriptor;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.ITmfDataProviderConfigurator;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigParamDescriptor;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.model.DataProviderCapabilities;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.incubator.internal.analysis.core.Activator;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Data provider factory for Reports. This factory manages different types of
 * report providers through a registry system. It creates data provider
 * descriptors for report configurations, validates configuration parameters,
 * and stores configurations for each trace (or experiment).
 *
 * @author Kaveh Shahedi
 */
public class ReportsDataProviderFactory implements IDataProviderFactory, ITmfDataProviderConfigurator, IReportDataProvider {

    /** Reports Provider factory ID */
    public static final String ID = "org.eclipse.tracecompass.incubator.analysis.core.reports.reportsDataProviderFactory"; //$NON-NLS-1$

    private static final String NAME = "Reports"; //$NON-NLS-1$
    private static final String DESCRIPTION = "Configure various types of reports (text, image, etc.) or report folders for grouping purposes"; //$NON-NLS-1$

    private static final ITmfConfigurationSourceType CONFIGURATION_SOURCE_TYPE;
    private static final String CONFIGURATION_ID = "org.eclipse.tracecompass.incubator.analysis.core.reports.folder.config"; //$NON-NLS-1$
    private static final String CONFIGURATION_NAME = "Reports Folder Configurator"; //$NON-NLS-1$
    private static final String CONFIGURATION_DESCRIPTION = "Configure custom report folders (i.e., for grouping the reports)"; //$NON-NLS-1$

    /** Constants for configuration parameters */
    private static final String PARENT_ID = "parentId"; //$NON-NLS-1$
    private static final String PARENT_ID_DESC = "The parent report ID"; //$NON-NLS-1$

    /** Table to store configurations per trace */
    private static final Table<String, UUID, ITmfConfiguration> fTmfConfigurationTable = HashBasedTable.create();
    private static final Table<UUID, String, String> fTmfConfigurationHierarchy = HashBasedTable.create();

    static {
        // Register built-in providers
        ReportsDataProviderRegistry.registerProvider(new ReportsDataProviderFactory());
        ReportsDataProviderRegistry.registerProvider(new ImageReportDataProvider());

        CONFIGURATION_SOURCE_TYPE = new TmfConfigurationSourceType.Builder()
                .setId(CONFIGURATION_ID)
                .setName(CONFIGURATION_NAME)
                .setDescription(CONFIGURATION_DESCRIPTION)
                .build();
    }

    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(ID)
            .setName(NAME)
            .setDescription(DESCRIPTION)
            .setProviderType(ProviderType.NONE)
            .setCapabilities(new DataProviderCapabilities.Builder().setCanCreate(true).build())
            .build();

    /**
     * Default constructor
     */
    public ReportsDataProviderFactory() {
        TmfSignalManager.register(this);
    }

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(@NonNull ITmfTrace trace) {
        // Reports factory doesn't create providers directly
        return null;
    }

    /**
     * Get the descriptors for a trace
     *
     * @param trace
     *            the trace
     * @return the descriptors
     */
    @SuppressWarnings("null")
    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(ITmfTrace trace) {
        List<IDataProviderDescriptor> list = new ArrayList<>();
        list.add(DESCRIPTOR);

        fTmfConfigurationTable.column(trace.getUUID()).values().stream()
                .map(config -> {
                    try {
                        ReportProviderType type = getReportType(config);
                        IReportDataProvider provider = ReportsDataProviderRegistry.getProvider(type);
                        return provider != null ? Map.entry(config, provider) : null;
                    } catch (TmfConfigurationException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(entry -> entry.getValue().getDescriptorFromConfig(trace, entry.getKey()))
                .forEach(list::add);

        list.sort(Comparator.comparing(IDataProviderDescriptor::getName));
        return list.stream()
                .map(descriptor -> descriptor.getConfiguration() == null ? descriptor : createDescriptor(descriptor))
                .collect(Collectors.toList());
    }

    /**
     * Create a descriptor from an existing configuration
     *
     * @param trace
     *            the trace
     * @param configuration
     *            the configuration
     * @return the descriptor
     */
    @SuppressWarnings("null")
    private static IDataProviderDescriptor createDescriptor(IDataProviderDescriptor descriptor) {
        ITmfConfiguration config = descriptor.getConfiguration();
        if (config == null) {
            return descriptor;
        }

        Map<String, Object> params = new HashMap<>(config.getParameters());
        String parentId = (String) params.get(PARENT_ID);
        if (parentId == null) {
            parentId = ID;
        }

        return new DataProviderDescriptor.Builder()
                .setId(descriptor.getId())
                .setParentId(parentId)
                .setName(descriptor.getName())
                .setDescription(descriptor.getDescription())
                .setProviderType(descriptor.getType())
                .setCapabilities(descriptor.getCapabilities())
                .setConfiguration(new TmfConfiguration.Builder()
                        .setId(config.getId())
                        .setSourceTypeId(config.getSourceTypeId())
                        .setName(config.getName())
                        .setDescription(config.getDescription())
                        .setParameters(params)
                        .build())
                .build();
    }

    /**
     * Create a new data provider descriptor
     *
     * @param trace
     *            the trace
     * @param configuration
     *            the configuration
     * @return the data provider descriptor
     * @throws TmfConfigurationException
     *             if an error occurs while creating the descriptor
     */
    @SuppressWarnings("null")
    @Override
    public @NonNull IDataProviderDescriptor createDataProviderDescriptors(@NonNull ITmfTrace trace, @NonNull ITmfConfiguration configuration) throws TmfConfigurationException {
        ITmfConfiguration config = validateConfiguration(trace, configuration);

        ReportProviderType type = getReportType(config);
        if (type == ReportProviderType.NONE) {
            // For NONE type, use this factory as the provider
            return createDescriptor(trace, config);
        }

        // For other types, use the appropriate provider
        IReportDataProvider provider = ReportsDataProviderRegistry.getProvider(type);
        if (provider == null) {
            throw new TmfConfigurationException("Unsupported report type: " + type); //$NON-NLS-1$
        }

        IDataProviderDescriptor descriptor = provider.createDescriptor(trace, config);

        fTmfConfigurationTable.put(config.getId(), trace.getUUID(), config);

        String parentId = (String) config.getParameters().get(PARENT_ID);
        if (parentId == null) {
            parentId = ID;
        }
        fTmfConfigurationHierarchy.put(trace.getUUID(), config.getId(), parentId);

        writeConfiguration(trace, config);

        return descriptor;
    }

    /**
     * Remove a data provider descriptor and all its children recursively
     *
     * @param trace
     *            the trace
     * @param descriptor
     *            the descriptor to remove
     * @throws TmfConfigurationException
     *             if an error occurs while removing
     */
    @Override
    public void removeDataProviderDescriptor(@NonNull ITmfTrace trace, @NonNull IDataProviderDescriptor descriptor) throws TmfConfigurationException {
        ITmfConfiguration config = descriptor.getConfiguration();
        if (config == null) {
            throw new TmfConfigurationException("Data provider was not created by a configuration"); //$NON-NLS-1$
        }

        ReportProviderType type = getReportType(config);
        if (type == ReportProviderType.NONE) {
            // For NONE type, use this factory's removeDescriptor
            removeDescriptor(trace, config);
        } else {
            // For other types, use their specific providers
            IReportDataProvider provider = ReportsDataProviderRegistry.getProvider(type);
            if (provider != null) {
                provider.removeDescriptor(trace, config);
            }

            removeConfiguration(trace, config);

            // Clean up tables
            fTmfConfigurationTable.remove(config.getId(), trace.getUUID());
            fTmfConfigurationHierarchy.remove(trace.getUUID(), config.getId());
        }
    }

    /**
     * Get the available configuration source types
     *
     * @return the configuration source types
     */
    @SuppressWarnings("null")
    @Override
    public @NonNull List<@NonNull ITmfConfigurationSourceType> getConfigurationSourceTypes() {
        List<ITmfConfigurationSourceType> result = new ArrayList<>();
        for (IReportDataProvider provider : ReportsDataProviderRegistry.getProviders()) {
            ITmfConfigurationSourceType sourceType = provider.getConfigurationSourceType();
            List<ITmfConfigParamDescriptor> allDescriptors = new ArrayList<>(sourceType.getConfigParamDescriptors());
            allDescriptors.addAll(List.of(
                    new TmfConfigParamDescriptor.Builder()
                            .setKeyName(PARENT_ID)
                            .setDescription(PARENT_ID_DESC)
                            .setIsRequired(false)
                            .build()));

            result.add(new TmfConfigurationSourceType.Builder()
                    .setName(sourceType.getName())
                    .setDescription(sourceType.getDescription())
                    .setId(sourceType.getId())
                    .setConfigParamDescriptors(allDescriptors)
                    .build());
        }

        return result;
    }

    @Override
    public ReportProviderType getReportType() {
        return ReportProviderType.NONE;
    }

    @Override
    public ITmfConfigurationSourceType getConfigurationSourceType() {
        return CONFIGURATION_SOURCE_TYPE;
    }

    @Override
    public @NonNull IDataProviderDescriptor createDescriptor(@NonNull ITmfTrace trace, @NonNull ITmfConfiguration configuration) throws TmfConfigurationException {
        ITmfConfiguration config = validateConfiguration(trace, configuration);

        // For NONE type (factory/group), create descriptor directly
        IDataProviderDescriptor descriptor = new DataProviderDescriptor.Builder()
                .setId(config.getId())
                .setName(config.getName())
                .setDescription(config.getDescription())
                .setProviderType(ProviderType.NONE)
                .setConfiguration(config)
                .setCapabilities(new DataProviderCapabilities.Builder()
                        .setCanCreate(false)
                        .setCanDelete(true)
                        .build())
                .build();

        fTmfConfigurationTable.put(config.getId(), trace.getUUID(), config);

        String parentId = (String) config.getParameters().get(PARENT_ID);
        if (parentId == null) {
            parentId = ID;
        }

        fTmfConfigurationHierarchy.put(trace.getUUID(), config.getId(), parentId);

        // Write configuration to disk
        writeConfiguration(trace, config);

        return descriptor;
    }

    @Override
    public @NonNull IDataProviderDescriptor getDescriptorFromConfig(@NonNull ITmfTrace trace, @NonNull ITmfConfiguration configuration) {
        return new DataProviderDescriptor.Builder()
                .setId(configuration.getId())
                .setName(configuration.getName())
                .setDescription(configuration.getDescription())
                .setProviderType(ProviderType.NONE)
                .setConfiguration(configuration)
                .setCapabilities(new DataProviderCapabilities.Builder()
                        .setCanCreate(false)
                        .setCanDelete(true)
                        .build())
                .build();
    }

    @SuppressWarnings("null")
    @Override
    public @NonNull IDataProviderDescriptor removeDescriptor(@NonNull ITmfTrace trace, @NonNull ITmfConfiguration configuration) throws TmfConfigurationException {
        // Create descriptor for return value
        IDataProviderDescriptor descriptor = getDescriptorFromConfig(trace, configuration);

        // First, collect all children that need to be removed
        List<Map.Entry<String, ITmfConfiguration>> childrenToRemove = new ArrayList<>();
        for (Map.Entry<String, ITmfConfiguration> entry : fTmfConfigurationTable.column(trace.getUUID()).entrySet()) {
            ITmfConfiguration childConfig = entry.getValue();
            String parentId = (String) childConfig.getParameters().get(PARENT_ID);

            if (configuration.getId().equals(parentId)) {
                childrenToRemove.add(entry);
            }
        }

        // Now remove all children
        for (Map.Entry<String, ITmfConfiguration> entry : childrenToRemove) {
            ITmfConfiguration childConfig = entry.getValue();
            ReportProviderType childType = getReportType(childConfig);
            if (childType == ReportProviderType.NONE) {
                removeDescriptor(trace, childConfig);
            } else {
                IReportDataProvider provider = ReportsDataProviderRegistry.getProvider(childType);
                if (provider != null) {
                    provider.removeDescriptor(trace, childConfig);
                }
                // Clean up tables for this child
                fTmfConfigurationTable.remove(childConfig.getId(), trace.getUUID());
                fTmfConfigurationHierarchy.remove(trace.getUUID(), childConfig.getId());
            }
        }

        // Remove configuration files
        removeConfiguration(trace, configuration);

        fTmfConfigurationTable.remove(configuration.getId(), trace.getUUID());
        fTmfConfigurationHierarchy.remove(trace.getUUID(), configuration.getId());

        return descriptor;
    }

    @Override
    public void validateConfiguration(@NonNull ITmfConfiguration configuration) throws TmfConfigurationException {
        return;
    }

    /**
     * Validate a configuration. These are the checks performed:
     * <ul>
     * <li>Configuration name and source type must not be unknown</li>
     * <li>Parent ID must be valid</li>
     * <li>Configuration must not already exist</li>
     * <li>Parent must be a NONE report type</li>
     * <li>No circular dependencies in the report hierarchy</li>
     * </ul>
     *
     * @param trace
     *            the trace
     * @param configuration
     *            the configuration
     * @return the validated configuration
     * @throws TmfConfigurationException
     *             if the configuration is invalid
     */
    @SuppressWarnings("null")
    private ITmfConfiguration validateConfiguration(@NonNull ITmfTrace trace, @NonNull ITmfConfiguration configuration) throws TmfConfigurationException {
        if (configuration.getName().equals(TmfConfiguration.UNKNOWN) || configuration.getSourceTypeId().equals(TmfConfiguration.UNKNOWN)) {
            throw new TmfConfigurationException("Missing configuration name or type");//$NON-NLS-1$
        }

        String parentId = (String) configuration.getParameters().get(PARENT_ID);
        if (parentId == null) {
            parentId = ID;
        }

        if (!parentId.equals(ID) && !fTmfConfigurationTable.contains(parentId, trace.getUUID())) {
            throw new TmfConfigurationException("Parent configuration does not exist");//$NON-NLS-1$
        }

        String configFullId = getFullUUID(configuration.getId(), parentId);
        if (fTmfConfigurationTable.contains(configFullId, trace.getUUID())) {
            throw new TmfConfigurationException("Configuration already exists");//$NON-NLS-1$
        }

        ReportProviderType parentType = parentId.equals(ID) ? ReportProviderType.NONE : getReportType(fTmfConfigurationTable.get(parentId, trace.getUUID()));
        if (parentType != ReportProviderType.NONE) {
            throw new TmfConfigurationException("Parent must be a NONE report type");//$NON-NLS-1$
        }

        validateNoCycles(trace, parentId, new HashSet<>());

        return new TmfConfiguration.Builder()
                .setId(configFullId)
                .setSourceTypeId(configuration.getSourceTypeId())
                .setName(configuration.getName())
                .setDescription(configuration.getDescription())
                .setParameters(configuration.getParameters())
                .build();
    }

    /**
     * Validate that there are no cycles in the report hierarchy
     *
     * @param trace
     *            the trace
     * @param currentId
     *            the current configuration ID
     * @param visited
     *            the set of visited IDs
     * @throws TmfConfigurationException
     *             if a cycle is detected
     */
    private void validateNoCycles(ITmfTrace trace, String currentId, HashSet<String> visited) throws TmfConfigurationException {
        if (!visited.add(currentId)) {
            throw new TmfConfigurationException("Circular dependency detected in report hierarchy");//$NON-NLS-1$
        }

        ITmfConfiguration config = fTmfConfigurationTable.get(currentId, trace.getUUID());
        if (config != null) {
            String parentId = (String) config.getParameters().get(PARENT_ID);
            if (parentId != null) {
                validateNoCycles(trace, parentId, visited);
            }
        }
    }

    /**
     * @param signal
     *            trace opened signal
     */
    @SuppressWarnings("null")
    @TmfSignalHandler
    public void traceOpened(TmfTraceOpenedSignal signal) {
        loadConfigurations(signal.getTrace());
    }

    /**
     * @param signal
     *            trace closed signal
     */
    @TmfSignalHandler
    public void traceClosed(TmfTraceClosedSignal signal) {
        fTmfConfigurationTable.column(signal.getTrace().getUUID()).clear();
        fTmfConfigurationHierarchy.row(signal.getTrace().getUUID()).clear();
    }

    /**
     * Load configurations for a trace from disk
     *
     * @param trace
     *            trace
     */
    private void loadConfigurations(@NonNull ITmfTrace trace) {
        String supplFolder = TmfTraceManager.getSupplementaryFileDir(trace);
        IPath supplPath = new Path(supplFolder);
        supplPath = supplPath.addTrailingSeparator().append(ID);

        File folder = supplPath.toFile();
        if (!folder.exists()) {
            return;
        }

        loadConfigurationsRecursive(trace, folder);
    }

    /**
     * Recursively load configurations from a directory and its sub-directories
     *
     * @param trace
     *            the trace
     * @param directory
     *            the current directory to process
     */
    private void loadConfigurationsRecursive(@NonNull ITmfTrace trace, File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                loadConfigurationsRecursive(trace, file);
            } else if (file.getName().endsWith(TmfConfiguration.JSON_EXTENSION)) {
                try {
                    ITmfConfiguration config = TmfConfiguration.fromJsonFile(file);
                    try {
                        validateConfiguration(trace, config);
                        fTmfConfigurationTable.put(config.getId(), trace.getUUID(), config);
                    } catch (TmfConfigurationException e) {
                        Activator.getInstance().logError("Error validating configuration from file: " + file.getName(), e); //$NON-NLS-1$
                    }
                } catch (TmfConfigurationException e) {
                    Activator.getInstance().logError("Error reading configuration from file: " + file.getName(), e); //$NON-NLS-1$
                }
            }
        }
    }

    /**
     * Write a configuration to disk for a trace
     *
     * @param trace
     *            the trace to write configuration for
     * @param configuration
     *            the configuration to write
     * @throws TmfConfigurationException
     *             if an error occurs while writing
     */
    private static void writeConfiguration(@NonNull ITmfTrace trace, @NonNull ITmfConfiguration configuration) throws TmfConfigurationException {
        IPath basePath = getConfigurationBasePath(trace, configuration);

        File folder = basePath.toFile();
        if (!folder.exists() && !folder.mkdirs()) {
            throw new TmfConfigurationException("Could not create configuration directory"); //$NON-NLS-1$
        }

        TmfConfiguration.writeConfiguration(configuration, basePath);
    }

    /**
     * Remove a configuration from disk for a trace (and its children)
     *
     * @param trace
     *            the trace to remove configuration for
     * @param configuration
     *            the configuration to remove
     * @throws TmfConfigurationException
     *             if an error occurs while removing
     */
    private static void removeConfiguration(@NonNull ITmfTrace trace, @NonNull ITmfConfiguration configuration) throws TmfConfigurationException {
        IPath configPath = getConfigurationBasePath(trace, configuration);
        File file = configPath.toFile();

        if (file.exists()) {
            try {
                java.nio.file.Path path = file.toPath();
                Files.walkFileTree(path, new SimpleFileVisitor<java.nio.file.Path>() {
                    @Override
                    public FileVisitResult visitFile(java.nio.file.Path f, BasicFileAttributes attrs) throws IOException {
                        Files.delete(f);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(java.nio.file.Path f, IOException exc) throws IOException {
                        if (f.toFile().setWritable(true)) {
                            Files.delete(f);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(java.nio.file.Path dir, IOException exc) throws IOException {
                        if (exc == null) {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                        throw exc;
                    }
                });
            } catch (IOException e) {
                throw new TmfConfigurationException("Could not delete configuration: " + e.getMessage()); //$NON-NLS-1$
            }
        }
    }

    /**
     * Get the base path for a configuration
     *
     * @param trace
     *            the trace
     * @param configuration
     *            the configuration
     * @return the base path
     */
    public static IPath getConfigurationBasePath(@NonNull ITmfTrace trace, @NonNull ITmfConfiguration configuration) {
        IPath supplPath = new Path(TmfTraceManager.getSupplementaryFileDir(trace));
        supplPath = supplPath.addTrailingSeparator().append(ID);

        // Collect configurations in correct order (from parent to child)
        List<ITmfConfiguration> configurationChain = new ArrayList<>();
        ITmfConfiguration currentConfig = configuration;

        // Add configurations from child to parent
        while (currentConfig != null) {
            configurationChain.add(0, currentConfig);
            String parentId = (String) currentConfig.getParameters().get(PARENT_ID);
            if (parentId == null || parentId.equals(ID)) {
                break;
            }
            currentConfig = fTmfConfigurationTable.get(parentId, trace.getUUID());
        }

        // Build path from root to leaf
        for (ITmfConfiguration config : configurationChain) {
            supplPath = supplPath.addTrailingSeparator().append(config.getId());
        }

        return supplPath;
    }

    /**
     * Generates a UUID based on the combination of configuration and parent
     * IDs.
     *
     * @param configId
     *            The configuration ID (can be null)
     * @param parentId
     *            The parent ID (can be null)
     * @return A deterministic UUID generated from the combined IDs
     */
    @SuppressWarnings("null")
    private static String getFullUUID(String configId, String parentId) {
        // Convert null values to empty strings and combine with delimiter
        String combinedId = String.format("%s:%s", //$NON-NLS-1$
                nullToEmptyString(parentId),
                nullToEmptyString(configId)).replaceAll("^:|:$", ""); //$NON-NLS-1$ //$NON-NLS-2$

        return UUID.nameUUIDFromBytes(combinedId.getBytes()).toString();
    }

    /**
     * Get the parent configuration of a configuration
     *
     * @param trace
     *            the trace
     * @param configuration
     *            the configuration
     * @return the parent configuration
     */
    public static ITmfConfiguration getConfigurationParent(@NonNull ITmfTrace trace, @NonNull ITmfConfiguration configuration) {
        String parentId = (String) configuration.getParameters().get(PARENT_ID);
        return parentId == null ? null : fTmfConfigurationTable.get(parentId, trace.getUUID());
    }

    /**
     * Get the report type for a configuration
     *
     * @param configuration
     *            the configuration
     * @return the report type
     * @throws TmfConfigurationException
     *             if the report type is invalid
     */
    private static ReportProviderType getReportType(ITmfConfiguration configuration) throws TmfConfigurationException {
        String sourceTypeId = configuration.getSourceTypeId();
        return ReportsDataProviderRegistry.getProviders().stream()
                .filter(provider -> provider.getConfigurationSourceType().getId().equals(sourceTypeId))
                .findFirst()
                .map(IReportDataProvider::getReportType)
                .orElseThrow(() -> new TmfConfigurationException("Invalid report configuration source type")); //$NON-NLS-1$
    }

}