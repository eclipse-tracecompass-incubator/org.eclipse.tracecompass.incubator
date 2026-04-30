/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.perf.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tracecompass.incubator.internal.perf.core.PerfDataReader;
import org.eclipse.tracecompass.incubator.internal.perf.core.PerfFileHeader;
import org.eclipse.tracecompass.incubator.internal.perf.core.PerfRecord;
import org.eclipse.tracecompass.incubator.internal.perf.core.trace.PerfDataTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Smoke test that opens the bundled {@code perf.data} sample with both the
 * low-level {@link PerfDataReader} and the {@link PerfDataTrace}, checking
 * that the magic is accepted and that events flow.
 */
public class PerfDataTraceTest {

    /** Relative path to the sample perf.data file inside this bundle. */
    public static final String TRACE_PATH = "traces/perf.data"; //$NON-NLS-1$

    private PerfDataTrace fTrace;

    /**
     * @throws Exception
     *             unused
     */
    @Before
    public void before() throws Exception {
        fTrace = new PerfDataTrace();
    }

    /**
     * @throws Exception
     *             unused
     */
    @After
    public void after() throws Exception {
        if (fTrace != null) {
            fTrace.dispose();
            fTrace = null;
        }
    }

    /**
     * Validate the bundled file as a perf.data trace.
     */
    @Test
    public void validate() {
        File file = resolveTrace();
        PerfDataTrace trace = fTrace;
        assertNotNull(trace);
        IStatus status = trace.validate(null, file.getAbsolutePath());
        assertTrue("validation should succeed, was: " + status, status.isOK()); //$NON-NLS-1$
    }

    /**
     * Parse the file with the raw {@link PerfDataReader} and check the
     * header and at least one decoded record.
     *
     * @throws IOException
     *             on I/O error
     */
    @Test
    public void readerSmoke() throws IOException {
        File file = resolveTrace();
        try (PerfDataReader reader = new PerfDataReader(file)) {
            PerfFileHeader header = reader.getHeader();
            assertEquals("magic", 0x32454c4946524550L, header.getMagic()); //$NON-NLS-1$
            assertTrue("attrs present", !reader.getAttrs().isEmpty()); //$NON-NLS-1$
            long off = reader.getDataOffset();
            PerfRecord record = reader.readRecordAt(off);
            assertNotNull("first record", record); //$NON-NLS-1$
            assertTrue("record size > 0", record.getSize() > 8); //$NON-NLS-1$
        }
    }

    /**
     * Walk every record and check that MMAP2 records decode into the
     * expected fields (prot, flags, filename) and that most of them carry
     * a non-zero timestamp through the trailing sample_id block. This
     * guards against a past bug where the MMAP2 union was read as 32
     * bytes instead of the correct 24, shifting prot/flags into the
     * filename and zeroing out the trailer.
     *
     * @throws IOException
     *             on I/O error
     */
    @Test
    public void mmap2DecodesFilenameAndTimestamp() throws IOException {
        File file = resolveTrace();
        try (PerfDataReader reader = new PerfDataReader(file)) {
            long off = reader.getDataOffset();
            long end = off + reader.getDataSize();
            int mmap2 = 0;
            int mmap2WithTs = 0;
            while (off < end) {
                PerfRecord rec = reader.readRecordAt(off);
                if (rec == null) {
                    break;
                }
                if (rec.getType() == 10 /* PERF_RECORD_MMAP2 */) {
                    mmap2++;
                    assertNotNull("filename", rec.getField("filename")); //$NON-NLS-1$ //$NON-NLS-2$
                    assertTrue("filename non-empty", //$NON-NLS-1$
                            !((String) rec.getField("filename")).isEmpty()); //$NON-NLS-1$
                    // prot should fit in a few bits (e.g. PROT_READ|WRITE|EXEC).
                    Object prot = rec.getField("prot"); //$NON-NLS-1$
                    assertTrue("prot must be a small integer", //$NON-NLS-1$
                            prot instanceof Integer && (Integer) prot >= 0 && (Integer) prot < 32);
                    if (rec.getTimestamp() != 0L) {
                        mmap2WithTs++;
                    }
                }
                off += rec.getSize();
            }
            assertTrue("expected at least one MMAP2", mmap2 > 0); //$NON-NLS-1$
            assertTrue("most MMAP2 records should carry a timestamp, " //$NON-NLS-1$
                    + mmap2WithTs + "/" + mmap2, //$NON-NLS-1$
                    mmap2WithTs * 2 >= mmap2);
        }
    }

    /**
     * Read events from the trace and check that the count is positive.
     *
     * @throws TmfTraceException
     *             if the trace fails to open
     */
    @Test
    public void readTraceEvents() throws TmfTraceException {
        File file = resolveTrace();
        PerfDataTrace trace = fTrace;
        assertNotNull(trace);
        trace.initTrace(null, file.getAbsolutePath(), TmfEvent.class);
        ITmfContext ctx = trace.seekEvent(0);
        assertNotNull(ctx);
        int count = 0;
        ITmfEvent event;
        while ((event = trace.getNext(ctx)) != null) {
            assertNotNull(event.getType());
            count++;
            if (count >= 100_000) {
                break; // guard against pathological loops
            }
        }
        assertTrue("expected at least one event, got " + count, count > 0); //$NON-NLS-1$
    }

    private static File resolveTrace() {
        // When running inside the OSGi runtime, the bundle activator resolves
        // resource URLs. Outside of it (plain mvn-surefire), walk up from the
        // current working directory to locate the bundle folder.
        File direct = new File(TRACE_PATH);
        if (direct.isFile()) {
            return direct;
        }
        File bundleRel = new File("org.eclipse.tracecompass.incubator.perf.core.tests", TRACE_PATH); //$NON-NLS-1$
        if (bundleRel.isFile()) {
            return bundleRel;
        }
        File fallback = new File("../org.eclipse.tracecompass.incubator.perf.core.tests", TRACE_PATH); //$NON-NLS-1$
        return fallback;
    }
}
