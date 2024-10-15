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
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.Activator;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataType;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.xy.IYModel;
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.collect.ImmutableList;

/**
 * The {@link DpdkEthdevThroughputBpsDataProvider} data provider will return a
 * XY model showing the Ethernet throughput per port queue, in bits per second
 * (bps).
 *
 * @author Adel Belkhiri
 */
public class DpdkEthdevThroughputBpsDataProvider extends AbstractDpdkEthdevThroughputDataProvider {

    /** The ID of this data provider */
    public static final String ID = "org.eclipse.tracecompass.incubator.dpdk.ethdev.throughput.bps.dataprovider"; //$NON-NLS-1$
    private static final String PROVIDER_TITLE = "Dpdk Ethernet Device Throughput - BPS"; //$NON-NLS-1$
    private static final String BINARY_SPEED_UNIT = "b/s"; //$NON-NLS-1$
    private static final TmfXYAxisDescription Y_AXIS_DESCRIPTION = new TmfXYAxisDescription(Objects.requireNonNull(Messages.DpdkEthdev_ThroughputBpsDataProvider_YAxis), BINARY_SPEED_UNIT, DataType.NUMBER);

    /**
     * Class for generating data series representing traffic reception and
     * transmission throughput
     */
    private class PortQueueBuilder extends AbstractPortQueueBuilder {
        private static final double BITS_PER_BYTE = 8.0;

        protected PortQueueBuilder(long id, int queueQuark, String name, int length) {
            super(id, queueQuark, name, length);
        }

        @Override
        protected void updateValue(int pos, double newCount, long deltaT) {
            /**
             * Linear interpolation between current and previous times to
             * compute packets throughput in bits per second
             */
            fValues[pos] = (newCount - fPrevCount) * BITS_PER_BYTE / (SECONDS_PER_NANOSECOND * deltaT);
            fPrevCount = newCount;
        }
    }

    /**
     * Create an instance of {@link DpdkEthdevThroughputBpsDataProvider}.
     * Returns a null instance if the analysis module is not found.
     *
     * @param trace
     *            A trace on which we are interested to fetch a model
     * @return A {@link DpdkEthdevThroughputBpsDataProvider} instance. If
     *         analysis module is not found, then returns null
     */
    public static @Nullable DpdkEthdevThroughputBpsDataProvider create(ITmfTrace trace) {
        DpdkEthdevThroughputAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace, DpdkEthdevThroughputAnalysisModule.class, DpdkEthdevThroughputAnalysisModule.ID);
        return module != null ? new DpdkEthdevThroughputBpsDataProvider(trace, module) : null;
    }

    /**
     * Constructor
     */
    private DpdkEthdevThroughputBpsDataProvider(ITmfTrace trace, DpdkEthdevThroughputAnalysisModule module) {
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
        List<PortQueueBuilder> builders = initBuilders(ss, filter);
        if (builders.isEmpty()) {
            return Collections.emptyList();
        }

        long currentEnd = ss.getCurrentEndTime();
        long prevTime = filter.getStart();

        if (prevTime >= ss.getStartTime() && prevTime <= currentEnd) {
            List<ITmfStateInterval> states = ss.queryFullState(prevTime);

            for (PortQueueBuilder builder : builders) {
                builder.setPrevCount(extractCount(ss, states, builder.fQueueQuark));
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

                for (PortQueueBuilder builder : builders) {
                    double count = extractCount(ss, states, builder.fQueueQuark);
                    builder.updateValue(i, count, time - prevTime);
                }
            }
            prevTime = time;
        }

        return ImmutableList.copyOf(
                builders.stream()
                        .map(builder -> builder.build(Y_AXIS_DESCRIPTION))
                        .collect(Collectors.toList()));
    }

    /**
     * Extract packet burst size
     *
     * @param ss
     *            State System
     * @param states
     *            ITmfStateInterval values
     * @param queueQuark
     *            Port queue quark
     * @return The number of bytes received or sent from a queue
     */
    private static double extractCount(ITmfStateSystem ss, List<ITmfStateInterval> states, int queueQuark) {
        try {
            int metricQuark = ss.optQuarkRelative(queueQuark, DpdkEthdevThroughputAttributes.PKT_SIZE_P);
            if (metricQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                Object stateValue = states.get(metricQuark).getValue();
                return stateValue instanceof Number ? ((Number) stateValue).doubleValue() : 0.0;
            }
        } catch (IndexOutOfBoundsException e) {
            Activator.getInstance().logError(e.getMessage());
        }
        return Double.NaN;
    }

    @Override
    protected List<PortQueueBuilder> initBuilders(ITmfStateSystem ss, SelectionTimeQueryFilter filter) {
        int length = filter.getTimesRequested().length;
        List<PortQueueBuilder> builders = new ArrayList<>();

        for (Entry<Long, Integer> entry : getSelectedEntries(filter).entrySet()) {
            long id = Objects.requireNonNull(entry.getKey());
            int quark = Objects.requireNonNull(entry.getValue());

            if (quark == ITmfStateSystem.ROOT_ATTRIBUTE || ss.getParentAttributeQuark(quark) == ITmfStateSystem.ROOT_ATTRIBUTE) {
                continue;
            }

            int parentQuark = ss.getParentAttributeQuark(quark);
            if (parentQuark != ITmfStateSystem.INVALID_ATTRIBUTE &&
                    (DpdkEthdevThroughputAttributes.RX_Q.equals(ss.getAttributeName(parentQuark)) ||
                            DpdkEthdevThroughputAttributes.TX_Q.equals(ss.getAttributeName(parentQuark)))) {
                int portQuark = ss.getParentAttributeQuark(parentQuark);
                String name = getTrace().getName() + '/' + ss.getAttributeName(portQuark) + '/' + ss.getAttributeName(parentQuark) + '/' + ss.getAttributeName(quark);
                builders.add(new PortQueueBuilder(id, quark, name, length));
            }
        }
        return builders;
    }
}
