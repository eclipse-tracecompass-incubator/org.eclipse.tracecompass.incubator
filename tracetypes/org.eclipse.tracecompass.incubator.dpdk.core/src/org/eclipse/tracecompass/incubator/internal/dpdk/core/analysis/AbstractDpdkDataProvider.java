/**********************************************************************
 * Copyright (c) 2024 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.lcore.analysis.DpdkLogicalCoreAnalysisModule;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.X11ColorUtils;
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
import org.eclipse.tracecompass.tmf.core.presentation.IPaletteProvider;
import org.eclipse.tracecompass.tmf.core.presentation.RGBAColor;
import org.eclipse.tracecompass.tmf.core.presentation.RotatingPaletteProvider;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

/**
 * Base data provider that will be used for all analysis.
 *
 * It is a generic implementation that will just create a tree and provide rows
 * based on what is available in the State system.
 *
 * @author Adel Belkhiri
 * @author Arnaud Fiorini
 */
@SuppressWarnings("restriction")
public abstract class AbstractDpdkDataProvider extends AbstractTimeGraphDataProvider<DpdkLogicalCoreAnalysisModule, TimeGraphEntryModel> implements IOutputStyleProvider {

    private static final int NUM_COLORS = 360;

    private static final Map<String, OutputElementStyle> STYLES;

    static {
        IPaletteProvider palette = new RotatingPaletteProvider.Builder().setNbColors(NUM_COLORS).build();
        int i = 0;
        ImmutableMap.Builder<String, OutputElementStyle> builder = new ImmutableMap.Builder<>();
        for (RGBAColor color : palette.get()) {
            builder.put(String.valueOf(i), new OutputElementStyle(null, ImmutableMap.of(
                    StyleProperties.STYLE_NAME, String.valueOf(i),
                    StyleProperties.BACKGROUND_COLOR, X11ColorUtils.toHexColor(color.getRed(), color.getGreen(), color.getBlue()),
                    StyleProperties.OPACITY, (float) color.getAlpha() / 255)));
            i++;
        }
        STYLES = builder.build();
    }

    /**
     * Default constructor
     *
     * @param trace
     *            The trace that was analyzed
     * @param analysisModule
     *            The analysis module that provides the state system
     */
    protected AbstractDpdkDataProvider(ITmfTrace trace, DpdkLogicalCoreAnalysisModule analysisModule) {
        super(trace, analysisModule);
    }

    @Override
    public TmfModelResponse<List<ITimeGraphArrow>> fetchArrows(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, Status.COMPLETED, "Not supported"); //$NON-NLS-1$
    }

    @Override
    public TmfModelResponse<Map<String, String>> fetchTooltip(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, Status.COMPLETED, "Not supported"); //$NON-NLS-1$
    }

    @Override
    public TmfModelResponse<OutputStyleModel> fetchStyle(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(new OutputStyleModel(STYLES), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    protected @Nullable TimeGraphModel getRowModel(ITmfStateSystem ss, Map<String, Object> parameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        TreeMultimap<Integer, ITmfStateInterval> intervals = TreeMultimap.create(Comparator.naturalOrder(),
                Comparator.comparing(ITmfStateInterval::getStartTime));
        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(parameters);
        if (filter == null) {
            return null;
        }
        Map<Long, Integer> entries = getSelectedEntries(filter);
        Collection<Long> times = getTimes(filter, ss.getStartTime(), ss.getCurrentEndTime());

        for (ITmfStateInterval interval : ss.query2D(entries.values(), times)) {
            if (monitor != null && monitor.isCanceled()) {
                return new TimeGraphModel(Collections.emptyList());
            }
            intervals.put(interval.getAttribute(), interval);
        }

        Map<Integer, Predicate<Multimap<String, Object>>> predicates = new HashMap<>();
        Multimap<Integer, String> regexesMap = DataProviderParameterUtils.extractRegexFilter(parameters);
        if (regexesMap != null) {
            predicates.putAll(computeRegexPredicate(regexesMap));
        }

        List<ITimeGraphRowModel> rows = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : entries.entrySet()) {
            if (monitor != null && monitor.isCanceled()) {
                return new TimeGraphModel(Collections.emptyList());
            }

            List<ITimeGraphState> eventList = new ArrayList<>();
            for (ITmfStateInterval interval : intervals.get(entry.getValue())) {
                long startTime = interval.getStartTime();
                long duration = interval.getEndTime() - startTime + 1;
                Object valObject = interval.getValue();
                if (valObject instanceof String) {
                    TimeGraphState state = new TimeGraphState(startTime, duration, Math.floorMod(valObject.hashCode(), NUM_COLORS),
                            (String) valObject, STYLES.get(String.valueOf(Math.floorMod(valObject.hashCode(), NUM_COLORS))));
                    applyFilterAndAddState(eventList, state, entry.getKey(), predicates, monitor);
                }
            }
            rows.add(new TimeGraphRowModel(entry.getKey(), eventList));
        }
        return new TimeGraphModel(rows);
    }

    @Override
    protected boolean isCacheable() {
        return true;
    }

    @Override
    protected TmfTreeModel<TimeGraphEntryModel> getTree(ITmfStateSystem ss, Map<String, Object> parameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        Builder<TimeGraphEntryModel> builder = new Builder<>();
        long parentId = getId(ITmfStateSystem.ROOT_ATTRIBUTE);
        builder.add(new TimeGraphEntryModel(parentId, -1, String.valueOf(getTrace().getName()), ss.getStartTime(), ss.getCurrentEndTime()));
        addChildren(ss, builder, ITmfStateSystem.ROOT_ATTRIBUTE, parentId);
        return new TmfTreeModel<>(Collections.emptyList(), builder.build());
    }

    private void addChildren(ITmfStateSystem ss, Builder<TimeGraphEntryModel> builder, int quark, long parentId) {
        for (Integer child : ss.getSubAttributes(quark, false)) {
            long childId = getId(child);
            String name = ss.getAttributeName(child);
            builder.add(new TimeGraphEntryModel(childId, parentId, name, ss.getStartTime(), ss.getCurrentEndTime(), true));
            addChildren(ss, builder, child, childId);
        }
    }
}
