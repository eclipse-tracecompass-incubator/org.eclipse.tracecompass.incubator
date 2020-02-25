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

package org.eclipse.tracecompass.incubator.internal.opentracing.core.trace;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.Activator;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.event.IOpenTracingConstants;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.event.OpenTracingAspects;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.event.OpenTracingEvent;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.event.OpenTracingField;
import org.eclipse.tracecompass.internal.provisional.jsontrace.core.trace.JsonTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfLostEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.io.BufferedRandomAccessFile;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

/**
 * Open Tracing trace. Can read jaeger unsorted or sorted JSON traces.
 *
 * @author Katherine Nadeau
 *
 */
public class OpenTracingTrace extends JsonTrace {

    private final @NonNull Iterable<@NonNull ITmfEventAspect<?>> fEventAspects;
    private final Map<String, String> fProcesses;

    /**
     * Constructor
     */
    @SuppressWarnings("null")
    public OpenTracingTrace() {
        fEventAspects = Lists.newArrayList(OpenTracingAspects.getAspects());
        fProcesses = new HashMap<>();
    }

    @Override
    public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type) throws TmfTraceException {
        super.initTrace(resource, path, type);
        fProperties.put("Type", "Open-Tracing"); //$NON-NLS-1$ //$NON-NLS-2$
        String dir = TmfTraceManager.getSupplementaryFileDir(this);
        fFile = new File(dir + new File(path).getName());
        if (!fFile.exists()) {
            Job sortJob = new OpenTracingSortingJob(this, path);
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
            registerProcesses(path);
        } catch (IOException e) {
            throw new TmfTraceException(e.getMessage(), e);
        }
    }

    /**
     * Save the processes list
     *
     * @param path
     *            trace file path
     */
    public void registerProcesses(String path) {
        try (FileReader fileReader = new FileReader(path)) {
            try (JsonReader reader = new JsonReader(fileReader);) {
                Gson gson = new Gson();
                JsonObject object = gson.fromJson(reader, JsonObject.class);
                JsonElement trace = object.get("data").getAsJsonArray().get(0); //$NON-NLS-1$
                JsonObject processes = trace.getAsJsonObject().get("processes").getAsJsonObject(); //$NON-NLS-1$
                for (int i = 1; i <= processes.size(); i++) {
                    String processName = "p" + i; //$NON-NLS-1$
                    fProcesses.put(processName, gson.toJson(processes.get(processName)));
                }
            }
        } catch (IOException e) {
            // Nothing
        }
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
                    OpenTracingField field = OpenTracingField.parseJson(line, null);
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
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Most assuredly NOT a Open-Tracing trace"); //$NON-NLS-1$
            }
        } catch (IOException e) {
            Activator.getInstance().logError("Error validating file: " + path, e); //$NON-NLS-1$
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "IOException validating file: " + path, e); //$NON-NLS-1$
        }
        return new TraceValidationStatus(confidence, Activator.PLUGIN_ID);
    }

    private static void goToCorrectStart(RandomAccessFile rafile) throws IOException {
        // skip start (ex.: "{"data":[{"traceID":"dcb56e6e03f3a509","spans":") before
        // the spans list
        StringBuilder sb = new StringBuilder();
        int val = rafile.read();
        /*
         * Skip list contains all the odd control characters
         */
        Set<Integer> skipList = new HashSet<>();
        skipList.add((int) ':');
        skipList.add((int) '\t');
        skipList.add((int) '\n');
        skipList.add((int) '\r');
        skipList.add((int) ' ');
        skipList.add((int) '\b');
        skipList.add((int) '\f');
        while (val != -1 && val != ':' && sb.length() < 46) {
            if (!skipList.contains(val)) {
                sb.append((char) val);
            }
            val = rafile.read();
        }

        if (sb.toString().startsWith("{\"data\"")) { //$NON-NLS-1$
            int data = 0;
            for (int nbBracket = 0; nbBracket < 2 && data != -1; nbBracket++) {
                data = rafile.read();
                while (data != '[' && data != -1) {
                    data = rafile.read();
                }
            }
        } else {
            rafile.seek(0);
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
            try {
                if (!locationInfo.equals(fFileInput.getFilePointer())) {
                    fFileInput.seek(locationInfo);
                }
                String nextJson = readNextEventString(() -> fFileInput.read());
                if (nextJson != null) {
                    String process = fProcesses.get(OpenTracingField.getProcess(nextJson));
                    OpenTracingField field = OpenTracingField.parseJson(nextJson, process);
                    if (field == null) {
                        return null;
                    }
                    return new OpenTracingEvent(this, context.getRank(), field);
                }
            } catch (IOException e) {
                Activator.getInstance().logError("Error parsing event", e); //$NON-NLS-1$
            }
        }
        return null;
    }

    @Override
    protected synchronized void updateAttributes(final ITmfContext context, final @NonNull ITmfEvent event) {
        ITmfTimestamp timestamp = event.getTimestamp();
        Long duration = event.getContent().getFieldValue(Long.class, IOpenTracingConstants.DURATION);
        ITmfTimestamp endTime = duration != null ? TmfTimestamp.fromNanos(timestamp.toNanos() + duration) : timestamp;
        if (event instanceof ITmfLostEvent) {
            endTime = ((ITmfLostEvent) event).getTimeRange().getEndTime();
        }
        if (getStartTime().equals(TmfTimestamp.BIG_BANG) || (getStartTime().compareTo(timestamp) > 0)) {
            setStartTime(timestamp);
        }
        if (getEndTime().equals(TmfTimestamp.BIG_CRUNCH) || (getEndTime().compareTo(endTime) < 0)) {
            setEndTime(endTime);
        }
        if (context.hasValidRank()) {
            long rank = context.getRank();
            if (getNbEvents() <= rank) {
                setNbEvents(rank + 1);
            }
            if (getIndexer() != null) {
                getIndexer().updateIndex(context, timestamp);
            }
        }
    }
}
