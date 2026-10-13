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
import static org.junit.Assert.assertTrue;

import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.tracex.core.analysis.TraceXAnalysis;
import org.eclipse.tracecompass.incubator.internal.tracex.core.analysis.TraceXDataProvider;
import org.eclipse.tracecompass.incubator.internal.tracex.core.analysis.TraceXStateProvider;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTrace;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.model.CoreFilterProperty;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.junit.Test;

/** Tests TraceX execution transitions and the shared time-graph provider. */
@SuppressWarnings("restriction")
public class TraceXExecutionAnalysisTest {

    private static final long THREAD_A = 0x10001000L;
    private static final long THREAD_B = 0x10002000L;
    private static final long UNKNOWN_THREAD = 0x10003000L;
    private static final long ISR_CONTEXT = 0xFFFFFFFFL;
    private static final long INITIALIZATION_CONTEXT = 0xF0F0F0F0L;

    /** Tests direct switches, idle, nested interrupts, and recovery. */
    @Test
    public void testExecutionTransitions() throws Exception {
        TraceXTestData.Fixture fixture = TraceXTestData.create(
                ByteOrder.LITTLE_ENDIAN, 0x70000000L, 16, 2, 12, 11);
        byte[] data = fixture.fileBytes();
        fixture.putThread(data, 0, THREAD_A, "alpha"); //$NON-NLS-1$
        fixture.putThread(data, 1, THREAD_B, "beta"); //$NON-NLS-1$
        put(fixture, data, 0, INITIALIZATION_CONTEXT, 0, 6, 0, 0, 0);
        put(fixture, data, 1, INITIALIZATION_CONTEXT, 0, 1, 1, 0, THREAD_A);
        put(fixture, data, 2, ISR_CONTEXT, THREAD_A, 3, 2, 7, 0);
        put(fixture, data, 3, ISR_CONTEXT, THREAD_A, 3, 3, 8, 0);
        put(fixture, data, 4, ISR_CONTEXT, THREAD_A, 4, 4, 8, 0);
        put(fixture, data, 5, ISR_CONTEXT, THREAD_A, 1, 5, 0, THREAD_B);
        put(fixture, data, 6, ISR_CONTEXT, THREAD_A, 4, 6, 99, 0);
        put(fixture, data, 7, ISR_CONTEXT, THREAD_A, 4, 7, 7, 0);
        put(fixture, data, 8, THREAD_B, 0, 2, 8, 0, 0);
        put(fixture, data, 9, THREAD_B, 0, 1, 9, 0, UNKNOWN_THREAD);
        put(fixture, data, 10, UNKNOWN_THREAD, 0, 5, 10, THREAD_A, 0);

        try (ExecutionFixture execution = analyze(data)) {
            ITmfStateSystem ss = execution.fStateSystem;
            int cpu = ss.getQuarkAbsolute(TraceXStateProvider.CPU,
                    TraceXStateProvider.EXECUTION);
            assertState(ss, cpu, 0, "INITIALIZATION|"); //$NON-NLS-1$
            assertState(ss, cpu, 1, "THREAD|" + THREAD_A); //$NON-NLS-1$
            assertState(ss, cpu, 2, "ISR|"); //$NON-NLS-1$
            assertState(ss, cpu, 3, "|ISR 8"); //$NON-NLS-1$
            assertState(ss, cpu, 4, "|ISR 7"); //$NON-NLS-1$
            assertState(ss, cpu, 6, "THREAD|" + THREAD_B); //$NON-NLS-1$
            assertState(ss, cpu, 7, "THREAD|" + THREAD_B); //$NON-NLS-1$
            assertState(ss, cpu, 8, "IDLE|0|Idle"); //$NON-NLS-1$
            assertState(ss, cpu, 9, "UNKNOWN|" + UNKNOWN_THREAD); //$NON-NLS-1$
            assertState(ss, cpu, 10, "THREAD|" + THREAD_A); //$NON-NLS-1$

            int diagnostics = ss.getQuarkAbsolute(TraceXStateProvider.DIAGNOSTICS,
                    TraceXStateProvider.UNMATCHED_ISR_EXITS);
            assertEquals(Long.valueOf(1),
                    ss.querySingleState(10, diagnostics).getValue());
            assertEquals(2, new TraceXStateProvider(execution.fTrace).getVersion());
        }
    }

