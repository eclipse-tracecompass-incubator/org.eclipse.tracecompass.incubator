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

import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxTidAspect;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXAspects;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXControlHeader;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXEvent;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXFile;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import java.lang.reflect.Proxy;
import org.junit.Test;

/** Tests typed event fields, OS-model aspects, and trace-local thread identities. */
public class TraceXAspectsTest {
    private static final long A = 0xf0001000L;
    private static final long B = 0xf0002000L;

    /** Tests values, unsigned fields, duplicate names, and immutable TID mappings. */
    @Test
    public void testFieldsAndThreadIdentity() throws Exception {
        Path path = fixture();
        TraceXTrace trace = new TraceXTrace();
        try {
            trace.initTrace(null, path.toString(), null);
            assertEquals(1, trace.getThreadId(A));
            assertEquals(2, trace.getThreadId(B));
            assertEquals(0, trace.getThreadId(0));
            assertEquals(-1, trace.getThreadId(0x12345678L));
            assertEquals(Map.of(1, "same", 2, "same"), trace.getThreadNames()); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(Map.of(1, 5, 2, 9), trace.getThreadPriorities());
            assertThrows(UnsupportedOperationException.class, () -> trace.getThreadNames().put(3, "bad")); //$NON-NLS-1$
            assertThrows(UnsupportedOperationException.class, () -> trace.getThreadPriorities().put(3, 7));
            Map<String, ITmfEventAspect<?>> aspects = aspects();
            ITmfEvent event = Objects.requireNonNull(trace.getNext(trace.seekEvent(0)));
            assertEquals("THREAD", Objects.requireNonNull(aspects.get("Context")).resolve(event)); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("0xF0001000", Objects.requireNonNull(aspects.get("Context address")).resolve(event)); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(7, Objects.requireNonNull(aspects.get("Priority")).resolve(event)); //$NON-NLS-1$
            assertEquals(9, Objects.requireNonNull(aspects.get("Preemption threshold")).resolve(event)); //$NON-NLS-1$
            assertEquals(0L, Objects.requireNonNull(aspects.get("Relative ticks")).resolve(event)); //$NON-NLS-1$
            assertEquals(0L, Objects.requireNonNull(aspects.get("Logical rank")).resolve(event)); //$NON-NLS-1$
            assertEquals(0xffffffffL, Objects.requireNonNull(aspects.get("Info word 1")).resolve(event)); //$NON-NLS-1$
            assertTrue(((Long) Objects.requireNonNull(Objects.requireNonNull(aspects.get("Physical offset")).resolve(event))).longValue() > 0); //$NON-NLS-1$
            for (ITmfEventAspect<?> aspect : TraceXAspects.getAspects()) {
                assertNotNull(aspect.getName());
                assertNotNull(aspect.getHelpText());
                assertNotNull(aspect.resolve(event));
                if (aspect instanceof LinuxTidAspect) {
                    assertEquals(1, aspect.resolve(event));
                }
                if (aspect instanceof TmfCpuAspect) {
                    assertEquals(0, aspect.resolve(event));
                }
            }
            ITmfEvent interrupt = Objects.requireNonNull(trace.getNext(trace.seekEvent(1)));
            ITmfEvent unresolved = Objects.requireNonNull(trace.getNext(trace.seekEvent(2)));
            for (ITmfEventAspect<?> aspect : TraceXAspects.getAspects()) {
                if (aspect instanceof LinuxTidAspect) {
                    assertNull(aspect.resolve(interrupt));
                    assertNull(aspect.resolve(unresolved));
                }
            }
            assertNull(Objects.requireNonNull(aspects.get("Preemption threshold")).resolve(interrupt)); //$NON-NLS-1$
        } finally {
            trace.dispose();
            Files.deleteIfExists(path);
        }
        assertTrue(trace.getThreadNames().isEmpty());
        assertTrue(trace.getThreadPriorities().isEmpty());
        assertEquals(-1, trace.getThreadId(A));
    }

