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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.event.IOpenTracingConstants;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.event.OpenTracingField;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Parses OTLP span JSON into an {@link OpenTracingField} so the existing
 * OpenTracing analysis and views can be reused.
 *
 * OTLP span format:
 * <pre>
 * {
 *   "traceId": "...",
 *   "spanId": "...",
 *   "parentSpanId": "...",
 *   "name": "...",
 *   "kind": 2,
 *   "startTimeUnixNano": "...",
 *   "endTimeUnixNano": "...",
 *   "attributes": [{"key": "...", "value": {"stringValue": "..."}}],
 *   "status": {"code": 1},
 *   "serviceName": "..." (injected by sorting job)
 * }
 * </pre>
 *
 * @author Matthew Khouzam
 */
public class OtlpField {

    private static final Gson G_SON = new Gson();

    private OtlpField() {
        // utility class
    }

    /**
     * Parse an OTLP span JSON string into an OpenTracingField
     *
     * @param jsonString
     *            the OTLP span JSON
     * @return an OpenTracingField, or null if parsing fails
     */
    public static @Nullable OpenTracingField parseJson(String jsonString) {
        JsonObject root = G_SON.fromJson(jsonString, JsonObject.class);
        if (root == null) {
            return null;
        }

        String name = optString(root, "name"); //$NON-NLS-1$
        if (name == null) {
            return null;
        }
        String traceId = optString(root, "traceId"); //$NON-NLS-1$
        String spanId = optString(root, "spanId"); //$NON-NLS-1$
        if (traceId == null || spanId == null) {
            return null;
        }

        String startTimeStr = optString(root, "startTimeUnixNano"); //$NON-NLS-1$
        String endTimeStr = optString(root, "endTimeUnixNano"); //$NON-NLS-1$
        if (startTimeStr == null || endTimeStr == null) {
            return null;
        }

        long startTimeNanos = Long.parseLong(startTimeStr);
        long endTimeNanos = Long.parseLong(endTimeStr);
        long durationNanos = endTimeNanos - startTimeNanos;

        String parentSpanId = optString(root, "parentSpanId"); //$NON-NLS-1$
        String serviceName = optString(root, "serviceName"); //$NON-NLS-1$
        if (serviceName == null) {
            serviceName = ""; //$NON-NLS-1$
        }

        // Convert to Jaeger-compatible JSON and delegate to OpenTracingField
        String jaegerJson = toJaegerJson(root, name, traceId, spanId,
                startTimeNanos, durationNanos, parentSpanId, serviceName);

        // Build a process field for OpenTracingField
        String processField = "{\"serviceName\":\"" + serviceName + "\"}"; //$NON-NLS-1$ //$NON-NLS-2$
        return OpenTracingField.parseJson(jaegerJson, processField);
    }

