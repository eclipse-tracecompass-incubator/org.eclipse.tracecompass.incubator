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
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.eclipse.tracecompass.incubator.internal.opentracing.core.event.IOpenTracingConstants;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.event.OpenTracingField;
import org.eclipse.tracecompass.incubator.internal.otlp.core.trace.OtlpField;
import org.junit.Test;

/**
 * Tests that {@link OtlpField#parseJson(String)} preserves the nanosecond
 * precision of OTLP timestamps instead of truncating them to Jaeger
 * microseconds. Sub-microsecond spans must keep a non-zero duration and
 * distinct sub-microsecond events must not collapse onto a single timestamp
 * key.
 *
 * @author Matthew Khouzam
 */
public class OtlpFieldPrecisionTest {

    /**
     * A span whose duration is below one microsecond must retain its exact
     * nanosecond start time and duration, not round to a zero-length span.
     */
    @Test
    public void testSubMicrosecondSpanDuration() {
        long startNano = 1_000_000_000_500L; // ...500 ns, not a micro multiple
        long endNano = 1_000_000_000_999L;   // 499 ns later, still sub-micro
        String json = "{"
                + "\"traceId\":\"abc123\","
                + "\"spanId\":\"span1\","
                + "\"name\":\"op\","
                + "\"startTimeUnixNano\":\"" + startNano + "\","
                + "\"endTimeUnixNano\":\"" + endNano + "\","
                + "\"serviceName\":\"svc\""
                + "}";

        OpenTracingField field = OtlpField.parseJson(json);
        assertNotNull("Field should parse", field); //$NON-NLS-1$

        assertEquals("Start time should keep nanosecond precision", //$NON-NLS-1$
                startNano, field.getStartTime().longValue());
        assertEquals("Duration should keep nanosecond precision", //$NON-NLS-1$
                endNano - startNano, field.getDuration().longValue());
    }

    /**
     * When a service name is absent the process name must fall back to a
     * non-empty value so the OpenTracing process attribute is well-formed.
     */
    @Test
    public void testMissingServiceNameFallback() {
        String json = "{"
                + "\"traceId\":\"abc123\","
                + "\"spanId\":\"span1\","
                + "\"name\":\"op\","
                + "\"startTimeUnixNano\":\"1000\","
                + "\"endTimeUnixNano\":\"2000\""
                + "}";

        OpenTracingField field = OtlpField.parseJson(json);
        assertNotNull("Field should parse", field); //$NON-NLS-1$
        assertEquals("Process name should use the unknown_service fallback", //$NON-NLS-1$
                "unknown_service", field.getProcessName()); //$NON-NLS-1$
    }

    /**
     * Two span events within the same microsecond must be preserved as two
     * distinct log entries, keyed by their exact nanosecond timestamps.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDistinctSubMicrosecondEvents() {
        long eventNanoA = 1_000_000_000_100L;
        long eventNanoB = 1_000_000_000_900L; // same microsecond as A
        String json = "{"
                + "\"traceId\":\"abc123\","
                + "\"spanId\":\"span1\","
                + "\"name\":\"op\","
                + "\"startTimeUnixNano\":\"1000000000000\","
                + "\"endTimeUnixNano\":\"1000000001000\","
                + "\"serviceName\":\"svc\","
                + "\"events\":["
                + "{\"name\":\"a\",\"timeUnixNano\":\"" + eventNanoA + "\"},"
                + "{\"name\":\"b\",\"timeUnixNano\":\"" + eventNanoB + "\"}"
                + "]}";

        OpenTracingField field = OtlpField.parseJson(json);
        assertNotNull("Field should parse", field); //$NON-NLS-1$

        Object logs = field.getContent().getFieldValue(Map.class, IOpenTracingConstants.LOGS);
        assertNotNull("Logs should be present", logs); //$NON-NLS-1$
        Map<Long, Map<String, String>> logMap = (Map<Long, Map<String, String>>) logs;
        assertEquals("Both sub-microsecond events should be kept distinct", //$NON-NLS-1$
                2, logMap.size());
    }
}
