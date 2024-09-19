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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.analysis.profiling.core.callgraph.ICallGraphProvider2;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLogBuilder;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.ScopeLog;
import org.eclipse.tracecompass.incubator.analysis.core.weighted.tree.diff.DifferentialWeightedTreeProvider;
import org.eclipse.tracecompass.incubator.internal.executioncomparison.core.DifferentialSeqCallGraphAnalysis;
import org.eclipse.tracecompass.internal.analysis.profiling.core.flamegraph.FlameGraphDataProvider;
import org.eclipse.tracecompass.internal.analysis.profiling.core.instrumented.FlameChartEntryModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.TmfFilterAppliedSignal;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.TraceCompassFilter;
import org.eclipse.tracecompass.internal.provisional.tmf.ui.widgets.timegraph.BaseDataProviderTimeGraphPresentationProvider;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.CoreFilterProperty;
import org.eclipse.tracecompass.tmf.core.model.IOutputElement;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.TmfUiRefreshHandler;
import org.eclipse.tracecompass.tmf.ui.editors.ITmfTraceEditor;
import org.eclipse.tracecompass.tmf.ui.symbols.TmfSymbolProviderUpdatedSignal;
import org.eclipse.tracecompass.tmf.ui.views.SaveImageUtil;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphViewer;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NamedTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry.Sampling;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphControl;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils.TimeFormat;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 * The differential flame graph used in the execution comparison view. It is
 * based on the flame graph view and is used to compare two different
 * executions.
 *
 * @author Fateme Faraji Daneshgar and Vlad Arama
 *
 */
@SuppressWarnings("restriction")
public class DifferentialFlameGraphView extends TmfView {

    /**
     * ID of the view
     */
    public static final String ID = DifferentialFlameGraphView.class.getPackage().getName() + ".diffflamegraphView"; //$NON-NLS-1$
    private static final int DEFAULT_BUFFER_SIZE = 3;
    private static final String DIRTY_UNDERFLOW = "Dirty underflow"; //$NON-NLS-1$
    /**
     * the Logger that is used in multipleDensityView class
     */
    protected static final Logger LOGGER = Logger.getLogger(DifferentialFlameGraphView.class.getName());

    private @Nullable DifferentialWeightedTreeProvider<?> fDataProvider;

    private @Nullable TimeGraphViewer fTimeGraphViewer;

    private @Nullable BaseDataProviderTimeGraphPresentationProvider fPresentationProvider;

    private @Nullable ITmfTrace fTrace = null;

    /**
     * A plain old semaphore is used since different threads will be competing
     * for the same resource.
     */
    private final Semaphore fLock = new Semaphore(1);

    private final AtomicInteger fDirty = new AtomicInteger();

    /** The trace to build thread hash map */
    private final Map<ITmfTrace, Job> fBuildJobMap = new HashMap<>();
    private final Map<ITimeGraphDataProvider<? extends TimeGraphEntryModel>, Map<Long, TimeGraphEntry>> fEntries = new HashMap<>();

    /**
     * Set of visible entries to zoom on.
     */
    private Set<TimeGraphEntry> fVisibleEntries = Collections.emptySet();

    private long fEndTime = Long.MIN_VALUE;

    /** The trace to entry list hash map */
    private final Map<ITmfTrace, List<TimeGraphEntry>> fEntryListMap = new HashMap<>();

    private int fDisplayWidth;
    private @Nullable ZoomThread fZoomThread;
    private final Object fZoomThreadResultLock = new Object();
    private Semaphore fBuildEntryLock = new Semaphore(1);

    /**
     * Constructor
     */
    public DifferentialFlameGraphView() {
        this(ID);

    }

    /**
     * Constructor with ID
     *
     * @param id
     *            The ID of the view
     */
    protected DifferentialFlameGraphView(String id) {
        super(id);
    }

