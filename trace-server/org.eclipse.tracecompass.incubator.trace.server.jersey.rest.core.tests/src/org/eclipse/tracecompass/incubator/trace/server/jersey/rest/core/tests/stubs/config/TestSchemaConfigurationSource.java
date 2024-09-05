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
import org.eclipse.tracecompass.tmf.core.config.TmfConfiguration;
import org.eclipse.tracecompass.tmf.core.config.TmfConfigurationSourceType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;
import org.osgi.framework.Bundle;

import com.google.common.collect.ImmutableList;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Test class
 */
public class TestSchemaConfigurationSource implements ITmfConfigurationSource {

    private static final @NonNull ITmfConfigurationSourceType fType;

    private static final @NonNull String TEST_ANALYSIS_TYPE_ID = "org.eclipse.tracecompass.tmf.core.config.testschemasourcetype"; //$NON-NLS-1$
    private static final @NonNull String NAME = nullToEmptyString("Test Schema Type"); //$NON-NLS-1$
    private static final @NonNull String DESCRIPTION = nullToEmptyString("Test Type with schema"); //$NON-NLS-1$

    private static final @NonNull String TEST_CONFIG_ID = "My Config Id"; //$NON-NLS-1$
    private static final @NonNull String TEST_CONFIG_NAME = "My Config Name"; //$NON-NLS-1$
    private static final @NonNull String TEST_CONFIG_DESCRIPTION = "My Config Description"; //$NON-NLS-1$

    private ITmfConfiguration fConfiguration;

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
        Object json = parameters.get(TmfConfiguration.JSON_STRING_KEY);
        if (json instanceof String) {
            TmfConfiguration.Builder builder = new TmfConfiguration.Builder();
            ITmfConfiguration configuration = builder.setId(TEST_CONFIG_ID)
                    .setSourceTypeId(TEST_ANALYSIS_TYPE_ID)
                    .setDescription(TEST_CONFIG_DESCRIPTION) // should be taken from json string
                    .setName(TEST_CONFIG_NAME) // should be taken from json string
                    .setParameters(parameters)
                    .build();
            fConfiguration = configuration;
            return configuration;
        }
        throw new TmfConfigurationException("No or invalid json provided: " + json); //$NON-NLS-1$
    }

    @Override
    public @NonNull ITmfConfiguration update(@NonNull String id, @NonNull Map<@NonNull String, @NonNull Object> parameters) throws TmfConfigurationException {
        throw new TmfConfigurationException("Not implemented yet"); //$NON-NLS-1$
    }

    @Override
    public @Nullable ITmfConfiguration get(@NonNull String id) {
        return fConfiguration;
    }

    @Override
    public @Nullable ITmfConfiguration remove(@NonNull String id) {
        ITmfConfiguration configuration = fConfiguration;
        fConfiguration = null;
        return configuration;
    }

    @Override
    public boolean contains(@NonNull String id) {
        ITmfConfiguration configuration = fConfiguration;
        return configuration != null;
    }

    @Override
    public @NonNull List<@NonNull ITmfConfiguration> getConfigurations() {
        ITmfConfiguration configuration = fConfiguration;
        if (configuration == null) {
            return Collections.emptyList();
        }
        return ImmutableList.of(configuration);
    }

    @Override
    public void dispose() {
    }

    /**
     * Class to deserialize json string
     */
    public class Parameters {
        @Expose
        @SerializedName(value = "label")
        private String fLabel;
        @Expose
        @SerializedName(value = "thread")
        private String fThread;
        @Expose
        @SerializedName(value = "phone")
        private String fPhone;
        @Expose
        @SerializedName(value = "cpus")
        private List<Integer> fCpus;

        /**
         * Default constructor for GSON
         */
        public Parameters() {
            fLabel = ""; //$NON-NLS-1$
            fThread = ""; //$NON-NLS-1$
            fPhone = ""; //$NON-NLS-1$
            fCpus = Collections.emptyList(); //$NON-NLS-1$
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            return builder.append("[label=").append(fLabel)
                   .append(", thread=").append(fThread)
                   .append(", phone=").append("phone")
                   .append(", fCpus=").append(fCpus)
                   .append("]")
                   .toString();
        }
    }

}
