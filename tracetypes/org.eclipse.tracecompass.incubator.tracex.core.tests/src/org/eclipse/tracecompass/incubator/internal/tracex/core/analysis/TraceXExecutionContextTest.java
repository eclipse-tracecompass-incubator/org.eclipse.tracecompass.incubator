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

package org.eclipse.tracecompass.incubator.internal.tracex.core.analysis;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXEvent;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXContext;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXObjectType;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXRegistry;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;
import org.eclipse.tracecompass.incubator.tracex.core.tests.TraceXTestData;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.junit.Test;

/** Tests every execution-context identity and its stored representation. */
public class TraceXExecutionContextTest {

    private static final long THREAD = 0xF0001000L;
    private static final long QUEUE = 0xF0002000L;

    /** Tests event/scheduler mapping, display identities, and serialization. */
    @Test
    public void testContextIdentities() throws Exception {
        TraceXTestData.Fixture fixture = TraceXTestData.create(
                ByteOrder.LITTLE_ENDIAN, 0x70000000L, 16, 2, 1, 0);
        byte[] data = fixture.fileBytes();
        fixture.putThread(data, 0, THREAD, "alpha"); //$NON-NLS-1$
        fixture.putObject(data, 1, TraceXObjectType.QUEUE.getCode(), QUEUE,
                "messages"); //$NON-NLS-1$
        Path path = Files.createTempFile("tracex-context-", ".trx"); //$NON-NLS-1$ //$NON-NLS-2$
        Files.write(path, data);
        TraceXTrace trace = new TraceXTrace();
        try {
            trace.initTrace(null, path.toString(), null);
            TraceXRegistry registry = trace.getRegistry();

            assertContext("THREAD", "Thread", "alpha [0xF0001000]", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    context("fromEventContext", //$NON-NLS-1$
                            new Class<?>[] { TraceXContext.class },
                            decode(THREAD, 0, false, registry)));
            assertContext("ISR", "ISR", "ISR (unknown)", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    context("fromEventContext", //$NON-NLS-1$
                            new Class<?>[] { TraceXContext.class },
                            decode(0xffffffffL, 0, false, registry)));
            assertContext("INITIALIZATION", "Initialization", "Initialization", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    context("fromEventContext", //$NON-NLS-1$
                            new Class<?>[] { TraceXContext.class },
                            decode(0xf0f0f0f0L, 0, false, registry)));
            assertContext("IDLE", "Idle", "Idle", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    context("fromEventContext", //$NON-NLS-1$
                            new Class<?>[] { TraceXContext.class },
                            decode(0, 0, false, registry)));
            assertContext("UNKNOWN", "Unknown", //$NON-NLS-1$ //$NON-NLS-2$
                    "messages [0xF0002000]", //$NON-NLS-1$
                    context("fromEventContext", //$NON-NLS-1$
                            new Class<?>[] { TraceXContext.class },
                            decode(QUEUE, 0, false, registry)));
            assertContext("UNKNOWN", "Unknown", //$NON-NLS-1$ //$NON-NLS-2$
                    "Unknown [0xF0003000]", //$NON-NLS-1$
                    context("fromEventContext", //$NON-NLS-1$
                            new Class<?>[] { TraceXContext.class },
                            decode(0xf0003000L, 0, false, registry)));
            assertContext("UNKNOWN", "Unknown", //$NON-NLS-1$ //$NON-NLS-2$
                    "Unknown [unavailable]", //$NON-NLS-1$
                    context("fromEventContext", //$NON-NLS-1$
                            new Class<?>[] { TraceXContext.class },
                            decode(0, 0, true, registry)));

            assertContext("IDLE", "Idle", "Idle", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    context("scheduler", //$NON-NLS-1$
                            new Class<?>[] { long.class, TraceXRegistry.class },
                            0L, registry));
            assertContext("THREAD", "Thread", "alpha [0xF0001000]", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    context("scheduler", //$NON-NLS-1$
                            new Class<?>[] { long.class, TraceXRegistry.class },
                            THREAD, registry));
            assertContext("UNKNOWN", "Unknown", //$NON-NLS-1$ //$NON-NLS-2$
                    "messages [0xF0002000]", //$NON-NLS-1$
                    context("scheduler", //$NON-NLS-1$
                            new Class<?>[] { long.class, TraceXRegistry.class },
                            QUEUE, registry));
            assertContext("UNKNOWN", "Unknown", //$NON-NLS-1$ //$NON-NLS-2$
                    "Unknown [0xF0003000]", //$NON-NLS-1$
                    context("scheduler", //$NON-NLS-1$
                            new Class<?>[] { long.class, TraceXRegistry.class },
                            0xf0003000L, registry));

            assertContext("THREAD", "Thread", "Thread [0x00000001]", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    context("thread", //$NON-NLS-1$
                            new Class<?>[] { long.class, String.class }, 1L,
                            null));
            assertContext("THREAD", "Thread", "Thread [0x00000001]", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    context("thread", //$NON-NLS-1$
                            new Class<?>[] { long.class, String.class }, 1L,
                            "")); //$NON-NLS-1$
            assertContext("ISR", "ISR", "ISR 12", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    context("isr", new Class<?>[] { long.class }, 12L)); //$NON-NLS-1$
            assertContext("INITIALIZATION", "Initialization", "Initialization", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    context("initialization", new Class<?>[0])); //$NON-NLS-1$
            assertContext("UNKNOWN", "Unknown", "named [0x00000002]", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    context("unknown", //$NON-NLS-1$
                            new Class<?>[] { long.class, String.class }, 2L,
                            "named")); //$NON-NLS-1$

            Object original = context("thread", //$NON-NLS-1$
                    new Class<?>[] { long.class, String.class }, THREAD,
                    "alpha"); //$NON-NLS-1$
            String serialized = (String) call(original, "serialize"); //$NON-NLS-1$
            Object restored = context("deserialize", //$NON-NLS-1$
                    new Class<?>[] { Object.class }, serialized);
            assertContext("THREAD", "Thread", //$NON-NLS-1$ //$NON-NLS-2$
                    (String) call(original, "getEntryName"), restored); //$NON-NLS-1$
            assertEquals(null, context("deserialize", //$NON-NLS-1$
                    new Class<?>[] { Object.class }, (Object) null));
            assertEquals(null, context("deserialize", //$NON-NLS-1$
                    new Class<?>[] { Object.class }, Integer.valueOf(1)));
            assertEquals(null, context("deserialize", //$NON-NLS-1$
                    new Class<?>[] { Object.class }, "THREAD|1")); //$NON-NLS-1$
            assertEquals(null, context("deserialize", //$NON-NLS-1$
                    new Class<?>[] { Object.class }, "BAD|1|name")); //$NON-NLS-1$
            assertEquals(null, context("deserialize", //$NON-NLS-1$
                    new Class<?>[] { Object.class }, "THREAD|bad|name")); //$NON-NLS-1$
        } finally {
            trace.dispose();
            Files.deleteIfExists(path);
        }
    }

