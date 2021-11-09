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

import java.util.Comparator;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractSelectTreeViewer2;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeColumnDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeColumnData;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeViewerEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfXYChartSettings;
import org.eclipse.tracecompass.tmf.ui.views.xychart.TmfChartView;

import com.google.common.collect.ImmutableList;

/**
 * View for OTF2 communicators
 *
 * @author Yoann Heitz
 */

public class Otf2SummaryTimelineView extends TmfChartView {

    /** View ID suffix */
    public static final String DATAPROVIDER_ID = "org.eclipse.tracecompass.incubator.otf2.core.analysis.summarytimeline.dataprovider"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public Otf2SummaryTimelineView() {
        super("Summary Timeline"); //$NON-NLS-1$
    }

    @Override
    protected TmfXYChartViewer createChartViewer(Composite parent) {
        TmfXYChartSettings settings = new TmfXYChartSettings("Summary Timeline", "Time", "Percentage of locations in this type of function", 1);  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
        return new Otf2SummaryTimelineXYViewer(parent, settings, DATAPROVIDER_ID);
    }

    private static final class TreeXyViewer extends AbstractSelectTreeViewer2 {

        public TreeXyViewer(Composite parent) {
            super(parent, 1, DATAPROVIDER_ID);
        }

        @Override
        protected ITmfTreeColumnDataProvider getColumnDataProvider() {
            return () -> ImmutableList.of(createColumn("Function type", Comparator.comparing(TmfTreeViewerEntry::getName)), //$NON-NLS-1$
                    new TmfTreeColumnData("Legend")); //$NON-NLS-1$
        }
    }

    @Override
    protected @NonNull TmfViewer createLeftChildViewer(@Nullable Composite parent) {
        return new TreeXyViewer(Objects.requireNonNull(parent));
    }
}