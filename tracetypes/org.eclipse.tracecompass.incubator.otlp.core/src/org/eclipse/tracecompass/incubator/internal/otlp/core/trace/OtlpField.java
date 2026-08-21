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
        if (attributes != null) {
            JsonArray tags = new JsonArray();
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
            // Add service.name as a tag
            if (!serviceName.isEmpty()) {
                JsonObject svcTag = new JsonObject();
                svcTag.addProperty(IOpenTracingConstants.KEY, "service.name"); //$NON-NLS-1$
                svcTag.addProperty(IOpenTracingConstants.VALUE, serviceName);
                tags.add(svcTag);
            }
            jaeger.add(IOpenTracingConstants.TAGS, tags);
        }

        // Process ID - use service name
        jaeger.addProperty(IOpenTracingConstants.PROCESS_ID, serviceName);

        return G_SON.toJson(jaeger);
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

    private static @Nullable JsonArray optJSONArray(JsonObject root, String key) {
        JsonElement el = root.get(key);
        return el != null && el.isJsonArray() ? el.getAsJsonArray() : null;
    }
}
