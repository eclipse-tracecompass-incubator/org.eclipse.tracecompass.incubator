/**********************************************************************
 * Copyright (c) 2025 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.dpdk.core.mempool.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.Activator;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.model.YModel;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.model.xy.AbstractTreeCommonXDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.IYModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList;

/**
 * This data provider will return a XY model showing the rate at which mempool
 * objects were allocated and deallocated.
 *
 * @author Adel Belkhiri
 */
public class DpdkMempoolAllocFreeRateDataProvider extends AbstractTreeCommonXDataProvider<DpdkMempoolAnalysisModule, TmfTreeDataModel> {

    /**
     * Extension point ID.
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.internal.dpdk.core.mempool.alloc.free.dataprovider"; //$NON-NLS-1$

    /**
     * Title of the {@link DpdkMempoolAllocFreeRateDataProvider}.
     */
    private static final String PROVIDER_TITLE = Objects.requireNonNull("DPDK Mempool Alloc/Dealloc Rate"); //$NON-NLS-1$
    private static final TmfXYAxisDescription Y_AXIS_DESCRIPTION = new TmfXYAxisDescription(Objects.requireNonNull(Messages.DpdkMempoolAllocFreeRate_DataProvider_YAxis), "/s", DataType.NUMBER); //$NON-NLS-1$

    private class MempoolBuilder {
        private final long fId;
        private final int fMetricQuark;
        private final String fName;
        private final double[] fValues;
        private static final double SECONDS_PER_NANOSECOND = 1E-9;

        private long fPrevObjectCount;
        private long fPrevTime;

        /**
         * Constructor
         *
         * @param id
         *            The series Id
         * @param metricQuark
         *            quark
         * @param name
         *            The name of this series
         * @param length
         *            The length of the series
         */
        public MempoolBuilder(long id, int metricQuark, String name, int length) {
            fId = id;
            fMetricQuark = metricQuark;
            fName = name;
            fValues = new double[length];
        }

        /**
         * Update the rate value at the specified index
         *
         * @param pos
         *            The index to update
         * @param objCount
         *            Number of mempool objects allocated or freed by a worker
         *            thread
         * @param currTime
         *            Timestamp related to the observation
         */
        public void updateValue(int pos, long objCount, long currTime) {
            if (pos < 0 || pos >= fValues.length) {
                Activator.getInstance().logError("Error updating the series' values at index " + pos); //$NON-NLS-1$
                return;
            }

            long deltaCount = objCount - fPrevObjectCount;
            long deltaTime = currTime - fPrevTime;

            if (deltaCount > 0 && deltaTime > 0) {
                double elapsedSeconds = deltaTime * SECONDS_PER_NANOSECOND;
                fValues[pos] = deltaCount / elapsedSeconds;
            } else {
                fValues[pos] = 0;
            }

            fPrevObjectCount = objCount;
            fPrevTime = currTime;
        }

        public void setPrevObservation(long prevObjCount, long timestamp) {
            fPrevObjectCount = prevObjCount;
            fPrevTime = timestamp;
        }

        /**
         * Build a data series
         *
         * @param yAxisDescription
         *            Description for the Y axis
         * @return an IYModel
         */
        public IYModel build(TmfXYAxisDescription yAxisDescription) {
            return new YModel(fId, fName, fValues, yAxisDescription);
        }

    }

    /**
     * Constructor
     */
    public DpdkMempoolAllocFreeRateDataProvider(ITmfTrace trace, DpdkMempoolAnalysisModule module) {
        super(trace, module);
    }

    @Override
    public String getId() {
        return ID;
    }

    private static String getMempoolName(ITmfStateSystem ss, Integer mempoolNameQuark) {
        ITmfStateInterval interval = StateSystemUtils.queryUntilNonNullValue(ss, mempoolNameQuark, ss.getStartTime(), ss.getCurrentEndTime());
        if (interval != null) {
            return String.valueOf(interval.getValue());
        }
        return "no_name"; //$NON-NLS-1$
    }

