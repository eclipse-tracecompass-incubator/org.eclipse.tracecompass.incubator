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
package org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.timegraph.BaseDataProviderTimeGraphMultiViewer;
import org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.xychart.ChartMultiViewer;
import org.eclipse.tracecompass.internal.provisional.tmf.ui.widgets.timegraph.BaseDataProviderTimeGraphPresentationProvider;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.ITmfImageConstants;
import org.eclipse.tracecompass.internal.tmf.ui.viewers.xycharts.TmfXYChartTimeAdapter;
import org.eclipse.tracecompass.internal.tmf.ui.views.TmfAlignmentSynchronizer;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentInfo;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentSignal;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractSelectTreeViewer;
import org.eclipse.tracecompass.tmf.ui.views.ITimeReset;
import org.eclipse.tracecompass.tmf.ui.views.ITmfTimeAligned;
import org.eclipse.tracecompass.tmf.ui.views.ResetUtil;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphViewer;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.ITimeDataProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphColorScheme;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphScale;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils.TimeFormat;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * The Multiview.
 *
 * @author Ivan Grinenko
 *
 */
@SuppressWarnings("restriction")
public class MultiView extends TmfView implements ITmfTimeAligned, ITimeReset {
    /**
     * The view's ID.
     */
    public static final String VIEW_ID = "org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.MultiView"; //$NON-NLS-1$

    private static final TmfAlignmentSynchronizer TIME_ALIGNMENT_SYNCHRONIZER = TmfAlignmentSynchronizer.getInstance();

    private static final double ZOOM_FACTOR = 1.5;
    private static final int DEFAULT_HEIGHT = 22;

    private List<@NonNull IMultiViewer> fLanes = new ArrayList<>();

    private @NonNull TimeGraphColorScheme fColorScheme = new TimeGraphColorScheme();

    private Composite fMainComposite;
    private SashForm fSashForm;
    private Composite fTopRowLeftFiller;
    private Composite fTopRowRightFiller;
    private Composite fBottomRowLeftFiller;
    private Composite fBottomRowRightFiller;
    private ITimeDataProvider fTimeProvider;

    private TimeGraphScale fTopTimeScaleCtrl;
    private TimeGraphScale fBottomTimeScaleCtrl;
    private boolean fTopTimeScaleVisible = true;
    private boolean fBottomTimeScaleVisible = true;

    private Action fZoomInAction;
    private Action fZoomOutAction;

    private ITmfTrace fTrace;

