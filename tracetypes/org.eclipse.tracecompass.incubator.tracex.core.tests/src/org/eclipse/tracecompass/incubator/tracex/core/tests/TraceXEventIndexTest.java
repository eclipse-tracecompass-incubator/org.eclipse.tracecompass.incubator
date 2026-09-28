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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXEvent;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXEventIndex;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXFile;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXFormatException;
import org.junit.Test;

/** Tests logical ordering, seeking arithmetic, and index cancellation. */
public class TraceXEventIndexTest {

    /** Tests every possible partial-buffer boundary, including empty. */
    @Test
    public void testPartialBoundaries() throws Exception {
        for (int count = 0; count < 4; count++) {
            TraceXTestData.Fixture fixture = TraceXTestData.create(
                    ByteOrder.LITTLE_ENDIAN, 0x10000000L, 8, 1, 4, count);
            byte[] data = fixture.fileBytes();
            for (int physical = 0; physical < count; physical++) {
                fixture.putEvent(data, physical, 0x2000 + physical, 1,
                        100 + physical);
            }
            try (OpenedIndex opened = open(data)) {
                TraceXEventIndex index = opened.fIndex;
                assertEquals(count, index.getEventCount());
                for (int rank = 0; rank < count; rank++) {
                    assertEquals(rank, index.getPhysicalIndex(rank));
                }
            }
        }
    }

    /** Tests full circular buffers at every current-pointer position. */
    @Test
    public void testCircularCurrentPositions() throws Exception {
        int capacity = 4;
        for (int current = 0; current < capacity; current++) {
            TraceXTestData.Fixture fixture = TraceXTestData.create(
                    ByteOrder.BIG_ENDIAN, 0xE0000000L, 4, 0, capacity,
                    current);
            byte[] data = fixture.fileBytes();
            for (int physical = 0; physical < capacity; physical++) {
                fixture.putEvent(data, physical, 0x8000 + physical, 1,
                        physical < 2 ? 0xfffffff0L + physical : physical);
            }
            try (OpenedIndex opened = open(data)) {
                TraceXEventIndex index = opened.fIndex;
                assertEquals(capacity, index.getEventCount());
                for (int rank = 0; rank < capacity; rank++) {
                    assertEquals((current + rank) % capacity,
                            index.getPhysicalIndex(rank));
                }
            }
        }
    }

    /** Proves ordering ignores equal and wrapped timestamps and keeps offsets. */
    @Test
    public void testIndexedEventPosition() throws Exception {
        TraceXTestData.Fixture fixture = TraceXTestData.create(
                ByteOrder.LITTLE_ENDIAN, 0x70000000L, 8, 1, 4, 2);
        byte[] data = fixture.fileBytes();
        long[] timestamps = { 9, 9, 0xfffffffeL, 1 };
        for (int physical = 0; physical < timestamps.length; physical++) {
            fixture.putEvent(data, physical, 0x5000 + physical,
                    0x10001, timestamps[physical]);
        }
        try (OpenedIndex opened = open(data)) {
            TraceXEventIndex index = opened.fIndex;
            TraceXEvent first = index.readEvent(opened.fInput, 0);
            TraceXEvent last = index.readEvent(opened.fInput, 3);
            assertEquals(0xfffffffeL, first.getRawTimestamp());
            assertEquals(9, last.getRawTimestamp());
            assertEquals(0, first.getRelativeTicks().getAsLong());
            assertEquals(11, last.getRelativeTicks().getAsLong());
            assertEquals(11L,
                    last.getFields().get(TraceXEvent.FIELD_RELATIVE_TICKS));
            assertEquals(0, first.getLogicalRank().getAsLong());
            assertEquals(fixture.fEventStart + 2 * 32,
                    first.getPhysicalOffset().getAsLong());
            assertEquals(first.getLogicalRank().getAsLong(),
                    first.getFields().get(TraceXEvent.FIELD_LOGICAL_RANK));
            assertEquals(first.getPhysicalOffset().getAsLong(),
                    first.getFields().get(TraceXEvent.FIELD_PHYSICAL_OFFSET));
        }
    }

    /** Tests malformed gaps and a partial pointer inconsistent with its data. */
    @Test
    public void testMalformedPartialBuffers() throws Exception {
        TraceXTestData.Fixture gap = TraceXTestData.create(
                ByteOrder.LITTLE_ENDIAN, 0x1000, 4, 0, 3, 1);
        byte[] gapData = gap.fileBytes();
        gap.putEvent(gapData, 0, 1, 1, 1);
        gap.putEvent(gapData, 2, 2, 1, 2);
        expectFormatFailure(gapData, "follows an empty"); //$NON-NLS-1$

        TraceXTestData.Fixture pointer = TraceXTestData.create(
                ByteOrder.LITTLE_ENDIAN, 0x1000, 4, 0, 3, 2);
        byte[] pointerData = pointer.fileBytes();
        pointer.putEvent(pointerData, 0, 1, 1, 1);
        expectFormatFailure(pointerData, "first empty"); //$NON-NLS-1$
    }

