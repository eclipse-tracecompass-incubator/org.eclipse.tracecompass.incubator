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
package org.eclipse.tracecompass.incubator.internal.dpdk.ui.ethdev.throughput.bps;

import java.util.Comparator;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.ethdev.throughput.analysis.DpdkEthdevThroughputBpsDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractSelectTreeViewer2;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeColumnDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfGenericTreeEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeColumnData;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfFilteredXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfXYChartSettings;
import org.eclipse.tracecompass.tmf.ui.views.xychart.TmfChartView;

import com.google.common.collect.ImmutableList;

/**
 * This view shows packet reception and transmission throughput per port queue
 *
 * @author Adel Belkhiri
 */
public class NicQueueThroughputBpsView extends TmfChartView {

    /**
     * Identifier of this view
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.dpdk.ethdev.throughput.bps.view"; //$NON-NLS-1$
    private static final double RESOLUTION = 0.2;

    /**
     * Default constructor
     */
    public NicQueueThroughputBpsView() {
        super(Messages.EthdevThroughputBpsView_Title);
    }

    @Override
    protected TmfXYChartViewer createChartViewer(Composite parent) {
        TmfXYChartSettings settings = new TmfXYChartSettings(Messages.EthdevThroughputBpsViewer_Title, Messages.EthdevThroughputBpsViewer_XAxis, null, RESOLUTION);
        return new TmfFilteredXYChartViewer(parent, settings, DpdkEthdevThroughputBpsDataProvider.ID);
    }

    @Override
    protected @NonNull TmfViewer createLeftChildViewer(@Nullable Composite parent) {
        return new AbstractSelectTreeViewer2(parent, 1, DpdkEthdevThroughputBpsDataProvider.ID) {
            @Override
            protected ITmfTreeColumnDataProvider getColumnDataProvider() {
                return () -> ImmutableList.of(createColumn(Messages.EthdevThroughputBpsTreeViewer_PortName, Comparator.comparing(TmfGenericTreeEntry::getName)),
                        new TmfTreeColumnData(Messages.EthdevThroughputBpsTreeViewer_Legend));
            }
        };
    }
}
