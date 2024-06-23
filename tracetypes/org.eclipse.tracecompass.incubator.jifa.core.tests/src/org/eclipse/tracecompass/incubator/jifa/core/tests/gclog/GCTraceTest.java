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

package org.eclipse.tracecompass.incubator.jifa.core.tests.gclog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tracecompass.incubator.internal.jifa.core.gclog.GCTrace;
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
public class GCTraceTest {

    private static final String TRACE_LOCATION = "src/org/eclipse/tracecompass/incubator/jifa/core/tests/gclog/res/11CMSGCParser.log";
    private static final Set<String> EMPTY_TRACES = Set.of("src/org/eclipse/tracecompass/incubator/jifa/core/tests/gclog/res/11ZGCParser.log",
            "src/org/eclipse/tracecompass/incubator/jifa/core/tests/gclog/res/17ZGCParser.log",
            "src/org/eclipse/tracecompass/incubator/jifa/core/tests/gclog/res/8CMSPromotionFailed.log",
            "src/org/eclipse/tracecompass/incubator/jifa/core/tests/gclog/res/8G1GCParserAdaptiveSize.log",
            "src/org/eclipse/tracecompass/incubator/jifa/core/tests/gclog/res/8GenerationalGCInterleave.log");
    private GCTrace fGcTrace;

    /**
     * Setup
     */
    @Before
    public void setUp() {
        fGcTrace = new GCTrace();
    }

    /**
     * Cleanup
     */
    @After
    public void tearDown() {
        fGcTrace.dispose();
    }

    /**
     * All traces are valid, some have no events.
     */
    @Test
    public void testValidateValidFile() {
        for (File trace : new File("src/org/eclipse/tracecompass/incubator/jifa/core/tests/gclog/res/").listFiles()) {
            IStatus status = fGcTrace.validate(null, trace.getAbsolutePath());
            assertEquals("OK", status.getMessage());
            assertEquals(trace.getPath(), 0, status.getCode());
        }
    }

    /**
     * Test an invalid trace
     */
    @Test
    public void testValidateInvalidFile() {
        IStatus status = fGcTrace.validate(null, "src/org/eclipse/tracecompass/incubator/jifa/core/tests/gclog/badgc/not_a_gc.log");
        assertEquals(IStatus.INFO, status.getSeverity());
        assertEquals("Can not recognize file format. Please check if the file is a gc log.", status.getMessage());
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
        fGcTrace.initTrace(null, TRACE_LOCATION, ITmfEvent.class);
        assertEquals(TRACE_LOCATION, fGcTrace.getPath());
    }

    /**
     * Test Get Current Location
     */
    @Test
    public void testGetCurrentLocation() {
        ITmfLocation location = fGcTrace.getCurrentLocation();
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
        fGcTrace.initTrace(null, TRACE_LOCATION, null);
        assertNotNull(fGcTrace.seekEvent(new TmfLongLocation(0)));
    }

    /**
     * Test seeking at a ratio
     *
     * @throws TmfTraceException
     *             the trace failed to load
     */
    @Test
    public void testSeekEventWithRatio() throws TmfTraceException {
        fGcTrace.initTrace(null, TRACE_LOCATION, null);
        ITmfContext a = fGcTrace.seekEvent(new TmfLongLocation(0));
        ITmfContext b = fGcTrace.seekEvent(0.5);
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
        File root = new File("src/org/eclipse/tracecompass/incubator/jifa/core/tests/gclog/res/");
        for (File trace : root.listFiles()) {
            fGcTrace.dispose();
            fGcTrace = new GCTrace();
            fGcTrace.initTrace(null, trace.getAbsolutePath(), ITmfEvent.class);
            assertEquals(fGcTrace.getNbEvents(), 0L);
            fGcTrace.readEnd();
            if (!EMPTY_TRACES.contains(trace.getPath())) {
                assertNotEquals(trace.getPath(), fGcTrace.getNbEvents(), 0L);
            } else {
                assertEquals(trace.getPath(), fGcTrace.getNbEvents(), 0L);
            }
        }
    }
}