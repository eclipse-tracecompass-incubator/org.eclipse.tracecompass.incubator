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

package org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.rate.analysis;

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
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

/**
 * This data provider will return a XY model (model is wrapped in a response)
 * based on a query filter. The model returned is related to the DPDK Ethernet throughput views
 *
 * @author Adel Belkhiri
 */
public class DpdkEtherRateDataProvider extends AbstractTreeCommonXDataProvider<DpdkEthdevRateAnalysis, TmfTreeDataModel>
        implements IOutputStyleProvider {

    /**
     * Extension point ID.
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.dpdk.ethdev.rate.dataprovider"; //$NON-NLS-1$

    /**
     * Title used to create XY models for the {@link DpdkEtherRateDataProvider}.
     */
    protected static final String PROVIDER_TITLE = Objects.requireNonNull("Dpdk Ethernet Device Throughput"); //$NON-NLS-1$

    private static final String BASE_STYLE = "base"; //$NON-NLS-1$
    private static final Map<String, OutputElementStyle> STATE_MAP;
    private static final String BINARY_SPEED_UNIT = "/s"; //$NON-NLS-1$
    private static final String NICS_LABEL = Objects.requireNonNull(Messages.DpdkEthdev_RateDataProvider_NICs);
    private static final String RX_QS_LABEL = Objects.requireNonNull(Messages.DpdkEthdev_RateDataProvider_NIC_RX);
    private static final String TX_QS_LABEL = Objects.requireNonNull(Messages.DpdkEthdev_RateDataProvider_NIC_TX);
    private static final List<Pair<String, String>> COLOR_LIST = IODataPalette.getColors();
    private static final TmfXYAxisDescription Y_AXIS_DESCRIPTION = new TmfXYAxisDescription(Objects.requireNonNull(Messages.DpdkEthdev_RateDataProvider_YAxis), BINARY_SPEED_UNIT, DataType.NUMBER);
    private static final List<String> SUPPORTED_STYLES = ImmutableList.of(
            StyleProperties.SeriesStyle.SOLID,
            StyleProperties.SeriesStyle.DASH,
            StyleProperties.SeriesStyle.DOT,
            StyleProperties.SeriesStyle.DASHDOT,
            StyleProperties.SeriesStyle.DASHDOTDOT);

    static {
        // Create the base style
        ImmutableMap.Builder<String, OutputElementStyle> builder = new ImmutableMap.Builder<>();
        builder.put(BASE_STYLE, new OutputElementStyle(null, ImmutableMap.of(StyleProperties.SERIES_TYPE, StyleProperties.SeriesType.AREA, StyleProperties.WIDTH, 1.0f)));
        STATE_MAP = builder.build();
    }

    /**
     * Class for encapsulating all the values required to build a series.
     */
    private static final class NicQueueBuilder {

        private static final double SECONDS_PER_NANOSECOND = 1E-9;
        private final long fId;
        /** This series' represent the number of packets received or transmitted by a NIC port queue */
        public final int fQueueQuark;
        private final String fName;
        private final double[] fValues;
        private double fPrevCount;

        /**
         * Constructor
         *
         * @param id
         *            the series Id
         * @param nicQueueQuark
         *            queue's quark
         * @param name
         *            name of this series
         * @param length
         *            desired length of the series
         */
        private NicQueueBuilder(long id, int nicQueueQuark, String name, int length) {
            fId = id;
            fQueueQuark = nicQueueQuark;
            fName = name;
            fValues = new double[length];
        }

        private void setPrevCount(double prevCount) {
            fPrevCount = prevCount;
        }

        /**
         * Update the value for the counter at the desired index.
         *
         * @param pos
         *            index to update
         * @param newCount
         *            new number of read / written sectors
         * @param deltaT
         *            time difference to the previous value for interpolation
         */
        private void updateValue(int pos, double newCount, long deltaT) {
            /**
             * Linear interpolation to compute the queue throughput between time
             * and the previous time, from the number of packets received or sent at each time.
             */
            fValues[pos] = (newCount - fPrevCount) / (SECONDS_PER_NANOSECOND * deltaT);
            fPrevCount = newCount;
        }

        private IYModel build() {
            return new YModel(fId, fName, fValues, Y_AXIS_DESCRIPTION);
        }
    }

    /**
     * Create an instance of {@link DpdkEtherRateDataProvider}. Returns a null
     * instance if the analysis module is not found.
     *
     * @param trace
     *            A trace on which we are interested to fetch a model
     * @return A {@link DpdkEtherRateDataProvider} instance. If analysis module is not
     *         found, it returns null
     */
    public static @Nullable DpdkEtherRateDataProvider create(ITmfTrace trace) {
        DpdkEthdevRateAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, DpdkEthdevRateAnalysis.class, DpdkEthdevRateAnalysis.ID);
        if (module != null) {
            module.schedule();
            return new DpdkEtherRateDataProvider(trace, module);
        }
        return null;
    }

    /**
     * Constructor
     */
    private DpdkEtherRateDataProvider(ITmfTrace trace, DpdkEthdevRateAnalysis module) {
        super(trace, module);
    }

    @Override
    public String getId() {
        return ID;
    }

    /**
     * Get parts of the XY tree that are related to the RX and TX queues
     *
     * @param ss
     *            State System
     * @param qsQuark
     *            Queues list quark
     * @param nicName
     *            Name of the NIC to which the queues belong
     * @param nicId
     *            Id of the related NIC
     * @return a list of {@link TmfTreeDataModel}
     */
    List<TmfTreeDataModel> getQueuesTree(ITmfStateSystem ss, int qsQuark, String nicName, long nicId) {
        int i = 0;
        List<TmfTreeDataModel> nodes = new ArrayList<>();
        boolean isRxQueue = true;

        try {
            if(ss.getAttributeName(qsQuark).equals(Attributes.TX_Q)) {
                isRxQueue = false;
            }

            long qsId = getId(qsQuark);
            nodes.add(new TmfTreeDataModel(qsId, nicId, Collections.singletonList(isRxQueue? RX_QS_LABEL: TX_QS_LABEL), false, null));
            for (Integer queueQuark : ss.getSubAttributes(qsQuark, false)) {
                String queueName = ss.getAttributeName(queueQuark);
                long queueId = getId(queueQuark);

                // get colors for this queue corresponding to their types (reception or transmission)
                Pair<String, String> pair = COLOR_LIST.get(i % COLOR_LIST.size());
                String seriesStyle = SUPPORTED_STYLES.get((i / COLOR_LIST.size()) % SUPPORTED_STYLES.size());

                if (queueQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                    nodes.add(new TmfTreeDataModel(queueId, qsId, Collections.singletonList(queueName), true,
                            new OutputElementStyle(BASE_STYLE, ImmutableMap.of(
                                    StyleProperties.COLOR, isRxQueue? pair.getFirst() : pair.getSecond(),
                                    StyleProperties.SERIES_STYLE, seriesStyle,
                                    StyleProperties.STYLE_NAME,
                                    nicName + '/' + RX_QS_LABEL + '/' + queueName))));
                }

                i++;
            }
        } catch (IndexOutOfBoundsException e) {
            Activator.getInstance().logWarning("A DPDK event (" + DpdkEthdevEventLayout.eventEthdevConfigure() + ") is missing"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return nodes;
    }

    @Override
    protected TmfTreeModel<TmfTreeDataModel> getTree(ITmfStateSystem ss, Map<String, Object> parameters, @Nullable IProgressMonitor monitor) {
        List<TmfTreeDataModel> nodes = new ArrayList<>();
        long rootId = getId(ITmfStateSystem.ROOT_ATTRIBUTE);
        nodes.add(new TmfTreeDataModel(rootId, -1, Collections.singletonList(Objects.requireNonNull(getTrace().getName())), false, null));

        try {
            int nicsQuark = ss.getQuarkAbsolute(Attributes.NICS);
            long nicsId = getId(nicsQuark);
            nodes.add(new TmfTreeDataModel(nicsId, rootId, Collections.singletonList(NICS_LABEL), false, null));

            for (Integer nicQuark : ss.getQuarks(Attributes.NICS, "*")) { //$NON-NLS-1$
                String nicName = ss.getAttributeName(nicQuark);
                long nicId = getId(nicQuark);
                nodes.add(new TmfTreeDataModel(nicId, nicsId, Collections.singletonList(nicName), false, null));

                int rxQsQuark = ss.optQuarkRelative(nicQuark, Attributes.RX_Q);
                int txQsQuark = ss.optQuarkRelative(nicQuark, Attributes.TX_Q);
                if (rxQsQuark == ITmfStateSystem.INVALID_ATTRIBUTE && txQsQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                    continue;
                }

                nodes.addAll(getQueuesTree(ss, rxQsQuark, nicName, nicId));
                nodes.addAll(getQueuesTree(ss, txQsQuark, nicName, nicId));

            }
        } catch (AttributeNotFoundException e) {
            Activator.getInstance().logError("Error getting the root attribute of " + Attributes.NICS); //$NON-NLS-1$
        }
        return new TmfTreeModel<>(Collections.emptyList(), nodes);
    }

    /**
     * Extract a packets counts
     *
     * @param queueQuark
     *          quark of the NIC queue
     * @param states
     *          states
     * @return
     *          number of received or transmitted packets
     */
    public static double extractCount(int queueQuark, List<ITmfStateInterval> states) {
        Object stateValue = states.get(queueQuark).getValue();
        double count = (stateValue instanceof Number) ? ((Number) stateValue).doubleValue() : 0.0;
        return count;
    }

    @Override
    protected @Nullable Collection<IYModel> getYSeriesModels(ITmfStateSystem ss, Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
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
            // reuse the results from the full query
            List<ITmfStateInterval> states = ss.queryFullState(prevTime);

            for (NicQueueBuilder entry : builders) {
                entry.setPrevCount(extractCount(entry.fQueueQuark, /*ss,*/ states/*, prevTime*/));
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

                for (NicQueueBuilder entry : builders) {
                    double count = extractCount(entry.fQueueQuark, states);
                    entry.updateValue(i, count, time - prevTime);
                }
            }
            prevTime = time;
        }
        return ImmutableList.copyOf(Iterables.transform(builders, NicQueueBuilder::build));
    }


    private List<NicQueueBuilder> initBuilders(ITmfStateSystem ss, SelectionTimeQueryFilter filter) {
        int length = filter.getTimesRequested().length;
        List<NicQueueBuilder> builders = new ArrayList<>();

        for (Entry<Long, Integer> entry : getSelectedEntries(filter).entrySet()) {
            long id = Objects.requireNonNull(entry.getKey());
            int quark = Objects.requireNonNull(entry.getValue());

            if((quark == ITmfStateSystem.ROOT_ATTRIBUTE) ||
                    (ss.getParentAttributeQuark(quark) == ITmfStateSystem.ROOT_ATTRIBUTE)) {
                continue;
            }

            int parentQuark = ss.getParentAttributeQuark(quark);
            if(parentQuark != ITmfStateSystem.INVALID_ATTRIBUTE &&
                    ((ss.getAttributeName(parentQuark).equals(Attributes.RX_Q) ||
                            ss.getAttributeName(parentQuark).equals(Attributes.TX_Q)))) {
                int nicQuark = ss.getParentAttributeQuark(parentQuark);
                String name = getTrace().getName() + '/' + ss.getAttributeName(nicQuark)  + '/' + ss.getAttributeName(parentQuark)  + '/' + ss.getAttributeName(quark);
                builders.add(new NicQueueBuilder(id, quark, name, length));
            }

        }
        return builders;
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
