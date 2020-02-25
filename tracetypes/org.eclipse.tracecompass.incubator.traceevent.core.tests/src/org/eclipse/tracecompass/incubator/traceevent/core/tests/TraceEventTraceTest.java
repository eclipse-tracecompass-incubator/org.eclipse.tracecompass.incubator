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

package org.eclipse.tracecompass.incubator.traceevent.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.trace.TraceEventTrace;
import org.eclipse.tracecompass.internal.provisional.jsontrace.core.trace.JsonTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.io.BufferedRandomAccessFile;
import org.eclipse.tracecompass.tmf.core.project.model.ITmfPropertiesProvider;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/**
 * Test reading real traces
 *
 * @author Matthew Khouzam
 *
 */
public class TraceEventTraceTest {

    /**
     * Test reading an event and get the aspects
     *
     * @throws TmfTraceException
     *             file error
     */
    @Test
    public void testEvent() throws TmfTraceException {
        String path = "traces/simple-in-order.json";
        Map<String, ITmfEventAspect<?>> eventAspects = getEventAspects(path);
        ITmfEvent event = getFirstEvent(path);
        assertNotNull(event);
        ImmutableSet<String> aspectNames = ImmutableSet.of("TID", "Args", "Phase", "Category", "PID", "Duration", "ID", "Callsite", "Timestamp", "LogLevel", "Name", "Process Name", "Thread Name");

        assertEquals(aspectNames, eventAspects.keySet());
        testAspect(eventAspects.get("TID"), event, 0);
        testAspect(eventAspects.get("Phase"), event, "C");
        testAspect(eventAspects.get("Category"), event, null);
        testAspect(eventAspects.get("Name"), event, "foo");
        testAspect(eventAspects.get("PID"), event, "0");
        testAspect(eventAspects.get("Timestamp"), event, TmfTimestamp.fromNanos(0));
        testAspect(eventAspects.get("Duration"), event, null);
        testAspect(eventAspects.get("LogLevel"), event, java.util.logging.Level.INFO);
        testAspect(eventAspects.get("Callsite"), event, null);
        testAspect(eventAspects.get("Args"), event, Collections.singletonMap("value", "1"));
    }

    private static void testAspect(ITmfEventAspect<?> eventAspect, ITmfEvent event, Object expected) {
        assertNotNull(event);
        assertNotNull(eventAspect);
        assertEquals(expected, eventAspect.resolve(event));
    }

    /**
     * Test a simple in order json trace
     *
     * @throws TmfTraceException
     *             should not happen
     */
    @Test
    public void testSimpleInOrder() throws TmfTraceException {
        String path = "traces/simple-in-order.json";
        int nbEvents = 8;
        ITmfTimestamp startTime = TmfTimestamp.fromNanos(0);
        ITmfTimestamp endTime = TmfTimestamp.fromMicros(77);
        testTrace(path, nbEvents, startTime, endTime);
    }

    /**
     * Test a simple out of order trace
     *
     * @throws TmfTraceException
     *             should not happen
     */
    @Test
    public void testSimpleOutOfOrder() throws TmfTraceException {
        String path = "traces/simple-out-of-order.json";
        int nbEvents = 8;
        ITmfTimestamp startTime = TmfTimestamp.fromNanos(0);
        ITmfTimestamp endTime = TmfTimestamp.fromMicros(77);
        testTrace(path, nbEvents, startTime, endTime);
    }

    /**
     * Test async begin and end trace
     *
     * @throws TmfTraceException
     *             should not happen
     */
    @Test
    public void testAsyncBeginEnd() throws TmfTraceException {
        String path = "traces/async_begin_end.json";
        int nbEvents = 23;
        ITmfTimestamp startTime = TmfTimestamp.fromMicros(50);
        ITmfTimestamp endTime = TmfTimestamp.fromMicros(900);
        testTrace(path, nbEvents, startTime, endTime);
    }

