/*******************************************************************************
 * Copyright (c) 2024 École Polytechnique de Montréal, Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.executioncomparison.ui;

import java.awt.Color;
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
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.ScopeLog;
import org.eclipse.tracecompass.incubator.internal.executioncomparison.core.TmfCheckboxChangedSignal;
import org.eclipse.tracecompass.incubator.internal.executioncomparison.core.TmfComparisonFilteringUpdatedSignal;
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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;

/**
 * The ExecutionComparisonView allows to compare two groups of traces (parts of
 * traces)
 *
 * @author Fateme Faraji Daneshgar and Vlad Arama
 */
@SuppressWarnings("restriction")
public class ExecutionComparisonView extends DifferentialFlameGraphView implements ICheckboxTreeViewerListener {

    /**
     * the id of the view
     */
    public static final String VIEW_ID = Objects.requireNonNull(Messages.multipleDensityViewId);
    /**
     * The title of the view
     */
    public static final String VIEW_TITLE = Objects.requireNonNull(Messages.multipleDensityViewTitle);
    private static final String DATA_SELECTION = Objects.requireNonNull(Messages.dataSelection);
    private static final String BACKGROUND = Objects.requireNonNull(Messages.background);
    private static final String FOREGROUND = Objects.requireNonNull(Messages.foreground);
    private static final String TRACE_NAME = Objects.requireNonNull(Messages.traceName);
    private static final String STATISTIC_NAME = Objects.requireNonNull(Messages.statisticName);
    private static final String TMF_VIEW_UI_CONTEXT = Objects.requireNonNull(Messages.tmfViewUiContext);
    private static final String Y_AXIS_LABEL = Objects.requireNonNull(Messages.yAxisLabel);
    private String fStatistic = Objects.requireNonNull(Messages.multipleDensityViewDuration);

    /**
     * Default weights for organizing the view
     */
    private static final int[] DEFAULT_WEIGHTS_ShowQuery = new int[] { 450, 290, 260 };
    private static final int[] DEFAULT_WEIGHTS_HideQuery = new int[] { 450, 100, 450 };
    private static final int[] DEFAULT_WEIGHTS_FILTERING_H = new int[] { 3, 9 };
    private static final int[] DEFAULT_WEIGHTS_TimeInterval = new int[] { 240, 380, 380 };

    /** A composite that allows us to add margins for part A and B */
    private @Nullable TmfXYChartViewer fChartViewerA;
    private @Nullable TmfXYChartViewer fChartViewerB;

    private @Nullable SashForm fXYViewerContainerA;
    private @Nullable SashForm fXYViewerContainerB;

    private @Nullable TmfViewer fTmfViewerA;
    private @Nullable TmfViewer fTmfViewerB;

    private @Nullable SashForm fSashFormLeftChildA;
    private @Nullable SashForm fSashFormLeftChildB;

    private List<String> fTraceListA = new ArrayList<>();
    private List<String> fTraceListB = new ArrayList<>();

    private ITmfTimestamp fStartTimeA = TmfTimestamp.BIG_BANG;
    private ITmfTimestamp fStartTimeB = TmfTimestamp.BIG_BANG;
    private ITmfTimestamp fEndTimeA = TmfTimestamp.BIG_CRUNCH;
    private ITmfTimestamp fEndTimeB = TmfTimestamp.BIG_CRUNCH;
    private TmfTimestampFormat fFormat = new TmfTimestampFormat("yyyy-MM-dd HH:mm:ss.SSS.SSS.SSS"); //$NON-NLS-1$

    private JFormattedTextField ftextAFrom = new JFormattedTextField();
    private JFormattedTextField ftextBFrom = new JFormattedTextField();
    private JFormattedTextField ftextATo = new JFormattedTextField();
    private JFormattedTextField ftextBTo = new JFormattedTextField();

    private @Nullable IContextService fContextService;
    private @Nullable Action fConfigureStatisticAction;
    private @Nullable Text ftextQuery;
    private @Nullable Listener fSashDragListener;
    private @Nullable SashForm fsashForm = null;
    private List<IContextActivation> fActiveContexts = new ArrayList<>();

    /**
     * Constructs two density charts for selecting the desired traces and time
     * ranges in order to do the comparison
     */
    public ExecutionComparisonView() {
        super();
    }