    /**
     * Parse an OTLP span JSON string into a native {@link OtlpSpan} model that
     * preserves all OTLP-specific fields.
     *
     * @param jsonString
     *            the OTLP span JSON
     * @return an OtlpSpan, or null if parsing fails
     */
    public static @Nullable OtlpSpan parseOtlpSpan(String jsonString) {
        JsonObject root = G_SON.fromJson(jsonString, JsonObject.class);
        if (root == null) {
            return null;
        }

        String name = optString(root, "name"); //$NON-NLS-1$
        if (name == null) {
            return null;
        }
        String traceId = optString(root, "traceId"); //$NON-NLS-1$
        String spanId = optString(root, "spanId"); //$NON-NLS-1$
        if (traceId == null || spanId == null) {
            return null;
        }

        String startTimeStr = optString(root, "startTimeUnixNano"); //$NON-NLS-1$
        String endTimeStr = optString(root, "endTimeUnixNano"); //$NON-NLS-1$
        if (startTimeStr == null || endTimeStr == null) {
            return null;
        }

        long startTimeNanos = Long.parseLong(startTimeStr);
        long endTimeNanos = Long.parseLong(endTimeStr);
        long durationNanos = endTimeNanos - startTimeNanos;

        String parentSpanId = optString(root, "parentSpanId"); //$NON-NLS-1$
        String serviceName = optString(root, "serviceName"); //$NON-NLS-1$
        if (serviceName == null) {
            serviceName = ""; //$NON-NLS-1$
        }

        // Parse kind
        int kindValue = 0;
        JsonElement kindEl = root.get("kind"); //$NON-NLS-1$
        if (kindEl != null && !kindEl.isJsonNull()) {
            kindValue = kindEl.getAsInt();
        }

        // Parse status
        OtlpSpanStatus status = parseStatus(root);

        // Parse attributes
        Map<@NonNull String, @NonNull String> attributes = parseAttributes(optJSONArray(root, "attributes")); //$NON-NLS-1$

        // Parse events
        List<@NonNull OtlpSpanEvent> events = parseSpanEvents(optJSONArray(root, "events")); //$NON-NLS-1$

        // Parse links
        List<@NonNull OtlpSpanLink> links = parseSpanLinks(optJSONArray(root, "links")); //$NON-NLS-1$

        // Parse dropped counts
        int droppedAttributesCount = optInt(root, "droppedAttributesCount"); //$NON-NLS-1$
        int droppedEventsCount = optInt(root, "droppedEventsCount"); //$NON-NLS-1$
        int droppedLinksCount = optInt(root, "droppedLinksCount"); //$NON-NLS-1$

        // Parse instrumentation scope (if injected)
        String scopeName = optString(root, "instrumentationScopeName"); //$NON-NLS-1$
        String scopeVersion = optString(root, "instrumentationScopeVersion"); //$NON-NLS-1$

        // Parse resource attributes
        Map<@NonNull String, @NonNull String> resourceAttributes = parseAttributes(optJSONArray(root, "resourceAttributes")); //$NON-NLS-1$

        return new OtlpSpan.Builder()
                .traceId(traceId)
                .spanId(spanId)
                .parentSpanId(parentSpanId)
                .operationName(name)
                .startTimeNanos(startTimeNanos)
                .durationNanos(durationNanos)
                .serviceName(serviceName)
                .kind(OtlpSpanKind.fromValue(kindValue))
                .status(status)
                .attributes(attributes)
                .resourceAttributes(resourceAttributes)
                .events(events)
                .links(links)
                .instrumentationScopeName(scopeName)
                .instrumentationScopeVersion(scopeVersion)
                .droppedAttributesCount(droppedAttributesCount)
                .droppedEventsCount(droppedEventsCount)
                .droppedLinksCount(droppedLinksCount)
                .build();
    }

