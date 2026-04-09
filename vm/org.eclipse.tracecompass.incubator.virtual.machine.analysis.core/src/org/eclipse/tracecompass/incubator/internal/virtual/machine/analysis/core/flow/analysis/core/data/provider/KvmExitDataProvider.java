/*******************************************************************************
 * Copyright (c) 2026 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.core.data.provider;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.KvmExitAnalysisModule;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.YModel;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.model.xy.IYModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXyTreeDataModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.tracecompass.tmf.core.model.SeriesModel.SeriesModelBuilder;
import org.eclipse.tracecompass.tmf.core.model.TmfXyModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;

/**
 * A data provider for KVM Exit events histogram.
 * This provider gives a visual representation of KVM exits evolution
 * for each CPU over time.
 *
 * Based on the pattern of HistogramDataProvider to improve visualization.
 *
 * @author Francois Belias
 */
@NonNullByDefault
@SuppressWarnings("restriction")
public class KvmExitDataProvider extends AbstractTmfTraceDataProvider implements ITmfTreeXYDataProvider<TmfTreeDataModel> {

    /**
     * Provider unique ID.
     */
    public static final String ID = "org.eclipse.incubator.overhead.xy.dataprovider"; //$NON-NLS-1$
    private static final String TITLE = "KVM Exits Density"; //$NON-NLS-1$
    private static final AtomicLong TRACE_IDS = new AtomicLong();

    private final KvmExitAnalysisModule fmodule;
    private @Nullable TmfModelResponse<TmfTreeModel<TmfTreeDataModel>> fCached = null;
    private final long fTraceId = TRACE_IDS.getAndIncrement();

    /**
     * Constructor
     *
     * @param trace
     *            The trace this data provider is for
     * @param analysisModule
     *            The analysis module
     */
    public KvmExitDataProvider(ITmfTrace trace, KvmExitAnalysisModule analysisModule) {
        super(trace);
        fmodule = analysisModule;
    }

    @Override
    public String getId() {
        return ID;
    }

