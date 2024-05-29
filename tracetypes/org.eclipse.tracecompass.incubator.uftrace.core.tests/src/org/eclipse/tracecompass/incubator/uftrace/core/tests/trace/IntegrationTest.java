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

package org.eclipse.tracecompass.incubator.uftrace.core.tests.trace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;

import org.eclipse.tracecompass.incubator.internal.uftrace.core.trace.Uftrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestampFormat;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Full test for a UFTrace
 *
 * @author Matthew Khouzam
 */
public class IntegrationTest {

    private Uftrace fUft;

    /**
     * Before
     */
    @Before
    public void before() {
        fUft = new Uftrace();
    }

    /**
     * After
     */
    @After
    public void after() {
        fUft.dispose();
        fUft = null;
    }

    /**
     * Read the trace and check the events count
     *
     * @throws TmfTraceException
     *             something went wrong
     */
    @Test
    public void readTrace() throws TmfTraceException {
        assertNotNull(fUft);
        fUft.initTrace(null, "res/uftrace-ls", TmfEvent.class);
        ITmfContext ctx = fUft.seekEvent(0);
        int cnt = 0;
        while (fUft.getNext(ctx) != null) {
            cnt++;
        }
        assertEquals("event count", 113751, cnt);
    }

    /**
     * Read the trace and check the events count as well as the TIDs
     *
     * @throws TmfTraceException
     *             something went wrong
     */
    @Test
    public void readFoobarTrace() throws TmfTraceException {
        Uftrace uftrace = fUft;
        assertNotNull(uftrace);
        uftrace.initTrace(null, "res/uftrace-foobar/uftrace.data", TmfEvent.class);
        ITmfContext ctx = uftrace.seekEvent(0);
        int cnt = 0;
        ITmfEvent event = null;
        HashSet<Object> tids = new HashSet<>();
        while ((event = uftrace.getNext(ctx)) != null) {
            cnt++;
            Object tid = TmfTraceUtils.resolveAspectOfNameForEvent(uftrace, "TID", event);
            tids.add(tid);

        }
        assertEquals("event count", 14, cnt);
        assertEquals("Tid count", 2, tids.size());
    }

    /**
     * Read the trace and check the events count as well as the timestamp for
     * UTCness
     *
     * @throws TmfTraceException
     *             something went wrong
     */
    @Test
    public void readUtcOffsetTrace() throws TmfTraceException {
        assertNotNull(fUft);
        fUft.initTrace(null, "res/uftrace-utc/uftrace.data", TmfEvent.class);
        ITmfContext ctx = fUft.seekEvent(0);
        int cnt = 0;
        ITmfEvent event = null;
        ITmfTimestamp ts = TmfTimestamp.BIG_BANG;
        while ((event = fUft.getNext(ctx)) != null) {
            cnt++;
            ts = event.getTimestamp();
        }
        assertEquals("event count", 12038, cnt);
        assertNotNull(ts);
        assertEquals("Timestamp", "2024", ts.toString(new TmfTimestampFormat("yyyy")));
    }

}