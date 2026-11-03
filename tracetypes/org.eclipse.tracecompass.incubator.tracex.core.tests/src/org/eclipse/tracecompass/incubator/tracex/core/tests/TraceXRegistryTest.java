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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXDataSource;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXFile;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXFormatException;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXObjectType;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXRegistry;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXRegistryRecord;
import org.junit.Test;

/** Tests lossless registry decoding and deterministic object lookup. */
public class TraceXRegistryTest {

    private static final int[] DOCUMENTED_TYPES = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
            21, 22, 23, 24, 25, 26, 27, 28
    };

    /** Tests every documented object type, unused entries, and raw fields. */
    @Test
    public void testDocumentedTypesAndUnusedEntries() throws IOException {
        TraceXTestData.Fixture fixture = TraceXTestData.create(ByteOrder.BIG_ENDIAN,
                0x80000000L, 8, DOCUMENTED_TYPES.length, 1, 0);
        byte[] data = fixture.fileBytes();
        for (int i = 0; i < DOCUMENTED_TYPES.length; i++) {
            int available = i == 0 || i == DOCUMENTED_TYPES.length - 1 ? 1 : 0;
            putRecord(data, fixture, i, available, DOCUMENTED_TYPES[i], 0, 0,
                    0x80001000L + i * 4L, i, 0xffffffffL - i,
                    ("obj" + i).getBytes(StandardCharsets.US_ASCII)); //$NON-NLS-1$
        }

        try (TraceXFile file = TraceXFile.open(new ArraySource(data))) {
            TraceXRegistry registry = file.getRegistry();
            assertSame(registry, file.getRegistry());
            assertEquals(DOCUMENTED_TYPES.length, registry.getRecords().size());
            for (int i = 0; i < DOCUMENTED_TYPES.length; i++) {
                TraceXRegistryRecord record = registry.getRecords().get(i);
                assertEquals(i, record.getIndex());
                assertEquals(DOCUMENTED_TYPES[i], record.getTypeCode());
                assertEquals(TraceXObjectType.fromCode(DOCUMENTED_TYPES[i]), record.getType());
                assertEquals(i, record.getParameter1());
                assertEquals(0xffffffffL - i, record.getParameter2());
                assertEquals(String.format("0x%08X", 0x80001000L + i * 4L), //$NON-NLS-1$
                        record.getFormattedAddress());
            }
            TraceXRegistryRecord unused = registry.getRecords().get(0);
            assertTrue(unused.isAvailable());
            assertFalse(unused.isValid());
            assertFalse(unused.isInUse());
            TraceXRegistryRecord stale = registry.getRecords().get(DOCUMENTED_TYPES.length - 1);
            assertTrue(stale.isAvailable());
            assertTrue(stale.isValid());
            assertFalse(stale.isInUse());
            assertTrue(registry.getRecords().get(1).isInUse());
            assertThrows(UnsupportedOperationException.class,
                    () -> registry.getRecords().add(unused));
        }
    }

    /** Tests minimum, typical, maximum, unterminated, embedded-NUL, and byte replacement names. */
    @Test
    public void testNameDecodingAndRawPreservation() throws IOException {
        TraceXRegistryRecord minimum = TraceXRegistryRecord.decode(0,
                record(ByteOrder.LITTLE_ENDIAN, 1, 0, 1, 0, 0,
                        0xffffffffL, 2, 3, new byte[] { 'A' }),
                1, ByteOrder.LITTLE_ENDIAN);
        assertEquals("A", minimum.getName()); //$NON-NLS-1$
        assertEquals(0xffffffffL, minimum.getAddress());
        assertEquals(0, minimum.getAvailable());
        assertEquals(0, minimum.getReserved1());
        assertEquals(0, minimum.getReserved2());
        assertNull(minimum.getInitialPriority());

        byte[] mixed = { 'a', 'b', 0, 'c', (byte) 0x80, '\n', 'z', 0 };
        TraceXRegistryRecord embedded = TraceXRegistryRecord.decode(1,
                record(ByteOrder.BIG_ENDIAN, mixed.length, 0, 1, 0, 0,
                        4, 5, 6, mixed), mixed.length, ByteOrder.BIG_ENDIAN);
        assertEquals("ab", embedded.getName()); //$NON-NLS-1$
        assertArrayEquals(mixed, embedded.getNameBytes());
        byte[] copy = embedded.getNameBytes();
        copy[0] = 'x';
        assertEquals('a', embedded.getNameBytes()[0]);

        byte[] malformed = { 'x', (byte) 0x80, '\n', '~' };
        TraceXRegistryRecord replacement = TraceXRegistryRecord.decode(2,
                record(ByteOrder.LITTLE_ENDIAN, malformed.length, 0, 1, 0, 0,
                        7, 8, 9, malformed), malformed.length, ByteOrder.LITTLE_ENDIAN);
        assertEquals("x\ufffd\ufffd~", replacement.getName()); //$NON-NLS-1$

        int maximumSize = 0xffff;
        byte[] maximumName = new byte[maximumSize];
        Arrays.fill(maximumName, (byte) 'M');
        maximumName[maximumSize - 1] = 'Z';
        TraceXRegistryRecord maximum = TraceXRegistryRecord.decode(3,
                record(ByteOrder.BIG_ENDIAN, maximumSize, 0, 1, 0, 0,
                        10, 11, 12, maximumName), maximumSize, ByteOrder.BIG_ENDIAN);
        assertEquals(maximumSize, maximum.getName().length());
        assertEquals('Z', maximum.getName().charAt(maximumSize - 1));

        byte[] raw = maximum.getRawBytes();
        raw[0] = 1;
        assertEquals(0, maximum.getRawBytes()[0]);
    }

    /** Tests initial thread-priority encoding boundaries and unfamiliar types. */
    @Test
    public void testInitialPriorityAndUnknownType() throws IOException {
        TraceXRegistryRecord highest = TraceXRegistryRecord.decode(0,
                record(ByteOrder.LITTLE_ENDIAN, 4, 0, 1, 0x83, 0xff,
                        1, 0, 0, new byte[4]), 4, ByteOrder.LITTLE_ENDIAN);
        assertEquals(Integer.valueOf(1023), highest.getInitialPriority());

        TraceXRegistryRecord lowest = TraceXRegistryRecord.decode(1,
                record(ByteOrder.LITTLE_ENDIAN, 4, 0, 1, 0x80, 0,
                        1, 0, 0, new byte[4]), 4, ByteOrder.LITTLE_ENDIAN);
        assertEquals(Integer.valueOf(0), lowest.getInitialPriority());

        TraceXRegistryRecord oldThread = TraceXRegistryRecord.decode(2,
                record(ByteOrder.LITTLE_ENDIAN, 4, 0, 1, 0x03, 0xff,
                        1, 0, 0, new byte[4]), 4, ByteOrder.LITTLE_ENDIAN);
        assertNull(oldThread.getInitialPriority());

        TraceXRegistryRecord unknown = TraceXRegistryRecord.decode(3,
                record(ByteOrder.LITTLE_ENDIAN, 4, 0, 255, 0, 0,
                        1, 0, 0, new byte[4]), 4, ByteOrder.LITTLE_ENDIAN);
        assertSame(TraceXObjectType.UNKNOWN, unknown.getType());
        assertEquals(-1, TraceXObjectType.UNKNOWN.getCode());
        assertEquals("Unknown", TraceXObjectType.UNKNOWN.getDisplayName()); //$NON-NLS-1$
    }

    /** Tests duplicate and unsigned addresses with deterministic first-record lookup. */
    @Test
    public void testAddressLookupAndDiagnostics() throws IOException {
        TraceXTestData.Fixture fixture = TraceXTestData.create(ByteOrder.LITTLE_ENDIAN,
                0x1000, 4, 3, 1, 0);
        byte[] data = fixture.fileBytes();
        putRecord(data, fixture, 0, 1, 1, 0, 0, 0xf1234567L, 0, 0,
                "old".getBytes(StandardCharsets.US_ASCII)); //$NON-NLS-1$
        putRecord(data, fixture, 1, 0, 1, 0, 0, 0xf1234567L, 0, 0,
                "new".getBytes(StandardCharsets.US_ASCII)); //$NON-NLS-1$
        putRecord(data, fixture, 2, 0, 0, 0, 0, 0x1234, 0, 0, new byte[0]);

        try (TraceXFile file = TraceXFile.open(new ArraySource(data))) {
            TraceXRegistry registry = file.getRegistry();
            TraceXRegistryRecord first = registry.find(0xf1234567L);
            assertNotNull(first);
            assertEquals("old", first.getName()); //$NON-NLS-1$
            assertNull(registry.find(0x1234));
            List<String> diagnostics = registry.getDiagnostics();
            assertEquals(1, diagnostics.size());
            assertTrue(diagnostics.get(0).contains("0xF1234567")); //$NON-NLS-1$
            assertThrows(UnsupportedOperationException.class,
                    () -> diagnostics.add("x")); //$NON-NLS-1$
        }
    }

    /** Tests rejected record sizes and a truncated final registry record. */
    @Test
    public void testInvalidAndTruncatedRecords() throws IOException {
        byte[] raw = new byte[17];
        assertEquals("Object registry name size is zero", //$NON-NLS-1$
                assertThrows(TraceXFormatException.class,
                        () -> TraceXRegistryRecord.decode(0, raw, 0,
                                ByteOrder.LITTLE_ENDIAN)).getMessage());
        assertEquals("Truncated object registry record 7", //$NON-NLS-1$
                assertThrows(TraceXFormatException.class,
                        () -> TraceXRegistryRecord.decode(7, raw, 2,
                                ByteOrder.LITTLE_ENDIAN)).getMessage());

        TraceXTestData.Fixture fixture = TraceXTestData.create(ByteOrder.LITTLE_ENDIAN,
                0x1000, 4, 2, 1, 0);
        byte[] complete = fixture.fileBytes();
        byte[] truncated = Arrays.copyOf(complete, (int) fixture.fRegistryEnd - 5);
        ArraySource source = new ArraySource(truncated, fixture.fFileSize);
        try (TraceXFile file = TraceXFile.open(source)) {
            TraceXFormatException exception = assertThrows(TraceXFormatException.class,
                    file::getRegistry);
            assertEquals("Truncated object registry record 1", exception.getMessage()); //$NON-NLS-1$
        }
    }

    private static byte[] record(ByteOrder order, int nameSize, int available,
            int type, int reserved1, int reserved2, long address,
            long parameter1, long parameter2, byte[] name) {
        int entrySize = (16 + nameSize + 3) & -4;
        ByteBuffer record = ByteBuffer.allocate(entrySize).order(order);
        record.put((byte) available).put((byte) type).put((byte) reserved1)
                .put((byte) reserved2).putInt((int) address)
                .putInt((int) parameter1).putInt((int) parameter2);
        record.put(name, 0, Math.min(name.length, nameSize));
        return record.array();
    }

    static void putRecord(byte[] data, TraceXTestData.Fixture fixture, int index,
            int available, int type, int reserved1, int reserved2, long address,
            long parameter1, long parameter2, byte[] name) {
        byte[] record = record(fixture.fOrder,
                (int) fixture.fRegistryEntrySize - 16, available, type,
                reserved1, reserved2, address, parameter1, parameter2, name);
        int offset = (int) (fixture.fRegistryStart + index * fixture.fRegistryEntrySize);
        System.arraycopy(record, 0, data, offset, record.length);
    }

    static final class ArraySource implements TraceXDataSource {
        private final byte[] fData;
        private final long fReportedSize;
        private boolean fOpen = true;
        int fMaximumRead = Integer.MAX_VALUE;

        ArraySource(byte[] data) {
            this(data, data.length);
        }

        ArraySource(byte[] data, long reportedSize) {
            fData = data;
            fReportedSize = reportedSize;
        }

        @Override
        public long size() {
            return fReportedSize;
        }

        @Override
        public int read(ByteBuffer destination, long position) {
            if (position >= fData.length) {
                return -1;
            }
            int count = Math.min(destination.remaining(), fMaximumRead);
            count = Math.min(count, fData.length - (int) position);
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
}
