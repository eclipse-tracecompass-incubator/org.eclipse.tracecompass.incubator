/*******************************************************************************
 * Copyright (c) 2018 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.ftrace.core.trace;

import com.google.common.collect.ImmutableSet;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.event.GenericFtraceAspects;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.event.GenericFtraceEvent;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.event.GenericFtraceField;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.event.IGenericFtraceConstants;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.layout.GenericFtraceEventLayout;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.io.BufferedRandomAccessFile;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.regex.Matcher;

/**
 * Generic Ftrace trace.
 *
 * @author Guillaume Champagne
 * @author Alexis-Maurer Fortin
 * @author Hugo Genesse
 * @author Pierre-Yves Lajoie
 * @author Eva Terriault
 */
public abstract class GenericFtrace extends TmfTrace implements IKernelTrace {
    /**
     * FTrace magic number
     */
    protected static final byte[] TRACE_CMD_DAT_MAGIC = { 0x17, 0x08, 0x44, 't', 'r', 'a', 'c', 'i', 'n', 'g' };

    private static final int ESTIMATED_EVENT_SIZE = 90;
    private static final TmfLongLocation NULL_LOCATION = new TmfLongLocation(-1L);
    private static final TmfContext INVALID_CONTEXT = new TmfContext(NULL_LOCATION, ITmfContext.UNKNOWN_RANK);

    /**
     * Trace file locations
     */
    private File fFile;

    private RandomAccessFile fFileInput;

    /**
     * @param line
     *            Trace line to be parsed. This method can be overridden by
     *            Trace types that inherits from GenericFtrace.
     * @return Parsed FtraceField
     */
    protected @Nullable GenericFtraceField parseLine(String line) {
        if (line != null) {
            return GenericFtraceField.parseLine(line);
        }
        return null;
    }