    /** Tests state-provider fallbacks and transition boundaries. */
    @Test
    public void testStateProviderBoundaries() throws Exception {
        TraceXTestData.Fixture fixture = TraceXTestData.create(
                ByteOrder.LITTLE_ENDIAN, 0x71000000L, 16, 3, 6, 5);
        byte[] data = fixture.fileBytes();
        fixture.putThread(data, 0, THREAD, "alpha"); //$NON-NLS-1$
        fixture.putObject(data, 1, TraceXObjectType.QUEUE.getCode(), QUEUE,
                "messages"); //$NON-NLS-1$
        fixture.putEvent(data, 0, 0xffffffffL, 0, 3, 0, 0, 7, 0, 0);
        fixture.putEvent(data, 1, 0xffffffffL, THREAD, 6, 1, 0, 0, 0, 0);
        fixture.putEvent(data, 2, THREAD, 0, 6, 2, 0, 0, 0, 0);
        fixture.putEvent(data, 3, 0xffffffffL, THREAD, 109, 3, 0,
                THREAD, 0, 0);
        fixture.putEvent(data, 4, THREAD, 0, 999, 4, 0, 0, 0, 0);
        Path path = Files.createTempFile("tracex-state-provider-", ".trx"); //$NON-NLS-1$ //$NON-NLS-2$
        Files.write(path, data);
        TraceXTrace trace = new TraceXTrace();
        try {
            trace.initTrace(null, path.toString(), null);
            ITmfEvent first = Objects.requireNonNull(
                    trace.getNext(trace.seekEvent(0)));
            assertEquals(TraceXEvent.class,
                    first.getContent().getValue().getClass());

            TraceXStateProvider unassigned = new TraceXStateProvider(trace);
            invokeProvider(unassigned, "eventHandle", //$NON-NLS-1$
                    new Class<?>[] { ITmfEvent.class }, first);
            unassigned.dispose();

            TraceXStateProvider provider = new TraceXStateProvider(trace);
            ITmfStateProvider duplicate = provider.getNewInstance();
            assertEquals(TraceXStateProvider.class, duplicate.getClass());
            duplicate.dispose();

            ITmfStateSystemBuilder builder = stateSystem();
            provider.assignTargetStateSystem(builder);
            ITmfEvent unrelated = new TmfEvent(trace, 0,
                    TmfTimestamp.fromNanos(0),
                    new TmfEventType("unrelated", null), //$NON-NLS-1$
                    new TmfEventField(ITmfEventField.ROOT_FIELD_ID,
                            "not TraceX", null)); //$NON-NLS-1$
            invokeProvider(provider, "eventHandle", //$NON-NLS-1$
                    new Class<?>[] { ITmfEvent.class }, unrelated);

            invokeProvider(provider, "writeScheduler", //$NON-NLS-1$
                    new Class<?>[] { ITmfStateSystemBuilder.class, long.class },
                    builder, 0L);
            Object idle = context("idle", new Class<?>[0]); //$NON-NLS-1$
            invokeSwitch(provider, builder, idle);
            invokeSwitch(provider, builder, null);
            invokeSwitch(provider, builder, null);

            Object idleCopy = context("idle", new Class<?>[0]); //$NON-NLS-1$
            assertEquals(Boolean.TRUE, same(idle, idle));
            assertEquals(Boolean.TRUE, same(idle, idleCopy));
            assertEquals(Boolean.FALSE, same(idle, null));
            assertEquals(Boolean.FALSE, same(null, idle));
            assertEquals(Boolean.TRUE, same(null, null));

            for (int rank = 0; rank < 5; rank++) {
                ITmfEvent event = Objects.requireNonNull(
                        trace.getNext(trace.seekEvent(rank)));
                invokeProvider(provider, "eventHandle", //$NON-NLS-1$
                        new Class<?>[] { ITmfEvent.class }, event);
            }
            provider.dispose();

            TraceXTrace disposed = new TraceXTrace();
            disposed.initTrace(null, path.toString(), null);
            disposed.dispose();
            try {
                new TraceXStateProvider(disposed);
                throw new AssertionError("Expected disposed registry failure"); //$NON-NLS-1$
            } catch (IllegalStateException expected) {
                assertEquals("Cannot read the TraceX registry", //$NON-NLS-1$
                        expected.getMessage());
            }
        } finally {
            trace.dispose();
            Files.deleteIfExists(path);
        }
    }