    @Override
    public void createPartControl(@Nullable Composite parent) {
        TmfSignalManager.register(this);

        this.setContentDescription(DATA_SELECTION);
        final SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
        fsashForm = sashForm;
        sashForm.setLayout(new GridLayout(1, false));
        sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        /*
         * Main organization of the view. There are three main parts in the
         * view: Filtering, Query and Differential Flame Graph
         */
        Composite sashFormFiltering = new Composite(sashForm, SWT.HORIZONTAL);
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = layout.marginWidth = 0;
        sashFormFiltering.setLayout(layout);
        sashFormFiltering.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // GroupA
        Group groupA = new Group(sashFormFiltering, SWT.NULL);
        GridLayout gridLayoutG = new GridLayout();
        GridData gridDataG = new GridData(GridData.FILL_BOTH);
        SashForm densityA = new SashForm(groupA, SWT.NULL);
        GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
        SashForm timeInputA = new SashForm(groupA, SWT.NULL);
        createGroups(groupA, gridLayoutG, gridDataG, densityA, data, timeInputA, true);

        // GroupB
        Group groupB = new Group(sashFormFiltering, SWT.NULL);
        SashForm densityB = new SashForm(groupB, SWT.NULL);
        SashForm timeInputB = new SashForm(groupB, SWT.NULL);
        createGroups(groupB, gridLayoutG, gridDataG, densityB, data, timeInputB, false);

        // Get the current theme
        IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
        ITheme currentTheme = themeManager.getCurrentTheme();

        // Get background color
        RGB backgroundRGB = currentTheme.getColorRegistry().getRGB(BACKGROUND);
        java.awt.Color backgroundColor;
        if (backgroundRGB != null) {
            backgroundColor = new java.awt.Color(backgroundRGB.red, backgroundRGB.green, backgroundRGB.blue);
        } else {
            org.eclipse.swt.graphics.Color swtColor = Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
            backgroundColor = new java.awt.Color(swtColor.getRed(), swtColor.getGreen(), swtColor.getBlue());
        }

        // Get foreground color
        RGB foregroundRGB = currentTheme.getColorRegistry().getRGB(FOREGROUND);
        java.awt.Color foregroundColor;
        if (foregroundRGB != null) {
            foregroundColor = new java.awt.Color(foregroundRGB.red, foregroundRGB.green, foregroundRGB.blue);
        } else {
            org.eclipse.swt.graphics.Color swtColor = Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
            foregroundColor = new java.awt.Color(swtColor.getRed(), swtColor.getGreen(), swtColor.getBlue());
        }
        // Group A Time Intervals
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

        SashForm sashFormLeftChildA = new SashForm(densityA, SWT.None);
        fSashFormLeftChildA = sashFormLeftChildA;

        TmfViewer tmfViewerA = createLeftChildViewer(sashFormLeftChildA);
        fTmfViewerA = tmfViewerA;

        SashForm xYViewerContainerA = new SashForm(densityA, SWT.None);
        fXYViewerContainerA = xYViewerContainerA;
        xYViewerContainerA.setLayout(GridLayoutFactory.fillDefaults().create());

        TmfXYChartViewer chartViewerA = createChartViewer(xYViewerContainerA);
        fChartViewerA = chartViewerA;

        // Reset Button A
        Button resetButtonA = new Button(timelableA, SWT.PUSH);
        createResetButtonA(resetButtonA, chartViewerA, tmfViewerA);

        // LabelFromA
        java.awt.Frame frameAFrom = SWT_AWT.new_Frame(timelableAFrom);

        java.awt.Panel panelAFrom = new java.awt.Panel();
        JFormattedTextField textAFrom = new JFormattedTextField(fFormat);
        if (frameAFrom != null) {
            createLabelFromA(frameAFrom, panelAFrom, textAFrom, backgroundColor, foregroundColor, chartViewerA);
        }

        // LabelToA
        java.awt.Frame frameATo = SWT_AWT.new_Frame(timelableATo);
        java.awt.Panel panelATo = new java.awt.Panel();
        JFormattedTextField textATo = new JFormattedTextField(fFormat);
        if (frameATo != null) {
            createLabelToA(frameATo, panelATo, textATo, backgroundColor, foregroundColor, chartViewerA);
        }

        // Group B Time Intervals
        Composite timelableB = new Composite(timeInputB, SWT.FILL);
        Composite timelableBFrom = new Composite(timeInputB, SWT.EMBEDDED);
        Composite timelableBTo = new Composite(timeInputB, SWT.EMBEDDED);

        timeInputB.setWeights(DEFAULT_WEIGHTS_TimeInterval);

        timelableB.setLayout(gridLayout);
        timelableBFrom.setLayout(rowLayout);
        timelableBTo.setLayout(rowLayout);

        SashForm sashFormLeftChildB = new SashForm(densityB, SWT.None);
        fSashFormLeftChildB = sashFormLeftChildB;

        TmfViewer tmfViewerB = createLeftChildViewer(sashFormLeftChildB);
        fTmfViewerB = tmfViewerB;

        SashForm xYViewerContainerB = new SashForm(densityB, SWT.VERTICAL);
        fXYViewerContainerB = xYViewerContainerB;
        xYViewerContainerB.setLayout(GridLayoutFactory.fillDefaults().create());

        TmfXYChartViewer chartViewerB = createChartViewer(xYViewerContainerB);
        fChartViewerB = chartViewerB;

        // Reset Button B
        Button resetButtonB = new Button(timelableB, SWT.PUSH);
        createResetButtonB(resetButtonB, chartViewerB, tmfViewerB);

        // LabelFromB
        java.awt.Frame frameBFrom = SWT_AWT.new_Frame(timelableBFrom);
        java.awt.Panel panelBFrom = new java.awt.Panel();
        JFormattedTextField textBFrom = new JFormattedTextField(fFormat);
        if (frameBFrom != null) {
            createLabelFromB(frameBFrom, panelBFrom, textBFrom, backgroundColor, foregroundColor, chartViewerB);
        }

        // LableToB
        java.awt.Frame frameBTo = SWT_AWT.new_Frame(timelableBTo);
        java.awt.Panel panelBTo = new java.awt.Panel();
        JFormattedTextField textBTo = new JFormattedTextField(fFormat);
        if (frameBTo != null) {
            createLabelToB(frameBTo, panelBTo, textBTo, backgroundColor, foregroundColor, chartViewerB);
        }

        createControlPaintListenerA(chartViewerA, sashFormLeftChildA);

        IStatusLineManager statusLineManager = getViewSite().getActionBars().getStatusLineManager();
        chartViewerA.setStatusLineManager(statusLineManager);
        coupleSelectViewer(tmfViewerA, chartViewerA);
        ((AbstractSelectTreeViewer2) tmfViewerA).addTreeListener(this);

        IWorkbenchPartSite site = getSite();
        fContextService = site.getWorkbenchWindow().getService(IContextService.class);

        createControlPaintListenerB(chartViewerB, sashFormLeftChildB);

        chartViewerB.setStatusLineManager(statusLineManager);
        coupleSelectViewer(tmfViewerB, chartViewerB);
        ((AbstractSelectTreeViewer2) tmfViewerB).addTreeListener(this);

        IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();

        IAction aggregatedAction = fConfigureStatisticAction;
        if (aggregatedAction == null) {
            aggregatedAction = getAggregateByAction();
            fConfigureStatisticAction = (Action) aggregatedAction;
        }
        menuManager.add(new Separator());
        menuManager.add(aggregatedAction);
        menuManager.add(new Separator());

        densityA.setWeights(DEFAULT_WEIGHTS_FILTERING_H);
        densityB.setWeights(DEFAULT_WEIGHTS_FILTERING_H);

        Group groupQuery = new Group(sashForm, SWT.NULL);
        groupQuery.setText(Messages.multipleDensityViewQueryGroup);
        gridLayoutG = new GridLayout();
        gridLayoutG.numColumns = 1;
        groupQuery.setLayout(gridLayoutG);
        gridDataG = new GridData(GridData.FILL_BOTH);
        gridDataG.horizontalSpan = 1;
        groupQuery.setLayoutData(gridDataG);

        // Organizing sashFormQuery
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
        queryText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Text
        Text text = new Text(queryText, SWT.MULTI | SWT.BORDER);
        data.verticalAlignment = SWT.FILL;
        data.horizontalAlignment = SWT.FILL;
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        data.heightHint = 75;
        data.widthHint = 100;
        text.setLayoutData(data);
        ftextQuery = text;
        addTextFocusListener(text, chartViewerA, chartViewerB, tmfViewerA, tmfViewerB);

        ExpandItem item0 = new ExpandItem(bar, SWT.NONE, 0);
        item0.setText(Messages.multipleDensityViewQueryExpandable);
        item0.setHeight(150);

        item0.setControl(queryText);
        item0.setExpanded(false);

        bar.setSpacing(5);
        createExpandBarListener(bar, queryText, sashForm);

        super.createPartControl(sashForm);
        sashForm.setWeights(DEFAULT_WEIGHTS_HideQuery);

        ITmfTrace activetrace = TmfTraceManager.getInstance().getActiveTrace();
        if (activetrace != null) {
            buildDifferentialFlameGraph();
        }
    }

