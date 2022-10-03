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

package org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.summarytimeline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.inputoutput.IODataPalette;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.model.YModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.model.xy.AbstractTreeCommonXDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.IYModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Otf2SummaryTimelineDataProvider factory
 *
 * @author Yoann Heitz
 *
 */
@SuppressWarnings("restriction")
public class Otf2SummaryTimelineDataProvider extends AbstractTreeCommonXDataProvider<Otf2SummaryTimelineAnalysis, TmfTreeDataModel> implements IOutputStyleProvider {

    private static final String TOTAL = "total"; //$NON-NLS-1$
    private static final String AREA_STYLE = "area"; //$NON-NLS-1$
    private static final String LINE_STYLE = "line"; //$NON-NLS-1$
    private static final Map<String, OutputElementStyle> STATE_MAP;
    private static final List<Pair<String, String>> COLOR_LIST = IODataPalette.getColors();
    private static final TmfXYAxisDescription Y_AXIS_DESCRIPTION = new TmfXYAxisDescription("Percentage of locations in this type of function", "%", DataType.NUMBER); //$NON-NLS-1$ //$NON-NLS-2$
    private static final List<String> SUPPORTED_STYLES = ImmutableList.of(
            StyleProperties.SeriesStyle.SOLID,
            StyleProperties.SeriesStyle.DASH,
            StyleProperties.SeriesStyle.DOT,
            StyleProperties.SeriesStyle.DASHDOT,
            StyleProperties.SeriesStyle.DASHDOTDOT);