    @Override
    public void createPartControl(@Nullable Composite parent) {
        super.createPartControl(parent);
        fDisplayWidth = Display.getDefault().getBounds().width;
        TimeGraphViewer timeGraphViewer = new TimeGraphViewer(parent, SWT.NONE);
        fPresentationProvider = new BaseDataProviderTimeGraphPresentationProvider();
        timeGraphViewer.setTimeGraphProvider(fPresentationProvider);
        timeGraphViewer.setTimeFormat(TimeFormat.NUMBER);
        IEditorPart editor = getSite().getPage().getActiveEditor();
        ITmfTrace trace = getCurrentTrace(editor);
        if (trace != null) {
            traceSelected(new TmfTraceSelectedSignal(this, trace));
        }
        TmfSignalManager.register(this);
        getSite().setSelectionProvider(timeGraphViewer.getSelectionProvider());
        timeGraphViewer.getTimeGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(@Nullable MouseEvent e) {
                handleDoubleClick(timeGraphViewer);
            }
        });
        timeGraphViewer.addRangeListener(event -> startZoomThread(event.getStartTime(), event.getEndTime(), false, timeGraphViewer));
        TimeGraphControl timeGraphControl = timeGraphViewer.getTimeGraphControl();
        timeGraphControl.addPaintListener(new PaintListener() {

            /**
             * This paint control allows the virtual time graph refresh to occur
             * on paint events instead of just scrolling the time axis or
             * zooming. To avoid refreshing the model on every paint event, we
             * use a TmfUiRefreshHandler to coalesce requests and only execute
             * the last one, we also check if the entries have changed to avoid
             * useless model refresh.
             *
             * @param e
             *            paint event on the visible area
             */
            @Override
            public void paintControl(@Nullable PaintEvent e) {
                TmfUiRefreshHandler.getInstance().queueUpdate(this, () -> {
                    if (timeGraphControl.isDisposed()) {
                        return;
                    }
                    Set<TimeGraphEntry> newSet = getVisibleItems(DEFAULT_BUFFER_SIZE, timeGraphViewer);
                    if (!fVisibleEntries.equals(newSet)) {
                        /*
                         * Start a zoom thread if the set of visible entries has
                         * changed. We do not use lists as the order is not
                         * important. We cannot use the start index / size of
                         * the visible entries as we can collapse / reorder
                         * events.
                         */
                        fVisibleEntries = newSet;
                        startZoomThread(timeGraphViewer.getTime0(), timeGraphViewer.getTime1(), false, timeGraphViewer);
                    }
                });
            }
        });
        fTimeGraphViewer = timeGraphViewer;
    }

    private static void handleDoubleClick(TimeGraphViewer timeGraphViewer) {
        TimeGraphControl timeGraphControl = timeGraphViewer.getTimeGraphControl();
        ISelection selection = timeGraphControl.getSelection();
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            for (Object object : (structuredSelection).toList()) {
                if (object instanceof TimeEvent) {
                    TimeEvent event = (TimeEvent) object;
                    long startTime = event.getTime();
                    long endTime = startTime + event.getDuration();
                    timeGraphViewer.setStartFinishTime(startTime, endTime);
                    break;
                }
            }
        }
    }

    private static @Nullable ITmfTrace getCurrentTrace(@Nullable IEditorPart editor) {
        ITmfTrace trace = null;
        if (editor instanceof ITmfTraceEditor) {
            trace = ((ITmfTraceEditor) editor).getTrace();
        } else {
            // Get the active trace, the editor might be opened on a script
            trace = TmfTraceManager.getInstance().getActiveTrace();
        }
        return trace;
    }

    /**
     * Handler for the trace selected signal
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public void traceSelected(final TmfTraceSelectedSignal signal) {
        try (ScopeLog sl = new ScopeLog(LOGGER, Level.FINE, "DifferentialFlameGraphView::traceSelected")) { //$NON-NLS-1$
            ITmfTrace trace = signal.getTrace();

            fTrace = trace;
            if (trace == null) {
                return;
            }
            /*
             * If entries for this trace are already available, just zoom on
             * them, otherwise, rebuild
             */
            List<TimeGraphEntry> list = fEntryListMap.get(trace);
            if (list == null) {
                refresh();
                Display.getDefault().asyncExec(() -> buildFlameGraph(trace, null, null));

            } else {
                // Reset end time
                long endTime = Long.MIN_VALUE;
                for (TimeGraphEntry entry : list) {
                    endTime = Math.max(endTime, entry.getEndTime());
                }
                fEndTime = endTime;
                refresh();
                if (fTimeGraphViewer != null) {
                    startZoomThread(0, endTime, false, fTimeGraphViewer);
                }
            }
        }
    }

    /**
     * Get the callgraph modules used to build the view
     *
     * @return The call graph provider modules
     */
    protected Iterable<ICallGraphProvider2> getCallgraphModules() {
        ITmfTrace trace = fTrace;
        if (trace == null) {
            return Collections.emptyList();
        }
        String analysisId = NonNullUtils.nullToEmptyString(getViewSite().getSecondaryId());
        Iterable<ICallGraphProvider2> modules = TmfTraceUtils.getAnalysisModulesOfClass(trace, ICallGraphProvider2.class);
        return StreamSupport.stream(modules.spliterator(), false)
                .filter(m -> {
                    if (m instanceof IAnalysisModule) {
                        return ((IAnalysisModule) m).getId().equals(analysisId);
                    }
                    return true;
                })
                .collect(Collectors.toSet());
    }

    private class BuildRunnable {
        private final ITmfTrace fBuildTrace;
        private final ITmfTrace fParentTrace;
        private final FlowScopeLog fScope;
        private final Map<String, Object> fParameters;

        public BuildRunnable(final ITmfTrace trace, final ITmfTrace parentTrace, @Nullable ITmfTimestamp selStart, @Nullable ITmfTimestamp selEnd, final FlowScopeLog log) {
            fBuildTrace = trace;
            fParentTrace = parentTrace;
            fScope = log;
            if (selStart != null && selEnd != null) {
                fParameters = ImmutableMap.of(FlameGraphDataProvider.SELECTION_RANGE_KEY, ImmutableList.of(selStart.toNanos(), selEnd.toNanos()));
            } else {
                fParameters = Collections.emptyMap();
            }
        }

        public void run(IProgressMonitor monitor) {
            try (FlowScopeLog log = new FlowScopeLogBuilder(LOGGER, Level.FINE, "FlameGraphView:BuildThread", "trace", fBuildTrace.getName()).setParentScope(fScope).build()) { //$NON-NLS-1$ //$NON-NLS-2$
                buildEntryList(fBuildTrace, fParentTrace, fParameters, Objects.requireNonNull(monitor));
                synchronized (fBuildJobMap) {
                    fBuildJobMap.remove(fBuildTrace);
                }
            }
        }

        private void buildEntryList(@Nullable ITmfTrace trace, ITmfTrace parentTrace, Map<String, Object> additionalParams, IProgressMonitor monitor) {

            if (trace != null) {
                DifferentialWeightedTreeProvider<?> dataProvider = getDataProvider(monitor);
                if (dataProvider == null) {
                    return;
                }
                fDataProvider = dataProvider;
                ITimeGraphDataProvider<@NonNull FlameChartEntryModel> dataProviderGroup = new FlameGraphDataProvider<>(trace, fDataProvider, FlameGraphDataProvider.ID + ':' + DifferentialSeqCallGraphAnalysis.ID);
                BaseDataProviderTimeGraphPresentationProvider presentationProvider = fPresentationProvider;
                if (presentationProvider != null) {
                    presentationProvider.addProvider(dataProviderGroup, getTooltipResolver(dataProviderGroup));
                }
                fetchAndBuildEntries(trace, parentTrace, additionalParams, monitor, dataProviderGroup);

            }
        }

        private void fetchAndBuildEntries(@Nullable ITmfTrace trace, ITmfTrace parentTrace, Map<String, Object> additionalParams, IProgressMonitor monitor, ITimeGraphDataProvider<@NonNull FlameChartEntryModel> dataProviderGroup) {
            try {
                fBuildEntryLock.acquire();
                boolean complete = false;
                while (!complete && !monitor.isCanceled()) {
                    Map<String, Object> parameters = new HashMap<>(additionalParams);
                    parameters.put(DataProviderParameterUtils.REQUESTED_TIME_KEY, ImmutableList.of(0, Long.MAX_VALUE));
                    TmfModelResponse<TmfTreeModel<@NonNull FlameChartEntryModel>> responseGroupA = dataProviderGroup.fetchTree(parameters, monitor);

                    if (responseGroupA.getStatus() == ITmfResponse.Status.FAILED) {
                        Activator.getDefault().getLog().error(getClass().getSimpleName() + " Data Provider failed: " + responseGroupA.getStatusMessage()); //$NON-NLS-1$
                        return;
                    } else if (responseGroupA.getStatus() == ITmfResponse.Status.CANCELLED) {
                        return;
                    }

                    complete = responseGroupA.getStatus() == ITmfResponse.Status.COMPLETED;
                    TmfTreeModel<@NonNull FlameChartEntryModel> groupAModel = responseGroupA.getModel();
                    if ((groupAModel != null)) {
                        processAndDisplayEntries(groupAModel, parentTrace, monitor, dataProviderGroup);

                    }

                    if (monitor.isCanceled()) {
                        if (trace == null) {
                            return;
                        }
                        resetEntries(trace);
                        return;
                    }

                    if (parentTrace.equals(getTrace())) {
                        refresh();
                    }
                    monitor.worked(1);

                    if (!complete && !monitor.isCanceled()) {
                        waitForDataProvider();
                    }

                }
            } catch (InterruptedException e1) {
                Activator.getDefault().getLog().error(e1.getMessage());
            } finally {
                fBuildEntryLock.release();
            }
        }

        private void processAndDisplayEntries(TmfTreeModel<@NonNull FlameChartEntryModel> groupAModel, ITmfTrace parentTrace, IProgressMonitor monitor, ITimeGraphDataProvider<@NonNull FlameChartEntryModel> dataProviderGroup) {
            Map<Long, TimeGraphEntry> entries;
            synchronized (fEntries) {
                entries = fEntries.computeIfAbsent(dataProviderGroup, dp -> new HashMap<>());
                /*
                 * The provider may send entries unordered and parents may not
                 * exist when child is constructor, we'll re-unite families at
                 * the end
                 */
                List<TimeGraphEntry> orphaned = new ArrayList<>();
                for (TimeGraphEntryModel entry : groupAModel.getEntries()) {
                    if (entry.getParentId() != -1) {
                        updateOrCreateOrphanedEntry(entry, entries, orphaned, monitor);
                    } else {
                        updateOrCreateEntry(entry, entries, parentTrace, monitor, dataProviderGroup);
                    }
                }
                findMissingParents(entries, orphaned);
            }
            long start = 0;
            final long resolutionN = Long.max(1, (fEndTime - start) / getDisplayWidth());

            if (!monitor.isCanceled()) {
                zoomEntries(ImmutableList.copyOf(entries.values()), start, fEndTime, resolutionN, monitor);
            }
        }

        private void updateOrCreateOrphanedEntry(TimeGraphEntryModel entry, Map<Long, TimeGraphEntry> entries, List<TimeGraphEntry> orphaned, IProgressMonitor monitor) {
            TimeGraphEntry uiEntry = entries.get(entry.getId());
            if (uiEntry == null) {
                uiEntry = new TimeGraphEntry(entry);
                TimeGraphEntry parent = entries.get(entry.getParentId());
                if (parent != null) {
                    parent.addChild(uiEntry);
                } else {
                    orphaned.add(uiEntry);
                }
                entries.put(entry.getId(), uiEntry);
            } else {
                if (!monitor.isCanceled()) {
                    uiEntry.updateModel(entry);
                }
            }
        }

        private void updateOrCreateEntry(TimeGraphEntryModel entry, Map<Long, TimeGraphEntry> entries, ITmfTrace parentTrace, IProgressMonitor monitor, ITimeGraphDataProvider<@NonNull FlameChartEntryModel> dataProviderGroup) {
            long endTimeN = fEndTime;
            TimeGraphEntry uiEntry = entries.get(entry.getId());
            fEndTime = Long.max(endTimeN, entry.getEndTime() + 1);
            List<String> lables = new ArrayList<>();
            lables.add("GroupB-GroupA"); //$NON-NLS-1$

            TimeGraphEntryModel newEntry = new TimeGraphEntryModel(0, -1, lables, entry.getStartTime(), entry.getEndTime(), entry.hasRowModel());

            if (uiEntry != null) {
                if (!monitor.isCanceled()) {
                    uiEntry.updateModel(newEntry);
                }
            } else {
                // Do not assume that parentless entries are trace entries
                uiEntry = new ParentEntry(newEntry, dataProviderGroup);
                entries.put(entry.getId(), uiEntry);
                addToEntryList(parentTrace, Collections.singletonList(uiEntry));

            }
        }

        private void findMissingParents(Map<Long, TimeGraphEntry> entries, List<TimeGraphEntry> orphaned) {
            for (TimeGraphEntry orphanedEntry : orphaned) {
                TimeGraphEntry parent = entries.get(orphanedEntry.getEntryModel().getParentId());
                if (parent != null) {
                    parent.addChild(orphanedEntry);
                }
            }
        }

        private void waitForDataProvider() throws InterruptedException {
            try {
                Thread.sleep(100);

            } catch (InterruptedException e) {
                Activator.getDefault().getLog().error("Failed to wait for data provider", e); //$NON-NLS-1$
                throw e;
            }
        }

        private int getDisplayWidth() {
            int displayWidth = fDisplayWidth;
            return displayWidth <= 0 ? 1 : displayWidth;
        }

        /**
         * Adds a list of entries to a trace's entry list
         *
         * @param trace
         *            the trace
         * @param list
         *            the list of time graph entries to add
         */
        private void addToEntryList(ITmfTrace trace, List<TimeGraphEntry> list) {
            synchronized (fEntryListMap) {
                List<TimeGraphEntry> entryList = fEntryListMap.get(trace);
                if (entryList == null) {
                    fEntryListMap.put(trace, new CopyOnWriteArrayList<>(list));
                } else {
                    for (TimeGraphEntry entry : list) {
                        if (!entryList.contains(entry)) {
                            entryList.add(entry);
                        }
                    }
                }
            }
        }
    }

    private static BiFunction<ITimeEvent, Long, Map<String, String>> getTooltipResolver(ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider) {
        return (event, time) -> getTooltip(event, time, provider, false);
    }

    private static Map<String, String> getTooltip(ITimeEvent event, Long time, ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider, boolean getActions) {
        ITimeGraphEntry entry = event.getEntry();

        if (!(entry instanceof TimeGraphEntry)) {
            return Collections.emptyMap();
        }
        long entryId = ((TimeGraphEntry) entry).getEntryModel().getId();
        IOutputElement element = null;
        if (event instanceof TimeEvent) {
            element = ((TimeEvent) event).getModel();
        }
        Map<String, Object> parameters = getFetchTooltipParameters(time, entryId, element);
        if (getActions) {
            parameters.put(FlameGraphDataProvider.TOOLTIP_ACTION_KEY, true);
        }
        TmfModelResponse<Map<String, String>> response = provider.fetchTooltip(parameters, new NullProgressMonitor());
        Map<String, String> tooltip = response.getModel();
        return (tooltip == null) ? Collections.emptyMap() : tooltip;
    }

    private static Map<String, Object> getFetchTooltipParameters(long time, long item, @Nullable IOutputElement element) {
        @NonNull
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(DataProviderParameterUtils.REQUESTED_TIME_KEY, Collections.singletonList(time));
        parameters.put(DataProviderParameterUtils.REQUESTED_ITEMS_KEY, Collections.singletonList(item));
        if (element != null) {
            parameters.put(DataProviderParameterUtils.REQUESTED_ELEMENT_KEY, element);
        }
        return parameters;
    }

    /**
     * The ZoomThread class is responsible for performing zoom operations on a
     * collection of TimeGraphEntry objects. It is a thread that runs in the
     * background and performs the zoom operation asynchronously.
     */

    protected class ZoomThread extends Thread {
        private final long fZoomStartTime;
        private final long fZoomEndTime;
        private final long fResolution;
        private int fScopeId = -1;
        private final IProgressMonitor fMonitor;
        private Collection<TimeGraphEntry> fCurrentEntries;
        private boolean fForce;

        /**
         * Constructor
         *
         * @param entries
         *            The entries to zoom on
         * @param startTime
         *            the start time
         * @param endTime
         *            the end time
         * @param resolution
         *            the resolution
         * @param force
         *            Whether to force the zoom of all entries or only those
         *            that have not the same sampling
         */
        public ZoomThread(Collection<TimeGraphEntry> entries, long startTime, long endTime, long resolution, boolean force) {
            super(DifferentialFlameGraphView.this.getName() + " zoom"); //$NON-NLS-1$
            fZoomStartTime = startTime;
            fZoomEndTime = endTime;
            fResolution = resolution;
            fCurrentEntries = entries;
            fMonitor = new NullProgressMonitor();
            fForce = force;
        }

        /**
         * Cancel the zoom thread
         */
        public void cancel() {
            fMonitor.setCanceled(true);
        }

        @Override
        public final void run() {
            try (FlowScopeLog log = new FlowScopeLogBuilder(LOGGER, Level.FINE, "FlameGraphView:ZoomThread", "start", fZoomStartTime, "end", fZoomEndTime).setCategoryAndId(getViewId(), fScopeId).build()) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                if (fCurrentEntries.isEmpty()) {
                    // No rows to zoom on
                    return;
                }
                Sampling sampling = new Sampling(fZoomStartTime, fZoomEndTime, fResolution);
                Iterable<TimeGraphEntry> incorrectSample = fForce ? fCurrentEntries : fCurrentEntries.stream().filter(entry -> !sampling.equals(entry.getSampling())).collect(Collectors.toList());
                Objects.requireNonNull(incorrectSample);
                zoomEntries(incorrectSample, fZoomStartTime, fZoomEndTime, fResolution, fMonitor);
            } finally {

                if (fDirty.decrementAndGet() < 0) {
                    Activator.getDefault().getLog().error(DIRTY_UNDERFLOW, new Throwable());
                }
            }
        }

        /**
         * Set the ID of the calling flow scope. This data will allow to
         * determine the causality between the zoom thread and its caller if
         * tracing is enabled.
         *
         * @param scopeId
         *            The ID of the calling flow scope
         * @since 3.0
         */
        public void setScopeId(int scopeId) {
            fScopeId = scopeId;
        }
    }

    /**
     * Start or restart the zoom thread.
     *
     * @param startTime
     *            the zoom start time
     * @param endTime
     *            the zoom end time
     * @param force
     *            Whether to force the fetch of all rows, or only those that
     *            don't have the same range
     * @param timeGraphViewer
     *            The TimeGraphViewer
     */
    protected final void startZoomThread(long startTime, long endTime, boolean force, TimeGraphViewer timeGraphViewer) {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return;
        }

        fDirty.incrementAndGet();
        try (FlowScopeLog log = new FlowScopeLogBuilder(LOGGER, Level.FINE, "FlameGraphView:ZoomThreadCreated").setCategory(getViewId()).build()) { //$NON-NLS-1$
            long clampedStartTime = Math.max(0, Math.min(startTime, fEndTime));
            long clampedEndTime = Math.min(fEndTime, Math.max(endTime, 0));
            // Ignore if end time < start time, data has not been set correctly
            // [yet]
            if (clampedEndTime < clampedStartTime) {
                return;
            }
            ZoomThread zoomThread = fZoomThread;
            if (zoomThread != null) {
                zoomThread.cancel();
            }
            int timeSpace = timeGraphViewer.getTimeSpace();
            if (timeSpace > 0) {
                long resolution = Long.max(1, (clampedEndTime - clampedStartTime) / timeSpace);
                zoomThread = new ZoomThread(getVisibleItems(DEFAULT_BUFFER_SIZE, timeGraphViewer), clampedStartTime, clampedEndTime, resolution, force);
            } else {
                zoomThread = null;
            }
            fZoomThread = zoomThread;
            if (zoomThread != null) {
                zoomThread.setScopeId(log.getId());
                /*
                 * Don't start a new thread right away if results are being
                 * applied from an old ZoomThread. Otherwise, the old results
                 * might overwrite the new results if it finishes after.
                 */
                synchronized (fZoomThreadResultLock) {
                    zoomThread.start();
                    // zoomThread decrements, so we increment here
                    fDirty.incrementAndGet();
                }
            }
        } finally {
            if (fDirty.decrementAndGet() < 0) {
                Activator.getDefault().getLog().error(DIRTY_UNDERFLOW, new Throwable());
            }
        }
    }

    private static Set<TimeGraphEntry> getVisibleItems(int buffer, TimeGraphViewer timeGraphViewer) {
        TimeGraphControl timeGraphControl = timeGraphViewer.getTimeGraphControl();
        if (timeGraphControl.isDisposed()) {
            return Collections.emptySet();
        }

        int start = Integer.max(0, timeGraphViewer.getTopIndex() - buffer);
        int end = Integer.min(timeGraphViewer.getExpandedElementCount() - 1,
                timeGraphViewer.getTopIndex() + timeGraphControl.countPerPage() + buffer);

        Set<TimeGraphEntry> visible = new HashSet<>(end - start + 1);
        for (int i = start; i <= end; i++) {
            /*
             * Use the getExpandedElement by index to avoid creating a copy of
             * all the the elements.
             */
            TimeGraphEntry element = (TimeGraphEntry) timeGraphControl.getExpandedElement(i);
            if (element != null) {
                visible.add(element);
            }
        }
        return visible;
    }

    private void zoomEntries(Iterable<TimeGraphEntry> normalEntries, long zoomStartTime, long zoomEndTime, long resolution, IProgressMonitor monitor) {
        if ((resolution < 0)) {
            // StateSystemUtils.getTimes would throw an illegal argument
            // exception.
            return;
        }

        long start = Long.min(zoomStartTime, zoomEndTime);
        long end = Long.max(zoomStartTime, zoomEndTime);
        List<Long> times = StateSystemUtils.getTimes(start, end, resolution);

        Multimap<ITimeGraphDataProvider<? extends TimeGraphEntryModel>, Long> providersToModelIds = filterGroupEntries(normalEntries, zoomStartTime, zoomEndTime);
        if (providersToModelIds != null) {

            SubMonitor subMonitor = SubMonitor.convert(monitor, getClass().getSimpleName() + "#zoomEntries", providersToModelIds.size()); //$NON-NLS-1$

            Entry<ITimeGraphDataProvider<? extends TimeGraphEntryModel>, Collection<Long>> entry = providersToModelIds.asMap().entrySet().iterator().next();
            ITimeGraphDataProvider<? extends TimeGraphEntryModel> dataProvider = Objects.requireNonNull(entry.getKey());
            SelectionTimeQueryFilter filter = new SelectionTimeQueryFilter(times, entry.getValue());
            Map<String, Object> parameters = FetchParametersUtils.selectionTimeQueryToMap(filter);
            Multimap<Integer, String> regexesMap = getRegexes();
            if (regexesMap != null && !regexesMap.isEmpty()) {
                parameters.put(DataProviderParameterUtils.REGEX_MAP_FILTERS_KEY, Objects.requireNonNull(regexesMap.asMap()));
            }
            TmfModelResponse<TimeGraphModel> response = dataProvider.fetchRowModel(parameters, monitor);
            TimeGraphModel model = response.getModel();
            Map<Long, TimeGraphEntry> entries = fEntries.get(dataProvider);
            if ((model != null) && (entries) != null) {
                zoomEntries(entries, model.getRows());

            }
            subMonitor.worked(1);
            redraw();
        }
    }

    /**
     * Builds the multimap of regexes by the property that will be used to
     * filter the timegraph states
     *
     * Override this method to add other regexes with their properties. The data
     * provider should handle everything after.
     *
     * @return The multimap of regexes by property
     */
    private @Nullable Multimap<Integer, String> getRegexes() {
        Multimap<Integer, String> regexes = HashMultimap.create();
        if (regexes != null) {
            ITmfTrace trace = getTrace();
            if (trace == null) {
                return regexes;
            }
            TraceCompassFilter globalFilter = TraceCompassFilter.getFilterForTrace(trace);
            if (globalFilter == null) {
                return regexes;
            }
            regexes.putAll(CoreFilterProperty.DIMMED, globalFilter.getRegexes());
        }
        return regexes;
    }

    private void zoomEntries(Map<Long, TimeGraphEntry> map, List<ITimeGraphRowModel> model) {
        for (ITimeGraphRowModel rowModel : model) {
            TimeGraphEntry entry = map.get(rowModel.getEntryID());

            if (entry != null) {
                List<ITimeEvent> events = createTimeEvents(entry, rowModel.getStates());
                entry.setEventList(events);
            }
        }
    }

    /**
     * Create {@link ITimeEvent}s for an entry from the list of
     * {@link ITimeGraphState}s, filling in the gaps.
     *
     * @param entry
     *            the {@link TimeGraphEntry} on which we are working
     * @param values
     *            the list of {@link ITimeGraphState}s from the
     *            {@link ITimeGraphDataProvider}.
     * @return a contiguous List of {@link ITimeEvent}s
     */
    private List<ITimeEvent> createTimeEvents(TimeGraphEntry entry, List<ITimeGraphState> values) {
        List<ITimeEvent> events = new ArrayList<>(values.size());
        ITimeEvent prev = null;
        for (ITimeGraphState state : values) {
            ITimeEvent event = createTimeEvent(entry, state);
            if (prev != null) {
                long prevEnd = prev.getTime() + prev.getDuration();
                if (prevEnd < event.getTime()) {
                    // fill in the gap.
                    TimeEvent timeEvent = new TimeEvent(entry, prevEnd, event.getTime() - prevEnd);
                    events.add(timeEvent);
                }
            }
            prev = event;
            events.add(event);
        }
        return events;
    }

    /**
     * Create a {@link TimeEvent} for a {@link TimeGraphEntry} and a
     * {@link TimeGraphState}
     *
     * @param entry
     *            {@link TimeGraphEntry} for which we create a state
     * @param state
     *            {@link ITimeGraphState} from the data provider
     * @return a new {@link TimeEvent} for these arguments
     */
    protected TimeEvent createTimeEvent(TimeGraphEntry entry, ITimeGraphState state) {
        String label = state.getLabel();
        if (state.getValue() == Integer.MIN_VALUE && label == null && state.getStyle() == null) {
            return new NullTimeEvent(entry, state.getStartTime(), state.getDuration());
        }
        if (label != null) {
            return new NamedTimeEvent(entry, label, state);
        }
        return new TimeEvent(entry, state);
    }

    /**
     * Filter the entries to return only the Non Null {@link TimeGraphEntry}
     * which intersect the time range.
     *
     * @param visible
     *            the input list of visible entries
     * @param zoomStartTime
     *            the leftmost time bound of the view
     * @param zoomEndTime
     *            the rightmost time bound of the view
     * @return A Multimap of data providers to their visible entries' model IDs.
     */
    private static @Nullable Multimap<ITimeGraphDataProvider<? extends TimeGraphEntryModel>, Long> filterGroupEntries(Iterable<TimeGraphEntry> visible,
            long zoomStartTime, long zoomEndTime) {
        Multimap<ITimeGraphDataProvider<? extends TimeGraphEntryModel>, Long> providersToModelIds = HashMultimap.create();
        for (TimeGraphEntry entry : visible) {
            if (zoomStartTime <= entry.getEndTime() && zoomEndTime >= entry.getStartTime() && entry.hasTimeEvents()) {
                ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider = getProvider(entry);
                providersToModelIds.put(provider, entry.getEntryModel().getId());
            }
        }
        return providersToModelIds;
    }

    /**
     * Get the {@link ITimeGraphDataProvider} from a {@link TimeGraphEntry}'s
     * parent.
     *
     * @param entry
     *            queried {@link TimeGraphEntry}.
     * @return the {@link ITimeGraphDataProvider}
     */
    public static ITimeGraphDataProvider<? extends TimeGraphEntryModel> getProvider(ITimeGraphEntry entry) {
        ITimeGraphEntry parent = entry;
        while (parent != null) {
            if (parent instanceof ParentEntry) {
                return ((ParentEntry) parent).getProvider();
            }
            parent = parent.getParent();
        }
        throw new IllegalStateException(entry + " should have a TraceEntry parent"); //$NON-NLS-1$
    }

    /**
     * Get the trace associated with this view
     *
     * @return The trace
     */
    protected @Nullable ITmfTrace getTrace() {
        return fTrace;
    }

    private void refresh() {
        try (FlowScopeLog parentLogger = new FlowScopeLogBuilder(LOGGER, Level.FINE, "FlameGraphView:RefreshRequested").setCategory(getViewId()).build()) { //$NON-NLS-1$
            final boolean isZoomThread = Thread.currentThread() instanceof ZoomThread;
            TmfUiRefreshHandler.getInstance().queueUpdate(this, () -> {
                try (FlowScopeLog log = new FlowScopeLogBuilder(LOGGER, Level.FINE, "FlameGraphView:Refresh").setParentScope(parentLogger).build()) { //$NON-NLS-1$
                    fDirty.incrementAndGet();
                    if (fTimeGraphViewer != null && fTimeGraphViewer.getControl().isDisposed()) {
                        return;
                    }
                    List<TimeGraphEntry> entries;
                    synchronized (fEntryListMap) {
                        entries = fEntryListMap.get(getTrace());
                        if (entries == null) {
                            entries = new CopyOnWriteArrayList<>();
                        }
                    }

                    if (fTimeGraphViewer != null) {
                        boolean inputChanged = entries != fTimeGraphViewer.getInput();
                        if (inputChanged && fTimeGraphViewer != null) {
                            fTimeGraphViewer.setInput(entries);
                        } else if (!inputChanged && fTimeGraphViewer != null) {
                            fTimeGraphViewer.refresh();
                        } else {
                            return;
                        }
                        long startBound = 0;
                        long endBound = fEndTime;
                        endBound = (endBound == Long.MIN_VALUE ? SWT.DEFAULT : endBound);
                        if (fTimeGraphViewer != null) {
                            fTimeGraphViewer.setTimeBounds(startBound, endBound);
                        }

                        if (inputChanged && !isZoomThread && fTimeGraphViewer != null) {
                            fTimeGraphViewer.resetStartFinishTime();
                        }
                    }

                } finally {
                    if (fDirty.decrementAndGet() < 0) {
                        Activator.getDefault().getLog().error(DIRTY_UNDERFLOW, new Throwable());
                    }
                }
            });
        }
    }

    private void redraw() {
        try (FlowScopeLog flowParent = new FlowScopeLogBuilder(LOGGER, Level.FINE, "FlameGraphView:RedrawRequested").setCategory(getViewId()).build()) { //$NON-NLS-1$
            Display.getDefault().asyncExec(() -> {
                try (FlowScopeLog log = new FlowScopeLogBuilder(LOGGER, Level.FINE, "FlameGraphView:Redraw").setParentScope(flowParent).build()) { //$NON-NLS-1$
                    if (fTimeGraphViewer != null && fTimeGraphViewer.getControl().isDisposed()) {
                        return;
                    }
                    if (fTimeGraphViewer != null) {
                        fTimeGraphViewer.getControl().redraw();
                        if (fTimeGraphViewer != null) {
                            fTimeGraphViewer.getControl().update();
                        }
                    }
                }
            });
        }
    }

    /**
     * A class for parent entries that contain a link to the data provider
     *
     * @author Geneviève Bastien
     */
    private static class ParentEntry extends TimeGraphEntry {
        private final ITimeGraphDataProvider<? extends TimeGraphEntryModel> fProvider;

        /**
         * Constructor
         *
         * @param model
         *            trace level model
         * @param provider
         *            reference to the provider for this trace and view
         */
        public ParentEntry(TimeGraphEntryModel model,
                ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider) {
            super(model);
            fProvider = provider;
        }

        /**
         * Getter for the data provider for this {@link ParentEntry}
         *
         * @return this entry's {@link ITimeGraphDataProvider}
         */
        public ITimeGraphDataProvider<? extends TimeGraphEntryModel> getProvider() {
            return fProvider;
        }
    }

    private void resetEntries(ITmfTrace trace) {
        synchronized (fEntries) {
            synchronized (fEntryListMap) {
                // Remove the entries from the entry list map and from the
                // fEntries cache
                List<TimeGraphEntry> entries = fEntryListMap.remove(trace);
                if (entries == null) {
                    return;
                }
                for (TimeGraphEntry entry : entries) {
                    if (entry instanceof ParentEntry) {
                        fEntries.remove(((ParentEntry) entry).getProvider());
                    }
                }
                refresh();
            }
        }
    }

    /**
     * Get the necessary data for the flame graph and display it
     *
     * @param viewTrace
     *            the trace
     * @param selStart
     *            The selection start timestamp or <code>null</code> to show all
     *            data
     * @param selEnd
     *            The selection end timestamp or <code>null</code> to show all
     *            data
     */
    public void buildFlameGraph(ITmfTrace viewTrace, @Nullable ITmfTimestamp selStart, @Nullable ITmfTimestamp selEnd) {
        /*
         * Note for synchronization:
         *
         * Acquire the lock at entry. then we have 4 places to release it
         *
         * 1- if the lock failed
         *
         * 2- if the data is null and we have no UI to update
         *
         * 3- when the job starts running and can thus be canceled
         */
        try (FlowScopeLog log = new FlowScopeLogBuilder(LOGGER, Level.FINE, "DifferentialFlameGraphView:Building").setCategory(getViewId()).build()) { //$NON-NLS-1$
            try {
                fLock.acquire();
            } catch (InterruptedException e) {
                Activator.getDefault().getLog().error(e.getMessage(), e);
                fLock.release();
            }

            // Run the build jobs through the site progress service if available
            IWorkbenchSiteProgressService service = null;
            IWorkbenchPartSite site = getSite();
            if (site != null) {
                service = Objects.requireNonNull(site.getService(IWorkbenchSiteProgressService.class));
            }

            // Cancel previous build job for this trace
            Job buildJob = fBuildJobMap.remove(viewTrace);
            if (buildJob != null) {
                buildJob.cancel();
            }
            resetEntries(viewTrace);
            // Build job will decrement

            buildJob = new Job(getTitle() + Messages.flameGraphViewRetrievingData) {
                @Override
                protected IStatus run(@Nullable IProgressMonitor monitor) {
                    Objects.requireNonNull(monitor);
                    new BuildRunnable(viewTrace, viewTrace, selStart, selEnd, log).run(monitor);
                    monitor.done();
                    return Status.OK_STATUS;
                }
            };
            fBuildJobMap.put(viewTrace, buildJob);
            if (service != null) {
                service.schedule(buildJob);
            } else {
                buildJob.schedule();
            }
            fLock.release();
        }
    }

    /**
     * Await the next refresh
     *
     * @return Whether the view is ready with new data
     *
     * @throws InterruptedException
     *             something took too long
     */
    public boolean isDirty() throws InterruptedException {
        /*
         * wait for the semaphore to be available, then release it immediately
         * and verify dirtiness
         */
        fLock.acquire();
        fLock.release();
        return (fDirty.get() != 0);
    }

    /**
     * Set the current trace of this view. This should be called only for
     * testing purposes, otherwise, the normal
     * {@link #traceSelected(TmfTraceSelectedSignal)} should be used.
     *
     * @param trace
     *            The trace to set
     */
    public void setTrace(ITmfTrace trace) {
        fTrace = trace;
    }

    /**
     * Trace is closed: clear the data structures and the view
     *
     * @param signal
     *            the signal received
     */
    @TmfSignalHandler
    public void traceClosed(final TmfTraceClosedSignal signal) {
        if (signal.getTrace() == fTrace && fTimeGraphViewer != null) {
            fTimeGraphViewer.setInput(null);
        }
    }

    @Override
    public void setFocus() {
        if (fTimeGraphViewer != null) {
            fTimeGraphViewer.setFocus();
        }
    }
    // --------------------------------
    // Sorting related methods
    // --------------------------------

    /**
     * Symbol map provider updated
     *
     * @param signal
     *            the signal
     */
    @TmfSignalHandler
    public void symbolMapUpdated(TmfSymbolProviderUpdatedSignal signal) {
        if (signal.getSource() != this && fTimeGraphViewer != null) {
            TimeGraphViewer timeGraphViewer = fTimeGraphViewer;
            startZoomThread(timeGraphViewer.getTime0(), timeGraphViewer.getTime1(), true, timeGraphViewer);
        }
    }

    @Override
    protected @Nullable IAction createSaveAction() {
        if (fTimeGraphViewer != null) {
            return SaveImageUtil.createSaveAction(getName(), () -> fTimeGraphViewer);
        }
        return null;
    }

    /**
     * Cancel and restart the zoom thread.
     */
    public void restartZoomThread() {
        ZoomThread zoomThread = fZoomThread;
        if (zoomThread != null) {
            // Make sure that the zoom thread is not a restart (resume of the
            // previous)
            zoomThread.cancel();
            fZoomThread = null;
        }
        if (fTimeGraphViewer != null) {
            TimeGraphViewer timeGraphViewer = fTimeGraphViewer;
            startZoomThread(timeGraphViewer.getTime0(), timeGraphViewer.getTime1(), true, timeGraphViewer);
        }
    }

    /**
     * Set or remove the global regex filter value
     *
     * @param signal
     *            the signal carrying the regex value
     */
    @TmfSignalHandler
    public void regexFilterApplied(TmfFilterAppliedSignal signal) {
        // Restart the zoom thread to apply the new filter
        Display.getDefault().asyncExec(this::restartZoomThread);
    }

    /**
     * Listen to see if one of the view's analysis is restarted
     *
     * @param monitor
     *            The progress monitor
     * @return The data provider or null
     */
    private static @Nullable DifferentialWeightedTreeProvider<?> getDataProvider(IProgressMonitor monitor) {
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace != null) {
            DifferentialSeqCallGraphAnalysis analysis = (DifferentialSeqCallGraphAnalysis) trace.getAnalysisModule("org.eclipse.tracecompass.incubator.executioncomparison.diffcallgraph"); //$NON-NLS-1$
            if (analysis != null) {
                return analysis.getDifferentialTreeProvider(monitor);
            }
        }
        return null;
    }

}