    private static void createGroups(Group group, GridLayout gridLayoutG, GridData gridDataG, SashForm density, GridData data, SashForm timeInput, boolean isA) {
        if (isA) {
            group.setText(Messages.multipleDensityViewGroupA);
        } else {
            group.setText(Messages.multipleDensityViewGroupB);
        }
        gridLayoutG.numColumns = 1;
        group.setLayout(gridLayoutG);
        gridDataG.horizontalSpan = 1;
        group.setLayoutData(gridDataG);
        data.heightHint = 200;
        density.setLayoutData(data);
        timeInput.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    private void createResetButtonA(Button resetButton, TmfXYChartViewer chartViewerA, TmfViewer tmfViewerA) {
        resetButton.setText("Reset Time IntervalA"); //$NON-NLS-1$
        resetButton.addListener(SWT.Selection, event -> {
            ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
            if (trace != null) {
                /*
                 * Resets tree viewer checked items. All items should be
                 * checked.
                 */
                List<ITmfTreeViewerEntry> treeCheckedElements = ((MultipleEventDensityViewer) chartViewerA).getWholeCheckedItems();
                setCheckedElements(chartViewerA, tmfViewerA, treeCheckedElements, false);

                // Reset start time and end time and relating objects
                fStartTimeA = trace.getStartTime();
                fEndTimeA = trace.getEndTime();
                ftextAFrom.setText(fStartTimeA.toString(fFormat));
                ftextATo.setText(fEndTimeA.toString(fFormat));
                if (ftextQuery != null) {
                    ftextQuery.setText(buildComparisonQuery());
                }

                // Dispatch signal to rebuild differential flame graph
                TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(chartViewerA, fStartTimeA, fEndTimeA, getTrace()));

            }
        });
    }