    /** Tests unused records, non-thread objects, duplicates, and absent priorities. */
    @Test
    public void testIncompleteRegistry() throws Exception {
        var fixture = TraceXTestData.create(ByteOrder.LITTLE_ENDIAN, 0x70000000L, 16, 4, 2, 1);
        byte[] data = fixture.fileBytes();
        fixture.putThread(data, 0, A, "first"); //$NON-NLS-1$
        fixture.putThread(data, 1, A, "duplicate"); //$NON-NLS-1$
        fixture.putObject(data, 2, 2, B, "queue"); //$NON-NLS-1$
        data[TraceXControlHeader.SIZE + 2] = 0;
        fixture.putEvent(data, 0, A, 7, 100, 0, 0, 0, 0, 0);
        Path path = Files.createTempFile("tracex-partial-registry-", ".trx"); //$NON-NLS-1$ //$NON-NLS-2$
        Files.write(path, data);
        TraceXTrace trace = new TraceXTrace();
        try {
            trace.initTrace(null, path.toString(), null);
            assertEquals(Map.of(1, "first"), trace.getThreadNames()); //$NON-NLS-1$
            assertTrue(trace.getThreadPriorities().isEmpty());
            assertEquals(-1, trace.getThreadId(B));
        } finally {
            trace.dispose();
            Files.deleteIfExists(path);
        }
    }

    /** Tests unrelated events, missing position fields, and a foreign trace. */
    @Test
    public void testAbsentAndForeignValues() throws Exception {
        ITmfTrace foreignTrace = (ITmfTrace) Proxy.newProxyInstance(ITmfTrace.class.getClassLoader(),
                new Class<?>[] { ITmfTrace.class }, (proxy, method, arguments) -> null);
        ITmfEvent foreign = event("other", foreignTrace); //$NON-NLS-1$
        for (ITmfEventAspect<?> aspect : TraceXAspects.getAspects()) {
            if (aspect != TmfBaseAspects.getTimestampAspect()
                    && aspect != TmfBaseAspects.getEventTypeAspect()
                    && aspect != TmfBaseAspects.getContentsAspect()) {
                assertNull(aspect.getName(), aspect.resolve(foreign));
            }
        }
        Path path = fixture();
        try (TraceXFile input = TraceXFile.open(path)) {
            TraceXEvent raw = input.readEvent(0);
            ITmfEvent unpositioned = event(raw, foreignTrace);
            TraceXTrace uninitialized = new TraceXTrace();
            try {
                for (ITmfEventAspect<?> aspect : TraceXAspects.getAspects()) {
                    if (aspect instanceof LinuxTidAspect) {
                        assertNull(aspect.resolve(event(raw, uninitialized)));
                    }
                }
            } finally {
                uninitialized.dispose();
            }
            Map<String, ITmfEventAspect<?>> aspects = aspects();
            assertNull(Objects.requireNonNull(aspects.get("Relative ticks")).resolve(unpositioned)); //$NON-NLS-1$
            assertNull(Objects.requireNonNull(aspects.get("Logical rank")).resolve(unpositioned)); //$NON-NLS-1$
            assertNull(Objects.requireNonNull(aspects.get("Physical offset")).resolve(unpositioned)); //$NON-NLS-1$
            for (ITmfEventAspect<?> aspect : TraceXAspects.getAspects()) {
                if (aspect instanceof LinuxTidAspect) {
                    assertNull(aspect.resolve(unpositioned));
                }
            }
            assertFalse(aspects.isEmpty());
        } finally {
            Files.deleteIfExists(path);
        }
    }

    private static Map<String, ITmfEventAspect<?>> aspects() {
        Map<String, ITmfEventAspect<?>> result = new LinkedHashMap<>();
        for (ITmfEventAspect<?> aspect : TraceXAspects.getAspects()) {
            result.put(aspect.getName(), aspect);
        }
        return result;
    }

    private static ITmfEvent event(Object value, ITmfTrace trace) {
        return new TmfEvent(trace, 0, TmfTimestamp.fromNanos(0),
                new TmfEventType("test", null), //$NON-NLS-1$
                new TmfEventField(ITmfEventField.ROOT_FIELD_ID, value, null));
    }

    private static Path fixture() throws Exception {
        var fixture = TraceXTestData.create(ByteOrder.BIG_ENDIAN, 0xe0000000L, 16, 2, 4, 3);
        byte[] data = fixture.fileBytes();
        fixture.putThread(data, 0, A, "same"); //$NON-NLS-1$
        fixture.putThread(data, 1, B, "same"); //$NON-NLS-1$
        data[TraceXControlHeader.SIZE + 32 + 3] = 9;
        fixture.putEvent(data, 0, A, 0x80090007L, 100, 10, 0xffffffffL, 2, 3, 4);
        fixture.putEvent(data, 1, 0xffffffffL, A, 3, 11, 0, 7, 0, 0);
        fixture.putEvent(data, 2, 0x12345678L, 7, 100, 12, 0, 0, 0, 0);
        Path path = Files.createTempFile("tracex-aspects-", ".trx"); //$NON-NLS-1$ //$NON-NLS-2$
        Files.write(path, data);
        return path;
    }
}
