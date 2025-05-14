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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Interface for report data providers. Implementations handle specific types of
 * reports (image, HTML, text, etc.). They are responsible for creating
 * descriptors for report configurations, validating configuration parameters,
 * and providing a type identifier.
 *
 * @author Kaveh Shahedi
 */
public interface IReportDataProvider {

    /**
     * Type identifier for report providers
     */
    enum ReportProviderType {
        /** None-type report provider (i.e., for grouping purposes) */
        NONE,
        /** Data provider for image reports */
        IMAGE
    }

    /**
     * Get the type identifier for this report provider
     *
     * @return The report type identifier
     */
    ReportProviderType getReportType();

    /**
     * Get the configuration source type for this report provider
     *
     * @return The configuration source type
     */
    ITmfConfigurationSourceType getConfigurationSourceType();

    /**
     * Create a descriptor for a specific report configuration
     *
     * @param trace
     *            Trace to create the descriptor for
     * @param configuration
     *            Base configuration
     * @return Data provider descriptor
     * @throws TmfConfigurationException
     *             If configuration is invalid
     */
    @NonNull
    IDataProviderDescriptor createDescriptor(@NonNull ITmfTrace trace, @NonNull ITmfConfiguration configuration) throws TmfConfigurationException;

    /**
     * Create a descriptor from an existing configuration
     *
     * @param trace
     *            The trace to get the descriptor for
     * @param configuration
     *            The configuration
     * @return The descriptor
     */
    @NonNull
    IDataProviderDescriptor getDescriptorFromConfig(@NonNull ITmfTrace trace, @NonNull ITmfConfiguration configuration);

    /**
     * Remove a descriptor from the data provider
     *
     * @param trace
     *            The trace to remove the descriptor for
     * @param configuration
     *            The configuration to remove
     * @return The removed descriptor
     * @throws TmfConfigurationException
     *             If configuration is invalid or cannot be removed
     */
    @NonNull
    IDataProviderDescriptor removeDescriptor(@NonNull ITmfTrace trace, @NonNull ITmfConfiguration configuration) throws TmfConfigurationException;

    /**
     * Validate provider-specific configuration parameters
     *
     * @param configuration
     *            The configuration to validate
     * @throws TmfConfigurationException
     *             If configuration is invalid
     */
    void validateConfiguration(@NonNull ITmfConfiguration configuration) throws TmfConfigurationException;
}