/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.otlp.core.trace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.event.IOpenTracingConstants;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.event.OpenTracingAspects;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.event.OpenTracingEvent;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.event.OpenTracingField;
import org.eclipse.tracecompass.incubator.internal.otlp.core.Activator;
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
import com.google.gson.stream.JsonReader;

/**
 * OTLP (OpenTelemetry Protocol) trace. Reads OTLP JSON traces exported from
 * Jaeger or other OpenTelemetry-compatible systems.
 *
 * @author Matthew Khouzam
 */
public class OtlpTrace extends JsonTrace {

    private final @NonNull Iterable<@NonNull ITmfEventAspect<?>> fEventAspects;

    /**
     * Constructor
     */
    public OtlpTrace() {
        fEventAspects = Lists.newArrayList(OpenTracingAspects.getAspects());
    }

    @Override
    public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type) throws TmfTraceException {
        super.initTrace(resource, path, type);
        fProperties.put("Type", "OTLP"); //$NON-NLS-1$ //$NON-NLS-2$
        String dir = TmfTraceManager.getSupplementaryFileDir(this);
        fFile = new File(dir + new File(path).getName());
        if (!fFile.exists()) {
            Job sortJob;
            if (isProtobufFile(path)) {
                sortJob = new OtlpProtobufSortingJob(this, path);
            } else {
                sortJob = new OtlpSortingJob(this, path);
            }
            sortJob.schedule();
            while (sortJob.getResult() == null) {
                try {
                    sortJob.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
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
    public IStatus validate(IProject project, String path) {
        File file = new File(path);
        if (!file.exists()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "File not found: " + path); //$NON-NLS-1$
        }
        if (!file.isFile()) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Not a file. It's a directory: " + path); //$NON-NLS-1$
        }
        boolean isText;
        try {
            isText = TmfTraceUtils.isText(file);
        } catch (IOException e) {
            Activator.getInstance().logError("Error validating file: " + path, e); //$NON-NLS-1$
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "IOException validating file: " + path, e); //$NON-NLS-1$
        }
        if (!isText) {
            // Check for protobuf format: first byte should be 0x0A
            // (field 1, wire type LENGTH_DELIMITED)
            if (isProtobufFile(path)) {
                return new TraceValidationStatus(MAX_CONFIDENCE - 5, Activator.PLUGIN_ID);
            }
            return new TraceValidationStatus(0, Activator.PLUGIN_ID);
        }
        // Check if this is an OTLP format file by looking for "resourceSpans"
        try (FileReader fileReader = new FileReader(path);
             JsonReader reader = new JsonReader(fileReader)) {
            reader.beginObject();
            if (reader.hasNext()) {
                String name = reader.nextName();
                if ("resourceSpans".equals(name)) { //$NON-NLS-1$
                    return new TraceValidationStatus(MAX_CONFIDENCE, Activator.PLUGIN_ID);
                }
            }
        } catch (Exception e) {
            // Not valid JSON or not OTLP - try JSONL format
        }
        // Check for JSONL format (one JSON object per line)
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String firstLine = br.readLine();
            if (firstLine != null && firstLine.trim().contains("\"resourceSpans\"")) { //$NON-NLS-1$
                return new TraceValidationStatus(MAX_CONFIDENCE - 1, Activator.PLUGIN_ID);
            }
        } catch (Exception e) {
            // Not valid JSONL
        }
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Not an OTLP trace"); //$NON-NLS-1$
    }

    /**
     * Check if a file looks like an OTLP protobuf file. The first byte of an
     * ExportTraceServiceRequest should be 0x0A (field 1, wire type 2 =
     * LENGTH_DELIMITED). Additionally validates that the length varint does not
     * exceed the remaining file size.
     */
    private static boolean isProtobufFile(String path) {
        File file = new File(path);
        try (FileInputStream fis = new FileInputStream(file)) {
            int firstByte = fis.read();
            if (firstByte != 0x0A) {
                return false;
            }
            // Read length varint
            long length = 0;
            int bytesConsumed = 1; // first byte already consumed
            int shift = 0;
            int b;
            do {
                b = fis.read();
                if (b == -1) {
                    return false;
                }
                bytesConsumed++;
                length |= (long) (b & 0x7F) << shift;
                shift += 7;
                if (shift > 35) {
                    return false; // varint too long
                }
            } while ((b & 0x80) != 0);
            // Verify declared length doesn't exceed remaining file size
            return length <= file.length() - bytesConsumed;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * For OTLP, the sorted file is already a flat list of JSON objects (one per
     * line), so we just need to skip to the start of the array.
     */
    private static void goToCorrectStart(RandomAccessFile rafile) throws IOException {
        // The sorted file starts with '[', skip it
        int val = rafile.read();
        while (val != -1 && val != '{') {
            val = rafile.read();
        }
        if (val == '{') {
            rafile.seek(rafile.getFilePointer() - 1);
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
                    OpenTracingField field = OtlpField.parseJson(nextJson);
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
