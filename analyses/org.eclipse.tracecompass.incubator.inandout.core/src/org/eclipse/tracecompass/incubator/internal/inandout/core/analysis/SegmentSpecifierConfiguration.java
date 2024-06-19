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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.inandout.core.Activator;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfConfigurationException;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Model of Segment specifiers
 */
@NonNullByDefault
public class SegmentSpecifierConfiguration {

    /** Configuration source*/
    public static final String IN_AND_OUT_CONFIG_SOURCE_TYPE_ID = "org.eclipse.tracecompass.incubator.internal.inandout.core.config"; //$NON-NLS-1$

    /** The configuration schema */
    public static final String IN_AND_OUT_CONFIG_SOURCE_SCHEMA= "schema/in-and-out-analysis.json"; //$NON-NLS-1$


    @Expose
    @SerializedName(value = "specifiers")
    private @Nullable List<SegmentSpecifier> fSpecifiers;

    SegmentSpecifierConfiguration() {
        fSpecifiers = Collections.emptyList();
    }

    /**
     * Get the list of {@link SegmentSpecifier}
     *
     * @return List of {@link SegmentSpecifier}
     */
    public @Nullable List<SegmentSpecifier> getSpecifiers() {
        return fSpecifiers;
    }

    /**
     * Converts custom JSON parameters to InAndOut specifiers
     *
     * @param parameters
     *            the custom parameters map
     * @return a SegmenSpecifierConiguration instance
     * @throws TmfConfigurationException
     *             if an error occurred
     */
    public static SegmentSpecifierConfiguration fromJsonMap(Map<String, Object> parameters) throws TmfConfigurationException {
        try {
            String jsonParameters = new Gson().toJson(parameters, Map.class);
            @SuppressWarnings("null")
            SegmentSpecifierConfiguration config = new Gson().fromJson(jsonParameters, SegmentSpecifierConfiguration.class);
            return config;
        } catch (JsonSyntaxException e) {
            Activator.getInstance().logError(e.getMessage(), e);
            throw new TmfConfigurationException("Can't parse json. ", e); //$NON-NLS-1$
        }
    }
}
