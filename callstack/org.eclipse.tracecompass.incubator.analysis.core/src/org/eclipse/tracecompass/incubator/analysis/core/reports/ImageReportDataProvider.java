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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.analysis.core.Activator;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigParamDescriptor;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigParamDescriptor;
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.model.DataProviderCapabilities;
import org.eclipse.tracecompass.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Handler for image-specific report configurations
 *
 * @author Kaveh Shahedi
 */
public class ImageReportDataProvider implements IReportDataProvider {

    private static final String[] VALID_EXTENSIONS = { "png", "jpg", "jpeg", "svg" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                                                                                      // //$NON-NLS-5$
    private static final String PATH = "path"; //$NON-NLS-1$
    private static final String PATH_DESCRIPTION = "The path to the image file"; //$NON-NLS-1$

    private static final ITmfConfigurationSourceType CONFIGURATION_SOURCE_TYPE;
    private static final List<@NonNull ITmfConfigParamDescriptor> CONFIGURATION_DESCRIPTORS;
    private static final String CONFIGURATION_ID = "org.eclipse.tracecompass.incubator.analysis.core.reports.image.config"; //$NON-NLS-1$
    private static final String CONFIGURATION_NAME = "Image Report Configurator"; //$NON-NLS-1$
    private static final String CONFIGURATION_DESCRIPTION = "Configure custom image reports (e.g., png, jpg, etc.)"; //$NON-NLS-1$

    static {
        CONFIGURATION_DESCRIPTORS = List.of(new TmfConfigParamDescriptor.Builder()
                .setKeyName(PATH)
                .setDescription(PATH_DESCRIPTION)
                .setIsRequired(true)
                .build());

        CONFIGURATION_SOURCE_TYPE = new TmfConfigurationSourceType.Builder()
                .setId(CONFIGURATION_ID)
                .setName(CONFIGURATION_NAME)
                .setDescription(CONFIGURATION_DESCRIPTION)
                .setConfigParamDescriptors(Objects.requireNonNull(CONFIGURATION_DESCRIPTORS))
                .build();
    }

    /**
     * Create a descriptor for an image configuration
     *
     * @param configuration
     *            Base configuration
     * @return Image data provider descriptor
     * @throws TmfConfigurationException
     *             If configuration is invalid
     */
    @SuppressWarnings("null")
    @Override
    public @NonNull IDataProviderDescriptor createDescriptor(@NonNull ITmfTrace trace, @NonNull ITmfConfiguration configuration) throws TmfConfigurationException {
        validateConfiguration(configuration);

        // Create configuration with image-specific description if needed
        String description = configuration.getDescription();
        if (description.equals(TmfConfiguration.UNKNOWN)) {
            description = configuration.getName();
        }

        IStatus status = createImage(trace, configuration);
        if (!status.isOK()) {
            throw new TmfConfigurationException(status.getMessage());
        }

        return getDescriptorFromConfig(trace, new TmfConfiguration.Builder()
                .setId(configuration.getId())
                .setSourceTypeId(configuration.getSourceTypeId())
                .setName(configuration.getName())
                .setDescription(description)
                .setParameters(configuration.getParameters())
                .build());
    }

    /**
     * Create a descriptor from an existing configuration
     *
     * @param configuration
     *            The configuration
     * @return The descriptor
     */
    @Override
    public @NonNull IDataProviderDescriptor getDescriptorFromConfig(@NonNull ITmfTrace trace, @NonNull ITmfConfiguration configuration) {
        return new DataProviderDescriptor.Builder()
                .setId(configuration.getId())
                .setName(configuration.getName())
                .setDescription(configuration.getDescription())
                .setProviderType(ProviderType.NONE)
                .setConfiguration(configuration)
                .setCapabilities(new DataProviderCapabilities.Builder().setCanDelete(true).build())
                .build();
    }

    /**
     * Create a copy of the image file in the reports directory
     *
     * @param trace
     *            The trace
     * @param configuration
     *            The configuration
     * @return Status of the operation
     */
    @SuppressWarnings("null")
    private static IStatus createImage(ITmfTrace trace, ITmfConfiguration configuration) {
        IPath originalPath = new Path((String) configuration.getParameters().get(PATH));

        if (!originalPath.toFile().exists()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The given image path does not exist"); //$NON-NLS-1$
        }

        if (originalPath.toFile().isDirectory()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The given image path is a directory"); //$NON-NLS-1$
        }

        String extension = originalPath.getFileExtension();
        if (extension == null || !extension.matches(String.join("|", VALID_EXTENSIONS))) { //$NON-NLS-1$
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Invalid image file extension. Supported extensions are: " //$NON-NLS-1$
                    + String.join(", ", VALID_EXTENSIONS)); //$NON-NLS-1$
        }

        IPath reportsPath = ReportsDataProviderFactory.getConfigurationBasePath(trace, ReportsDataProviderFactory.getConfigurationParent(trace, configuration))
                .addTrailingSeparator().append(configuration.getId());

        File fromFile = originalPath.toFile();
        File toFile = reportsPath.addTrailingSeparator().append(configuration.getName()).addFileExtension(extension).toFile();

        try {
            File parentDir = toFile.getParentFile();
            if (!parentDir.exists()) {
                boolean success = parentDir.mkdirs();
                if (!success) {
                    throw new IllegalStateException("Directory " + parentDir.getAbsolutePath() + " failed to create."); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }

            if (!toFile.exists()) {
                boolean success = toFile.createNewFile();
                if (!success) {
                    throw new IllegalStateException("File " + toFile.getAbsolutePath() + " failed to create."); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        } catch (IOException e) {
            String error = "Failed to create destination file"; //$NON-NLS-1$
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, error, e);
        }

        try (FileInputStream fis = new FileInputStream(fromFile);
                FileOutputStream fos = new FileOutputStream(toFile);
                FileChannel source = fis.getChannel();
                FileChannel destination = fos.getChannel()) {
            destination.transferFrom(source, 0, source.size());
        } catch (IOException e) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to copy image file", e); //$NON-NLS-1$
        }

        // Set the configuration path to the new path
        configuration.getParameters().put(PATH, toFile.getAbsolutePath());

        return Status.OK_STATUS;
    }

    @Override
    public ReportProviderType getReportType() {
        return ReportProviderType.IMAGE;
    }

    @Override
    public ITmfConfigurationSourceType getConfigurationSourceType() {
        return CONFIGURATION_SOURCE_TYPE;
    }

    /**
     * Validate the image configuration
     * <ul>
     * <li>Check if the image path is set</li>
     * <li>Check if the image path is a valid string</li>
     * </ul>
     *
     * @param configuration
     *            The configuration
     * @throws TmfConfigurationException
     *             If the configuration is invalid
     */
    @SuppressWarnings("null")
    @Override
    public void validateConfiguration(@NonNull ITmfConfiguration configuration) throws TmfConfigurationException {
        Map<String, Object> parameters = configuration.getParameters();

        // Validate image-specific parameters
        if (!parameters.containsKey(PATH)) {
            throw new TmfConfigurationException("Image path is required"); //$NON-NLS-1$
        }

        Object path = parameters.get(PATH);
        if (!(path instanceof String) || ((String) path).isEmpty()) {
            throw new TmfConfigurationException("Invalid image path"); //$NON-NLS-1$
        }
    }

    /**
     * Remove the image file
     *
     * @param trace
     *            The trace
     * @param configuration
     *            The configuration
     * @return The removed descriptor
     * @throws TmfConfigurationException
     *             If configuration is invalid or cannot be removed
     */
    @Override
    public @NonNull IDataProviderDescriptor removeDescriptor(@NonNull ITmfTrace trace, @NonNull ITmfConfiguration configuration) throws TmfConfigurationException {
        File imageFile = new Path((String) configuration.getParameters().get(PATH)).toFile();

        if (imageFile.exists() && !imageFile.delete()) {
            throw new TmfConfigurationException("Failed to delete image file"); //$NON-NLS-1$
        }

        return getDescriptorFromConfig(trace, configuration);
    }
}