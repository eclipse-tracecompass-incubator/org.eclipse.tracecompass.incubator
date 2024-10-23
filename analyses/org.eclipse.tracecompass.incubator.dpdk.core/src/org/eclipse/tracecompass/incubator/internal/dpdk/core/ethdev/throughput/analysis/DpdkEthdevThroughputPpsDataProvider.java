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
package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.throughput.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.Activator;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.xy.IYModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * This data provider returns an XY model representing the throughput of
 * transmitted or received network traffic in packets per second (pps). This
 * model can be used to generate time-series charts that visualize the network's
 * bandwidth usage over time.
 *
 * @author Adel Belkhiri
 */
public class DpdkEthdevThroughputPpsDataProvider extends AbstractDpdkEthdevThroughputDataProvider {

    public static final String ID = "org.eclipse.tracecompass.incubator.dpdk.ethdev.throughput.pps.dataprovider"; //$NON-NLS-1$
    protected static final String PROVIDER_TITLE = "Dpdk Ethernet Device Throughput - PPS"; //$NON-NLS-1$
    private static final String BINARY_SPEED_UNIT = "/s"; //$NON-NLS-1$
    private static final TmfXYAxisDescription Y_AXIS_DESCRIPTION = new TmfXYAxisDescription(
            Objects.requireNonNull(Messages.DpdkEthdev_ThroughputPpsDataProvider_YAxis), BINARY_SPEED_UNIT, DataType.NUMBER);

    /**
     * Class for generating data series representing packets RX/TX rates
     */
    protected class NicQueueBuilder extends AbstractNicQueueBuilder {
        private final boolean fIsProfileMetric;

        /**
         * Constructor
         *
         * @param id
         *          The unique identifier for this data series
         * @param nicQueueQuark
         *          The quark representing the target NIC queue in the state system
         * @param isProfileMetric
         *          {@code true} if the values are generated from profiling events; {@code false} otherwise.
         * @param name
         *          The name of this data series
         * @param length
         *          The number of data points in this series
         */
        protected NicQueueBuilder(long id, int nicQueueQuark, boolean isProfileMetric, String name, int length) {
            super(id, nicQueueQuark, name, length);
            fIsProfileMetric = isProfileMetric;
        }

        @Override
        protected void updateValue(int pos, double newCount, long deltaT) {
            fValues[pos] = (newCount - fPrevCount) / (SECONDS_PER_NANOSECOND * deltaT);
            fPrevCount = newCount;
        }
    }

    /**
     * Create an instance of {@link DpdkEthdevThroughputPpsDataProvider}.
     * Returns a null instance if the analysis module is not found.
     *
     * @param trace
     *            A trace on which we are interested to fetch a model
     * @return A {@link DpdkEthdevThroughputPpsDataProvider} instance. If
     *         analysis module is not found, it returns null
     */
    public static @Nullable DpdkEthdevThroughputPpsDataProvider create(ITmfTrace trace) {
        DpdkEthdevThroughputAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace,
                DpdkEthdevThroughputAnalysisModule.class, DpdkEthdevThroughputAnalysisModule.ID);
        return module != null ? new DpdkEthdevThroughputPpsDataProvider(trace, module) : null;
    }

    private DpdkEthdevThroughputPpsDataProvider(ITmfTrace trace, DpdkEthdevThroughputAnalysisModule module) {
        super(trace, module);
    }

    @Override
    protected String getTitle() {
        return PROVIDER_TITLE;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected @Nullable Collection<IYModel> getYSeriesModels(ITmfStateSystem ss, Map<String, Object> fetchParameters,
            @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(fetchParameters);
        if (filter == null) {
            return null;
        }

        long[] xValues = filter.getTimesRequested();
        List<NicQueueBuilder> builders = initBuilders(ss, filter);
        if (builders.isEmpty()) {
            return Collections.emptyList();
        }

        long currentEnd = ss.getCurrentEndTime();
        long prevTime = filter.getStart();

        if (prevTime >= ss.getStartTime() && prevTime <= currentEnd) {
            List<ITmfStateInterval> states = ss.queryFullState(prevTime);

            for (NicQueueBuilder builder : builders) {
                builder.setPrevCount(extractCount(ss, states, builder.fQueueQuark, builder.fIsProfileMetric));
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

                for (NicQueueBuilder builder : builders) {
                    double count = extractCount(ss, states, builder.fQueueQuark, builder.fIsProfileMetric);
                    builder.updateValue(i, count, time - prevTime);
                }
            }
            prevTime = time;
        }

        return ImmutableList.copyOf(Iterables.transform(builders, builder -> builder.build(Y_AXIS_DESCRIPTION)));
    }

    /**
     * Extract a packets counts
     *
     * @param ss
     *            State system
     * @param queueQuark
     *            NIC queue quark
     * @param states
     *            ITmfStateInterval state values
     * @param isProfileMetric
     *            Whether the metric attribute was generated via the profiling
     *            library or not
     * @return number of packets received or sent from the RX/TX queue
     */
    private static double extractCount(ITmfStateSystem ss, List<ITmfStateInterval> states, int queueQuark, boolean isProfileMetric) {
        try {
            int metricQuark = isProfileMetric
                    ? ss.getQuarkRelative(queueQuark, DpdkEthdevThroughputAttributes.PKT_COUNT_P)
                    : ss.getQuarkRelative(queueQuark, DpdkEthdevThroughputAttributes.PKT_COUNT);

            Object stateValue = states.get(metricQuark).getValue();
            return stateValue instanceof Number ? ((Number) stateValue).doubleValue() : 0.0;
        } catch (AttributeNotFoundException | IndexOutOfBoundsException e) {
            Activator.getInstance().logError("Error accessing packets count on state system", e); //$NON-NLS-1$
            return 0.0;
        }
    }

    @Override
    protected List<NicQueueBuilder> initBuilders(ITmfStateSystem ss, SelectionTimeQueryFilter filter) {
        int length = filter.getTimesRequested().length;
        List<NicQueueBuilder> builders = new ArrayList<>();

        for (Entry<Long, Integer> entry : getSelectedEntries(filter).entrySet()) {
            long id = entry.getKey();
            int quark = entry.getValue();

            if (quark == ITmfStateSystem.ROOT_ATTRIBUTE || ss.getParentAttributeQuark(quark) == ITmfStateSystem.ROOT_ATTRIBUTE) {
                continue;
            }

            int parentQuark = ss.getParentAttributeQuark(quark);
            if (parentQuark != ITmfStateSystem.INVALID_ATTRIBUTE &&
                (DpdkEthdevThroughputAttributes.RX_Q.equals(ss.getAttributeName(parentQuark)) ||
                 DpdkEthdevThroughputAttributes.TX_Q.equals(ss.getAttributeName(parentQuark)))) {
                int nicQuark = ss.getParentAttributeQuark(parentQuark);
                String name = getTrace().getName() + '/' + ss.getAttributeName(nicQuark) + '/' + ss.getAttributeName(parentQuark) + '/' + ss.getAttributeName(quark);

                boolean isProfileMetric = ss.getAttributeName(quark).equals(DpdkEthdevThroughputAttributes.PKT_COUNT_P);
                builders.add(new NicQueueBuilder(id, quark, isProfileMetric, name, length));
            }
        }
        return builders;
    }
}