    @Override
    protected TmfTreeModel<TmfTreeDataModel> getTree(ITmfStateSystem ss, Map<String, Object> parameters, @Nullable IProgressMonitor monitor) {
        List<TmfTreeDataModel> nodes = new ArrayList<>();
        long rootId = getId(ITmfStateSystem.ROOT_ATTRIBUTE);
        nodes.add(new TmfTreeDataModel(rootId, -1, Collections.singletonList(Objects.requireNonNull(getTrace().getName())), false, null));

        try {
            for (int mempoolQuark : ss.getQuarks(DpdkMempoolAttributes.MEMPOOLS, "*")) { //$NON-NLS-1$
                long mempoolId = getId(mempoolQuark);

                int nameQuark = ss.getQuarkRelative(mempoolQuark, DpdkMempoolAttributes.MEMPOOL_NAME);
                String mempoolName = getMempoolName(ss, nameQuark);
                nodes.add(new TmfTreeDataModel(mempoolId, rootId, Collections.singletonList(mempoolName), false, null));

                int threadsQuark = ss.optQuarkRelative(mempoolQuark, DpdkMempoolAttributes.THREADS);
                if (threadsQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                    for (Integer threadQuark : ss.getQuarks(threadsQuark, "*")) { //$NON-NLS-1$
                        long threadId = getId(threadQuark);
                        String threadName = ss.getAttributeName(threadQuark);
                        nodes.add(new TmfTreeDataModel(threadId, mempoolId, Collections.singletonList(threadName), true, null));

                        int allocQuark = ss.optQuarkRelative(threadQuark, DpdkMempoolAttributes.THREAD_OBJ_ALLOC);
                        if (allocQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                            long allocMetricId = getId(allocQuark);
                            nodes.add(new TmfTreeDataModel(allocMetricId, threadId, Collections.singletonList(DpdkMempoolAttributes.THREAD_OBJ_ALLOC), true, null));
                        }

                        int deallocMetricQuark = ss.optQuarkRelative(threadQuark, DpdkMempoolAttributes.THREAD_OBJ_FREE);
                        if (deallocMetricQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                            long deallocMetricId = getId(deallocMetricQuark);
                            nodes.add(new TmfTreeDataModel(deallocMetricId, threadId, Collections.singletonList(DpdkMempoolAttributes.THREAD_OBJ_FREE), true, null));
                        }
                    }
                }
            }
        } catch (AttributeNotFoundException | IndexOutOfBoundsException e) {
            Activator.getInstance().logError(e.getMessage());
        }
        return new TmfTreeModel<>(Collections.emptyList(), nodes);
    }

    @Override
    protected @Nullable Collection<IYModel> getYSeriesModels(ITmfStateSystem ss, Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(fetchParameters);
        if (filter == null) {
            return null;
        }

        Map<Integer, MempoolBuilder> builderByQuark = initBuilders(ss, filter);
        if (builderByQuark.isEmpty()) {
            return Collections.emptyList();
        }

        long[] xValues = filter.getTimesRequested();

        long currentEnd = ss.getCurrentEndTime();
        long prevTime = filter.getStart();

        // Set the first observation
        if (prevTime >= ss.getStartTime() && prevTime <= currentEnd) {
            for (MempoolBuilder builder : builderByQuark.values()) {
                ITmfStateInterval interval = ss.querySingleState(prevTime, builder.fMetricQuark);

                Object value = interval.getValue();
                long nbObjCount = value instanceof Number ? ((Number) value).longValue() : 0L;

                builder.setPrevObservation(nbObjCount, prevTime);
            }
        }

        // Interpolate values based on current and previous observations
        for (ITmfStateInterval interval : ss.query2D(builderByQuark.keySet(), prevTime + 1, currentEnd)) {
            if (monitor != null && monitor.isCanceled()) {
                return null;
            }

            MempoolBuilder builder = Objects.requireNonNull(builderByQuark.get(interval.getAttribute()));

            Object value = interval.getValue();
            long nbObjCount = value instanceof Number ? ((Number) value).longValue() : 0L;

            int from = Arrays.binarySearch(xValues, interval.getStartTime());
            from = (from >= 0) ? from : -1 - from;

            int to = Arrays.binarySearch(xValues, interval.getEndTime());
            to = (to >= 0) ? to + 1 : -1 - to;

            for (int j = from; j < to; j++) {
                Long time = xValues[j];
                builder.updateValue(j, nbObjCount, time);
            }
        }

        return ImmutableList.copyOf(
                builderByQuark.values().stream()
                        .map(builder -> builder.build(Y_AXIS_DESCRIPTION))
                        .collect(Collectors.toList()));
    }

    @Override
    protected boolean isCacheable() {
        return true;
    }

    @Override
    protected String getTitle() {
        return PROVIDER_TITLE;
    }

    private Map<Integer, MempoolBuilder> initBuilders(ITmfStateSystem ss, SelectionTimeQueryFilter filter) {
        int length = filter.getTimesRequested().length;
        Map<Integer, MempoolBuilder> builderMap = new HashMap<>();

        for (Entry<Long, Integer> entry : getSelectedEntries(filter).entrySet()) {
            int quark = Objects.requireNonNull(entry.getValue());
            try {
                String metricLabel = ss.getAttributeName(quark);
                if (DpdkMempoolAttributes.THREAD_OBJ_ALLOC.equals(metricLabel)
                        || (DpdkMempoolAttributes.THREAD_OBJ_FREE.equals(metricLabel))) {

                    int threadQuark = ss.getParentAttributeQuark(quark);
                    String threadName = ss.getAttributeName(threadQuark);

                    int threadsQuark = ss.getParentAttributeQuark(threadQuark);
                    int mempoolQuark = ss.getParentAttributeQuark(threadsQuark);
                    String mempoolName = ss.getAttributeName(mempoolQuark);

                    String name = getTrace().getName() + '/' + mempoolName + '/' + threadName + '/' + metricLabel;
                    builderMap.put(quark, new MempoolBuilder(entry.getKey(), quark, name, length));
                }
            } catch (IndexOutOfBoundsException e) {
                Activator.getInstance().logError(e.getMessage());
            }
        }
        return builderMap;
    }
}