    @Override
    public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type) throws TmfTraceException {
        super.initTrace(resource, path, type);
        try {
            fFile = new File(path);
            fFileInput = new BufferedRandomAccessFile(fFile, "r"); //$NON-NLS-1$
        } catch (IOException e) {
            throw new TmfTraceException(e.getMessage(), e);
        }
    }

    @Override
    public synchronized void dispose() {
        setFileInput(null);
        super.dispose();

    }

    @Override
    public double getLocationRatio(ITmfLocation location) {
        return ((Long) getCurrentLocation().getLocationInfo()).doubleValue() / getFile().length();
    }

    @Override
    public ITmfContext seekEvent(ITmfLocation location) {
        if (getFile() == null) {
            return INVALID_CONTEXT;
        }
        final TmfContext context = new TmfContext(NULL_LOCATION, ITmfContext.UNKNOWN_RANK);
        if (NULL_LOCATION.equals(location)) {
            return context;
        }
        try {
            RandomAccessFile fileInput = getFileInput();
            return seek(fileInput, location, context);
        } catch (final FileNotFoundException e) {
            Activator.getInstance().logError("Error seeking event. File not found: " + getPath(), e); //$NON-NLS-1$
            return context;
        } catch (final IOException e) {
            Activator.getInstance().logError("Error seeking event. File: " + getPath(), e); //$NON-NLS-1$
            return context;
        }
    }

    /**
     * Internal seek
     *
     * @param fileInput
     *            file to seek
     * @param location
     *            location of file
     * @param context
     *            context of the file
     * @return new context
     * @throws IOException
     *             file not found and such
     */
    protected ITmfContext seek(RandomAccessFile fileInput, ITmfLocation location, final TmfContext context) throws IOException {
        if (location == null) {
            fileInput.seek(0);
            long lineStartOffset = fileInput.getFilePointer();
            String line = fileInput.readLine();
            if (line == null) {
                return context;
            }
            Matcher matcher = IGenericFtraceConstants.FTRACE_PATTERN.matcher(line);
            while (!matcher.matches()) {
                lineStartOffset = fileInput.getFilePointer();
                line = fileInput.readLine();
                if (line == null) {
                    break;
                }
                matcher = IGenericFtraceConstants.FTRACE_PATTERN.matcher(line);
            }
            fileInput.seek(lineStartOffset);
        } else if (location.getLocationInfo() instanceof Long) {
            fileInput.seek((Long) location.getLocationInfo());
        }
        context.setLocation(new TmfLongLocation(fileInput.getFilePointer()));
        context.setRank(0);
        return context;
    }

    @Override
    public ITmfEvent parseEvent(ITmfContext context) {
        ITmfLocation location = context.getLocation();
        long rank = context.getRank();
        if (location instanceof TmfLongLocation) {
            TmfLongLocation tmfLongLocation = (TmfLongLocation) location;
            return parseEvent(getFileInput(), tmfLongLocation, rank);
        }
        return null;
    }

    /**
     * Internal parse
     *
     * @param fileInput
     *            file to read
     * @param tmfLongLocation
     *            location of event
     * @param rank
     *            rank of event
     * @return the event or null
     */
    protected ITmfEvent parseEvent(RandomAccessFile fileInput, TmfLongLocation tmfLongLocation, long rank) {
        Long locationInfo = tmfLongLocation.getLocationInfo();
        if (tmfLongLocation.equals(NULL_LOCATION)) {
            locationInfo = 0L;
        }
        if (locationInfo != null) {
            try {
                if (!locationInfo.equals(fileInput.getFilePointer())) {
                    fileInput.seek(locationInfo);
                }

                // Sometimes ftrace traces are contains comments starting with
                // '#' between
                // events
                String nextLine;
                do {
                    nextLine = fileInput.readLine();
                } while (nextLine != null && nextLine.startsWith(IGenericFtraceConstants.FTRACE_COMMENT_CHAR));

                GenericFtraceField field = parseLine(nextLine);
                if (field != null) {
                    return new GenericFtraceEvent(this, rank, field);
                }
            } catch (IOException e) {
                Activator.getInstance().logError("Error parsing event", e); //$NON-NLS-1$
            }
        }
        return null;
    }

    @Override
    public ITmfLocation getCurrentLocation() {
        long temp = -1;
        try {
            temp = getFileInput().getFilePointer();
        } catch (IOException e) {
            Activator.getInstance().logError(e.getMessage(), e);
        }
        return new TmfLongLocation(temp);
    }

    @Override
    public ITmfContext seekEvent(double ratio) {
        File file = getFile();
        if (file == null) {
            return INVALID_CONTEXT;
        }
        long filePos = (long) (file.length() * ratio);
        long estimatedRank = filePos / ESTIMATED_EVENT_SIZE;
        return seekEvent(new TmfLongLocation(estimatedRank));
    }

    @Override
    public Iterable<ITmfEventAspect<?>> getEventAspects() {

        /*
         * This method needs to fill the aspects dynamically because aspects in
         * the parent class are not all present at the beginning of the trace
         */
        ImmutableSet.Builder<ITmfEventAspect<?>> builder = ImmutableSet.builder();
        builder.addAll(GenericFtraceAspects.getAspects());
        return builder.build();
    }

    @Override
    public IKernelAnalysisEventLayout getKernelEventLayout() {
        return GenericFtraceEventLayout.getInstance();
    }

    /**
     * Get the fTrace file input
     *
     * @return the fTrace file input
     */
    protected RandomAccessFile getFileInput() {
        return fFileInput;
    }

    private void setFileInput(RandomAccessFile newFileInput) {
        try (RandomAccessFile fileInput = getFileInput()) {
            // do nothing
        } catch (IOException e) {
            Activator.getInstance().logError("Error disposing trace. File: " + getPath(), e); //$NON-NLS-1$
        }
        fFileInput = newFileInput;
    }

    /**
     * Get the fTrace file
     *
     * @return the fTrace file
     */
    protected File getFile() {
        return fFile;
    }

    /**
     * Set the fTrace file location
     *
     * @param file
     *            the file location
     * @throws TmfTraceException
     *             IO issue
     */
    protected void setFile(File file) throws TmfTraceException {
        fFile = file;
        try {
            setFileInput(new BufferedRandomAccessFile(file, "r")); //$NON-NLS-1$
        } catch (IOException e) {
            throw new TmfTraceException(e.getMessage(), e);
        }
    }
}