    private void createResetButtonB(Button resetButton, TmfXYChartViewer chartViewerB, TmfViewer tmfViewerB) {
        resetButton.setText("Reset Time IntervalB"); //$NON-NLS-1$
        resetButton.addListener(SWT.Selection, event -> {
            ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
            if (trace != null) {
                /*
                 * Resets tree viewer checked items. All items should be checked
                 */
                List<ITmfTreeViewerEntry> treeCheckedElements = ((MultipleEventDensityViewer) chartViewerB).getWholeCheckedItems();
                setCheckedElements(chartViewerB, tmfViewerB, treeCheckedElements, false);

                // Reset start time and end time and relating objects
                fStartTimeB = trace.getStartTime();
                fEndTimeB = trace.getEndTime();
                ftextBFrom.setText(fStartTimeB.toString(fFormat));
                ftextBTo.setText(fEndTimeB.toString(fFormat));
                if (ftextQuery != null) {
                    ftextQuery.setText(buildComparisonQuery());
                }

                // Dispatch signal to rebuild differential flame graph
                TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(chartViewerB, fStartTimeB, fEndTimeB, getTrace()));

            }
        });
    }

    private void createLabelFromA(java.awt.Frame frameFrom, java.awt.Panel panelFrom, JFormattedTextField textFrom, Color backgroundColor, Color foregroundColor, TmfXYChartViewer chartViewer) {
        frameFrom.add(panelFrom);
        JLabel labelAFrom = new JLabel();
        labelAFrom.setText(Messages.multipleDensityViewFrom);

        // Set the background and foreground colors
        panelFrom.setBackground(backgroundColor);
        textFrom.setBackground(backgroundColor);
        labelAFrom.setBackground(backgroundColor);

        textFrom.setForeground(foregroundColor);
        labelAFrom.setForeground(foregroundColor);

        textFrom.addFocusListener(new java.awt.event.FocusListener() {
            private String oldVal = ""; //$NON-NLS-1$

            @Override
            public void focusGained(java.awt.event.@Nullable FocusEvent e) {
                String aFrom = textFrom.getText();
                if (aFrom != null) {
                    oldVal = aFrom;
                }
            }

            @Override
            public void focusLost(java.awt.event.@Nullable FocusEvent e) {
                if (!oldVal.equals(textFrom.getText())) {
                    long newTime = 0;
                    try {
                        newTime = fFormat.parseValue(textFrom.getText());
                        ITmfTimestamp fromTime = TmfTimestamp.fromNanos(newTime);
                        fStartTimeA = fromTime;
                        textFrom.setText(fStartTimeA.toString(fFormat));

                        updateSelectedRange();
                        chartViewer.selectionRangeUpdated(new TmfSelectionRangeUpdatedSignal(this, fStartTimeA, fEndTimeA, getTrace()));

                    } catch (ParseException e1) {
                        textFrom.setText(oldVal);
                        e1.printStackTrace();
                    }
                }
            }
        });
        textFrom.setText(fStartTimeA.toString(fFormat));
        panelFrom.add(labelAFrom);
        panelFrom.add(textFrom);
        ftextAFrom = textFrom;

    }

    private void createLabelFromB(java.awt.Frame frameFrom, java.awt.Panel panelFrom, JFormattedTextField textFrom, Color backgroundColor, Color foregroundColor, TmfXYChartViewer chartViewer) {
        frameFrom.add(panelFrom);
        JLabel labelBFrom = new JLabel();
        labelBFrom.setText(Messages.multipleDensityViewFrom);

        // Set the background and foreground colors
        panelFrom.setBackground(backgroundColor);
        textFrom.setBackground(backgroundColor);
        labelBFrom.setBackground(backgroundColor);

        textFrom.setForeground(foregroundColor);
        labelBFrom.setForeground(foregroundColor);

        textFrom.addFocusListener(new java.awt.event.FocusListener() {
            private String oldVal = ""; //$NON-NLS-1$

            @Override
            public void focusGained(java.awt.event.@Nullable FocusEvent e) {
                String bFrom = textFrom.getText();
                if (bFrom != null) {
                    oldVal = bFrom;
                }
            }

            @Override
            public void focusLost(java.awt.event.@Nullable FocusEvent e) {
                if (!oldVal.equals(textFrom.getText())) {
                    long newTime = 0;
                    try {
                        newTime = fFormat.parseValue(textFrom.getText());
                        ITmfTimestamp fromTime = TmfTimestamp.fromNanos(newTime);
                        fStartTimeB = fromTime;
                        textFrom.setText(fStartTimeB.toString(fFormat));

                        updateSelectedRange();
                        chartViewer.selectionRangeUpdated(new TmfSelectionRangeUpdatedSignal(this, fStartTimeB, fEndTimeB, getTrace()));

                    } catch (ParseException e1) {
                        textFrom.setText(oldVal);
                        e1.printStackTrace();
                    }

                }
            }
        });

        textFrom.setText(fStartTimeB.toString(fFormat));
        panelFrom.add(labelBFrom);
        panelFrom.add(textFrom);
        ftextBFrom = textFrom;
    }

    private void createLabelToA(java.awt.Frame frameTo, java.awt.Panel panelTo, JFormattedTextField textTo, Color backgroundColor, Color foregroundColor, TmfXYChartViewer chartViewer) {
        frameTo.add(panelTo);
        JLabel labelATo = new JLabel();
        labelATo.setText(Messages.multipleDensityViewTo);

        // Set the background and foreground colors
        panelTo.setBackground(backgroundColor);
        textTo.setBackground(backgroundColor);
        labelATo.setBackground(backgroundColor);

        textTo.setForeground(foregroundColor);
        labelATo.setForeground(foregroundColor);

        textTo.addFocusListener(new java.awt.event.FocusListener() {
            private String oldVal = ""; //$NON-NLS-1$

            @Override
            public void focusGained(java.awt.event.@Nullable FocusEvent e) {
                String aTo = textTo.getText();
                if (aTo != null) {
                    oldVal = aTo;
                }
            }

            @Override
            public void focusLost(java.awt.event.@Nullable FocusEvent e) {
                if (!oldVal.equals(textTo.getText())) {
                    long newTime = 0;
                    try {
                        newTime = fFormat.parseValue(textTo.getText());
                        ITmfTimestamp fromTime = TmfTimestamp.fromNanos(newTime);
                        fEndTimeA = fromTime;
                        textTo.setText(fEndTimeA.toString(fFormat));

                        updateSelectedRange();
                        chartViewer.selectionRangeUpdated(new TmfSelectionRangeUpdatedSignal(this, fStartTimeA, fEndTimeA, getTrace()));

                    } catch (ParseException e1) {
                        textTo.setText(oldVal);
                        e1.printStackTrace();
                    }
                }
            }
        });
        textTo.setText(fEndTimeA.toString(fFormat));
        panelTo.add(labelATo);
        panelTo.add(textTo);
        ftextATo = textTo;
    }

    private void createLabelToB(java.awt.Frame frameTo, java.awt.Panel panelTo, JFormattedTextField textTo, Color backgroundColor, Color foregroundColor, TmfXYChartViewer chartViewer) {
        frameTo.add(panelTo);
        JLabel labelBTo = new JLabel();
        labelBTo.setText(Messages.multipleDensityViewTo);

        // Set the background and foreground colors
        panelTo.setBackground(backgroundColor);
        textTo.setBackground(backgroundColor);
        labelBTo.setBackground(backgroundColor);

        textTo.setForeground(foregroundColor);
        labelBTo.setForeground(foregroundColor);

        textTo.addFocusListener(new java.awt.event.FocusListener() {

            public @Nullable String oldVal = null;

            @Override
            public void focusGained(java.awt.event.@Nullable FocusEvent e) {
                oldVal = textTo.getText();
            }

            @Override
            public void focusLost(java.awt.event.@Nullable FocusEvent e) {
                if (oldVal != null && !oldVal.equals(textTo.getText())) {
                    long newTime = 0;
                    try {
                        newTime = fFormat.parseValue(textTo.getText());
                        ITmfTimestamp fromTime = TmfTimestamp.fromNanos(newTime);
                        fEndTimeB = fromTime;
                        textTo.setText(fEndTimeB.toString(fFormat));
                        updateSelectedRange();
                        chartViewer.selectionRangeUpdated(new TmfSelectionRangeUpdatedSignal(this, fStartTimeA, fEndTimeA, getTrace()));

                    } catch (ParseException e1) {
                        textTo.setText(oldVal);
                        e1.printStackTrace();
                    }
                }
            }

        });
        textTo.setText(fEndTimeB.toString(fFormat));
        panelTo.add(labelBTo);
        panelTo.add(textTo);
        ftextBTo = textTo;
    }

    private void createControlPaintListenerA(TmfXYChartViewer chartViewer, SashForm sashFormLeftChildA) {
        chartViewer.getControl().addPaintListener(new PaintListener() {
            @Override
            public void paintControl(@Nullable PaintEvent e) {
                /*
                 * Sashes in a SashForm are being created on layout so we need
                 * to add the drag listener here.
                 */
                Listener sashDragListener = fSashDragListener;
                if (sashDragListener == null) {
                    for (Control control : sashFormLeftChildA.getChildren()) {
                        if (control instanceof Sash) {
                            sashDragListener = event -> TmfSignalManager.dispatchSignal(new TmfTimeViewAlignmentSignal(sashFormLeftChildA, getTimeViewAlignmentInfo(chartViewer, sashFormLeftChildA)));
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
    }

    private void createControlPaintListenerB(TmfXYChartViewer chartViewer, SashForm sashFormLeftChildB) {
        chartViewer.getControl().addPaintListener(new PaintListener() {
            @Override
            public void paintControl(@Nullable PaintEvent e) {
                /*
                 * Sashes in a SashForm are being created on layout so we need
                 * to add the drag listener here.
                 */
                Listener sashDragListener = fSashDragListener;
                if (sashDragListener == null) {
                    for (Control control : sashFormLeftChildB.getChildren()) {
                        if (control instanceof Sash) {
                            sashDragListener = event -> TmfSignalManager.dispatchSignal(new TmfTimeViewAlignmentSignal(sashFormLeftChildB, getTimeViewAlignmentInfo(chartViewer, sashFormLeftChildB)));
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
    }

    private void addTextFocusListener(Text text, TmfXYChartViewer chartViewerA, TmfXYChartViewer chartViewerB, TmfViewer tmfViewerA, TmfViewer tmfViewerB) {
        text.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(@Nullable FocusEvent e) {
                // Does nothing
            }

            @Override
            public void focusLost(@Nullable FocusEvent e) {
                if (ftextQuery != null) {
                    String query = ftextQuery.getText();
                    if (query == null) {
                        return;
                    }
                    boolean parsed = parseComparisonQuery(query);
                    if (parsed) {
                        /// Updating blue lines in density chats
                        chartViewerA.selectionRangeUpdated(new TmfSelectionRangeUpdatedSignal(this, fStartTimeA, fEndTimeA, getTrace()));
                        chartViewerB.selectionRangeUpdated(new TmfSelectionRangeUpdatedSignal(this, fStartTimeB, fEndTimeB, getTrace()));

                        // treeViewerA
                        List<ITmfTreeViewerEntry> treeWholeElements = ((MultipleEventDensityViewer) chartViewerA).getWholeCheckedItems();
                        List<ITmfTreeViewerEntry> treeCheckedElements = updateCheckedElements(treeWholeElements, true);

                        setCheckedElements(chartViewerA, tmfViewerA, treeCheckedElements, true);
                        // treeViewerB
                        treeWholeElements = ((MultipleEventDensityViewer) chartViewerB).getWholeCheckedItems();
                        treeCheckedElements = updateCheckedElements(treeWholeElements, false);

                        setCheckedElements(chartViewerB, tmfViewerB, treeCheckedElements, true);

                        TmfComparisonFilteringUpdatedSignal rangUpdateSignal = new TmfComparisonFilteringUpdatedSignal(this, fStartTimeA, fEndTimeA, fStartTimeB, fEndTimeB, fStatistic, fTraceListA, fTraceListB);
                        TmfSignalManager.dispatchSignal(rangUpdateSignal);
                        buildDifferentialFlameGraph();
                    }
                }
            }
        });
    }

    private List<ITmfTreeViewerEntry> updateCheckedElements(List<ITmfTreeViewerEntry> treeWholeElements, boolean isGroupA) {
        List<ITmfTreeViewerEntry> treeCheckedElements = new ArrayList<>();
        if (isGroupA) {
            for (ITmfTreeViewerEntry trace : treeWholeElements) {
                if (fTraceListA.contains(trace.getName())) {
                    treeCheckedElements.add(trace);
                    treeCheckedElements.addAll(trace.getChildren());
                }
            }
        } else {
            for (ITmfTreeViewerEntry trace : treeWholeElements) {
                if (fTraceListB.contains(trace.getName())) {
                    treeCheckedElements.add(trace);
                    treeCheckedElements.addAll(trace.getChildren());
                }
            }
        }
        return treeCheckedElements;
    }

    private static void createExpandBarListener(ExpandBar bar, Composite queryText, SashForm sashForm) {
        bar.addExpandListener(new ExpandListener() {

            @Override
            public void itemExpanded(@Nullable ExpandEvent e) {
                Display.getCurrent().asyncExec(() -> {
                    queryText.pack(true);
                    sashForm.setWeights(DEFAULT_WEIGHTS_ShowQuery);

                });
            }

            @Override
            public void itemCollapsed(@Nullable ExpandEvent e) {
                Display.getCurrent().asyncExec(() -> {
                    queryText.pack(true);
                    sashForm.setWeights(DEFAULT_WEIGHTS_HideQuery);
                });
            }
        });
    }

    /**
     * Returns the time alignment information
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
        if (fTmfViewerA instanceof TmfTimeViewer) {
            ((TmfTimeViewer) fTmfViewerA).traceSelected(signal);
        }
        if (fChartViewerA != null) {
            fChartViewerA.traceSelected(signal);
        }

        if (fTmfViewerB instanceof TmfTimeViewer) {
            ((TmfTimeViewer) fTmfViewerB).traceSelected(signal);
        }

        if (fChartViewerB != null) {
            fChartViewerB.traceSelected(signal);
        }
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
        if (ftextQuery != null) {
            ftextQuery.setText(buildComparisonQuery());
        }
        buildDifferentialFlameGraph();
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
                if (ftextQuery != null) {
                    ftextQuery.setText(buildComparisonQuery());
                }
                buildDifferentialFlameGraph();
            }
        };
    }

    /**
     * Signal that the trace was opened
     *
     * @param signal
     *            the trace open signal
     */
    @TmfSignalHandler
    public void traceOpened(TmfTraceOpenedSignal signal) {
        if (fChartViewerA != null) {
            fChartViewerA.traceOpened(signal);
        }
        if (fChartViewerB != null) {
            fChartViewerB.traceOpened(signal);
        }

    }

    private void buildDifferentialFlameGraph() {

        ITmfTrace activetrace = TmfTraceManager.getInstance().getActiveTrace();
        if (activetrace != null) {
            Display.getDefault().asyncExec(() -> buildFlameGraph(activetrace, null, null));

        }
    }

    /**
     * Creates left child viewer for event density chart
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
        MultipleEventDensityViewer chartViewer = new MultipleEventDensityViewer(parent, new TmfXYChartSettings(null, null, Y_AXIS_LABEL, 1));
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
     * Sets checked for the elements in TreeCheckedElements
     *
     * @param chart
     *            the chart viewer
     * @param tree
     *            the tree viewer
     * @param treeCheckedElements
     *            the elements in tree that should be checked
     * @param queryUpdate
     *            updates the check state of the tree viewer if true
     */
    public void setCheckedElements(TmfXYChartViewer chart, TmfViewer tree, List<ITmfTreeViewerEntry> treeCheckedElements, boolean queryUpdate) {
        if (queryUpdate) {
            ((MultipleEventDensityViewer) chart).updateCheckStateChangedEvent(treeCheckedElements);
        } else {
            ((MultipleEventDensityViewer) chart).handleCheckStateChangedEvent(treeCheckedElements);
        }
        Object[] treeCheckedElementsObj = new Object[treeCheckedElements.size()];

        treeCheckedElements.toArray(treeCheckedElementsObj);
        ((AbstractSelectTreeViewer2) tree).getTriStateFilteredCheckboxTree().setCheckedElements(treeCheckedElementsObj);
    }

    /**
     * Resets the start and end times.
     *
     * @param notify
     *            indicating if we should broadcast the update signal
     * @param chart
     *            determines which chart to reset start and end times
     */
    public void resetStartFinishTime(boolean notify, TmfXYChartViewer chart) {
        if (notify) {
            TmfWindowRangeUpdatedSignal signal = new TmfWindowRangeUpdatedSignal(this, TmfTimeRange.ETERNITY, getTrace());
            broadcast(signal);

        }
    }

    private void deactivateContextService() {
        if (fContextService != null) {
            fContextService.deactivateContexts(fActiveContexts);
            fActiveContexts.clear();
        }
    }

    private void activateContextService() {
        if (fActiveContexts.isEmpty() && fContextService != null) {
            IContextActivation activateContext = fContextService.activateContext(TMF_VIEW_UI_CONTEXT);
            Objects.requireNonNull(activateContext);
            fActiveContexts.add(activateContext);
        }
    }

    private static void coupleSelectViewer(TmfViewer tree, TmfXYChartViewer chart) {
        if (tree instanceof AbstractSelectTreeViewer2 && chart instanceof TmfFilteredXYChartViewer) {
            AbstractSelectTreeViewer2 selectTree = (AbstractSelectTreeViewer2) tree;
            ILegendImageProvider2 legendImageProvider = new XYChartLegendImageProvider((TmfCommonXAxisChartViewer) chart);
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
        Object source = signal.getSource();
        if (source == fChartViewerA) {
            fStartTimeA = signal.getBeginTime();
            fEndTimeA = signal.getEndTime();
            ftextAFrom.setText(fStartTimeA.toString(fFormat));
            ftextATo.setText(fEndTimeA.toString(fFormat));

        } else if (source == fChartViewerB) {
            fStartTimeB = signal.getBeginTime();
            fEndTimeB = signal.getEndTime();
            ftextBFrom.setText(fStartTimeB.toString(fFormat));
            ftextBTo.setText(fEndTimeB.toString(fFormat));
        }
        updateSelectedRange();

    }

    /**
     * Updates the filtering parameters and query and builds flamegraph
     */
    public void updateSelectedRange() {
        try (ScopeLog sl = new ScopeLog(LOGGER, Level.FINE, "MultiDensityView::SelectionRangeUpdated")) { //$NON-NLS-1$
            TmfComparisonFilteringUpdatedSignal rangUpdateSignal = new TmfComparisonFilteringUpdatedSignal(this, fStartTimeA, fEndTimeA, fStartTimeB, fEndTimeB, null, null, null);
            TmfSignalManager.dispatchSignal(rangUpdateSignal);
            Display.getDefault().syncExec(() -> {
                if (ftextQuery != null) {
                    ftextQuery.setText(buildComparisonQuery());
                }
            });
            buildDifferentialFlameGraph();
        }
    }

    /**
     * Get the statistic type
     *
     * @return fStatistic (Duration or Self time) which will be represented in
     *         the flame graph
     */
    public String getStatisticType() {
        return fStatistic;

    }

    private Action getAggregateByAction() {
        Action configureStatisticAction = new Action(Messages.flameGraphViewGroupByName, IAction.AS_DROP_DOWN_MENU) {
        };
        configureStatisticAction.setToolTipText(Messages.flameGraphViewStatisticTooltip);
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

                Action statisticActionDur = createStatisticAction(Objects.requireNonNull(Messages.multipleDensityViewDuration));
                new ActionContributionItem(statisticActionDur).fill(menu, -1);

                Action statisticActionSelf = createStatisticAction(Objects.requireNonNull(Messages.multipleDensityViewSelfTime));
                new ActionContributionItem(statisticActionSelf).fill(menu, -1);
                return menu;

            }
        });

        return Objects.requireNonNull(configureStatisticAction);
    }

    /**
     * @param signal
     *            the TmfCheckboxChangedSignal signal
     */
    @TmfSignalHandler
    public void checkBoxUpdated(TmfCheckboxChangedSignal signal) {
        try (ScopeLog sl = new ScopeLog(LOGGER, Level.FINE, "MultiDensityView::CheckBoxUpdated")) {//$NON-NLS-1$
            TmfComparisonFilteringUpdatedSignal rangUpdateSignal = null;

            if (signal.getSource() == fChartViewerA) {
                fTraceListA.clear();
                for (String name : signal.getTraceList()) {
                    fTraceListA.add(name);
                }

                rangUpdateSignal = new TmfComparisonFilteringUpdatedSignal(this, null, fTraceListA, null);
                TmfSignalManager.dispatchSignal(rangUpdateSignal);
            }

            if (signal.getSource() == fChartViewerB) {
                fTraceListB.clear();
                for (String name : signal.getTraceList()) {
                    fTraceListB.add(name);
                }

                rangUpdateSignal = new TmfComparisonFilteringUpdatedSignal(this, null, null, fTraceListB);
                TmfSignalManager.dispatchSignal(rangUpdateSignal);

            }
            if (ftextQuery != null) {
                ftextQuery.setText(buildComparisonQuery());
            }
            buildDifferentialFlameGraph();
        }

    }

    @Override
    public void handleCheckStateChangedEvent(Collection<ITmfTreeViewerEntry> entries) {
        // do nothing
    }

    @Override
    public void dispose() {
        super.dispose();
        TmfSignalManager.deregister(this);

        if (fChartViewerA != null) {
            fChartViewerA.dispose();
        }
        if (fTmfViewerA != null) {
            fTmfViewerA.dispose();
        }
        if (fChartViewerB != null) {
            fChartViewerB.dispose();
        }
        if (fTmfViewerB != null) {
            fTmfViewerB.dispose();
        }
        if (fXYViewerContainerA != null) {
            fXYViewerContainerA.dispose();
        }
        if (fXYViewerContainerB != null) {
            fXYViewerContainerB.dispose();
        }
        if (fContextService != null) {
            fContextService.dispose();
        }
        if (fSashFormLeftChildA != null) {
            fSashFormLeftChildA.dispose();
        }
        if (fSashFormLeftChildB != null) {
            fSashFormLeftChildB.dispose();
        }
        if (fsashForm != null) {
            fsashForm.dispose();
        }
    }

    /*
     * Constructs a query string based on the current state of selected traces
     * and time ranges
     */
    private String buildComparisonQuery() {
        StringBuilder query = new StringBuilder();
        /// Query PartA
        query.append(TRACE_NAME);
        for (String trace : fTraceListA) {
            if (!trace.equals("Total")) { //$NON-NLS-1$
                query.append(trace);
                query.append(","); //$NON-NLS-1$
            }
        }
        query.append(System.lineSeparator());

        query.append(Messages.multipleDensityViewFrom);
        query.append(fStartTimeA.toString(fFormat));
        query.append(System.lineSeparator());

        query.append(Messages.multipleDensityViewTo);
        query.append(fEndTimeA.toString(fFormat));
        query.append(System.lineSeparator());

        query.append(Messages.multipleDensityViewQueryCompare);
        query.append(System.lineSeparator());

        /// Query PartB
        query.append(TRACE_NAME);
        for (String trace : fTraceListB) {
            if (!trace.equals("Total")) { //$NON-NLS-1$
                query.append(trace);
                query.append(","); //$NON-NLS-1$
            }
        }
        query.append(System.lineSeparator());

        query.append(Messages.multipleDensityViewFrom);
        query.append(fStartTimeB.toString(fFormat));
        query.append(System.lineSeparator());

        query.append(Messages.multipleDensityViewTo);
        query.append(fEndTimeB.toString(fFormat));
        query.append(System.lineSeparator());

        //// Query Statistic Part
        query.append(STATISTIC_NAME);
        query.append(fStatistic);

        return query.toString();
    }

    /*
     * Parses a structured text query into trace and time selection parameters
     */
    boolean parseComparisonQuery(String query) {
        try {
            String lineSeparator = System.lineSeparator();
            if (lineSeparator != null) {
                String[] parts = query.split(lineSeparator);
                // Times

                if (parts[1].indexOf(Messages.multipleDensityViewFrom) == -1) {
                    return false;
                }
                String fromStrA = parts[1].substring(parts[1].indexOf(Messages.multipleDensityViewFrom) + Messages.multipleDensityViewFrom.length(), parts[1].length());
                fStartTimeA = TmfTimestamp.fromNanos(fFormat.parseValue(fromStrA));

                if (parts[2].indexOf(Messages.multipleDensityViewTo) == -1) {
                    return false;
                }
                String toStrA = parts[2].substring(parts[2].indexOf(Messages.multipleDensityViewTo) + Messages.multipleDensityViewTo.length(), parts[2].length());
                fEndTimeA = TmfTimestamp.fromNanos(fFormat.parseValue(toStrA));

                if (parts[5].indexOf(Messages.multipleDensityViewFrom) == -1) {
                    return false;
                }
                String fromStrB = parts[5].substring(parts[5].indexOf(Messages.multipleDensityViewFrom) + Messages.multipleDensityViewFrom.length(), parts[5].length());
                fStartTimeB = TmfTimestamp.fromNanos(fFormat.parseValue(fromStrB));

                if (parts[6].indexOf(Messages.multipleDensityViewTo) == -1) {
                    return false;
                }
                String toStrB = parts[6].substring(parts[6].indexOf(Messages.multipleDensityViewTo) + Messages.multipleDensityViewTo.length(), parts[6].length());
                fEndTimeB = TmfTimestamp.fromNanos(fFormat.parseValue(toStrB));

                // traceListA
                if (parts[0].indexOf(TRACE_NAME) == -1) {
                    return false;
                }
                String traceStrtA = parts[0].substring(parts[0].indexOf(TRACE_NAME) + TRACE_NAME.length(), parts[0].length());
                String[] traces = traceStrtA.split(","); //$NON-NLS-1$

                parseAndAddTraces(fTraceListA, traces);

                // traceListB
                String traceStrtB = parts[4].substring(parts[4].indexOf(TRACE_NAME) + TRACE_NAME.length(), parts[4].length());
                String[] tracesB = traceStrtB.split(","); //$NON-NLS-1$

                parseAndAddTraces(fTraceListB, tracesB);

                //// Statistic
                fStatistic = parts[7].substring(parts[7].indexOf(STATISTIC_NAME) + STATISTIC_NAME.length(), parts[7].length());

                // Set time range related objects
                ftextAFrom.setText(fStartTimeA.toString(fFormat));
                ftextATo.setText(fEndTimeA.toString(fFormat));

                ftextBFrom.setText(fStartTimeB.toString(fFormat));
                ftextBTo.setText(fEndTimeB.toString(fFormat));
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static void parseAndAddTraces(List<String> fTraceList, String[] traces) {
        fTraceList.clear();
        for (String trace : traces) {
            if (trace != null) {
                fTraceList.add(trace);
            }
        }
    }

}
