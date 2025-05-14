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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.reports.IReportDataProvider.ReportProviderType;

/**
 * Registry for report data providers
 *
 * @author Kaveh Shahedi
 */
public class ReportsDataProviderRegistry {
    private static final Map<ReportProviderType, IReportDataProvider> PROVIDERS = new HashMap<>();

    /**
     * Register a report data provider
     *
     * @param provider
     *            The provider to register
     */
    public static void registerProvider(IReportDataProvider provider) {
        PROVIDERS.put(provider.getReportType(), provider);
    }

    /**
     * Get the list of registered providers
     *
     * @return The list of registered providers
     */
    public static List<IReportDataProvider> getProviders() {
        return List.copyOf(PROVIDERS.values());
    }

    /**
     * Get a provider for a specific report type
     *
     * @param type
     *            The report type
     * @return The provider, or null if none exists for the type
     */
    public static @Nullable IReportDataProvider getProvider(ReportProviderType type) {
        return PROVIDERS.get(type);
    }

    /**
     * Clear all registered providers
     */
    public static void cleanup() {
        PROVIDERS.clear();
    }
}
