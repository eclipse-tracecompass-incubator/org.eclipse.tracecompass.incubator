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
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.Activator;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.analysis.DpdkEthdevEventLayout;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.inputoutput.IODataPalette;
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
import org.eclipse.tracecompass.tmf.core.model.xy.TmfXYAxisDescription;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Abstract base class for DPDK Ethernet Device Throughput Data Providers
 */
public abstract class AbstractDpdkEthdevThroughputDataProvider extends AbstractTreeCommonXDataProvider<DpdkEthdevThroughputAnalysisModule, TmfTreeDataModel>
        implements IOutputStyleProvider {

    protected static final String BASE_STYLE = "base"; //$NON-NLS-1$
    protected static final Map<String, OutputElementStyle> STATE_MAP;
    protected static final List<Pair<String, String>> COLOR_LIST = IODataPalette.getColors();
    protected static final List<String> SUPPORTED_STYLES = ImmutableList.of(
            StyleProperties.SeriesStyle.SOLID,
            StyleProperties.SeriesStyle.DASH,
            StyleProperties.SeriesStyle.DOT,
            StyleProperties.SeriesStyle.DASHDOT,
            StyleProperties.SeriesStyle.DASHDOTDOT);

    static {
        // Create the base style
        ImmutableMap.Builder<String, OutputElementStyle> builder = ImmutableMap.builder();
        builder.put(BASE_STYLE, new OutputElementStyle(null, ImmutableMap.of(
                StyleProperties.SERIES_TYPE, StyleProperties.SeriesType.AREA,
                StyleProperties.WIDTH, 1.0f)));
        STATE_MAP = builder.build();
    }

    /**  NIC queues label */
    protected static final String NICS_LABEL = Objects.requireNonNull(Messages.DpdkEthdev_ThroughputDataProvider_NICs);
    /** Packets reception label */
    protected static final String RX_LABEL = Objects.requireNonNull(Messages.DpdkEthdev_ThroughputDataProvider_NIC_RX);
    /** Packets transmission label */
    protected static final String TX_LABEL = Objects.requireNonNull(Messages.DpdkEthdev_ThroughputDataProvider_NIC_TX);

    /**
     * Constructor
     *
     * @param trace
     *            Target trace
     * @param module
     *            Analysis module
     */
    protected AbstractDpdkEthdevThroughputDataProvider(ITmfTrace trace, DpdkEthdevThroughputAnalysisModule module) {
        super(trace, module);
    }

    @Override
    protected boolean isCacheable() {
        return true;
    }

    @Override
    public TmfModelResponse<OutputStyleModel> fetchStyle(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(new OutputStyleModel(STATE_MAP), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    protected TmfTreeModel<TmfTreeDataModel> getTree(ITmfStateSystem ss, Map<String, Object> parameters, @Nullable IProgressMonitor monitor) {
        List<TmfTreeDataModel> nodes = new ArrayList<>();
        long rootId = getId(ITmfStateSystem.ROOT_ATTRIBUTE);
        nodes.add(new TmfTreeDataModel(rootId, -1, Collections.singletonList(getTrace().getName()), false, null));

        try {
            int nicsQuark = ss.getQuarkAbsolute(DpdkEthdevThroughputAttributes.NICS);
            long nicsId = getId(nicsQuark);
            nodes.add(new TmfTreeDataModel(nicsId, rootId, Collections.singletonList(NICS_LABEL), false, null));

            for (Integer nicQuark : ss.getQuarks(DpdkEthdevThroughputAttributes.NICS, "*")) { //$NON-NLS-1$
                String nicName = ss.getAttributeName(nicQuark);
                long nicId = getId(nicQuark);
                nodes.add(new TmfTreeDataModel(nicId, nicsId, Collections.singletonList(nicName), false, null));

                int rxQsQuark = ss.optQuarkRelative(nicQuark, DpdkEthdevThroughputAttributes.RX_Q);
                int txQsQuark = ss.optQuarkRelative(nicQuark, DpdkEthdevThroughputAttributes.TX_Q);

                nodes.addAll(getQueuesTree(ss, rxQsQuark, nicName, nicId));
                nodes.addAll(getQueuesTree(ss, txQsQuark, nicName, nicId));
            }
        } catch (AttributeNotFoundException e) {
            Activator.getInstance().logError("Error getting the root attribute of " + DpdkEthdevThroughputAttributes.NICS); //$NON-NLS-1$
        }
        return new TmfTreeModel<>(Collections.emptyList(), nodes);
    }

    /**
     * Get parts of the XY tree that are related to the RX and TX queues
     *
     * @param ss
     *            The state system
     * @param qsQuark
     *            The quark of the queues list
     * @param nicName
     *            The name of the NIC to which the queues are attached
     * @param nicId
     *            The id of the related port
     * @return a list of {@link TmfTreeDataModel}
     */
    protected List<TmfTreeDataModel> getQueuesTree(ITmfStateSystem ss, int qsQuark, String nicName, long nicId) {
        int i = 0;
        List<TmfTreeDataModel> nodes = new ArrayList<>();
        boolean isRxQueue = true;

        try {
            if (DpdkEthdevThroughputAttributes.TX_Q.equals(ss.getAttributeName(qsQuark))) {
                isRxQueue = false;
            }

            long qsId = getId(qsQuark);
            nodes.add(new TmfTreeDataModel(qsId, nicId, Collections.singletonList(isRxQueue ? RX_LABEL : TX_LABEL), false, null));

            for (Integer queueQuark : ss.getSubAttributes(qsQuark, false)) {
                String queueName = ss.getAttributeName(queueQuark);
                long queueId = getId(queueQuark);

                // Get color and style for the queue
                Pair<String, String> colorPair = COLOR_LIST.get(i % COLOR_LIST.size());
                String seriesStyle = SUPPORTED_STYLES.get((i / COLOR_LIST.size()) % SUPPORTED_STYLES.size());

                nodes.add(new TmfTreeDataModel(queueId, qsId, Collections.singletonList(queueName), true,
                        new OutputElementStyle(BASE_STYLE, ImmutableMap.of(
                                StyleProperties.COLOR, isRxQueue ? colorPair.getFirst() : colorPair.getSecond(),
                                StyleProperties.SERIES_STYLE, seriesStyle,
                                StyleProperties.STYLE_NAME, nicName + '/' + (isRxQueue ? RX_LABEL : TX_LABEL) + '/' + queueName))));

                i++;
            }
        } catch (IndexOutOfBoundsException e) {
            Activator.getInstance().logWarning("A DPDK event (" + DpdkEthdevEventLayout.eventEthdevConfigure() + ") is missing", e); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return nodes;
    }

    @Override
    protected abstract String getTitle();

    @Override
    public abstract String getId();

    @Override
    protected abstract @Nullable Collection<IYModel> getYSeriesModels(ITmfStateSystem ss, Map<String, Object> fetchParameters,
            @Nullable IProgressMonitor monitor) throws StateSystemDisposedException;

    protected abstract List<? extends AbstractNicQueueBuilder> initBuilders(ITmfStateSystem ss, SelectionTimeQueryFilter filter);

    /**
     * Abstract class for building data series illustrating DPDK NICs throughput.
     */
    protected abstract class AbstractNicQueueBuilder {
        protected final long fId;
        protected final int fQueueQuark;
        protected final String fName;
        protected final double[] fValues;
        protected double fPrevCount;

        protected static final double SECONDS_PER_NANOSECOND = 1E-9;

        /**
         * Constructor
         *
         * @param id
         *            The series Id
         * @param nicQueueQuark
         *            The queue's quark
         * @param name
         *            The name of this series
         * @param length
         *            The length of the series
         */
        protected AbstractNicQueueBuilder(long id, int nicQueueQuark, String name, int length) {
            fId = id;
            fQueueQuark = nicQueueQuark;
            fName = name;
            fValues = new double[length];
        }

        protected void setPrevCount(double prevCount) {
            fPrevCount = prevCount;
        }

        /**
         * Update the value for the counter at the desired index.
         *
         * @param pos
         *            The index to update
         * @param newCount
         *            The new count of bytes received or transmitted
         * @param deltaT
         *            The time difference to the previous value for
         *            interpolation
         */
        protected abstract void updateValue(int pos, double newCount, long deltaT);

        /**
         * Build a data series
         *
         * @param yAxisDescription
         *          Description for the Y axis
         * @return an IYModel
         */
        protected IYModel build(TmfXYAxisDescription yAxisDescription) {
            return new YModel(fId, fName, fValues, yAxisDescription);
        }
    }
}
