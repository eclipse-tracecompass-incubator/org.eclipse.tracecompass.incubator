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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.ExpandListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.ScopeLog;
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
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestampFormat;
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
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;

/**
 * ExecutionComparisonView allows to compare two groups of traces (parts of traces)
 *
 * @author Fateme Faraji Daneshgar
 */
@SuppressWarnings("restriction")
public class ExecutionComparisonView extends DifferentialFlameGraphView implements ICheckboxTreeViewerListener {

    /**
     * the id of the view
     */
    public static final String id = "org.eclipse.tracecompass.incubator.internal.entexecutioncomparison.ui.execComparison"; //$NON-NLS-1$
    /**
     * Default weights for organizing the view
     */
    private static final int[] DEFAULT_WEIGHTS_ShowQuery = new int[] { 450, 290, 260 };
    private static final int[] DEFAULT_WEIGHTS_HideQuery = new int[] { 450, 100, 450 };
    private static final int[] DEFAULT_WEIGHTS_FILTERING_H = new int[] { 3, 9 };
    private static final int[] DEFAULT_WEIGHTS_TimeInterval = new int[] { 240, 380, 380 };

    /** A composite that allows us to add margins for part A */
    private @Nullable TmfXYChartViewer fChartViewerA;
    private @Nullable SashForm fXYViewerContainerA;
    private @Nullable TmfViewer fTmfViewerA;
    private @Nullable SashForm fSashFormLeftChildA;
    private @Nullable SashForm fSashFormLeftChildB;
    private @Nullable IContextService fContextService;
    private @Nullable Action fConfigureStatisticAction;
    private List<IContextActivation> fActiveContexts = new ArrayList<>();

    /** A composite that allows us to add margins for part B */
    private @Nullable TmfXYChartViewer fChartViewerB;
    private @Nullable SashForm fXYViewerContainerB;
    private @Nullable TmfViewer fTmfViewerB;
    private List<String> fTraceListA = new ArrayList<>();
    private List<String> fTraceListB = new ArrayList<>();
    private String fTraceStr = "Trace(s): ";//$NON-NLS-1$
    private String fStatisticStr = "Statistic: ";//$NON-NLS-1$

    /**
     * the title of the view
     */
    @SuppressWarnings("null")
    public static final String VIEW_TITLE = Messages.MultipleDensityView_title;
    private ITmfTimestamp fStartTimeA = TmfTimestamp.BIG_BANG;
    private ITmfTimestamp fStartTimeB = TmfTimestamp.BIG_BANG;
    private ITmfTimestamp fEndTimeA = TmfTimestamp.BIG_CRUNCH;
    private ITmfTimestamp fEndTimeB = TmfTimestamp.BIG_CRUNCH;
    private String fStatistic = "duration"; //$NON-NLS-1$
    private JFormattedTextField ftextAFrom = new JFormattedTextField();
    private JFormattedTextField ftextBFrom = new JFormattedTextField();
    private JFormattedTextField ftextATo = new JFormattedTextField();
    private JFormattedTextField ftextBTo = new JFormattedTextField();
    private @Nullable Text ftextQuery;
    private TmfTimestampFormat fFormat = new TmfTimestampFormat("yyyy-MM-dd HH:mm:ss.SSS.SSS.SSS"); //$NON-NLS-1$
    private @Nullable Listener fSashDragListener;
    private SashForm fsashForm;
    private static final String TMF_VIEW_UI_CONTEXT = "org.eclipse.tracecompass.tmf.ui.view.context"; //$NON-NLS-1$

    /**
     * Constructs two density charts for selecting the desired traces and time
     * ranges in order to comparison
     */
    public ExecutionComparisonView() {
        super();
    }

