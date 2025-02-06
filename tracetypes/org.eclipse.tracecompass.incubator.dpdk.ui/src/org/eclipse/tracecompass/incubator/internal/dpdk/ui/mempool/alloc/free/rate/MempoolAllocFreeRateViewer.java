/*******************************************************************************
 * Copyright (c) 2025 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.dpdk.ui.mempool.alloc.free.rate;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.StyleProperties;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfFilteredXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfXYChartSettings;

/**
 * Viewer for the {@link MempoolAllocFreeRateView} view
 *
 * @author Adel Belkhiri
 */
public class MempoolAllocFreeRateViewer extends TmfFilteredXYChartViewer {

    private static final int DEFAULT_SERIES_WIDTH = 2;

    /**
     * Constructor
     *
     * @param parent
     *            Parent composite
     * @param settings
     *            Chart settings
     * @param providerId
     *            Data provider ID
     */
    public MempoolAllocFreeRateViewer(Composite parent, TmfXYChartSettings settings, String providerId) {
        super(parent, settings, providerId);
    }

    @Override
    public @NonNull OutputElementStyle getSeriesStyle(@NonNull Long seriesId) {
        return getPresentationProvider().getSeriesStyle(seriesId, StyleProperties.SeriesType.LINE, DEFAULT_SERIES_WIDTH);
    }

}
