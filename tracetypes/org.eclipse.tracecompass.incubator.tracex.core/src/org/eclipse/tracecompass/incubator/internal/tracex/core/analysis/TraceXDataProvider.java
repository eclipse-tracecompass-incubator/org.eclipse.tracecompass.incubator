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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.AbstractTimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.Multimap;

/** Shared desktop and server provider for TraceX execution intervals. */
@SuppressWarnings("restriction")
public final class TraceXDataProvider
        extends AbstractTimeGraphDataProvider<TraceXAnalysis, TimeGraphEntryModel>
        implements IOutputStyleProvider {

    private static final Map<String, OutputElementStyle> STYLES = createStyles();

    /**
     * Construct a provider for one trace and its execution analysis.
     *
     * @param trace
     *            initialized trace
     * @param analysis
     *            execution analysis
     */
    public TraceXDataProvider(ITmfTrace trace, TraceXAnalysis analysis) {
        super(trace, analysis);
    }

    @Override
    public String getId() {
        return TraceXDataProviderFactory.ID;
    }

    @Override
    protected boolean isCacheable() {
        return true;
    }

    @Override
    public TmfModelResponse<OutputStyleModel> fetchStyle(
            Map<String, Object> fetchParameters,
            @Nullable IProgressMonitor monitor) {
        return completed(new OutputStyleModel(STYLES));
    }

    @Override
    public TmfModelResponse<List<ITimeGraphArrow>> fetchArrows(
            Map<String, Object> fetchParameters,
            @Nullable IProgressMonitor monitor) {
        return completed(Collections.emptyList());
    }

    @Override
    public TmfModelResponse<Map<String, String>> fetchTooltip(
            Map<String, Object> fetchParameters,
            @Nullable IProgressMonitor monitor) {
        if (monitor != null && monitor.isCanceled()) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED,
                    CommonStatusMessage.TASK_CANCELLED);
        }
        SelectionTimeQueryFilter filter = FetchParametersUtils
                .createSelectionTimeQuery(fetchParameters);
        ITmfStateSystem ss = getAnalysisModule().getStateSystem();
        if (filter == null || ss == null) {
            return completed(Collections.emptyMap());
        }
        Map<Long, Integer> selected = getSelectedEntries(filter);
        if (selected.size() != 1 || filter.getStart() < ss.getStartTime()
                || filter.getStart() > ss.getCurrentEndTime()
                || !getAnalysisModule().isQueryable(filter.getStart())) {
            return completed(Collections.emptyMap());
        }
        try {
            int quark = selected.values().iterator().next();
            ITmfStateInterval interval = ss.querySingleState(filter.getStart(), quark);
            TraceXExecutionContext context = TraceXExecutionContext
                    .deserialize(interval.getValue());
            if (context == null) {
                return completed(Collections.emptyMap());
            }
            Map<String, String> tooltip = new LinkedHashMap<>();
            tooltip.put("Context", context.getName()); //$NON-NLS-1$
            tooltip.put("Category", context.getType().getLabel()); //$NON-NLS-1$
            tooltip.put("Address", context.getAddressText()); //$NON-NLS-1$
            return completed(Collections.unmodifiableMap(tooltip));
        } catch (StateSystemDisposedException e) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED,
                    CommonStatusMessage.TASK_CANCELLED);
        }
    }

    @Override
    protected @Nullable TimeGraphModel getRowModel(ITmfStateSystem ss,
            Map<String, Object> parameters, @Nullable IProgressMonitor monitor)
            throws StateSystemDisposedException {
        SelectionTimeQueryFilter filter = FetchParametersUtils
                .createSelectionTimeQuery(parameters);
        if (filter == null) {
            return null;
        }
        Map<Long, Integer> entries = getSelectedEntries(filter);
        Collection<Long> times = getTimes(filter, ss.getStartTime(),
                ss.getCurrentEndTime());
        Map<Integer, List<ITmfStateInterval>> intervals = new HashMap<>();
        for (ITmfStateInterval interval : ss.query2D(entries.values(), times)) {
            if (monitor != null && monitor.isCanceled()) {
                return null;
            }
            intervals.computeIfAbsent(interval.getAttribute(), key -> new ArrayList<>())
                    .add(interval);
        }
        Map<@NonNull Integer, @NonNull Predicate<@NonNull Multimap<@NonNull String,
                @NonNull Object>>> predicates = new HashMap<>();
        Multimap<@NonNull Integer, @NonNull String> regexes = DataProviderParameterUtils
                .extractRegexFilter(parameters);
        if (regexes != null) {
            predicates.putAll(computeRegexPredicate(regexes));
        }
        List<ITimeGraphRowModel> rows = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : entries.entrySet()) {
            if (monitor != null && monitor.isCanceled()) {
                return null;
            }
            List<ITmfStateInterval> rowIntervals = intervals
                    .getOrDefault(entry.getValue(), Collections.emptyList());
            rowIntervals.sort(Comparator.comparingLong(ITmfStateInterval::getStartTime));
            List<@NonNull ITimeGraphState> states = new ArrayList<>();
            boolean cpuRow = isCpuExecution(ss, entry.getValue());
            for (ITmfStateInterval interval : rowIntervals) {
                TraceXExecutionContext context = TraceXExecutionContext
                        .deserialize(interval.getValue());
                if (context == null) {
                    continue;
                }
                long start = interval.getStartTime();
                long duration = interval.getEndTime() - start + 1;
                String styleKey = context.getType().name().toLowerCase();
                String label = cpuRow ? context.getEntryName() : "Running"; //$NON-NLS-1$
                TimeGraphState state = new TimeGraphState(start, duration,
                        context.getType().ordinal(), label,
                        new OutputElementStyle(styleKey));
                applyFilterAndAddState(states, state, entry.getKey(), predicates,
                        monitor);
            }
            rows.add(new TimeGraphRowModel(entry.getKey(), states));
        }
        return new TimeGraphModel(rows);
    }

    private static boolean isCpuExecution(ITmfStateSystem ss, int quark) {
        String[] path = ss.getFullAttributePathArray(quark);
        return path.length == 2 && TraceXStateProvider.CPU.equals(path[0])
                && TraceXStateProvider.EXECUTION.equals(path[1]);
    }

    @Override
    protected TmfTreeModel<TimeGraphEntryModel> getTree(ITmfStateSystem ss,
            Map<String, Object> parameters, @Nullable IProgressMonitor monitor)
            throws StateSystemDisposedException {
        List<TimeGraphEntryModel> entries = new ArrayList<>();
        long rootId = getId(ITmfStateSystem.ROOT_ATTRIBUTE);
        entries.add(new TimeGraphEntryModel(rootId, -1, getTrace().getName(),
                ss.getStartTime(), ss.getCurrentEndTime(), false));
        for (Integer quark : ss.getSubAttributes(ITmfStateSystem.ROOT_ATTRIBUTE,
                false)) {
            String name = ss.getAttributeName(quark);
            if (!TraceXStateProvider.CPU.equals(name)
                    && !TraceXStateProvider.CONTEXTS.equals(name)) {
                continue;
            }
            addTree(ss, entries, quark, rootId, monitor);
        }
        return new TmfTreeModel<>(Collections.singletonList("Context"), entries); //$NON-NLS-1$
    }

    private void addTree(ITmfStateSystem ss, List<TimeGraphEntryModel> entries,
            int quark, long parentId, @Nullable IProgressMonitor monitor) {
        if (monitor != null && monitor.isCanceled()) {
            return;
        }
        List<Integer> children = ss.getSubAttributes(quark, false);
        long id = getId(quark);
        entries.add(new TimeGraphEntryModel(id, parentId,
                ss.getAttributeName(quark), ss.getStartTime(),
                ss.getCurrentEndTime(), children.isEmpty()));
        for (Integer child : children) {
            addTree(ss, entries, child, id, monitor);
        }
    }

    private static Map<String, OutputElementStyle> createStyles() {
        Map<String, OutputElementStyle> styles = new LinkedHashMap<>();
        addStyle(styles, "thread", "Thread", "#4E9A06"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addStyle(styles, "isr", "Interrupt", "#CC0000"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addStyle(styles, "initialization", "Initialization", "#75507B"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addStyle(styles, "idle", "Idle", "#888A85"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        addStyle(styles, "unknown", "Unknown", "#F57900"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        return Collections.unmodifiableMap(styles);
    }

    private static void addStyle(Map<String, OutputElementStyle> styles,
            String key, String name, String color) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(StyleProperties.STYLE_NAME, name);
        values.put(StyleProperties.BACKGROUND_COLOR, color);
        values.put(StyleProperties.OPACITY, Float.valueOf(1.0f));
        styles.put(key, new OutputElementStyle(null,
                Collections.unmodifiableMap(values)));
    }

    private static <T> TmfModelResponse<T> completed(T model) {
        return new TmfModelResponse<>(model, ITmfResponse.Status.COMPLETED,
                CommonStatusMessage.COMPLETED);
    }
}
