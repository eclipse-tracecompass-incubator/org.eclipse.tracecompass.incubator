/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.trace;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

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
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.io.BufferedRandomAccessFile;
import org.eclipse.tracecompass.tmf.core.project.model.ITmfPropertiesProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceKnownSize;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.indexer.ITmfPersistentlyIndexable;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

import com.google.common.collect.Lists;

/**
 * Trace event trace. Can read trace event unsorted or sorted JSON traces.
 *
 * @author Matthew Khouzam
 *
 */
public class TraceEventTrace extends TmfTrace implements ITmfPersistentlyIndexable, ITmfPropertiesProvider, ITmfTraceKnownSize {

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

    private static final int CHECKPOINT_SIZE = 10000;
    private static final int ESTIMATED_EVENT_SIZE = 90;
    private static final TmfLongLocation NULL_LOCATION = new TmfLongLocation(-1L);
    private static final TmfContext INVALID_CONTEXT = new TmfContext(NULL_LOCATION, ITmfContext.UNKNOWN_RANK);
    private static final int MAX_LINES = 100;
    private static final int MAX_CONFIDENCE = 100;
    private final @NonNull Map<@NonNull String, @NonNull String> fProperties = new LinkedHashMap<>();
    private final @NonNull Map<Object, String> fPidNames = new HashMap<>();
    private final @NonNull NavigableMap<Integer, String> fTidNames = new TreeMap<>();

    private File fFile;

