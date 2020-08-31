/**********************************************************************
 * Copyright (c) 2020 Draeger, Auriga
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.xychart;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.IMultiViewer;
import org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.MultiView;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentInfo;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentSignal;
import org.eclipse.tracecompass.tmf.ui.viewers.ILegendImageProvider2;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfTimeViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractSelectTreeViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractSelectTreeViewer2;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.XYChartLegendImageProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfCommonXAxisChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfFilteredXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfXYChartSettings;
import org.eclipse.tracecompass.tmf.ui.views.ManyEntriesSelectedDialogPreCheckedListener;
import org.eclipse.tracecompass.tmf.ui.views.xychart.TmfChartView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.dialogs.TriStateFilteredCheckboxTree;

/**
 * The viewer corresponding to the {@link TmfChartView} to be used with the
 * {@link MultiView}.
 *
 * @author Ivan Grinenko
 *
 */
public class ChartMultiViewer extends TmfTimeViewer implements IMultiViewer {

    private static final int[] DEFAULT_WEIGHTS = { 1, 3 };

    private final SashForm fSashForm;
    private final TreeViewer fLeftViewer;
    /** The TMF XY Chart reference */
    private final TmfXYChartViewer fChartViewer;
    /** A composite that allows us to add margins */
    private final Composite fXYViewerContainer;

    /**
     * Constructor.
     *
     * @param parent
     *            parent for the viewer
     * @param providerId
     *            provider's ID
     */
    public ChartMultiViewer(Composite parent, String providerId) {
        super(parent);
        fSashForm = new SashForm(parent, SWT.NONE);

        fLeftViewer = new TreeViewer(fSashForm, providerId);
        fXYViewerContainer = new Composite(fSashForm, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        fXYViewerContainer.setLayout(layout);

        fChartViewer = new TmfFilteredXYChartViewer(fXYViewerContainer, new TmfXYChartSettings("", "", "", 1), providerId); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        fChartViewer.setTimeAxisVisible(false);
        fChartViewer.setSendTimeAlignSignals(true);
        fChartViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        fChartViewer.getSwtChart().getAxisSet().getXAxis(0).getTick().setVisible(false);

        fChartViewer.getControl().addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                // Sashes in a SashForm are being created on layout so add the
                // drag listener here
                for (Control control : fSashForm.getChildren()) {
                    if (control instanceof Sash) {
                        control.removePaintListener(this);
                        control.addListener(SWT.Selection, event -> TmfSignalManager.dispatchSignal(
                                new TmfTimeViewAlignmentSignal(fSashForm, getTimeViewAlignmentInfo())));
                        // There should be only one sash
                        break;
                    }
                }
            }
        });

        fSashForm.setWeights(DEFAULT_WEIGHTS);
        coupleSelectViewer();
    }

    @Override
    public Control getControl() {
        return fSashForm;
    }

    @Override
    public void dispose() {
        super.dispose();
        fLeftViewer.dispose();
        fSashForm.dispose();
    }

    @Override
    public void refresh() {
        fLeftViewer.refresh();
        fChartViewer.refresh();
    }

    @Override
    public void loadTrace(ITmfTrace trace) {
        super.loadTrace(trace);
        fLeftViewer.loadTrace(trace);
        fChartViewer.loadTrace(trace);
    }

    @Override
    public TmfTimeViewAlignmentInfo getTimeViewAlignmentInfo() {
        if (fChartViewer == null) {
            return null;
        }

        return new TmfTimeViewAlignmentInfo(fChartViewer.getControl().getShell(), fSashForm.toDisplay(0, 0), getTimeAxisOffset());
    }

    @Override
    public int getAvailableWidth(int requestedOffset) {
        if (fChartViewer == null) {
            return 0;
        }

        int pointAreaWidth = fChartViewer.getPointAreaWidth();
        int curTimeAxisOffset = getTimeAxisOffset();
        if (pointAreaWidth <= 0) {
            pointAreaWidth = fSashForm.getBounds().width - curTimeAxisOffset;
        }
        int endOffset = curTimeAxisOffset + pointAreaWidth;
        GridLayout layout = (GridLayout) fXYViewerContainer.getLayout();
        int endOffsetWithoutMargin = endOffset + layout.marginRight;
        int availableWidth = endOffsetWithoutMargin - requestedOffset;
        availableWidth = Math.min(fSashForm.getBounds().width, Math.max(0, availableWidth));
        return availableWidth;
    }

    @Override
    public void performAlign(int offset, int width) {
        int total = fSashForm.getBounds().width;
        int plotAreaOffset = fChartViewer.getPointAreaOffset();
        int width1 = Math.max(0, offset - plotAreaOffset - fSashForm.getSashWidth());
        int width2 = Math.max(0, total - width1 - fSashForm.getSashWidth());
        if (width1 >= 0 && width2 > 0 || width1 > 0 && width2 >= 0) {
            fSashForm.setWeights(new int[] { width1, width2 });
            fSashForm.layout();
        }

        Composite composite = fXYViewerContainer;
        GridLayout layout = (GridLayout) composite.getLayout();
        int timeAxisWidth = getAvailableWidth(offset);
        int marginSize = timeAxisWidth - width;
        layout.marginRight = Math.max(0, marginSize);
        composite.layout();
    }

    /**
     * Sets status line manager for the chart viewer.
     *
     * @param statusLineManager
     *            status line manager for the chart viewer
     */
    public void setStatusLineManager(IStatusLineManager statusLineManager) {
        fChartViewer.setStatusLineManager(statusLineManager);
    }

    /**
     * Returns the TMF XY chart viewer implementation.
     *
     * @return the TMF XY chart viewer {@link TmfXYChartViewer}
     */
    public TmfXYChartViewer getChartViewer() {
        return fChartViewer;
    }

    /**
     * Returns the left TMF viewer implementation.
     *
     * @return the left TMF viewer {@link TmfViewer}
     */
    public TmfViewer getLeftChildViewer() {
        return fLeftViewer;
    }

    /**
     * Method to couple {@link AbstractSelectTreeViewer} and
     * {@link TmfFilteredXYChartViewer} so that they use the same legend and
     * that the chart listens to selected items in the tree
     */
    private void coupleSelectViewer() {
        TmfViewer tree = fLeftViewer;
        TmfXYChartViewer chart = fChartViewer;
        if (tree instanceof AbstractSelectTreeViewer2 && chart instanceof TmfFilteredXYChartViewer) {
            ILegendImageProvider2 legendImageProvider = new XYChartLegendImageProvider((TmfCommonXAxisChartViewer) chart);
            AbstractSelectTreeViewer2 selectTree = (AbstractSelectTreeViewer2) tree;
            selectTree.addTreeListener((TmfFilteredXYChartViewer) chart);
            selectTree.setLegendImageProvider(legendImageProvider);
            TriStateFilteredCheckboxTree checkboxTree = selectTree.getTriStateFilteredCheckboxTree();
            checkboxTree.addPreCheckStateListener(new ManyEntriesSelectedDialogPreCheckedListener(checkboxTree));
        }
    }

    private int getTimeAxisOffset() {
        return fSashForm.getChildren()[0].getSize().x + fSashForm.getSashWidth() + fChartViewer.getPointAreaOffset();
    }

}
