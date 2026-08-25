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

package org.eclipse.tracecompass.incubator.internal.agent.trace.core.trace;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.agent.trace.core.Activator;
import org.eclipse.tracecompass.incubator.internal.agent.trace.core.event.AgentTraceEvent;
import org.eclipse.tracecompass.incubator.internal.agent.trace.core.event.AgentTraceEventType;
import org.eclipse.tracecompass.incubator.internal.agent.trace.core.event.IAgentTraceConstants;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.io.BufferedRandomAccessFile;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * Trace implementation for Agent Trace format (JSONL).
 * <p>
 * The Agent Trace format is a line-based JSON format where each line represents
 * a single event. Each event has required fields: "type", "timestamp", and
 * "event_id", with optional "parent_id", "metadata", and "data" fields.
 * </p>
 *
 * @author Eya Matkho
 */
public class AgentTraceTrace extends TmfTrace {

    private static final int MAX_LINES = 100;
    private static final int MAX_CONFIDENCE = 100;
    private static final TmfLongLocation NULL_LOCATION = new TmfLongLocation(-1L);
    private static final TmfContext INVALID_CONTEXT = new TmfContext(NULL_LOCATION, ITmfContext.UNKNOWN_RANK);
    private static final String JSONL_EXTENSION = ".jsonl"; //$NON-NLS-1$

    private @Nullable File fFile;
    private @Nullable RandomAccessFile fFileInput;
    private final Gson fGson = new Gson();

    /**
     * Default constructor
     */
    public AgentTraceTrace() {
        super();
    }

    @Override
    public void initTrace(@Nullable IResource resource, @Nullable String path,
            @Nullable Class<? extends ITmfEvent> type) throws TmfTraceException {
        super.initTrace(resource, path, type);
        if (path == null) {
            throw new TmfTraceException("Path cannot be null"); //$NON-NLS-1$
        }
        try {
            fFile = new File(path);
            fFileInput = new BufferedRandomAccessFile(fFile, "r"); //$NON-NLS-1$
        } catch (IOException e) {
            throw new TmfTraceException(e.getMessage(), e);
        }
    }

    @Override
    public synchronized void dispose() {
        RandomAccessFile fileInput = fFileInput;
        if (fileInput != null) {
            try {
                fileInput.close();
            } catch (IOException e) {
                Activator.getInstance().logError("Error closing trace file", e); //$NON-NLS-1$
            }
            fFileInput = null;
        }
        super.dispose();
    }

    @Override
    public IStatus validate(@Nullable IProject project, @Nullable String path) {
        if (path == null) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Path is null"); //$NON-NLS-1$
        }
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

        // Bonus confidence for .jsonl extension
        if (path.endsWith(JSONL_EXTENSION)) {
            confidence += 10;
        }

