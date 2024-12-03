/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.trace;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.Activator;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.event.TraceEventAspects;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.event.TraceEventEvent;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.event.TraceEventField;
import org.eclipse.tracecompass.internal.provisional.jsontrace.core.trace.JsonTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.io.BufferedRandomAccessFile;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

import com.google.common.collect.Lists;

/**
 * Trace event trace. Can read trace event unsorted or sorted JSON traces.
 *
 * @author Matthew Khouzam
 *
 */
public class TraceEventTrace extends JsonTrace {

    /**
     * Tid prefix to add to thread name
     */
    private static final String TID_PREFIX = "tid-"; //$NON-NLS-1$
    /**
     * Pid Labels prefix
     */
    private static final String PID_LABEL_PREFIX = "pidLabel-"; //$NON-NLS-1$
    /**
     * Pid name prefix
     */
    private static final String PID_PREFIX = "pid-"; //$NON-NLS-1$
    /**
     * Metadata Field String Name
     */
    private static final String NAME_ARG = "name"; //$NON-NLS-1$
    /**
     * Metadata Field String labels
     */
    private static final String LABELS = "labels"; //$NON-NLS-1$
    /**
     * Metadata Field String sort index
     */
    private static final String SORT_INDEX = "sort_index"; //$NON-NLS-1$
    /**
     * Metadata String Process Name
     */
    private static final String PROCESS_NAME = "process_name"; //$NON-NLS-1$
    /**
     * Metadata String Process Labels
     */
    private static final String PROCESS_LABELS = "process_labels"; //$NON-NLS-1$
    /**
     * Metadata String Process Sort Index
     */
    private static final String PROCESS_SORT_INDEX = "process_sort_index"; //$NON-NLS-1$
    /**
     * Metadata String Thread Name
     */
    private static final String THREAD_NAME = "thread_name"; //$NON-NLS-1$
    /**
     * Metadata String Thread Sort Index
     */
    private static final String THREAD_SORT_INDEX = "thread_sort_index"; //$NON-NLS-1$

    private static final String EVENTS_ARRAY_KEY = "traceEvents"; //$NON-NLS-1$

    private static final Pattern TID_REGEX = Pattern.compile("\\d+"); //$NON-NLS-1$

    private final @NonNull Map<Object, String> fPidNames = new HashMap<>();
    private final @NonNull Map<Object, Integer> fTidMap = new HashMap<>();
    private final @NonNull NavigableMap<Integer, String> fTidNames = new TreeMap<>();
    private final @NonNull Iterable<@NonNull ITmfEventAspect<?>> fEventAspects;

    /**
     * Constructor
     */
    public TraceEventTrace() {
        List<@NonNull ITmfEventAspect<?>> aspects = Lists.newArrayList(TraceEventAspects.getAspects());
        aspects.add(new ProcessNameAspect());
        aspects.add(new ThreadNameAspect());
        fEventAspects = aspects;
    }

    @Override
    public IStatus validate(IProject project, String path) {
        File file = new File(path);
        if (!file.exists()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "File not found: " + path); //$NON-NLS-1$
        }
        if (!file.isFile()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Not a file. It's a directory: " + path); //$NON-NLS-1$
        }
        int confidence = 0;

