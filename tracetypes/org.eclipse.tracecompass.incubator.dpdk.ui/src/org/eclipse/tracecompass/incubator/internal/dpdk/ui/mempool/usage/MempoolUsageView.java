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
package org.eclipse.tracecompass.incubator.internal.dpdk.ui.mempool.usage;

import java.util.Comparator;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.incubator.internal.dpdk.core.mempool.analysis.DpdkMempoolUsageDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractSelectTreeViewer2;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeColumnDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfGenericTreeEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeColumnData;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfXYChartSettings;
import org.eclipse.tracecompass.tmf.ui.views.xychart.TmfChartView;

import com.google.common.collect.ImmutableList;

/**
 * Showing Mempools usage over time
 *
 * @author Adel Belkhiri
 */
public class MempoolUsageView extends TmfChartView {

    /**
     * Identifier of this view
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.internal.dpdk.ui.mempool.usage.view"; //$NON-NLS-1$
    private static final double RESOLUTION = 0.2;

    /**
     * Default constructor
     */
    public MempoolUsageView() {
        super(Messages.MempoolThreadUsageView_Title);
    }

    @Override
    protected TmfXYChartViewer createChartViewer(Composite parent) {
        TmfXYChartSettings settings = new TmfXYChartSettings(Messages.MempoolThreadUsageView_Title, Messages.MempoolThreadUsageViewer_XAxis, Messages.MempoolThreadUsageViewer_YAxis, RESOLUTION);
        return new MempoolUsageViewer(parent, settings, DpdkMempoolUsageDataProvider.ID);
    }

    @Override
    protected @NonNull TmfViewer createLeftChildViewer(@Nullable Composite parent) {
        return new AbstractSelectTreeViewer2(parent, 1, DpdkMempoolUsageDataProvider.ID) {
            @Override
            protected ITmfTreeColumnDataProvider getColumnDataProvider() {
                return () -> ImmutableList.of(createColumn(Messages.MempoolThreadUsageTreeViewer_MempoolName, Comparator.comparing(TmfGenericTreeEntry::getName)),
                        new TmfTreeColumnData(Messages.MempoolThreadUsageTreeViewer_Legend));
            }
        };
    }
}
