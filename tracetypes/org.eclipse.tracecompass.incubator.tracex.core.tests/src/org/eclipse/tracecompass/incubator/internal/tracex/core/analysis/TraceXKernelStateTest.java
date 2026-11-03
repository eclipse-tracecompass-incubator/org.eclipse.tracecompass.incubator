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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.StateValues;
import org.eclipse.tracecompass.analysis.os.linux.core.model.ProcessStatus;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXControlHeader;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;
import org.eclipse.tracecompass.incubator.tracex.core.tests.TraceXTestData;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemFactory;
import org.eclipse.tracecompass.statesystem.core.backend.StateHistoryBackendFactory;
import org.junit.Test;

/** Tests partial-history projection and CPU usage at state-system boundaries. */
@NonNullByDefault({})
public class TraceXKernelStateTest {
    private static final long A = 0x10001000L;
    private static final long B = 0x10002000L;

    /** Tests unknown/initial contexts, identical names, and incomplete interrupts. */
    @Test
    public void testPartialContextsAndPriorities() throws Exception {
        var input = TraceXTestData.create(ByteOrder.LITTLE_ENDIAN, 0x70000000L, 16, 2, 2, 1);
        byte[] data = input.fileBytes();
        input.putThread(data, 0, A, "same"); //$NON-NLS-1$
        input.putThread(data, 1, B, "same"); //$NON-NLS-1$
        data[TraceXControlHeader.SIZE + 32 + 3] = 9;
        input.putEvent(data, 0, A, 7, 100, 0, 0, 0, 0, 0);
        Path path = Files.createTempFile("tracex-kernel-boundaries-", ".trx"); //$NON-NLS-1$ //$NON-NLS-2$
        Files.write(path, data);
        TraceXTrace trace = new TraceXTrace();
        ITmfStateSystemBuilder ss = stateSystem();
        try {
            trace.initTrace(null, path.toString(), null);
            Class<?> type = Class.forName("org.eclipse.tracecompass.incubator.internal.tracex.core.analysis.TraceXKernelState"); //$NON-NLS-1$
            Constructor<?> constructor = type.getDeclaredConstructor(TraceXTrace.class);
            constructor.setAccessible(true);
            Object projection = constructor.newInstance(trace);
            invoke(projection, "seed", new Class<?>[] { ITmfStateSystemBuilder.class, long.class }, ss, 0L); //$NON-NLS-1$
            assertEquals(5, ss.queryOngoing(ss.getQuarkAbsolute("Threads", "1", "Prio"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals(9, ss.queryOngoing(ss.getQuarkAbsolute("Threads", "2", "Prio"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            invoke(projection, "enterInterrupt", new Class<?>[] { ITmfStateSystemBuilder.class, long.class, int.class, long.class }, ss, 1L, 0, 7L); //$NON-NLS-1$
            switchActive(projection, ss, 1, context("isr", new Class<?>[] { long.class }, 7L), context("thread", new Class<?>[] { long.class, String.class }, A, "same")); //$NON-NLS-1$
            assertEquals(ProcessStatus.INTERRUPTED.getStateValue().unboxValue(), ongoing(ss, "Threads", "1")); //$NON-NLS-1$ //$NON-NLS-2$
            invoke(projection, "exitInterrupt", new Class<?>[] { ITmfStateSystemBuilder.class, long.class, int.class }, ss, 2L, 0); //$NON-NLS-1$
            invoke(projection, "exitInterrupt", new Class<?>[] { ITmfStateSystemBuilder.class, long.class, int.class }, ss, 2L, 99); //$NON-NLS-1$
            switchActive(projection, ss, 2, context("thread", new Class<?>[] { long.class, String.class }, B, "same"), null); //$NON-NLS-1$
            assertEquals(ProcessStatus.WAIT_CPU.getStateValue().unboxValue(), ongoing(ss, "Threads", "1")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(ProcessStatus.RUN.getStateValue().unboxValue(), ongoing(ss, "Threads", "2")); //$NON-NLS-1$ //$NON-NLS-2$
            switchActive(projection, ss, 3, null, null);
            assertNull(ongoing(ss, "CPUs", "0", "Current_thread")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            switchActive(projection, ss, 4, context("isr", new Class<?>[] { long.class }, -1L), null);
            assertEquals(StateValues.CPU_STATUS_IRQ, ongoing(ss, "CPUs", "0")); //$NON-NLS-1$ //$NON-NLS-2$
            switchActive(projection, ss, 5, context("thread", new Class<?>[] { long.class, String.class }, 0x12345678L, "missing"), null); //$NON-NLS-1$
            assertNull(ongoing(ss, "CPUs", "0")); //$NON-NLS-1$ //$NON-NLS-2$
            switchActive(projection, ss, 6, context("initialization", new Class<?>[0]), null);
            switchActive(projection, ss, 7, context("unknown", new Class<?>[] { long.class, String.class }, -1L, null), null);
            switchActive(projection, ss, 8, context("idle", new Class<?>[0]), null);
            assertEquals(0, ongoing(ss, "CPUs", "0", "Current_thread")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals(StateValues.CPU_STATUS_IDLE, ongoing(ss, "CPUs", "0")); //$NON-NLS-1$ //$NON-NLS-2$
        } finally {
            ss.dispose();
            trace.dispose();
            Files.deleteIfExists(path);
        }
    }

    /** Tests empty/disposed histories and counts only known, running threads. */
    @Test
    public void testCpuUsageHistoryBoundaries() throws Exception {
        ITmfStateSystemBuilder empty = stateSystem();
        empty.modifyAttribute(0, StateValues.CPU_STATUS_IDLE, empty.getQuarkAbsoluteAndAdd("CPUs", "0")); //$NON-NLS-1$ //$NON-NLS-2$
        empty.closeHistory(10);
        ExposedCpuUsage analysis = new ExposedCpuUsage(empty);
        try {
            assertTrue(analysis.getCpuUsageInRange(Collections.emptySet(), 0, 10).isEmpty());
        } finally {
            analysis.dispose();
            empty.dispose();
        }
        ITmfStateSystemBuilder ss = stateSystem();
        int cpu = ss.getQuarkAbsoluteAndAdd("CPUs", "0"); //$NON-NLS-1$ //$NON-NLS-2$
        int tid = ss.getQuarkRelativeAndAdd(cpu, "Current_thread"); //$NON-NLS-1$
        ss.modifyAttribute(0, StateValues.CPU_STATUS_RUN_USERMODE, cpu);
        ss.modifyAttribute(1, -1, tid);
        ss.modifyAttribute(2, 0, tid);
        ss.modifyAttribute(3, 1, tid);
        ss.modifyAttribute(3, StateValues.CPU_STATUS_IRQ, cpu);
        ss.modifyAttribute(4, StateValues.CPU_STATUS_RUN_USERMODE, cpu);
        ss.modifyAttribute(5, 2, tid);
        ss.modifyAttribute(6, (Object) null, cpu);
        ss.modifyAttribute(7, StateValues.CPU_STATUS_RUN_USERMODE, cpu);
        ss.closeHistory(10);
        analysis = new ExposedCpuUsage(ss);
        try {
            var usage = analysis.getCpuUsageInRange(Collections.emptySet(), 0, 10);
            assertEquals(Long.valueOf(1), usage.get("total/1")); //$NON-NLS-1$
            assertEquals(Long.valueOf(4), usage.get("total/2")); //$NON-NLS-1$
            assertEquals(Long.valueOf(5), usage.get("total")); //$NON-NLS-1$
            ss.dispose();
            assertTrue(analysis.getCpuUsageInRange(Collections.emptySet(), 0, 10).isEmpty());
        } finally {
            analysis.dispose();
            ss.dispose();
        }
    }

    private static Object context(String name, Class<?>[] types, Object... arguments) throws Exception {
        Class<?> type = Class.forName("org.eclipse.tracecompass.incubator.internal.tracex.core.analysis.TraceXExecutionContext"); //$NON-NLS-1$
        Method method = type.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(null, arguments);
    }

    private static void switchActive(Object target, ITmfStateSystemBuilder ss, long timestamp,
            Object next, Object scheduler) throws Exception {
        Class<?> context = Class.forName("org.eclipse.tracecompass.incubator.internal.tracex.core.analysis.TraceXExecutionContext"); //$NON-NLS-1$
        invoke(target, "switchActive", new Class<?>[] { ITmfStateSystemBuilder.class, long.class, context, context }, //$NON-NLS-1$
                ss, timestamp, next, scheduler);
    }

    private static void invoke(Object target, String name, Class<?>[] types, Object... arguments) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(target, arguments);
    }

    private static ITmfStateSystemBuilder stateSystem() {
        return StateSystemFactory.newStateSystem(StateHistoryBackendFactory.createInMemoryBackend("TraceX test", 0)); //$NON-NLS-1$
    }

    private static @Nullable Object ongoing(ITmfStateSystemBuilder ss, String... path) throws Exception {
        return ss.queryOngoing(ss.getQuarkAbsolute(path));
    }

    private static final class ExposedCpuUsage extends TraceXCpuUsageAnalysis {
        private final ITmfStateSystem fStateSystem;

        ExposedCpuUsage(ITmfStateSystem stateSystem) {
            fStateSystem = stateSystem;
        }

        @Override
        public ITmfStateSystem getStateSystem() {
            return fStateSystem;
        }
    }
}
