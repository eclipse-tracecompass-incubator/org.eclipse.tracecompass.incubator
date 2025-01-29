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

package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.spin.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.Activator;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.model.YModel;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.model.xy.AbstractTreeCommonXDataProvider;
import org.eclipse.tracecompass.tmf.core.model.xy.IYModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * This data provider will return a XY model, showing the percentage of
 * occupancy of a PMD thread. The model also shows how much time the thread
 * spends processing packets versus being idle over a period.
 *
 * @author Adel Belkhiri
 */
public class DpdkEthdevSpinDataProvider extends AbstractTreeCommonXDataProvider<DpdkEthdevSpinAnalysisModule, TmfTreeDataModel>
        implements IOutputStyleProvider {

    /**
     * Extension point ID.
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.spin.dataprovider"; //$NON-NLS-1$

    /**
     * Title used to create XY models for the
     * {@link DpdkEthdevSpinDataProvider}.
     */
    private static final String PROVIDER_TITLE = Objects.requireNonNull("DPDK Threads Effective CPU Usage"); //$NON-NLS-1$

    private static final String BASE_STYLE = "base"; //$NON-NLS-1$

    private static final String THREADS_LABEL = Objects.requireNonNull(Messages.DpdkEthdevSpin_DataProvider_Threads);

    private static final Map<String, OutputElementStyle> STATE_MAP;

    static {
        // Create the base style
        ImmutableMap.Builder<String, OutputElementStyle> builder = new ImmutableMap.Builder<>();
        builder.put(BASE_STYLE, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.SERIES_TYPE, StyleProperties.SeriesType.SCATTER, StyleProperties.WIDTH, 1.0f)));
        STATE_MAP = builder.build();
    }

    /**
     * Create an instance of {@link DpdkEthdevSpinDataProvider}. Returns a null
     * instance if the analysis module is not found.
     *
     * @param trace
     *            A trace on which we are interested to fetch a model
     * @return a {@link DpdkEthdevSpinDataProvider} instance. If analysis module
     *         is not found, it returns null
     */
    public static @Nullable DpdkEthdevSpinDataProvider create(ITmfTrace trace) {
        DpdkEthdevSpinAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, DpdkEthdevSpinAnalysisModule.class, DpdkEthdevSpinAnalysisModule.ID);
        if (module != null) {
            module.schedule();
            return new DpdkEthdevSpinDataProvider(trace, module);
        }
        return null;
    }

    /**
     * Constructor
     */
    private DpdkEthdevSpinDataProvider(ITmfTrace trace, DpdkEthdevSpinAnalysisModule module) {
        super(trace, module);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected TmfTreeModel<TmfTreeDataModel> getTree(ITmfStateSystem ss, Map<String, Object> parameters, @Nullable IProgressMonitor monitor) {
        List<TmfTreeDataModel> nodes = new ArrayList<>();
        long rootId = getId(ITmfStateSystem.ROOT_ATTRIBUTE);
        nodes.add(new TmfTreeDataModel(rootId, -1, Collections.singletonList(Objects.requireNonNull(getTrace().getName())), false, null));

        try {
            int threadsQuark = ss.getQuarkAbsolute(DpdkEthdevSpinAttributes.POLL_THREADS);
            long threadsId = getId(threadsQuark);
            nodes.add(new TmfTreeDataModel(threadsId, rootId, Collections.singletonList(THREADS_LABEL), false, null));

            for (Integer threadQuark : ss.getQuarks(DpdkEthdevSpinAttributes.POLL_THREADS, "*")) { //$NON-NLS-1$
                String threadName = ss.getAttributeName(threadQuark);
                long threadId = getId(threadQuark);
                nodes.add(new TmfTreeDataModel(threadId, threadsId, Collections.singletonList(threadName), false, null));
            }
        } catch (AttributeNotFoundException e) {
            Activator.getInstance().logError("Error getting the root attribute of " + DpdkEthdevSpinAttributes.POLL_THREADS); //$NON-NLS-1$
        }
        return new TmfTreeModel<>(Collections.emptyList(), nodes);
    }

    /**
     * Subtract from start time the same interval as the interval from start
     * time to next time, ignoring duplicates in the times requested.
     */
    private static long getInitialPrevTime(SelectionTimeQueryFilter filter) {
        long startTime = filter.getStart();
        for (long time : filter.getTimesRequested()) {
            if (time > startTime) {
                return startTime - (time - startTime);
            }
        }
        return startTime;
    }

    @Override
    protected @Nullable Collection<IYModel> getYSeriesModels(ITmfStateSystem ss, Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(fetchParameters);
        if (filter == null || getSelectedEntries(filter).isEmpty()) {
            return null;
        }

        Set<Integer> threadQuarks = new HashSet<>();
        Map<String, IYModel> threadYModels = new HashMap<>();

        // Fetch the quarks of PMD threads for analysis
        for (Entry<Long, Integer> entry : getSelectedEntries(filter).entrySet()) {
            int quark = Objects.requireNonNull(entry.getValue());
            if ((quark == ITmfStateSystem.ROOT_ATTRIBUTE) ||
                    (ss.getParentAttributeQuark(quark) == ITmfStateSystem.ROOT_ATTRIBUTE)) {
                continue;
            }
            threadQuarks.add(quark);
            String name = ss.getAttributeName(quark);
            threadYModels.put(name, new YModel(entry.getKey(), getTrace().getName() + '/' + name, new double[filter.getTimesRequested().length]));
        }

        calculateThreadStatePercentages(ss, filter, monitor, threadQuarks, threadYModels);
        return ImmutableList.copyOf(threadYModels.values());
    }

    /**
     * Updates the thread YSeries models with the percentage of time each thread spent in active and spinning states
     *
     * @param ss
     *      State System
     * @param filter
     *      Query Filter
     * @param monitor
     *      Monitor
     * @param threadQuarks
     *      Set of quarks representing the threads to be analyzed
     * @param threadYModels
     *      A map of thread names to their corresponding Y-axis data models
     */
    private void calculateThreadStatePercentages(ITmfStateSystem ss, SelectionTimeQueryFilter filter, @Nullable IProgressMonitor monitor, Set<Integer> threadQuarks, Map<String, IYModel> threadYModels) {
        long[] xValues = filter.getTimesRequested();
        long prevTime = Math.max(getInitialPrevTime(filter), ss.getStartTime());
        long currentEnd = ss.getCurrentEndTime();

        for (int i = 0; i < xValues.length; i++) {
            long currentTime = xValues[i];
            if (currentTime < ss.getStartTime() || currentTime > currentEnd) {
                prevTime = currentTime;
                continue;
            }

            if (prevTime < currentTime) {
                Map<String, Pair<Long, Long>> threadUsageMap = getAnalysisModule().calculateThreadStateDurations(
                        threadQuarks, prevTime, currentTime);

                final int index = i;
                threadUsageMap.forEach((key, durations) -> {
                    IYModel values = threadYModels.get(key);
                    if (values != null) {
                        values.getData()[index] = getPercentageValue(durations.getFirst(), durations.getSecond());
                    }
                });
            } else if (i > 0) {
                for (IYModel values : threadYModels.values()) {
                    values.getData()[i] = values.getData()[i - 1];
                }
            }

            prevTime = currentTime;

            if (monitor != null && monitor.isCanceled()) {
                return;
            }
        }
    }

    private static double getPercentageValue(long countActive, long countSpin) {
        return (countActive + countSpin) == 0 ? 0.0 : (double) countActive * 100 / (countActive + countSpin);
    }

    @Override
    protected boolean isCacheable() {
        return true;
    }

    @Override
    protected String getTitle() {
        return PROVIDER_TITLE;
    }

    @Override
    public TmfModelResponse<OutputStyleModel> fetchStyle(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(new OutputStyleModel(STATE_MAP), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }
}