        try {
            if (!TmfTraceUtils.isText(file)) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Trace events are text only"); //$NON-NLS-1$
            }
        } catch (IOException e) {
            Activator.getInstance().logError("Error validating file: " + path, e); //$NON-NLS-1$
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "IOException validating file: " + path, e); //$NON-NLS-1$
        }
        try (BufferedRandomAccessFile rafile = new BufferedRandomAccessFile(path, "r")) { //$NON-NLS-1$
            goToCorrectStart(rafile);
            int lineCount = 0;
            int matches = 0;
            String line = readNextEventString(rafile::read);
            while ((line != null) && (lineCount++ < MAX_LINES)) {
                try {
                    TraceEventField field = TraceEventField.parseJson(line);
                    if (field != null) {
                        matches++;
                    }
                } catch (RuntimeException e) {
                    confidence = Integer.MIN_VALUE;
                }

                confidence = MAX_CONFIDENCE * matches / lineCount;
                line = readNextEventString(rafile::read);
            }
            if (matches == 0) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No Trace Event events found in the first " + lineCount + " lines. File appears to still be a JSON formatted file though."); //$NON-NLS-1$ //$NON-NLS-2$
            }
        } catch (IOException e) {
            Activator.getInstance().logError("Error validating file: " + path, e); //$NON-NLS-1$
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "IOException validating file: " + path, e); //$NON-NLS-1$
        }
        return new TraceValidationStatus(confidence, Activator.PLUGIN_ID);
    }

    @Override
    public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type) throws TmfTraceException {
        super.initTrace(resource, path, type);
        fProperties.put("Type", "Trace-Event"); //$NON-NLS-1$ //$NON-NLS-2$
        String dir = TmfTraceManager.getSupplementaryFileDir(this);
        fFile = new File(dir + new File(path).getName());

        try {
            if (!fFile.exists()) {
                Job sortJob;
                try (BufferedRandomAccessFile rafile = new BufferedRandomAccessFile(path, "r")) { //$NON-NLS-1$
                    if (isArrayTrace(rafile)) {
                        sortJob = new TraceEventSortingJob(this, path, Collections.emptyList());
                    } else {
                        sortJob = new TraceEventSortingJob(this, path, List.of(EVENTS_ARRAY_KEY));
                    }
                }
                sortJob.schedule();
                while (sortJob.getResult() == null) {
                    try {
                        sortJob.join();
                    } catch (InterruptedException e) {
                        throw new TmfTraceException(e.getMessage(), e);
                    }
                }
                IStatus result = sortJob.getResult();
                if (!result.isOK()) {
                    throw new TmfTraceException("Job failed " + result.getMessage()); //$NON-NLS-1$
                }
            }
            fFileInput = new BufferedRandomAccessFile(fFile, "r"); //$NON-NLS-1$
            goToCorrectStart(fFileInput);
            /* Set the start and (current) end times for this trace */
            ITmfContext ctx = seekEvent(0L);
            if (ctx == null) {
                return;
            }
            ITmfEvent event = getNext(ctx);
            if (event != null) {
                final ITmfTimestamp curTime = event.getTimestamp();
                setStartTime(curTime);
                setEndTime(curTime);
            }
            ctx.dispose();
        } catch (IOException e) {
            throw new TmfTraceException(e.getMessage(), e);
        }
    }

    private static boolean isArrayTrace(RandomAccessFile rafile) throws IOException {
        int val = ' ';
        while(val == ' ') {
            val = rafile.read();
        }
        rafile.seek(0);
        return val == '[';
    }

    /**
     * Update the file position to be at the actual start of events, after the
     * trace event header
     *
     * @param rafile
     *            The random access file
     * @throws IOException
     *             Exceptions reading the file
     */
    protected static void goToCorrectStart(RandomAccessFile rafile) throws IOException {
        // The file either starts with [ and then all the subsequent objects are
        // events
        if (isArrayTrace(rafile)) {
            return;
        }
        // or we have to skip to the key EVENTS_ARRAY_KEY to get the array of
        // events
        StringBuilder stringBuffer = new StringBuilder();
        /*
         * Skip list contains all the odd control characters
         */
        Set<Integer> skipList = new HashSet<>();
        skipList.add((int) '\t');
        skipList.add((int) '\n');
        skipList.add((int) '\r');
        skipList.add((int) ' ');
        skipList.add((int) '\b');
        skipList.add((int) '\f');
        int maxBufferSize = 200;
        int maxOffset = 50000;
        int offset = 1;
        int val = 0;
        while (val != -1 && offset < maxOffset) {
            if (!skipList.contains(val)) {
                stringBuffer.append((char) val);
                if (stringBuffer.length() > maxBufferSize) {
                    String trailingEnd = stringBuffer.substring(maxBufferSize - EVENTS_ARRAY_KEY.length(), stringBuffer.length());
                    stringBuffer = new StringBuilder(trailingEnd);
                }
            }
            if (stringBuffer.toString().endsWith(EVENTS_ARRAY_KEY)) {
                while (val != ':') {
                    val = rafile.read();
                }
                return;
            }
            val = rafile.read();
            offset += 1;
        }
    }

    @Override
    public Iterable<@NonNull ITmfEventAspect<?>> getEventAspects() {
        return fEventAspects;
    }

    @Override
    public ITmfEvent parseEvent(ITmfContext context) {
        @Nullable
        ITmfLocation location = context.getLocation();
        if (location instanceof TmfLongLocation) {
            TmfLongLocation tmfLongLocation = (TmfLongLocation) location;
            Long locationInfo = tmfLongLocation.getLocationInfo();
            if (location.equals(NULL_LOCATION)) {
                locationInfo = 0L;
            }
            if (locationInfo != null) {
                try {
                    if (!locationInfo.equals(fFileInput.getFilePointer())) {
                        fFileInput.seek(locationInfo);
                    }
                    String nextJson = readNextEventString(() -> fFileInput.read());
                    while (nextJson != null) {
                        TraceEventField field = TraceEventField.parseJson(nextJson);
                        if (field == null) {
                            nextJson = readNextEventString(() -> fFileInput.read());
                            continue;
                        }
                        if (field.getPhase() != 'M') {
                            return new TraceEventEvent(this, context.getRank(), field);
                        }
                        parseMetadata(field);
                        nextJson = readNextEventString(() -> fFileInput.read());
                    }
                } catch (IOException e) {
                    Activator.getInstance().logError("Error parsing event", e); //$NON-NLS-1$
                }
            }
        }
        return null;
    }

    private void parseMetadata(TraceEventField field) {
        Map<@NonNull String, @NonNull Object> args = field.getArgs();
        String name = field.getName();
        if (args == null) {
            return;
        }
        Map<@NonNull String, @NonNull String> properties = fProperties;
        Object tid = field.getTid();
        switch (name) {
        case PROCESS_NAME:
            String procName = (String) args.get(NAME_ARG);
            fPidNames.put(field.getPid(), procName);
            if (procName != null) {
                properties.put(PID_PREFIX + field.getPid(), procName);
            }
            break;
        case PROCESS_LABELS:
            String procLabels = (String) args.get(LABELS);
            if (procLabels != null) {
                properties.put(PID_LABEL_PREFIX + field.getPid(), procLabels);
            }
            break;
        case PROCESS_SORT_INDEX:
            String sortIndex = (String) args.get(SORT_INDEX);
            if (sortIndex != null) {
                properties.put(name + '-' + field.getPid(), sortIndex);
            }
            break;
        case THREAD_NAME:
            String threadName = (String) args.get(NAME_ARG);
            if (tid instanceof Integer tidInt) {
                fTidNames.put(tidInt, threadName);
            }
            if (threadName != null) {
                properties.put(TID_PREFIX + tid, threadName);
            }
            break;
        case THREAD_SORT_INDEX:
            sortIndex = (String) args.get(SORT_INDEX);
            if (sortIndex != null) {
                properties.put(name + '-' + tid, sortIndex);
            }
            break;
        default:
            properties.put(name, String.valueOf(args));
            break;
        }

    }

    /**
     * Get the Process name
     */
    public class ProcessNameAspect extends org.eclipse.tracecompass.incubator.analysis.core.aspects.ProcessNameAspect {

        @Override
        public @Nullable String resolve(@NonNull ITmfEvent event) {
            if (event instanceof TraceEventEvent traceEventEvent) {
                TraceEventField field = traceEventEvent.getField();
                if (field.getPid() != null) {
                    return fPidNames.get(field.getPid());
                }
            }
            return null;
        }
    }

    /**
     * Get the Thread name
     */
    public class ThreadNameAspect extends org.eclipse.tracecompass.incubator.analysis.core.aspects.ThreadNameAspect {

        @Override
        public @Nullable String resolve(@NonNull ITmfEvent event) {
            if (event instanceof TraceEventEvent traceEventEvent) {
                TraceEventField field = traceEventEvent.getField();
                Object tid = field.getTid();
                if (tid instanceof String tidString && TID_REGEX.matcher(tidString).matches()) {
                    tid = Integer.parseInt(tidString);
                }
                if (tid instanceof Integer) {
                    return fTidNames.get(tid);
                }

            }
            return null;
        }
    }

    /**
     * Register a TID, it is normally numerical, but a best effort is made for
     * string TIDs.
     *
     * @param tid
     *            the TID object
     * @return an integer value
     */
    public @Nullable Integer registerTid(Object tid) {
        if (tid instanceof Integer tidInt) {
            fTidMap.put(tid, tidInt);
            return tidInt;
        }
        if (tid instanceof String tidString && TID_REGEX.matcher(tidString).matches()) {
            return Integer.parseInt(tidString);
        }

        return null;
    }
}