        try (BufferedRandomAccessFile rafile = new BufferedRandomAccessFile(path, "r")) { //$NON-NLS-1$
            int lineCount = 0;
            int matches = 0;
            String line = rafile.readLine();
            while (line != null && lineCount < MAX_LINES) {
                line = line.trim();
                if (line.isEmpty()) {
                    line = rafile.readLine();
                    continue;
                }
                lineCount++;
                if (isValidAgentTraceLine(line)) {
                    matches++;
                }
                line = rafile.readLine();
            }
            if (lineCount == 0 || matches == 0) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Not an Agent Trace file"); //$NON-NLS-1$
            }
            confidence += MAX_CONFIDENCE * matches / lineCount;
            confidence = Math.min(confidence, MAX_CONFIDENCE);
        } catch (IOException e) {
            Activator.getInstance().logError("Error validating file: " + path, e); //$NON-NLS-1$
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "IOException validating file: " + path, e); //$NON-NLS-1$
        }
        return new TraceValidationStatus(confidence, Activator.PLUGIN_ID);
    }

    /**
     * Check if a JSON line is a valid agent trace event (has "type" and
     * "timestamp" with known type).
     */
    private boolean isValidAgentTraceLine(String line) {
        try {
            JsonObject json = fGson.fromJson(line, JsonObject.class);
            if (json == null) {
                return false;
            }
            JsonElement typeElement = json.get(IAgentTraceConstants.TYPE);
            JsonElement timestampElement = json.get(IAgentTraceConstants.TIMESTAMP);
            if (typeElement == null || timestampElement == null) {
                return false;
            }
            if (!typeElement.isJsonPrimitive() || !timestampElement.isJsonPrimitive()) {
                return false;
            }
            String typeStr = typeElement.getAsString();
            return AgentTraceEventType.isKnownType(typeStr);
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    @Override
    public @Nullable ITmfEvent parseEvent(@Nullable ITmfContext context) {
        if (context == null) {
            return null;
        }
        ITmfLocation location = context.getLocation();
        if (!(location instanceof TmfLongLocation)) {
            return null;
        }
        TmfLongLocation tmfLongLocation = (TmfLongLocation) location;
        Long locationInfo = tmfLongLocation.getLocationInfo();
        if (locationInfo == null || tmfLongLocation.equals(NULL_LOCATION)) {
            return null;
        }

        RandomAccessFile fileInput = fFileInput;
        if (fileInput == null) {
            return null;
        }

        try {
            if (!locationInfo.equals(fileInput.getFilePointer())) {
                fileInput.seek(locationInfo);
            }
            String line = fileInput.readLine();
            while (line != null && line.trim().isEmpty()) {
                line = fileInput.readLine();
            }
            if (line == null) {
                return null;
            }
            return parseLine(line.trim(), context.getRank());
        } catch (IOException e) {
            Activator.getInstance().logError("Error parsing event", e); //$NON-NLS-1$
        }
        return null;
    }

    /**
     * Parse a single JSONL line into an {@link AgentTraceEvent}.
     */
    private @Nullable ITmfEvent parseLine(String line, long rank) {
        try {
            JsonObject json = fGson.fromJson(line, JsonObject.class);
            if (json == null) {
                return null;
            }

            // Extract required fields
            JsonElement typeElement = json.get(IAgentTraceConstants.TYPE);
            JsonElement timestampElement = json.get(IAgentTraceConstants.TIMESTAMP);
            JsonElement eventIdElement = json.get(IAgentTraceConstants.EVENT_ID);
            if (typeElement == null || timestampElement == null || eventIdElement == null) {
                return null;
            }

            String typeStr = typeElement.getAsString();
            String timestampStr = timestampElement.getAsString();

            // Parse timestamp
            ITmfTimestamp timestamp = parseTimestamp(timestampStr);
            if (timestamp == null) {
                return null;
            }

            // Build event content fields
            int fieldCount = json.entrySet().size();
            ITmfEventField[] fields = new ITmfEventField[fieldCount];
            int i = 0;
            for (Entry<String, JsonElement> entry : json.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();
                String fieldValue;
                if (value.isJsonPrimitive()) {
                    fieldValue = value.getAsString();
                } else {
                    fieldValue = value.toString();
                }
                fields[i] = new TmfEventField(key, fieldValue, null);
                i++;
            }

            ITmfEventField content = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, null, fields);
            TmfEventType eventType = new TmfEventType(typeStr, content);

            return new AgentTraceEvent(this, rank, timestamp, eventType, content);
        } catch (JsonSyntaxException e) {
            Activator.getInstance().logError("Error parsing JSON line", e); //$NON-NLS-1$
            return null;
        }
    }

    /**
     * Parse an ISO 8601 timestamp string to nanoseconds from epoch.
     *
     * @param timestampStr
     *            the ISO 8601 timestamp string
     * @return the timestamp, or {@code null} if parsing fails
     */
    private static @Nullable ITmfTimestamp parseTimestamp(String timestampStr) {
        try {
            Instant instant = Instant.parse(timestampStr);
            long nanos = Math.addExact(
                    Math.multiplyExact(instant.getEpochSecond(), 1_000_000_000L),
                    instant.getNano());
            return TmfTimestamp.fromNanos(nanos);
        } catch (DateTimeParseException | ArithmeticException e) {
            return null;
        }
    }

    @Override
    public ITmfLocation getCurrentLocation() {
        RandomAccessFile fileInput = fFileInput;
        if (fileInput == null) {
            return NULL_LOCATION;
        }
        try {
            return new TmfLongLocation(fileInput.getFilePointer());
        } catch (IOException e) {
            Activator.getInstance().logError("Error getting current location", e); //$NON-NLS-1$
            return NULL_LOCATION;
        }
    }

    @Override
    public double getLocationRatio(@Nullable ITmfLocation location) {
        File file = fFile;
        if (file == null || location == null) {
            return 0;
        }
        Object locationInfo = location.getLocationInfo();
        if (locationInfo instanceof Long) {
            return ((Long) locationInfo).doubleValue() / file.length();
        }
        return 0;
    }

    @Override
    public ITmfContext seekEvent(@Nullable ITmfLocation location) {
        RandomAccessFile fileInput = fFileInput;
        if (fileInput == null || fFile == null) {
            return INVALID_CONTEXT;
        }
        TmfContext context = new TmfContext(NULL_LOCATION, ITmfContext.UNKNOWN_RANK);
        if (NULL_LOCATION.equals(location)) {
            return context;
        }
        try {
            if (location == null) {
                fileInput.seek(0);
            } else {
                Object locationInfo = location.getLocationInfo();
                if (locationInfo instanceof Long) {
                    fileInput.seek((Long) locationInfo);
                }
            }
            context.setLocation(new TmfLongLocation(fileInput.getFilePointer()));
            context.setRank(0);
            return context;
        } catch (IOException e) {
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
        return seekEvent(new TmfLongLocation(filePos));
    }
}
