/*******************************************************************************
 * Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.tracex.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXDataSource;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXFile;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;
import org.junit.Test;

/**
 * Tests Trace Compass validation, properties, and input ownership integration.
 */
public class TraceXTraceTest {

    /** Tests all validation outcomes and repeated validation. */
    @Test
    public void testValidation() throws IOException {
        TraceXTrace trace = new TraceXTrace();
        assertFalse(trace.validate(null, null).isOK());
        assertFalse(trace.validate(null, "missing-tracex-test-file").isOK()); //$NON-NLS-1$
        assertFalse(trace.validate(null, System.getProperty("java.io.tmpdir")).isOK()); //$NON-NLS-1$

        Path valid = writeTemp(TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN).fileBytes());
        Path invalid = writeTemp(new byte[48]);
        try {
            for (int i = 0; i < 2; i++) {
                IStatus status = trace.validate(null, valid.toString());
                assertTrue(status.toString(), status.isOK());
            }
            IStatus invalidStatus = trace.validate(null, invalid.toString());
            assertFalse(invalidStatus.isOK());
            assertTrue(invalidStatus.getMessage().contains("magic mismatch")); //$NON-NLS-1$

            TraceXTrace ioTrace = new FailingOpenTrace();
            try {
                IStatus ioStatus = ioTrace.validate(null, valid.toString());
                assertFalse(ioStatus.isOK());
                assertEquals("I/O error validating TraceX trace", ioStatus.getMessage()); //$NON-NLS-1$
            } finally {
                ioTrace.dispose();
            }

            TraceXTrace closeTrace = new CloseFailingTrace();
            try {
                IStatus closeStatus = closeTrace.validate(null, valid.toString());
                assertFalse(closeStatus.isOK());
                assertEquals("I/O error validating TraceX trace", closeStatus.getMessage()); //$NON-NLS-1$
            } finally {
                closeTrace.dispose();
            }
        } finally {
            trace.dispose();
            Files.deleteIfExists(valid);
            Files.deleteIfExists(invalid);
        }
    }

    /** Tests trace initialization and stable validated properties. */
    @Test
    public void testInitializationAndProperties() throws Exception {
        TraceXTrace trace = new TraceXTrace();
        assertTrue(trace.getProperties().isEmpty());
        expectInitFailure(trace, null);

        Path invalid = writeTemp(new byte[48]);
        Path valid = writeTemp(TraceXTestData.standard(ByteOrder.BIG_ENDIAN).fileBytes());
        try {
            expectInitFailure(new TraceXTrace(), invalid.toString());
            expectInitFailure(new FailingOpenTrace(), valid.toString());

            trace.initTrace(null, valid.toString(), null);
            Map<String, String> properties = trace.getProperties();
            assertEquals("BIG_ENDIAN", properties.get("byte_order")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("0xF0000000", properties.get("base_address")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("2", properties.get("event_capacity")); //$NON-NLS-1$ //$NON-NLS-2$
            assertTrue(trace.getCurrentLocation().toString().contains("96")); //$NON-NLS-1$
            assertEquals(0.0, trace.getLocationRatio(null), 0.0);
            assertTrue(trace.seekEvent(0.5).toString().contains("96")); //$NON-NLS-1$
            assertNull(trace.parseEvent(null));
        } finally {
            trace.dispose();
            trace.dispose();
            Files.deleteIfExists(valid);
            Files.deleteIfExists(invalid);
        }
    }

    /** Tests that a close failure during disposal is contained. */
    @Test
    public void testDisposeCloseFailure() throws Exception {
        Path placeholder = writeTemp(TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN).fileBytes());
        TraceXTrace trace = new CloseFailingTrace();
        try {
            trace.initTrace(null, placeholder.toString(), null);
        } finally {
            trace.dispose();
            trace.dispose();
            Files.deleteIfExists(placeholder);
        }
    }

    private static void expectInitFailure(TraceXTrace trace, String path) {
        try {
            trace.initTrace(null, path, null);
            fail("Expected TmfTraceException"); //$NON-NLS-1$
        } catch (Exception e) {
            assertTrue(e.getMessage().contains(path == null ? "No path" : "Failed to parse")); //$NON-NLS-1$ //$NON-NLS-2$
        } finally {
            trace.dispose();
        }
    }

    private static Path writeTemp(byte[] data) throws IOException {
        Path path = Files.createTempFile("tracex-", ".trx"); //$NON-NLS-1$ //$NON-NLS-2$
        Files.write(path, data);
        return path;
    }

    private static class FailingOpenTrace extends TraceXTrace {

        @Override
        protected TraceXFile openTrace(Path path) throws IOException {
            throw new IOException("synthetic open failure"); //$NON-NLS-1$
        }
    }

    private static final class CloseFailingTrace extends TraceXTrace {

        @Override
        protected TraceXFile openTrace(Path path) throws IOException {
            TraceXTestData.Fixture fixture = TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN);
            return TraceXFile.open(new CloseFailingSource(fixture));
        }
    }

    private static final class CloseFailingSource implements TraceXDataSource {

        private final byte[] fHeader;
        private final long fSize;
        private boolean fOpen = true;

        private CloseFailingSource(TraceXTestData.Fixture fixture) {
            fHeader = fixture.fHeader.clone();
            fSize = fixture.fFileSize;
        }

        @Override
        public long size() {
            return fSize;
        }

        @Override
        public int read(ByteBuffer destination, long position) {
            if (position >= fHeader.length) {
                return -1;
            }
            int count = Math.min(destination.remaining(), fHeader.length - (int) position);
            destination.put(fHeader, (int) position, count);
            return count;
        }

        @Override
        public boolean isOpen() {
            return fOpen;
        }

        @Override
        public void close() throws IOException {
            fOpen = false;
            throw new IOException("synthetic close failure"); //$NON-NLS-1$
        }
    }
}