    private RandomAccessFile fFileInput;
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
                return new TraceValidationStatus(confidence, Activator.PLUGIN_ID);
            }
        } catch (IOException e) {
            Activator.getInstance().logError("Error validating file: " + path, e); //$NON-NLS-1$
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "IOException validating file: " + path, e); //$NON-NLS-1$
        }
        try (BufferedRandomAccessFile rafile = new BufferedRandomAccessFile(path, "r")) { //$NON-NLS-1$
            goToCorrectStart(rafile);
            int lineCount = 0;
            int matches = 0;
            String line = readNextEventString(() -> rafile.read());
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
                line = readNextEventString(() -> rafile.read());
            }
            if (matches == 0) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Most assuredly NOT a traceevent trace"); //$NON-NLS-1$
            }
        } catch (IOException e) {
            Activator.getInstance().logError("Error validating file: " + path, e); //$NON-NLS-1$
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "IOException validating file: " + path, e); //$NON-NLS-1$
        }
        return new TraceValidationStatus(confidence, Activator.PLUGIN_ID);
    }

    private static void goToCorrectStart(RandomAccessFile rafile) throws IOException {
        // skip start if it's {"traceEvents":
        String readLine = rafile.readLine();
        if (readLine == null) {
            return;
        }
        if (readLine.startsWith("{\"traceEvents\":")) { //$NON-NLS-1$
            rafile.seek(14);
        } else {
            rafile.seek(0);
        }
    }

    @Override
    public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type) throws TmfTraceException {
        super.initTrace(resource, path, type);
        fProperties.put("Type", "Trace-Event"); //$NON-NLS-1$ //$NON-NLS-2$ , value)
        String dir = TmfTraceManager.getSupplementaryFileDir(this);
        fFile = new File(dir + new File(path).getName());
        if (!fFile.exists()) {
            Job sortJob = new SortingJob(this, path);
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
        try {
            fFileInput = new BufferedRandomAccessFile(fFile, "r"); //$NON-NLS-1$
            goToCorrectStart(fFileInput);
        } catch (IOException e) {
            throw new TmfTraceException(e.getMessage(), e);
        }
    }

    @Override
    public synchronized void dispose() {
        if (fFileInput != null) {
            try {
                fFileInput.close();
            } catch (IOException e) {
                Activator.getInstance().logError("Error disposing trace. File: " + getPath(), e); //$NON-NLS-1$
            }
        }
        super.dispose();

    }

    @Override
    public double getLocationRatio(ITmfLocation location) {
        return ((Long) getCurrentLocation().getLocationInfo()).doubleValue() / fFile.length();
    }

    @Override
    public ITmfContext seekEvent(ITmfLocation location) {
        if (fFile == null) {
            return INVALID_CONTEXT;
        }
        final TmfContext context = new TmfContext(NULL_LOCATION, ITmfContext.UNKNOWN_RANK);
        if (NULL_LOCATION.equals(location) || fFile == null) {
            return context;
        }
        try {
            if (location == null) {
                fFileInput.seek(1);
            } else if (location.getLocationInfo() instanceof Long) {
                fFileInput.seek((Long) location.getLocationInfo());
            }
            context.setLocation(new TmfLongLocation(fFileInput.getFilePointer()));
            context.setRank(0);
            return context;
        } catch (final FileNotFoundException e) {
            Activator.getInstance().logError("Error seeking event. File not found: " + getPath(), e); //$NON-NLS-1$
            return context;
        } catch (final IOException e) {
            Activator.getInstance().logError("Error seeking event. File: " + getPath(), e); //$NON-NLS-1$
            return context;
        }
    }

    @Override
    public ITmfContext seekEvent(double ratio) {
        File file = fFile;
        if (file == null) {
            return INVALID_CONTEXT;
        }
        long filePos = (long) (file.length() * ratio);
        long estimatedRank = filePos / ESTIMATED_EVENT_SIZE;
        return seekEvent(new TmfLongLocation(estimatedRank));
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
                            return null;
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
        switch (name) {
        case PROCESS_NAME:
            String procName = (String) args.get(NAME_ARG);
            fPidNames.put(field.getPid(), procName);
            if (procName != null) {
                fProperties.put(PID_PREFIX + field.getPid(), procName);
            }
            break;
        case PROCESS_LABELS:
            String procLabels = (String) args.get(LABELS);
            if (procLabels != null) {
                fProperties.put(PID_LABEL_PREFIX + field.getPid(), procLabels);
            }
            break;
        case PROCESS_SORT_INDEX:
            String sortIndex = (String) args.get(SORT_INDEX);
            if (sortIndex != null) {
                fProperties.put(name + '-' + field.getPid(), sortIndex);
            }
            break;
        case THREAD_NAME:
            String threadName = (String) args.get(NAME_ARG);
            fTidNames.put(field.getTid(), threadName);
            if (threadName != null) {
                fProperties.put(TID_PREFIX + field.getTid(), threadName);
            }
            break;
        case THREAD_SORT_INDEX:
            sortIndex = (String) args.get(SORT_INDEX);
            if (sortIndex != null) {
                fProperties.put(name + '-' + field.getTid(), sortIndex);
            }
            break;
        default:
            fProperties.put(name, String.valueOf(args));
            break;
        }

    }

    @Override
    public ITmfLocation getCurrentLocation() {
        long temp = -1;
        try {
            temp = fFileInput.getFilePointer();
        } catch (IOException e) {
        }
        return new TmfLongLocation(temp);
    }

    @Override
    public @NonNull Map<@NonNull String, @NonNull String> getProperties() {
        return fProperties;
    }

    @Override
    public ITmfLocation restoreLocation(ByteBuffer bufferIn) {
        return new TmfLongLocation(bufferIn);
    }

    @Override
    public int getCheckpointSize() {
        return CHECKPOINT_SIZE;
    }

    /**
     * Wrapper to get a character reader, allows to reconcile between java.nio and
     * java.io
     *
     * @author Matthew Khouzam
     *
     */
    public static interface IReaderWrapper {
        /**
         * Read the next character
         *
         * @return the next char
         * @throws IOException
         *             out of chars to read
         */
        int read() throws IOException;
    }

    /**
     * Manually parse a string of JSON. High performance to extract one object
     *
     * @param parser
     *            the reader
     * @return a String with a json object
     * @throws IOException
     *             end of file, file not found or such
     */
    public static @Nullable String readNextEventString(IReaderWrapper parser) throws IOException {
        StringBuffer sb = new StringBuffer();
        int scope = -1;
        int arrScope = 0;
        boolean inQuotes = false;
        int elem = parser.read();
        while (elem != -1) {
            if (elem == '"') {
                inQuotes = !inQuotes;
            } else {
                if (inQuotes) {
                    // do nothing
                } else if (elem == '[') {
                    arrScope++;
                } else if (elem == ']') {
                    if (arrScope > 0) {
                        arrScope--;
                    } else {
                        return null;
                    }
                } else if (elem == '{') {
                    scope++;
                } else if (elem == '}') {
                    if (scope > 0) {
                        scope--;
                    } else {
                        sb.append((char) elem);
                        return sb.toString();
                    }
                }
            }
            if (scope >= 0) {
                sb.append((char) elem);
            }
            elem = parser.read();
        }
        return null;
    }

    @Override
    public int size() {
        RandomAccessFile fileInput = fFileInput;
        if (fileInput == null) {
            return 0;
        }
        long length = 0;
        try {
            length = fileInput.length();
        } catch (IOException e) {
            // swallow it for now
        }
        return length > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) length;
    }

    @Override
    public int progress() {
        RandomAccessFile fileInput = fFileInput;
        if (fileInput == null) {
            return 0;
        }
        long length = 0;
        try {
            length = fileInput.getFilePointer();
        } catch (IOException e) {
            // swallow it for now
        }
        return length > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) length;
    }

    /**
     * Get the Process name
     */
    public class ProcessNameAspect extends org.eclipse.tracecompass.incubator.analysis.core.aspects.ProcessNameAspect {

        @Override
        public @Nullable String resolve(@NonNull ITmfEvent event) {
            if (event instanceof TraceEventEvent) {
                TraceEventField field = ((TraceEventEvent) event).getField();
                return fPidNames.get(field.getPid());
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
            if (event instanceof TraceEventEvent) {
                TraceEventField field = ((TraceEventEvent) event).getField();
                return fTidNames.get(field.getTid());
            }
            return null;
        }
    }
}
