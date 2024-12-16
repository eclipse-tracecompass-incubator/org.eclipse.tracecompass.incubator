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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.Activator;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.inputoutput.IODataPalette;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.model.xy.AbstractTreeCommonXDataProvider;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Abstract base class for DPDK Ethernet throughput per queue data providers
 *
 * @author Adel Belkhiri
 */
public abstract class AbstractDpdkEthdevThroughputDataProvider extends AbstractTreeCommonXDataProvider<DpdkEthdevThroughputAnalysisModule, TmfTreeDataModel>
        implements IOutputStyleProvider {

    private static final String BASE_STYLE = "base"; //$NON-NLS-1$
    private static final Map<String, OutputElementStyle> STATE_MAP;
    private static final List<Pair<String, String>> COLOR_LIST = IODataPalette.getColors();
    private static final List<String> SUPPORTED_STYLES = ImmutableList.of(
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

    /** Ports label */
    protected static final String PORTS_LABEL = Objects.requireNonNull(Messages.DpdkEthdev_ThroughputDataProvider_PORTS);
    /** Traffic reception label */
    protected static final String RX_LABEL = Objects.requireNonNull(Messages.DpdkEthdev_ThroughputDataProvider_TRAFFIC_RX);
    /** Traffic transmission label */
    protected static final String TX_LABEL = Objects.requireNonNull(Messages.DpdkEthdev_ThroughputDataProvider_TRAFFIC_TX);

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
        nodes.add(new TmfTreeDataModel(rootId, -1, Collections.singletonList(Objects.requireNonNull(getTrace().getName())), false, null));

        try {
            int portsQuark = ss.getQuarkAbsolute(DpdkEthdevThroughputAttributes.PORTS);
            long portsId = getId(portsQuark);
            nodes.add(new TmfTreeDataModel(portsId, rootId, Collections.singletonList(PORTS_LABEL), false, null));

            for (Integer portQuark : ss.getQuarks(DpdkEthdevThroughputAttributes.PORTS, "*")) { //$NON-NLS-1$
                String portName = ss.getAttributeName(portQuark);
                long portId = getId(portQuark);
                nodes.add(new TmfTreeDataModel(portId, portsId, Collections.singletonList(portName), false, null));

                int rxQsQuark = ss.optQuarkRelative(portQuark, DpdkEthdevThroughputAttributes.RX_Q);
                int txQsQuark = ss.optQuarkRelative(portQuark, DpdkEthdevThroughputAttributes.TX_Q);

                nodes.addAll(getQueuesTree(ss, rxQsQuark, portName, portId));
                nodes.addAll(getQueuesTree(ss, txQsQuark, portName, portId));
            }
        } catch (AttributeNotFoundException e) {
            Activator.getInstance().logError("Error getting the root attribute of " + DpdkEthdevThroughputAttributes.PORTS); //$NON-NLS-1$
        }
        return new TmfTreeModel<>(Collections.emptyList(), nodes);
    }

    /**
     * Get the XY subtrees related to the RX and TX queues
     *
     * @param ss
     *            The state system
     * @param qsQuark
     *            Quark of the queues list
     * @param portName
     *            Name of the port to which the queues are attached
     * @param portId
     *            The ID of the selected port
     * @return a list of {@link TmfTreeDataModel}
     */
    protected List<TmfTreeDataModel> getQueuesTree(ITmfStateSystem ss, int qsQuark, String portName, long portId) {
        int i = 0;
        List<TmfTreeDataModel> nodes = new ArrayList<>();
        boolean isRxQueue = true;

        try {
            if (DpdkEthdevThroughputAttributes.TX_Q.equals(ss.getAttributeName(qsQuark))) {
                isRxQueue = false;
            }

            long qsId = getId(qsQuark);
            nodes.add(new TmfTreeDataModel(qsId, portId, Collections.singletonList(isRxQueue ? RX_LABEL : TX_LABEL), false, null));

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
                                StyleProperties.STYLE_NAME, portName + '/' + (isRxQueue ? RX_LABEL : TX_LABEL) + '/' + queueName))));

                i++;
            }
        } catch (IndexOutOfBoundsException e) {
            Activator.getInstance().logWarning(e.getMessage());
        }
        return nodes;
    }

    /**
     * Create instances of builders
     *
     * @param ss
     *            State System
     * @param filter
     *            Filter
     * @return a list of builders
     */
    protected abstract List<? extends AbstractPortQueueBuilder> initBuilders(ITmfStateSystem ss, SelectionTimeQueryFilter filter);

}