    /** Tests deterministic rank order when execution transitions share a time. */
    @Test
    public void testEqualTimestampTransitions() throws Exception {
        TraceXTestData.Fixture fixture = TraceXTestData.create(
                ByteOrder.LITTLE_ENDIAN, 0x71000000L, 16, 2, 5, 4);
        byte[] data = fixture.fileBytes();
        fixture.putThread(data, 0, THREAD_A, "alpha"); //$NON-NLS-1$
        fixture.putThread(data, 1, THREAD_B, "beta"); //$NON-NLS-1$
        put(fixture, data, 0, THREAD_A, 0, 6, 0, 0, 0);
        put(fixture, data, 1, THREAD_A, 0, 5, 1, THREAD_B, 0);
        put(fixture, data, 2, THREAD_B, 0, 2, 1, 0, 0);
        put(fixture, data, 3, THREAD_B, 0, 1, 1, 0, THREAD_A);

        try (ExecutionFixture execution = analyze(data)) {
            int cpu = execution.fStateSystem.getQuarkAbsolute(
                    TraceXStateProvider.CPU, TraceXStateProvider.EXECUTION);
            assertState(execution.fStateSystem, cpu, 1,
                    "THREAD|" + THREAD_A); //$NON-NLS-1$
        }
    }

    /** Tests initial idle, incomplete ISR, and recovery from an unknown ISR. */
    @Test
    public void testBoundaryContexts() throws Exception {
        TraceXTestData.Fixture boundary = TraceXTestData.create(
                ByteOrder.LITTLE_ENDIAN, 0x71500000L, 16, 1, 5, 4);
        byte[] boundaryData = boundary.fileBytes();
        boundary.putThread(boundaryData, 0, THREAD_A, "alpha"); //$NON-NLS-1$
        put(boundary, boundaryData, 0, INITIALIZATION_CONTEXT, 0, 6, 0, 0, 0);
        put(boundary, boundaryData, 1, INITIALIZATION_CONTEXT, 0, 1, 1, 0, 0);
        put(boundary, boundaryData, 2, INITIALIZATION_CONTEXT, 0, 1, 2, 0,
                THREAD_A);
        put(boundary, boundaryData, 3, ISR_CONTEXT, THREAD_A, 3, 3, 9, 0);

        try (ExecutionFixture execution = analyze(boundaryData)) {
            ITmfStateSystem ss = execution.fStateSystem;
            int cpu = ss.getQuarkAbsolute(TraceXStateProvider.CPU,
                    TraceXStateProvider.EXECUTION);
            assertState(ss, cpu, 0, "INITIALIZATION|"); //$NON-NLS-1$
            assertState(ss, cpu, 1, "IDLE|0|Idle"); //$NON-NLS-1$
            assertState(ss, cpu, 2, "THREAD|" + THREAD_A); //$NON-NLS-1$
            assertState(ss, cpu, 3, "|ISR 9"); //$NON-NLS-1$
            assertEquals(3, ss.querySingleState(3, cpu).getEndTime());
        }

        TraceXTestData.Fixture initialIsr = TraceXTestData.create(
                ByteOrder.LITTLE_ENDIAN, 0x71600000L, 16, 1, 3, 2);
        byte[] initialIsrData = initialIsr.fileBytes();
        initialIsr.putThread(initialIsrData, 0, THREAD_A, "alpha"); //$NON-NLS-1$
        put(initialIsr, initialIsrData, 0, ISR_CONTEXT, THREAD_A, 112, 0, 0, 0);
        put(initialIsr, initialIsrData, 1, ISR_CONTEXT, THREAD_A, 4, 1, 42, 0);

        try (ExecutionFixture execution = analyze(initialIsrData)) {
            ITmfStateSystem ss = execution.fStateSystem;
            int cpu = ss.getQuarkAbsolute(TraceXStateProvider.CPU,
                    TraceXStateProvider.EXECUTION);
            assertState(ss, cpu, 0, "ISR (unknown)"); //$NON-NLS-1$
            assertState(ss, cpu, 1, "THREAD|" + THREAD_A); //$NON-NLS-1$
        }

        ExposedAnalysis exposed = new ExposedAnalysis();
        try {
            assertEquals(TraceXAnalysis.ID + ".v2.ht", //$NON-NLS-1$
                    exposed.stateSystemFileName());
        } finally {
            exposed.dispose();
        }
    }

