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
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridLayoutFactory;
//import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.ScopeLog;
//import org.eclipse.tracecompass.incubator.internal.callstack.ui.Activator;
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
    private static final int[] DEFAULT_WEIGHTS_ShowQuery = new int[] { 425, 150, 425 };
    private static final int[] DEFAULT_WEIGHTS_HideQery = new int[] { 425, 1, 574 };
    private static final int[] DEFAULT_WEIGHTS_FILTERING_VIEW = new int[] { 495, 10, 495 };
    private static final int[] DEFAULT_WEIGHTS_FILTERING_H = new int[] { 3, 9 };
    private static final int[] DEFAULT_WEIGHTS_LABELS = new int[] { 1, 8, 1 };
    private static final int[] DEFAULT_WEIGHTS_TimeInterval = new int[] { 250, 375, 375 };

    /**
     * Default zoom range
     *
     * @since 4.1
     */
    private static final String STATISTIC_ICON_PATH = "icons/etool16/group_by.gif"; //$NON-NLS-1$
    //@SuppressWarnings("null")
    //private static final ImageDescriptor STATISTIC_BY_ICON = Activator.getDefault().getImageDescripterFromPath(STATISTIC_ICON_PATH);

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
    private String fTraceStr = "Trace(s): ";//$NON-NLS-1$
    private String fFromStr = "From: ";//$NON-NLS-1$
    private String fToStr = "To: ";//$NON-NLS-1$
    private String fStatisticStr = "Statistic: ";//$NON-NLS-1$

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
    private JFormattedTextField ftextAFrom;
    private JFormattedTextField ftextBFrom;
    private JFormattedTextField ftextATo;
    private JFormattedTextField ftextBTo;
    private JTextArea ftextQuery;
    private TmfTimestampFormat fFormat = new TmfTimestampFormat("yyyy-MM-dd HH:mm:ss.SSS.SSS.SSS"); //$NON-NLS-1$
    private @Nullable Listener fSashDragListener;
    private static final String TMF_VIEW_UI_CONTEXT = "org.eclipse.tracecompass.tmf.ui.view.context"; //$NON-NLS-1$
    private IAction fAggregatedAction;


    // SashForm fsashForm;

    /**
     * Constructs a segment store density view
     */
    public MultipleDensityView() {
        super();
    }

    /**
      */
    @Override
    public void createPartControl(@Nullable Composite parent) {
        TmfSignalManager.register(this);

        final SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
        //// Main organization of the view
        SashForm sashFormFiltering = new SashForm(sashForm, SWT.HORIZONTAL);
        SashForm sashFormQuery = new SashForm(sashForm, SWT.HORIZONTAL);
        // SashForm sashFormDoneButton = new SashForm(sashForm, SWT.HORIZONTAL);

        ///// Organizing sashFormFiltering
        SashForm sashFormGroupA = new SashForm(sashFormFiltering, SWT.VERTICAL);

        SashForm distance = new SashForm(sashFormFiltering, SWT.NONE);
        distance.pack();

        SashForm sashFormGroupB = new SashForm(sashFormFiltering, SWT.VERTICAL);

        Text labelGroupA = new Text(sashFormGroupA, SWT.BORDER | SWT.CENTER);
        labelGroupA.setText(Messages.AbstractMultipleDensityView_GroupA);

        Text labelGroupB = new Text(sashFormGroupB, SWT.BORDER | SWT.CENTER);
        labelGroupB.setText(Messages.AbstractMultipleDensityView_GroupB);

        /////// density charts
        SashForm densityA = new SashForm(sashFormGroupA, SWT.HORIZONTAL);
        SashForm densityB = new SashForm(sashFormGroupB, SWT.HORIZONTAL);

        ///////// Time inputs
        SashForm timeInputA = new SashForm(sashFormGroupA, SWT.HORIZONTAL);
        SashForm timeInputB = new SashForm(sashFormGroupB, SWT.HORIZONTAL);

        //// Group A time Intervals
        Composite timelableA = new Composite(timeInputA, SWT.NONE);
        Composite timelableAFrom = new Composite(timeInputA, SWT.EMBEDDED);
        Composite timelableATo = new Composite(timeInputA, SWT.EMBEDDED);

        timeInputA.setWeights(DEFAULT_WEIGHTS_TimeInterval);

        RowLayout rowLayout = new RowLayout();

        FillLayout fillLayout = new FillLayout();
        fillLayout.type = SWT.VERTICAL;

        timelableA.setLayout(fillLayout);
        timelableAFrom.setLayout(rowLayout);
        timelableATo.setLayout(rowLayout);

        // LabelA
        Label labelA = new Label(timelableA, SWT.NONE);
        labelA.setText("Time IntervalA "); //$NON-NLS-1$

        Link link = new Link(timelableA, SWT.PUSH);
        link.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(@Nullable SelectionEvent e) {
                if (link.getText().equals("<a>Show Query</a>")) { //$NON-NLS-1$
                    sashForm.setWeights(DEFAULT_WEIGHTS_ShowQuery);
                    link.setText("<a>Hide Query</a>"); //$NON-NLS-1$

                } else {
                    sashForm.setWeights(DEFAULT_WEIGHTS_HideQery);
                    link.setText("<a>Show Query</a>"); //$NON-NLS-1$
                }
            }

            @Override
            public void widgetDefaultSelected(@Nullable SelectionEvent e) {
                // TODO Auto-generated method stub

            }
        });
        link.setText("<a>Show Query</a>"); //$NON-NLS-1$

        /// LableAfrom
        java.awt.Frame frameAFrom = SWT_AWT.new_Frame(timelableAFrom);
        java.awt.Panel panelAFrom = new java.awt.Panel();
        frameAFrom.add(panelAFrom);

        JLabel labelAFrom = new JLabel();
        labelAFrom.setText("From: "); //$NON-NLS-1$

        JFormattedTextField textAFrom = new JFormattedTextField(fFormat);
        textAFrom.addFocusListener(new java.awt.event.FocusListener() {
            public String oldVal = null;

            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                oldVal = textAFrom.getText();
                //((TmfHistogramTooltipProvider) getChartViewerA().fToolTipProvider).mouseHover2(55);

            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                // TODO Auto-generated method stub
                if (!oldVal.equals(textAFrom.getText())) {
                    long newTime = 0;
                    try {
                        newTime = fFormat.parseValue(textAFrom.getText());
                        ITmfTimestamp fromTime = TmfTimestamp.fromNanos(newTime);
                        fStartTimeA = fromTime;
                        textAFrom.setText(fStartTimeA.toString(fFormat));
                        getChartViewerA().getMouseSelectionProvider().refresh();
                        TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(this, fStartTimeA, fEndTimeA, getTrace()));

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
        labelATo.setText("To: "); //$NON-NLS-1$
        JFormattedTextField textATo = new JFormattedTextField(fFormat);
        textATo.addFocusListener(new java.awt.event.FocusListener() {
            public String oldVal = null;

            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                oldVal = textATo.getText();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (!oldVal.equals(textATo.getText())) {
                    long newTime = 0;
                    try {
                        newTime = fFormat.parseValue(textATo.getText());
                        ITmfTimestamp fromTime = TmfTimestamp.fromNanos(newTime);
                        fEndTimeA = fromTime;
                        textATo.setText(fEndTimeA.toString(fFormat));
                        getChartViewerA().getMouseSelectionProvider().refresh();
                        TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(this, fStartTimeA, fEndTimeA, getTrace()));
                    } catch (ParseException e1) {
                        textATo.setText(oldVal);
                        e1.printStackTrace();
                    }

                }
            }
        });

        panelATo.add(labelATo);
        panelATo.add(textATo);
        ftextATo = textATo;

        //// Group B time Intervals
        Composite timelableB = new Composite(timeInputB, SWT.NONE);
        Composite timelableBFrom = new Composite(timeInputB, SWT.EMBEDDED);
        Composite timelableBTo = new Composite(timeInputB, SWT.EMBEDDED);

        timeInputB.setWeights(DEFAULT_WEIGHTS_TimeInterval);

        timelableB.setLayout(rowLayout);
        timelableBFrom.setLayout(rowLayout);
        timelableBTo.setLayout(rowLayout);

        // LabelA
        Label labelB = new Label(timelableB, SWT.NONE);
        labelB.setText("Time IntervalB "); //$NON-NLS-1$

        /// LableBFrom
        java.awt.Frame frameBFrom = SWT_AWT.new_Frame(timelableBFrom);
        java.awt.Panel panelBFrom = new java.awt.Panel();
        frameBFrom.add(panelBFrom);

        JLabel labelBFrom = new JLabel();
        labelBFrom.setText("From: "); //$NON-NLS-1$
        JFormattedTextField textBFrom = new JFormattedTextField(fFormat);
        textBFrom.addFocusListener(new java.awt.event.FocusListener() {
            public String oldVal = null;

            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                oldVal = textBFrom.getText();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (!oldVal.equals(textBFrom.getText())) {
                    long newTime = 0;
                    try {
                        newTime = fFormat.parseValue(textBFrom.getText());
                        ITmfTimestamp fromTime = TmfTimestamp.fromNanos(newTime);
                        fStartTimeB = fromTime;
                        textBFrom.setText(fStartTimeB.toString(fFormat));
                        getChartViewerB().getMouseSelectionProvider().refresh();
                        TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(this, fStartTimeA, fEndTimeA, getTrace()));
                    } catch (ParseException e1) {
                        textBFrom.setText(oldVal);
                        e1.printStackTrace();
                    }
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
        labelBTo.setText("To: "); //$NON-NLS-1$
        JFormattedTextField textBTo = new JFormattedTextField(fFormat);
        textBTo.addFocusListener(new java.awt.event.FocusListener() {
            public String oldVal = null;

            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                oldVal = textBTo.getText();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                // TODO Auto-generated method stub
                if (!oldVal.equals(textBTo.getText())) {
                    long newTime = 0;
                    try {
                        newTime = fFormat.parseValue(textBTo.getText());
                        ITmfTimestamp fromTime = TmfTimestamp.fromNanos(newTime);
                        fEndTimeB = fromTime;
                        textBTo.setText(fEndTimeB.toString(fFormat));
                        getChartViewerB().getMouseSelectionProvider().refresh();
                        TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(this, fStartTimeA, fEndTimeA, getTrace()));
                    } catch (ParseException e1) {
                        textBTo.setText(oldVal);
                        e1.printStackTrace();
                    }

                }
            }
        });

        panelBTo.add(labelBTo);
        panelBTo.add(textBTo);
        ftextBTo = textBTo;

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

        //contributeToActionBars();
        IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();

        IAction AggregatedAction = fAggregatedAction;
        if (AggregatedAction == null) {
            AggregatedAction = getAggregateByAction();
            fAggregatedAction = AggregatedAction;
        }
        if (AggregatedAction != null) {
            menuManager.add(new Separator());
            menuManager.add(AggregatedAction);
            menuManager.add(new Separator());
        }



        densityA.setWeights(DEFAULT_WEIGHTS_FILTERING_H);
        densityB.setWeights(DEFAULT_WEIGHTS_FILTERING_H);

        sashFormFiltering.setWeights(DEFAULT_WEIGHTS_FILTERING_VIEW);

        ///// Organizing sashFormQuery
        Composite queryText = new Composite(sashFormQuery, SWT.EMBEDDED);
        queryText.setLayout(new FillLayout());

        // Text

        java.awt.Frame frameQuery = SWT_AWT.new_Frame(queryText);
        java.awt.Panel panelQuery = new java.awt.Panel();
        frameQuery.add(panelQuery);

        JTextArea textQuery = new JTextArea();

        panelQuery.add(textQuery);
        JScrollPane scroll = new JScrollPane(textQuery,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        frameQuery.add(scroll);
        ftextQuery = textQuery;
        textQuery.addFocusListener(new java.awt.event.FocusListener() {

            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                // TODO Auto-generated method stub
                boolean parsed = parseQuery(ftextQuery.getText());
                if (parsed) {
                    TmfComparisonFilteringUpdatedSignal rangUpdateSignal = new TmfComparisonFilteringUpdatedSignal(this, fStartTimeA, fEndTimeA, fStartTimeB, fEndTimeB, fStatistic, fTraceListA, fTraceListB);
                    TmfSignalManager.dispatchSignal(rangUpdateSignal);
                    buildDifferetialFlameGraph();
                }
                System.out.println("insertUpdate\n");

            }
        });

        //// Organizing sashFormDoneButton
        /*
         * Composite doneButton = new Composite(sashFormDoneButton, SWT.NONE);
         * doneButton.setLayout(new GridLayout(1, false));
         *
         * final Button button = new Button(doneButton, SWT.PUSH);
         * button.setText("Differential Flamegraph"); //$NON-NLS-1$
         * button.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));
         *
         * button.addSelectionListener(new SelectionListener() {
         *
         * @Override public void widgetSelected(@Nullable SelectionEvent event)
         * { // text.setText("No worries!"); parseQuery(ftextQuery.getText());
         * TmfComparisonFilteringUpdatedSignal rangUpdateSignal = new
         * TmfComparisonFilteringUpdatedSignal(this, fStartTimeA, fEndTimeA,
         * fStartTimeB, fEndTimeB, fStatistic, fTraceListA, fTraceListB);
         * TmfSignalManager.dispatchSignal(rangUpdateSignal);
         * buildDifferetialFlameGraph();
         *
         * }
         *
         * @Override public void widgetDefaultSelected(@Nullable SelectionEvent
         * event) { // text.setText("No worries!"); } });
         */

        super.createPartControl(sashForm);
        sashForm.setWeights(DEFAULT_WEIGHTS_HideQery);

        ITmfTrace activetrace = TmfTraceManager.getInstance().getActiveTrace();
        if (activetrace != null) {
            buildDifferetialFlameGraph();

        }

        // IMenuManager menuManager =
        // getViewSite().getActionBars().getMenuManager();
        // menuManager.add(new Separator());
        // menuManager.add(getAggregateByAction());
        // menuManager.add(new Separator());

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

        // ftextAFrom.setValue(fStartTimeA);
        ftextAFrom.setText(fStartTimeA.toString(fFormat));
        // ftextBFrom.setValue(fStartTimeB);
        ftextBFrom.setText(fStartTimeB.toString(fFormat));
        // ftextATo.setValue(fEndTimeA);
        ftextATo.setText(fEndTimeA.toString(fFormat));
        // ftextBTo.setValue(fEndTimeB);
        ftextBTo.setText(fEndTimeB.toString(fFormat));

        TmfComparisonFilteringUpdatedSignal rangUpdateSignal = new TmfComparisonFilteringUpdatedSignal(this, fStartTimeA, fEndTimeA, fStartTimeB, fEndTimeB, fStatistic, fTraceListA, fTraceListB);
        TmfSignalManager.dispatchSignal(rangUpdateSignal);
        ftextQuery.setText(makeQuery());
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
                ftextQuery.setText(makeQuery());
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
                ftextAFrom.setText(fStartTimeA.toString(fFormat));
                ftextATo.setText(fEndTimeA.toString(fFormat));

            } else if (source == getChartViewerB()) {
                fStartTimeB = TmfTimestamp.fromNanos(getChartViewerB().getSelectionBeginTime());
                fEndTimeB = TmfTimestamp.fromNanos(getChartViewerB().getSelectionEndTime());
                ftextBFrom.setText(fStartTimeB.toString(fFormat));
                ftextBTo.setText(fEndTimeB.toString(fFormat));

            }
            TmfComparisonFilteringUpdatedSignal rangUpdateSignal = new TmfComparisonFilteringUpdatedSignal(this, fStartTimeA, fEndTimeA, fStartTimeB, fEndTimeB, null, null, null);
            TmfSignalManager.dispatchSignal(rangUpdateSignal);
            ftextQuery.setText(makeQuery());
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

                    Action statisticActionDur = createStatisticAction(Objects.requireNonNull(Messages.AbstractMultipleDensityView_Duration));
                    new ActionContributionItem(statisticActionDur).fill(menu, -1);

                    Action statisticActionSelf = createStatisticAction(Objects.requireNonNull(Messages.AbstractMultipleDensityView_SelfTime));
                    new ActionContributionItem(statisticActionSelf).fill(menu, -1);
                    return menu;

                }
            });
        }
        Action configureStatisticAction1 = Objects.requireNonNull(fConfigureStatisticAction);
        return configureStatisticAction1;
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
                /*
                 * for (String name : signal.getTraceList()) { if
                 * (fTraceListA.contains(name)) { fTraceListA.remove(name); }
                 * else { fTraceListA.add(name); } }
                 */
                fTraceListA.clear();
                for (String name : signal.getTraceList()) {
                    fTraceListA.add(name);
                }

                rangUpdateSignal = new TmfComparisonFilteringUpdatedSignal(this, null, fTraceListA, null);
                TmfSignalManager.dispatchSignal(rangUpdateSignal);
            }

            if (signal.getSource() == getChartViewerB()) {
                /*
                 * for (String name : signal.getTraceList()) { if
                 * (fTraceListB.contains(name)) { fTraceListB.remove(name); }
                 * else { fTraceListB.add(name); } }
                 */
                fTraceListB.clear();
                for (String name : signal.getTraceList()) {
                    fTraceListB.add(name);
                }

                rangUpdateSignal = new TmfComparisonFilteringUpdatedSignal(this, null, null, fTraceListB);
                TmfSignalManager.dispatchSignal(rangUpdateSignal);

            }
            ftextQuery.setText(makeQuery());
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

    private String makeQuery() {
        String query = ""; //$NON-NLS-1$
        /// Query PartA
        query = query.concat(fTraceStr);
        for (String trace : fTraceListA) {
            if (trace.indexOf(".") != -1) {
                query = query.concat(trace);
                query = query.concat(","); //$NON-NLS-1$
            }
        }
        query = query.concat(System.lineSeparator());

        query = query.concat(fFromStr);
        query = query.concat(fStartTimeA.toString(fFormat));
        query = query.concat(System.lineSeparator());

        query = query.concat(fToStr);
        query = query.concat(fEndTimeA.toString(fFormat));
        query = query.concat(System.lineSeparator());

        query = query.concat("Compared to: "); //$NON-NLS-1$
        query = query.concat(System.lineSeparator());

        /// Query PartB
        query = query.concat(fTraceStr);
        for (String trace : fTraceListB) {
            if (trace.indexOf(".") != -1) {
                query = query.concat(trace);
                query = query.concat(","); //$NON-NLS-1$
            }
        }
        query = query.concat(System.lineSeparator());

        query = query.concat(fFromStr);
        query = query.concat(fStartTimeB.toString(fFormat));
        query = query.concat(System.lineSeparator());

        query = query.concat(fToStr);
        query = query.concat(fEndTimeB.toString(fFormat));
        query = query.concat(System.lineSeparator());

        //// Query Statistic Part
        query = query.concat(fStatisticStr);
        query = query.concat(fStatistic);

        return query;
    }

    boolean parseQuery(String query) {
        try {
            String[] parts = query.split(System.lineSeparator());
            // Times

            if (parts[1].indexOf(fFromStr) == -1) {
                return false;
            }
            String fromStrA = parts[1].substring(parts[1].indexOf(fFromStr) + fFromStr.length(), parts[1].length());
            fStartTimeA = TmfTimestamp.fromNanos(fFormat.parseValue(fromStrA));

            if (parts[2].indexOf(fToStr) == -1) {
                return false;
            }
            String toStrA = parts[2].substring(parts[2].indexOf(fToStr) + fToStr.length(), parts[2].length());
            fEndTimeA = TmfTimestamp.fromNanos(fFormat.parseValue(toStrA));

            if (parts[5].indexOf(fFromStr) == -1) {
                return false;
            }
            String fromStrB = parts[5].substring(parts[5].indexOf(fFromStr) + fFromStr.length(), parts[5].length());
            fStartTimeB = TmfTimestamp.fromNanos(fFormat.parseValue(fromStrB));

            if (parts[6].indexOf(fToStr) == -1) {
                return false;
            }
            String toStrB = parts[6].substring(parts[6].indexOf(fToStr) + fToStr.length(), parts[6].length());
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
            ftextAFrom.setText(fromStrA);
            ftextATo.setText(toStrA);
            ftextBFrom.setText(fromStrB);
            ftextBTo.setText(toStrB);

        } catch (ParseException e) {
            // TODO Auto-generated catch block
            System.out.println("query format is incorrect " + e.toString());
            e.printStackTrace();
            return false;

        }

        return true;
    }
    //public void click(TmfXYChartViewer viewer, int millis) throws AWTException
 /*   public void click() throws AWTException
    {
        //Point p = viewer.getLocationOnScreen();
        Robot r = new Robot();
        int mask = InputEvent.BUTTON1_DOWN_MASK;
        r.mouseMove(95,94);
        System.out.println("injaaaaaammmmmm");
        r.mousePress(mask);
        try { Thread.sleep(100); } catch (Exception e) {}
        r.mouseRelease(mask);
    }*/
}