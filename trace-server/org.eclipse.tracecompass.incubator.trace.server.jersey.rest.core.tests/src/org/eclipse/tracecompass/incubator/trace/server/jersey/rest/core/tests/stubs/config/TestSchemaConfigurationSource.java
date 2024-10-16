/*******************************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs.config;

import static org.eclipse.tracecompass.common.core.NonNullUtils.nullToEmptyString;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSource;
import org.eclipse.tracecompass.tmf.core.config.ITmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.osgi.framework.Bundle;

/**
 * Test class
 */
public class TestSchemaConfigurationSource implements ITmfConfigurationSource {

    private static final @NonNull ITmfConfigurationSourceType fType;

    private static final @NonNull String TEST_ANALYSIS_TYPE_ID = "org.eclipse.tracecompass.tmf.core.config.testschemasourcetype"; //$NON-NLS-1$
    private static final @NonNull String NAME = nullToEmptyString("Test Schema Type"); //$NON-NLS-1$
    private static final @NonNull String DESCRIPTION = nullToEmptyString("Test Type with schema"); //$NON-NLS-1$

    static {
        Bundle bundle = Platform.getBundle("org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests");
        IPath defaultPath = new org.eclipse.core.runtime.Path("schema/custom-execution-analysis.json");  //$NON-NLS-1$
        URL url = FileLocator.find(bundle, defaultPath, null);
        File schemaFile = null;
        try {
            schemaFile = new File(FileLocator.toFileURL(url).toURI());
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
        fType = new TmfConfigurationSourceType.Builder()
                .setId(TEST_ANALYSIS_TYPE_ID)
                .setDescription(DESCRIPTION)
                .setName(NAME)
                .setSchemaFile(schemaFile)
                .setConfigParamDescriptors(Collections.emptyList()).build();
    }

    /**
     * Constructor
     */
    public TestSchemaConfigurationSource() {
    }

    @Override
    public @NonNull ITmfConfigurationSourceType getConfigurationSourceType() {
        return fType;
    }

    @Override
    public @NonNull ITmfConfiguration create(@NonNull Map<@NonNull String, @NonNull Object> parameters) throws TmfConfigurationException {
        throw new TmfConfigurationException("Not implemented yet"); //$NON-NLS-1$
    }

    @Override
    public @NonNull ITmfConfiguration update(@NonNull String id, @NonNull Map<@NonNull String, @NonNull Object> parameters) throws TmfConfigurationException {
        throw new TmfConfigurationException("Not implemented yet"); //$NON-NLS-1$
    }

    @Override
    public @Nullable ITmfConfiguration get(@NonNull String id) {
        return null;
    }

    @Override
    public @Nullable ITmfConfiguration remove(@NonNull String id) {
        return null;
    }

    @Override
    public boolean contains(@NonNull String id) {
        return false;
    }

    @Override
    public @NonNull List<@NonNull ITmfConfiguration> getConfigurations() {
        return Collections.emptyList();
    }

    @Override
    public void dispose() {
    }
}