    /**
     * Test a "big" trace
     *
     * @throws TmfTraceException
     *             should not happen
     */
    @Test
    public void testBigTrace() throws TmfTraceException {
        String path = "traces/big_trace.json";
        int nbEvents = 1866;
        ITmfTimestamp startTime = TmfTimestamp.fromMicros(438877834451L);
        ITmfTimestamp endTime = TmfTimestamp.fromMicros(438881451344L);
        testTrace(path, nbEvents, startTime, endTime);
    }

    /**
     * Test a trace with duplicate entries
     *
     * @throws TmfTraceException
     *             should not happen
     */
    @Test
    public void testDubs() throws TmfTraceException {
        String path = "traces/instant-events-duplicates.json";
        int nbEvents = 2;
        ITmfTimestamp startTime = TmfTimestamp.fromMicros(510075891653L);
        ITmfTimestamp endTime = TmfTimestamp.fromMicros(510075907469L);
        testTrace(path, nbEvents, startTime, endTime);
    }

    /**
     * Test a chromeOs system trace
     *
     * @throws TmfTraceException
     *             should not happen
     */
    @Test
    public void testChromeosTrace() throws TmfTraceException {
        String[] env = { "Type", "Trace-Event",
                "process_sort_index-5044", "-1",
                "pid-5044", "GPU Process",
                "tid-5051", "Chrome_ChildIOThread",
                "process_sort_index-5075", "-5",
                "pid-5075", "Renderer",
                "pidLabel-5075", "chrome://tracing",
                "thread_sort_index-12", "-1",
                "tid-13", "Chrome_ChildIOThread",
                "process_sort_index-5243", "-5",
                "pid-5243", "Renderer",
                "pidLabel-5243", "The New York Times - Breaking News, World News & Multimedia",
                "thread_sort_index-73", "-1",
                "process_sort_index-5145", "-5",
                "pid-5145", "Renderer",
                "pidLabel-5145", "The New York Times - Breaking News, World News & Multimedia",
                "thread_sort_index-27", "-1",
                "process_sort_index-5173", "-5",
                "pid-5173", "Renderer",
                "pidLabel-5173", "The New York Times - Breaking News, World News & Multimedia",
                "thread_sort_index-43", "-1",
                "process_sort_index-5014", "-6",
                "pid-5014", "Browser",
                "tid-5036", "Chrome_IOThread",
                "tid-5014", "CrBrowserMain" };
        Map<String, String> expectedProperties = new LinkedHashMap<>();
        for (int i = 0; i < env.length; i += 2) {
            expectedProperties.put(env[i], env[i + 1]);
        }
        String path = "traces/chromeos_system_trace.json";
        int nbEvents = 12;
        ITmfTimestamp startTime = TmfTimestamp.fromMicros(5443650636079L);
        ITmfTimestamp endTime = TmfTimestamp.fromMicros(5443672642443L);
        Map<String, String> properties = testTrace(path, nbEvents, startTime, endTime);
        assertEquals(expectedProperties, properties);
    }

    private static Map<String, String> testTrace(String path, int nbEvents, ITmfTimestamp startTime, ITmfTimestamp endTime) throws TmfTraceException {
        ITmfTrace trace = new TraceEventTrace();
        try {
            IStatus validate = trace.validate(null, path);
            assertTrue(validate.getMessage(), validate.isOK());
            trace.initTrace(null, path, ITmfEvent.class);
            // Make sure start and end time have been set at this point to timestamp of first event
            assertEquals(startTime, trace.getStartTime());
            assertEquals(startTime, trace.getEndTime());

            ITmfContext context = trace.seekEvent(0.0);
            ITmfEvent event = trace.getNext(context);
            long count = 0;
            long prevTs = -1;
            while (event != null) {
                count++;
                @NonNull
                ITmfTimestamp currentTime = event.getTimestamp();
                assertNotNull("Event has a timestamp", currentTime);
                assertTrue("Monotonic events", currentTime.toNanos() >= prevTs);
                prevTs = currentTime.toNanos();
                event = trace.getNext(context);
            }
            assertEquals(nbEvents, count);
            assertEquals(nbEvents, trace.getNbEvents());
            assertEquals(startTime.toNanos(), trace.getStartTime().toNanos());
            assertEquals(endTime.toNanos(), trace.getEndTime().toNanos());
            assertTrue(trace instanceof ITmfPropertiesProvider);
            return ((ITmfPropertiesProvider) trace).getProperties();
        } finally {
            trace.dispose();
        }
    }

