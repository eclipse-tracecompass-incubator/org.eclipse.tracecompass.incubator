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

package org.eclipse.tracecompass.incubator.internal.traceevent.core.event;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Trace Event fields. Used as a quick wrapper for Trace Event log data.
 *
 * @author Matthew Khouzam
 */
@NonNullByDefault
public class TraceEventField {

    /**
     * Name of exit events when no other name is available
     */
    public static final String UNKNOWN_EXIT_EVENT = "exit unknown"; //$NON-NLS-1$
    /**
     * Name of exit duration events when no other name is available
     */
    public static final String UNKNOWN_DURATION_EXIT_EVENT = "duration exit"; //$NON-NLS-1$
    private static final double MICRO_TO_NANO = 1000.0;

    private final long fTs;
    private final char fPhase;
    private final String fName;
    private ITmfEventField fContent;
    private final @Nullable Map<String, Object> fArgs;
    private final @Nullable Object fTid;
    private final @Nullable String fCategory;
    private final @Nullable String fId;
    private final @Nullable Long fDuration;
    private final @Nullable Object fPid;

    private static final Gson G_SON = new Gson();

    /**
     * Parse a JSON string
     *
     * @param fieldsString
     *            the string
     * @return an event field
     */
    public static @Nullable TraceEventField parseJson(String fieldsString) {
        // looks like this
        // {"ts":94824347413117,"phase":"B","tid":39,"name":"TimeGraphView:BuildThread","args"={"trace":"django-httpd"}}
        JsonObject root;
        Map<@NonNull String, @NonNull Object> argsMap = new HashMap<>();
        root = G_SON.fromJson(fieldsString, JsonObject.class);
        long ts = 0;
        if (root.size() == 0) {
            return null;
        }
        JsonElement timestamp = root.get(ITraceEventConstants.TIMESTAMP);
        if (timestamp == null) {
            return null;
        }
        Double tso = timestamp.getAsDouble();
        if (Double.isFinite(tso)) {
            ts = (long) (tso * MICRO_TO_NANO);
        }
        String phase = optString(root, ITraceEventConstants.PHASE, "I"); //$NON-NLS-1$
        if (phase == null) {
            // FIXME: Easy way to avoid null warning
            phase = "I"; //$NON-NLS-1$
        }
        // We differentiate between the duration exit and the other exits for some reason
        String name = String.valueOf(optString(root, ITraceEventConstants.NAME,  TraceEventPhases.DURATION_END.equals(phase) ? UNKNOWN_DURATION_EXIT_EVENT : UNKNOWN_EXIT_EVENT));
        String tid = optString(root, ITraceEventConstants.TID, null);
        JsonElement jsonElement = root.get(ITraceEventConstants.PID);
        JsonPrimitive primitive = jsonElement == null ? null : jsonElement.isJsonPrimitive() ? jsonElement.getAsJsonPrimitive() : null;
        Object pid = primitive == null ? null : primitive.isNumber() ? primitive.getAsNumber() : primitive.isString() ? primitive.getAsString() : null;
        Double duration = optDouble(root, ITraceEventConstants.DURATION);
        if (Double.isFinite(duration)) {
            duration = (duration * MICRO_TO_NANO);
        }
        String category = optString(root, ITraceEventConstants.CATEGORY);
        String id = optString(root, ITraceEventConstants.ID);
        String scope = optString(root, ITraceEventConstants.SCOPE);
        JsonObject args = optJSONObject(root, ITraceEventConstants.ARGS);
        if (args != null) {
            for (Entry<String, JsonElement> entry : args.entrySet()) {
                String key = Objects.requireNonNull(entry.getKey());
                JsonElement element = Objects.requireNonNull(entry.getValue());
                String value = String.valueOf(element.isJsonPrimitive() ? element.getAsJsonPrimitive().getAsString() : element.toString());
                argsMap.put(ITraceEventConstants.ARGS + "/" + key, value); //$NON-NLS-1$
            }
        }
        argsMap.put(ITraceEventConstants.TIMESTAMP, timestamp.getAsString());
        argsMap.put(ITraceEventConstants.PHASE, phase);
        argsMap.put(ITraceEventConstants.NAME, name);
        if (tid != null) {
            argsMap.put(ITraceEventConstants.TID, tid);
        }
        if (pid != null) {
            argsMap.put(ITraceEventConstants.PID, pid);
        }
        if (Double.isFinite(duration)) {
            argsMap.put(ITraceEventConstants.DURATION, duration);
        }
        if (category != null) {
            argsMap.put(ITraceEventConstants.CATEGORY, category);
        }
        if (id != null) {
            argsMap.put(ITraceEventConstants.ID, id);
        }
        if (scope != null) {
            argsMap.put(ITraceEventConstants.SCOPE, scope);
        }
        return new TraceEventField(name, ts, phase, pid, tid, category, id, duration, argsMap);
    }

