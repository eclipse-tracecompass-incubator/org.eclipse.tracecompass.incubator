/*******************************************************************************
 * Copyright (c) 2020 VMware, Inc.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.scripting.ui.views.histogram;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.tracecompass.incubator.internal.scripting.ui.views.xychart.ScriptedXYTreeViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfXYChartSettings;
import org.eclipse.tracecompass.tmf.ui.views.xychart.TmfChartView;

/**
 * A data provider view to display the results of a scripted analysis. It uses
 * the secondary ID as the data provider ID to display
 *
 * @author Qing Chi
 */
public class ScriptedHistogramView extends TmfChartView {

    /**
     * Because colons are not allowed in secondary IDs, but can be present in
     * data provider IDs, they can be replaced upstream by this string and it
     * will be replaced again when getting the data provider ID.
     */
    public static final String COLON = "[COLON]"; //$NON-NLS-1$

    /** The view ID. */
    public static final String ID = "org.eclipse.tracecompass.incubator.internal.scripting.ui.views.histogram"; //$NON-NLS-1$

    /**
     * Default Constructor
     */
    public ScriptedHistogramView() {
        super(ID);
    }

    @Override
    protected TmfXYChartViewer createChartViewer(Composite parent) {
        TmfXYChartSettings settings = new TmfXYChartSettings(Messages.ScriptedHistogramTreeViewer_DefaultViewerTitle, Messages.ScriptedHistogramTreeViewer_DefaultXAxis, Messages.ScriptedHistogramTreeViewer_DefaultYAxis, 1);
        return new ScriptedHistogramViewer(parent, settings, getSecondaryIdName());
    }

    @Override
    protected @NonNull TmfViewer createLeftChildViewer(@Nullable Composite parent) {
        return new ScriptedXYTreeViewer(Objects.requireNonNull(parent), getSecondaryIdName());
    }

    private String getSecondaryIdName() {
        return getViewSite().getSecondaryId().replace(ScriptedHistogramView.COLON, ":"); //$NON-NLS-1$
    }
}