    static {
        ImmutableMap.Builder<@NonNull String, @NonNull OutputElementStyle> builder = new ImmutableMap.Builder<>();
        builder.put(AREA_STYLE, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.SERIES_TYPE, StyleProperties.SeriesType.AREA, StyleProperties.WIDTH, 1.0f)));
        builder.put(LINE_STYLE, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.SERIES_TYPE, StyleProperties.SeriesType.LINE, StyleProperties.WIDTH, 1.0f)));
        STATE_MAP = builder.build();
    }

    /** Data provider suffix ID */
    public static final String SUFFIX = ".dataprovider"; //$NON-NLS-1$

    private static final AtomicLong sfAtomicId = new AtomicLong();

    private final BiMap<Long, Integer> fIDToDisplayQuark = HashBiMap.create();

    /**
     * Constructor
     *
     * @param trace
     *            the trace for this provider
     * @param analysisModule
     *            the corresponding analysis module
     */
    public Otf2SummaryTimelineDataProvider(ITmfTrace trace, Otf2SummaryTimelineAnalysis analysisModule) {
        super(trace, analysisModule);
    }

    /**
     * Create the time graph data provider
     *
     * @param trace
     *            The trace for which is the data provider
     * @return The data provider
     */
    public static @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> create(ITmfTrace trace) {
        Otf2SummaryTimelineAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, Otf2SummaryTimelineAnalysis.class, Otf2SummaryTimelineAnalysis.getFullAnalysisId());
        return module != null ? new Otf2SummaryTimelineDataProvider(trace, module) : null;
    }

    @Override
    public String getId() {
        return getAnalysisModule().getId() + SUFFIX;
    }

    @Override
    protected boolean isCacheable() {
        return false;
    }

    @Override
    protected TmfTreeModel<TmfTreeDataModel> getTree(ITmfStateSystem ss, Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        // Make an entry for each quark : the State System should have a depth
        // of maximum 2
        List<TmfTreeDataModel> entryList = new ArrayList<>();
        int styleIndex = 0;
        for (Integer quark : ss.getQuarks("*")) { //$NON-NLS-1$
            Long id = fIDToDisplayQuark.inverse().computeIfAbsent(quark, q -> sfAtomicId.getAndIncrement());
            entryList.add(new TmfTreeDataModel(id, -1, ss.getAttributeName(quark)));
            if (!ss.getSubAttributes(quark, false).isEmpty()) {
                for (Integer subQuark : ss.getSubAttributes(quark, false)) {
                    Long subQuarkId = fIDToDisplayQuark.inverse().computeIfAbsent(subQuark, q -> sfAtomicId.getAndIncrement());

                    // Computing the style for this entry
                    String seriesStyle = SUPPORTED_STYLES.get((styleIndex / COLOR_LIST.size()) % SUPPORTED_STYLES.size());
                    Pair<String, String> pair = COLOR_LIST.get(styleIndex % COLOR_LIST.size());
                    String styleType;
                    if (ss.getAttributeName(subQuark).equals(TOTAL)) {
                        styleType = LINE_STYLE;
                    } else {
                        styleType = AREA_STYLE;
                    }

                    entryList.add(new TmfTreeDataModel(subQuarkId, id, Collections.singletonList(ss.getAttributeName(subQuark)), true, new OutputElementStyle(styleType, ImmutableMap.of(
                            StyleProperties.COLOR, pair.getFirst(),
                            StyleProperties.SERIES_STYLE, seriesStyle,
                            StyleProperties.STYLE_NAME, ss.getAttributeName(subQuark)))));
                    styleIndex++;
                }
            }
        }
        return new TmfTreeModel<>(Collections.emptyList(), entryList);
    }

    private static List<Long> getTimes(ITmfStateSystem key, @Nullable List<Long> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        List<@NonNull Long> times = new ArrayList<>();
        for (long t : list) {
            if (key.getStartTime() <= t && t <= key.getCurrentEndTime()) {
                times.add(t);
            }
        }
        Collections.sort(times);
        return times;
    }

    @Override
    protected @Nullable Collection<IYModel> getYSeriesModels(ITmfStateSystem ss, Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        ImmutableList.Builder<IYModel> ySeries = ImmutableList.builder();
        Map<Integer, double[]> quarkToValues = new HashMap<>();
        // Prepare the quarks to display
        Collection<Long> selectedItems = DataProviderParameterUtils.extractSelectedItems(fetchParameters);
        if (selectedItems == null) {
            // No selected items, take them all
            selectedItems = fIDToDisplayQuark.keySet();
        }
        List<Long> times = getTimes(ss, DataProviderParameterUtils.extractTimeRequested(fetchParameters));
        for (Long id : selectedItems) {
            Integer quark = fIDToDisplayQuark.get(id);
            if (quark != null) {
                quarkToValues.put(quark, new double[times.size()]);
            }
        }

        // Query the state system to fill the arrays of values
        try {
            for (ITmfStateInterval interval : ss.query2D(quarkToValues.keySet(), times)) {
                if (monitor != null && monitor.isCanceled()) {
                    return null;
                }
                double[] row = quarkToValues.get(interval.getAttribute());
                Object value = interval.getValue();
                if (row != null && (value instanceof Number)) {
                    Double dblValue = ((Number) value).doubleValue();
                    for (int i = 0; i < times.size(); i++) {
                        Long time = times.get(i);
                        if (interval.getStartTime() <= time && interval.getEndTime() >= time) {
                            // conversion to percentages
                            row[i] = 100 * dblValue;
                        }
                    }
                }
            }
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            e.printStackTrace();
            return null;
        }

        for (Entry<Integer, double[]> values : quarkToValues.entrySet()) {
            if (ss.getSubAttributes(values.getKey(), false).isEmpty()) {
                ySeries.add(new YModel(fIDToDisplayQuark.inverse().getOrDefault(values.getKey(), -1L), ss.getAttributeName(values.getKey()), values.getValue(), Y_AXIS_DESCRIPTION));
            }
        }
        return ySeries.build();
    }

    @Override
    protected String getTitle() {
        return "Summary Timeline"; //$NON-NLS-1$
    }

    @Override
    public TmfModelResponse<OutputStyleModel> fetchStyle(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(new OutputStyleModel(STATE_MAP), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

}