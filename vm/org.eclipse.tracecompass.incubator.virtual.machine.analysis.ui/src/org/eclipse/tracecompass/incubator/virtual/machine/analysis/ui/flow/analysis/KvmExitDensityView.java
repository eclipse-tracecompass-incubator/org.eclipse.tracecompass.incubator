/*******************************************************************************
 * Copyright (c) 2026 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.virtual.machine.analysis.ui.flow.analysis;


import java.util.Comparator;
import java.util.Objects;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.core.data.provider.KvmExitRateDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractSelectTreeViewer2;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeColumnDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeColumnData;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeViewerEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfFilteredXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfXYChartSettings;
import org.eclipse.tracecompass.tmf.ui.views.xychart.TmfChartView;
import com.google.common.collect.ImmutableList;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfGenericTreeEntry;

/**
 * Histogram view of the distribution of KVM exits
 *
 * @author Francois Belias
 */
public class KvmExitDensityView extends TmfChartView {
    /** View ID */
    public static final String ID = "org.eclipse.tracecompass.incubator.virtual.machine.analysis.ui.flow.analysis.kvm.density.view"; //$NON-NLS-1$

    private static final String TITLE = "KVM Exit Density"; //$NON-NLS-1$
    private static final String X_AXIS_TITLE = "Time"; //$NON-NLS-1$
    private static final String Y_AXIS_TITLE = "Exits/sec"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public KvmExitDensityView() {
        super(ID);
    }

    @Override
    protected TmfXYChartViewer createChartViewer(Composite parent) {
        TmfXYChartSettings settings = new TmfXYChartSettings(TITLE, X_AXIS_TITLE, Y_AXIS_TITLE, 1);
        return new TmfFilteredXYChartViewer(parent, settings, KvmExitRateDataProvider.ID);
    }

    private static final class TreeXyViewer extends AbstractSelectTreeViewer2 {

        private final class LegendLabelProvider extends TreeLabelProvider {
            @Override
            public Image getColumnImage(Object element, int columnIndex) {
                if (columnIndex == 1 && element instanceof TmfGenericTreeEntry genericEntry && isChecked(element)) {
                    if (!genericEntry.hasChildren()) {
                        return getLegendImage(genericEntry.getModel().getId());
                    }
                }
                return null;
            }
        }

        public TreeXyViewer(Composite parent) {
            super(parent, 1, KvmExitRateDataProvider.ID);
            setLabelProvider(new LegendLabelProvider());
        }

        @Override
        protected ITmfTreeColumnDataProvider getColumnDataProvider() {
            return () -> ImmutableList.of(
                    createColumn("CPUs", Comparator.comparing(TmfTreeViewerEntry::getName)), //$NON-NLS-1$
                    new TmfTreeColumnData("Legend")); //$NON-NLS-1$
        }
    }

    @Override
    protected @NonNull TmfViewer createLeftChildViewer(@Nullable Composite parent) {
        return new TreeXyViewer(Objects.requireNonNull(parent));
    }


}
