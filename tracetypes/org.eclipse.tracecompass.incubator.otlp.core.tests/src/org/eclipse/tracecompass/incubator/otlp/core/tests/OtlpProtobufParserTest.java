/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.otlp.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.otlp.core.trace.OtlpProtobufParser;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.protobuf.CodedOutputStream;

/**
 * Tests for {@link OtlpProtobufParser}.
 *
 * The OTLP protobuf wire format is encoded by hand using
 * {@link CodedOutputStream} (no generated stubs), mirroring the structure the
 * parser decodes. This keeps the test self-contained while exercising the
 * length-delimited nesting, varints, fixed64 timestamps and the oneof
 * {@code AnyValue} encoding that the parser is most likely to get wrong.
 *
 * @author Matthew Khouzam
 */
public class OtlpProtobufParserTest {

    /* OTLP SpanKind.SPAN_KIND_SERVER */
    private static final int SPAN_KIND_SERVER = 2;
    /* OTLP StatusCode.STATUS_CODE_OK */
    private static final int STATUS_CODE_OK = 1;

    /**
     * Encode a length-delimited (wire type 2) field: tag + length + bytes.
     */
    private static void writeMessage(CodedOutputStream cos, int fieldNumber, byte[] message) throws IOException {
        cos.writeByteArray(fieldNumber, message);
    }

