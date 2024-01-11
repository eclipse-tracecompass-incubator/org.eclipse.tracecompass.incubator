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
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
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
    private static final int[] DEFAULT_WEIGHTS = new int[] { 40, 15, 5, 40 };
    private static final int[] DEFAULT_WEIGHTS_FILTERING_VIEW = new int[] { 495, 10, 495 };
    private static final int[] DEFAULT_WEIGHTS_FILTERING_H = new int[] { 3, 9 };
    private static final int[] DEFAULT_WEIGHTS_LABELS = new int[] { 1, 7, 2 };
    private static final int[] DEFAULT_WEIGHTS_TimeInterval = new int[] { 250, 375, 375 };

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
    private JFormattedTextField ftextAFrom;
    private JFormattedTextField ftextBFrom;
    private JFormattedTextField ftextATo;
    private JFormattedTextField ftextBTo;
    private JTextArea ftextQuery;
    private char queryPartSplitter = '&';

    private TmfTimestampFormat fFormat = new TmfTimestampFormat("yyyy-MM-dd HH:mm:ss.SSS.SSS.SSS");
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
        //// Main organization of the view
        SashForm sashFormFiltering = new SashForm(sashForm, SWT.HORIZONTAL);
        SashForm sashFormQuery = new SashForm(sashForm, SWT.HORIZONTAL);
        SashForm sashFormDoneButton = new SashForm(sashForm, SWT.HORIZONTAL);

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

        timelableA.setLayout(rowLayout);
        timelableAFrom.setLayout(rowLayout);
        timelableATo.setLayout(rowLayout);

        // LabelA
        Label labelA = new Label(timelableA, SWT.NONE);
        labelA.setText("Time IntervalA "); //$NON-NLS-1$

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
                // TODO Auto-generated method stub
                oldVal = textAFrom.getText();
                System.out.println("I was there: Gain");

            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                // TODO Auto-generated method stub
                if (!oldVal.equals(textAFrom.getText())) {
                    long newTime = 0;
                    try {
                        newTime = fFormat.parseValue(textAFrom.getText());
                    } catch (ParseException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    ITmfTimestamp fromTime = TmfTimestamp.fromNanos(newTime);
                    fStartTimeA = fromTime;

                    TmfSelectionRangeUpdatedSignal signal = new TmfSelectionRangeUpdatedSignal(this, fStartTimeA, fEndTimeA, getTrace());
                    broadcast(signal);
                    System.out.println("I was there: Lost,change");

                    // }
                    System.out.println("I was there: Lost");
                    System.out.println("I was there: Lost" + newTime);

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
                // TODO Auto-generated method stub
                oldVal = textATo.getText();
                System.out.println("I was there: Gain");

            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                // TODO Auto-generated method stub
                if (!oldVal.equals(textATo.getText())) {
                    long newTime = 0;
                    try {
                        newTime = fFormat.parseValue(textATo.getText());
                    } catch (ParseException e1) {
                        // TODO Auto-generated catch block
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
                // TODO Auto-generated method stub
                oldVal = textBFrom.getText();
                System.out.println("I was there: Gain");

            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                // TODO Auto-generated method stub
                if (!oldVal.equals(textBFrom.getText())) {
                    long newTime = 0;
                    try {
                        newTime = fFormat.parseValue(textBFrom.getText());
                    } catch (ParseException e1) {
                        // TODO Auto-generated catch block
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
        labelBTo.setText("To: "); //$NON-NLS-1$
        JFormattedTextField textBTo = new JFormattedTextField(fFormat);
        textBTo.addFocusListener(new java.awt.event.FocusListener() {
            public String oldVal = null;

            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                // TODO Auto-generated method stub
                oldVal = textBTo.getText();
                System.out.println("I was there: Gain");

            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                // TODO Auto-generated method stub
                if (!oldVal.equals(textBTo.getText())) {
                    long newTime = 0;
                    try {
                        newTime = fFormat.parseValue(textBTo.getText());
                    } catch (ParseException e1) {
                        // TODO Auto-generated catch block
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

        contributeToActionBars();

        densityA.setWeights(DEFAULT_WEIGHTS_FILTERING_H);
        densityB.setWeights(DEFAULT_WEIGHTS_FILTERING_H);

        sashFormFiltering.setWeights(DEFAULT_WEIGHTS_FILTERING_VIEW);

        ///// Organizing sashFormQuery


        Composite querylable = new Composite(sashFormQuery, SWT.NONE);
        Composite queryText = new Composite(sashFormQuery, SWT.EMBEDDED);

        sashFormQuery.setWeights(new int[] { 5, 95 });

        querylable.setLayout(rowLayout);
        queryText.setLayout(new FillLayout());


        // Label
        Label labelQuery = new Label(querylable, SWT.NONE);
        labelQuery.setText("Query"); //$NON-NLS-1$

        // Text

        java.awt.Frame frameQuery = SWT_AWT.new_Frame(queryText);
        java.awt.Panel panelQuery = new java.awt.Panel();
        frameQuery.add(panelQuery);

        JTextArea textQuery = new JTextArea();
        panelQuery.add(textQuery);

        JScrollPane scroll = new JScrollPane (textQuery,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

        frameQuery.add(scroll);
        ftextQuery = textQuery;

        //// Organizing sashFormDoneButton
        Composite doneButton = new Composite(sashFormDoneButton, SWT.NONE);
        doneButton.setLayout(new GridLayout(1, false));

        final Button button = new Button(doneButton, SWT.PUSH);
        button.setText("Differential Flamegraph"); //$NON-NLS-1$
        button.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));

        button.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(@Nullable SelectionEvent event) {
                parseQuery(ftextQuery.getText());
                TmfComparisonFilteringUpdatedSignal rangUpdateSignal = new TmfComparisonFilteringUpdatedSignal(this, fStartTimeA, fEndTimeA, fStartTimeB, fEndTimeB, fStatistic, fTraceListA, fTraceListB);
                TmfSignalManager.dispatchSignal(rangUpdateSignal);
                buildDifferetialFlameGraph();

            }

            @Override
            public void widgetDefaultSelected(@Nullable SelectionEvent event) {
            }
        });

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

        ftextAFrom.setText(fStartTimeA.toString(fFormat));
        ftextBFrom.setText(fStartTimeB.toString(fFormat));
        ftextATo.setText(fEndTimeA.toString(fFormat));
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
        query = query.concat("Trace(s): ");//$NON-NLS-1$
        query = query.concat(System.lineSeparator());
        for (String trace : fTraceListA) {
            query = query.concat(trace);
            query = query.concat(","); //$NON-NLS-1$
        }
        query = query.concat("\nFrom:\n "); //$NON-NLS-1$
        query = query.concat(fStartTimeA.toString(fFormat));

        query = query.concat("\nTo: \n"); //$NON-NLS-1$
        query = query.concat(fEndTimeA.toString(fFormat));

        query = query.concat("\nCompared to:\n "); //$NON-NLS-1$

        /// Query PartB
        query = query.concat("Trace(s): \n"); //$NON-NLS-1$
        for (String trace : fTraceListB) {
            query = query.concat(trace);
            query = query.concat(","); //$NON-NLS-1$
        }
        query = query.concat("\nFrom:\n "); //$NON-NLS-1$
        query = query.concat(fStartTimeB.toString(fFormat));

        query = query.concat("\nTo:\n "); //$NON-NLS-1$
        query = query.concat(fEndTimeB.toString(fFormat));
        //// Query Sttistic Part
        query = query.concat("\nStatistic: \n"); //$NON-NLS-1$
        query = query.concat(fStatistic);

        return query;
    }

    void parseQuery(String query) {
        try {
        String[] parts = query.split(System.lineSeparator());
        //// Part A
        // traceListA
        fTraceListA.clear();
        String[] traces = parts[1].split(","); //$NON-NLS-1$
        for (String trace : traces) {
            fTraceListA.add(trace);
        }
        // Times
        try {
            fStartTimeA = TmfTimestamp.fromNanos(fFormat.parseValue(parts[3]));
            ftextAFrom.setText(parts[3]);
            fEndTimeA = TmfTimestamp.fromNanos(fFormat.parseValue(parts[5]));
            ftextATo.setText(parts[5]);
            fStartTimeB = TmfTimestamp.fromNanos(fFormat.parseValue(parts[10]));
            ftextBFrom.setText(parts[10]);
            fEndTimeB = TmfTimestamp.fromNanos(fFormat.parseValue(parts[12]));
            ftextBTo.setText(parts[12]);

        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // traceListB
        fTraceListB.clear();
        traces = parts[8].split(",");
        for (String trace : traces) {
            fTraceListB.add(trace);
        }
        ////Statistic
        fStatistic = parts[14];
        }
        catch(ArrayIndexOutOfBoundsException ex){
            System.out.println("query format is incorrect "+ex.toString());

        }


    }



}