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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.analysis.spanlife.SpanLifeAnalysis;
import org.eclipse.tracecompass.incubator.internal.opentracing.core.analysis.spanlife.SpanLifeStateProvider;
import org.eclipse.tracecompass.incubator.internal.otlp.core.trace.OtlpTrace;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test that the SpanLifeAnalysis correctly records span durations for an OTLP
 * trace.
 *
 * @author Matthew Khouzam
 */
public class OtlpSpanLifeTest {

    private static final String TRACE_PATH = "traces/hotrod-jaeger.json"; //$NON-NLS-1$

    private OtlpTrace fTrace;

    private static void deleteStateFiles(@NonNull OtlpTrace trace) {
        File suppDir = new File(TmfTraceManager.getSupplementaryFileDir(trace));
        File[] files = suppDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".ht")) { //$NON-NLS-1$
                    file.delete();
                }
            }
        }
    }

    /**
     * Setup: open the trace and fire the traceOpened signal so analysis modules
     * are registered.
     *
     * @throws TmfTraceException
     *             if the trace cannot be opened
     */
    @Before
    public void setUp() throws TmfTraceException {
        OtlpTrace trace = new OtlpTrace();
        trace.initTrace(null, TRACE_PATH, ITmfEvent.class);
        deleteStateFiles(trace);
        trace.traceOpened(new TmfTraceOpenedSignal(this, trace, null));
        fTrace = trace;
    }

    /**
     * Dispose the trace after each test.
     */
    @After
    public void tearDown() {
        OtlpTrace trace = fTrace;
        if (trace != null) {
            trace.dispose();
        }
    }

    /**
     * Load the OTLP trace, run SpanLifeAnalysis, and verify that each span in
     * the state system has a non-zero duration (endTime &gt; startTime for the
     * interval where the span is active).
     *
     * @throws AttributeNotFoundException
     *             if a state system attribute is missing
     * @throws StateSystemDisposedException
     *             if the state system is disposed
     */
    @Test
    public void testSpanDurations() throws AttributeNotFoundException, StateSystemDisposedException {
        OtlpTrace trace = fTrace;
        assertNotNull("Trace should not be null", trace); //$NON-NLS-1$

        // Find the SpanLifeAnalysis module registered for this trace
        SpanLifeAnalysis analysis = null;
        for (SpanLifeAnalysis module : TmfTraceUtils.getAnalysisModulesOfClass(trace, SpanLifeAnalysis.class)) {
            analysis = module;
            break;
        }
        assertNotNull("SpanLifeAnalysis should be available for OtlpTrace", analysis); //$NON-NLS-1$

        // Schedule the analysis and wait for completion
        analysis.schedule();
        assertTrue("SpanLifeAnalysis should complete successfully", analysis.waitForCompletion()); //$NON-NLS-1$

        // Get the state system built by the analysis
        ITmfStateSystem ss = analysis.getStateSystem();
        assertNotNull("State system should not be null after analysis completes", ss); //$NON-NLS-1$

        // Get top-level quarks (trace IDs)
        List<@NonNull Integer> traceIdQuarks = ss.getSubAttributes(ITmfStateSystem.ROOT_ATTRIBUTE, false);
        assertFalse("There should be at least one traceId in the state system", traceIdQuarks.isEmpty()); //$NON-NLS-1$

        // Use the first traceId
        int traceIdQuark = traceIdQuarks.get(0);
        String traceIdName = ss.getAttributeName(traceIdQuark);

        // Navigate to the openTracingSpans sub-attribute
        int openTracingQuark = ss.getQuarkRelative(traceIdQuark, SpanLifeStateProvider.OPEN_TRACING_ATTRIBUTE);

        // Collect all leaf span quarks recursively under openTracingSpans
        List<@NonNull Integer> spanQuarks = ss.getSubAttributes(openTracingQuark, true);
        assertFalse("There should be span quarks under openTracingSpans for traceId " + traceIdName, //$NON-NLS-1$
                spanQuarks.isEmpty());

        int spanCount = 0;
        long ssStart = ss.getStartTime();
        long ssEnd = ss.getCurrentEndTime();

        for (int quark : spanQuarks) {
            // Only check leaf quarks (actual span entries have no children)
            if (!ss.getSubAttributes(quark, false).isEmpty()) {
                continue;
            }

            // Query the full history of this quark to find the non-null interval
            List<ITmfStateInterval> intervals = StateSystemUtils.queryHistoryRange(ss, quark, ssStart, ssEnd);
            for (ITmfStateInterval interval : intervals) {
                if (interval.getValue() != null) {
                    // This is the active span interval
                    long start = interval.getStartTime();
                    long end = interval.getEndTime();
                    long duration = end - start;
                    String spanName = interval.getValue().toString();
                    String quarkPath = ss.getFullAttributePath(quark);

                    // Print for debugging
                    System.out.println("Span: " + spanName + //$NON-NLS-1$
                            " | path: " + quarkPath + //$NON-NLS-1$
                            " | start: " + start + //$NON-NLS-1$
                            " | end: " + end + //$NON-NLS-1$
                            " | duration: " + duration + " ns"); //$NON-NLS-1$ //$NON-NLS-2$

                    assertTrue("Span '" + spanName + "' at " + quarkPath + //$NON-NLS-1$ //$NON-NLS-2$
                            " should have non-zero duration, got " + duration, //$NON-NLS-1$
                            end > start);
                    spanCount++;
                }
            }
        }

        assertTrue("Should have found at least one active span interval, found " + spanCount, //$NON-NLS-1$
                spanCount > 0);
        System.out.println("Total spans with non-zero duration verified: " + spanCount); //$NON-NLS-1$
    }
}