    /** Tests clamped rank and ratio behavior at all special values. */
    @Test
    public void testSeekingArithmetic() throws Exception {
        TraceXTestData.Fixture fixture = TraceXTestData.create(
                ByteOrder.LITTLE_ENDIAN, 0x1000, 4, 0, 4, 0);
        byte[] data = fixture.fileBytes();
        for (int i = 0; i < 4; i++) {
            fixture.putEvent(data, i, i + 1, 1, i);
        }
        try (OpenedIndex opened = open(data)) {
            TraceXEventIndex index = opened.fIndex;
            assertEquals(0, index.clampRank(Long.MIN_VALUE));
            assertEquals(0, index.clampRank(0));
            assertEquals(3, index.clampRank(3));
            assertEquals(4, index.clampRank(4));
            assertEquals(4, index.clampRank(Long.MAX_VALUE));
            assertEquals(0, index.rankForRatio(Double.NaN));
            assertEquals(0, index.rankForRatio(Double.NEGATIVE_INFINITY));
            assertEquals(0, index.rankForRatio(-0.1));
            assertEquals(0, index.rankForRatio(0.0));
            assertEquals(2, index.rankForRatio(0.5));
            assertEquals(4, index.rankForRatio(1.0));
            assertEquals(4, index.rankForRatio(2.0));
            assertEquals(4, index.rankForRatio(Double.POSITIVE_INFINITY));
            expectRankFailure(index, -1);
            expectRankFailure(index, 4);
        }
    }

    /** Tests cancellation before and during the bounded physical scan. */
    @Test
    public void testCancellation() throws Exception {
        TraceXTestData.Fixture fixture = TraceXTestData.create(
                ByteOrder.LITTLE_ENDIAN, 0x1000, 4, 0, 4, 0);
        byte[] data = fixture.fileBytes();
        for (int i = 0; i < 4; i++) {
            fixture.putEvent(data, i, i + 1, 1, i);
        }
        Path path = Files.createTempFile("tracex-index-", ".trx"); //$NON-NLS-1$ //$NON-NLS-2$
        Files.write(path, data);
        try (TraceXFile input = TraceXFile.open(path)) {
            try {
                TraceXEventIndex.build(input, () -> true);
                fail("Expected cancellation"); //$NON-NLS-1$
            } catch (InterruptedIOException e) {
                assertTrue(e.getMessage().contains("cancelled")); //$NON-NLS-1$
            }
            AtomicInteger checks = new AtomicInteger();
            try {
                TraceXEventIndex.build(input,
                        () -> checks.incrementAndGet() == 3);
                fail("Expected cancellation during scan"); //$NON-NLS-1$
            } catch (InterruptedIOException e) {
                assertEquals(3, checks.get());
            }
            assertTrue(input.isOpen());
        } finally {
            Files.deleteIfExists(path);
        }
    }

    private static void expectRankFailure(TraceXEventIndex index, long rank) {
        try {
            index.getPhysicalIndex(rank);
            fail("Expected invalid rank"); //$NON-NLS-1$
        } catch (IllegalArgumentException e) {
            assertFalse(e.getMessage().isEmpty());
        }
    }

    private static void expectFormatFailure(byte[] data, String message)
            throws Exception {
        try (OpenedIndex ignored = open(data)) {
            fail("Expected malformed event area"); //$NON-NLS-1$
        } catch (TraceXFormatException e) {
            assertTrue(e.getMessage().contains(message));
        }
    }

    private static OpenedIndex open(byte[] data) throws IOException {
        return new OpenedIndex(data);
    }

    private static final class OpenedIndex implements AutoCloseable {

        private final Path fPath;
        private final TraceXFile fInput;
        private final TraceXEventIndex fIndex;

        private OpenedIndex(byte[] data) throws IOException {
            fPath = Files.createTempFile("tracex-index-", ".trx"); //$NON-NLS-1$ //$NON-NLS-2$
            Files.write(fPath, data);
            fInput = TraceXFile.open(fPath);
            try {
                fIndex = TraceXEventIndex.build(fInput);
            } catch (IOException | RuntimeException | Error e) {
                try {
                    fInput.close();
                } finally {
                    Files.deleteIfExists(fPath);
                }
                throw e;
            }
        }

        @Override
        public void close() throws IOException {
            try {
                fInput.close();
            } finally {
                Files.deleteIfExists(fPath);
            }
        }
    }
}
