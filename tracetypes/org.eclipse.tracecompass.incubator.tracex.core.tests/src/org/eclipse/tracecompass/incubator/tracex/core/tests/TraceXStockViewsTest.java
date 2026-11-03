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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.tracecompass.analysis.os.linux.core.cpuusage.KernelCpuUsageAnalysis;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.StateValues;
import org.eclipse.tracecompass.analysis.os.linux.core.model.ProcessStatus;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.incubator.internal.tracex.core.analysis.TraceXCpuUsageAnalysis;
import org.eclipse.tracecompass.incubator.internal.tracex.core.analysis.TraceXKernelAnalysis;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXEvent;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAnalysisManager;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;
import org.eclipse.tracecompass.tmf.core.model.ITmfDataProvider;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.junit.Test;

/** Exercises stock OS views through real extension and data-provider APIs. */
@SuppressWarnings("restriction")
public class TraceXStockViewsTest {
    private static final long A = 0x10001000L;
    private static final long B = 0x10002000L;
    private static final long ISR = 0xffffffffL;
    private static final String CONTROL_FLOW = "org.eclipse.tracecompass.internal.analysis.os.linux.core.threadstatus.ThreadStatusDataProvider"; //$NON-NLS-1$
    private static final String RESOURCES = "org.eclipse.tracecompass.internal.analysis.os.linux.core.threadstatus.ResourcesStatusDataProvider"; //$NON-NLS-1$
    private static final String CPU_USAGE = "org.eclipse.tracecompass.analysis.os.linux.core.cpuusage.CpuUsageDataProvider"; //$NON-NLS-1$

    /** Verifies unique registration and stock view discovery without IKernelTrace. */
    @Test
    public void testRegistrationAndStockProviders() throws Exception {
        assertFalse(IKernelTrace.class.isAssignableFrom(TraceXTrace.class));
        for (String id : new String[] { KernelAnalysisModule.ID, KernelCpuUsageAnalysis.ID }) {
            long matches = TmfAnalysisManager.getAnalysisModules().get(id).stream()
                    .filter(helper -> helper.appliesToTraceType(TraceXTrace.class)).count();
            assertEquals(id, 1, matches);
        }
        try (Fixture fixture = fixture()) {
            assertTrue(fixture.trace.getAnalysisModule(KernelAnalysisModule.ID) instanceof TraceXKernelAnalysis);
            assertTrue(fixture.trace.getAnalysisModule(KernelCpuUsageAnalysis.ID) instanceof TraceXCpuUsageAnalysis);
            for (String id : new String[] { CONTROL_FLOW, RESOURCES, CPU_USAGE }) {
                var response = provider(fixture.trace, id).fetchTree(parameters(0, 80), null);
                assertEquals(id, Status.COMPLETED, response.getStatus());
                assertFalse(id, Objects.requireNonNull(response.getModel()).getEntries().isEmpty());
            }
        }
    }

    /** Verifies the stock factories combine TraceX members in an experiment. */
    @Test
    public void testExperimentProviders() throws Exception {
        try (Fixture first = fixture(); Fixture second = fixture()) {
            TmfExperiment experiment = new TmfExperiment(ITmfEvent.class, "TraceX stock views", //$NON-NLS-1$
                    new ITmfTrace[] { first.trace, second.trace }, TmfExperiment.DEFAULT_INDEX_PAGE_SIZE, null);
            TmfTraceOpenedSignal opened = new TmfTraceOpenedSignal(this, experiment, null);
            try {
                experiment.traceOpened(opened);
                TmfTraceManager.getInstance().traceOpened(opened);
                experiment.indexTrace(true);
                for (String id : new String[] { CONTROL_FLOW, RESOURCES, CPU_USAGE }) {
                    var response = provider(experiment, id).fetchTree(parameters(0, 80), null);
                    assertEquals(id, Status.COMPLETED, response.getStatus());
                    var entries = Objects.requireNonNull(response.getModel()).getEntries();
                    assertTrue(id, entries.stream().anyMatch(entry -> entry.getName().equals(first.trace.getName())));
                    assertTrue(id, entries.stream().anyMatch(entry -> entry.getName().equals(second.trace.getName())));
                    DataProviderManager.getInstance().removeDataProvider(experiment, id);
                }
            } finally {
                TmfTraceManager.getInstance().traceClosed(new TmfTraceClosedSignal(this, experiment));
                experiment.dispose();
            }
        }
    }

