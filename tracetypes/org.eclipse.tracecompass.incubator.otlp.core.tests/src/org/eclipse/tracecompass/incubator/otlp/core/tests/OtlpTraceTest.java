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
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.otlp.core.trace.OtlpTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.junit.Test;

/**
 * Test reading OTLP traces
 *
 * @author Matthew Khouzam
 */
public class OtlpTraceTest {

    private static final String TRACE_PATH = "traces/hotrod-jaeger.json"; //$NON-NLS-1$
    private static final String LARGE_TRACE_PATH = "traces/otel-spans-large.json"; //$NON-NLS-1$
    private static final int NB_EVENTS = 79;
    private static final int NB_EVENTS_LARGE = 600;

    /**
     * Test that the trace validates as OTLP
     */
    @Test
    public void testValidate() {
        OtlpTrace trace = new OtlpTrace();
        try {
            IStatus status = trace.validate(null, TRACE_PATH);
            assertTrue("Trace should validate: " + status.getMessage(), status.isOK()); //$NON-NLS-1$
        } finally {
            trace.dispose();
        }
    }

    /**
     * Test reading all events from the trace
     *
     * @throws TmfTraceException
     *             if the trace cannot be opened
     */
    @Test
    public void testReadTrace() throws TmfTraceException {
        OtlpTrace trace = new OtlpTrace();
        try {
            trace.initTrace(null, TRACE_PATH, ITmfEvent.class);
            ITmfContext context = trace.seekEvent(0.0);
            ITmfEvent event = trace.getNext(context);
            long count = 0;
            long prevTs = -1;
            while (event != null) {
                count++;
                @NonNull ITmfTimestamp timestamp = event.getTimestamp();
                assertNotNull("Event has a timestamp", timestamp); //$NON-NLS-1$
                assertTrue("Monotonic events", timestamp.toNanos() >= prevTs); //$NON-NLS-1$
                prevTs = timestamp.toNanos();
                event = trace.getNext(context);
            }
            assertEquals(NB_EVENTS, count);
            assertEquals(NB_EVENTS, trace.getNbEvents());
        } finally {
            trace.dispose();
        }
    }

    /**
     * Test reading all events from the large trace
     *
     * @throws TmfTraceException
     *             if the trace cannot be opened
     */
    @Test
    public void testReadLargeTrace() throws TmfTraceException {
        OtlpTrace trace = new OtlpTrace();
        try {
            IStatus status = trace.validate(null, LARGE_TRACE_PATH);
            assertTrue("Trace should validate: " + status.getMessage(), status.isOK()); //$NON-NLS-1$
            trace.initTrace(null, LARGE_TRACE_PATH, ITmfEvent.class);
            ITmfContext context = trace.seekEvent(0.0);
            ITmfEvent event = trace.getNext(context);
            long count = 0;
            long prevTs = -1;
            while (event != null) {
                count++;
                @NonNull ITmfTimestamp timestamp = event.getTimestamp();
                assertNotNull("Event has a timestamp", timestamp); //$NON-NLS-1$
                assertTrue("Monotonic events", timestamp.toNanos() >= prevTs); //$NON-NLS-1$
                prevTs = timestamp.toNanos();
                event = trace.getNext(context);
            }
            assertEquals(NB_EVENTS_LARGE, count);
            assertEquals(NB_EVENTS_LARGE, trace.getNbEvents());
        } finally {
            trace.dispose();
        }
    }
}
