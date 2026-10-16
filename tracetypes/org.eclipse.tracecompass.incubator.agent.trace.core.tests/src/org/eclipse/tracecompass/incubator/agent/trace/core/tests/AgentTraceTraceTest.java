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

package org.eclipse.tracecompass.incubator.agent.trace.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tracecompass.incubator.internal.agent.trace.core.trace.AgentTraceTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the Agent Trace (JSONL) trace parser.
 *
 * @author Eya Matkho
 */
@SuppressWarnings("restriction")
public class AgentTraceTraceTest {

    private static final String TRACE_PATH = "res/agent-trace-sample.jsonl"; //$NON-NLS-1$
    private static final int EXPECTED_EVENT_COUNT = 6;

    private ITmfTrace fTrace;

    /**
     * Set up the trace for tests that need it open.
     *
     * @throws TmfTraceException
     *             if trace init fails
     */
    @Before
    public void setUp() throws TmfTraceException {
        ITmfTrace trace = new AgentTraceTrace();
        trace.initTrace(null, TRACE_PATH, ITmfEvent.class);
        fTrace = trace;
    }

    /**
     * Clean up trace resources.
     */
    @After
    public void tearDown() {
        ITmfTrace trace = fTrace;
        if (trace != null) {
            trace.dispose();
            fTrace = null;
        }
    }

    // ------------------------------------------------------------------------
    // Validation tests
    // ------------------------------------------------------------------------

    /**
     * Test that validate() returns OK with high confidence for the sample
     * agent trace file.
     */
    @Test
    public void testValidateOk() {
        ITmfTrace trace = new AgentTraceTrace();
        try {
            IStatus status = trace.validate(null, TRACE_PATH);
            assertTrue("Validation should be OK for agent trace file", status.isOK()); //$NON-NLS-1$
            assertTrue("Confidence should be high (>= 100)", status.getCode() >= 100); //$NON-NLS-1$
        } finally {
            trace.dispose();
        }
    }

    /**
     * Test that validate() returns ERROR for a non-agent-trace file.
     *
     * @throws IOException
     *             if temp file creation fails
     */
    @Test
    public void testValidateError() throws IOException {
        File tempFile = File.createTempFile("not-agent-trace", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("This is not a JSONL file\n"); //$NON-NLS-1$
            writer.write("It has plain text content\n"); //$NON-NLS-1$
        }

        ITmfTrace trace = new AgentTraceTrace();
        try {
            IStatus status = trace.validate(null, tempFile.getAbsolutePath());
            assertFalse("Validation should fail for non-agent-trace file", status.isOK()); //$NON-NLS-1$
            assertEquals("Status should be ERROR", IStatus.ERROR, status.getSeverity()); //$NON-NLS-1$
        } finally {
            trace.dispose();
        }
    }

    /**
     * Test that validate() returns ERROR for a JSON file with unknown event
     * types.
     *
     * @throws IOException
     *             if temp file creation fails
     */
    @Test
    public void testValidateUnknownTypes() throws IOException {
        File tempFile = File.createTempFile("unknown-types", ".jsonl"); //$NON-NLS-1$ //$NON-NLS-2$
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("{\"type\":\"unknown_thing\",\"timestamp\":\"2024-01-15T10:30:00.000Z\",\"event_id\":\"e1\"}\n"); //$NON-NLS-1$
            writer.write("{\"type\":\"another_unknown\",\"timestamp\":\"2024-01-15T10:30:01.000Z\",\"event_id\":\"e2\"}\n"); //$NON-NLS-1$
        }

