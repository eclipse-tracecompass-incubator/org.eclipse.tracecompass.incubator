/*******************************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.executioncomparision.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.ScopeLog;
import org.eclipse.tracecompass.incubator.internal.callstack.ui.Activator;
import org.eclipse.tracecompass.incubator.internal.executioncomparision.core.TmfCheckboxChangedSignal;
import org.eclipse.tracecompass.incubator.internal.executioncomparision.core.TmfComparisonFilteringUpdatedSignal;
import org.eclipse.tracecompass.internal.tmf.ui.viewers.eventdensity.EventDensityTreeViewer;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentInfo;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentSignal;
import org.eclipse.tracecompass.tmf.ui.viewers.ILegendImageProvider2;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfTimeViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractSelectTreeViewer2;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ICheckboxTreeViewerListener;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeViewerEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.TmfXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.XYChartLegendImageProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfCommonXAxisChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfFilteredXYChartViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.xychart.linechart.TmfXYChartSettings;
import org.eclipse.tracecompass.tmf.ui.views.ManyEntriesSelectedDialogPreCheckedListener;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.dialogs.TriStateFilteredCheckboxTree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;

/**
 *
 */
@SuppressWarnings("restriction")
public class MultipleDensityView extends DifferentialFlameGraphView implements ICheckboxTreeViewerListener {

    /**
     * the id of the view
     */
    public static final String id = "org.eclipse.tracecompass.incubator.internal.entexecutioncomparison.ui.execComparison"; //$NON-NLS-1$
    private static final int[] DEFAULT_WEIGHTS = new int[] { 4, 6 };
    private static final int[] DEFAULT_WEIGHTS_FILTERING_VIEW = new int[] { 495, 10, 495 };
    private static final int[] DEFAULT_WEIGHTS_FILTERING_H = new int[] { 3, 9 };
    private static final int[] DEFAULT_WEIGHTS_LABELS = new int[] { 1, 9 };

    /**
     * Default zoom range
     *
     * @since 4.1
     */
    private static final String STATISTIC_ICON_PATH = "icons/etool16/group_by.gif"; //$NON-NLS-1$
    @SuppressWarnings("null")
    private static final ImageDescriptor STATISTIC_BY_ICON = Activator.getDefault().getImageDescripterFromPath(STATISTIC_ICON_PATH);

    private @Nullable TmfXYChartViewer fChartViewerA;
    /** A composite that allows us to add margins */
    private @Nullable SashForm fXYViewerContainerA;
    private @Nullable TmfViewer fTmfViewerA;
    private @Nullable SashForm fSashFormLeftChildA;
    private @Nullable SashForm fSashFormLeftChildB;
    private @Nullable IContextService fContextService;
    private @Nullable Action fConfigureStatisticAction;
    private List<IContextActivation> fActiveContexts = new ArrayList<>();

    private @Nullable TmfXYChartViewer fChartViewerB;
    /** A composite that allows us to add margins */
    private @Nullable SashForm fXYViewerContainerB;
    private @Nullable TmfViewer fTmfViewerB;
    private List<String> fTraceListA = new ArrayList<>();
    private List<String> fTraceListB = new ArrayList<>();

    /**
     * the title of the view
     */
    @SuppressWarnings("null")
    public static final String VIEW_TITLE = Messages.AbstractMultipleDensityView_title;
    private ITmfTimestamp fStartTimeA = TmfTimestamp.BIG_BANG;
    private ITmfTimestamp fStartTimeB = TmfTimestamp.BIG_BANG;
    private ITmfTimestamp fEndTimeA = TmfTimestamp.BIG_CRUNCH;
    private ITmfTimestamp fEndTimeB = TmfTimestamp.BIG_CRUNCH;
    private String fStatistic = "duration"; //$NON-NLS-1$

    private @Nullable Listener fSashDragListener;
    private static final String TMF_VIEW_UI_CONTEXT = "org.eclipse.tracecompass.tmf.ui.view.context"; //$NON-NLS-1$

