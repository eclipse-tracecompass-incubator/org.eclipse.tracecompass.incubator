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

package org.eclipse.tracecompass.incubator.sqlite.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.tracecompass.analysis.counters.core.aspects.ITmfCounterAspect;
import org.eclipse.tracecompass.incubator.internal.sqlite.core.trace.SQLiteTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Tests for {@link SQLiteTrace} using the {@code synthetic_trace.sqlite} sample
 * produced by {@code res/generate_synthetic_trace.py}.
 *
 * @author Matthew Khouzam
 */
public class SQLiteTraceTest {

    private static final String SAMPLE = "res/synthetic_trace.sqlite"; //$NON-NLS-1$

    /** Number of event rows across all trace tables in the sample. */
    private static final int EXPECTED_EVENTS = 22;
    /** First (smallest) timestamp in nanoseconds since the Unix epoch. */
    private static final long FIRST_TIMESTAMP = 1721752204623337000L;
    /** Last (largest) timestamp in nanoseconds since the Unix epoch. */
    private static final long LAST_TIMESTAMP = 1721752204639108000L;

    private static String samplePath() {
        Bundle bundle = FrameworkUtil.getBundle(SQLiteTraceTest.class);
        assertNotNull("Test bundle not found", bundle); //$NON-NLS-1$
        URL url = FileLocator.find(bundle, new Path(SAMPLE), null);
        assertNotNull("Sample trace not found in bundle", url); //$NON-NLS-1$
        try {
            return new Path(FileLocator.toFileURL(url).getPath()).toOSString();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The sample validates with a positive confidence.
     */
    @Test
    public void testValidateSample() {
        SQLiteTrace trace = new SQLiteTrace();
        IStatus status = trace.validate(null, samplePath());
        assertTrue("Expected OK status, got: " + status, status.isOK()); //$NON-NLS-1$
        assertTrue(status instanceof TraceValidationStatus);
        int confidence = ((TraceValidationStatus) status).getConfidence();
        assertTrue("Expected positive confidence, got " + confidence, confidence > 0); //$NON-NLS-1$
        trace.dispose();
    }

    /**
     * A non-existent path yields an ERROR status.
     */
    @Test
    public void testValidateMissing() {
        SQLiteTrace trace = new SQLiteTrace();
        IStatus status = trace.validate(null, "/this/does/not/exist.sqlite"); //$NON-NLS-1$
        assertEquals(IStatus.ERROR, status.getSeverity());
        trace.dispose();
    }

    /**
     * A non-SQLite file yields an ERROR status.
     *
     * @throws Exception
     *             on I/O error
     */
    @Test
    public void testValidateNonSqlite() throws Exception {
        File tmp = File.createTempFile("not-a", ".sqlite"); //$NON-NLS-1$ //$NON-NLS-2$
        try {
            java.nio.file.Files.write(tmp.toPath(), "this is not a sqlite database".getBytes()); //$NON-NLS-1$
            SQLiteTrace trace = new SQLiteTrace();
            IStatus status = trace.validate(null, tmp.getAbsolutePath());
            assertEquals(IStatus.ERROR, status.getSeverity());
            trace.dispose();
        } finally {
            assertTrue(tmp.delete());
        }
    }

    /**
     * Reading the sample yields the expected number of events, ordered by
     * timestamp, with the expected first and last timestamps.
     *
     * @throws TmfTraceException
     *             on trace initialization failure
     */
    @Test
    public void testReadEvents() throws TmfTraceException {
        SQLiteTrace trace = new SQLiteTrace();
        try {
            trace.initTrace(null, samplePath(), ITmfEvent.class);

            ITmfContext context = trace.seekEvent(0L);
            ITmfEvent first = trace.getNext(context);
            assertNotNull("Expected at least one event", first); //$NON-NLS-1$
            assertEquals(FIRST_TIMESTAMP, first.getTimestamp().toNanos());

            int count = 1;
            long previous = first.getTimestamp().toNanos();
            ITmfEvent last = first;
            ITmfEvent event = trace.getNext(context);
            while (event != null) {
                long ts = event.getTimestamp().toNanos();
                assertTrue("Events must be in non-decreasing timestamp order", ts >= previous); //$NON-NLS-1$
                previous = ts;
                last = event;
                count++;
                event = trace.getNext(context);
            }

            assertEquals(EXPECTED_EVENTS, count);
            assertEquals(LAST_TIMESTAMP, last.getTimestamp().toNanos());

            // The event type should be the dotted trace-point name.
            assertFalse(first.getName().isEmpty());
        } finally {
            trace.dispose();
        }
    }

    /**
     * The external schema table (name ending in "trace") is applied: events are
     * named with the dotted trace-point name and carry the declared severity.
     *
     * @throws TmfTraceException
     *             on trace initialization failure
     */
    @Test
    public void testSchemaApplied() throws TmfTraceException {
        SQLiteTrace trace = new SQLiteTrace();
        try {
            trace.initTrace(null, samplePath(), ITmfEvent.class);

            ITmfEventAspect<?> severityAspect = null;
            for (ITmfEventAspect<?> aspect : trace.getEventAspects()) {
                if ("Severity".equals(aspect.getName())) { //$NON-NLS-1$
                    severityAspect = aspect;
                }
            }
            assertNotNull("Severity aspect should be present", severityAspect); //$NON-NLS-1$

            ITmfContext context = trace.seekEvent(0L);
            ITmfEvent event = trace.getNext(context);
            assertNotNull(event);
            // Event name is the dotted trace-point name from the schema table,
            // e.g. "sensor.alpha" (contains a dot, not the "_" table name).
            assertTrue("Expected a dotted schema name, got: " + event.getName(), //$NON-NLS-1$
                    event.getName().contains(".")); //$NON-NLS-1$
            // Severity is declared as TRACE3 for every row in the sample.
            assertEquals("TRACE3", severityAspect.resolve(event)); //$NON-NLS-1$
        } finally {
            trace.dispose();
        }
    }

    /**
     * Every column of every event table is exposed as a deduplicated events-
     * table aspect: shared columns (time, flag, counter1, counter2) appear
     * exactly once, and a table-specific column appears too.
     *
     * @throws TmfTraceException
     *             on trace initialization failure
     */
    @Test
    public void testColumnAspects() throws TmfTraceException {
        SQLiteTrace trace = new SQLiteTrace();
        try {
            trace.initTrace(null, samplePath(), ITmfEvent.class);
            Map<String, Integer> byName = new HashMap<>();
            for (ITmfEventAspect<?> aspect : trace.getEventAspects()) {
                byName.merge(aspect.getName(), 1, Integer::sum);
            }
            // Shared columns are present and appear exactly once (deduplicated).
            assertEquals("flag should be deduplicated", Integer.valueOf(1), byName.get("flag")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("counter1 should be deduplicated", Integer.valueOf(1), byName.get("counter1")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(Integer.valueOf(1), byName.get("counter2")); //$NON-NLS-1$
            // A column specific to a single table is also exposed.
            assertTrue("expected a table-specific column aspect", byName.containsKey("counter3")); //$NON-NLS-1$ //$NON-NLS-2$
            // No aspect name is duplicated.
            for (Map.Entry<String, Integer> entry : byName.entrySet()) {
                assertEquals("aspect '" + entry.getKey() + "' duplicated", Integer.valueOf(1), entry.getValue()); //$NON-NLS-1$ //$NON-NLS-2$
            }
        } finally {
            trace.dispose();
        }
    }

    /**
     * Only the {@code time} column aspect is visible by default; every other
     * column aspect is hidden.
     *
     * @throws TmfTraceException
     *             on trace initialization failure
     */
    @Test
    public void testColumnsHiddenExceptTime() throws TmfTraceException {
        SQLiteTrace trace = new SQLiteTrace();
        try {
            trace.initTrace(null, samplePath(), ITmfEvent.class);
            boolean sawTime = false;
            for (ITmfEventAspect<?> aspect : trace.getEventAspects()) {
                String name = aspect.getName();
                if ("time".equals(name)) { //$NON-NLS-1$
                    sawTime = true;
                    assertFalse("'time' column must be visible", aspect.isHiddenByDefault()); //$NON-NLS-1$
                } else if ("flag".equals(name) || "counter1".equals(name) || "counter2".equals(name) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        || "counter3".equals(name)) { //$NON-NLS-1$
                    assertTrue("column '" + name + "' must be hidden", aspect.isHiddenByDefault()); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
            assertTrue("'time' column aspect should exist", sawTime); //$NON-NLS-1$
        } finally {
            trace.dispose();
        }
    }

    /**
     * Numeric columns are exposed as counter aspects; non-numeric columns are
     * not. All remain hidden by default except {@code time}.
     *
     * @throws TmfTraceException
     *             on trace initialization failure
     */
    @Test
    public void testNumericColumnsAreCounterAspects() throws TmfTraceException {
        SQLiteTrace trace = new SQLiteTrace();
        try {
            trace.initTrace(null, samplePath(), ITmfEvent.class);
            boolean sawNumericCounter = false;
            for (ITmfEventAspect<?> aspect : trace.getEventAspects()) {
                String name = aspect.getName();
                if ("counter1".equals(name) || "counter2".equals(name) || "counter3".equals(name)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    assertTrue("numeric column '" + name + "' must be a counter aspect", //$NON-NLS-1$ //$NON-NLS-2$
                            aspect instanceof ITmfCounterAspect);
                    sawNumericCounter = true;
                } else if ("flag".equals(name)) { //$NON-NLS-1$
                    assertFalse("text column 'flag' must not be a counter aspect", //$NON-NLS-1$
                            aspect instanceof ITmfCounterAspect);
                }
            }
            assertTrue("expected at least one numeric counter aspect", sawNumericCounter); //$NON-NLS-1$
        } finally {
            trace.dispose();
        }
    }

    /**
     * Reading past the end returns {@code null}.
     *
     * @throws TmfTraceException
     *             on trace initialization failure
     */
    @Test
    public void testReadPastEnd() throws TmfTraceException {
        SQLiteTrace trace = new SQLiteTrace();
        try {
            trace.initTrace(null, samplePath(), ITmfEvent.class);
            long endIndex = EXPECTED_EVENTS;
            ITmfContext context = trace.seekEvent(endIndex);
            assertNull(trace.getNext(context));
        } finally {
            trace.dispose();
        }
    }
}
