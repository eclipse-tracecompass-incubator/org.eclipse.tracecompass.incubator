/**********************************************************************
 * Copyright (c) 2021 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.ui.views.summarytimeline;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfFilteredXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfXYChartSettings;

/**
 * SummaryTimeline viewer with XY line chart. It displays the proportion of
 * location in specific APIs calls depending on which APIs and function types
 * are selected in the tree viewer
 *
 * @author Yoann Heitz
 *
 */
public class Otf2SummaryTimelineXYViewer extends TmfFilteredXYChartViewer {

    /**
     * Constructor
     *
     * @param parent
     *            parent composite
     * @param settings
     *            settings for the chart viewer
     * @param id
     *            id of the dataprovider
     */
    public Otf2SummaryTimelineXYViewer(Composite parent, TmfXYChartSettings settings, String id) {
        super(parent, settings, id);
        getSwtChart().getTitle().setVisible(true);
        getSwtChart().getLegend().setVisible(false);
    }

}