    /**
     * Constructor.
     */
    public MultiView() {
        super(VIEW_ID);
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        fMainComposite = new Composite(parent, SWT.NONE) {
            @Override
            public void redraw() {
                redrawTimeScales();
                super.redraw();
            }
        };
        GridLayout mainLayout = new GridLayout(3, false);
        mainLayout.marginHeight = 0;
        mainLayout.marginWidth = 0;
        mainLayout.verticalSpacing = 0;
        mainLayout.horizontalSpacing = 0;
        fMainComposite.setLayout(mainLayout);

        fTopRowLeftFiller = new Composite(fMainComposite, SWT.NONE);
        fTopRowLeftFiller.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
        fTopRowLeftFiller.setLayout(new FillLayout());

        fTopTimeScaleCtrl = new TimeGraphScale(fMainComposite, fColorScheme);
        fTopTimeScaleCtrl.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
        fTopTimeScaleCtrl.setHeight(DEFAULT_HEIGHT);

        fTopRowRightFiller = new Composite(fMainComposite, SWT.NONE);
        fTopRowRightFiller.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
        fTopRowRightFiller.setLayout(new FillLayout());

        fSashForm = new SashForm(fMainComposite, SWT.VERTICAL);
        fSashForm.setBackground(fColorScheme.getColor(TimeGraphColorScheme.TOOL_BACKGROUND));
        fSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));

        fBottomRowLeftFiller = new Composite(fMainComposite, SWT.NONE);
        fBottomRowLeftFiller.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
        fBottomRowLeftFiller.setLayout(new FillLayout());

        fBottomTimeScaleCtrl = new TimeGraphScale(fMainComposite, fColorScheme, SWT.BOTTOM);
        fBottomTimeScaleCtrl.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        fBottomTimeScaleCtrl.setHeight(DEFAULT_HEIGHT);

        fBottomRowRightFiller = new Composite(fMainComposite, SWT.NONE);
        fBottomRowRightFiller.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        fBottomRowRightFiller.setLayout(new FillLayout());

        // Don't show time scales at the very beginning
        hideTimeScales();

        createMenuItems();
        createToolbarItems();
        selectActiveTrace();
    }

    @Override
    public void setFocus() {
        // Nothing yet
    }

    @Override
    public void dispose() {
        super.dispose();
        for (IMultiViewer lane : fLanes) {
            lane.dispose();
        }
        fLanes.clear();
    }

    @Override
    public TmfTimeViewAlignmentInfo getTimeViewAlignmentInfo() {
        if (fLanes.isEmpty()) {
            return new TmfTimeViewAlignmentInfo(fSashForm.getShell(), fSashForm.toDisplay(0, 0), 0);
        }

        return fLanes.get(0).getTimeViewAlignmentInfo();
    }

    @Override
    public int getAvailableWidth(int requestedOffset) {
        if (fLanes.isEmpty()) {
            return fSashForm.getDisplay().getBounds().width;
        }
        int ret = Integer.MAX_VALUE;
        for (IMultiViewer lane : fLanes) {
            ret = Math.min(ret, lane.getAvailableWidth(requestedOffset));
        }
        return ret;
    }

    @Override
    public void performAlign(int offset, int width) {
        for (IMultiViewer lane : fLanes) {
            lane.performAlign(offset, width);
        }
        GridData gdTop = (GridData) fTopRowLeftFiller.getLayoutData();
        gdTop.widthHint = offset;
        GridData gdBottom = (GridData) fBottomRowLeftFiller.getLayoutData();
        gdBottom.widthHint = offset;
        fMainComposite.layout();
    }

    @Override
    public void resetStartFinishTime(boolean notify) {
        TmfWindowRangeUpdatedSignal signal = new TmfWindowRangeUpdatedSignal(this, TmfTimeRange.ETERNITY, getTrace());
        broadcast(signal);
    }

    /**
     * Trace selected handler.
     *
     * @param signal
     *            the object with signal's data
     */
    @TmfSignalHandler
    public void traceSelected(final TmfTraceSelectedSignal signal) {
        fTrace = signal.getTrace();
        redrawTimeScales();
        alignViewers(false);
    }

    /**
     * Window range updated handler.
     *
     * @param signal
     *            the object with signal's data
     */
    @TmfSignalHandler
    public void windowRangeUpdated(TmfWindowRangeUpdatedSignal signal) {
        redrawTimeScales();
        alignViewers(false);
    }

    /**
     * @return Current trace of the view.
     */
    public ITmfTrace getTrace() {
        return fTrace;
    }

    /**
     * Toggles visibility of the top time axis.
     *
     * @param visible
     *            {@code true} to make it visible, {@code false} otherwise
     */
    public void setTopTimeScaleVisible(boolean visible) {
        fTopTimeScaleVisible = visible;
        showTimeScales();
    }

    /**
     * Toggles visibility of the bottom time axis.
     *
     * @param visible
     *            {@code true} to make it visible, {@code false} otherwise
     */
    public void setBottomTimeScaleVisible(boolean visible) {
        fBottomTimeScaleVisible = visible;
        showTimeScales();
    }

    private void hideTimeScales() {
        setControlVisible(fTopRowLeftFiller, false);
        setControlVisible(fTopTimeScaleCtrl, false);
        setControlVisible(fTopRowRightFiller, false);
        setControlVisible(fBottomRowLeftFiller, false);
        setControlVisible(fBottomTimeScaleCtrl, false);
        setControlVisible(fBottomRowRightFiller, false);
        fMainComposite.requestLayout();
    }

    private void showTimeScales() {
        setControlVisible(fTopRowLeftFiller, fTopTimeScaleVisible);
        setControlVisible(fTopTimeScaleCtrl, fTopTimeScaleVisible);
        setControlVisible(fTopRowRightFiller, fTopTimeScaleVisible);
        setControlVisible(fBottomRowLeftFiller, fBottomTimeScaleVisible);
        setControlVisible(fBottomTimeScaleCtrl, fBottomTimeScaleVisible);
        setControlVisible(fBottomRowRightFiller, fBottomTimeScaleVisible);
        fMainComposite.requestLayout();
    }

    private void addLane(@NonNull IMultiViewer lane) {
        if (fLanes.isEmpty()) {
            showTimeScales();
        }
        fLanes.add(lane);
    }

    private void removeLane() {
        if (fLanes.isEmpty()) {
            return;
        }
        IMultiViewer lane = fLanes.get(fLanes.size() - 1);
        if (lane instanceof TmfViewer) {
            Composite parent = ((TmfViewer) lane).getParent();
            if (parent != fSashForm) {
                parent.dispose();
            }
        }
        lane.dispose();
        fLanes.remove(lane);
        if (fLanes.isEmpty()) {
            hideTimeScales();
        }
    }

    private void selectActiveTrace() {
        ITmfTrace activeTrace = TmfTraceManager.getInstance().getActiveTrace();
        if (activeTrace != null) {
            traceSelected(new TmfTraceSelectedSignal(this, activeTrace));
        }
    }

    private void redrawTimeScales() {
        fTopTimeScaleCtrl.redraw();
        fBottomTimeScaleCtrl.redraw();
    }

    private void alignViewers(boolean synchronous) {
        getSite().getShell().getDisplay().asyncExec(() -> TIME_ALIGNMENT_SYNCHRONIZER.timeViewAlignmentUpdated(
                new TmfTimeViewAlignmentSignal(MultiView.this, getTimeViewAlignmentInfo(), synchronous)));
    }

    private void setTimeProvider(ITimeDataProvider timeProvider) {
        fTimeProvider = timeProvider;
        fTopTimeScaleCtrl.setTimeProvider(fTimeProvider);
        fBottomTimeScaleCtrl.setTimeProvider(fTimeProvider);
    }

    private void zoomIn() {
        long prevTime0 = fTimeProvider.getTime0();
        long prevTime1 = fTimeProvider.getTime1();
        long prevRange = prevTime1 - prevTime0;
        if (prevRange == 0) {
            return;
        }
        ITimeDataProvider provider = fTimeProvider;
        long selTime = (provider.getSelectionEnd() + provider.getSelectionBegin()) / 2;
        if (selTime < prevTime0 || selTime > prevTime1) {
            selTime = (prevTime0 + prevTime1) / 2;
        }
        long time0 = selTime - (long) ((selTime - prevTime0) / ZOOM_FACTOR);
        long time1 = selTime + (long) ((prevTime1 - selTime) / ZOOM_FACTOR);

        long min = fTimeProvider.getMinTimeInterval();
        if ((time1 - time0) < min) {
            time0 = selTime - (selTime - prevTime0) * min / prevRange;
            time1 = time0 + min;
        }

        fTimeProvider.setStartFinishTimeNotify(time0, time1);
    }

    private void zoomOut() {
        long prevTime0 = fTimeProvider.getTime0();
        long prevTime1 = fTimeProvider.getTime1();
        ITimeDataProvider provider = fTimeProvider;
        long selTime = (provider.getSelectionEnd() + provider.getSelectionBegin()) / 2;
        if (selTime < prevTime0 || selTime > prevTime1) {
            selTime = (prevTime0 + prevTime1) / 2;
        }
        long newInterval;
        long time0;
        if (prevTime1 - prevTime0 <= 1) {
            newInterval = 2;
            time0 = selTime - 1;
        } else {
            newInterval = (long) Math.ceil((prevTime1 - prevTime0) * ZOOM_FACTOR);
            time0 = selTime - (long) Math.ceil((selTime - prevTime0) * ZOOM_FACTOR);
        }
        /* snap to bounds if zooming out of range */
        time0 = Math.max(fTimeProvider.getMinTime(), Math.min(time0, fTimeProvider.getMaxTime() - newInterval));
        long time1 = time0 + newInterval;

        fTimeProvider.setStartFinishTimeNotify(time0, time1);
    }

    /**
     * Create items for general actions.
     */
    private void createToolbarItems() {
        IToolBarManager bars = getViewSite().getActionBars().getToolBarManager();
        bars.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, ResetUtil.createResetAction(this));
        bars.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getZoomInAction());
        bars.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getZoomOutAction());
    }

    /**
     * Create necessary items in the menu.
     */
    private void createMenuItems() {
        IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
        menuManager.add(createAddLaneAction());
        menuManager.add(createRemoveLaneAction());

    }

    private Action createAddLaneAction() {
        return new Action(Messages.Action_Add, IAction.AS_PUSH_BUTTON) {
            @Override
            public void run() {
                Shell shell = getSite().getShell();
                AddProviderDialog dialog = new AddProviderDialog(shell, fTrace);
                dialog.setBlockOnOpen(true);
                if (dialog.open() == Window.OK) {
                    if (dialog.getProviderType() == ProviderType.TREE_TIME_XY) {
                        addChartViewer(dialog);
                    }
                    if (dialog.getProviderType() == ProviderType.TIME_GRAPH) {
                        addTimeGraphViewer(dialog);
                    }
                    alignViewers(true);
                }
            }

            private void addChartViewer(AddProviderDialog dialog) {
                Composite composite = new Composite(fSashForm, SWT.NONE);
                composite.setLayout(new FillLayout());
                composite.setBackground(fColorScheme.getColor(TimeGraphColorScheme.BACKGROUND));
                String providerId = dialog.getProviderId();
                ChartMultiViewer viewer = new ChartMultiViewer(composite, providerId, providerId);
                viewer.setStatusLineManager(getViewSite().getActionBars().getStatusLineManager());
                if (fLanes.isEmpty()) {
                    viewer.getChartViewer().getSwtChart().addPaintListener(e -> redrawTimeScales());
                    TmfXYChartTimeAdapter timeProvider = new TmfXYChartTimeAdapter(viewer.getChartViewer());
                    timeProvider.setTimeFormat(TimeFormat.CALENDAR.convert());
                    setTimeProvider(timeProvider);
                }
                addLane(viewer);
                if (fTrace != null) {
                    viewer.loadTrace(fTrace);
                }
                // A workaround for XYCharts to realign after a selection
                // changes leading to possible changing of Y axis labels' width.
                if (viewer.getLeftChildViewer() instanceof AbstractSelectTreeViewer) {
                    AbstractSelectTreeViewer tree = (AbstractSelectTreeViewer) viewer.getLeftChildViewer();
                    tree.addSelectionChangeListener(e->alignViewers(false));
                }
            }

            private void addTimeGraphViewer(AddProviderDialog dialog) {
                Composite composite = new Composite(fSashForm, SWT.NONE);
                composite.setLayout(new FillLayout());
                composite.setBackground(fColorScheme.getColor(TimeGraphColorScheme.BACKGROUND));
                BaseDataProviderTimeGraphMultiViewer viewer = new BaseDataProviderTimeGraphMultiViewer(
                        composite, new BaseDataProviderTimeGraphPresentationProvider(), getViewSite(), dialog.getProviderId());
                viewer.init();
                if (fLanes.isEmpty()) {
                    TimeGraphViewer timeGraphViewer = viewer.getTimeGraphViewer();
                    timeGraphViewer.getTimeGraphControl().addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseUp(MouseEvent e) {
                            redrawTimeScales();
                        }
                    });
                    setTimeProvider(timeGraphViewer);
                }
                addLane(viewer);
            }

        };
    }

    private Action createRemoveLaneAction() {
        return new Action(Messages.Action_Remove, IAction.AS_PUSH_BUTTON) {
            @Override
            public void run() {
                removeLane();
                fSashForm.requestLayout();
            }
        };
    }

    /**
     * Get the zoom in action
     *
     * @return The Action object
     */
    private Action getZoomInAction() {
        if (fZoomInAction == null) {
            fZoomInAction = new Action() {
                @Override
                public void run() {
                    zoomIn();
                }
            };
            fZoomInAction.setText(org.eclipse.tracecompass.internal.tmf.ui.Messages.TmfTimeGraphViewer_ZoomInActionNameText);
            fZoomInAction.setToolTipText(org.eclipse.tracecompass.internal.tmf.ui.Messages.TmfTimeGraphViewer_ZoomInActionToolTipText);
            fZoomInAction.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(ITmfImageConstants.IMG_UI_ZOOM_IN_MENU));
        }
        return fZoomInAction;
    }

    /**
     * Get the zoom out action
     *
     * @return The Action object
     */
    private Action getZoomOutAction() {
        if (fZoomOutAction == null) {
            fZoomOutAction = new Action() {
                @Override
                public void run() {
                    zoomOut();
                }
            };
            fZoomOutAction.setText(org.eclipse.tracecompass.internal.tmf.ui.Messages.TmfTimeGraphViewer_ZoomOutActionNameText);
            fZoomOutAction.setToolTipText(org.eclipse.tracecompass.internal.tmf.ui.Messages.TmfTimeGraphViewer_ZoomOutActionToolTipText);
            fZoomOutAction.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(ITmfImageConstants.IMG_UI_ZOOM_OUT_MENU));
        }
        return fZoomOutAction;
    }

    private static void setControlVisible(Control control, boolean visible) {
        GridData gridData = (GridData) control.getLayoutData();
        gridData.exclude = !visible;
        control.setVisible(visible);
    }

}