    private static double optDouble(JsonObject root, String key) {
        JsonElement jsonElement = root.get(key);
        return jsonElement != null ? jsonElement.getAsDouble() : Double.NaN;
    }

    private static @Nullable JsonObject optJSONObject(JsonObject root, String key) {
        JsonElement jsonElement = root.get(key);
        return jsonElement != null ? jsonElement.getAsJsonObject() : null;
    }

    private static @Nullable String optString(JsonObject root, String key, @Nullable String defaultValue) {
        JsonElement jsonElement = root.get(key);
        return jsonElement != null ? jsonElement.getAsString() : defaultValue;
    }

    private static @Nullable String optString(JsonObject root, String key) {
        return optString(root, key, null);
    }

    /**
     * Constructor
     *
     * @param name
     *            event name
     * @param ts
     *            the timestamp in ns
     * @param phase
     *            the phase of the event
     * @param pid
     *            the process id
     * @param tid
     *            the threadId
     * @param category
     *            the category
     * @param id
     *            the ID of the event stream
     * @param duration
     *            the duration in ns
     * @param fields
     *            event fields (arguments)
     */
    protected TraceEventField(String name, long ts, String phase, @Nullable Object pid, @Nullable Object tid, @Nullable String category, @Nullable String id, @Nullable Double duration, Map<String, Object> fields) {
        fName = name;
        fPid = pid;
        fTid = tid;
        fCategory = category;
        fId = id;
        ITmfEventField[] array = fields.entrySet().stream()
                .map(entry -> new TmfEventField(entry.getKey(), entry.getValue(), null))
                .toArray(ITmfEventField[]::new);
        fContent = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, fields, array);
        fields.put(ITraceEventConstants.TIMESTAMP, ts / MICRO_TO_NANO);
        fTs = ts;
        fDuration = duration == null ? null : Double.isFinite(duration) ? duration.longValue() : null;
        fPhase = phase.charAt(0);
        @SuppressWarnings("null")
        Map<@NonNull String, @NonNull Object> args = fields.entrySet().stream()
                .filter(entry -> {
                    return entry.getKey().startsWith(ITraceEventConstants.ARGS + "/"); //$NON-NLS-1$
                })
                .collect(Collectors.toMap(entry -> entry.getKey().substring(5), Entry::getValue));
        fArgs = args.isEmpty() ? null : args;

    }

    /**
     * Get the event category
     *
     * @return the event category
     */
    public @Nullable String getCategory() {
        return fCategory;
    }

    /**
     * Get the event content
     *
     * @return the event content
     */
    public ITmfEventField getContent() {
        return fContent;
    }

    /**
     * Get the event ID
     *
     * @return the event ID
     */
    public @Nullable String getId() {
        return fId;
    }

    /**
     * Get the name of the event
     *
     * @return the event name
     */
    public String getName() {
        return fName;
    }

    /**
     * Get the phase of the event
     *
     * @return the event phase
     */
    public char getPhase() {
        return fPhase;
    }

    /**
     * Get the TID of the event
     *
     * @return the event TID
     */
    public @Nullable Object getTid() {
        return fTid;
    }

    /**
     * Get the timestamp
     *
     * @return the timestamp in ns
     */
    public long getTs() {
        return fTs;
    }

    /**
     * Get the event duration if applicable
     *
     * @return the duration or null in ns
     */
    @Nullable
    public Long getDuration() {
        return fDuration;
    }

    /**
     * Get pid
     *
     * @return the process ID
     */
    @Nullable
    public Object getPid() {
        return fPid;
    }

    /**
     * Get the arguments passed
     *
     * @return a map of the arguments and their field names
     */
    @Nullable
    public Map<String, Object> getArgs() {
        return fArgs;
    }
}