    @Override
    public void createPartControl(@Nullable Composite parent) {
        TmfSignalManager.register(this);

        final SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
        fsashForm = sashForm;
        sashForm.setLayout(new GridLayout(1, false));
        sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        //// Main organization of the view. there are three main parts in the
        //// view:filtering, query and differential flame graph
        Composite sashFormFiltering = new Composite(sashForm, SWT.HORIZONTAL);
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = layout.marginWidth = 0;
        sashFormFiltering.setLayout(layout);
        sashFormFiltering.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        ////GroupA
        Group groupA = new Group(sashFormFiltering, SWT.NULL);
        groupA.setText(Messages.MultipleDensityView_GroupA);
        GridLayout gridLayoutG = new GridLayout();
        gridLayoutG.numColumns = 1;
        groupA.setLayout(gridLayoutG);
        GridData gridDataG = new GridData(GridData.FILL_BOTH);
        gridDataG.horizontalSpan = 1;
        groupA.setLayoutData(gridDataG);


        SashForm densityA = new SashForm(groupA, SWT.NULL);
        GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
        data.heightHint = 200;
        densityA.setLayoutData(data);

        SashForm timeInputA = new SashForm(groupA, SWT.NULL);
        timeInputA.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        ////GroupB
        Group groupB = new Group(sashFormFiltering, SWT.NULL);
        groupB.setText(Messages.MultipleDensityView_GroupB);
        groupB.setLayout(gridLayoutG);
        groupB.setLayoutData(gridDataG);

        SashForm densityB = new SashForm(groupB, SWT.NULL);
        densityB.setLayoutData(data);

        SashForm timeInputB = new SashForm(groupB, SWT.NULL);
        timeInputB.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        //// Group A time Intervals
        Composite timelableA = new Composite(timeInputA, SWT.NONE);
        Composite timelableAFrom = new Composite(timeInputA, SWT.EMBEDDED);
        Composite timelableATo = new Composite(timeInputA, SWT.EMBEDDED);

        timeInputA.setWeights(DEFAULT_WEIGHTS_TimeInterval);

        RowLayout rowLayout = new RowLayout();
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;

        timelableA.setLayout(gridLayout);
        timelableAFrom.setLayout(rowLayout);
        timelableATo.setLayout(rowLayout);

        // ButtonA
        Button resetButtonA = new Button(timelableA, SWT.PUSH);
        resetButtonA.setText("Reset Time IntervalA"); //$NON-NLS-1$
        resetButtonA.addListener(SWT.Selection, new Listener()
        {
            @Override
            public void handleEvent(@Nullable Event event) {
                ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();

                if (trace != null) {
                    //Reset tree viewer checked items. all items should be checked
                    List<ITmfTreeViewerEntry> TreeCheckedElements = ((MultipleEventDensityViewer) getChartViewerA()).getWholeCheckedItems();
                    setCheckedElements(getChartViewerA(),getTmfViewerA(),TreeCheckedElements,false);

                    //Reset start time and end time and relating objects
                    fStartTimeA = trace.getStartTime();
                    fEndTimeA = trace.getEndTime();
                    ftextAFrom.setText(fStartTimeA.toString(fFormat));
                    ftextATo.setText(fEndTimeA.toString(fFormat));
                    if(ftextQuery!=null) {
                        ftextQuery.setText(makeQuery());
                    }

                    //dispatch signal to rebuild differential flame graph
                    TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(getChartViewerA(), fStartTimeA, fEndTimeA, getTrace()));
                    //Reset the selection blue lines in density chart viewer
                    getChartViewerA().refreshMouseSelectionProvider();
                }
            }
        });

        /// LableAfrom
        java.awt.Frame frameAFrom = SWT_AWT.new_Frame(timelableAFrom);
        java.awt.Panel panelAFrom = new java.awt.Panel();
        frameAFrom.add(panelAFrom);

        JLabel labelAFrom = new JLabel();
        labelAFrom.setText(Messages.MultipleDensityView_From);

