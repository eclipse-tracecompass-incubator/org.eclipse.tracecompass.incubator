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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtchart.ICustomPaintListener;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.IAxis;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.ITmfChartTimeProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfBaseProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfMouseDragZoomProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfXYChartViewer;


/**
 * Enables context menu if no dragging was detected. Is a copy of
 * {@link TmfMouseDragZoomProvider}. The difference from the original is that the
 * mouseUp method adds the possiblity to show the menu when right-clicking in
 * the viewer.
 *
 * @author Ivan Grinenko
 *
 */
public class MouseDragZoomProvider extends TmfBaseProvider
        implements MouseListener, MouseMoveListener, ICustomPaintListener {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------
    /** Cached start time */
    private long fStartTime;
    /** Cached end time */
    private long fEndTime;
    /** Flag indicating that an update is ongoing */
    private boolean fIsUpdate;
    private ActionsChartMultiViewer fChartViewer;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------
    /**
     * Default constructor
     *
     * @param tmfChartViewer
     *            the chart viewer reference.
     */
    public MouseDragZoomProvider(ActionsChartMultiViewer tmfChartViewer) {
        super(tmfChartViewer.getChartViewer());
        fChartViewer = tmfChartViewer;
    }

    // ------------------------------------------------------------------------
    // TmfBaseProvider
    // ------------------------------------------------------------------------

    @Override
    public void refresh() {
        // nothing to do
    }

    // ------------------------------------------------------------------------
    // MouseListener
    // ------------------------------------------------------------------------
    @Override
    public void mouseDoubleClick(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseDown(MouseEvent e) {
        if ((getChartViewer().getWindowDuration() != 0) && (e.button == 3)) {
            IAxis xAxis = getXAxis();
            fStartTime = limitXDataCoordinate(xAxis.getDataCoordinate(e.x));
            fEndTime = fStartTime;
            fIsUpdate = true;
        }
    }

    @Override
    public void mouseUp(MouseEvent e) {
        if ((fIsUpdate) && (fStartTime != fEndTime)) {
            if (fStartTime > fEndTime) {
                long tmp = fStartTime;
                fStartTime = fEndTime;
                fEndTime = tmp;
            }
            ITmfChartTimeProvider viewer = getChartViewer();
            viewer.updateWindow(fStartTime + viewer.getTimeOffset(), fEndTime + viewer.getTimeOffset());
        } else {
            if (e.button == 3) {
                fChartViewer.showMenu();
            }
        }

        if (fIsUpdate) {
            redraw();
        }
        fIsUpdate = false;
    }

    // ------------------------------------------------------------------------
    // MouseMoveListener
    // ------------------------------------------------------------------------
    @Override
    public void mouseMove(MouseEvent e) {
        if (fIsUpdate) {
            ITmfChartTimeProvider viewer = getChartViewer();

            fStartTime = viewer.getWindowStartTime();
            fEndTime = viewer.getWindowEndTime();

            if (viewer instanceof TmfXYChartViewer) {
                IAxis xAxis = getXAxis();
                long cursorTime = limitXDataCoordinate(xAxis.getDataCoordinate(e.x));
                TmfXYChartViewer xyChartViewer = (TmfXYChartViewer) viewer;
                xyChartViewer.updateStatusLine(fStartTime, fEndTime, cursorTime);
            }

            redraw();
        }
    }

    // ------------------------------------------------------------------------
    // ICustomPaintListener
    // ------------------------------------------------------------------------
    @Override
    public void paintControl(PaintEvent e) {
        if (fIsUpdate && (fStartTime != fEndTime)) {
            IAxis xAxis = getXAxis();
            int startX = xAxis.getPixelCoordinate(fStartTime);
            int endX = xAxis.getPixelCoordinate(fEndTime);

            e.gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND));
            if (fStartTime < fEndTime) {
                e.gc.fillRectangle(startX, 0, endX - startX, e.height);
            } else {
                e.gc.fillRectangle(endX, 0, startX - endX, e.height);
            }
            e.gc.drawLine(startX, 0, startX, e.height);
            e.gc.drawLine(endX, 0, endX, e.height);
        }
    }

    @Override
    public boolean drawBehindSeries() {
        return true;
    }

    /**
     * Returns the current or default display.
     *
     * @return the current or default display
     */
    protected static Display getDisplay() {
        Display display = Display.getCurrent();
        // may be null if outside the UI thread
        if (display == null) {
            display = Display.getDefault();
        }
        return display;
    }

}