    /** Tests provider tree, rows, tooltips, styles, and cancellation. */
    @Test
    public void testDataProvider() throws Exception {
        TraceXTestData.Fixture fixture = TraceXTestData.create(
                ByteOrder.LITTLE_ENDIAN, 0x72000000L, 16, 2, 6, 5);
        byte[] data = fixture.fileBytes();
        fixture.putThread(data, 0, THREAD_A, "alpha"); //$NON-NLS-1$
        fixture.putThread(data, 1, THREAD_B, "beta"); //$NON-NLS-1$
        put(fixture, data, 0, THREAD_A, 0, 6, 0, 0, 0);
        put(fixture, data, 1, THREAD_A, 0, 5, 1, THREAD_B, 0);
        put(fixture, data, 2, ISR_CONTEXT, THREAD_B, 3, 2, 12, 0);
        put(fixture, data, 3, ISR_CONTEXT, THREAD_B, 4, 3, 12, 0);
        put(fixture, data, 4, THREAD_B, 0, 2, 4, 0, 0);

        try (ExecutionFixture execution = analyze(data)) {
            TraceXDataProvider provider = new TraceXDataProvider(execution.fTrace,
                    execution.fAnalysis);
            Map<String, Object> treeParameters = FetchParametersUtils.timeQueryToMap(
                    new TimeQueryFilter(0, 4, 5));
            TmfModelResponse<TmfTreeModel<@NonNull TimeGraphEntryModel>> treeResponse = provider
                    .fetchTree(treeParameters, new NullProgressMonitor());
            assertEquals(Status.COMPLETED, treeResponse.getStatus());
            TmfTreeModel<@NonNull TimeGraphEntryModel> tree = treeResponse.getModel();
            assertNotNull(tree);
            TimeGraphEntryModel cpu = find(tree.getEntries(), "Execution"); //$NON-NLS-1$
            TimeGraphEntryModel alpha = findContaining(tree.getEntries(), "alpha"); //$NON-NLS-1$
            assertNotNull(cpu);
            assertNotNull(alpha);
            assertTrue(cpu.hasRowModel());

            SelectionTimeQueryFilter rowFilter = new SelectionTimeQueryFilter(0,
                    4, 5, List.of(cpu.getId(), alpha.getId()));
            Map<String, Object> rowParameters = FetchParametersUtils
                    .selectionTimeQueryToMap(rowFilter);
            TmfModelResponse<TimeGraphModel> rowsResponse = provider
                    .fetchRowModel(rowParameters, new NullProgressMonitor());
            assertEquals(Status.COMPLETED, rowsResponse.getStatus());
            TimeGraphModel rows = rowsResponse.getModel();
            assertNotNull(rows);
            assertEquals(2, rows.getRows().size());
            List<ITimeGraphState> cpuStates = findRow(rows.getRows(), cpu.getId())
                    .getStates();
            assertFalse(cpuStates.isEmpty());
            assertTrue(cpuStates.stream().anyMatch(state -> state.getStartTime() == 2
                    && "ISR 12".equals(state.getLabel()))); //$NON-NLS-1$

            Map<String, Object> filteredParameters = new HashMap<>(rowParameters);
            filteredParameters.put(DataProviderParameterUtils.REGEX_MAP_FILTERS_KEY,
                    Map.of(CoreFilterProperty.EXCLUDE,
                            Collections.singleton("ISR"))); //$NON-NLS-1$
            TimeGraphModel filtered = provider.fetchRowModel(filteredParameters,
                    new NullProgressMonitor()).getModel();
            assertNotNull(filtered);
            List<ITimeGraphState> filteredCpu = findRow(filtered.getRows(),
                    cpu.getId()).getStates();
            assertTrue(filteredCpu.stream().anyMatch(state -> "ISR 12" //$NON-NLS-1$
                    .equals(state.getLabel())
                    && !state.isPropertyActive(CoreFilterProperty.EXCLUDE)));
            assertTrue(filteredCpu.stream().anyMatch(state -> state
                    .isPropertyActive(CoreFilterProperty.EXCLUDE)));

            SelectionTimeQueryFilter tooltipFilter = new SelectionTimeQueryFilter(
                    2, 2, 1, Collections.singleton(cpu.getId()));
            TmfModelResponse<Map<String, String>> tooltip = provider.fetchTooltip(
                    FetchParametersUtils.selectionTimeQueryToMap(tooltipFilter),
                    new NullProgressMonitor());
            assertNotNull(tooltip.getModel());
            assertEquals("ISR", Objects.requireNonNull(tooltip.getModel()) //$NON-NLS-1$
                    .get("Category")); //$NON-NLS-1$

            for (long outsideTime : new long[] { -1, 5, Long.MAX_VALUE }) {
                SelectionTimeQueryFilter outsideFilter = new SelectionTimeQueryFilter(
                        outsideTime, outsideTime, 1, Collections.singleton(cpu.getId()));
                TmfModelResponse<Map<String, String>> outsideTooltip = provider.fetchTooltip(
                        FetchParametersUtils.selectionTimeQueryToMap(outsideFilter), null);
                assertEquals(Status.COMPLETED, outsideTooltip.getStatus());
                assertEquals(Collections.emptyMap(), outsideTooltip.getModel());
            }
            SelectionTimeQueryFilter lastFilter = new SelectionTimeQueryFilter(
                    4, 4, 1, Collections.singleton(cpu.getId()));
            TmfModelResponse<Map<String, String>> lastTooltip = provider.fetchTooltip(
                    FetchParametersUtils.selectionTimeQueryToMap(lastFilter), null);
            assertEquals(Status.COMPLETED, lastTooltip.getStatus());
            assertEquals("Idle", Objects.requireNonNull(lastTooltip.getModel()) //$NON-NLS-1$
                    .get("Category")); //$NON-NLS-1$

            TmfModelResponse<OutputStyleModel> styles = provider.fetchStyle(
                    Collections.emptyMap(), null);
            assertNotNull(styles.getModel());
            assertEquals(5, Objects.requireNonNull(styles.getModel())
                    .getStyles().size());

            NullProgressMonitor cancelled = new NullProgressMonitor();
            cancelled.setCanceled(true);
            assertEquals(Status.CANCELLED,
                    provider.fetchRowModel(rowParameters, cancelled).getStatus());
            assertEquals(Status.CANCELLED,
                    provider.fetchTooltip(FetchParametersUtils
                            .selectionTimeQueryToMap(tooltipFilter), cancelled)
                            .getStatus());
            provider.dispose();
        }
    }