    /**
     * Convert OTLP span data to Jaeger-compatible JSON string that
     * OpenTracingField.parseJson can handle.
     */
    private static String toJaegerJson(JsonObject otlpRoot,
            String name, String traceId, String spanId, long startTimeNanos,
            long durationNanos, @Nullable String parentSpanId, String serviceName) {
        JsonObject jaeger = new JsonObject();
        jaeger.addProperty(IOpenTracingConstants.OPERATION_NAME, name);
        jaeger.addProperty(IOpenTracingConstants.TRACE_ID, traceId);
        jaeger.addProperty(IOpenTracingConstants.SPAN_ID, spanId);
        // Jaeger uses microseconds
        jaeger.addProperty(IOpenTracingConstants.START_TIME, startTimeNanos / 1000);
        jaeger.addProperty(IOpenTracingConstants.DURATION, durationNanos / 1000);

        // References
        if (parentSpanId != null && !parentSpanId.isEmpty()) {
            JsonArray refs = new JsonArray();
            JsonObject ref = new JsonObject();
            ref.addProperty(IOpenTracingConstants.REFERENCE_TYPE, "CHILD_OF"); //$NON-NLS-1$
            ref.addProperty(IOpenTracingConstants.SPAN_ID, parentSpanId);
            refs.add(ref);
            jaeger.add(IOpenTracingConstants.REFERENCES, refs);
        }

        // Tags from OTLP attributes
        JsonArray attributes = optJSONArray(otlpRoot, "attributes"); //$NON-NLS-1$
        JsonArray tags = new JsonArray();
        if (attributes != null) {
            for (int i = 0; i < attributes.size(); i++) {
                JsonObject attr = attributes.get(i).getAsJsonObject();
                String key = attr.get("key").getAsString(); //$NON-NLS-1$
                JsonObject valueObj = attr.getAsJsonObject("value"); //$NON-NLS-1$
                String value = extractAttributeValue(valueObj);
                JsonObject tag = new JsonObject();
                tag.addProperty(IOpenTracingConstants.KEY, key);
                tag.addProperty(IOpenTracingConstants.VALUE, value);
                tags.add(tag);
            }
        }
        // Add resource attributes as resource.* tags
        JsonArray resourceAttributes = optJSONArray(otlpRoot, "resourceAttributes"); //$NON-NLS-1$
        if (resourceAttributes != null) {
            for (int i = 0; i < resourceAttributes.size(); i++) {
                JsonObject attr = resourceAttributes.get(i).getAsJsonObject();
                String key = attr.get("key").getAsString(); //$NON-NLS-1$
                JsonObject valueObj = attr.getAsJsonObject("value"); //$NON-NLS-1$
                String value = extractAttributeValue(valueObj);
                JsonObject tag = new JsonObject();
                tag.addProperty(IOpenTracingConstants.KEY, "resource." + key); //$NON-NLS-1$
                tag.addProperty(IOpenTracingConstants.VALUE, value);
                tags.add(tag);
            }
        }
        // Add service.name as a tag
        if (!serviceName.isEmpty()) {
            JsonObject svcTag = new JsonObject();
            svcTag.addProperty(IOpenTracingConstants.KEY, "service.name"); //$NON-NLS-1$
            svcTag.addProperty(IOpenTracingConstants.VALUE, serviceName);
            tags.add(svcTag);
        }
        if (tags.size() > 0) {
            jaeger.add(IOpenTracingConstants.TAGS, tags);
        }

        // Process ID - use service name
        jaeger.addProperty(IOpenTracingConstants.PROCESS_ID, serviceName);

        // Map OTLP span events to Jaeger logs
        JsonArray events = optJSONArray(otlpRoot, "events"); //$NON-NLS-1$
        if (events != null && events.size() > 0) {
            JsonArray logs = new JsonArray();
            for (int i = 0; i < events.size(); i++) {
                JsonObject eventObj = events.get(i).getAsJsonObject();
                JsonObject log = new JsonObject();

                // Convert timeUnixNano to microseconds for Jaeger
                long timeNano = 0;
                JsonElement timeEl = eventObj.get("timeUnixNano"); //$NON-NLS-1$
                if (timeEl != null && !timeEl.isJsonNull()) {
                    timeNano = Long.parseLong(timeEl.getAsString());
                }
                log.addProperty(IOpenTracingConstants.TIMESTAMP, timeNano / 1000);

                // Build fields array: first field is always "event" -> name
                JsonArray fields = new JsonArray();
                JsonObject eventField = new JsonObject();
                eventField.addProperty(IOpenTracingConstants.KEY, "event"); //$NON-NLS-1$
                String eventName = ""; //$NON-NLS-1$
                JsonElement nameEl = eventObj.get("name"); //$NON-NLS-1$
                if (nameEl != null && !nameEl.isJsonNull()) {
                    eventName = nameEl.getAsString();
                }
                eventField.addProperty(IOpenTracingConstants.VALUE, eventName);
                fields.add(eventField);

                // Add event attributes as additional fields
                JsonArray eventAttrs = optJSONArray(eventObj, "attributes"); //$NON-NLS-1$
                if (eventAttrs != null) {
                    for (int j = 0; j < eventAttrs.size(); j++) {
                        JsonObject attr = eventAttrs.get(j).getAsJsonObject();
                        String key = attr.get("key").getAsString(); //$NON-NLS-1$
                        JsonObject valueObj = attr.getAsJsonObject("value"); //$NON-NLS-1$
                        String value = extractAttributeValue(valueObj);
                        JsonObject field = new JsonObject();
                        field.addProperty(IOpenTracingConstants.KEY, key);
                        field.addProperty(IOpenTracingConstants.VALUE, value);
                        fields.add(field);
                    }
                }
                log.add(IOpenTracingConstants.FIELDS, fields);
                logs.add(log);
            }
            jaeger.add(IOpenTracingConstants.LOGS, logs);
        }

        return G_SON.toJson(jaeger);
    }

    private static OtlpSpanStatus parseStatus(JsonObject root) {
        JsonElement statusEl = root.get("status"); //$NON-NLS-1$
        if (statusEl == null || !statusEl.isJsonObject()) {
            return new OtlpSpanStatus(OtlpSpanStatus.STATUS_CODE_UNSET, null);
        }
        JsonObject statusObj = statusEl.getAsJsonObject();
        int code = OtlpSpanStatus.STATUS_CODE_UNSET;
        JsonElement codeEl = statusObj.get("code"); //$NON-NLS-1$
        if (codeEl != null && !codeEl.isJsonNull()) {
            code = codeEl.getAsInt();
        }
        String message = null;
        JsonElement msgEl = statusObj.get("message"); //$NON-NLS-1$
        if (msgEl != null && !msgEl.isJsonNull()) {
            message = msgEl.getAsString();
        }
        return new OtlpSpanStatus(code, message);
    }

