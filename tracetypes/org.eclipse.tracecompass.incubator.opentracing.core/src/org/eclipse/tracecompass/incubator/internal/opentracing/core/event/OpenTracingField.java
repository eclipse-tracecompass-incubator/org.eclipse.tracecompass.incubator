/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.core.event;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Open Tracing fields. Used as a quick wrapper for Open Tracing log data.
 *
 * @author Katherine Nadeau
 */
public class OpenTracingField {

    private final String fOperationName;
    private final ITmfEventField fContent;
    private final String fSpanId;
    private final Long fStartTime;
    private final Long fDuration;
    private final @Nullable Map<String, Object> fTags;
    private final String fPid;

    private static final Gson G_SON = new Gson();

    /**
     * Parse a JSON string
     *
     * @param fieldsString
     *            the string
     * @return an event field
     */
    public static @Nullable OpenTracingField parseJson(String fieldsString) {
        @Nullable
        JsonObject root = G_SON.fromJson(fieldsString, JsonObject.class);

        String name = String.valueOf(optString(root, IOpenTracingConstants.OPERATION_NAME));
        if (name == "null") { //$NON-NLS-1$
            return null;
        }
        String id = optString(root, IOpenTracingConstants.SPAN_ID);
        Integer flags = optInt(root, IOpenTracingConstants.FLAGS);
        Long startTime = optLong(root, IOpenTracingConstants.START_TIME);
        if (Double.isFinite(startTime)) {
            startTime = TmfTimestamp.fromMicros(startTime).toNanos();
        }
        Long duration = optLong(root, IOpenTracingConstants.DURATION);
        if (Double.isFinite(duration)) {
            duration = TmfTimestamp.fromMicros(duration).toNanos();
        }
        String pid = optString(root, IOpenTracingConstants.PROCESS_ID);

        Map<@NonNull String, @NonNull Object> fieldsMap = new HashMap<>();

        JsonArray refs = optJSONArray(root, IOpenTracingConstants.REFERENCES);
        if (refs != null) {
            for (int i = 0; i < refs.size(); i++) {
                String key = Objects.requireNonNull(refs.get(i).getAsJsonObject().get("refType").getAsString()); //$NON-NLS-1$
                JsonElement element = Objects.requireNonNull(refs.get(i).getAsJsonObject().get("spanID")); //$NON-NLS-1$
                String value = String.valueOf(element.isJsonPrimitive() ? element.getAsJsonPrimitive().getAsString() : element.toString());
                fieldsMap.put(IOpenTracingConstants.REFERENCES + "/" + key, value); //$NON-NLS-1$
            }
        }

        JsonArray tags = optJSONArray(root, IOpenTracingConstants.TAGS);
        if (tags != null) {
            for (int i = 0; i < tags.size(); i++) {
                String key = Objects.requireNonNull(tags.get(i).getAsJsonObject().get("key").getAsString()); //$NON-NLS-1$
                JsonElement element = Objects.requireNonNull(tags.get(i).getAsJsonObject().get("value")); //$NON-NLS-1$
                String value = String.valueOf(element.isJsonPrimitive() ? element.getAsJsonPrimitive().getAsString() : element.toString());
                fieldsMap.put(IOpenTracingConstants.TAGS + "/" + key, value); //$NON-NLS-1$
            }
        }

        if (id == null || pid == null) {
            return null;
        }
        fieldsMap.put(IOpenTracingConstants.OPERATION_NAME, name);
        fieldsMap.put(IOpenTracingConstants.SPAN_ID, id);
        if (flags != Integer.MIN_VALUE) {
            fieldsMap.put(IOpenTracingConstants.FLAGS, flags);
        }
        fieldsMap.put(IOpenTracingConstants.START_TIME, startTime);
        fieldsMap.put(IOpenTracingConstants.DURATION, duration);
        fieldsMap.put(IOpenTracingConstants.PROCESS_ID, pid);

        return new OpenTracingField(name, fieldsMap, id, startTime, duration, pid);
    }

    private static long optLong(JsonObject root, String key) {
        JsonElement jsonElement = root.get(key);
        return jsonElement != null ? jsonElement.getAsLong() : Long.MIN_VALUE;
    }

    private static int optInt(JsonObject root, String key) {
        JsonElement jsonElement = root.get(key);
        return jsonElement != null ? jsonElement.getAsInt() : Integer.MIN_VALUE;
    }

    private static @Nullable JsonArray optJSONArray(JsonObject root, String key) {
        JsonElement jsonElement = root.get(key);
        return jsonElement != null ? jsonElement.getAsJsonArray() : null;
    }

    private static @Nullable String optString(JsonObject root, String key) {
        JsonElement jsonElement = root.get(key);
        return jsonElement != null ? jsonElement.getAsString() : null;
    }

    /**
     * Constructor
     *
     * @param name
     *            operation name
     * @param fields
     *            span fields (arguments)
     * @param spanId
     *            the span id
     * @param startTime
     *            the span start time
     * @param duration
     *            the span duration
     * @param pid
     *            the span process id
     */
    private OpenTracingField(String name, Map<String, Object> fields, String spanId, Long startTime, Long duration, String pid) {
        fOperationName = name;
        ITmfEventField[] array = fields.entrySet().stream()
                .map(entry -> new TmfEventField(entry.getKey(), entry.getValue(), null))
                .toArray(ITmfEventField[]::new);
        fContent = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, fields, array);
        fSpanId = spanId;
        fStartTime = startTime;
        fDuration = duration;
        @SuppressWarnings("null")
        Map<@NonNull String, @NonNull Object> tgs = fields.entrySet().stream()
                .filter(entry -> {
                    return entry.getKey().startsWith(IOpenTracingConstants.TAGS + '/');
                })
                .collect(Collectors.toMap(entry -> entry.getKey().substring(5), Entry::getValue));
        fTags = tgs.isEmpty() ? null : tgs;
        fPid = pid;
    }

    /**
     * Get the operation name
     *
     * @return the operation name
     */
    public String getName() {
        return fOperationName;
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
     * Get the span id
     *
     * @return the span id
     */
    public String getSpanId() {
        return fSpanId;
    }

    /**
     * Get the span start time
     *
     * @return the start time or null
     */
    public Long getStartTime() {
        return fStartTime;
    }

    /**
     * Get the event duration if applicable
     *
     * @return the duration or null
     */
    public Long getDuration() {
        return fDuration;
    }

    /**
     * Get the span tags
     *
     * @return a map of the tags and their field names
     */
    @Nullable
    public Map<String, Object> getTags() {
        return fTags;
    }

    /**
     * Get the span processId
     *
     * @return the process ID
     */
    public String getPid() {
        return fPid;
    }
}
