/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.uftrace.core.tests.trace;

import static org.junit.Assert.assertEquals;

import org.eclipse.tracecompass.incubator.internal.uftrace.core.trace.Uftrace;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.junit.Test;

/**
 * Full test for a UFTrace
 *
 * @author Matthew Khouzam
 */
public class IntegrationTest {

    /**
     * Read the trace and check the events count
     *
     * @throws TmfTraceException
     *             something went wrong
     */
    @Test
    public void readTrace() throws TmfTraceException {
        Uftrace uft = new Uftrace();
        uft.initTrace(null, "res/uftrace-ls", TmfEvent.class);
        ITmfContext ctx = uft.seekEvent(0);
        int cnt = 0;
        while (uft.getNext(ctx) != null) {
            cnt++;
        }
        assertEquals("event count", 113751, cnt);
    }

}