        JFormattedTextField textAFrom = new JFormattedTextField(fFormat);
        textAFrom.addFocusListener(new java.awt.event.FocusListener() {
            public String oldVal = ""; //$NON-NLS-1$

            @Override
            public void focusGained( java.awt.event.@Nullable FocusEvent e) {
                String aFrom = textAFrom.getText();
                if (aFrom!=null) {
                    oldVal = aFrom;
                }
            }

            @Override
            public void focusLost(java.awt.event.@Nullable FocusEvent e) {
                if (!oldVal.equals(textAFrom.getText())) {
                    long newTime = 0;
                    try {
                        newTime = fFormat.parseValue(textAFrom.getText());
                        ITmfTimestamp fromTime = TmfTimestamp.fromNanos(newTime);
                        fStartTimeA = fromTime;
                        textAFrom.setText(fStartTimeA.toString(fFormat));

                        TmfSelectionRangeUpdatedSignal signal = new TmfSelectionRangeUpdatedSignal(getChartViewerA(), fStartTimeA, fEndTimeA, getTrace());
                        TmfSignalManager.dispatchSignal(signal);

                    } catch (ParseException e1) {
                        textAFrom.setText(oldVal);
                        e1.printStackTrace();
                    }
                }
            }
        });
        panelAFrom.add(labelAFrom);
        panelAFrom.add(textAFrom);
        ftextAFrom = textAFrom;

        //// LableATo
        java.awt.Frame frameATo = SWT_AWT.new_Frame(timelableATo);
        java.awt.Panel panelATo = new java.awt.Panel();
        frameATo.add(panelATo);

