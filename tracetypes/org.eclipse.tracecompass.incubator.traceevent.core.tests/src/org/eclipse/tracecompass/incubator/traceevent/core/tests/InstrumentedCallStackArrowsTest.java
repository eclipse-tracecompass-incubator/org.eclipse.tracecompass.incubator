/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.callstack.core.base.EdgeStateValue;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.callstack.TraceEventCallstackAnalysis;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.trace.TraceEventTrace;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.junit.Test;

/**
 * Test the links from the {@link TraceEventCallstackAnalysis}
 *
 * @author Loic Prieur-Drevon
 */
public class InstrumentedCallStackArrowsTest {
    /**
     * Trace compass trace with a small number (9) of arrows.
     */
    private static final String TRACE_PATH = "traces/tracecompassLog_small.json";

    /**
     * Trace compass trace with a small number (9) of arrows.
     */
    private static final String TRACE_PATH_SCOPE = "traces/flow_various.json";

    /**
     * Test that the edges returned by the {@link TraceEventCallstackAnalysis} are
     * the expected ones.
     *
     * @throws TmfTraceException
     *             If we couldn't open the trace
     * @throws TmfAnalysisException
     *             This exception should be thrown if the trace is set more than
     *             once
     * @throws IOException
     *             if an I/O error occurs reading from the file or a malformed or
     *             unmappable byte sequence is read
     */
    @Test
    public void testArrows() throws TmfTraceException, TmfAnalysisException, IOException {
        TraceEventTrace trace = new TraceEventTrace();
        TraceEventCallstackAnalysis analysis = new TraceEventCallstackAnalysis();
        try {
            trace.initTrace(null, TRACE_PATH, TmfEvent.class);

            /*
             * Overcome the default start time which is at Long.MIN_VALUE
             */
            ITmfContext ctx = trace.seekEvent(0L);
            trace.getNext(ctx);

            assertTrue(analysis.setTrace(trace));
            analysis.schedule();
            assertTrue(analysis.waitForCompletion());

            ITmfStateSystem ss = analysis.getStateSystem();
            assertNotNull(ss);

            List<@NonNull ITmfStateInterval> actual = analysis.getLinks(ss.getStartTime(), ss.getCurrentEndTime(), new NullProgressMonitor());

            assertEqualsEdges("resources/expectedArrows.csv", actual);
        } finally {
            analysis.dispose();
            trace.dispose();
        }
    }

    /**
     * Test that the edges returned by the {@link TraceEventCallstackAnalysis}
     * are the expected ones for a trace with events that have scope, cat and
     * id, or missing flow events
     *
     * @throws TmfTraceException
     *             If we couldn't open the trace
     * @throws TmfAnalysisException
     *             This exception should be thrown if the trace is set more than
     *             once
     * @throws IOException
     *             if an I/O error occurs reading from the file or a malformed
     *             or unmappable byte sequence is read
     */
    @Test
    public void testArrowsWithScopes() throws TmfTraceException, TmfAnalysisException, IOException {
        TraceEventTrace trace = new TraceEventTrace();
        TraceEventCallstackAnalysis analysis = new TraceEventCallstackAnalysis();
        try {
            trace.initTrace(null, TRACE_PATH_SCOPE, TmfEvent.class);

            /*
             * Overcome the default start time which is at Long.MIN_VALUE
             */
            ITmfContext ctx = trace.seekEvent(0L);
            trace.getNext(ctx);

            assertTrue(analysis.setTrace(trace));
            analysis.schedule();
            assertTrue(analysis.waitForCompletion());

            ITmfStateSystem ss = analysis.getStateSystem();
            assertNotNull(ss);

            List<@NonNull ITmfStateInterval> actual = analysis.getLinks(ss.getStartTime(), ss.getCurrentEndTime(), new NullProgressMonitor());

            assertEqualsEdges("resources/expectedScopeArrows.csv", actual);
        } finally {
            analysis.dispose();
            trace.dispose();
        }
    }

    private static void assertEqualsEdges(String path, List<@NonNull ITmfStateInterval> actual) throws IOException {
        List<String> expectedStrings = Files.readAllLines(Paths.get(path));
        /*
         * we could build intervals and compare the lists but that would require a
         * separate equals method for intervals to avoid comparing quarks...
         */
        assertEquals("wrong number of arrows", expectedStrings.size(), actual.size());

        Iterator<String> stringIterator = expectedStrings.iterator();
        Iterator<ITmfStateInterval> intervalIterator = actual.iterator();
        int i = 0;
        while (stringIterator.hasNext() && intervalIterator.hasNext()) {
            String[] split = stringIterator.next().split(",");
            ITmfStateInterval interval = intervalIterator.next();

            long start = Long.parseLong(split[0]);
            long end = Long.parseLong(split[1]);
            assertEquals("Wrong start time for arrow " + i, start, interval.getStartTime());
            assertEquals("Wrong duration for arrow " + i, end - start, interval.getEndTime() - interval.getStartTime() + 1);

            Object value = interval.getValue();
            assertTrue(value instanceof EdgeStateValue);
            EdgeStateValue edge = (EdgeStateValue) value;

            assertEquals("Wrong source host for arrow " + i, split[2], edge.getSource().getHost());
            Integer srcTid = Integer.parseInt(split[3]);
            assertEquals("Wrong source TID for arrow " + i, srcTid, edge.getSource().getTid());

            assertEquals("Wrong destination host for arrow " + i, Objects.requireNonNull(split[4]), edge.getDestination().getHost());
            Integer dstTid = Integer.parseInt(split[5]);
            assertEquals("Wrong destination TID for arrow " + i, dstTid, edge.getDestination().getTid());

            assertEquals("Wrong edge id for arrow " + i, Integer.parseInt(split[6]), edge.getId());
            i++;
        }
    }

}
