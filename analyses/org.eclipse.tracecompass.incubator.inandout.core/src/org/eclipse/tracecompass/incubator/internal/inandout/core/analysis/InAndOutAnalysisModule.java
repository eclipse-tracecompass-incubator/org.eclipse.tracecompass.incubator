/*******************************************************************************
 * Copyright (c) 2022 Ericsson
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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.inandout.core.Activator;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * In and out analysis
 *
 * @author Matthew Khouzam
 */
public class InAndOutAnalysisModule extends InstrumentedCallStackAnalysis {

    /**
     * ID
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.inandout.analysis"; //$NON-NLS-1$

    /**
     * JSON configuration file extension.
     */
    public static final String JSON = ".config.json"; //$NON-NLS-1$

    /**
     * Reference (default) specifier configured.
     */
    public static final SegmentSpecifier REFERENCE = new SegmentSpecifier("latency", "(\\S*)_entry", "(\\S*)_exit", "(\\S*)_entry", "(\\S*)_exit", "CPU"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

    @Override
    public @NonNull String getId() {
        return ID;
    }

    @Override
    public boolean canExecute(@NonNull ITmfTrace trace) {
        File config = getConfig(trace);
        return config.exists() && super.canExecute(trace);
    }

    private static File getConfig(@NonNull ITmfTrace trace) {
        String folder = TmfTraceManager.getSupplementaryFileDir(trace);
        File config = new File(folder + File.separator + ID + JSON);
        if (!config.exists()) {
            List<@NonNull SegmentSpecifier> specifiers = new ArrayList<>();
            specifiers.add(new SegmentSpecifier(REFERENCE));
            write(config, specifiers);
        }
        return config;
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        ITmfTrace trace = Objects.requireNonNull(getTrace(), "Trace should not be null at this point"); //$NON-NLS-1$
        File configFile = getConfig(trace);
        List<@NonNull SegmentSpecifier> list = read(configFile);
        return new InAndOutAnalysisStateProvider(trace, list);
    }

    /**
     * Read the config file and return segment specifiers
     *
     * @param file
     *            the file
     * @return a list of specifiers, which could be empty even though unexpected
     */
    public static List<@NonNull SegmentSpecifier> read(File file) {
        Type listType = new TypeToken<ArrayList<@NonNull SegmentSpecifier>>() {
        }.getType();
        List<@NonNull SegmentSpecifier> specifiers = Collections.emptyList();
        try (Reader reader = new FileReader(file)) {
            List<@NonNull SegmentSpecifier> list = new Gson().fromJson(reader, listType);
            if (list != null) {
                specifiers = list;
            }
        } catch (IOException e) {
            Activator.getInstance().logError(e.getMessage(), e);
        }
        return specifiers;
    }

    /**
     * Write a list of specifiers to disk
     *
     * @param file
     *            the file to write to
     * @param specifiers
     *            the specifiers
     */
    public static void write(File file, List<@NonNull SegmentSpecifier> specifiers) {
        try (Writer writer = new FileWriter(file)) {
            writer.append(new Gson().toJson(specifiers));
        } catch (IOException e) {
            Activator.getInstance().logError(e.getMessage(), e);
        }
    }
}