    private static @NonNull Map<@NonNull String, @NonNull String> parseAttributes(@Nullable JsonArray attributes) {
        if (attributes == null || attributes.size() == 0) {
            return new HashMap<>();
        }
        Map<@NonNull String, @NonNull String> result = new HashMap<>();
        for (int i = 0; i < attributes.size(); i++) {
            JsonObject attr = attributes.get(i).getAsJsonObject();
            JsonElement keyEl = attr.get("key"); //$NON-NLS-1$
            if (keyEl == null) {
                continue;
            }
            String key = keyEl.getAsString();
            JsonObject valueObj = attr.getAsJsonObject("value"); //$NON-NLS-1$
            String value = extractAttributeValue(valueObj);
            result.put(key, value);
        }
        return result;
    }

    private static @NonNull List<@NonNull OtlpSpanEvent> parseSpanEvents(@Nullable JsonArray events) {
        if (events == null || events.size() == 0) {
            return new ArrayList<>();
        }
        List<@NonNull OtlpSpanEvent> result = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            JsonObject eventObj = events.get(i).getAsJsonObject();
            String eventName = ""; //$NON-NLS-1$
            JsonElement nameEl = eventObj.get("name"); //$NON-NLS-1$
            if (nameEl != null && !nameEl.isJsonNull()) {
                eventName = nameEl.getAsString();
            }
            long timeUnixNano = 0;
            JsonElement timeEl = eventObj.get("timeUnixNano"); //$NON-NLS-1$
            if (timeEl != null && !timeEl.isJsonNull()) {
                timeUnixNano = Long.parseLong(timeEl.getAsString());
            }
            Map<@NonNull String, @NonNull String> attrs = parseAttributes(optJSONArray(eventObj, "attributes")); //$NON-NLS-1$
            result.add(new OtlpSpanEvent(eventName, timeUnixNano, attrs));
        }
        return result;
    }

    private static @NonNull List<@NonNull OtlpSpanLink> parseSpanLinks(@Nullable JsonArray links) {
        if (links == null || links.size() == 0) {
            return new ArrayList<>();
        }
        List<@NonNull OtlpSpanLink> result = new ArrayList<>();
        for (int i = 0; i < links.size(); i++) {
            JsonObject linkObj = links.get(i).getAsJsonObject();
            String linkTraceId = ""; //$NON-NLS-1$
            JsonElement traceIdEl = linkObj.get("traceId"); //$NON-NLS-1$
            if (traceIdEl != null && !traceIdEl.isJsonNull()) {
                linkTraceId = traceIdEl.getAsString();
            }
            String linkSpanId = ""; //$NON-NLS-1$
            JsonElement spanIdEl = linkObj.get("spanId"); //$NON-NLS-1$
            if (spanIdEl != null && !spanIdEl.isJsonNull()) {
                linkSpanId = spanIdEl.getAsString();
            }
            String traceState = null;
            JsonElement stateEl = linkObj.get("traceState"); //$NON-NLS-1$
            if (stateEl != null && !stateEl.isJsonNull()) {
                traceState = stateEl.getAsString();
            }
            Map<@NonNull String, @NonNull String> attrs = parseAttributes(optJSONArray(linkObj, "attributes")); //$NON-NLS-1$
            result.add(new OtlpSpanLink(linkTraceId, linkSpanId, attrs, traceState));
        }
        return result;
    }

    private static String extractAttributeValue(JsonObject valueObj) {
        if (valueObj == null) {
            return ""; //$NON-NLS-1$
        }
        if (valueObj.has("stringValue")) { //$NON-NLS-1$
            return valueObj.get("stringValue").getAsString(); //$NON-NLS-1$
        }
        if (valueObj.has("intValue")) { //$NON-NLS-1$
            return valueObj.get("intValue").getAsString(); //$NON-NLS-1$
        }
        if (valueObj.has("boolValue")) { //$NON-NLS-1$
            return valueObj.get("boolValue").getAsString(); //$NON-NLS-1$
        }
        if (valueObj.has("doubleValue")) { //$NON-NLS-1$
            return valueObj.get("doubleValue").getAsString(); //$NON-NLS-1$
        }
        if (valueObj.has("arrayValue")) { //$NON-NLS-1$
            return valueObj.get("arrayValue").toString(); //$NON-NLS-1$
        }
        return valueObj.toString();
    }

    private static @Nullable String optString(JsonObject root, String key) {
        JsonElement el = root.get(key);
        return el != null && !el.isJsonNull() ? el.getAsString() : null;
    }

    private static int optInt(JsonObject root, String key) {
        JsonElement el = root.get(key);
        return el != null && !el.isJsonNull() ? el.getAsInt() : 0;
    }

    private static @Nullable JsonArray optJSONArray(JsonObject root, String key) {
        JsonElement el = root.get(key);
        return el != null && el.isJsonArray() ? el.getAsJsonArray() : null;
    }
}
