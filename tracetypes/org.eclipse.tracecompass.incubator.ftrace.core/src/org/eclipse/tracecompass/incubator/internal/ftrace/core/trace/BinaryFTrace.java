/*******************************************************************************
 * Copyright (c) 2018, 2022 Ericsson
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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceVersionHeader;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.parser.BinaryFTraceFileParser;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.strategies.BinaryFTraceV6Strategy;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.strategies.IBinaryFTraceStrategy;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.project.model.ITmfPropertiesProvider;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;

/**
 * Helper to open fTrace traces. By default, the helper will check if the file
 * is of binary format and the trace version is supported. If both of the
 * conditions are not met, the helper will convert and process the trace as a
 * text file as a fallback mechanism.
 *
 * @author Matthew Khouzam
 * @author Hoang Thuan Pham
 */
public class BinaryFTrace extends GenericFtrace implements ITmfPropertiesProvider {

    private static final String TRACE_CMD = "trace-cmd"; //$NON-NLS-1$
    private static final String REPORT = "report"; //$NON-NLS-1$
    private final @NonNull Map<@NonNull String, @NonNull String> fProperties = new LinkedHashMap<>();
    private IBinaryFTraceStrategy fStrategy;

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
                // Try to get a IBinaryFTraceStrategy to process the trace
                IBinaryFTraceStrategy strategy = getStrategy(file);
                if (strategy != null) {
                    return new TraceValidationStatus(confidence, Activator.PLUGIN_ID);
                }

                // Fallback implementation
                int magicLength = TRACE_CMD_DAT_MAGIC.length;
                if (file.length() < magicLength || !convertTraceToText(file)) {
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

        File file = new File(path);
        fStrategy = getStrategy(file);

        if (fStrategy != null) {
            fStrategy.initTrace(path);
        } else {
            String dir = TmfTraceManager.getSupplementaryFileDir(this);
            if (!new File(dir).exists()) {
                throw new TmfTraceException("Could not create temporary folder " + dir); //$NON-NLS-1$
            }
            file = new File(dir + new File(path).getName());
            initTraceAsText(file);
        }

        setFile(file);
    }

    private static File initTraceAsText(File file) throws TmfTraceException {
        if (!file.exists()) {
            ProcessBuilder pb = new ProcessBuilder(TRACE_CMD, REPORT, "-i", file.getAbsolutePath(), "-R"); //$NON-NLS-1$ //$NON-NLS-2$
                                                                                                           // //$NON-NLS-3$
            pb.redirectOutput(file);
            try {
                pb.start().waitFor();
            } catch (IOException e) {
                throw new TmfTraceException(e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TmfTraceException(e.getMessage(), e);
            }
            if (!file.exists()) {
                throw new TmfTraceException("Could not create temporary file " + file.getAbsolutePath()); //$NON-NLS-1$
            }
            if (file.length() <= 0) {
                throw new TmfTraceException("Empty temporary file " + file.getAbsolutePath()); //$NON-NLS-1$
            }
        }

        return file;
    }

    private IBinaryFTraceStrategy getStrategy(File file) {
        IBinaryFTraceStrategy strategy = null;

        try {
            BinaryFTraceVersionHeader versionHeader = BinaryFTraceFileParser.getFtraceVersionHeader(file.getAbsolutePath());
            if (BinaryFTraceV6Strategy.validate(versionHeader)) {
                strategy = new BinaryFTraceV6Strategy(this);
            }
        } catch (TmfTraceException e) {
            Activator.getInstance().logError("Invalid binary ftrace file.", e); //$NON-NLS-1$
        }

        return strategy;
    }

    private static boolean convertTraceToText(File file) throws IOException {
        int magicLength = TRACE_CMD_DAT_MAGIC.length;
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] start = new byte[magicLength];
            int read = fis.read(start);
            if (read == magicLength && Arrays.equals(TRACE_CMD_DAT_MAGIC, start)) {
                ProcessBuilder pb = new ProcessBuilder(TRACE_CMD);
                Process traceCmd = pb.start();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(traceCmd.getInputStream(), StandardCharsets.UTF_8));) {
                    String line = br.readLine();
                    while (line != null) {
                        if (line.contains(REPORT)) {
                            return true;
                        }
                        line = br.readLine();
                    }
                }
            }
        }

        return false;
    }

    @Override
    public synchronized ITmfEvent getNext(final ITmfContext context) {
        if (fStrategy != null) {
            return fStrategy.getNext(context);
        }

        return super.getNext(context);
    }

    /**
     * Instantiate a new iterator that iterates through the events of a binary
     * FTrace file that is associated with the current {@link BinaryFTrace}
     * object.
     *
     * @return A {@link ITmfContext} object; or null if there is no trace
     *         reading strategy found
     * @throws IOException
     *             If an error occurred while creating the iterator
     */
    public @Nullable ITmfContext createIterator() throws IOException {
        if (fStrategy != null) {
            return fStrategy.createIterator();
        }

        return null;
    }

    @Override
    public ITmfContext seekEvent(ITmfLocation location) {
        if (fStrategy != null) {
            return fStrategy.seekEvent(location);
        }

        return super.seekEvent(location);
    }

    @Override
    public void setStartTime(final @NonNull ITmfTimestamp startTime) {
        super.setStartTime(startTime);
    }

    @Override
    public void setEndTime(final @NonNull ITmfTimestamp endTime) {
        super.setEndTime(endTime);
    }

    @Override
    public synchronized void updateAttributes(final ITmfContext context, final @NonNull ITmfEvent event) {
        super.updateAttributes(context, event);
    }

    @Override
    public @NonNull Map<@NonNull String, @NonNull String> getProperties() {
        return fProperties;
    }
}
