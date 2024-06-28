/*******************************************************************************
 * Copyright (c) 2024 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.jifa.core.tests.jfr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tracecompass.incubator.internal.jifa.core.jfr.JfrTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * GC Trace test, basically checking the bridge between trace compass and jifa.
 * Integration tests, it reads ALL the traces available.
 */
public class JfrTraceTest {

    private static final String TRACE_LOCATION = "src/org/eclipse/tracecompass/incubator/jifa/core/tests/jfr/res/jfr.jfr";
    private static final Set<String> EMPTY_TRACES = Set.of();
    private JfrTrace fJfrTrace;

    /**
     * Setup
     */
    @Before
    public void setUp() {
        fJfrTrace = new JfrTrace();
    }

    /**
     * Cleanup
     */
    @After
    public void tearDown() {
        fJfrTrace.dispose();
    }

    /**
     * All traces are valid, some have no events.
     */
    @Test
    public void testValidateValidFile() {
        for (File trace : new File("src/org/eclipse/tracecompass/incubator/jifa/core/tests/jfr/res/").listFiles()) {
            IStatus status = fJfrTrace.validate(null, trace.getAbsolutePath());
            assertEquals("OK", status.getMessage());
            assertEquals(trace.getPath(), 0, status.getCode());
        }
    }

    /**
     * Test an invalid trace
     */
    @Test
    public void testValidateInvalidFile() {
        IStatus status = fJfrTrace.validate(null, "src/org/eclipse/tracecompass/incubator/jifa/core/tests/jfr/badgc/not_a_gc.log");
        assertEquals(IStatus.INFO, status.getSeverity());
        assertEquals("Trace File Not found", status.getMessage());
    }

    /**
     * Test init trace
     *
     * @throws TmfTraceException
     *             The trace initialization failed
     * @throws IOException
     *             file parsing failed
     */
    @Test
    public void testInitTrace() throws TmfTraceException, IOException {
        fJfrTrace.initTrace(null, TRACE_LOCATION, ITmfEvent.class);
        assertEquals(TRACE_LOCATION, fJfrTrace.getPath());
    }

    /**
     * Test Get Current Location
     */
    @Test
    public void testGetCurrentLocation() {
        ITmfLocation location = fJfrTrace.getCurrentLocation();
        assertEquals(0L, location.getLocationInfo());
    }

    /**
     * Test seek
     *
     * @throws TmfTraceException
     *             the trace failed to load
     */
    @Test
    public void testSeekEventWithLocation() throws TmfTraceException {
        fJfrTrace.initTrace(null, TRACE_LOCATION, null);
        assertNotNull(fJfrTrace.seekEvent(new TmfLongLocation(0)));
    }

    /**
     * Test seeking at a ratio
     *
     * @throws TmfTraceException
     *             the trace failed to load
     */
    @Test
    public void testSeekEventWithRatio() throws TmfTraceException {
        fJfrTrace.initTrace(null, TRACE_LOCATION, null);
        ITmfContext a = fJfrTrace.seekEvent(new TmfLongLocation(0));
        ITmfContext b = fJfrTrace.seekEvent(0.5);
        assertNotEquals(a, b);

    }

    /**
     * Test parsing every event
     *
     * @throws TmfTraceException
     *             the trace failed to load
     */
    @Test
    public void testParseEvent() throws TmfTraceException {
        File root = new File("src/org/eclipse/tracecompass/incubator/jifa/core/tests/jfr/res/");
        for (File trace : root.listFiles()) {
            fJfrTrace.dispose();
            fJfrTrace = new JfrTrace();
            fJfrTrace.initTrace(null, trace.getAbsolutePath(), ITmfEvent.class);
            assertEquals(fJfrTrace.getNbEvents(), 0L);
            fJfrTrace.readEnd();
            if (!EMPTY_TRACES.contains(trace.getPath())) {
                assertNotEquals(trace.getPath(), fJfrTrace.getNbEvents(), 0L);
            } else {
                assertEquals(trace.getPath(), fJfrTrace.getNbEvents(), 0L);
            }
        }
    }
}