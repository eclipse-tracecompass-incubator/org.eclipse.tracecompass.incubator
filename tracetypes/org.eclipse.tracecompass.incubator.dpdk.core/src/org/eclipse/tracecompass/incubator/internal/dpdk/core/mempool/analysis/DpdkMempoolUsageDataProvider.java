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
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.ImmutableList;

/**
 * This data provider will return a XY model, showing the variation of the
 * number of used mempool objects over time.
 *
 * @author Adel Belkhiri
 */
public class DpdkMempoolUsageDataProvider extends AbstractTreeCommonXDataProvider<DpdkMempoolAnalysisModule, TmfTreeDataModel> {

    /**
     * Extension point ID.
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.internal.dpdk.core.mempool.dataprovider"; //$NON-NLS-1$

    /**
     * Title of the {@link DpdkMempoolUsageDataProvider}.
     */
    private static final String PROVIDER_TITLE = Objects.requireNonNull("DPDK Mempool Objects In Use"); //$NON-NLS-1$
    private static final TmfXYAxisDescription Y_AXIS_DESCRIPTION = new TmfXYAxisDescription(Objects.requireNonNull(Messages.DpdkMempoolUsage_DataProvider_YAxis), "", DataType.NUMBER); //$NON-NLS-1$

    private class MempoolBuilder {
        private final long fId;
        private final int fMempoolQuark;
        private final String fName;
        private final double[] fValues;