    private static Map<String, ITmfEventAspect<?>> getEventAspects(String path) {
        ITmfTrace trace = new TraceEventTrace();
        Map<String, ITmfEventAspect<?>> returnValues = new HashMap<>();
        for (@NonNull
        ITmfEventAspect<?> aspect : trace.getEventAspects()) {
            returnValues.put(aspect.getName(), aspect);
        }
        trace.dispose();
        return returnValues;
    }

    private static ITmfEvent getFirstEvent(String path) throws TmfTraceException {
        ITmfTrace trace = new TraceEventTrace();
        try {
            trace.initTrace(null, path, ITmfEvent.class);
            return trace.getNext(trace.seekEvent(0.0));
        } finally {
            trace.dispose();
        }
    }

    private static class TraceEventTraceStub extends TraceEventTrace {

        public static void findStart(RandomAccessFile file) throws IOException {
            TraceEventTrace.goToCorrectStart(file);
        }

    }

    /**
     * Test the methods inside the TraceEventTrace class, for eg. some data used
     * in validation
     *
     * @throws IOException
     *             should not happen
     */
    @SuppressWarnings({ "restriction", "resource" })
    @Test
    public void testTraceEventInsides() throws IOException {
        // Test a trace that starts with {"traceEvents"
        String path = "traces/flow_simple.json";

        BufferedRandomAccessFile rafile = new BufferedRandomAccessFile(path, "r");

        TraceEventTraceStub.findStart(rafile);
        assertTrue(rafile.getFilePointer() >= 14);

        // There should be one event string read per event
        int eventCount = 0;
        while (JsonTrace.readNextEventString(() -> rafile.read()) != null) {
            eventCount++;
        }
        assertEquals(10, eventCount);

        // Test a trace that starts directly with events
        path = "traces/simple-in-order.json";

        BufferedRandomAccessFile rafile2 = new BufferedRandomAccessFile(path, "r");

        TraceEventTraceStub.findStart(rafile2);
        assertEquals(0, rafile2.getFilePointer());

        eventCount = 0;
        while (JsonTrace.readNextEventString(() -> rafile2.read()) != null) {
            eventCount++;
        }
        assertEquals(8, eventCount);
    }

    /**
     * Test a trace with an empty event
     *
     * @throws TmfTraceException
     *             should not happen
     */
    @Test
    public void testEmptyEvent() throws TmfTraceException {
        String path = "traces/empty_event.json";
        int nbEvents = 2;
        ITmfTimestamp startTime = TmfTimestamp.fromMicros(456123453L);
        ITmfTimestamp endTime = TmfTimestamp.fromMicros(456123455L);
        testTrace(path, nbEvents, startTime, endTime);

        // Test the values of the first event.
        ITmfTrace trace = new TraceEventTrace();
        try {
            trace.initTrace(null, path, ITmfEvent.class);
            ITmfContext context = trace.seekEvent(0.0);
            ITmfEvent event = trace.getNext(context);
            ITmfEventField eventField = event.getContent();
            assertEquals(1000000.0, eventField.getField("dur").getValue());
            assertEquals("X", eventField.getField("ph").getValue());
            assertEquals("event1", eventField.getField("name").getValue());
            assertEquals("12", eventField.getField("pid").getValue());
            assertEquals(12, eventField.getField("tid").getValue());
            assertEquals(456123453000L, eventField.getField("ts").getValue());
        } finally {
            trace.dispose();
        }
    }
}