    // SashForm fsashForm;

    /**
     * Constructs a segment store density view
     */
    public MultipleDensityView() {
        super();
    }

    /**
     * Used to keep the density charts in sync with Duration chart.
     */
    @Override
    public void createPartControl(@Nullable Composite parent) {
        TmfSignalManager.register(this);

        final SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
        // fsashForm = sashForm;

        SashForm sashFormFiltering = new SashForm(sashForm, SWT.HORIZONTAL);

        SashForm sashFormGroupA = new SashForm(sashFormFiltering, SWT.VERTICAL);

        SashForm distance = new SashForm(sashFormFiltering, SWT.NONE);
        distance.pack();

        SashForm sashFormGroupB = new SashForm(sashFormFiltering, SWT.VERTICAL);

        Text labelGroupA = new Text(sashFormGroupA, SWT.BORDER | SWT.CENTER);
        labelGroupA.setText(Messages.AbstractMultipleDensityView_GroupA);

        Text labelGroupB = new Text(sashFormGroupB, SWT.BORDER | SWT.CENTER);
        labelGroupB.setText(Messages.AbstractMultipleDensityView_GroupB);

        SashForm densityA = new SashForm(sashFormGroupA, SWT.HORIZONTAL);
        SashForm densityB = new SashForm(sashFormGroupB, SWT.HORIZONTAL);

        sashFormGroupA.setWeights(DEFAULT_WEIGHTS_LABELS);
        sashFormGroupB.setWeights(DEFAULT_WEIGHTS_LABELS);

        setSashFormLeftChildA(new SashForm(densityA, SWT.None));

        setTmfViewerA(createLeftChildViewer(getSashFormLeftChildA()));
        SashForm xYViewerContainerA = new SashForm(densityA, SWT.None);
        fXYViewerContainerA = xYViewerContainerA;
        xYViewerContainerA.setLayout(GridLayoutFactory.fillDefaults().create());

        TmfXYChartViewer chartViewerA = createChartViewer(xYViewerContainerA);
        setChartViewerA(chartViewerA);

        chartViewerA.getControl().addPaintListener(new PaintListener() {
            @Override
            public void paintControl(@Nullable PaintEvent e) {
                // Sashes in a SashForm are being created on layout so add the
                // drag listener here
                Listener sashDragListener = fSashDragListener;
                if (sashDragListener == null) {
                    for (Control control : getSashFormLeftChildA().getChildren()) {
                        if (control instanceof Sash) {
                            sashDragListener = event -> TmfSignalManager.dispatchSignal(new TmfTimeViewAlignmentSignal(getSashFormLeftChildA(), getTimeViewAlignmentInfo(chartViewerA, getSashFormLeftChildA())));
                            fSashDragListener = sashDragListener;
                            control.removePaintListener(this);
                            control.addListener(SWT.Selection, sashDragListener);
                            // There should be only one sash
                            break;
                        }

                    }
                }
            }
        });

        IStatusLineManager statusLineManager = getViewSite().getActionBars().getStatusLineManager();
        chartViewerA.setStatusLineManager(statusLineManager);
        coupleSelectViewer(getTmfViewerA(), chartViewerA);
        ((AbstractSelectTreeViewer2) getTmfViewerA()).addTreeListener(this);

        IWorkbenchPartSite site = getSite();
        setContextService(site.getWorkbenchWindow().getService(IContextService.class));

        setSashFormLeftChildB(new SashForm(densityB, SWT.VERTICAL));
        setTmfViewerB(createLeftChildViewer(getSashFormLeftChildB()));
        SashForm xYViewerContainerB = new SashForm(densityB, SWT.VERTICAL);
        fXYViewerContainerB = xYViewerContainerB;
        xYViewerContainerB.setLayout(GridLayoutFactory.fillDefaults().create());

        TmfXYChartViewer chartViewerB = createChartViewer(xYViewerContainerB);
        setChartViewerB(chartViewerB);

        chartViewerB.getControl().addPaintListener(new PaintListener() {
            @Override
            public void paintControl(@Nullable PaintEvent e) {
                // Sashes in a SashForm are being created on layout so add the
                // drag listener here
                if (fSashDragListener == null) {
                    for (Control control : getSashFormLeftChildB().getChildren()) {
                        if (control instanceof Sash) {
                            Objects.requireNonNull(getSashFormLeftChildB());
                            fSashDragListener = event -> TmfSignalManager.dispatchSignal(new TmfTimeViewAlignmentSignal(getSashFormLeftChildB(), getTimeViewAlignmentInfo(chartViewerB, getSashFormLeftChildB())));
                            control.removePaintListener(this);
                            control.addListener(SWT.Selection, fSashDragListener);
                            // There should be only one sash
                            break;
                        }

                    }
                }
            }
        });

        chartViewerB.setStatusLineManager(statusLineManager);
        coupleSelectViewer(getTmfViewerB(), chartViewerB);
        ((AbstractSelectTreeViewer2) getTmfViewerB()).addTreeListener(this);

        contributeToActionBars();

        densityA.setWeights(DEFAULT_WEIGHTS_FILTERING_H);
        densityB.setWeights(DEFAULT_WEIGHTS_FILTERING_H);

        sashFormFiltering.setWeights(DEFAULT_WEIGHTS_FILTERING_VIEW);

        super.createPartControl(sashForm);

        sashForm.setWeights(DEFAULT_WEIGHTS);
        ITmfTrace activetrace = TmfTraceManager.getInstance().getActiveTrace();
        if (activetrace != null) {
            buildDifferetialFlameGraph();

        }

    }

