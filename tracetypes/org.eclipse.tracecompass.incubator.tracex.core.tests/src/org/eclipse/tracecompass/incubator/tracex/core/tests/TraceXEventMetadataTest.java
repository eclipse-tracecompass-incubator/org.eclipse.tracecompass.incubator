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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXContext;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXEvent;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXEventCategory;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXEventField;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXEventMetadata;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXFile;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXFormatException;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXMetadataCatalog;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXRegistry;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXRegistryRecord;
import org.junit.Test;

/** Tests event metadata, contexts, aliases, and generic fallbacks. */
public class TraceXEventMetadataTest {

    private static final int[] DEMO_EVENT_IDS = {
            1, 2, 3, 4, 5, 6, 32, 36, 50, 51, 52, 57, 68, 69, 83, 88, 103, 107, 109, 112,
            201, 202, 203, 204, 205, 206, 207, 208, 210, 211, 214, 243, 244, 247, 248, 251,
            254, 258, 261,
            308, 309, 310, 311, 312, 313, 316, 317, 318, 319, 323, 324, 384, 386, 387, 388,
            390, 394, 395, 400, 401, 403, 407, 409, 410, 412, 413, 414, 419, 421, 434, 436
    };

    /** Tests exact catalog coverage of every event exercised by all four demos. */
    @Test
    public void testDemoCatalogCoverage() {
        Set<Integer> expected = new HashSet<>();
        Arrays.stream(DEMO_EVENT_IDS).forEach(expected::add);
        Map<Integer, TraceXEventMetadata> catalog = TraceXMetadataCatalog.getCatalog();
        assertEquals(expected, catalog.keySet());
        for (int id : DEMO_EVENT_IDS) {
            TraceXEventMetadata metadata = TraceXMetadataCatalog.lookup(id);
            assertEquals(id, metadata.getId());
            assertFalse(metadata.getName().isEmpty());
            assertTrue(metadata.getCategory() == TraceXEventCategory.THREADX
                    || metadata.getCategory() == TraceXEventCategory.FILEX
                    || metadata.getCategory() == TraceXEventCategory.NETX_DUO);
            metadata.getAliases().forEach(alias -> {
                assertTrue(alias.getWord() >= 1 && alias.getWord() <= 4);
                assertFalse(alias.getName().isEmpty());
            });
        }
        assertThrows(UnsupportedOperationException.class,
                () -> catalog.put(7, TraceXMetadataCatalog.lookup(7)));
        assertThrows(UnsupportedOperationException.class,
                () -> TraceXMetadataCatalog.lookup(1).getAliases().clear());
    }

    /** Tests stable component, USBX, future, user, invalid, and unknown fallbacks. */
    @Test
    public void testGenericCategories() {
        assertGeneric(0, TraceXEventCategory.INVALID, "INVALID_EVENT_0000"); //$NON-NLS-1$
        assertGeneric(199, TraceXEventCategory.THREADX, "THREADX_EVENT_00C7"); //$NON-NLS-1$
        assertGeneric(299, TraceXEventCategory.FILEX, "FILEX_EVENT_012B"); //$NON-NLS-1$
        assertGeneric(599, TraceXEventCategory.NETX_DUO, "NETX_DUO_EVENT_0257"); //$NON-NLS-1$
        assertGeneric(600, TraceXEventCategory.USBX, "USBX_EVENT_0258"); //$NON-NLS-1$
        assertGeneric(999, TraceXEventCategory.USBX, "USBX_EVENT_03E7"); //$NON-NLS-1$
        assertGeneric(1000, TraceXEventCategory.RESERVED, "RESERVED_EVENT_03E8"); //$NON-NLS-1$
        assertGeneric(4095, TraceXEventCategory.RESERVED, "RESERVED_EVENT_0FFF"); //$NON-NLS-1$
        assertGeneric(4096, TraceXEventCategory.USER, "USER_EVENT_1000"); //$NON-NLS-1$
        assertGeneric(0xffff, TraceXEventCategory.USER, "USER_EVENT_FFFF"); //$NON-NLS-1$
        assertGeneric(-1, TraceXEventCategory.UNKNOWN, "UNKNOWN_EVENT_FFFFFFFF"); //$NON-NLS-1$
        assertGeneric(0x10000, TraceXEventCategory.UNKNOWN, "UNKNOWN_EVENT_10000"); //$NON-NLS-1$
    }

