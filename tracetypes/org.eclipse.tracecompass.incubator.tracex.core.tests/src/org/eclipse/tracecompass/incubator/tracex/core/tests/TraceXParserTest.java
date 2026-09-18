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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Map;

import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXCheckedMath;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXControlHeader;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXDataSource;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXFile;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXFormatException;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXLayout;
import org.junit.Test;

/**
 * Exhaustive tests for TraceX control-header decoding and layout validation.
 */
public class TraceXParserTest {

    /** Tests equivalent valid layouts in both target byte orders. */
    @Test
    public void testEquivalentByteOrders() throws IOException {
        TraceXLayout little = parse(TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN));
        TraceXLayout big = parse(TraceXTestData.standard(ByteOrder.BIG_ENDIAN));

        assertSame(ByteOrder.LITTLE_ENDIAN, little.getByteOrder());
        assertSame(ByteOrder.BIG_ENDIAN, big.getByteOrder());
        assertEquals(little.getBaseAddress(), big.getBaseAddress());
        assertEquals(little.getTimerValidMask(), big.getTimerValidMask());
        assertEquals(little.getFileSize(), big.getFileSize());
        assertEquals(little.getRegistryStartOffset(), big.getRegistryStartOffset());
        assertEquals(little.getRegistryEndOffset(), big.getRegistryEndOffset());
        assertEquals(little.getRegistryEntrySize(), big.getRegistryEntrySize());
        assertEquals(little.getRegistryCapacity(), big.getRegistryCapacity());
        assertEquals(little.getEventStartOffset(), big.getEventStartOffset());
        assertEquals(little.getEventEndOffset(), big.getEventEndOffset());
        assertEquals(little.getEventCurrentOffset(), big.getEventCurrentOffset());
        assertEquals(little.getEventCapacity(), big.getEventCapacity());
    }

    /** Tests decoding of every raw header field as an unsigned value. */
    @Test
    public void testHeaderFields() throws TraceXFormatException {
        TraceXTestData.Fixture fixture = TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN);
        TraceXControlHeader header = TraceXControlHeader.decode(ByteBuffer.wrap(fixture.fHeader));
        assertSame(ByteOrder.LITTLE_ENDIAN, header.getByteOrder());
        assertEquals(TraceXTestData.TIMER_MASK, header.getTimerValidMask());
        assertEquals(fixture.fBase, header.getTraceBaseAddress());
        assertEquals(fixture.fBase + fixture.fRegistryStart, header.getRegistryStartAddress());
        assertEquals(0, header.getReserved1());
        assertEquals(32, header.getRegistryNameSize());
        assertEquals(fixture.fBase + fixture.fRegistryEnd, header.getRegistryEndAddress());
        assertEquals(fixture.fBase + fixture.fEventStart, header.getEventStartAddress());
        assertEquals(fixture.fBase + fixture.fEventEnd, header.getEventEndAddress());
        assertEquals(fixture.fBase + fixture.fEventCurrent, header.getEventCurrentAddress());
        assertEquals(0xAAAAAAAAL, header.getReserved2());
        assertEquals(0xBBBBBBBBL, header.getReserved3());
        assertEquals(0xCCCCCCCCL, header.getReserved4());
    }

    /** Tests every possible truncation point in the 48-byte header. */
    @Test
    public void testEveryHeaderTruncation() {
        byte[] header = TraceXTestData.standard(ByteOrder.BIG_ENDIAN).fHeader;
        for (int length = 0; length < TraceXControlHeader.SIZE; length++) {
            TrackingSource source = new TrackingSource(Arrays.copyOf(header, length), length);
            TraceXFormatException exception = expectFormat(() -> TraceXFile.open(source));
            assertTrue(exception.getMessage().startsWith("Truncated TraceX")); //$NON-NLS-1$
            assertFalse(source.isOpen());
        }
    }

    /** Tests wrong, reversed, and one-byte-near magic sequences. */
    @Test
    public void testMagicMismatches() {
        byte[][] badMagic = {
                { 0x54, 0x58, 0x42, 0x54 },
                { 0x54, 0x58, 0x54, 0x41 },
                { 0x41, 0x54, 0x58, 0x54 },
                { 0x42, 0x54, 0x58, 0x55 }
        };
        for (byte[] magic : badMagic) {
            byte[] header = TraceXTestData.standard(ByteOrder.BIG_ENDIAN).fHeader.clone();
            System.arraycopy(magic, 0, header, 0, magic.length);
            TraceXFormatException exception = expectFormat(
                    () -> TraceXFile.open(new TrackingSource(header, header.length)));
            assertEquals("TraceX control-header magic mismatch", exception.getMessage()); //$NON-NLS-1$
        }
    }

    /** Tests unsigned addresses and a custom registry-name alignment. */
    @Test
    public void testUnsignedAddressesAndCustomNameSize() throws IOException {
        TraceXTestData.Fixture fixture = TraceXTestData.create(ByteOrder.BIG_ENDIAN,
                0xFFE00000L, 1, 3, 1, 0);
        TraceXLayout layout = parse(fixture);
        assertEquals(0xFFE00000L, layout.getBaseAddress());
        assertEquals(20, layout.getRegistryEntrySize());
        assertEquals(3, layout.getRegistryCapacity());
        assertEquals(1, layout.getEventCapacity());
    }

    /** Tests zero/minimum capacities and the published maximum event limit. */
    @Test
    public void testCapacityBounds() throws IOException {
        TraceXLayout minimum = parse(TraceXTestData.create(ByteOrder.LITTLE_ENDIAN,
                0x1000, 32, 0, 1, 0));
        assertEquals(0, minimum.getRegistryCapacity());
        assertEquals(1, minimum.getEventCapacity());

        TraceXLayout maximum = parse(TraceXTestData.create(ByteOrder.LITTLE_ENDIAN,
                0x1000, 32, 0, TraceXLayout.MAX_EVENT_CAPACITY, 0));
        assertEquals(TraceXLayout.MAX_EVENT_CAPACITY, maximum.getEventCapacity());

        TraceXTestData.Fixture empty = TraceXTestData.create(ByteOrder.LITTLE_ENDIAN,
                0x1000, 32, 0, 0, 0);
        assertMessage(empty, empty.fFileSize, "Event area contains no records"); //$NON-NLS-1$

        TraceXTestData.Fixture tooLarge = TraceXTestData.create(ByteOrder.LITTLE_ENDIAN,
                0x1000, 32, 0, TraceXLayout.MAX_EVENT_CAPACITY + 1L, 0);
        assertMessage(tooLarge, tooLarge.fFileSize,
                "Event capacity exceeds the documented limit of 32768"); //$NON-NLS-1$
    }

    /** Tests checked arithmetic successes, negative operands, and overflows. */
    @Test
    public void testCheckedArithmetic() throws TraceXFormatException {
        assertEquals(7, TraceXCheckedMath.add(3, 4, "sum")); //$NON-NLS-1$
        assertEquals(12, TraceXCheckedMath.multiply(3, 4, "product")); //$NON-NLS-1$
        assertEquals(5, TraceXCheckedMath.subtract(9, 4, "difference")); //$NON-NLS-1$

        assertEquals("sum is negative", //$NON-NLS-1$
                expectFormat(() -> TraceXCheckedMath.add(-1, 0, "sum")).getMessage()); //$NON-NLS-1$
        assertEquals("sum is negative", //$NON-NLS-1$
                expectFormat(() -> TraceXCheckedMath.add(0, -1, "sum")).getMessage()); //$NON-NLS-1$
        assertEquals("sum overflows", //$NON-NLS-1$
                expectFormat(() -> TraceXCheckedMath.add(Long.MAX_VALUE, 1, "sum")).getMessage()); //$NON-NLS-1$
        assertEquals("product is negative", //$NON-NLS-1$
                expectFormat(() -> TraceXCheckedMath.multiply(-1, 0, "product")).getMessage()); //$NON-NLS-1$
        assertEquals("product is negative", //$NON-NLS-1$
                expectFormat(() -> TraceXCheckedMath.multiply(0, -1, "product")).getMessage()); //$NON-NLS-1$
        assertEquals("product overflows", //$NON-NLS-1$
                expectFormat(() -> TraceXCheckedMath.multiply(Long.MAX_VALUE, 2, "product")).getMessage()); //$NON-NLS-1$
        assertEquals("difference is negative", //$NON-NLS-1$
                expectFormat(() -> TraceXCheckedMath.subtract(-1, 0, "difference")).getMessage()); //$NON-NLS-1$
        assertEquals("difference is negative", //$NON-NLS-1$
                expectFormat(() -> TraceXCheckedMath.subtract(0, -1, "difference")).getMessage()); //$NON-NLS-1$
        assertEquals("difference underflows", //$NON-NLS-1$
                expectFormat(() -> TraceXCheckedMath.subtract(0, 1, "difference")).getMessage()); //$NON-NLS-1$
    }

    /** Tests reserved markers and timer-mask validation. */
    @Test
    public void testControlSignature() {
        assertMessage(TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN)
                .putShort(TraceXTestData.RESERVED1_OFFSET, 1), "reserved signature mismatch"); //$NON-NLS-1$
        assertMessage(TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN)
                .putInt(TraceXTestData.RESERVED2_OFFSET, 0), "reserved signature mismatch"); //$NON-NLS-1$
        assertMessage(TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN)
                .putInt(TraceXTestData.RESERVED3_OFFSET, 0), "reserved signature mismatch"); //$NON-NLS-1$
        assertMessage(TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN)
                .putInt(TraceXTestData.RESERVED4_OFFSET, 0), "reserved signature mismatch"); //$NON-NLS-1$
        assertMessage(TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN).putInt(4, 0),
                "Timer valid mask is not a contiguous low-bit mask"); //$NON-NLS-1$
        assertMessage(TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN).putInt(4, 0x00FF00FF),
                "Timer valid mask is not a contiguous low-bit mask"); //$NON-NLS-1$
    }

    /** Tests word alignment for the base and every target pointer. */
    @Test
    public void testWordAlignment() {
        int[] offsets = { TraceXTestData.BASE_OFFSET, TraceXTestData.REGISTRY_START_OFFSET,
                TraceXTestData.REGISTRY_END_OFFSET, TraceXTestData.EVENT_START_OFFSET,
                TraceXTestData.EVENT_END_OFFSET, TraceXTestData.EVENT_CURRENT_OFFSET };
        String[] names = { "Trace base address", "Registry start address", //$NON-NLS-1$ //$NON-NLS-2$
                "Registry end address", "Event start address", "Event end address", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                "Current event address" }; //$NON-NLS-1$
        for (int i = 0; i < offsets.length; i++) {
            TraceXTestData.Fixture fixture = TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN);
            long original = Integer.toUnsignedLong(ByteBuffer.wrap(fixture.fHeader)
                    .order(fixture.fOrder).getInt(offsets[i]));
            fixture.putInt(offsets[i], original + 1);
            assertMessage(fixture, names[i] + " is not 4-byte aligned"); //$NON-NLS-1$
        }
    }

    /** Tests base-relative underflow and all registry-range invariants. */
    @Test
    public void testRegistryRanges() {
        TraceXTestData.Fixture belowBase = TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN);
        belowBase.putInt(TraceXTestData.REGISTRY_START_OFFSET, belowBase.fBase - 4);
        assertMessage(belowBase, "Registry start address underflows"); //$NON-NLS-1$

        TraceXTestData.Fixture wrongStart = TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN);
        wrongStart.putInt(TraceXTestData.REGISTRY_START_OFFSET,
                wrongStart.fBase + TraceXControlHeader.SIZE + 4);
        assertMessage(wrongStart, "Object registry must start immediately after the 48-byte header"); //$NON-NLS-1$

        assertMessage(TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN)
                .putShort(TraceXTestData.NAME_SIZE_OFFSET, 0),
                "Object registry name size is zero"); //$NON-NLS-1$

        TraceXTestData.Fixture reversed = TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN);
        reversed.putInt(TraceXTestData.REGISTRY_END_OFFSET,
                reversed.fBase + reversed.fRegistryStart - 4);
        assertMessage(reversed, "Registry range underflows"); //$NON-NLS-1$

        TraceXTestData.Fixture partial = TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN);
        partial.putInt(TraceXTestData.REGISTRY_END_OFFSET, partial.fBase + partial.fRegistryEnd + 4);
        partial.putInt(TraceXTestData.EVENT_START_OFFSET, partial.fBase + partial.fEventStart + 4);
        partial.putInt(TraceXTestData.EVENT_END_OFFSET, partial.fBase + partial.fEventEnd + 4);
        partial.putInt(TraceXTestData.EVENT_CURRENT_OFFSET, partial.fBase + partial.fEventCurrent + 4);
        assertMessage(partial, partial.fFileSize + 4,
                "Object registry range is not a whole number of entries"); //$NON-NLS-1$

        TraceXTestData.Fixture overlap = TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN);
        overlap.putInt(TraceXTestData.EVENT_START_OFFSET, overlap.fBase + overlap.fEventStart - 4);
        assertMessage(overlap, "Event area must immediately follow the object registry"); //$NON-NLS-1$
    }

    /** Tests event ordering, record alignment, current position, and file ranges. */
    @Test
    public void testEventRanges() {
        TraceXTestData.Fixture reversed = TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN);
        reversed.putInt(TraceXTestData.EVENT_END_OFFSET, reversed.fBase + reversed.fEventStart - 4);
        assertMessage(reversed, "Event range underflows"); //$NON-NLS-1$

        TraceXTestData.Fixture partial = TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN);
        partial.putInt(TraceXTestData.EVENT_END_OFFSET, partial.fBase + partial.fEventEnd + 4);
        assertMessage(partial, partial.fFileSize + 4,
                "Event area is not aligned to 32-byte records"); //$NON-NLS-1$

        TraceXTestData.Fixture before = TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN);
        before.putInt(TraceXTestData.EVENT_CURRENT_OFFSET, before.fBase + before.fEventStart - 4);
        assertMessage(before, "Current event pointer is outside the event area"); //$NON-NLS-1$

        TraceXTestData.Fixture after = TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN);
        after.putInt(TraceXTestData.EVENT_CURRENT_OFFSET, after.fBase + after.fEventEnd);
        assertMessage(after, "Current event pointer is outside the event area"); //$NON-NLS-1$

        TraceXTestData.Fixture unaligned = TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN);
        unaligned.putInt(TraceXTestData.EVENT_CURRENT_OFFSET, unaligned.fBase + unaligned.fEventStart + 4);
        assertMessage(unaligned, "Current event pointer is not record-aligned"); //$NON-NLS-1$

        TraceXTestData.Fixture range = TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN);
        assertMessage(range, range.fRegistryEnd - 1,
                "TraceX data range extends beyond the file"); //$NON-NLS-1$
        assertMessage(range, range.fEventEnd - 1,
                "TraceX data range extends beyond the file"); //$NON-NLS-1$
    }

    /** Tests the direct short-file guard and trailing bytes. */
    @Test
    public void testFileSizeBounds() throws TraceXFormatException {
        TraceXTestData.Fixture fixture = TraceXTestData.standard(ByteOrder.BIG_ENDIAN);
        TraceXControlHeader header = TraceXControlHeader.decode(ByteBuffer.wrap(fixture.fHeader));
        assertEquals("TraceX file is shorter than the control header", //$NON-NLS-1$
                expectFormat(() -> TraceXLayout.validate(header, 47)).getMessage());
        TraceXLayout layout = TraceXLayout.validate(header, fixture.fFileSize + 31);
        assertEquals(fixture.fFileSize + 31, layout.getFileSize());
    }

    /** Tests stable properties and their immutability. */
    @Test
    public void testProperties() throws IOException {
        TraceXLayout layout = parse(TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN));
        Map<String, String> properties = layout.getProperties();
        assertEquals("LITTLE_ENDIAN", properties.get("byte_order")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("0xF0000000", properties.get("base_address")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("0x0000FFFF", properties.get("timer_valid_mask")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("1", properties.get("registry_capacity")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("48", properties.get("registry_entry_size")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("48", properties.get("registry_start_offset")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("96", properties.get("registry_end_offset")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("2", properties.get("event_capacity")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("96", properties.get("event_start_offset")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("160", properties.get("event_end_offset")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("128", properties.get("event_current_offset")); //$NON-NLS-1$ //$NON-NLS-2$
        try {
            properties.put("x", "y"); //$NON-NLS-1$ //$NON-NLS-2$
            fail("Properties must be immutable"); //$NON-NLS-1$
        } catch (UnsupportedOperationException e) {
            assertNotNull(e);
        }
    }

    /** Tests partial positional reads, repeated opens, and disposal. */
    @Test
    public void testResourceOwnership() throws IOException {
        TraceXTestData.Fixture fixture = TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN);
        TrackingSource source = new TrackingSource(fixture.fHeader, fixture.fFileSize);
        source.fMaxRead = 3;
        try (TraceXFile file = TraceXFile.open(source)) {
            assertTrue(file.isOpen());
            assertEquals(1, file.getLayout().getRegistryCapacity());
            file.close();
            file.close();
            assertFalse(file.isOpen());
        }
        assertFalse(source.isOpen());
        assertEquals(1, source.fCloseCount);

        for (int i = 0; i < 2; i++) {
            TrackingSource repeated = new TrackingSource(fixture.fHeader, fixture.fFileSize);
            try (TraceXFile ignored = TraceXFile.open(repeated)) {
                assertTrue(repeated.isOpen());
            }
            assertFalse(repeated.isOpen());
        }
    }

    /** Tests source failures and mandatory close-on-failure behavior. */
    @Test
    public void testInputFailures() {
        TraceXTestData.Fixture fixture = TraceXTestData.standard(ByteOrder.LITTLE_ENDIAN);

        TrackingSource negative = new TrackingSource(fixture.fHeader, -1);
        assertEquals("TraceX source reports a negative size", //$NON-NLS-1$
                expectFormat(() -> TraceXFile.open(negative)).getMessage());
        assertFalse(negative.isOpen());

        TrackingSource sizeFailure = new TrackingSource(fixture.fHeader, fixture.fFileSize);
        sizeFailure.fFailSize = true;
        IOException sizeException = expectIo(() -> TraceXFile.open(sizeFailure));
        assertEquals("size failure", sizeException.getMessage()); //$NON-NLS-1$
        assertFalse(sizeFailure.isOpen());

        TrackingSource readFailure = new TrackingSource(fixture.fHeader, fixture.fFileSize);
        readFailure.fFailRead = true;
        IOException readException = expectIo(() -> TraceXFile.open(readFailure));
        assertEquals("read failure", readException.getMessage()); //$NON-NLS-1$
        assertFalse(readFailure.isOpen());

        TrackingSource runtimeFailure = new TrackingSource(fixture.fHeader, fixture.fFileSize);
        runtimeFailure.fFailRuntime = true;
        try {
            TraceXFile.open(runtimeFailure);
            fail("Expected runtime failure"); //$NON-NLS-1$
        } catch (IllegalStateException e) {
            assertEquals("runtime failure", e.getMessage()); //$NON-NLS-1$
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        assertFalse(runtimeFailure.isOpen());

        TrackingSource errorFailure = new TrackingSource(fixture.fHeader, fixture.fFileSize);
        errorFailure.fFailError = true;
        try {
            TraceXFile.open(errorFailure);
            fail("Expected error failure"); //$NON-NLS-1$
        } catch (AssertionError e) {
            assertEquals("error failure", e.getMessage()); //$NON-NLS-1$
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        assertFalse(errorFailure.isOpen());

        TrackingSource stalled = new TrackingSource(fixture.fHeader, fixture.fFileSize);
        stalled.fMaxRead = 0;
        assertEquals("Truncated TraceX magic: expected 4 bytes, found 0", //$NON-NLS-1$
                expectFormat(() -> TraceXFile.open(stalled)).getMessage());
        assertFalse(stalled.isOpen());

        TrackingSource closeFailure = new TrackingSource(new byte[0], 0);
        closeFailure.fFailClose = true;
        TraceXFormatException failure = expectFormat(() -> TraceXFile.open(closeFailure));
        assertEquals(1, failure.getSuppressed().length);
        assertEquals("close failure", failure.getSuppressed()[0].getMessage()); //$NON-NLS-1$

        TrackingSource runtimeCloseFailure = new TrackingSource(new byte[0], 0);
        runtimeCloseFailure.fFailCloseRuntime = true;
        TraceXFormatException runtimeClose = expectFormat(() -> TraceXFile.open(runtimeCloseFailure));
        assertEquals(1, runtimeClose.getSuppressed().length);
        assertEquals("runtime close failure", runtimeClose.getSuppressed()[0].getMessage()); //$NON-NLS-1$
    }

    private static TraceXLayout parse(TraceXTestData.Fixture fixture) throws IOException {
        try (TraceXFile file = TraceXFile.open(new TrackingSource(fixture.fHeader, fixture.fFileSize))) {
            return file.getLayout();
        }
    }

    private static void assertMessage(TraceXTestData.Fixture fixture, String expected) {
        assertMessage(fixture, fixture.fFileSize, expected);
    }

    private static void assertMessage(TraceXTestData.Fixture fixture, long fileSize, String expected) {
        TraceXFormatException exception = expectFormat(
                () -> TraceXFile.open(new TrackingSource(fixture.fHeader, fileSize)));
        assertTrue(exception.getMessage(), exception.getMessage().contains(expected));
    }

    private static TraceXFormatException expectFormat(ThrowingRunnable runnable) {
        try {
            runnable.run();
            fail("Expected TraceXFormatException"); //$NON-NLS-1$
        } catch (TraceXFormatException e) {
            return e;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        throw new AssertionError();
    }

    private static IOException expectIo(ThrowingRunnable runnable) {
        try {
            runnable.run();
            fail("Expected IOException"); //$NON-NLS-1$
        } catch (TraceXFormatException e) {
            throw new AssertionError(e);
        } catch (IOException e) {
            return e;
        }
        throw new AssertionError();
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws IOException;
    }

    private static final class TrackingSource implements TraceXDataSource {

        private final byte[] fData;
        private final long fReportedSize;
        private boolean fOpen = true;
        private int fMaxRead = Integer.MAX_VALUE;
        private boolean fFailSize;
        private boolean fFailRead;
        private boolean fFailRuntime;
        private boolean fFailError;
        private boolean fFailClose;
        private boolean fFailCloseRuntime;
        private int fCloseCount;

        private TrackingSource(byte[] data, long reportedSize) {
            fData = data.clone();
            fReportedSize = reportedSize;
        }

        @Override
        public long size() throws IOException {
            if (fFailRuntime) {
                throw new IllegalStateException("runtime failure"); //$NON-NLS-1$
            }
            if (fFailError) {
                throw new AssertionError("error failure"); //$NON-NLS-1$
            }
            if (fFailSize) {
                throw new IOException("size failure"); //$NON-NLS-1$
            }
            return fReportedSize;
        }

        @Override
        public int read(ByteBuffer destination, long position) throws IOException {
            if (fFailRead) {
                throw new IOException("read failure"); //$NON-NLS-1$
            }
            if (fMaxRead == 0) {
                return 0;
            }
            if (position >= fData.length) {
                return -1;
            }
            int count = Math.min(destination.remaining(), fMaxRead);
            count = Math.min(count, fData.length - (int) position);
            destination.put(fData, (int) position, count);
            return count;
        }

        @Override
        public boolean isOpen() {
            return fOpen;
        }

        @Override
        public void close() throws IOException {
            fCloseCount++;
            fOpen = false;
            if (fFailCloseRuntime) {
                throw new IllegalStateException("runtime close failure"); //$NON-NLS-1$
            }
            if (fFailClose) {
                throw new IOException("close failure"); //$NON-NLS-1$
            }
        }
    }
}
