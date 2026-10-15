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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXDataSource;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXEvent;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXFile;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;
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
        Path valid = writeTemp(oneEventData(ByteOrder.BIG_ENDIAN));
        try {
            expectInitFailure(new TraceXTrace(), invalid.toString());
            expectInitFailure(new FailingOpenTrace(), valid.toString());

            trace.initTrace(null, valid.toString(), null);
            Map<String, String> properties = trace.getProperties();
            assertEquals("BIG_ENDIAN", properties.get("byte_order")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("0xF0000000", properties.get("base_address")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("2", properties.get("event_capacity")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("1", properties.get("event_count")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("partial", properties.get("event_buffer_status")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("1", properties.get("registry_record_count")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("0", properties.get("registry_valid_count")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("0", properties.get("registry_in_use_count")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("0", properties.get("registry_diagnostic_count")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("oldest-to-newest", properties.get("event_order")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(Long.valueOf(0),
                    ((TmfLongLocation) trace.getCurrentLocation()).getLocationInfo());
            assertEquals(0.0, trace.getLocationRatio(null), 0.0);
            assertEquals(0, trace.seekEvent(0.5).getRank());
            assertNull(trace.parseEvent(null));
            ITmfContext context = trace.seekEvent((TmfLongLocation) null);
            ITmfEvent event = trace.getNext(context);
            assertNotNull(event);
            assertEquals(0, event.getRank());
            assertEquals(1, context.getRank());
            assertEquals(1, trace.size());
            assertEquals(1, trace.progress());
            assertEquals(1.0, trace.getLocationRatio(context.getLocation()), 0.0);
            assertNull(trace.getNext(context));
        } finally {
            trace.dispose();
            trace.dispose();
            Files.deleteIfExists(valid);
            Files.deleteIfExists(invalid);
        }
    }

    /** Tests rank/ratio seeking and independent interleaved contexts. */
    @Test
    public void testSeekingAndConcurrentContexts() throws Exception {
        TraceXTestData.Fixture fixture = TraceXTestData.create(
                ByteOrder.LITTLE_ENDIAN, 0x80000000L, 8, 1, 4, 2);
        byte[] data = fixture.fileBytes();
        long[] timestamps = { 30, 40, 10, 20 };
        for (int physical = 0; physical < timestamps.length; physical++) {
            fixture.putEvent(data, physical, 0x1000 + physical, 1,
                    timestamps[physical]);
        }
        Path path = writeTemp(data);
        TraceXTrace trace = new TraceXTrace();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            trace.initTrace(null, path.toString(), null);
            assertEquals(4, trace.getNbEvents());
            assertEquals(0, trace.seekEvent(Double.NaN).getRank());
            assertEquals(0, trace.seekEvent(Double.NEGATIVE_INFINITY).getRank());
            assertEquals(0, trace.seekEvent(-1).getRank());
            assertEquals(2, trace.seekEvent(0.5).getRank());
            assertEquals(4, trace.seekEvent(1.0).getRank());
            assertEquals(4, trace.seekEvent(Double.POSITIVE_INFINITY).getRank());
            assertEquals(0, trace.seekEvent(new TmfLongLocation(-1)).getRank());
            assertEquals(3, trace.seekEvent(new TmfLongLocation(3)).getRank());
            assertEquals(4, trace.seekEvent(new TmfLongLocation(5)).getRank());

            for (int iteration = 0; iteration < 100; iteration++) {
                ITmfContext first = trace.seekEvent(new TmfLongLocation(0));
                ITmfContext second = trace.seekEvent(new TmfLongLocation(2));
                CountDownLatch start = new CountDownLatch(1);
                Callable<long[]> firstRead = () -> readTwo(trace, first, start);
                Callable<long[]> secondRead = () -> readTwo(trace, second, start);
                Future<long[]> firstResult = executor.submit(firstRead);
                Future<long[]> secondResult = executor.submit(secondRead);
                start.countDown();
                assertArrayEquals(new long[] { 10, 20 },
                        firstResult.get(5, TimeUnit.SECONDS));
                assertArrayEquals(new long[] { 30, 40 },
                        secondResult.get(5, TimeUnit.SECONDS));
                assertEquals(2, first.getRank());
                assertEquals(4, second.getRank());
            }

            ITmfContext repeated = trace.seekEvent(new TmfLongLocation(1));
            assertEquals(20, rawEvent(trace.getNext(repeated)).getRawTimestamp());
            repeated = trace.seekEvent(new TmfLongLocation(1));
            assertEquals(20, rawEvent(trace.getNext(repeated)).getRawTimestamp());
            trace.dispose();
            assertNull(trace.parseEvent(repeated));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            trace.dispose();
            Files.deleteIfExists(path);
        }
    }

    /** Tests cancellation closes a partially initialized owned source. */
    @Test
    public void testInitializationCancellation() throws Exception {
        TraceXTestData.Fixture fixture = TraceXTestData.standard(
                ByteOrder.LITTLE_ENDIAN);
        byte[] data = fixture.fileBytes();
        fixture.putEvent(data, 0, 0x1000, 1, 1);
        ByteArraySource source = new ByteArraySource(data, false);
        TraceXTrace trace = new CancelledTrace(source);
        Path placeholder = writeTemp(data);
        try {
            expectInitFailure(trace, placeholder.toString());
            assertFalse(source.isOpen());
        } finally {
            trace.dispose();
            Files.deleteIfExists(placeholder);
        }
    }

    /** Tests cancellation during iteration without consuming the context. */
    @Test
    public void testIterationCancellation() throws Exception {
        byte[] data = oneEventData(ByteOrder.LITTLE_ENDIAN);
        Path path = writeTemp(data);
        AtomicBoolean cancelled = new AtomicBoolean();
        ToggleCancellationTrace trace = new ToggleCancellationTrace(cancelled);
        try {
            trace.initTrace(null, path.toString(), null);
            ITmfContext context = trace.seekEvent(0);
            cancelled.set(true);
            assertNull(trace.getNext(context));
            assertEquals(0, context.getRank());
            cancelled.set(false);
            assertEquals(1, rawEvent(trace.getNext(context)).getRawTimestamp());
        } finally {
            trace.dispose();
            Files.deleteIfExists(path);
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
            assertTrue(e.getMessage().contains(path == null ? "No path" : "Failed to initialize")); //$NON-NLS-1$ //$NON-NLS-2$
        } finally {
            trace.dispose();
        }
    }

    private static Path writeTemp(byte[] data) throws IOException {
        Path path = Files.createTempFile("tracex-", ".trx"); //$NON-NLS-1$ //$NON-NLS-2$
        Files.write(path, data);
        return path;
    }

    private static byte[] oneEventData(ByteOrder order) {
        TraceXTestData.Fixture fixture = TraceXTestData.standard(order);
        byte[] data = fixture.fileBytes();
        fixture.putEvent(data, 0, 0x1000, 1, 1);
        return data;
    }

    private static long[] readTwo(TraceXTrace trace, ITmfContext context,
            CountDownLatch start) throws Exception {
        assertTrue(start.await(5, TimeUnit.SECONDS));
        return new long[] {
                rawEvent(trace.getNext(context)).getRawTimestamp(),
                rawEvent(trace.getNext(context)).getRawTimestamp() };
    }

    private static TraceXEvent rawEvent(ITmfEvent event) {
        return (TraceXEvent) event.getContent().getValue();
    }

    private static void assertArrayEquals(long[] expected, long[] actual) {
        org.junit.Assert.assertArrayEquals(expected, actual);
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

        private final ByteArraySource fDelegate;

        private CloseFailingSource(TraceXTestData.Fixture fixture) {
            byte[] data = fixture.fileBytes();
            fixture.putEvent(data, 0, 0x1000, 1, 1);
            fDelegate = new ByteArraySource(data, true);
        }

        @Override
        public long size() {
            return fDelegate.size();
        }

        @Override
        public int read(ByteBuffer destination, long position) {
            return fDelegate.read(destination, position);
        }

        @Override
        public boolean isOpen() {
            return fDelegate.isOpen();
        }

        @Override
        public void close() throws IOException {
            fDelegate.close();
            throw new IOException("synthetic close failure"); //$NON-NLS-1$
        }
    }

    private static class ByteArraySource implements TraceXDataSource {

        private final byte[] fData;
        private final boolean fPartialReads;
        private boolean fOpen = true;

        private ByteArraySource(byte[] data, boolean partialReads) {
            fData = data.clone();
            fPartialReads = partialReads;
        }

        @Override
        public long size() {
            return fData.length;
        }

        @Override
        public int read(ByteBuffer destination, long position) {
            if (!fOpen || position >= fData.length) {
                return -1;
            }
            int count = Math.min(destination.remaining(),
                    fData.length - (int) position);
            if (fPartialReads) {
                count = Math.min(count, 3);
            }
            destination.put(fData, (int) position, count);
            return count;
        }

        @Override
        public boolean isOpen() {
            return fOpen;
        }

        @Override
        public void close() {
            fOpen = false;
        }
    }

    private static final class CancelledTrace extends TraceXTrace {

        private final TraceXDataSource fSource;

        private CancelledTrace(TraceXDataSource source) {
            fSource = source;
        }

        @Override
        protected TraceXFile openTrace(Path path) throws IOException {
            return TraceXFile.open(fSource);
        }

        @Override
        protected boolean isCancelled() {
            return true;
        }
    }

    private static final class ToggleCancellationTrace extends TraceXTrace {

        private final AtomicBoolean fCancelled;

        private ToggleCancellationTrace(AtomicBoolean cancelled) {
            fCancelled = cancelled;
        }

        @Override
        protected boolean isCancelled() {
            return fCancelled.get();
        }
    }
}
