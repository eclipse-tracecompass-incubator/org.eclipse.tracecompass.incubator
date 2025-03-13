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
import java.util.Collection;
import java.util.Collections;
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
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.collect.ImmutableList;

/**
 * This data provider will return a XY model, showing the number of objects
 * retrieved from a mempool over time.
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
        protected final long fId;
        protected final int fMetricQuark;
        protected final String fName;
        protected final double[] fValues;
        protected long fPrevObjectCount;
        protected static final double SECONDS_PER_NANOSECOND = 1E-9;

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
         * Update the allocation/freeing rates of mempool objects at a specified
         * index.
         *
         * @param pos
         *            The index to update
         * @param objCount
         *            Number of objects allocated or freed by a thread
         * @param deltaT
         *            Period between two observations
         */
        public void updateValue(int pos, long objCount, long deltaT) {
            long deltaCount = objCount - fPrevObjectCount;
            if (deltaCount > 0) {
                fValues[pos] = deltaCount / (SECONDS_PER_NANOSECOND * deltaT);
            } else {
                fValues[pos] = 0;
            }

            fPrevObjectCount = objCount;
        }

        public void setPrevObjectCount(long prevObjCount) {
            fPrevObjectCount = prevObjCount;
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
     * Create an instance of {@link DpdkMempoolAllocFreeRateDataProvider}.
     * Returns a null instance if the analysis module is not found.
     *
     * @param trace
     *            A trace on which we are interested to fetch a model
     * @return a {@link DpdkMempoolAllocFreeRateDataProvider} instance. If
     *         analysis module is not found, it returns null
     */
    public static @Nullable DpdkMempoolAllocFreeRateDataProvider create(ITmfTrace trace) {
        DpdkMempoolAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, DpdkMempoolAnalysisModule.class, DpdkMempoolAnalysisModule.ID);
        if (module != null) {
            module.schedule();
            return new DpdkMempoolAllocFreeRateDataProvider(trace, module);
        }
        return null;
    }

    /**
     * Constructor
     */
    private DpdkMempoolAllocFreeRateDataProvider(ITmfTrace trace, DpdkMempoolAnalysisModule module) {
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

        long[] xValues = filter.getTimesRequested();
        List<MempoolBuilder> builders = initBuilders(ss, filter);
        if (builders.isEmpty()) {
            return Collections.emptyList();
        }

        long currentEnd = ss.getCurrentEndTime();
        long prevTime = filter.getStart();

        if (prevTime >= ss.getStartTime() && prevTime <= currentEnd) {
            List<ITmfStateInterval> states = ss.queryFullState(prevTime);

            for (MempoolBuilder builder : builders) {
                long nbObjCount = extractCount(states, builder.fMetricQuark);
                builder.setPrevObjectCount(nbObjCount);
            }
        }

        for (int i = 1; i < xValues.length; i++) {
            if (monitor != null && monitor.isCanceled()) {
                return null;
            }
            long time = xValues[i];
            if (time > currentEnd) {
                break;
            } else if (time >= ss.getStartTime()) {
                List<ITmfStateInterval> states = ss.queryFullState(time);

                for (MempoolBuilder builder : builders) {
                    long nbObjAlloc = extractCount(states, builder.fMetricQuark);
                    builder.updateValue(i, nbObjAlloc, time - prevTime);
                }
            }
            prevTime = time;
        }

        return ImmutableList.copyOf(
                builders.stream()
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

    /**
     * Extract the count of used mempool objects
     *
     * @param states
     *            A list of state intervals
     * @param mempoolQuark
     *            The mempool quark
     * @return The number of used mempool objects
     */
    private static long extractCount(List<ITmfStateInterval> states, int metricQuark) {
        try {
            Object stateValue = states.get(metricQuark).getValue();
            return stateValue instanceof Number ? ((Number) stateValue).longValue() : 0L;
        } catch (IndexOutOfBoundsException e) {
            Activator.getInstance().logError(e.getMessage());
        }
        return 0L;
    }

    private List<MempoolBuilder> initBuilders(ITmfStateSystem ss, SelectionTimeQueryFilter filter) {
        int length = filter.getTimesRequested().length;
        List<MempoolBuilder> builders = new ArrayList<>();

        for (Entry<Long, Integer> entry : getSelectedEntries(filter).entrySet()) {
            int quark = Objects.requireNonNull(entry.getValue());
            try {
                if (DpdkMempoolAttributes.THREAD_OBJ_ALLOC.equals(ss.getAttributeName(quark))
                        || (DpdkMempoolAttributes.THREAD_OBJ_FREE.equals(ss.getAttributeName(quark)))) {

                    int threadQuark = ss.getParentAttributeQuark(quark);
                    String threadName = ss.getAttributeName(threadQuark);

                    int threadsQuark = ss.getParentAttributeQuark(threadQuark);
                    int mempoolQuark = ss.getParentAttributeQuark(threadsQuark);
                    String mempoolName = ss.getAttributeName(mempoolQuark);

                    String name = getTrace().getName() + '/' + mempoolName + '/' + threadName + '/' + ss.getAttributeName(quark);
                    builders.add(new MempoolBuilder(entry.getKey(), quark, name, length));
                }
            } catch (IndexOutOfBoundsException e) {
                Activator.getInstance().logError(e.getMessage());
            }
        }
        return builders;
    }
}
