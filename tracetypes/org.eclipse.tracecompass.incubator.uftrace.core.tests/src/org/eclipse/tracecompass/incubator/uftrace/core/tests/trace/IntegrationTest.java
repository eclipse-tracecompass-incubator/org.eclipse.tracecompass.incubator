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

    private Uftrace uft;

    /**
     * Before
     */
    @Before
    public void before() {
        uft = new Uftrace();
    }

    /**
     * After
     */
    @After
    public void after() {
        uft.dispose();
        uft = null;
    }

    /**
     * Read the trace and check the events count
     *
     * @throws TmfTraceException
     *             something went wrong
     */
    @Test
    public void readTrace() throws TmfTraceException {
        assertNotNull(uft);
        uft.initTrace(null, "res/uftrace-ls", TmfEvent.class);
        ITmfContext ctx = uft.seekEvent(0);
        int cnt = 0;
        while (uft.getNext(ctx) != null) {
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
        assertNotNull(uft);
        uft.initTrace(null, "res/uftrace-foobar/uftrace.data", TmfEvent.class);
        ITmfContext ctx = uft.seekEvent(0);
        int cnt = 0;
        ITmfEvent event = null;
        HashSet<Object> tids = new HashSet<>();
        while ((event = uft.getNext(ctx)) != null) {
            cnt++;
            Object tid = TmfTraceUtils.resolveAspectOfNameForEvent(uft, "TID", event);
            tids.add(tid);

        }
        assertEquals("event count", 14, cnt);
        assertEquals("Tid count", 2, tids.size());
    }

}
