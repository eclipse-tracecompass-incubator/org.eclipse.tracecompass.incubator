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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

/**
 * Low-level parser for OTLP protobuf binary trace files
 * (ExportTraceServiceRequest). Parses the wire format directly using
 * {@link CodedInputStream}, without generated proto stubs.
 *
 * The parser produces a list of {@link JsonObject} spans in the same flat
 * format as the JSON sorting job, so they can feed directly into
 * {@link OtlpField#parseJson(String)}.
 *
 * @author Matthew Khouzam
 */
public class OtlpProtobufParser {

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray(); //$NON-NLS-1$

    private OtlpProtobufParser() {
        // utility class
    }

    /**
     * Parse an OTLP protobuf binary file and return all spans as JsonObjects.
     *
     * @param path
     *            path to the .pb file
     * @return list of span JsonObjects in flat OTLP JSON format
     * @throws IOException
     *             if reading fails
     */
    public static @NonNull List<@NonNull JsonObject> parse(String path) throws IOException {
        List<@NonNull JsonObject> allSpans = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(path)) {
            CodedInputStream cis = CodedInputStream.newInstance(fis);
            cis.setSizeLimit(Integer.MAX_VALUE);
            parseExportTraceServiceRequest(cis, allSpans);
        }
        return allSpans;
    }

    /**
     * Parse ExportTraceServiceRequest: repeated ResourceSpans resource_spans =
     * 1
     */
    private static void parseExportTraceServiceRequest(CodedInputStream cis, List<@NonNull JsonObject> allSpans) throws IOException {
        while (!cis.isAtEnd()) {
            int tag = cis.readTag();
            int fieldNumber = WireFormat.getTagFieldNumber(tag);
            int wireType = WireFormat.getTagWireType(tag);
            if (fieldNumber == 1 && wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                int length = cis.readRawVarint32();
                int oldLimit = cis.pushLimit(length);
                parseResourceSpans(cis, allSpans);
                cis.popLimit(oldLimit);
            } else {
                skipField(cis, wireType);
            }
        }
    }

    /**
     * Parse ResourceSpans message
     */
    private static void parseResourceSpans(CodedInputStream cis, List<@NonNull JsonObject> allSpans) throws IOException {
        JsonArray resourceAttributes = new JsonArray();
        String serviceName = ""; //$NON-NLS-1$
        List<@NonNull JsonObject> scopeSpansList = new ArrayList<>();

        while (!cis.isAtEnd()) {
            int tag = cis.readTag();
            int fieldNumber = WireFormat.getTagFieldNumber(tag);
            int wireType = WireFormat.getTagWireType(tag);
            switch (fieldNumber) {
            case 1: // Resource resource = 1
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    int length = cis.readRawVarint32();
                    int oldLimit = cis.pushLimit(length);
                    parseResource(cis, resourceAttributes);
                    cis.popLimit(oldLimit);
                    // Extract service.name from resource attributes
                    serviceName = extractServiceName(resourceAttributes);
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 2: // repeated ScopeSpans scope_spans = 2
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    int length = cis.readRawVarint32();
                    int oldLimit = cis.pushLimit(length);
                    parseScopeSpans(cis, scopeSpansList);
                    cis.popLimit(oldLimit);
                } else {
                    skipField(cis, wireType);
                }
                break;
            default:
                skipField(cis, wireType);
                break;
            }
        }

        // Inject resource info into each span
        for (JsonObject span : scopeSpansList) {
            span.addProperty("serviceName", serviceName); //$NON-NLS-1$
            if (resourceAttributes.size() > 0) {
                span.add("resourceAttributes", resourceAttributes.deepCopy()); //$NON-NLS-1$
            }
            allSpans.add(span);
        }
    }

    /**
     * Parse Resource message: repeated KeyValue attributes = 1
     */
    private static void parseResource(CodedInputStream cis, JsonArray resourceAttributes) throws IOException {
        while (!cis.isAtEnd()) {
            int tag = cis.readTag();
            int fieldNumber = WireFormat.getTagFieldNumber(tag);
            int wireType = WireFormat.getTagWireType(tag);
            if (fieldNumber == 1 && wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                int length = cis.readRawVarint32();
                int oldLimit = cis.pushLimit(length);
                JsonObject kv = parseKeyValue(cis);
                cis.popLimit(oldLimit);
                if (kv != null) {
                    resourceAttributes.add(kv);
                }
            } else {
                skipField(cis, wireType);
            }
        }
    }

    /**
     * Parse ScopeSpans message
     */
    private static void parseScopeSpans(CodedInputStream cis, List<@NonNull JsonObject> spans) throws IOException {
        String scopeName = null;
        String scopeVersion = null;

        // We need to buffer spans because scope info comes in field 1,
        // but spans are in field 2
        List<@NonNull JsonObject> localSpans = new ArrayList<>();

        while (!cis.isAtEnd()) {
            int tag = cis.readTag();
            int fieldNumber = WireFormat.getTagFieldNumber(tag);
            int wireType = WireFormat.getTagWireType(tag);
            switch (fieldNumber) {
            case 1: // InstrumentationScope scope = 1
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    int length = cis.readRawVarint32();
                    int oldLimit = cis.pushLimit(length);
                    String[] scopeInfo = parseInstrumentationScope(cis);
                    cis.popLimit(oldLimit);
                    scopeName = scopeInfo[0];
                    scopeVersion = scopeInfo[1];
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 2: // repeated Span spans = 2
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    int length = cis.readRawVarint32();
                    int oldLimit = cis.pushLimit(length);
                    JsonObject span = parseSpan(cis);
                    cis.popLimit(oldLimit);
                    localSpans.add(span);
                } else {
                    skipField(cis, wireType);
                }
                break;
            default:
                skipField(cis, wireType);
                break;
            }
        }

        // Inject scope info into spans
        for (JsonObject span : localSpans) {
            if (scopeName != null) {
                span.addProperty("instrumentationScopeName", scopeName); //$NON-NLS-1$
            }
            if (scopeVersion != null) {
                span.addProperty("instrumentationScopeVersion", scopeVersion); //$NON-NLS-1$
            }
            spans.add(span);
        }
    }

    /**
     * Parse InstrumentationScope: name=1, version=2
     *
     * @return [name, version]
     */
    private static String[] parseInstrumentationScope(CodedInputStream cis) throws IOException {
        String name = null;
        String version = null;
        while (!cis.isAtEnd()) {
            int tag = cis.readTag();
            int fieldNumber = WireFormat.getTagFieldNumber(tag);
            int wireType = WireFormat.getTagWireType(tag);
            switch (fieldNumber) {
            case 1: // string name = 1
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    name = cis.readStringRequireUtf8();
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 2: // string version = 2
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    version = cis.readStringRequireUtf8();
                } else {
                    skipField(cis, wireType);
                }
                break;
            default:
                skipField(cis, wireType);
                break;
            }
        }
        return new String[] { name, version };
    }

    /**
     * Parse a Span message into a flat JsonObject
     */
    private static @NonNull JsonObject parseSpan(CodedInputStream cis) throws IOException {
        JsonObject span = new JsonObject();
        JsonArray attributes = new JsonArray();
        JsonArray events = new JsonArray();
        JsonArray links = new JsonArray();

        while (!cis.isAtEnd()) {
            int tag = cis.readTag();
            int fieldNumber = WireFormat.getTagFieldNumber(tag);
            int wireType = WireFormat.getTagWireType(tag);
            switch (fieldNumber) {
            case 1: // bytes trace_id = 1
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    ByteString bs = cis.readBytes();
                    span.addProperty("traceId", bytesToHex(bs.toByteArray())); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 2: // bytes span_id = 2
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    ByteString bs = cis.readBytes();
                    span.addProperty("spanId", bytesToHex(bs.toByteArray())); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 3: // string trace_state = 3
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    span.addProperty("traceState", cis.readStringRequireUtf8()); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 4: // bytes parent_span_id = 4
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    ByteString bs = cis.readBytes();
                    byte[] bytes = bs.toByteArray();
                    if (bytes.length > 0) {
                        span.addProperty("parentSpanId", bytesToHex(bytes)); //$NON-NLS-1$
                    }
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 5: // string name = 5
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    span.addProperty("name", cis.readStringRequireUtf8()); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 6: // SpanKind kind = 6 (enum/varint)
                if (wireType == WireFormat.WIRETYPE_VARINT) {
                    span.addProperty("kind", cis.readEnum()); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 7: // fixed64 start_time_unix_nano = 7
                if (wireType == WireFormat.WIRETYPE_FIXED64) {
                    long startTime = cis.readFixed64();
                    span.addProperty("startTimeUnixNano", String.valueOf(startTime)); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 8: // fixed64 end_time_unix_nano = 8
                if (wireType == WireFormat.WIRETYPE_FIXED64) {
                    long endTime = cis.readFixed64();
                    span.addProperty("endTimeUnixNano", String.valueOf(endTime)); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 9: // repeated KeyValue attributes = 9
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    int length = cis.readRawVarint32();
                    int oldLimit = cis.pushLimit(length);
                    JsonObject kv = parseKeyValue(cis);
                    cis.popLimit(oldLimit);
                    if (kv != null) {
                        attributes.add(kv);
                    }
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 10: // uint32 dropped_attributes_count = 10
                if (wireType == WireFormat.WIRETYPE_VARINT) {
                    span.addProperty("droppedAttributesCount", cis.readUInt32()); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 11: // repeated Event events = 11
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    int length = cis.readRawVarint32();
                    int oldLimit = cis.pushLimit(length);
                    JsonObject event = parseEvent(cis);
                    cis.popLimit(oldLimit);
                    events.add(event);
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 12: // uint32 dropped_events_count = 12
                if (wireType == WireFormat.WIRETYPE_VARINT) {
                    span.addProperty("droppedEventsCount", cis.readUInt32()); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 13: // repeated Link links = 13
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    int length = cis.readRawVarint32();
                    int oldLimit = cis.pushLimit(length);
                    JsonObject link = parseLink(cis);
                    cis.popLimit(oldLimit);
                    links.add(link);
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 14: // uint32 dropped_links_count = 14
                if (wireType == WireFormat.WIRETYPE_VARINT) {
                    span.addProperty("droppedLinksCount", cis.readUInt32()); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 15: // Status status = 15
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    int length = cis.readRawVarint32();
                    int oldLimit = cis.pushLimit(length);
                    JsonObject status = parseStatus(cis);
                    cis.popLimit(oldLimit);
                    span.add("status", status); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            default:
                skipField(cis, wireType);
                break;
            }
        }

        if (attributes.size() > 0) {
            span.add("attributes", attributes); //$NON-NLS-1$
        }
        if (events.size() > 0) {
            span.add("events", events); //$NON-NLS-1$
        }
        if (links.size() > 0) {
            span.add("links", links); //$NON-NLS-1$
        }
        return span;
    }

    /**
     * Parse Event message
     */
    private static @NonNull JsonObject parseEvent(CodedInputStream cis) throws IOException {
        JsonObject event = new JsonObject();
        JsonArray attributes = new JsonArray();

        while (!cis.isAtEnd()) {
            int tag = cis.readTag();
            int fieldNumber = WireFormat.getTagFieldNumber(tag);
            int wireType = WireFormat.getTagWireType(tag);
            switch (fieldNumber) {
            case 1: // fixed64 time_unix_nano = 1
                if (wireType == WireFormat.WIRETYPE_FIXED64) {
                    long timeNano = cis.readFixed64();
                    event.addProperty("timeUnixNano", String.valueOf(timeNano)); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 2: // string name = 2
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    event.addProperty("name", cis.readStringRequireUtf8()); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 3: // repeated KeyValue attributes = 3
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    int length = cis.readRawVarint32();
                    int oldLimit = cis.pushLimit(length);
                    JsonObject kv = parseKeyValue(cis);
                    cis.popLimit(oldLimit);
                    if (kv != null) {
                        attributes.add(kv);
                    }
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 4: // uint32 dropped_attributes_count = 4
                if (wireType == WireFormat.WIRETYPE_VARINT) {
                    event.addProperty("droppedAttributesCount", cis.readUInt32()); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            default:
                skipField(cis, wireType);
                break;
            }
        }

        if (attributes.size() > 0) {
            event.add("attributes", attributes); //$NON-NLS-1$
        }
        return event;
    }

    /**
     * Parse Link message
     */
    private static @NonNull JsonObject parseLink(CodedInputStream cis) throws IOException {
        JsonObject link = new JsonObject();
        JsonArray attributes = new JsonArray();

        while (!cis.isAtEnd()) {
            int tag = cis.readTag();
            int fieldNumber = WireFormat.getTagFieldNumber(tag);
            int wireType = WireFormat.getTagWireType(tag);
            switch (fieldNumber) {
            case 1: // bytes trace_id = 1
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    ByteString bs = cis.readBytes();
                    link.addProperty("traceId", bytesToHex(bs.toByteArray())); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 2: // bytes span_id = 2
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    ByteString bs = cis.readBytes();
                    link.addProperty("spanId", bytesToHex(bs.toByteArray())); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 3: // string trace_state = 3
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    link.addProperty("traceState", cis.readStringRequireUtf8()); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 4: // repeated KeyValue attributes = 4
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    int length = cis.readRawVarint32();
                    int oldLimit = cis.pushLimit(length);
                    JsonObject kv = parseKeyValue(cis);
                    cis.popLimit(oldLimit);
                    if (kv != null) {
                        attributes.add(kv);
                    }
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 5: // uint32 dropped_attributes_count = 5
                if (wireType == WireFormat.WIRETYPE_VARINT) {
                    link.addProperty("droppedAttributesCount", cis.readUInt32()); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            default:
                skipField(cis, wireType);
                break;
            }
        }

        if (attributes.size() > 0) {
            link.add("attributes", attributes); //$NON-NLS-1$
        }
        return link;
    }

    /**
     * Parse Status message: message=2, code=3
     */
    private static @NonNull JsonObject parseStatus(CodedInputStream cis) throws IOException {
        JsonObject status = new JsonObject();
        while (!cis.isAtEnd()) {
            int tag = cis.readTag();
            int fieldNumber = WireFormat.getTagFieldNumber(tag);
            int wireType = WireFormat.getTagWireType(tag);
            switch (fieldNumber) {
            case 2: // string message = 2
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    status.addProperty("message", cis.readStringRequireUtf8()); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 3: // StatusCode code = 3 (enum/varint)
                if (wireType == WireFormat.WIRETYPE_VARINT) {
                    status.addProperty("code", cis.readEnum()); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            default:
                skipField(cis, wireType);
                break;
            }
        }
        return status;
    }

    /**
     * Parse KeyValue message: key=1 (string), value=2 (AnyValue)
     */
    private static JsonObject parseKeyValue(CodedInputStream cis) throws IOException {
        String key = null;
        JsonObject value = null;

        while (!cis.isAtEnd()) {
            int tag = cis.readTag();
            int fieldNumber = WireFormat.getTagFieldNumber(tag);
            int wireType = WireFormat.getTagWireType(tag);
            switch (fieldNumber) {
            case 1: // string key = 1
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    key = cis.readStringRequireUtf8();
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 2: // AnyValue value = 2
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    int length = cis.readRawVarint32();
                    int oldLimit = cis.pushLimit(length);
                    value = parseAnyValue(cis);
                    cis.popLimit(oldLimit);
                } else {
                    skipField(cis, wireType);
                }
                break;
            default:
                skipField(cis, wireType);
                break;
            }
        }

        if (key == null) {
            return null;
        }
        JsonObject kv = new JsonObject();
        kv.addProperty("key", key); //$NON-NLS-1$
        if (value != null) {
            kv.add("value", value); //$NON-NLS-1$
        }
        return kv;
    }

    /**
     * Parse AnyValue (oneof): string_value=1, bool_value=2, int_value=3,
     * double_value=4, array_value=5, kvlist_value=6, bytes_value=7
     */
    private static @NonNull JsonObject parseAnyValue(CodedInputStream cis) throws IOException {
        JsonObject anyValue = new JsonObject();
        while (!cis.isAtEnd()) {
            int tag = cis.readTag();
            int fieldNumber = WireFormat.getTagFieldNumber(tag);
            int wireType = WireFormat.getTagWireType(tag);
            switch (fieldNumber) {
            case 1: // string string_value = 1
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    anyValue.addProperty("stringValue", cis.readStringRequireUtf8()); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 2: // bool bool_value = 2
                if (wireType == WireFormat.WIRETYPE_VARINT) {
                    anyValue.addProperty("boolValue", cis.readBool()); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 3: // int64 int_value = 3
                if (wireType == WireFormat.WIRETYPE_VARINT) {
                    anyValue.addProperty("intValue", String.valueOf(cis.readInt64())); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 4: // double double_value = 4
                if (wireType == WireFormat.WIRETYPE_FIXED64) {
                    anyValue.addProperty("doubleValue", cis.readDouble()); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 5: // ArrayValue array_value = 5
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    int length = cis.readRawVarint32();
                    int oldLimit = cis.pushLimit(length);
                    JsonObject arrayValue = parseArrayValue(cis);
                    cis.popLimit(oldLimit);
                    anyValue.add("arrayValue", arrayValue); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 6: // KeyValueList kvlist_value = 6
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    int length = cis.readRawVarint32();
                    int oldLimit = cis.pushLimit(length);
                    JsonObject kvList = parseKeyValueList(cis);
                    cis.popLimit(oldLimit);
                    anyValue.add("kvlistValue", kvList); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            case 7: // bytes bytes_value = 7
                if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    ByteString bs = cis.readBytes();
                    anyValue.addProperty("bytesValue", bytesToHex(bs.toByteArray())); //$NON-NLS-1$
                } else {
                    skipField(cis, wireType);
                }
                break;
            default:
                skipField(cis, wireType);
                break;
            }
        }
        return anyValue;
    }

    /**
     * Parse ArrayValue: repeated AnyValue values = 1
     */
    private static @NonNull JsonObject parseArrayValue(CodedInputStream cis) throws IOException {
        JsonObject arrayValue = new JsonObject();
        JsonArray values = new JsonArray();
        while (!cis.isAtEnd()) {
            int tag = cis.readTag();
            int fieldNumber = WireFormat.getTagFieldNumber(tag);
            int wireType = WireFormat.getTagWireType(tag);
            if (fieldNumber == 1 && wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                int length = cis.readRawVarint32();
                int oldLimit = cis.pushLimit(length);
                JsonObject val = parseAnyValue(cis);
                cis.popLimit(oldLimit);
                values.add(val);
            } else {
                skipField(cis, wireType);
            }
        }
        arrayValue.add("values", values); //$NON-NLS-1$
        return arrayValue;
    }

    /**
     * Parse KeyValueList: repeated KeyValue values = 1
     */
    private static @NonNull JsonObject parseKeyValueList(CodedInputStream cis) throws IOException {
        JsonObject kvList = new JsonObject();
        JsonArray values = new JsonArray();
        while (!cis.isAtEnd()) {
            int tag = cis.readTag();
            int fieldNumber = WireFormat.getTagFieldNumber(tag);
            int wireType = WireFormat.getTagWireType(tag);
            if (fieldNumber == 1 && wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                int length = cis.readRawVarint32();
                int oldLimit = cis.pushLimit(length);
                JsonObject kv = parseKeyValue(cis);
                cis.popLimit(oldLimit);
                if (kv != null) {
                    values.add(kv);
                }
            } else {
                skipField(cis, wireType);
            }
        }
        kvList.add("values", values); //$NON-NLS-1$
        return kvList;
    }

    /**
     * Extract service.name from resource attributes
     */
    private static String extractServiceName(JsonArray resourceAttributes) {
        for (int i = 0; i < resourceAttributes.size(); i++) {
            JsonObject attr = resourceAttributes.get(i).getAsJsonObject();
            if ("service.name".equals(attr.get("key").getAsString())) { //$NON-NLS-1$ //$NON-NLS-2$
                JsonObject value = attr.getAsJsonObject("value"); //$NON-NLS-1$
                if (value != null && value.has("stringValue")) { //$NON-NLS-1$
                    return value.get("stringValue").getAsString(); //$NON-NLS-1$
                }
            }
        }
        return ""; //$NON-NLS-1$
    }

    /**
     * Skip an unknown field based on its wire type
     */
    private static void skipField(CodedInputStream cis, int wireType) throws IOException {
        switch (wireType) {
        case WireFormat.WIRETYPE_VARINT:
            cis.readRawVarint64();
            break;
        case WireFormat.WIRETYPE_FIXED64:
            cis.readFixed64();
            break;
        case WireFormat.WIRETYPE_LENGTH_DELIMITED:
            int length = cis.readRawVarint32();
            cis.skipRawBytes(length);
            break;
        case WireFormat.WIRETYPE_FIXED32:
            cis.readFixed32();
            break;
        default:
            break;
        }
    }

    /**
     * Convert a byte array to a lowercase hex string
     */
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_CHARS[v >>> 4];
            hexChars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hexChars);
    }
}