        /**
         * Constructor
         *
         * @param id
         *            The series Id
         * @param quark
         *            Mempool quark
         * @param name
         *            The name of this series
         * @param length
         *            The length of the series
         */
        public MempoolBuilder(long id, int quark, String name, int length) {
            fId = id;
            fMempoolQuark = quark;
            fName = name;
            fValues = new double[length];
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
     *
     * @param trace
     *            Source trace for the analysis
     * @param module
     *            Analysis module
     */
    public DpdkMempoolUsageDataProvider(ITmfTrace trace, DpdkMempoolAnalysisModule module) {
        super(trace, module);
    }

    @Override
    public String getId() {
        return ID;
    }

    private static String getMempoolName(ITmfStateSystem ss, Integer mempoolNameQuark) {
        ITmfStateInterval interval = StateSystemUtils.queryUntilNonNullValue(
                ss, mempoolNameQuark, ss.getStartTime(), ss.getCurrentEndTime());
        return (interval != null) ? String.valueOf(interval.getValue()) : "no_name"; //$NON-NLS-1$
    }

    @Override
    protected TmfTreeModel<TmfTreeDataModel> getTree(ITmfStateSystem ss, Map<String, Object> parameters, @Nullable IProgressMonitor monitor) {
        List<TmfTreeDataModel> nodes = new ArrayList<>();
        long rootId = getId(ITmfStateSystem.ROOT_ATTRIBUTE);
        nodes.add(new TmfTreeDataModel(rootId, -1, Collections.singletonList(Objects.requireNonNull(getTrace().getName())), false, null));

        try {
            for (int mempoolQuark : ss.getSubAttributes(ss.getQuarkAbsolute(DpdkMempoolAttributes.MEMPOOLS), false)) {
                long mempoolId = getId(mempoolQuark);
                int nameQuark = ss.getQuarkRelative(mempoolQuark, DpdkMempoolAttributes.MEMPOOL_NAME);
                String mempoolName = getMempoolName(ss, nameQuark);

                nodes.add(new TmfTreeDataModel(mempoolId, rootId, Collections.singletonList(mempoolName), false, null));
            }
        } catch (AttributeNotFoundException e) {
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

        Collection<Integer> totalMetricQuarks = new ArrayList<>();
        Map<Integer, Pair<Integer, String>> metricToDataMap = new HashMap<>();
        for (MempoolBuilder builder : builderByQuark.values()) {
            final Collection<Integer> allocQuarks = getMetricQuarks(ss, builder.fMempoolQuark, DpdkMempoolAttributes.THREAD_OBJ_ALLOC);
            allocQuarks.forEach(metricQuark -> metricToDataMap.put(metricQuark, new Pair<>(builder.fMempoolQuark, DpdkMempoolAttributes.THREAD_OBJ_ALLOC)));
            totalMetricQuarks.addAll(allocQuarks);

            final Collection<Integer> freeQuarks = getMetricQuarks(ss, builder.fMempoolQuark, DpdkMempoolAttributes.THREAD_OBJ_FREE);
            freeQuarks.forEach(metricQuark -> metricToDataMap.put(metricQuark, new Pair<>(builder.fMempoolQuark, DpdkMempoolAttributes.THREAD_OBJ_FREE)));
            totalMetricQuarks.addAll(freeQuarks);
        }

        long[] xValues = filter.getTimesRequested();
        Collection<Long> times = getTimes(filter, ss.getStartTime(), ss.getCurrentEndTime());

        for (ITmfStateInterval interval : ss.query2D(totalMetricQuarks, times)) {
            if (monitor != null && monitor.isCanceled()) {
                return null;
            }

            Object value = interval.getValue();
            long nbObjCount = value instanceof Number ? ((Number) value).longValue() : 0L;

            if (nbObjCount == 0) {
                continue;
            }

            int from = Arrays.binarySearch(xValues, interval.getStartTime());
            from = (from >= 0) ? from : -1 - from;

            int to = Arrays.binarySearch(xValues, interval.getEndTime());
            to = (to >= 0) ? to + 1 : -1 - to;

            if (from < to) {
                Pair<Integer, String> metricData = Objects.requireNonNull(metricToDataMap.get(interval.getAttribute()));
                int sign = DpdkMempoolAttributes.THREAD_OBJ_ALLOC.equals(metricData.getSecond()) ? 1 : -1;

                MempoolBuilder builder = Objects.requireNonNull(builderByQuark.get(metricData.getFirst()));

                for (int i = from; i < to; i++) {
                    builder.fValues[i] += sign * nbObjCount;
                }
            }
        }
        return ImmutableList.copyOf(
                builderByQuark.values().stream()
                        .map(builder -> builder.build(Y_AXIS_DESCRIPTION))
                        .collect(Collectors.toList()));
    }

    /**
     * Retrieves the metric quarks associated with a given mempool and metric
     * label
     *
     * @param ss
     *            State system
     * @param mempoolQuark
     *            Mempool quark
     * @param metricLabel
     *            The label used to filter metric attributes
     * @return A collection of sub-attribute quarks for the mempool quark that
     *         match the specified metric label
     */
    private static Collection<Integer> getMetricQuarks(ITmfStateSystem ss, Integer mempoolQuark, String metricLabel) {
        Collection<Integer> metricQuarks = new ArrayList<>();
        try {
            int threadsQuark = ss.optQuarkRelative(mempoolQuark, DpdkMempoolAttributes.THREADS);
            if (threadsQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                for (int quark : ss.getSubAttributes(threadsQuark, true)) {
                    if (quark != ITmfStateSystem.INVALID_ATTRIBUTE && metricLabel.equals(ss.getAttributeName(quark))) {
                        metricQuarks.add(quark);
                    }
                }
            }

        } catch (IndexOutOfBoundsException e) {
            Activator.getInstance().logError(e.getMessage());
        }
        return metricQuarks;
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
            if (quark == ITmfStateSystem.ROOT_ATTRIBUTE || ss.getParentAttributeQuark(quark) == ITmfStateSystem.ROOT_ATTRIBUTE) {
                continue;
            }

            int parentQuark = ss.getParentAttributeQuark(quark);
            if (parentQuark != ITmfStateSystem.INVALID_ATTRIBUTE &&
                    (DpdkMempoolAttributes.MEMPOOLS.equals(ss.getAttributeName(parentQuark)))) {

                String name = getTrace().getName() + '/' + ss.getAttributeName(quark);
                builderMap.put(quark, new MempoolBuilder(entry.getKey(), quark, name, length));
            }
        }
        return builderMap;
    }
}