    private static void put(TraceXTestData.Fixture fixture, byte[] data,
            int index, long context, long priority, long eventId, long timestamp,
            long first, long fourth) {
        fixture.putEvent(data, index, context, priority, eventId, timestamp,
                first, eventId == 3 || eventId == 4 ? first : 0, 0, fourth);
    }

    private static ExecutionFixture analyze(byte[] data) throws Exception {
        Path path = Files.createTempFile("tracex-execution", ".trx"); //$NON-NLS-1$ //$NON-NLS-2$
        Files.write(path, data);
        TraceXTrace trace = new TraceXTrace();
        TraceXAnalysis analysis = new TraceXAnalysis();
        try {
            trace.initTrace(null, path.toString(), ITmfEvent.class);
            analysis.setId(TraceXAnalysis.ID);
            assertTrue(analysis.setTrace(trace));
            analysis.schedule();
            assertTrue(analysis.waitForCompletion());
            ITmfStateSystem ss = analysis.getStateSystem();
            assertNotNull(ss);
            return new ExecutionFixture(path, trace, analysis, ss);
        } catch (Exception e) {
            analysis.dispose();
            trace.dispose();
            Files.deleteIfExists(path);
            throw e;
        }
    }

    private static void assertState(ITmfStateSystem ss, int quark, long time,
            String expectedPart) throws Exception {
        ITmfStateInterval interval = ss.querySingleState(time, quark);
        assertNotNull(interval.getValue());
        assertTrue(String.valueOf(interval.getValue()),
                String.valueOf(interval.getValue()).contains(expectedPart));
    }

    private static @org.eclipse.jdt.annotation.Nullable TimeGraphEntryModel find(
            List<TimeGraphEntryModel> entries, String name) {
        return entries.stream().filter(entry -> name.equals(entry.getName()))
                .findFirst().orElse(null);
    }

    private static @org.eclipse.jdt.annotation.Nullable TimeGraphEntryModel findContaining(
            List<TimeGraphEntryModel> entries, String text) {
        return entries.stream().filter(entry -> entry.getName().contains(text))
                .findFirst().orElse(null);
    }

    private static ITimeGraphRowModel findRow(List<ITimeGraphRowModel> rows,
            long id) {
        return rows.stream().filter(row -> row.getEntryID() == id).findFirst()
                .orElseThrow(AssertionError::new);
    }

    private static final class ExecutionFixture implements AutoCloseable {
        private final Path fPath;
        private final TraceXTrace fTrace;
        private final TraceXAnalysis fAnalysis;
        private final ITmfStateSystem fStateSystem;

        private ExecutionFixture(Path path, TraceXTrace trace,
                TraceXAnalysis analysis, ITmfStateSystem stateSystem) {
            fPath = path;
            fTrace = trace;
            fAnalysis = analysis;
            fStateSystem = stateSystem;
        }

        @Override
        public void close() throws Exception {
            fAnalysis.dispose();
            fTrace.dispose();
            Files.deleteIfExists(fPath);
        }
    }

    private static final class ExposedAnalysis extends TraceXAnalysis {
        private String stateSystemFileName() {
            return getSsFileName();
        }
    }
}