    @SuppressWarnings("null")
    @Override
    public TmfModelResponse<TmfTreeModel<TmfTreeDataModel>> fetchTree(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        if (this.fCached != null) {
            return this.fCached;
        }

        fmodule.waitForInitialization();
        ITmfStateSystem ss = fmodule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        Builder<TmfTreeDataModel> builder = ImmutableList.builder();

        // Root: the trace itself
        builder.add(new TmfTreeDataModel(fTraceId, -1, Collections.singletonList(getTrace().getName())));

        Map<Integer, Long> cpuMap = buildCpuIdToSeriesId(ss);
        for (Map.Entry<Integer, Long> entry : cpuMap.entrySet()) {
            int cpuId = entry.getKey();
            long seriesId = entry.getValue();

            if (cpuId == -1) {
                builder.add(new TmfXyTreeDataModel(seriesId, fTraceId,
                        Collections.singletonList("All CPUs"), true, null, true)); //$NON-NLS-1$
            } else {
                builder.add(new TmfXyTreeDataModel(seriesId, fTraceId,
                        Collections.singletonList("CPU " + cpuId), true, null, true)); //$NON-NLS-1$
            }
        }

        if (ss.waitUntilBuilt(0)) {
            TmfModelResponse<TmfTreeModel<TmfTreeDataModel>> response = new TmfModelResponse<>(
                    new TmfTreeModel<>(Collections.emptyList(), builder.build()),
                    ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
            fCached = response;
            return response;
        }

        return new TmfModelResponse<>(
                new TmfTreeModel<>(Collections.emptyList(), builder.build()),
                ITmfResponse.Status.RUNNING, CommonStatusMessage.RUNNING);
    }

    @Override
    public TmfModelResponse<ITmfXyModel> fetchXY(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        fmodule.waitForInitialization();
        ITmfStateSystem ss = fmodule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(fetchParameters);
        long[] xValues = new long[0];
        if (filter == null) {
            return createXyResponse(TITLE, xValues, Collections.emptyList(), true);
        }
        xValues = filter.getTimesRequested();

        Collection<Long> selected = filter.getSelectedItems();
        if (selected.isEmpty()) {
            return createXyResponse(TITLE, xValues, Collections.emptyList(), true);
        }

        Map<Integer, Long> localCpuIdToSeriesId = buildCpuIdToSeriesId(ss);

        int numPoints = xValues.length;
        ImmutableList.Builder<IYModel> builder = ImmutableList.builder();

        try {
            for (Map.Entry<Integer, Long> entry : localCpuIdToSeriesId.entrySet()) {
                int cpuId = entry.getKey();
                long seriesId = entry.getValue();

                if (!selected.contains(seriesId)) {
                    continue;
                }

                double[] values = new double[numPoints];
                Arrays.fill(values, 0.0);

                if (cpuId == -1) {
                    aggregateAllCpuData(ss, xValues, values);
                } else {
                    fillCpuExitData(ss, cpuId, xValues, values);
                }

                String name = cpuId == -1
                        ? getTrace().getName() + "/All CPUs"  //$NON-NLS-1$
                        : getTrace().getName() + "/CPU " + cpuId;  //$NON-NLS-1$

                builder.add(new YModel(seriesId, name, values));
            }
        } catch (StateSystemDisposedException e) {
            return createFailedXyResponse(CommonStatusMessage.STATE_SYSTEM_FAILED);
        }

        boolean completed = ss.waitUntilBuilt(0) || ss.getCurrentEndTime() >= filter.getEnd();
        return createXyResponse(TITLE, xValues, builder.build(), completed);
    }

    private static TmfModelResponse<ITmfXyModel> createXyResponse(String title, long[] xValues,
            Collection<IYModel> yModels, boolean isComplete) {

        List<ISeriesModel> series = new ArrayList<>(yModels.size());
        for (IYModel model : yModels) {
            SeriesModelBuilder builder = new SeriesModelBuilder(model.getId(), model.getName(), xValues, model.getData());
            TmfXYAxisDescription yAxis = model.getYAxisDescription();
            if (yAxis != null) {
                builder.yAxisDescription(yAxis);
            }
            series.add(builder.build());
        }

        ITmfXyModel xyModel = new TmfXyModel(title, series);
        if (isComplete) {
            return new TmfModelResponse<>(xyModel, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }
        return new TmfModelResponse<>(xyModel, ITmfResponse.Status.RUNNING, CommonStatusMessage.RUNNING);
    }

    private static TmfModelResponse<ITmfXyModel> createFailedXyResponse(String message) {
        return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, message);
    }

    private Map<Integer, Long> buildCpuIdToSeriesId(ITmfStateSystem ss) {
        Map<Integer, Long> map = new HashMap<>();

        for (Integer cpuQuark : ss.getQuarks("CPUs", "*")) { //$NON-NLS-1$ //$NON-NLS-2$
            String cpuName = ss.getAttributeName(cpuQuark);
            int cpuId;
            try {
                cpuId = Integer.parseInt(cpuName);
            } catch (NumberFormatException e) {
                continue;
            }

            int exitCountQuark = ss.optQuarkRelative(cpuQuark, "kvm_exits"); //$NON-NLS-1$
            if (exitCountQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                map.put(cpuId, fTraceId * 1000 + cpuId + 2);  // même formule que fetchTree()
            }
        }

        // All CPUs
        map.put(-1, fTraceId * 1000 + 1);

        return map;
    }

    /**
     * Fill the values array with KVM exit counts for a specific CPU
     *
     * @param ss The state system
     * @param cpuId The CPU ID
     * @param times The time points
     * @param values The array to fill with values
     * @throws StateSystemDisposedException If the state system is disposed
     */
    private static void fillCpuExitData(ITmfStateSystem ss, int cpuId, long[] times, double[] values)
            throws StateSystemDisposedException {

        int cpuQuark = ss.optQuarkAbsolute("CPUs", String.valueOf(cpuId)); //$NON-NLS-1$
        if (cpuQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return;
        }

        int exitQuark = ss.optQuarkRelative(cpuQuark, "kvm_exits"); //$NON-NLS-1$
        if (exitQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return;
        }

        for (int i = 0; i < times.length - 1; i++) {
            long bucketStart = times[i];
            long bucketEnd   = times[i + 1];

            long startCount = queryCumulativeCount(ss, exitQuark, bucketStart);
            long endCount   = queryCumulativeCount(ss, exitQuark, bucketEnd);

            long delta    = endCount - startCount;           // exits in this interval
            long duration = bucketEnd - bucketStart;         // in nanoseconds

            if (duration > 0 && delta >= 0) {
                // exits per seconds
                values[i] = delta / (duration / 1_000_000_000.0);
            }
        }


        if (values.length > 1) {
            values[values.length - 1] = values[values.length - 2];
        }
    }

    private static long queryCumulativeCount(ITmfStateSystem ss, int quark, long timestamp) throws StateSystemDisposedException {
        long clampedTs = Math.max(ss.getStartTime(), Math.min(ss.getCurrentEndTime(), timestamp));
        ITmfStateInterval interval = ss.querySingleState(clampedTs, quark);
        Object value = interval.getValue();
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    /**
     * Aggregate KVM exit data from all CPUs
     *
     * @param ss The state system
     * @param times The time points
     * @param values The array to fill with aggregated values
     * @throws StateSystemDisposedException If the state system is disposed
     */
    private static void aggregateAllCpuData(ITmfStateSystem ss, long[] times, double[] values) throws StateSystemDisposedException {
        // Get all CPUs with KVM exit data
        for (Integer cpuQuark : ss.getQuarks("CPUs", "*")) { //$NON-NLS-1$ //$NON-NLS-2$
            String cpuName = ss.getAttributeName(cpuQuark);
            int cpuId;
            try {
                cpuId = Integer.parseInt(cpuName);

                // Create a temporary array for this CPU's data
                double[] cpuValues = new double[values.length];
                Arrays.fill(cpuValues, 0.0);

                // Get this CPU's data
                fillCpuExitData(ss, cpuId, times, cpuValues);

                // Add to the aggregate values
                for (int i = 0; i < values.length; i++) {
                    values[i] += cpuValues[i];
                }

            } catch (NumberFormatException e) {
                continue; // Skip entries that aren't CPU numbers
            }
        }
    }
}