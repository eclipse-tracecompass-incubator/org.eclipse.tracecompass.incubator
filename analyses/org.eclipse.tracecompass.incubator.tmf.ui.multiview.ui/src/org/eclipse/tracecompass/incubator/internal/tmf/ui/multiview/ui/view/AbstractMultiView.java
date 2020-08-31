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
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.timegraph.ActionsDataProviderTimeGraphMultiViewer;
import org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.timegraph.BaseDataProviderTimeGraphMultiViewer;
import org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.xychart.ActionsChartMultiViewer;
import org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.xychart.ChartMultiViewer;
import org.eclipse.tracecompass.internal.provisional.tmf.ui.widgets.timegraph.BaseDataProviderTimeGraphPresentationProvider;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.ITmfImageConstants;
import org.eclipse.tracecompass.internal.tmf.ui.viewers.xychart.TmfXYChartTimeAdapter;
import org.eclipse.tracecompass.internal.tmf.ui.views.TmfAlignmentSynchronizer;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentInfo;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentSignal;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractSelectTreeViewer2;
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

import com.google.common.collect.ImmutableList;

/**
 * An abstract multi view, that allows to add/removed viewer lanes inside a same
 * view. The base class contains the main widget, for example the time scales,
 * the main actions for zoom in/out, etc.
 *
 * @author Ivan Grinenko
 */
@SuppressWarnings("restriction")
public abstract class AbstractMultiView extends TmfView implements ITmfTimeAligned, ITimeReset {

    private static final TmfAlignmentSynchronizer TIME_ALIGNMENT_SYNCHRONIZER = TmfAlignmentSynchronizer.getInstance();

    private static final double ZOOM_FACTOR = 1.5;
    private static final int DEFAULT_HEIGHT = 22;