    /** Verifies initial context, deferred switches, nested IRQs, and CPU totals. */
    @Test
    public void testExecutionAndCpuUsage() throws Exception {
        try (Fixture fixture = fixture()) {
            ITmfStateSystem ss = Objects.requireNonNull(fixture.kernel.getStateSystem());
            assertEquals(ProcessStatus.RUN.getStateValue().unboxValue(), state(ss, 0, "Threads", "1")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(StateValues.CPU_STATUS_RUN_USERMODE, state(ss, 0, "CPUs", "0")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(0, state(ss, 30, "CPUs", "0", "IRQs", "7")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            assertEquals(1, state(ss, 40, "CPUs", "0", "Current_thread")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals(ProcessStatus.WAIT_CPU.getStateValue().unboxValue(), state(ss, 50, "Threads", "1")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(ProcessStatus.RUN.getStateValue().unboxValue(), state(ss, 50, "Threads", "2")); //$NON-NLS-1$ //$NON-NLS-2$
            assertNull(state(ss, 50, "CPUs", "0", "IRQs", "7")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            assertEquals(0, state(ss, 60, "CPUs", "0", "Current_thread")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals(StateValues.CPU_STATUS_IDLE, state(ss, 60, "CPUs", "0")); //$NON-NLS-1$ //$NON-NLS-2$
            Map<String, Long> totals = fixture.cpu.getCpuUsageInRange(Collections.emptySet(), 0, 80);
            assertEquals(Long.valueOf(20), totals.get("total/1")); //$NON-NLS-1$
            assertEquals(Long.valueOf(10), totals.get("total/2")); //$NON-NLS-1$
            assertEquals(Long.valueOf(30), totals.get("total")); //$NON-NLS-1$
            assertEquals(totals, fixture.cpu.getCpuUsageInRange(Set.of(0), -10, 100));
            assertTrue(fixture.cpu.getCpuUsageInRange(Set.of(1), 0, 80).isEmpty());
            assertTrue(fixture.cpu.getCpuUsageInRange(Collections.emptySet(), 90, 100).isEmpty());
            assertTrue(fixture.cpu.getCpuUsageInRange(Collections.emptySet(), 20, 20).isEmpty());
            assertEquals(Long.valueOf(20), fixture.cpu.getCpuUsageInRange(Collections.emptySet(), 5, 75).get("total")); //$NON-NLS-1$
            assertEquals(1, fixture.cpu.getNumberOfCores());
            assertFalse(fixture.cpu.getAnalysisRequirements().iterator().hasNext());
        }
        TraceXCpuUsageAnalysis uninitialized = new TraceXCpuUsageAnalysis();
        try {
            assertTrue(uninitialized.getCpuUsageInRange(Collections.emptySet(), 0, 1).isEmpty());
        } finally {
            uninitialized.dispose();
        }
    }

    /** Verifies Resources accepts unsigned IRQ IDs and retains their full labels. */
    @Test
    public void testUnsignedInterruptNumbers() throws Exception {
        for (long number : new long[] { 0x80000000L, 0xffffffffL }) {
            try (Fixture fixture = fixture(number)) {
                var response = provider(fixture.trace, RESOURCES).fetchTree(parameters(0, 80), null);
                assertEquals(Status.COMPLETED, response.getStatus());
                assertTrue(Objects.requireNonNull(response.getModel()).getEntries().stream()
                        .anyMatch(entry -> entry.getName().contains(Long.toString(number))));
                var ss = Objects.requireNonNull(fixture.kernel.getStateSystem());
                String key = Integer.toString((int) number) + "/" + number; //$NON-NLS-1$
                assertEquals(0, state(ss, 20, "CPUs", "0", "IRQs", key)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                assertNull(state(ss, 50, "CPUs", "0", "IRQs", key)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        }
    }

    /** Verifies names and full Contents survive the addition of typed columns. */
    @Test
    public void testOriginalEventsAndContents() throws Exception {
        try (Fixture fixture = fixture()) {
            boolean contents = false;
            for (var aspect : fixture.trace.getEventAspects()) {
                contents |= aspect == TmfBaseAspects.getContentsAspect();
            }
            assertTrue(contents);
            var custom = provider(fixture.trace, "org.eclipse.tracecompass.incubator.tracex.core.dataprovider"); //$NON-NLS-1$
            assertTrue(Objects.requireNonNull(fixture.trace.getAnalysisModule("org.eclipse.tracecompass.incubator.tracex.core.analysis")).waitForCompletion()); //$NON-NLS-1$
            var tree = Objects.requireNonNull(custom.fetchTree(parameters(0, 80), null).getModel());
            assertFalse(tree.getEntries().stream().anyMatch(entry -> entry.getName().equals("CPUs") || entry.getName().equals("Threads"))); //$NON-NLS-1$ //$NON-NLS-2$
            for (int rank = 0; rank < 9; rank++) {
                ITmfEvent event = Objects.requireNonNull(fixture.trace.getNext(fixture.trace.seekEvent(rank)));
                TraceXEvent raw = (TraceXEvent) event.getContent().getValue();
                assertEquals(raw.getMetadata().getName(), event.getName());
                for (var field : raw.getFields().entrySet()) {
                    assertEquals(field.getValue(), Objects.requireNonNull(event.getContent().getField(field.getKey())).getValue());
                }
                assertNull(event.getContent().getField("prev_tid")); //$NON-NLS-1$
                assertEquals(raw.getFields().size(), event.getContent().getFields().size());
            }
        }
    }

    /** Verifies clock changes rebuild stock analyses and cached providers. */
    @Test
    public void testClockRefresh() throws Exception {
        try (Fixture fixture = fixture()) {
            var previous = provider(fixture.trace, CONTROL_FLOW);
            previous.fetchTree(parameters(0, 80), null);
            fixture.trace.setTicksPerSecond("100000000"); //$NON-NLS-1$
            assertTrue(fixture.kernel.waitForCompletion());
            assertTrue(fixture.cpu.waitForCompletion());
            assertEquals(800, Objects.requireNonNull(fixture.kernel.getStateSystem()).getCurrentEndTime());
            assertEquals(Long.valueOf(300), fixture.cpu.getCpuUsageInRange(Collections.emptySet(), 0, 800).get("total")); //$NON-NLS-1$
            var refreshed = provider(fixture.trace, CONTROL_FLOW);
            assertNotSame(previous, refreshed);
            var tree = refreshed.fetchTree(parameters(0, 800), null);
            assertEquals(Status.COMPLETED, tree.getStatus());
            assertTrue(Objects.requireNonNull(tree.getModel()).getEntries().stream().anyMatch(entry -> ((org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel) entry).getEndTime() == 800));
        }
    }

    private static Map<String, Object> parameters(long start, long end) {
        return FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(start, end, 2));
    }

    private static ITmfTreeDataProvider<? extends ITmfTreeDataModel> provider(ITmfTrace trace, String id) {
        ITmfDataProvider result = DataProviderManager.getInstance()
                .fetchOrCreateDataProvider(trace, id, ITmfDataProvider.class);
        if (result instanceof ITmfTreeDataProvider<?> tree) {
            return tree;
        }
        throw new AssertionError(id);
    }

    private static Object state(ITmfStateSystem ss, long timestamp, String... path) throws Exception {
        return ss.querySingleState(timestamp, ss.getQuarkAbsolute(path)).getValue();
    }

    private static Fixture fixture() throws Exception {
        return fixture(7);
    }

    private static Fixture fixture(long interrupt) throws Exception {
        var input = TraceXTestData.create(ByteOrder.LITTLE_ENDIAN, 0x70000000L, 16, 2, 10, 9);
        byte[] data = input.fileBytes();
        input.putThread(data, 0, A, "alpha"); //$NON-NLS-1$
        input.putThread(data, 1, B, "beta"); //$NON-NLS-1$
        input.putEvent(data, 0, A, 2, 100, 0, 0, 0, 0, 0);
        input.putEvent(data, 1, ISR, A, 3, 10, 0, interrupt, 0, 0);
        input.putEvent(data, 2, ISR, A, 3, 20, 0, interrupt, 0, 0);
        input.putEvent(data, 3, ISR, A, 4, 30, 0, interrupt, 0, 0);
        input.putEvent(data, 4, ISR, A, 1, 40, 0, 0, 0, B);
        input.putEvent(data, 5, ISR, A, 4, 50, 0, interrupt, 0, 0);
        input.putEvent(data, 6, B, 3, 2, 60, 0, 0, 0, 0);
        input.putEvent(data, 7, B, 3, 1, 70, 0, 0, 0, A);
        input.putEvent(data, 8, A, 2, 100, 80, 0, 0, 0, 0);
        Path path = Files.createTempFile("tracex-stock-views-", ".trx"); //$NON-NLS-1$ //$NON-NLS-2$
        Files.write(path, data);
        TraceXTrace trace = new TraceXTrace();
        trace.initTrace(null, path.toString(), null);
        trace.refreshAnalysisModules();
        TmfTraceManager.getInstance().traceOpened(new TmfTraceOpenedSignal(trace, trace, null));
        var kernel = (TraceXKernelAnalysis) Objects.requireNonNull(trace.getAnalysisModule(KernelAnalysisModule.ID));
        var cpu = (TraceXCpuUsageAnalysis) Objects.requireNonNull(trace.getAnalysisModule(KernelCpuUsageAnalysis.ID));
        kernel.schedule();
        cpu.schedule();
        assertTrue(kernel.waitForCompletion());
        assertTrue(cpu.waitForCompletion());
        return new Fixture(path, trace, kernel, cpu);
    }

    private static final class Fixture implements AutoCloseable {
        final Path path;
        final TraceXTrace trace;
        final TraceXKernelAnalysis kernel;
        final TraceXCpuUsageAnalysis cpu;

        Fixture(Path path, TraceXTrace trace, TraceXKernelAnalysis kernel, TraceXCpuUsageAnalysis cpu) {
            this.path = path;
            this.trace = trace;
            this.kernel = kernel;
            this.cpu = cpu;
        }

        @Override
        public void close() throws Exception {
            for (var descriptor : DataProviderManager.getInstance().getAvailableProviders(trace)) {
                DataProviderManager.getInstance().removeDataProvider(trace, descriptor.getId());
            }
            TmfTraceManager.getInstance().traceClosed(new TmfTraceClosedSignal(trace, trace));
            trace.dispose();
            Files.deleteIfExists(path);
        }
    }
}