        JLabel labelATo = new JLabel();
        labelATo.setText(Messages.MultipleDensityView_To);
        JFormattedTextField textATo = new JFormattedTextField(fFormat);
        textATo.addFocusListener(new java.awt.event.FocusListener() {
            public String oldVal = ""; //$NON-NLS-1$

            @Override
            public void focusGained(java.awt.event.@Nullable FocusEvent e) {
                String aTo = textATo.getText();
                if (aTo!=null) {
                    oldVal = aTo;
                }
            }

            @Override
            public void focusLost( java.awt.event.@Nullable FocusEvent e) {
                if (!oldVal.equals(textATo.getText())) {
                    long newTime = 0;
                    try {
                        newTime = fFormat.parseValue(textATo.getText());
                        ITmfTimestamp fromTime = TmfTimestamp.fromNanos(newTime);
                        fEndTimeA = fromTime;
                        textATo.setText(fEndTimeA.toString(fFormat));

                        TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(getChartViewerA(), fStartTimeA, fEndTimeA, getTrace()));

                    } catch (ParseException e1) {
                        textATo.setText(oldVal);
                        e1.printStackTrace();
                    }
                    ITmfTimestamp fromTime = TmfTimestamp.fromNanos(newTime);
                    fEndTimeA = fromTime;

                    TmfSelectionRangeUpdatedSignal signal = new TmfSelectionRangeUpdatedSignal(this, fStartTimeA, fEndTimeA, getTrace());
                    broadcast(signal);
                }
            }
        });

        panelATo.add(labelATo);
        panelATo.add(textATo);
        ftextATo = textATo;

        //// Group B time Intervals
        Composite timelableB = new Composite(timeInputB, SWT.FILL);
        Composite timelableBFrom = new Composite(timeInputB, SWT.EMBEDDED);
        Composite timelableBTo = new Composite(timeInputB, SWT.EMBEDDED);

        timeInputB.setWeights(DEFAULT_WEIGHTS_TimeInterval);

        timelableB.setLayout(gridLayout);
        timelableBFrom.setLayout(rowLayout);
        timelableBTo.setLayout(rowLayout);

        // Button B
        Button resetButtonB = new Button(timelableB, SWT.PUSH);
        resetButtonB.setText("Reset Time IntervalB"); //$NON-NLS-1$
        resetButtonB.addListener(SWT.Selection, new Listener()
        {
            @Override
            public void handleEvent(@Nullable Event event) {
                ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();

                if (trace != null) {
                    //Reset tree viewer checked items. all items should be checked
                    List<ITmfTreeViewerEntry> TreeCheckedElements = ((MultipleEventDensityViewer) getChartViewerB()).getWholeCheckedItems();
                    setCheckedElements(getChartViewerB(),getTmfViewerB(),TreeCheckedElements,false);

                    //Reset start time and end time and relating objects
                    fStartTimeB = trace.getStartTime();
                    fEndTimeB = trace.getEndTime();
                    ftextBFrom.setText(fStartTimeB.toString(fFormat));
                    ftextBTo.setText(fEndTimeB.toString(fFormat));
                    if(ftextQuery!=null) {
                        ftextQuery.setText(makeQuery());
                    }

                    //dispatch signal to rebuild differential flame graph
                    TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(getChartViewerB(), fStartTimeB, fEndTimeB, getTrace()));
                    //Reset the selection blue lines in density chart viewer
                    getChartViewerB().refreshMouseSelectionProvider();
                }
            }
        });


        /// LableBFrom
        java.awt.Frame frameBFrom = SWT_AWT.new_Frame(timelableBFrom);
        java.awt.Panel panelBFrom = new java.awt.Panel();
        frameBFrom.add(panelBFrom);

        JLabel labelBFrom = new JLabel();
        labelBFrom.setText(Messages.MultipleDensityView_From);
        JFormattedTextField textBFrom = new JFormattedTextField(fFormat);
        textBFrom.addFocusListener(new java.awt.event.FocusListener() {
            public String oldVal=""; //$NON-NLS-1$

            @Override
            public void focusGained(java.awt.event.@Nullable FocusEvent e) {
                String bFrom = textBFrom.getText();
                if (bFrom!=null) {
                    oldVal = bFrom;
                }
            }

            @Override
            public void focusLost(java.awt.event.@Nullable FocusEvent e) {
                if (!oldVal.equals(textBFrom.getText())) {
                    long newTime = 0;
                    try {
                        newTime = fFormat.parseValue(textBFrom.getText());
                        ITmfTimestamp fromTime = TmfTimestamp.fromNanos(newTime);
                        fStartTimeB = fromTime;
                        textBFrom.setText(fStartTimeB.toString(fFormat));
                        TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(getChartViewerB(), fStartTimeB, fEndTimeB, getTrace()));
                    } catch (ParseException e1) {
                        textBFrom.setText(oldVal);
                        e1.printStackTrace();
                    }
                    ITmfTimestamp fromTime = TmfTimestamp.fromNanos(newTime);
                    fStartTimeB = fromTime;

                    TmfSelectionRangeUpdatedSignal signal = new TmfSelectionRangeUpdatedSignal(this, fStartTimeA, fEndTimeA, getTrace());
                    broadcast(signal);

                }
            }
        });

        panelBFrom.add(labelBFrom);
        panelBFrom.add(textBFrom);
        ftextBFrom = textBFrom;

        //// LableBTo
        java.awt.Frame frameBTo = SWT_AWT.new_Frame(timelableBTo);
        java.awt.Panel panelBTo = new java.awt.Panel();
        frameBTo.add(panelBTo);

        JLabel labelBTo = new JLabel();
        labelBTo.setText(Messages.MultipleDensityView_To);
        JFormattedTextField textBTo = new JFormattedTextField(fFormat);
        textBTo.addFocusListener(new java.awt.event.FocusListener() {
            public @Nullable String oldVal = null;

            @SuppressWarnings("null")
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                oldVal = textBTo.getText();
            }

            @SuppressWarnings("null")
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (!oldVal.equals(textBTo.getText())) {
                    long newTime = 0;
                    try {
                        newTime = fFormat.parseValue(textBTo.getText());
                        ITmfTimestamp fromTime = TmfTimestamp.fromNanos(newTime);
                        fEndTimeB = fromTime;
                        textBTo.setText(fEndTimeB.toString(fFormat));
                        TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(getChartViewerB(), fStartTimeB, fEndTimeB, getTrace()));
                    } catch (ParseException e1) {
                        textBTo.setText(oldVal);
                        e1.printStackTrace();
                    }
                    ITmfTimestamp fromTime = TmfTimestamp.fromNanos(newTime);
                    fEndTimeB = fromTime;

                    TmfSelectionRangeUpdatedSignal signal = new TmfSelectionRangeUpdatedSignal(this, fStartTimeA, fEndTimeA, getTrace());
                    broadcast(signal);
                }
            }
        });

        panelBTo.add(labelBTo);
        panelBTo.add(textBTo);
        ftextBTo = textBTo;

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
                        Objects.requireNonNull(getSashFormLeftChildB());

                        fSashDragListener = event -> TmfSignalManager.dispatchSignal(new TmfTimeViewAlignmentSignal(getSashFormLeftChildB(), getTimeViewAlignmentInfo(chartViewerB, getSashFormLeftChildB())));
                        control.removePaintListener(this);
                        control.addListener(SWT.Selection, fSashDragListener);
                        // There should be only one sash
                        break;
                    }
                }
            }
        });

        chartViewerB.setStatusLineManager(statusLineManager);
        coupleSelectViewer(getTmfViewerB(), chartViewerB);
        ((AbstractSelectTreeViewer2) getTmfViewerB()).addTreeListener(this);

        IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();

        IAction AggregatedAction = fConfigureStatisticAction;
        if (AggregatedAction == null) {
            AggregatedAction = getAggregateByAction();
            fConfigureStatisticAction = (Action) AggregatedAction;
        }
        menuManager.add(new Separator());
        menuManager.add(AggregatedAction);
        menuManager.add(new Separator());

        densityA.setWeights(DEFAULT_WEIGHTS_FILTERING_H);
        densityB.setWeights(DEFAULT_WEIGHTS_FILTERING_H);

        Group groupQuery = new Group(sashForm, SWT.NULL);
        groupQuery.setText(Messages.MultipleDensityView_QueryGroup);
        gridLayoutG = new GridLayout();
        gridLayoutG.numColumns = 1;
        groupQuery.setLayout(gridLayoutG);
        gridDataG = new GridData(GridData.FILL_BOTH);
        gridDataG.horizontalSpan = 1;
        groupQuery.setLayoutData(gridDataG);

        ///// Organizing sashFormQuery
        ExpandBar bar = new ExpandBar(groupQuery, SWT.NONE);
        bar.setLayout(new GridLayout(1, false));
        GridData data2 = new GridData();
        data2.verticalAlignment = SWT.FILL;
        data2.horizontalAlignment = SWT.FILL;
        data2.grabExcessHorizontalSpace = true;
        data2.grabExcessVerticalSpace = true;
        data2.heightHint = 75;
        data2.widthHint = 100;
        bar.setLayoutData(data2);


        Composite queryText = new Composite(bar, SWT.NONE);
        queryText.setLayout(new GridLayout(1, false));
        queryText.setLayoutData(new GridData(SWT.FILL, SWT.FILL,true,true));

        // Text
        Text text = new Text(queryText, SWT.MULTI | SWT.BORDER );
        data.verticalAlignment = SWT.FILL;
        data.horizontalAlignment = SWT.FILL;
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        data.heightHint = 75;
        data.widthHint = 100;
        text.setLayoutData(data);
        ftextQuery = text;
        text.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(@Nullable FocusEvent e) {
                // TODO Auto-generated method stub
            }

            @Override
            public void focusLost(@Nullable FocusEvent e) {
                String query = ftextQuery.getText();
                if (query==null)
                {
                    return;
                }
                boolean parsed = parseQuery(query);
                if (parsed) {
                    ///updating blue lines in density chats
                    getChartViewerA().selectionRangeUpdated(new TmfSelectionRangeUpdatedSignal(getChartViewerA(), fStartTimeA, fEndTimeA, getTrace()));
                    getChartViewerB().selectionRangeUpdated(new TmfSelectionRangeUpdatedSignal(getChartViewerB(), fStartTimeB, fEndTimeB, getTrace()));
                    getChartViewerA().refreshMouseSelectionProvider();
                    getChartViewerB().refreshMouseSelectionProvider();

                    //updates checked elements in treeviewers
                    //treeViewerA
                    List<ITmfTreeViewerEntry> TreeWholeElements = ((MultipleEventDensityViewer) getChartViewerA()).getWholeCheckedItems();
                    List<ITmfTreeViewerEntry> TreeCheckedElements = new ArrayList<>();

                    for (ITmfTreeViewerEntry trace:TreeWholeElements) {
                        if(fTraceListA.contains(trace.getName())) {
                            TreeCheckedElements.add(trace);
                            TreeCheckedElements.addAll(trace.getChildren());
                        }
                    }

                    setCheckedElements(getChartViewerA(),getTmfViewerA(),TreeCheckedElements,true);
                    //TreeVierB
                    TreeWholeElements = ((MultipleEventDensityViewer) getChartViewerB()).getWholeCheckedItems();
                    TreeCheckedElements = new ArrayList<>();

                    for (ITmfTreeViewerEntry trace:TreeWholeElements) {
                        if(fTraceListB.contains(trace.getName())) {
                            TreeCheckedElements.add(trace);
                            TreeCheckedElements.addAll(trace.getChildren());
                        }
                    }

                    setCheckedElements(getChartViewerB(),getTmfViewerB(),TreeCheckedElements,true);

                    TmfComparisonFilteringUpdatedSignal rangUpdateSignal = new TmfComparisonFilteringUpdatedSignal(this, fStartTimeA, fEndTimeA, fStartTimeB, fEndTimeB, fStatistic, fTraceListA, fTraceListB);
                    TmfSignalManager.dispatchSignal(rangUpdateSignal);
                    buildDifferetialFlameGraph();
                }
            }
        });

        ExpandItem item0 = new ExpandItem(bar, SWT.NONE,0);
        item0.setText(Messages.MultipleDensityView_QueryExpandable);
        item0.setHeight(150);

        item0.setControl(queryText);
        item0.setExpanded(false);

        bar.setSpacing(5);
        bar.addExpandListener(new ExpandListener() {

            @Override
            public void itemExpanded(@Nullable ExpandEvent e) {
                Display.getCurrent().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        queryText.pack(true);
                        sashForm.setWeights(DEFAULT_WEIGHTS_ShowQuery);

                    }
                });
            }

            @Override
            public void itemCollapsed(@Nullable ExpandEvent e) {
                Display.getCurrent().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        queryText.pack(true);
                        sashForm.setWeights(DEFAULT_WEIGHTS_HideQuery);

                    }
                });
            }
        });

        super.createPartControl(sashForm);
        sashForm.setWeights(DEFAULT_WEIGHTS_HideQuery);

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

        ftextAFrom.setText(fStartTimeA.toString(fFormat));
        ftextBFrom.setText(fStartTimeB.toString(fFormat));
        ftextATo.setText(fEndTimeA.toString(fFormat));
        ftextBTo.setText(fEndTimeB.toString(fFormat));

        TmfComparisonFilteringUpdatedSignal rangUpdateSignal = new TmfComparisonFilteringUpdatedSignal(this, fStartTimeA, fEndTimeA, fStartTimeB, fEndTimeB, fStatistic, fTraceListA, fTraceListB);
        TmfSignalManager.dispatchSignal(rangUpdateSignal);
        if(ftextQuery!=null) {
            ftextQuery.setText(makeQuery());
        }
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
                if (ftextQuery!=null) {
                    ftextQuery.setText(makeQuery());
                }
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
     * Set checked for the elements in TreeCheckedElements
     * @param TreeCheckedElements
     * the elements in tree that should be checked
     */
    public void setCheckedElements(TmfXYChartViewer chart,TmfViewer tree,List<ITmfTreeViewerEntry> TreeCheckedElements, Boolean queryUpdate) {
    if (queryUpdate) {
        ((MultipleEventDensityViewer) chart).UpdateCheckStateChangedEvent(TreeCheckedElements);
    }
    else {
        ((MultipleEventDensityViewer) chart).handleCheckStateChangedEvent(TreeCheckedElements);
    }
    Object[] TreeCheckedElementsObj = new Object[TreeCheckedElements.size()];

    TreeCheckedElements.toArray(TreeCheckedElementsObj);
    ((AbstractSelectTreeViewer2) tree).getTriStateFilteredCheckboxTree().setCheckedElements(TreeCheckedElementsObj);
    }

    /**
     * Reset the start and end times.
     *
     * @param chart
     *            determines which chart to reset start and end times
     */
    public void resetStartFinishTime(boolean notify, TmfXYChartViewer chart) {
        TmfWindowRangeUpdatedSignal signal = new TmfWindowRangeUpdatedSignal(this, TmfTimeRange.ETERNITY, getTrace());
        if (notify) {
            broadcast(signal);

        } else {
            chart.windowRangeUpdated(signal);
            TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(chart, TmfTimestamp.fromNanos(chart.getWindowStartTime()), TmfTimestamp.fromNanos(chart.getWindowEndTime())));
            chart.refreshMouseSelectionProvider();

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
                fStartTimeA = signal.getBeginTime();
                fEndTimeA = signal.getEndTime();
                ftextAFrom.setText(fStartTimeA.toString(fFormat));
                ftextATo.setText(fEndTimeA.toString(fFormat));

            } else if (source == getChartViewerB()) {
                fStartTimeB = signal.getBeginTime();
                fEndTimeB = signal.getEndTime();
                ftextBFrom.setText(fStartTimeB.toString(fFormat));
                ftextBTo.setText(fEndTimeB.toString(fFormat));
            }
            TmfComparisonFilteringUpdatedSignal rangUpdateSignal = new TmfComparisonFilteringUpdatedSignal(this, fStartTimeA, fEndTimeA, fStartTimeB, fEndTimeB, null, null, null);
            TmfSignalManager.dispatchSignal(rangUpdateSignal);
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    if(ftextQuery!=null) {
                        ftextQuery.setText(makeQuery());
                    }
                }
            });
            getChartViewerA().refreshMouseSelectionProvider();
            getChartViewerB().refreshMouseSelectionProvider();
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
        Action configureStatisticAction = new Action(Messages.FlameGraphView_GroupByName, IAction.AS_DROP_DOWN_MENU) {
        };
        configureStatisticAction.setToolTipText(Messages.FlameGraphView_StatisticTooltip);
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
                return null;
            }

            @Override
            public @Nullable Menu getMenu(@Nullable Menu parent) {
                menu = new Menu(parent);

                Action statisticActionDur = createStatisticAction(Objects.requireNonNull(Messages.MultipleDensityView_Duration));
                new ActionContributionItem(statisticActionDur).fill(menu, -1);

                Action statisticActionSelf = createStatisticAction(Objects.requireNonNull(Messages.MultipleDensityView_SelfTime));
                new ActionContributionItem(statisticActionSelf).fill(menu, -1);
                return menu;

            }
        });

        Action configureStatisticAction1 = Objects.requireNonNull(configureStatisticAction);
        return configureStatisticAction1;
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
                fTraceListA.clear();
                for (String name : signal.getTraceList()) {
                    fTraceListA.add(name);
                }

                rangUpdateSignal = new TmfComparisonFilteringUpdatedSignal(this, null, fTraceListA, null);
                TmfSignalManager.dispatchSignal(rangUpdateSignal);
            }

            if (signal.getSource() == getChartViewerB()) {
                fTraceListB.clear();
                for (String name : signal.getTraceList()) {
                    fTraceListB.add(name);
                }

                rangUpdateSignal = new TmfComparisonFilteringUpdatedSignal(this, null, null, fTraceListB);
                TmfSignalManager.dispatchSignal(rangUpdateSignal);

            }
            if(ftextQuery!=null) {
                ftextQuery.setText(makeQuery());
            }
            buildDifferetialFlameGraph();
        }

    }

    @Override
    public void handleCheckStateChangedEvent(@SuppressWarnings("null") @Nullable Collection<ITmfTreeViewerEntry> entries) {
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
        if (fContextService!=null) {
            fContextService.dispose();
        }
        if (fSashFormLeftChildA!=null) {
            fSashFormLeftChildA.dispose();
        }
        if (fSashFormLeftChildB!=null) {
            fSashFormLeftChildB.dispose();
        }
        if (fsashForm!=null) {
            fsashForm.dispose();
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

    @SuppressWarnings("null")
    private String makeQuery() {
        String query = ""; //$NON-NLS-1$
        /// Query PartA
        query = query.concat(fTraceStr);
        for (String trace : fTraceListA) {
            if (!trace.equals("Total")) { //$NON-NLS-1$
                query = query.concat(trace);
                query = query.concat(","); //$NON-NLS-1$
            }
        }
        query = query.concat(System.lineSeparator());

        query = query.concat(Messages.MultipleDensityView_From);
        query = query.concat(fStartTimeA.toString(fFormat));
        query = query.concat(System.lineSeparator());

        query = query.concat(Messages.MultipleDensityView_To);
        query = query.concat(fEndTimeA.toString(fFormat));
        query = query.concat(System.lineSeparator());

        query = query.concat(Messages.MultipleDensityView_QueryCompare);
        query = query.concat(System.lineSeparator());

        /// Query PartB
        query = query.concat(fTraceStr);
        for (String trace : fTraceListB) {
            if (!trace.equals("Total")) { //$NON-NLS-1$
                query = query.concat(trace);
                query = query.concat(","); //$NON-NLS-1$
            }
        }
        query = query.concat(System.lineSeparator());

        query = query.concat(Messages.MultipleDensityView_From);
        query = query.concat(fStartTimeB.toString(fFormat));
        query = query.concat(System.lineSeparator());

        query = query.concat(Messages.MultipleDensityView_To);
        query = query.concat(fEndTimeB.toString(fFormat));
        query = query.concat(System.lineSeparator());

        //// Query Statistic Part
        query = query.concat(fStatisticStr);
        query = query.concat(fStatistic);

        return query;
    }

    @SuppressWarnings("null")
    boolean parseQuery(String query) {
        try {
            String[] parts = query.split(System.lineSeparator());
            // Times

            if (parts[1].indexOf(Messages.MultipleDensityView_From) == -1) {
                return false;
            }
            String fromStrA = parts[1].substring(parts[1].indexOf(Messages.MultipleDensityView_From) + Messages.MultipleDensityView_From.length(), parts[1].length());
            fStartTimeA = TmfTimestamp.fromNanos(fFormat.parseValue(fromStrA));

            if (parts[2].indexOf(Messages.MultipleDensityView_To) == -1) {
                return false;
            }
            String toStrA = parts[2].substring(parts[2].indexOf(Messages.MultipleDensityView_To) + Messages.MultipleDensityView_To.length(), parts[2].length());
            fEndTimeA = TmfTimestamp.fromNanos(fFormat.parseValue(toStrA));

            if (parts[5].indexOf(Messages.MultipleDensityView_From) == -1) {
                return false;
            }
            String fromStrB = parts[5].substring(parts[5].indexOf(Messages.MultipleDensityView_From) + Messages.MultipleDensityView_From.length(), parts[5].length());
            fStartTimeB = TmfTimestamp.fromNanos(fFormat.parseValue(fromStrB));

            if (parts[6].indexOf(Messages.MultipleDensityView_To) == -1) {
                return false;
            }
            String toStrB = parts[6].substring(parts[6].indexOf(Messages.MultipleDensityView_To) + Messages.MultipleDensityView_To.length(), parts[6].length());
            fEndTimeB = TmfTimestamp.fromNanos(fFormat.parseValue(toStrB));

            // traceListA
            fTraceListA.clear();

            if (parts[0].indexOf(fTraceStr) == -1) {
                return false;
            }
            String traceStrtA = parts[0].substring(parts[0].indexOf(fTraceStr) + fTraceStr.length(), parts[0].length());
            String[] traces = traceStrtA.split(","); //$NON-NLS-1$

            for (String trace : traces) {
                fTraceListA.add(trace);
            }

            // traceListB
            fTraceListB.clear();
            String traceStrtB = parts[4].substring(parts[4].indexOf(fTraceStr) + fTraceStr.length(), parts[4].length());
            String[] tracesB = traceStrtB.split(","); //$NON-NLS-1$

            for (String trace : tracesB) {
                fTraceListB.add(trace);
            }
            //// Statistic
            fStatistic = parts[7].substring(parts[7].indexOf(fStatisticStr) + fStatisticStr.length(), parts[7].length());

            //Set time range related objects
            ftextAFrom.setText(fStartTimeA.toString(fFormat));
            ftextATo.setText(fEndTimeA.toString(fFormat));

            ftextBFrom.setText(fStartTimeB.toString(fFormat));
            ftextBTo.setText(fEndTimeB.toString(fFormat));


        } catch (ParseException e) {
            // TODO Auto-generated catch block
            System.out.println("query format is incorrect " + e.toString()); //$NON-NLS-1$
            e.printStackTrace();
            return false;
        }
        return true;
    }
}