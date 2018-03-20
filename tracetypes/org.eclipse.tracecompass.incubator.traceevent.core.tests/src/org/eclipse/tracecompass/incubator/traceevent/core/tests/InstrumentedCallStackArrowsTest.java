/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.traceevent.core.tests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackEdge;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.callstack.TraceEventCallstackAnalysis;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.trace.TraceEventTrace;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
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
        ITmfTrace trace = new TraceEventTrace();
        TraceEventCallstackAnalysis analysis = new TraceEventCallstackAnalysis();
        try {
            trace.initTrace(null, TRACE_PATH, ITmfEvent.class);

            /*
             * Overcome the default start time which is at Long.MIN_VALUE
             */
            ITmfContext ctx = trace.seekEvent(0L);
            trace.getNext(ctx);

            analysis.setTrace(trace);
            analysis.schedule();
            analysis.waitForCompletion();

            List<CallStackEdge> expected = readEdges("resources/expectedArrows.csv");

            // copy the segment store into a list for easier comparison
            ISegmentStore<@NonNull CallStackEdge> links = analysis.getLinks();
            List<CallStackEdge> actual = new ArrayList<>();
            links.forEach(actual::add);

            assertEquals(expected, actual);
        } finally {
            analysis.dispose();
            trace.dispose();
        }
    }

    private static List<CallStackEdge> readEdges(String path) throws IOException {
        List<String> expectedStrings = Files.readAllLines(Paths.get(path));
        List<CallStackEdge> edges = new ArrayList<>(expectedStrings.size());

        for (String string : expectedStrings) {

            String[] split = string.split(",");
            long start = Long.parseLong(split[0]);
            long duration = Long.parseLong(split[1]);
            String srcHost = Objects.requireNonNull(split[2]);
            int srcTid = Integer.parseInt(split[3]);
            String dstHost = Objects.requireNonNull(split[4]);
            int dstTid = Integer.parseInt(split[5]);
            int id = Integer.parseInt(split[6]);

            edges.add(new CallStackEdge(new HostThread(srcHost, srcTid), new HostThread(dstHost, dstTid), start, duration, id));
        }

        return edges;
    }

}
