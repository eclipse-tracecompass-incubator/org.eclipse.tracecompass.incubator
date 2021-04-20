/**********************************************************************
 * Copyright (c) 2021 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.communicators;

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
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.internal.tmf.core.model.timegraph.AbstractTimeGraphDataProvider;
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
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse.Status;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

/**
 * Data provider for the OTF2 communicators view
 *
 * @author Yoann Heitz
 */

@SuppressWarnings("restriction")
public class Otf2CommunicatorsDataProvider extends AbstractTimeGraphDataProvider<Otf2CommunicatorsAnalysis, TimeGraphEntryModel> implements IOutputStyleProvider {

    private static final int NUM_COLORS = 360;
    private static final IPaletteProvider PALETTE = new RotatingPaletteProvider.Builder().setNbColors(NUM_COLORS).build();

    /*
     * A map of integers as style names to basic style (only the color changes)
     */
    private static final Map<String, OutputElementStyle> STYLE_MAP;

    static {
        /*
         * Build the different styles with colors provided by a rotating palette
         * provider
         */
        ImmutableMap.Builder<String, OutputElementStyle> builder = new ImmutableMap.Builder<>();
        int i = 0;
        for (RGBAColor color : PALETTE.get()) {
            builder.put(String.valueOf(i), new OutputElementStyle(null, ImmutableMap.of(StyleProperties.STYLE_NAME, String.valueOf(i),
                    StyleProperties.BACKGROUND_COLOR, color.toString().substring(0, 7))));
            i++;
        }
        STYLE_MAP = builder.build();
    }

    @Override
    public TmfModelResponse<OutputStyleModel> fetchStyle(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(new OutputStyleModel(STYLE_MAP), Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    /** Data provider suffix ID */
    public static final String SUFFIX = ".dataprovider"; //$NON-NLS-1$

    /**
     * Constructor
     *
     * @param trace
     *            the trace for this provider
     * @param analysisModule
     *            the corresponding analysis module
     */
    public Otf2CommunicatorsDataProvider(ITmfTrace trace, Otf2CommunicatorsAnalysis analysisModule) {
        super(trace, analysisModule);
    }

    @Override
    public TmfModelResponse<List<ITimeGraphArrow>> fetchArrows(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public TmfModelResponse<Map<String, String>> fetchTooltip(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public String getId() {
        return getAnalysisModule().getId() + SUFFIX;
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

        // Query
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
                if (valObject == null) {
                    TimeGraphState value = new TimeGraphState(startTime, duration, null, null);
                    applyFilterAndAddState(eventList, value, entry.getKey(), predicates, monitor);
                }
                /*
                 * If this is a location quark then the name of the code region
                 * is displayed
                 */
                if (valObject instanceof String) {
                    String name = (String) valObject;
                    int indexColor = Math.floorMod(name.hashCode(), PALETTE.get().size()) + 1;
                    TimeGraphState value = new TimeGraphState(startTime, duration, name, STYLE_MAP.get(String.valueOf(indexColor)));
                    applyFilterAndAddState(eventList, value, entry.getKey(), predicates, monitor);
                }
                /*
                 * If this is a communicator quark then the number of locations
                 * that need to call a MPI routine in this communicator is
                 * displayed and the opacity of the event scales with this
                 * number
                 */
                if (valObject instanceof Long) {
                    Integer numberOfNodes = ss.getSubAttributes(entry.getValue(), false).size();
                    Long numberOfPendingThreads = (Long) valObject;
                    if (numberOfPendingThreads > 0) {
                        OutputElementStyle style = new OutputElementStyle(null, ImmutableMap.of(StyleProperties.BACKGROUND_COLOR, "#FF0000", //$NON-NLS-1$
                                StyleProperties.OPACITY, Math.min(1f, 0.1f + ((float) numberOfPendingThreads) / ((float) numberOfNodes) * 0.5f)));
                        TimeGraphState value = new TimeGraphState(startTime, duration, numberOfPendingThreads.toString(), style);
                        applyFilterAndAddState(eventList, value, entry.getKey(), predicates, monitor);
                    } else {
                        TimeGraphState value = new TimeGraphState(startTime, duration, null, null);
                        applyFilterAndAddState(eventList, value, entry.getKey(), predicates, monitor);
                    }
                }
            }
            rows.add(new TimeGraphRowModel(entry.getKey(), eventList));
        }
        return new TimeGraphModel(rows);
    }

    @Override
    protected boolean isCacheable() {
        return false;
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

    /**
     * @return the full dataprovider ID
     */
    public static String getFullDataProviderId() {
        return Otf2CommunicatorsAnalysis.getFullAnalysisId() + Otf2CommunicatorsDataProvider.SUFFIX;
    }
}