    /**
     * /** Return the time alignment information
     *
     * @param chartViewer
     *            the event distribution viewer
     * @param sashFormLeftChild
     *            the check box viewer
     * @return the time alignment information
     */
    public TmfTimeViewAlignmentInfo getTimeViewAlignmentInfo(TmfXYChartViewer chartViewer, SashForm sashFormLeftChild) {
        return new TmfTimeViewAlignmentInfo(chartViewer.getControl().getShell(), sashFormLeftChild.toDisplay(0, 0), getTimeAxisOffset(chartViewer, sashFormLeftChild));
    }

    private static int getTimeAxisOffset(TmfXYChartViewer chartViewer, SashForm sashFormLeftChild) {
        return sashFormLeftChild.getChildren()[0].getSize().x + sashFormLeftChild.getSashWidth() + chartViewer.getPointAreaOffset();
    }

    @Override
    @TmfSignalHandler
    public void traceSelected(final TmfTraceSelectedSignal signal) {
        super.traceSelected(signal);
        if (getTmfViewerA() instanceof TmfTimeViewer) {
            ((TmfTimeViewer) getTmfViewerA()).traceSelected(signal);
        }
        getChartViewerA().traceSelected(signal);

        if (getTmfViewerB() instanceof TmfTimeViewer) {
            ((TmfTimeViewer) getTmfViewerB()).traceSelected(signal);
        }
        getChartViewerB().traceSelected(signal);
        ITmfTrace trace = signal.getTrace();
        fStartTimeA = trace.getStartTime();
        fEndTimeA = trace.getEndTime();
        fStartTimeB = trace.getStartTime();
        fEndTimeB = trace.getEndTime();

        TmfComparisonFilteringUpdatedSignal rangUpdateSignal = new TmfComparisonFilteringUpdatedSignal(this, fStartTimeA, fEndTimeA, fStartTimeB, fEndTimeB, fStatistic, fTraceListA, fTraceListB);
        TmfSignalManager.dispatchSignal(rangUpdateSignal);
        buildDifferetialFlameGraph();
    }

