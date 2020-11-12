/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.scripting.ui.views.xychart;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfFilteredXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfXYChartSettings;
import org.eclipse.tracecompass.tmf.ui.views.xychart.TmfChartView;

/**
 * A data provider view to display the results of a scripted analysis. It uses
 * the secondary ID as the data provider ID to display
 *
 * @author Benjamin Saint-Cyr
 */
public class ScriptedXYView extends TmfChartView {

    /**
     * Because colons are not allowed in secondary IDs, but can be present in
     * data provider IDs, they can be replaced upstream by this string and it
     * will be replaced again when getting the data provider ID.
     */
    public static final String COLON = "[COLON]"; //$NON-NLS-1$

    /** View ID */
    public static final String ID = "org.eclipse.tracecompass.incubator.internal.scripting.ui.views.xychart"; //$NON-NLS-1$

    /**
     * Default constructor
     */
    public ScriptedXYView() {
        super(Messages.ScriptedXYTreeViewer_DefaultTitle);
    }

    @Override
    protected TmfXYChartViewer createChartViewer(Composite parent) {
        TmfXYChartSettings settings = new TmfXYChartSettings(Messages.ScriptedXYTreeViewer_DefaultViewerTitle,
                Messages.ScriptedXYTreeViewer_DefaultXAxis, Messages.ScriptedXYTreeViewer_DefaultYAxis, 1);
        return new TmfFilteredXYChartViewer(parent, settings, getSecondaryIdName());
    }

    @Override
    protected @NonNull TmfViewer createLeftChildViewer(@Nullable Composite parent) {
        return new ScriptedXYTreeViewer(Objects.requireNonNull(parent), getSecondaryIdName());
    }

    private String getSecondaryIdName() {
        return getViewSite().getSecondaryId().replace(ScriptedXYView.COLON, ":"); //$NON-NLS-1$
    }
}
