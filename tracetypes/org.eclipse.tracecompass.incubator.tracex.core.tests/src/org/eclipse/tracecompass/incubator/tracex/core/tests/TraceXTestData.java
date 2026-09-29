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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXControlHeader;

/**
 * Compact generated TraceX fixtures used by the parser tests.
 */
final class TraceXTestData {

    static final int TIMER_MASK = 0x0000FFFF;
    static final int BASE_OFFSET = 8;
    static final int REGISTRY_START_OFFSET = 12;
    static final int RESERVED1_OFFSET = 16;
    static final int NAME_SIZE_OFFSET = 18;
    static final int REGISTRY_END_OFFSET = 20;
    static final int EVENT_START_OFFSET = 24;
    static final int EVENT_END_OFFSET = 28;
    static final int EVENT_CURRENT_OFFSET = 32;
    static final int RESERVED2_OFFSET = 36;
    static final int RESERVED3_OFFSET = 40;
    static final int RESERVED4_OFFSET = 44;

    private TraceXTestData() {
    }

    static Fixture standard(ByteOrder order) {
        return create(order, 0xF0000000L, 32, 1, 2, 1);
    }

    static Fixture create(ByteOrder order, long base, int nameSize,
            long registryCapacity, long eventCapacity, long currentIndex) {
        long entrySize = (16L + nameSize + 3L) & -4L;
        long registryStart = TraceXControlHeader.SIZE;
        long registryEnd = registryStart + entrySize * registryCapacity;
        long eventStart = registryEnd;
        long eventEnd = eventStart + 32L * eventCapacity;
        long eventCurrent = eventStart + 32L * currentIndex;

        ByteBuffer header = ByteBuffer.allocate(TraceXControlHeader.SIZE).order(order);
        header.putInt(0x54585442);
        header.putInt(TIMER_MASK);
        header.putInt((int) base);
        header.putInt((int) (base + registryStart));
        header.putShort((short) 0);
        header.putShort((short) nameSize);
        header.putInt((int) (base + registryEnd));
        header.putInt((int) (base + eventStart));
        header.putInt((int) (base + eventEnd));
        header.putInt((int) (base + eventCurrent));
        header.putInt(0xAAAAAAAA);
        header.putInt(0xBBBBBBBB);
        header.putInt(0xCCCCCCCC);
        return new Fixture(order, header.array(), eventEnd, base, registryStart,
                registryEnd, entrySize, eventStart, eventEnd, eventCurrent);
    }

    static final class Fixture {

        final ByteOrder fOrder;
        final byte[] fHeader;
        final long fFileSize;
        final long fBase;
        final long fRegistryStart;
        final long fRegistryEnd;
        final long fRegistryEntrySize;
        final long fEventStart;
        final long fEventEnd;
        final long fEventCurrent;

        private Fixture(ByteOrder order, byte[] header, long fileSize, long base,
                long registryStart, long registryEnd, long registryEntrySize,
                long eventStart, long eventEnd, long eventCurrent) {
            fOrder = order;
            fHeader = header;
            fFileSize = fileSize;
            fBase = base;
            fRegistryStart = registryStart;
            fRegistryEnd = registryEnd;
            fRegistryEntrySize = registryEntrySize;
            fEventStart = eventStart;
            fEventEnd = eventEnd;
            fEventCurrent = eventCurrent;
        }

        byte[] fileBytes() {
            byte[] file = new byte[(int) fFileSize];
            System.arraycopy(fHeader, 0, file, 0, fHeader.length);
            return file;
        }

        void putEvent(byte[] file, int physicalIndex, long context,
                long eventId, long timestamp) {
            putEvent(file, physicalIndex, context, 0x80010002L, eventId,
                    timestamp, physicalIndex + 1, physicalIndex + 2,
                    physicalIndex + 3, physicalIndex + 4);
        }

        void putEvent(byte[] file, int physicalIndex, long context,
                long priority, long eventId, long timestamp, long info1,
                long info2, long info3, long info4) {
            int offset = (int) fEventStart + physicalIndex * 32;
            ByteBuffer record = ByteBuffer.wrap(file).order(fOrder);
            record.putInt(offset, (int) context);
            record.putInt(offset + 4, (int) priority);
            record.putInt(offset + 8, (int) eventId);
            record.putInt(offset + 12, (int) timestamp);
            record.putInt(offset + 16, (int) info1);
            record.putInt(offset + 20, (int) info2);
            record.putInt(offset + 24, (int) info3);
            record.putInt(offset + 28, (int) info4);
        }

        void putThread(byte[] file, int registryIndex, long address,
                String name) {
            int offset = (int) (fRegistryStart
                    + registryIndex * fRegistryEntrySize);
            ByteBuffer record = ByteBuffer.wrap(file).order(fOrder);
            record.put(offset, (byte) 0);
            record.put(offset + 1, (byte) 1);
            record.put(offset + 2, (byte) 0x80);
            record.put(offset + 3, (byte) 5);
            record.putInt(offset + 4, (int) address);
            byte[] nameBytes = name.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            int count = Math.min(nameBytes.length,
                    (int) fRegistryEntrySize - 16);
            System.arraycopy(nameBytes, 0, file, offset + 16, count);
        }

        Fixture putInt(int offset, long value) {
            ByteBuffer.wrap(fHeader).order(fOrder).putInt(offset, (int) value);
            return this;
        }

        Fixture putShort(int offset, int value) {
            ByteBuffer.wrap(fHeader).order(fOrder).putShort(offset, (short) value);
            return this;
        }
    }
}