    /**
     * Build an AnyValue message holding a string_value (field 1).
     */
    private static byte[] anyStringValue(String value) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CodedOutputStream cos = CodedOutputStream.newInstance(baos);
        cos.writeString(1, value);
        cos.flush();
        return baos.toByteArray();
    }

    /**
     * Build a KeyValue message: key (field 1, string) + value (field 2,
     * AnyValue).
     */
    private static byte[] keyValue(String key, String value) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CodedOutputStream cos = CodedOutputStream.newInstance(baos);
        cos.writeString(1, key);
        writeMessage(cos, 2, anyStringValue(value));
        cos.flush();
        return baos.toByteArray();
    }

    /**
     * Build a Status message: message (field 2, string) + code (field 3,
     * enum/varint).
     */
    private static byte[] status(int code, String message) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CodedOutputStream cos = CodedOutputStream.newInstance(baos);
        cos.writeString(2, message);
        cos.writeEnum(3, code);
        cos.flush();
        return baos.toByteArray();
    }

    /**
     * Build an Event message: time_unix_nano (field 1, fixed64) + name (field
     * 2, string).
     */
    private static byte[] event(long timeNano, String name) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CodedOutputStream cos = CodedOutputStream.newInstance(baos);
        cos.writeFixed64(1, timeNano);
        cos.writeString(2, name);
        cos.flush();
        return baos.toByteArray();
    }

    /**
     * Build a Span message with the common fields plus one attribute, one
     * event and a status.
     */
    private static byte[] span(byte[] traceId, byte[] spanId, byte[] parentSpanId, String name,
            long start, long end) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CodedOutputStream cos = CodedOutputStream.newInstance(baos);
        cos.writeByteArray(1, traceId); // trace_id
        cos.writeByteArray(2, spanId); // span_id
        if (parentSpanId != null) {
            cos.writeByteArray(4, parentSpanId); // parent_span_id
        }
        cos.writeString(5, name); // name
        cos.writeEnum(6, SPAN_KIND_SERVER); // kind
        cos.writeFixed64(7, start); // start_time_unix_nano
        cos.writeFixed64(8, end); // end_time_unix_nano
        writeMessage(cos, 9, keyValue("http.method", "GET")); // attributes //$NON-NLS-1$ //$NON-NLS-2$
        writeMessage(cos, 11, event(start + 5, "cache-miss")); // events //$NON-NLS-1$
        writeMessage(cos, 15, status(STATUS_CODE_OK, "ok")); // status //$NON-NLS-1$
        cos.flush();
        return baos.toByteArray();
    }

    /**
     * Build a ScopeSpans message: scope (field 1) + one span (field 2).
     */
    private static byte[] scopeSpans(byte[] spanMessage) throws IOException {
        ByteArrayOutputStream scope = new ByteArrayOutputStream();
        CodedOutputStream scopeCos = CodedOutputStream.newInstance(scope);
        scopeCos.writeString(1, "test-scope"); // name //$NON-NLS-1$
        scopeCos.writeString(2, "1.0.0"); // version //$NON-NLS-1$
        scopeCos.flush();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CodedOutputStream cos = CodedOutputStream.newInstance(baos);
        writeMessage(cos, 1, scope.toByteArray()); // scope
        writeMessage(cos, 2, spanMessage); // spans
        cos.flush();
        return baos.toByteArray();
    }

    /**
     * Build a ResourceSpans message: resource (field 1) + scope_spans (field
     * 2).
     */
    private static byte[] resourceSpans(String serviceName, byte[] scopeSpansMessage) throws IOException {
        ByteArrayOutputStream resource = new ByteArrayOutputStream();
        CodedOutputStream resourceCos = CodedOutputStream.newInstance(resource);
        writeMessage(resourceCos, 1, keyValue("service.name", serviceName)); // attributes //$NON-NLS-1$
        resourceCos.flush();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CodedOutputStream cos = CodedOutputStream.newInstance(baos);
        writeMessage(cos, 1, resource.toByteArray()); // resource
        writeMessage(cos, 2, scopeSpansMessage); // scope_spans
        cos.flush();
        return baos.toByteArray();
    }

    /**
     * Build a full ExportTraceServiceRequest: repeated resource_spans (field
     * 1).
     */
    private static byte[] exportRequest(byte[]... resourceSpansMessages) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CodedOutputStream cos = CodedOutputStream.newInstance(baos);
        for (byte[] rs : resourceSpansMessages) {
            writeMessage(cos, 1, rs);
        }
        cos.flush();
        return baos.toByteArray();
    }

    private static File writeToTempFile(byte[] content) throws IOException {
        File file = Files.createTempFile("otlp-test", ".pb").toFile(); //$NON-NLS-1$ //$NON-NLS-2$
        file.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content);
        }
        return file;
    }

    /**
     * Parse a single, fully-populated span and verify every decoded field.
     *
     * @throws IOException
     *             if writing or parsing the temporary file fails
     */
    @Test
    public void testParseSingleSpan() throws IOException {
        byte[] traceId = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
                0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10 };
        byte[] spanId = new byte[] { 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88 };
        byte[] parentSpanId = new byte[] { (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd,
                (byte) 0xee, (byte) 0xff, 0x00, 0x11 };
        long start = 1_000_000_000L;
        long end = 1_000_000_500L;

        byte[] payload = exportRequest(resourceSpans("frontend", //$NON-NLS-1$
                scopeSpans(span(traceId, spanId, parentSpanId, "GET /api", start, end)))); //$NON-NLS-1$
        File file = writeToTempFile(payload);

        List<@NonNull JsonObject> spans = OtlpProtobufParser.parse(file.getAbsolutePath());
        assertNotNull(spans);
        assertEquals("One span expected", 1, spans.size()); //$NON-NLS-1$

        JsonObject s = spans.get(0);
        assertEquals("0102030405060708090a0b0c0d0e0f10", s.get("traceId").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("1122334455667788", s.get("spanId").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("aabbccddeeff0011", s.get("parentSpanId").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("GET /api", s.get("name").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(SPAN_KIND_SERVER, s.get("kind").getAsInt()); //$NON-NLS-1$
        assertEquals(String.valueOf(start), s.get("startTimeUnixNano").getAsString()); //$NON-NLS-1$
        assertEquals(String.valueOf(end), s.get("endTimeUnixNano").getAsString()); //$NON-NLS-1$
        assertEquals("frontend", s.get("serviceName").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("test-scope", s.get("instrumentationScopeName").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("1.0.0", s.get("instrumentationScopeVersion").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$

        // Resource attributes carry service.name
        assertTrue("Resource attributes present", s.has("resourceAttributes")); //$NON-NLS-1$ //$NON-NLS-2$

        // Span attribute http.method=GET
        assertTrue("Span attributes present", s.has("attributes")); //$NON-NLS-1$ //$NON-NLS-2$
        JsonObject attr = s.getAsJsonArray("attributes").get(0).getAsJsonObject(); //$NON-NLS-1$
        assertEquals("http.method", attr.get("key").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("GET", attr.getAsJsonObject("value").get("stringValue").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        // Event
        assertTrue("Events present", s.has("events")); //$NON-NLS-1$ //$NON-NLS-2$
        JsonObject ev = s.getAsJsonArray("events").get(0).getAsJsonObject(); //$NON-NLS-1$
        assertEquals("cache-miss", ev.get("name").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(String.valueOf(start + 5), ev.get("timeUnixNano").getAsString()); //$NON-NLS-1$

        // Status
        assertTrue("Status present", s.has("status")); //$NON-NLS-1$ //$NON-NLS-2$
        JsonObject st = s.getAsJsonObject("status"); //$NON-NLS-1$
        assertEquals(STATUS_CODE_OK, st.get("code").getAsInt()); //$NON-NLS-1$
        assertEquals("ok", st.get("message").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Parse multiple resource/scope groups and verify each span keeps its own
     * service name and the total count is correct.
     *
     * @throws IOException
     *             if writing or parsing the temporary file fails
     */
    @Test
    public void testParseMultipleResourceSpans() throws IOException {
        byte[] id = new byte[] { 0x01 };
        byte[] rs1 = resourceSpans("service-a", //$NON-NLS-1$
                scopeSpans(span(id, id, null, "a-op", 10, 20))); //$NON-NLS-1$
        byte[] rs2 = resourceSpans("service-b", //$NON-NLS-1$
                scopeSpans(span(id, id, null, "b-op", 30, 40))); //$NON-NLS-1$
        File file = writeToTempFile(exportRequest(rs1, rs2));

        List<@NonNull JsonObject> spans = OtlpProtobufParser.parse(file.getAbsolutePath());
        assertEquals("Two spans expected", 2, spans.size()); //$NON-NLS-1$
        assertEquals("service-a", spans.get(0).get("serviceName").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("a-op", spans.get(0).get("name").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("service-b", spans.get(1).get("serviceName").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("b-op", spans.get(1).get("name").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * A span without a parent_span_id must not produce a parentSpanId property.
     *
     * @throws IOException
     *             if writing or parsing the temporary file fails
     */
    @Test
    public void testSpanWithoutParent() throws IOException {
        byte[] id = new byte[] { 0x42 };
        File file = writeToTempFile(exportRequest(resourceSpans("root", //$NON-NLS-1$
                scopeSpans(span(id, id, null, "root-op", 1, 2))))); //$NON-NLS-1$

        List<@NonNull JsonObject> spans = OtlpProtobufParser.parse(file.getAbsolutePath());
        assertEquals(1, spans.size());
        assertFalse("No parentSpanId expected", spans.get(0).has("parentSpanId")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * An empty payload yields no spans and does not throw.
     *
     * @throws IOException
     *             if writing or parsing the temporary file fails
     */
    @Test
    public void testParseEmpty() throws IOException {
        File file = writeToTempFile(new byte[0]);
        List<@NonNull JsonObject> spans = OtlpProtobufParser.parse(file.getAbsolutePath());
        assertNotNull(spans);
        assertTrue("Empty payload yields no spans", spans.isEmpty()); //$NON-NLS-1$
    }

    /**
     * Unknown fields with each wire type must be skipped without corrupting the
     * parse of known fields.
     *
     * @throws IOException
     *             if writing or parsing the temporary file fails
     */
    @Test
    public void testUnknownFieldsAreSkipped() throws IOException {
        byte[] id = new byte[] { 0x07 };
        // Build a span and prepend/append unknown fields at the request level.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CodedOutputStream cos = CodedOutputStream.newInstance(baos);
        // Unknown varint field 99
        cos.writeInt64(99, 123456789L);
        // Unknown fixed64 field 98
        cos.writeFixed64(98, 42L);
        // Known resource_spans field 1
        writeMessage(cos, 1, resourceSpans("svc", //$NON-NLS-1$
                scopeSpans(span(id, id, null, "op", 100, 200)))); //$NON-NLS-1$
        // Unknown length-delimited field 97
        cos.writeString(97, "ignored"); //$NON-NLS-1$
        cos.flush();
        File file = writeToTempFile(baos.toByteArray());

        List<@NonNull JsonObject> spans = OtlpProtobufParser.parse(file.getAbsolutePath());
        assertEquals("Known span still parsed", 1, spans.size()); //$NON-NLS-1$
        assertEquals("op", spans.get(0).get("name").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("svc", spans.get(0).get("serviceName").getAsString()); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
