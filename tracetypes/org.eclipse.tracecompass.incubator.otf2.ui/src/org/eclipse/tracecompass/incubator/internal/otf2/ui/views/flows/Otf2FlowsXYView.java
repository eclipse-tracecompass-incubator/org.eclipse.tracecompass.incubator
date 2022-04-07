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

import java.util.Comparator;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.incubator.internal.otf2.core.analysis.flows.Otf2FlowsXYDataProvider;
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
 * XY View for OTF2 flows
 *
 * @author Yoann Heitz
 */
public class Otf2FlowsXYView extends TmfChartView {

    /** View ID suffix */
    public static final String ID_SUFFIX = "flows.xy"; //$NON-NLS-1$

    /** Name of this view **/
    private static final String VIEW_NAME = "Flows (XY view)"; //$NON-NLS-1$

    /** Chart settings for this view **/
    private static final TmfXYChartSettings SETTINGS = new TmfXYChartSettings("Perceived data flows", "Time", "Perceived data flows", 1); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$

    /**
     * Constructor
     */
    public Otf2FlowsXYView() {
        super(VIEW_NAME);
    }

    @Override
    protected TmfXYChartViewer createChartViewer(Composite parent) {
        return new Otf2FlowsXYViewer(parent, SETTINGS, Otf2FlowsXYDataProvider.getFullDataProviderId());
    }

    private static final class TreeXyViewer extends AbstractSelectTreeViewer2 {

        public TreeXyViewer(Composite parent) {
            super(parent, 1, Otf2FlowsXYDataProvider.getFullDataProviderId());
        }

        @Override
        protected ITmfTreeColumnDataProvider getColumnDataProvider() {
            return () -> ImmutableList.of(createColumn("Nodes", Comparator.comparing(TmfTreeViewerEntry::getName)), //$NON-NLS-1$
                    new TmfTreeColumnData("Legend")); //$NON-NLS-1$
        }
    }

    @Override
    protected @NonNull TmfViewer createLeftChildViewer(@Nullable Composite parent) {
        return new TreeXyViewer(Objects.requireNonNull(parent));
    }
}