    private static Class<?> executionClass() throws ClassNotFoundException {
        return Class.forName(
                "org.eclipse.tracecompass.incubator.internal.tracex.core.analysis.TraceXExecutionContext", //$NON-NLS-1$
                true, TraceXAnalysis.class.getClassLoader());
    }

    private static Object context(String name, Class<?>[] parameterTypes,
            Object... arguments) throws Exception {
        Method method = executionClass().getDeclaredMethod(name,
                parameterTypes);
        method.setAccessible(true);
        return method.invoke(null, arguments);
    }

    private static Object call(Object target, String name) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static Object invokeProvider(TraceXStateProvider provider,
            String name, Class<?>[] parameterTypes, Object... arguments)
            throws Exception {
        Method method = TraceXStateProvider.class.getDeclaredMethod(name,
                parameterTypes);
        method.setAccessible(true);
        return method.invoke(provider, arguments);
    }

    private static void invokeSwitch(TraceXStateProvider provider,
            ITmfStateSystemBuilder builder, @Nullable Object context)
            throws Exception {
        invokeProvider(provider, "switchActive", //$NON-NLS-1$
                new Class<?>[] { ITmfStateSystemBuilder.class, long.class,
                        executionClass() },
                builder, 0L, context);
    }

    private static Object same(@Nullable Object left, @Nullable Object right)
            throws Exception {
        Method method = TraceXStateProvider.class.getDeclaredMethod("same", //$NON-NLS-1$
                executionClass(), executionClass());
        method.setAccessible(true);
        return method.invoke(null, left, right);
    }

    private static ITmfStateSystemBuilder stateSystem() {
        Map<String, Integer> quarks = new HashMap<>();
        return (ITmfStateSystemBuilder) java.lang.reflect.Proxy.newProxyInstance(
                ITmfStateSystemBuilder.class.getClassLoader(),
                new Class<?>[] { ITmfStateSystemBuilder.class },
                (proxy, method, arguments) -> {
                    switch (method.getName()) {
                    case "getQuarkAbsoluteAndAdd": //$NON-NLS-1$
                        return quarks.computeIfAbsent(
                                String.join("/", (String[]) arguments[0]), //$NON-NLS-1$
                                key -> Integer.valueOf(quarks.size()));
                    case "modifyAttribute": //$NON-NLS-1$
                        return null;
                    case "getStartTime": //$NON-NLS-1$
                    case "getCurrentEndTime": //$NON-NLS-1$
                        return 0L;
                    default:
                        Class<?> type = method.getReturnType();
                        if (!type.isPrimitive()) {
                            return null;
                        }
                        if (type == boolean.class) {
                            return false;
                        }
                        if (type == long.class) {
                            return 0L;
                        }
                        return 0;
                    }
                });
    }

    private static TraceXContext decode(long address, long priority,
            boolean unused, TraceXRegistry registry) throws Exception {
        Method method = TraceXContext.class.getDeclaredMethod("decode", //$NON-NLS-1$
                long.class, long.class, boolean.class, TraceXRegistry.class);
        method.setAccessible(true);
        return (TraceXContext) method.invoke(null, address, priority, unused,
                registry);
    }

    private static void assertContext(String type, String label,
            String entryName, Object context) throws Exception {
        Object actualType = call(context, "getType"); //$NON-NLS-1$
        assertEquals(type, actualType.toString());
        assertEquals(label, call(actualType, "getLabel")); //$NON-NLS-1$
        assertEquals(entryName, call(context, "getEntryName")); //$NON-NLS-1$
    }
}