        ITmfTrace trace = new AgentTraceTrace();
        try {
            IStatus status = trace.validate(null, tempFile.getAbsolutePath());
            assertFalse("Validation should fail for unknown event types", status.isOK()); //$NON-NLS-1$
        } finally {
            trace.dispose();
        }
    }

    // ------------------------------------------------------------------------
    // Event count test
    // ------------------------------------------------------------------------

    /**
     * Test that the trace can be opened and contains 6 events.
     */
    @Test
    public void testEventCount() {
        ITmfTrace trace = fTrace;
        assertNotNull(trace);

        ITmfContext context = trace.seekEvent(0.0);
        assertNotNull(context);

        long count = 0;
        ITmfEvent event = trace.getNext(context);
        while (event != null) {
            count++;
            event = trace.getNext(context);
        }
        assertEquals("Trace should contain 6 events", EXPECTED_EVENT_COUNT, count); //$NON-NLS-1$
    }

    // ------------------------------------------------------------------------
    // Timestamp tests
    // ------------------------------------------------------------------------

    /**
     * Test that timestamps are properly parsed from ISO 8601 format.
     */
    @Test
    public void testTimestampsParsed() {
        ITmfTrace trace = fTrace;
        assertNotNull(trace);

        // Expected timestamps in nanoseconds from epoch for 2024-01-15T10:30:XX.XXXZ
        // 2024-01-15T10:30:00.000Z
        long expectedFirstNanos = 1705314600000000000L;
        // 2024-01-15T10:30:05.000Z
        long expectedLastNanos = 1705314605000000000L;
        // 2024-01-15T10:30:04.500Z (llm_response)
        long expectedLlmResponseNanos = 1705314604500000000L;

        ITmfContext context = trace.seekEvent(0.0);
        List<ITmfEvent> events = readAllEvents(trace, context);

        assertEquals(EXPECTED_EVENT_COUNT, events.size());

        // First event: agent_start at 2024-01-15T10:30:00.000Z
        ITmfTimestamp firstTs = events.get(0).getTimestamp();
        assertNotNull("First event timestamp should not be null", firstTs); //$NON-NLS-1$
        assertEquals("First event timestamp", expectedFirstNanos, firstTs.toNanos()); //$NON-NLS-1$

        // Last event: agent_end at 2024-01-15T10:30:05.000Z
        ITmfTimestamp lastTs = events.get(5).getTimestamp();
        assertNotNull("Last event timestamp should not be null", lastTs); //$NON-NLS-1$
        assertEquals("Last event timestamp", expectedLastNanos, lastTs.toNanos()); //$NON-NLS-1$

        // Fifth event: llm_response at 2024-01-15T10:30:04.500Z (fractional seconds)
        ITmfTimestamp llmTs = events.get(4).getTimestamp();
        assertNotNull("LLM response timestamp should not be null", llmTs); //$NON-NLS-1$
        assertEquals("LLM response timestamp with fractional seconds", //$NON-NLS-1$
                expectedLlmResponseNanos, llmTs.toNanos());

        // Verify timestamps are monotonically increasing
        long prevNanos = Long.MIN_VALUE;
        for (ITmfEvent event : events) {
            long currentNanos = event.getTimestamp().toNanos();
            assertTrue("Timestamps should be monotonically increasing", //$NON-NLS-1$
                    currentNanos >= prevNanos);
            prevNanos = currentNanos;
        }
    }

    // ------------------------------------------------------------------------
    // Event type tests
    // ------------------------------------------------------------------------

    /**
     * Test that event types are correctly read from the trace.
     */
    @Test
    public void testEventTypes() {
        ITmfTrace trace = fTrace;
        assertNotNull(trace);

        ITmfContext context = trace.seekEvent(0.0);
        List<ITmfEvent> events = readAllEvents(trace, context);

        assertEquals(EXPECTED_EVENT_COUNT, events.size());

        // Verify event types in order
        String[] expectedTypes = {
                "agent_start", //$NON-NLS-1$
                "tool_call", //$NON-NLS-1$
                "tool_result", //$NON-NLS-1$
                "llm_request", //$NON-NLS-1$
                "llm_response", //$NON-NLS-1$
                "agent_end" //$NON-NLS-1$
        };

        for (int i = 0; i < EXPECTED_EVENT_COUNT; i++) {
            ITmfEvent event = events.get(i);
            assertNotNull("Event " + i + " should not be null", event); //$NON-NLS-1$ //$NON-NLS-2$
            String actualType = event.getType().getName();
            assertEquals("Event " + i + " type", expectedTypes[i], actualType); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * Test that event content fields are accessible.
     */
    @Test
    public void testEventContent() {
        ITmfTrace trace = fTrace;
        assertNotNull(trace);

        ITmfContext context = trace.seekEvent(0.0);
        ITmfEvent event = trace.getNext(context);
        assertNotNull(event);

        // First event (agent_start) should have type, timestamp, event_id, data fields
        assertNotNull("Event should have 'type' field", //$NON-NLS-1$
                event.getContent().getField("type")); //$NON-NLS-1$
        assertNotNull("Event should have 'timestamp' field", //$NON-NLS-1$
                event.getContent().getField("timestamp")); //$NON-NLS-1$
        assertNotNull("Event should have 'event_id' field", //$NON-NLS-1$
                event.getContent().getField("event_id")); //$NON-NLS-1$
        assertNotNull("Event should have 'data' field", //$NON-NLS-1$
                event.getContent().getField("data")); //$NON-NLS-1$

        // Check event_id value
        assertEquals("evt-001", event.getContent().getField("event_id").getValue()); //$NON-NLS-1$ //$NON-NLS-2$
    }

    // ------------------------------------------------------------------------
    // Helper methods
    // ------------------------------------------------------------------------

    private static List<ITmfEvent> readAllEvents(ITmfTrace trace, ITmfContext context) {
        List<ITmfEvent> events = new ArrayList<>();
        ITmfEvent event = trace.getNext(context);
        while (event != null) {
            events.add(event);
            event = trace.getNext(context);
        }
        return events;
    }
}