    /** Tests high event flags, raw fields, priority bounds, aliases, and resolution. */
    @Test
    public void testLosslessEventAndAliases() throws IOException {
        TraceXTestData.Fixture fixture = TraceXTestData.create(ByteOrder.BIG_ENDIAN,
                0x1000, 8, 2, 1, 0);
        byte[] data = fixture.fileBytes();
        long thread = 0xf1234567L;
        long mutex = 0x81234567L;
        TraceXRegistryTest.putRecord(data, fixture, 0, 0, 1, 0x80, 31,
                thread, 0, 0, "worker".getBytes(StandardCharsets.US_ASCII)); //$NON-NLS-1$
        TraceXRegistryTest.putRecord(data, fixture, 1, 0, 5, 0, 0,
                mutex, 1, 0, "lock".getBytes(StandardCharsets.US_ASCII)); //$NON-NLS-1$
        putEvent(data, fixture, 0, thread,
                0x80000000L | (1023L << 16) | 1023L, 0xabcd0034L,
                0xfedcba98L, mutex, 7, thread, 2);

        TraceXRegistryTest.ArraySource source = new TraceXRegistryTest.ArraySource(data);
        source.fMaximumRead = 3;
        try (TraceXFile file = TraceXFile.open(source)) {
            TraceXEvent event = file.readEvent(0);
            assertEquals(0xabcd0034L, event.getRawEventId());
            assertEquals(52, event.getNormalizedEventId());
            assertEquals("TX_TRACE_MUTEX_GET", event.getMetadata().getName()); //$NON-NLS-1$
            assertEquals(TraceXEventCategory.THREADX, event.getMetadata().getCategory());
            assertEquals(thread, event.getRawContextAddress());
            assertEquals(0x83ff03ffL, event.getRawPriority());
            assertEquals(0xfedcba98L, event.getRawTimestamp());
            assertFalse(event.getRelativeTicks().isPresent());
            assertEquals(TraceXContext.Kind.THREAD, event.getContext().getKind());
            assertEquals("worker", event.getContext().getName()); //$NON-NLS-1$
            assertEquals("0xF1234567", event.getContext().getFormattedAddress()); //$NON-NLS-1$
            assertEquals(Integer.valueOf(1023), event.getContext().getPriority());
            assertEquals(Integer.valueOf(1023), event.getContext().getPreemptionThreshold());

            assertEquals(mutex, event.getInformationWord(1));
            assertEquals(7, event.getInformationWord(2));
            assertEquals(thread, event.getInformationWord(3));
            assertEquals(2, event.getInformationWord(4));
            assertThrows(IllegalArgumentException.class, () -> event.getInformationWord(0));
            assertThrows(IllegalArgumentException.class, () -> event.getInformationWord(5));

            TraceXEventField mutexAlias = event.getAliases().get("mutex"); //$NON-NLS-1$
            assertNotNull(mutexAlias);
            assertEquals(1, mutexAlias.getRawWord());
            assertEquals(mutex, mutexAlias.getRawValue());
            assertTrue(mutexAlias.isAddress());
            assertEquals("0x81234567", mutexAlias.getFormattedAddress()); //$NON-NLS-1$
            TraceXRegistryRecord resolvedMutex = mutexAlias.getResolvedObject();
            assertNotNull(resolvedMutex);
            assertEquals("lock", resolvedMutex.getName()); //$NON-NLS-1$
            TraceXEventField waitAlias = event.getAliases().get("wait_option"); //$NON-NLS-1$
            assertNotNull(waitAlias);
            assertFalse(waitAlias.isAddress());
            assertNull(waitAlias.getFormattedAddress());
            assertNull(waitAlias.getResolvedObject());
            assertThrows(UnsupportedOperationException.class,
                    () -> event.getAliases().clear());

            Map<String, Object> fields = event.getFields();
            assertEquals(0xabcd0034L, fields.get(TraceXEvent.FIELD_RAW_EVENT_ID));
            assertEquals(52, fields.get(TraceXEvent.FIELD_EVENT_ID));
            assertEquals(0xfedcba98L, fields.get(TraceXEvent.FIELD_RAW_TIMESTAMP));
            assertEquals(TraceXContext.Kind.THREAD.name(), fields.get(TraceXEvent.FIELD_CONTEXT));
            assertEquals("lock", fields.get("mutex_name")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(mutex, fields.get("raw_info_1")); //$NON-NLS-1$
            assertFalse(fields.containsKey(TraceXEvent.FIELD_RELATIVE_TICKS));
            assertThrows(UnsupportedOperationException.class,
                    () -> fields.put("x", 1)); //$NON-NLS-1$
        }
    }

    /** Tests all sentinel, resolved component, and unresolved context kinds. */
    @Test
    public void testContextKindsAndPriorityPresence() throws IOException {
        TraceXTestData.Fixture fixture = TraceXTestData.create(ByteOrder.LITTLE_ENDIAN,
                0x1000, 4, 2, 7, 0);
        byte[] data = fixture.fileBytes();
        TraceXRegistryTest.putRecord(data, fixture, 0, 0, 1, 0, 0,
                0x2000, 0, 0, "t".getBytes(StandardCharsets.US_ASCII)); //$NON-NLS-1$
        TraceXRegistryTest.putRecord(data, fixture, 1, 0, 11, 0, 0,
                0x3000, 0, 0, "ip".getBytes(StandardCharsets.US_ASCII)); //$NON-NLS-1$
        putEvent(data, fixture, 0, 0x2000, 17, 1, 1, 0, 0, 0, 0);
        putEvent(data, fixture, 1, 0xffffffffL, 0x2000, 3, 2, 0, 0, 0, 0);
        putEvent(data, fixture, 2, 0xffffffffL, 0, 4, 3, 0, 0, 0, 0);
        putEvent(data, fixture, 3, 0xf0f0f0f0L, 0, 103, 4, 0, 0, 0, 0);
        putEvent(data, fixture, 4, 0, 0, 4096, 5, 1, 2, 3, 4);
        putEvent(data, fixture, 5, 0x3000, 0xffffffffL, 600, 6, 1, 2, 3, 4);
        putEvent(data, fixture, 6, 0x4000, 0x80000000L | (1L << 16) | 2,
                199, 7, 1, 2, 3, 4);

        try (TraceXFile file = TraceXFile.open(new TraceXRegistryTest.ArraySource(data))) {
            TraceXEvent thread = file.readEvent(0);
            assertEquals(TraceXContext.Kind.THREAD, thread.getContext().getKind());
            assertEquals(0x2000, thread.getContext().getRawAddress());
            assertEquals("0x00002000", thread.getContext().getFormattedAddress()); //$NON-NLS-1$
            assertEquals(Integer.valueOf(17), thread.getContext().getPriority());
            assertNull(thread.getContext().getPreemptionThreshold());

            TraceXEvent isr = file.readEvent(1);
            assertEquals(TraceXContext.Kind.ISR, isr.getContext().getKind());
            assertEquals(Long.valueOf(0x2000), isr.getContext().getInterruptedThreadAddress());
            assertNull(file.readEvent(2).getContext().getInterruptedThreadAddress());
            assertEquals(TraceXContext.Kind.INITIALIZATION, file.readEvent(3).getContext().getKind());

            TraceXEvent idleUser = file.readEvent(4);
            assertEquals(TraceXContext.Kind.IDLE, idleUser.getContext().getKind());
            assertEquals(TraceXEventCategory.USER, idleUser.getMetadata().getCategory());
            assertEquals("USER_EVENT_1000", idleUser.getMetadata().getName()); //$NON-NLS-1$

            TraceXEvent component = file.readEvent(5);
            assertEquals(TraceXContext.Kind.COMPONENT, component.getContext().getKind());
            assertEquals("ip", component.getContext().getName()); //$NON-NLS-1$
            assertNull(component.getContext().getPriority());
            assertEquals(TraceXEventCategory.USBX, component.getMetadata().getCategory());

            TraceXEvent unknown = file.readEvent(6);
            assertEquals(TraceXContext.Kind.UNKNOWN, unknown.getContext().getKind());
            assertNull(unknown.getContext().getName());
            assertEquals(Integer.valueOf(2), unknown.getContext().getPriority());
            assertEquals(Integer.valueOf(1), unknown.getContext().getPreemptionThreshold());
            assertEquals(TraceXEventCategory.THREADX, unknown.getMetadata().getCategory());
        }
    }

    /** Tests invalid slots and truncation without discarding any raw word. */
    @Test
    public void testInvalidUnknownAndTruncatedEvents() throws IOException {
        TraceXTestData.Fixture fixture = TraceXTestData.create(ByteOrder.LITTLE_ENDIAN,
                0x1000, 4, 0, 2, 0);
        byte[] data = fixture.fileBytes();
        putEvent(data, fixture, 0, 0, 0x12345678, 0xffffffffL,
                0xffffffffL, 1, 2, 3, 4);
        putEvent(data, fixture, 1, 0x4567, 0x89abcdefL, 777,
                0x76543210L, 0xffffffffL, 0x80000000L, 0x7fffffffL, 0);

        try (TraceXFile file = TraceXFile.open(new TraceXRegistryTest.ArraySource(data))) {
            TraceXEvent unused = file.readEvent(0);
            assertEquals(TraceXContext.Kind.UNUSED, unused.getContext().getKind());
            assertEquals(TraceXEventCategory.INVALID, unused.getMetadata().getCategory());
            assertEquals("TX_TRACE_INVALID_EVENT", unused.getMetadata().getName()); //$NON-NLS-1$
            assertEquals(1, unused.getInformationWord(1));

            TraceXEvent unknown = file.readEvent(1);
            assertEquals(TraceXEventCategory.USBX, unknown.getMetadata().getCategory());
            assertEquals("USBX_EVENT_0309", unknown.getMetadata().getName()); //$NON-NLS-1$
            assertEquals(0xffffffffL, unknown.getInformationWord(1));
            assertEquals(0x80000000L, unknown.getInformationWord(2));
            assertEquals(0x7fffffffL, unknown.getInformationWord(3));
            assertEquals(0, unknown.getInformationWord(4));

            assertThrows(IllegalArgumentException.class, () -> file.readEvent(-1));
            assertThrows(IllegalArgumentException.class, () -> file.readEvent(2));
        }

        TraceXRegistry registry = emptyRegistry(ByteOrder.BIG_ENDIAN);
        assertEquals("Truncated TraceX event record", //$NON-NLS-1$
                assertThrows(TraceXFormatException.class,
                        () -> TraceXEvent.decode(ByteBuffer.allocate(31),
                                ByteOrder.BIG_ENDIAN, registry)).getMessage());

        byte[] truncated = Arrays.copyOf(data, (int) fixture.fEventEnd - 3);
        try (TraceXFile file = TraceXFile.open(
                new TraceXRegistryTest.ArraySource(truncated, fixture.fFileSize))) {
            assertEquals("Truncated TraceX event record", //$NON-NLS-1$
                    assertThrows(TraceXFormatException.class,
                            () -> file.readEvent(1)).getMessage());
        }

        try (TraceXFile closed = TraceXFile.open(new TraceXRegistryTest.ArraySource(data))) {
            closed.close();
            assertEquals("TraceX file is closed", //$NON-NLS-1$
                    assertThrows(IOException.class, closed::getRegistry).getMessage());
            assertEquals("TraceX file is closed", //$NON-NLS-1$
                    assertThrows(IOException.class, () -> closed.readEvent(0)).getMessage());
        }
    }

    private static TraceXRegistry emptyRegistry(ByteOrder order) throws IOException {
        TraceXTestData.Fixture fixture = TraceXTestData.create(order, 0x1000, 4, 0, 1, 0);
        try (TraceXFile file = TraceXFile.open(
                new TraceXRegistryTest.ArraySource(fixture.fileBytes()))) {
            return file.getRegistry();
        }
    }

    private static void assertGeneric(int id, TraceXEventCategory category,
            String name) {
        TraceXEventMetadata metadata = TraceXMetadataCatalog.lookup(id);
        assertEquals(id, metadata.getId());
        assertEquals(category, metadata.getCategory());
        assertEquals(name, metadata.getName());
        assertTrue(metadata.getAliases().isEmpty());
    }

    private static void putEvent(byte[] data, TraceXTestData.Fixture fixture,
            int index, long context, long priority, long id, long timestamp,
            long info1, long info2, long info3, long info4) {
        int offset = (int) (fixture.fEventStart + index * 32L);
        ByteBuffer.wrap(data).order(fixture.fOrder).position(offset)
                .putInt((int) context).putInt((int) priority).putInt((int) id)
                .putInt((int) timestamp).putInt((int) info1).putInt((int) info2)
                .putInt((int) info3).putInt((int) info4);
    }
}