    private final @NonNull List<@NonNull IMultiViewer> fLanes = new ArrayList<>();

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
     *
     * @param viewId
     *            The ID of this view
     */
    public AbstractMultiView(String viewId) {
        super(viewId);
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        Composite mainComposite = new Composite(parent, SWT.NONE) {
            @Override
            public void redraw() {
                redrawTimeScales();
                super.redraw();
            }
        };
        fMainComposite = mainComposite;
        GridLayout mainLayout = new GridLayout(3, false);
        mainLayout.marginHeight = 0;
        mainLayout.marginWidth = 0;
        mainLayout.verticalSpacing = 0;
        mainLayout.horizontalSpacing = 0;
        mainComposite.setLayout(mainLayout);

        fTopRowLeftFiller = new Composite(mainComposite, SWT.NONE);
        fTopRowLeftFiller.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
        fTopRowLeftFiller.setLayout(new FillLayout());

        fTopTimeScaleCtrl = new TimeGraphScale(mainComposite, fColorScheme);
        fTopTimeScaleCtrl.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false));
        fTopTimeScaleCtrl.setHeight(DEFAULT_HEIGHT);

        fTopRowRightFiller = new Composite(mainComposite, SWT.NONE);
        fTopRowRightFiller.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
        fTopRowRightFiller.setLayout(new FillLayout());

        SashForm sashForm = new SashForm(mainComposite, SWT.VERTICAL);
        sashForm.setBackground(fColorScheme.getColor(TimeGraphColorScheme.TOOL_BACKGROUND));
        sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
        fSashForm = sashForm;

        fBottomRowLeftFiller = new Composite(mainComposite, SWT.NONE);
        fBottomRowLeftFiller.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
        fBottomRowLeftFiller.setLayout(new FillLayout());

        fBottomTimeScaleCtrl = new TimeGraphScale(mainComposite, fColorScheme, SWT.BOTTOM);
        fBottomTimeScaleCtrl.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        fBottomTimeScaleCtrl.setHeight(DEFAULT_HEIGHT);

        fBottomRowRightFiller = new Composite(mainComposite, SWT.NONE);
        fBottomRowRightFiller.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        fBottomRowRightFiller.setLayout(new FillLayout());

        createMenuItems();
        createToolbarItems();
        selectActiveTrace();
        partControlCreated(mainComposite, sashForm);
    }

    /**
     * The view content and common widgets have been initialized. This method
     * can be overridden by children classes to initialize what they need
     *
     * @param mainComposite
     *            The main composite to which additional widgets should be added
     * @param sashForm
     *            The main sashForm, to which the viewers should be added
     */
    protected abstract void partControlCreated(@NonNull Composite mainComposite, @NonNull SashForm sashForm);

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
     * Get the view's color scheme
     *
     * @return The color scheme
     */
    public TimeGraphColorScheme getColorScheme() {
        return fColorScheme;
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

    /**
     * Hide all the times scales of the view
     */
    protected void hideTimeScales() {
        setControlVisible(fTopRowLeftFiller, false);
        setControlVisible(fTopTimeScaleCtrl, false);
        setControlVisible(fTopRowRightFiller, false);
        setControlVisible(fBottomRowLeftFiller, false);
        setControlVisible(fBottomTimeScaleCtrl, false);
        setControlVisible(fBottomRowRightFiller, false);
        fMainComposite.requestLayout();
    }

    /**
     * Show all the time scales of the view
     */
    protected void showTimeScales() {
        setControlVisible(fTopRowLeftFiller, fTopTimeScaleVisible);
        setControlVisible(fTopTimeScaleCtrl, fTopTimeScaleVisible);
        setControlVisible(fTopRowRightFiller, fTopTimeScaleVisible);
        setControlVisible(fBottomRowLeftFiller, fBottomTimeScaleVisible);
        setControlVisible(fBottomTimeScaleCtrl, fBottomTimeScaleVisible);
        setControlVisible(fBottomRowRightFiller, fBottomTimeScaleVisible);
        fMainComposite.requestLayout();
    }

    /**
     * Add a lane to this view. This method is automatically called if viewers
     * have been created using the default methods
     * {@link #addChartViewer(String, boolean)} and
     * {@link #addTimeGraphViewer(String, boolean)}. Implementations creating
     * their own lanes will call this method to add the viewer's lane.
     *
     * @param lane
     *            The lane to add
     */
    protected void addLane(@NonNull IMultiViewer lane) {
        if (fLanes.isEmpty()) {
            showTimeScales();
        }
        fLanes.add(lane);
    }

    /**
     * Remove a lane from the view
     *
     * @param lane
     *            The lane to remove
     */
    protected void removeLane(@NonNull IMultiViewer lane) {
        if (fLanes.isEmpty()) {
            return;
        }
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

    /**
     * Get the immutable list of lanes displayed in the view
     *
     * @return The immutable list of lanes
     */
    protected final List<@NonNull IMultiViewer> getLanes() {
        return ImmutableList.copyOf(fLanes);
    }

    /**
     * Get whether this view has lanes
     *
     * @return <code>true</code> if the view has active lanes
     */
    protected boolean hasLanes() {
        return !fLanes.isEmpty();
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

    /**
     * Request an alignment of the viewers
     *
     * @param synchronous
     *            whether or not the signal should be processed right away. This
     *            is useful for signals that are sent not repetitively. For
     *            example, a sash being dragged would not be synchronous because
     *            the signal gets fired repeatedly. A view that has completed
     *            computing its data could send a synchronous signal.
     */
    protected void alignViewers(boolean synchronous) {
        getSite().getShell().getDisplay().asyncExec(() -> TIME_ALIGNMENT_SYNCHRONIZER.timeViewAlignmentUpdated(
                new TmfTimeViewAlignmentSignal(AbstractMultiView.this, getTimeViewAlignmentInfo(), synchronous)));
    }

    private void setTimeProvider(ITimeDataProvider timeProvider) {
        fTimeProvider = timeProvider;
        fTopTimeScaleCtrl.setTimeProvider(fTimeProvider);
        fBottomTimeScaleCtrl.setTimeProvider(fTimeProvider);
    }

    /**
     * Get the time provider for this view. This is the object that provides
     * information on the current window range and selection, time format, etc.
     * The time provider will typically have been set when the first viewer is
     * added to the view, and there will be one as long as there are viewers.
     *
     * @return The view's time provider
     */
    protected ITimeDataProvider getTimeProvider() {
        return fTimeProvider;
    }

    /**
     * Refresh the view's layout. This method should be called after viewers
     * have been added to the view.
     */
    protected void refreshLayout() {
        fSashForm.requestLayout();
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
     * Create items for general actions. Inheriting classes can override this to
     * add actions to the toolbar. The base method puts the actions to manage
     * the time range (zoom in/out, reset)
     */
    protected void createToolbarItems() {
        IToolBarManager bars = getViewSite().getActionBars().getToolBarManager();
        bars.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, ResetUtil.createResetAction(this));
        bars.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getZoomInAction());
        bars.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, getZoomOutAction());
    }

    /**
     * Create necessary items in the menu. Inheriting classes can override this
     * to add actions to the menu. By default, no items are specified.
     */
    protected void createMenuItems() {

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

    /**
     * Create and add a new chart viewer for the given data provider
     *
     * @param providerId
     *            The ID of the data provider to source in the viewer
     * @param withActions
     *            Whether to construct a viewer with actions as context menu or
     *            not
     * @return The newly created chart viewer
     */
    protected ChartMultiViewer addChartViewer(String providerId, boolean withActions) {
        SashForm sashForm = fSashForm;
        ITmfTrace trace = getTrace();
        Composite composite = new Composite(sashForm, SWT.NONE);
        composite.setLayout(new FillLayout());
        composite.setBackground(fColorScheme.getColor(TimeGraphColorScheme.BACKGROUND));
        ChartMultiViewer viewer = withActions ? new ActionsChartMultiViewer(composite, providerId, getViewSite()) : new ChartMultiViewer(composite, providerId);
        viewer.setStatusLineManager(getViewSite().getActionBars().getStatusLineManager());
        if (!hasLanes()) {
            viewer.getChartViewer().getSwtChart().addPaintListener(e -> redrawTimeScales());
            TmfXYChartTimeAdapter timeProvider = new TmfXYChartTimeAdapter(Objects.requireNonNull(viewer.getChartViewer()));
            timeProvider.setTimeFormat(TimeFormat.CALENDAR.convert());
            setTimeProvider(timeProvider);
        }
        addLane(viewer);
        if (trace != null) {
            viewer.loadTrace(trace);
        }
        // A workaround for XYCharts to realign after a selection
        // changes leading to possible changing of Y axis labels' width.
        if (viewer.getLeftChildViewer() instanceof AbstractSelectTreeViewer2) {
            AbstractSelectTreeViewer2 tree = (AbstractSelectTreeViewer2) viewer.getLeftChildViewer();
            tree.addSelectionChangeListener(e -> alignViewers(false));
        }
        return viewer;
    }

    /**
     * Create and add a new time graph viewer for the given data provider
     *
     * @param providerId
     *            The ID of the data provider to source in the viewer
     * @param withActions
     *            Whether to construct a viewer with actions as context menu or
     *            not
     * @return The new time graph viewer
     */
    protected BaseDataProviderTimeGraphMultiViewer addTimeGraphViewer(String providerId, boolean withActions) {
        SashForm sashForm = fSashForm;
        Composite composite = new Composite(sashForm, SWT.NONE);
        composite.setLayout(new FillLayout());
        composite.setBackground(fColorScheme.getColor(TimeGraphColorScheme.BACKGROUND));
        BaseDataProviderTimeGraphMultiViewer viewer = withActions ? new ActionsDataProviderTimeGraphMultiViewer(
                composite, new BaseDataProviderTimeGraphPresentationProvider(), getViewSite(), providerId)
                : new BaseDataProviderTimeGraphMultiViewer(
                        composite, new BaseDataProviderTimeGraphPresentationProvider(), getViewSite(), providerId);
        viewer.init();
        if (!hasLanes()) {
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
        return viewer;
    }

    private static void setControlVisible(Control control, boolean visible) {
        GridData gridData = (GridData) control.getLayoutData();
        gridData.exclude = !visible;
        control.setVisible(visible);
    }

}
