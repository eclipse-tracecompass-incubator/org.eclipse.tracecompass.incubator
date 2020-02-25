/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.ftrace.core.trace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.Activator;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.project.model.ITmfPropertiesProvider;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;

/**
 * Helper to open fTrace traces
 *
 * @author Matthew Khouzam
 *
 */
public class BinaryFTrace extends GenericFtrace implements ITmfPropertiesProvider {

    private static final String TRACE_CMD = "trace-cmd"; //$NON-NLS-1$
    private static final String REPORT = "report"; //$NON-NLS-1$
    private final @NonNull Map<@NonNull String, @NonNull String> fProperties = new LinkedHashMap<>();

    @Override
    public IStatus validate(IProject project, String path) {
        File file = new File(path);
        if (!file.exists()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "File not found: " + path); //$NON-NLS-1$
        }
        if (!file.isFile()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Not a file. It's a directory: " + path); //$NON-NLS-1$
        }
        int confidence = 32;
        try {
            if (!TmfTraceUtils.isText(file)) {
                int magicLength = TRACE_CMD_DAT_MAGIC.length;
                if (file.length() > magicLength) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] start = new byte[magicLength];
                        int read = fis.read(start);
                        if (read == magicLength && Arrays.equals(TRACE_CMD_DAT_MAGIC, start)) {
                            ProcessBuilder pb = new ProcessBuilder(TRACE_CMD);
                            Process traceCmd = pb.start();
                            try (BufferedReader br = new BufferedReader(new InputStreamReader(traceCmd.getInputStream(), Charset.forName("UTF-8")));) { //$NON-NLS-1$
                                String line = br.readLine();
                                while (line != null) {
                                    if (line.contains(REPORT)) {
                                        return new TraceValidationStatus(confidence, Activator.PLUGIN_ID);
                                    }
                                    line = br.readLine();
                                }
                            }
                        }
                    }
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Magic mismatch"); //$NON-NLS-1$
                }
            }
        } catch (IOException e) {
            Activator.getInstance().logError("Error validating file: " + path, e); //$NON-NLS-1$
            if (e.getMessage().startsWith("Cannot run program: \"" + TRACE_CMD + "\": error=2,")) { //$NON-NLS-1$ //$NON-NLS-2$
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "It appears trace-cmd is not available on your machine. Please install it to use the binary ftrace parser. (sudo apt install trace-cmd)", e); //$NON-NLS-1$
            }

            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "IOException validating file: " + path, e); //$NON-NLS-1$
        }
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Not an FTrace bin"); //$NON-NLS-1$
    }

    @Override
    public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type, String name, String traceTypeId) throws TmfTraceException {

        super.initTrace(resource, path, type, name, traceTypeId);
        fProperties.put("Type", "Trace-Event"); //$NON-NLS-1$ //$NON-NLS-2$
        String dir = TmfTraceManager.getSupplementaryFileDir(this);
        if (!new File(dir).exists()) {
            throw new TmfTraceException("Could not create temporary folder " + dir); //$NON-NLS-1$
        }
        File file = new File(dir + new File(path).getName());
        if (!file.exists()) {

            ProcessBuilder pb = new ProcessBuilder(TRACE_CMD, "report", "-i", path, "-R"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            pb.redirectOutput(file);
            try {
                pb.start().waitFor();
            } catch (IOException | InterruptedException e) {
                throw new TmfTraceException(e.getMessage(), e);
            }
            if (!file.exists()) {
                throw new TmfTraceException("Could not create temporary file " + file.getAbsolutePath()); //$NON-NLS-1$
            }
            if (file.length() <= 0) {
                throw new TmfTraceException("Empty temporary file " + file.getAbsolutePath()); //$NON-NLS-1$
            }
        }
        setFile(file);
    }

    @Override
    public @NonNull Map<@NonNull String, @NonNull String> getProperties() {
        return fProperties;
    }
}
