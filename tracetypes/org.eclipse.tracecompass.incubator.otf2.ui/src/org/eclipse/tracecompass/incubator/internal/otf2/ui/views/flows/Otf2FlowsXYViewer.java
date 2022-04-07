/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.otf2.ui.views.flows;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfFilteredXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfXYChartSettings;

/**
 * Flows viewer with XY line chart. It displays the perceived flows for blocking
 * communications. It is not the real flows since it represents the amount of
 * data transfered through an MPI call divided by the time spent in the call.
 *
 * @author Yoann Heitz
 */
public class Otf2FlowsXYViewer extends TmfFilteredXYChartViewer {

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
    public Otf2FlowsXYViewer(Composite parent, TmfXYChartSettings settings, String id) {
        super(parent, settings, id);
        getSwtChart().getTitle().setVisible(true);
        getSwtChart().getLegend().setVisible(false);
    }
}
