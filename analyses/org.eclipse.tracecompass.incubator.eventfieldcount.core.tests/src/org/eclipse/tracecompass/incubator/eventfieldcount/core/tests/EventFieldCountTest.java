/*******************************************************************************
 * Copyright (c) 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.eventfieldcount.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.fieldcount.core.FieldCountAnalysis;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiResultTable;
import org.eclipse.tracecompass.internal.provisional.analysis.lami.core.module.LamiTableEntry;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStub;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStubNs;
import org.junit.After;
import org.junit.Test;

public class EventFieldCountTest {

    private TmfXmlTraceStub fTrace;

    /**
     * Get the test trace
     *
     * @return The test trace, initialized
     */
    public ITmfTrace getTrace() {
        TmfXmlTraceStub trace = fTrace;
        if (trace == null) {
            trace = new TmfXmlTraceStubNs();
            IPath filePath = ActivatorTest.getAbsoluteFilePath("traceFiles/test.xml");
            IStatus status = trace.validate(null, filePath.toOSString());
            if (!status.isOK()) {
                fail(status.getException().getMessage());
            }
            try {
                trace.initTrace(null, filePath.toOSString(), TmfEvent.class);
            } catch (TmfTraceException e) {
                fail(e.getMessage());
            }
            trace.traceOpened(new TmfTraceOpenedSignal(this, trace, null));
            fTrace = trace;
        }
        return trace;
    }

    @After
    public void after() {
        if(fTrace != null) {
            fTrace.dispose();
        }
    }

    /**
     * Test with a trace
     * @throws CoreException
     */
    @Test
    public void test() throws CoreException {
        FieldCountAnalysis fca = new FieldCountAnalysis();
        Object retVal = fca.execute(getTrace(), TmfTimeRange.ETERNITY, "", new NullProgressMonitor());
        assertNotNull(retVal);
        assertTrue(retVal instanceof List);
        List<?> data = (List<?>) retVal;
        List<LamiResultTable> results = new ArrayList<>();
        for(Object datum: data) {
            assertTrue( datum instanceof LamiResultTable);
            results.add((LamiResultTable) datum);
        }
        assertEquals(1, results.size());
        LamiResultTable lrt = results.get(0);
        assertEquals("test", lrt.getName());
        List<@NonNull LamiTableEntry> entries = lrt.getEntries();
        assertEquals(2, entries.size());
        assertEquals("abc", entries.get(0).getValue(0).toString());
        assertEquals("2", entries.get(0).getValue(1).toString());
        assertEquals("def", entries.get(1).getValue(0).toString());
        assertEquals("2", entries.get(1).getValue(1).toString());
    }

}
