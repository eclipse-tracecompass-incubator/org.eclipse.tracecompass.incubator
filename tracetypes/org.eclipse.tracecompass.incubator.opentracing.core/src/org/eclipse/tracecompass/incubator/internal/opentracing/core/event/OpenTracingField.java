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
    private final @Nullable Map<String, Object> fProcessTags;
    private String fProcessName;

    private static final Gson G_SON = new Gson();

    /**
     * Get the process id
     *
     * @param eventString
     *            the event string
     * @return the process id
     */
    public static @Nullable String getProcess(String eventString) {
        @Nullable
        JsonObject root = G_SON.fromJson(eventString, JsonObject.class);
        String process = optString(root, IOpenTracingConstants.PROCESS_ID);
        return process == null ? "" : process; //$NON-NLS-1$
    }

    /**
     * Parse a JSON string
     *
     * @param fieldsString
     *            the string
     * @param processField
     *            the process name and tags
     * @return an event field
     */
    public static @Nullable OpenTracingField parseJson(String fieldsString, @Nullable String processField) {
        @Nullable
        JsonObject root = G_SON.fromJson(fieldsString, JsonObject.class);

        String name = String.valueOf(optString(root, IOpenTracingConstants.OPERATION_NAME));
        if (name == "null") { //$NON-NLS-1$
            return null;
        }
        String traceId = optString(root, IOpenTracingConstants.TRACE_ID);
        String spanId = optString(root, IOpenTracingConstants.SPAN_ID);
        Integer flags = optInt(root, IOpenTracingConstants.FLAGS);
        Long startTime = optLong(root, IOpenTracingConstants.START_TIME);
        if (Double.isFinite(startTime)) {
            startTime = TmfTimestamp.fromMicros(startTime).toNanos();
        }
        Long duration = optLong(root, IOpenTracingConstants.DURATION);
        if (Double.isFinite(duration)) {
            duration = TmfTimestamp.fromMicros(duration).toNanos();
        }

        Map<@NonNull String, @NonNull Object> fieldsMap = new HashMap<>();

        JsonArray refs = optJSONArray(root, IOpenTracingConstants.REFERENCES);
        if (refs != null) {
            for (int i = 0; i < refs.size(); i++) {
                String key = Objects.requireNonNull(refs.get(i).getAsJsonObject().get(IOpenTracingConstants.REFERENCE_TYPE).getAsString());
                JsonElement element = Objects.requireNonNull(refs.get(i).getAsJsonObject().get(IOpenTracingConstants.SPAN_ID));
                String value = String.valueOf(element.isJsonPrimitive() ? element.getAsJsonPrimitive().getAsString() : element.toString());
                fieldsMap.put(IOpenTracingConstants.REFERENCES + '/' + key, value);
            }
        }

        JsonArray tags = optJSONArray(root, IOpenTracingConstants.TAGS);
        if (tags != null) {
            for (int i = 0; i < tags.size(); i++) {
                String key = Objects.requireNonNull(tags.get(i).getAsJsonObject().get(IOpenTracingConstants.KEY).getAsString());
                JsonElement element = Objects.requireNonNull(tags.get(i).getAsJsonObject().get(IOpenTracingConstants.VALUE));
                String value = String.valueOf(element.isJsonPrimitive() ? element.getAsJsonPrimitive().getAsString() : element.toString());
                fieldsMap.put(IOpenTracingConstants.TAGS + '/' + key, value);
            }
        }

        JsonArray logs = optJSONArray(root, IOpenTracingConstants.LOGS);
        if (logs != null) {
            Map<Long, Map<String, String>> timestampList = new HashMap();
            for (int i = 0; i < logs.size(); i++) {
                Long timestamp = optLong(logs.get(i).getAsJsonObject(), IOpenTracingConstants.TIMESTAMP);
                if (Double.isFinite(timestamp)) {
                    timestamp = TmfTimestamp.fromMicros(timestamp).toNanos();
                }
                JsonArray fields = Objects.requireNonNull(logs.get(i).getAsJsonObject().get(IOpenTracingConstants.FIELDS).getAsJsonArray());
                Map<String, String> fieldsList = new HashMap();
                for (int j = 0; j < fields.size(); j++) {
                    String key = Objects.requireNonNull(fields.get(j).getAsJsonObject().get(IOpenTracingConstants.KEY).getAsString());
                    JsonElement element = Objects.requireNonNull(fields.get(j).getAsJsonObject().get(IOpenTracingConstants.VALUE));
                    String value = String.valueOf(element.isJsonPrimitive() ? element.getAsJsonPrimitive().getAsString() : element.toString());
                    fieldsList.put(key, value);
                }
                timestampList.put(timestamp.longValue(), fieldsList);
            }
            fieldsMap.put(IOpenTracingConstants.LOGS, timestampList);
        }

        if (traceId == null || spanId == null) {
            return null;
        }

        fieldsMap.put(IOpenTracingConstants.OPERATION_NAME, name);
        fieldsMap.put(IOpenTracingConstants.TRACE_ID, traceId);
        fieldsMap.put(IOpenTracingConstants.SPAN_ID, spanId);
        if (flags != Integer.MIN_VALUE) {
            fieldsMap.put(IOpenTracingConstants.FLAGS, flags);
        }
        fieldsMap.put(IOpenTracingConstants.START_TIME, startTime);
        fieldsMap.put(IOpenTracingConstants.DURATION, duration);

        String processName = processField == null ? "" : parseProcess(processField, fieldsMap); //$NON-NLS-1$
        fieldsMap.put(IOpenTracingConstants.PROCESS_NAME, processName);

        return new OpenTracingField(name, fieldsMap, spanId, startTime, duration, processName);
    }

    /**
     * Parse a JSON string of the process and add it in fieldsMap
     *
     * @param processField
     *            the string
     * @param fieldsMap
     *            processes list
     * @return the process name
     */
    public static String parseProcess(String processField, Map<String, Object> fieldsMap) {
        @Nullable
        JsonObject root = G_SON.fromJson(processField, JsonObject.class);

        String name = String.valueOf(optString(root, IOpenTracingConstants.SERVICE_NAME));
        if (name == "null") { //$NON-NLS-1$
            return ""; //$NON-NLS-1$
        }

        JsonArray tags = optJSONArray(root, IOpenTracingConstants.TAGS);
        if (tags != null) {
            for (int i = 0; i < tags.size(); i++) {
                String key = Objects.requireNonNull(tags.get(i).getAsJsonObject().get("key").getAsString()); //$NON-NLS-1$
                JsonElement element = Objects.requireNonNull(tags.get(i).getAsJsonObject().get("value")); //$NON-NLS-1$
                String value = String.valueOf(element.isJsonPrimitive() ? element.getAsJsonPrimitive().getAsString() : element.toString());
                fieldsMap.put(IOpenTracingConstants.PROCESS_TAGS + '/' + key, value);
            }
        }

        return name;

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
     * @param processName
     *            the span process name
     */
    private OpenTracingField(String name, Map<String, Object> fields, String spanId, Long startTime, Long duration, String processName) {
        fOperationName = name;
        ITmfEventField[] array = fields.entrySet().stream()
                .map(entry -> new TmfEventField(entry.getKey(), entry.getValue(), null))
                .toArray(ITmfEventField[]::new);
        fContent = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, fields, array);
        fSpanId = spanId;
        fStartTime = startTime;
        fDuration = duration;
        @SuppressWarnings("null")
        Map<@NonNull String, @NonNull Object> tags = fields.entrySet().stream()
                .filter(entry -> {
                    return entry.getKey().startsWith(IOpenTracingConstants.TAGS + '/');
                })
                .collect(Collectors.toMap(entry -> entry.getKey().substring(5), Entry::getValue));
        fTags = tags.isEmpty() ? null : tags;
        @SuppressWarnings("null")
        Map<@NonNull String, @NonNull Object> processTags = fields.entrySet().stream()
                .filter(entry -> {
                    return entry.getKey().startsWith(IOpenTracingConstants.PROCESS_TAGS + '/');
                })
                .collect(Collectors.toMap(entry -> entry.getKey().substring(12), Entry::getValue));
        fProcessTags = processTags.isEmpty() ? null : processTags;
        fProcessName = processName;
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
     * @return the start time
     */
    public Long getStartTime() {
        return fStartTime;
    }

    /**
     * Get the event duration
     *
     * @return the duration
     */
    public Long getDuration() {
        return fDuration;
    }

    /**
     * Get the span tags
     *
     * @return a map of the tags and their field names
     */
    public @Nullable Map<String, Object> getTags() {
        return fTags;
    }

    /**
     * Get the span process tags
     *
     * @return a map of the process tags and their field names
     */
    @Nullable
    public Map<String, Object> getProcessTags() {
        return fProcessTags;
    }

    /**
     * Get the span processName
     *
     * @return the process name
     */
    public String getProcessName() {
        return fProcessName;
    }
}