    private Action createStatisticAction(String name) {
        return new Action(name, IAction.AS_RADIO_BUTTON) {
            @Override
            public void run() {
                ITmfTrace trace = getTrace();
                if (trace == null) {
                    return;
                }
                fStatistic = name;
                TmfComparisonFilteringUpdatedSignal rangUpdateSignal = new TmfComparisonFilteringUpdatedSignal(this, fStatistic, null, null);
                TmfSignalManager.dispatchSignal(rangUpdateSignal);

                buildDifferetialFlameGraph();
            }
        };
    }

    /**
     * @param signal
     *            the trace open signal
     */
    @TmfSignalHandler
    public void traceOpened(TmfTraceOpenedSignal signal) {
        getChartViewerA().traceOpened(signal);
        getChartViewerB().traceOpened(signal);
    }

    private void buildDifferetialFlameGraph() {

        ITmfTrace activetrace = TmfTraceManager.getInstance().getActiveTrace();
        if (activetrace != null) {
            Display.getDefault().asyncExec(() -> buildFlameGraph(activetrace, null, null));

        }
    }

    /**
     * create left child viewer for event density chart
     *
     * @param parent
     *            composite
     * @return left chart viewer
     */
    public TmfViewer createLeftChildViewer(Composite parent) {
        EventDensityTreeViewer histogramTreeViewer = new EventDensityTreeViewer(parent);
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace != null) {
            histogramTreeViewer.traceSelected(new TmfTraceSelectedSignal(this, trace));
        }
        return histogramTreeViewer;
    }

    private TmfXYChartViewer createChartViewer(Composite parent) {
        MultipleEventDensityViewer chartViewer = new MultipleEventDensityViewer(parent, new TmfXYChartSettings(null, null, null, 1));
        chartViewer.setSendTimeAlignSignals(true);
        chartViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        chartViewer.getControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(@Nullable MouseEvent e) {
                super.mouseDoubleClick(e);
                resetStartFinishTime(true, chartViewer);
            }
        });
        chartViewer.getControl().addFocusListener(new FocusListener() {
            @Override
            public void focusLost(@Nullable FocusEvent e) {
                deactivateContextService();
            }

            @Override
            public void focusGained(@Nullable FocusEvent e) {
                activateContextService();
            }
        });

        return chartViewer;
    }

    /**
     * Reset the start and end times.
     *
     * @param notify
     *            if true, notify the registered listeners
     * @param chart
     *            determines which chart to reset start and end times
     */
    public void resetStartFinishTime(boolean notify, TmfXYChartViewer chart) {
        TmfWindowRangeUpdatedSignal signal = new TmfWindowRangeUpdatedSignal(this, TmfTimeRange.ETERNITY, getTrace());
        if (notify) {
            broadcast(signal);

        } else {
            chart.windowRangeUpdated(signal);
            chart.selectionRangeUpdated(new TmfSelectionRangeUpdatedSignal(this, TmfTimestamp.fromNanos(chart.getWindowStartTime()), TmfTimestamp.fromNanos(chart.getWindowEndTime())));
        }
    }

    private void deactivateContextService() {
        getContextService().deactivateContexts(fActiveContexts);
        fActiveContexts.clear();
    }

    private void activateContextService() {
        if (fActiveContexts.isEmpty()) {
            IContextActivation activateContext = getContextService().activateContext(TMF_VIEW_UI_CONTEXT);
            Objects.requireNonNull(activateContext);
            fActiveContexts.add(activateContext);
        }
    }

    private static void coupleSelectViewer(TmfViewer tree, TmfXYChartViewer chart) {
        if (tree instanceof AbstractSelectTreeViewer2 && chart instanceof TmfFilteredXYChartViewer) {
            ILegendImageProvider2 legendImageProvider = new XYChartLegendImageProvider((TmfCommonXAxisChartViewer) chart);
            AbstractSelectTreeViewer2 selectTree = (AbstractSelectTreeViewer2) tree;
            selectTree.addTreeListener((TmfFilteredXYChartViewer) chart);
            selectTree.setLegendImageProvider(legendImageProvider);
            TriStateFilteredCheckboxTree checkboxTree = selectTree.getTriStateFilteredCheckboxTree();
            checkboxTree.addPreCheckStateListener(new ManyEntriesSelectedDialogPreCheckedListener(checkboxTree));
        }
    }

    /**
     * @param signal
     *            TmfSelectionRangeUpdatedSignal signal raised when time ranges
     *            are updated
     */
    @TmfSignalHandler
    public void selectionRangeUpdated(TmfSelectionRangeUpdatedSignal signal) {
        try (ScopeLog sl = new ScopeLog(LOGGER, Level.FINE, "MultiDensityView::SelectionRangeUpdated")) { //$NON-NLS-1$
            Object source = signal.getSource();
            if (source == getChartViewerA()) {
                fStartTimeA = TmfTimestamp.fromNanos(getChartViewerA().getSelectionBeginTime());
                fEndTimeA = TmfTimestamp.fromNanos(getChartViewerA().getSelectionEndTime());
            } else if (source == getChartViewerB()) {
                fStartTimeB = TmfTimestamp.fromNanos(getChartViewerB().getSelectionBeginTime());
                fEndTimeB = TmfTimestamp.fromNanos(getChartViewerB().getSelectionEndTime());
            }
            TmfComparisonFilteringUpdatedSignal rangUpdateSignal = new TmfComparisonFilteringUpdatedSignal(this, fStartTimeA, fEndTimeA, fStartTimeB, fEndTimeB, null, null, null);
            TmfSignalManager.dispatchSignal(rangUpdateSignal);

            buildDifferetialFlameGraph();
        }
    }

    /**
     * @return fStatistic (Duration or Selftime) which will be represented in
     *         the flame graph
     */
    public String getStatisticType() {
        return fStatistic;

    }

    private Action getAggregateByAction() {
        Action configureStatisticAction = fConfigureStatisticAction;
        if (configureStatisticAction == null) {
            configureStatisticAction = new Action(Messages.FlameGraphView_GroupByName, IAction.AS_DROP_DOWN_MENU) {
            };
            fConfigureStatisticAction = configureStatisticAction;
            configureStatisticAction.setToolTipText(Messages.FlameGraphView_StatisticTooltip);
            configureStatisticAction.setImageDescriptor(STATISTIC_BY_ICON);
            configureStatisticAction.setMenuCreator(new IMenuCreator() {
                @Nullable
                Menu menu = null;

                @Override
                public void dispose() {
                    if (menu != null) {
                        menu.dispose();
                        menu = null;
                    }
                }

                @Override
                public @Nullable Menu getMenu(@Nullable Control parent) {
                    if (menu != null) {
                        menu.dispose();
                    }
                    menu = new Menu(parent);
                    Action statisticActionDur = createStatisticAction(Objects.requireNonNull(Messages.AbstractMultipleDensityView_Duration));
                    new ActionContributionItem(statisticActionDur).fill(menu, -1);

                    Action statisticActionSelf = createStatisticAction(Objects.requireNonNull(Messages.AbstractMultipleDensityView_SelfTime));
                    new ActionContributionItem(statisticActionSelf).fill(menu, -1);
                    return menu;
                }

                @Override
                public @Nullable Menu getMenu(@Nullable Menu parent) {
                    return null;
                }
            });
        }
        Action configureStatisticAction3 = Objects.requireNonNull(fConfigureStatisticAction);
        return configureStatisticAction3;
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        IToolBarManager toolBarManager = Objects.requireNonNull(bars.getToolBarManager());
        fillLocalToolBar(toolBarManager);
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(getAggregateByAction());
    }

    /**
     * @param signal
     *            the TmfCheckboxChangedSignal signal
     */
    @TmfSignalHandler
    public void CheckBoxUpdated(TmfCheckboxChangedSignal signal) {
        try (ScopeLog sl = new ScopeLog(LOGGER, Level.FINE, "MultiDensityView::CheckBoxUpdated")) {//$NON-NLS-1$
            TmfComparisonFilteringUpdatedSignal rangUpdateSignal = null;

            if (signal.getSource() == getChartViewerA()) {
                for (String name : signal.getTraceList()) {
                    if (fTraceListA.contains(name)) {
                        fTraceListA.remove(name);
                    } else {
                        fTraceListA.add(name);
                    }
                }
                rangUpdateSignal = new TmfComparisonFilteringUpdatedSignal(this, null, fTraceListA, null);
                TmfSignalManager.dispatchSignal(rangUpdateSignal);

            }

            if (signal.getSource() == getChartViewerB()) {
                for (String name : signal.getTraceList()) {
                    if (fTraceListB.contains(name)) {
                        fTraceListB.remove(name);
                    } else {
                        fTraceListB.add(name);
                    }
                }
                rangUpdateSignal = new TmfComparisonFilteringUpdatedSignal(this, null, null, fTraceListB);
                TmfSignalManager.dispatchSignal(rangUpdateSignal);

            }
            buildDifferetialFlameGraph();
        }
    }

    @SuppressWarnings("null")
    @Override
    public void handleCheckStateChangedEvent(@Nullable Collection<ITmfTreeViewerEntry> entries) {
        // do nothing
    }

    @Override
    public void dispose() {
        super.dispose();
        TmfSignalManager.deregister(this);

        getChartViewerA().dispose();
        getTmfViewerA().dispose();
        getChartViewerB().dispose();
        getTmfViewerB().dispose();

        if (fXYViewerContainerA != null) {
            fXYViewerContainerA.dispose();
        }
        if (fXYViewerContainerB != null) {
            fXYViewerContainerB.dispose();
        }

    }

    private TmfXYChartViewer getChartViewerA() {
        Objects.requireNonNull(fChartViewerA);
        return fChartViewerA;
    }

    private void setChartViewerA(@Nullable TmfXYChartViewer chartViewerA) {
        fChartViewerA = chartViewerA;
    }

    private TmfXYChartViewer getChartViewerB() {
        Objects.requireNonNull(fChartViewerB);
        return fChartViewerB;
    }

    private void setChartViewerB(@Nullable TmfXYChartViewer chartViewerB) {
        fChartViewerB = chartViewerB;
    }

    private SashForm getSashFormLeftChildA() {
        Objects.requireNonNull(fSashFormLeftChildA);
        return fSashFormLeftChildA;
    }

    private void setSashFormLeftChildA(@Nullable SashForm sashFormLeftChildA) {
        fSashFormLeftChildA = sashFormLeftChildA;
    }

    private SashForm getSashFormLeftChildB() {
        Objects.requireNonNull(fSashFormLeftChildB);
        return fSashFormLeftChildB;
    }

    private void setSashFormLeftChildB(@Nullable SashForm sashFormLeftChildB) {
        fSashFormLeftChildB = sashFormLeftChildB;
    }

    private TmfViewer getTmfViewerA() {
        Objects.requireNonNull(fTmfViewerA);
        return fTmfViewerA;
    }

    private void setTmfViewerA(@Nullable TmfViewer tmfViewerA) {
        fTmfViewerA = tmfViewerA;
    }

    private TmfViewer getTmfViewerB() {
        Objects.requireNonNull(fTmfViewerB);
        return fTmfViewerB;
    }

    private void setTmfViewerB(@Nullable TmfViewer tmfViewerB) {
        fTmfViewerB = tmfViewerB;
    }

    private IContextService getContextService() {
        Objects.requireNonNull(fContextService);
        return fContextService;
    }

    private void setContextService(@Nullable IContextService contextService) {
        fContextService = contextService;
    }

}