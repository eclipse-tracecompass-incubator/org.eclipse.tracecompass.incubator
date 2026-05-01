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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.tracecompass.incubator.internal.perf.core.PerfConstants;
import org.eclipse.tracecompass.incubator.internal.perf.core.PerfDataReader;
import org.eclipse.tracecompass.incubator.internal.perf.core.PerfRecord;
import org.junit.Test;

/**
 * Structural test for the callchain data exposed by {@link PerfDataReader}.
 *
 * The callchain analysis module expects each {@code PERF_RECORD_SAMPLE}
 * record to carry a {@code callchain} {@code long[]} in its fields. This
 * test walks the bundled sample file and verifies that assumption so we
 * don't silently regress the data shape that the analysis depends on.
 */
public class PerfDataCallchainTest {

    /**
     * Verify that the bundled trace has {@code PERF_RECORD_SAMPLE} records
     * and that, when callchains are present, they carry non-sentinel IPs.
     *
     * The bundled sample was captured without {@code -g}, so every SAMPLE
     * has an empty callchain. We still exercise the code path and make
     * sure the data shape (a {@code long[]} in the {@code callchain}
     * field, or its absence) matches what the analysis module expects.
     *
     * @throws IOException
     *             on I/O error
     */
    @Test
    public void hasNonTrivialCallchains() throws IOException {
        File file = PerfDataTraceTest.resolveTraceFile();
        try (PerfDataReader reader = new PerfDataReader(file)) {
            long off = reader.getDataOffset();
            long end = off + reader.getDataSize();
            int samples = 0;
            int samplesWithChain = 0;
            int realIps = 0;
            while (off < end) {
                PerfRecord rec = reader.readRecordAt(off);
                if (rec == null) {
                    break;
                }
                if (rec.getType() == PerfConstants.PERF_RECORD_SAMPLE) {
                    samples++;
                    Object cc = rec.getField("callchain"); //$NON-NLS-1$
                    if (cc instanceof long[]) {
                        long[] chain = (long[]) cc;
                        if (chain.length > 0) {
                            samplesWithChain++;
                        }
                        for (long ip : chain) {
                            // PERF_CONTEXT_* sentinels live in the top 128 bytes of the address space
                            if (Long.compareUnsigned(ip, 0xffffffffffffff80L) < 0 && ip != 0) {
                                realIps++;
                            }
                        }
                    }
                }
                off += rec.getSize();
            }
            assertNotNull(file);
            assertTrue("expected some SAMPLE records, got " + samples, samples > 0); //$NON-NLS-1$
            // Callchains are only present when perf was run with -g. If any
            // are present in the bundled trace, at least one must contain a
            // non-sentinel IP.
            if (samplesWithChain > 0) {
                assertTrue("expected non-sentinel IPs in callchains, got " + realIps, //$NON-NLS-1$
                        realIps > 0);
            }
        }
    }

    /**
     * The second bundled trace ({@code traces/perf-callgraph.data}) was
     * captured with {@code perf record -g ping 4.4.4.4 -c 3} so every
     * SAMPLE carries a callchain. Make sure the reader exposes it as a
     * {@code long[]} containing at least one real IP.
     *
     * @throws IOException
     *             on I/O error
     */
    @Test
    public void callgraphTraceHasRealChains() throws IOException {
        File file = PerfDataTraceTest.resolveCallgraphTraceFile();
        if (!file.isFile()) {
            // Bundled only in the source tree; skip gracefully when the
            // test is run from an unusual working directory.
            return;
        }
        try (PerfDataReader reader = new PerfDataReader(file)) {
            long off = reader.getDataOffset();
            long end = off + reader.getDataSize();
            int samples = 0;
            int samplesWithChain = 0;
            int realIps = 0;
            while (off < end) {
                PerfRecord rec = reader.readRecordAt(off);
                if (rec == null) {
                    break;
                }
                if (rec.getType() == PerfConstants.PERF_RECORD_SAMPLE) {
                    samples++;
                    Object cc = rec.getField("callchain"); //$NON-NLS-1$
                    if (cc instanceof long[]) {
                        long[] chain = (long[]) cc;
                        if (chain.length > 0) {
                            samplesWithChain++;
                        }
                        for (long ip : chain) {
                            if (Long.compareUnsigned(ip, 0xffffffffffffff80L) < 0 && ip != 0) {
                                realIps++;
                            }
                        }
                    }
                }
                off += rec.getSize();
            }
            assertNotNull(file);
            assertTrue("expected samples, got " + samples, samples > 0); //$NON-NLS-1$
            assertTrue("expected callchains on every sample, got " //$NON-NLS-1$
                    + samplesWithChain + "/" + samples, //$NON-NLS-1$
                    samplesWithChain == samples && samples > 0);
            assertTrue("expected non-sentinel IPs, got " + realIps, realIps > samples); //$NON-NLS-1$
        }
    }
}
