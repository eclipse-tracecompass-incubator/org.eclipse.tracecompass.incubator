/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Author:
 *     Sonia Farrah
 *******************************************************************************/
package org.eclipse.tracecompass.incubator.internal.callstack.ui.flamegraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.common.core.StreamUtils;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLogBuilder;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.AllGroupDescriptor;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.CallGraph;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.CallGraphGroupBy;
import org.eclipse.tracecompass.incubator.callstack.core.callgraph.ICallGraphProvider;
import org.eclipse.tracecompass.incubator.internal.callstack.ui.Activator;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.SymbolProviderManager;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.editors.ITmfTraceEditor;
import org.eclipse.tracecompass.tmf.ui.symbols.ISymbolProviderPreferencePage;
import org.eclipse.tracecompass.tmf.ui.symbols.SymbolProviderConfigDialog;
import org.eclipse.tracecompass.tmf.ui.symbols.TmfSymbolProviderUpdatedSignal;
import org.eclipse.tracecompass.tmf.ui.views.SaveImageUtil;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphViewer;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphControl;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils.TimeFormat;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * View to display the flame graph .This uses the flameGraphNode tree generated
 * by CallGraphAnalysisUI.
 *
 * @author Sonia Farrah
 */
@NonNullByDefault({})
public class FlameGraphView extends TmfView {

    /**
     * ID of the view
     */
    public static final String ID = FlameGraphView.class.getPackage().getName() + ".flamegraphView"; //$NON-NLS-1$

    private static final String SYMBOL_MAPPING_ICON_PATH = "icons/obj16/binaries_obj.gif"; //$NON-NLS-1$
    private static final @NonNull String GROUP_BY_ICON_PATH = "icons/etool16/group_by.gif"; //$NON-NLS-1$

    private static final String SORT_OPTION_KEY = "sort.option"; //$NON-NLS-1$
    private static final ImageDescriptor SORT_BY_NAME_ICON = Activator.getDefault().getImageDescripterFromPath("icons/etool16/sort_alpha.gif"); //$NON-NLS-1$
    private static final ImageDescriptor SORT_BY_NAME_REV_ICON = Activator.getDefault().getImageDescripterFromPath("icons/etool16/sort_alpha_rev.gif"); //$NON-NLS-1$
    private static final ImageDescriptor SORT_BY_ID_ICON = Activator.getDefault().getImageDescripterFromPath("icons/etool16/sort_num.gif"); //$NON-NLS-1$
    private static final ImageDescriptor SORT_BY_ID_REV_ICON = Activator.getDefault().getImageDescripterFromPath("icons/etool16/sort_num_rev.gif"); //$NON-NLS-1$
    private static final ImageDescriptor AGGREGATE_BY_ICON = Activator.getDefault().getImageDescripterFromPath(GROUP_BY_ICON_PATH);

    private TimeGraphViewer fTimeGraphViewer;

    private FlameGraphContentProvider fTimeGraphContentProvider;

    private TimeGraphPresentationProvider fPresentationProvider;

    private ITmfTrace fTrace;
    private static final @NonNull Logger LOGGER = Logger.getLogger(FlameGraphView.class.getName());

    private final @NonNull MenuManager fEventMenuManager = new MenuManager();
    private Action fAggregateByAction;
    private Action fSortByNameAction;
    private Action fSortByIdAction;
    // The action to import a binary file mapping */
    private Action fConfigureSymbolsAction;

    private final Multimap<ITmfTrace, ISymbolProvider> fSymbolProviders = LinkedHashMultimap.create();
    private @Nullable ICallStackGroupDescriptor fGroupBy = null;
    /**
     * A plain old semaphore is used since different threads will be competing
     * for the same resource.
     */
    private final Semaphore fLock = new Semaphore(1);

    // Variable used to specify when the graph is dirty, ie waiting for data refresh
    private final AtomicInteger fDirty = new AtomicInteger();

    private Job fJob;

    /**
     * Constructor
     */
    public FlameGraphView() {
        super(ID);
    }

    /**
     * Constructor with ID
     *
     * @param id
     *            The ID of the view
     */
    protected FlameGraphView(String id) {
        super(id);
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        fTimeGraphViewer = new TimeGraphViewer(parent, SWT.NONE);
        fTimeGraphContentProvider = new FlameGraphContentProvider();
        fPresentationProvider = new FlameGraphPresentationProvider();
        fTimeGraphViewer.setTimeGraphContentProvider(fTimeGraphContentProvider);
        fTimeGraphViewer.setTimeGraphProvider(fPresentationProvider);
        fTimeGraphViewer.setTimeFormat(TimeFormat.NUMBER);
        IEditorPart editor = getSite().getPage().getActiveEditor();
        if (editor instanceof ITmfTraceEditor) {
            ITmfTrace trace = ((ITmfTraceEditor) editor).getTrace();
            if (trace != null) {
                traceSelected(new TmfTraceSelectedSignal(this, trace));
            }
        }
        contributeToActionBars();
        loadSortOption();
        TmfSignalManager.register(this);
        getSite().setSelectionProvider(fTimeGraphViewer.getSelectionProvider());
        createTimeEventContextMenu();
        fTimeGraphViewer.getTimeGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                TimeGraphControl timeGraphControl = getTimeGraphViewer().getTimeGraphControl();
                ISelection selection = timeGraphControl.getSelection();
                if (selection instanceof IStructuredSelection) {
                    for (Object object : ((IStructuredSelection) selection).toList()) {
                        if (object instanceof FlamegraphEvent) {
                            FlamegraphEvent event = (FlamegraphEvent) object;
                            long startTime = event.getTime();
                            long endTime = startTime + event.getDuration();
                            getTimeGraphViewer().setStartFinishTime(startTime, endTime);
                            break;
                        }
                    }
                }
            }
        });
    }

    /**
     * Get the time graph viewer
     *
     * @return the time graph viewer
     */
    @VisibleForTesting
    public TimeGraphViewer getTimeGraphViewer() {
        return fTimeGraphViewer;
    }

    /**
     * Handler for the trace selected signal
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public void traceSelected(final TmfTraceSelectedSignal signal) {
        fTrace = signal.getTrace();
        Display.getDefault().asyncExec(() -> buildFlameGraph(getCallgraphModules(), null, null));
    }

    /**
     * Get the callgraph modules used to build the view
     *
     * @return The call graph provider modules
     */
    protected Iterable<ICallGraphProvider> getCallgraphModules() {
        ITmfTrace trace = fTrace;
        if (trace == null) {
            return null;
        }
        String analysisId = NonNullUtils.nullToEmptyString(getViewSite().getSecondaryId());
        Iterable<ICallGraphProvider> modules = TmfTraceUtils.getAnalysisModulesOfClass(trace, ICallGraphProvider.class);
        return StreamUtils.getStream(modules)
                .filter(m -> {
                    if (m instanceof IAnalysisModule) {
                        return ((IAnalysisModule) m).getId().equals(analysisId);
                    }
                    return true;
                })
                .collect(Collectors.toSet());
    }

    /**
     * Get the necessary data for the flame graph and display it
     *
     * @param callGraphProviders
     *            the callGraphAnalysis
     * @param selStart
     *            The selection start timestamp or <code>null</code> to show all
     *            data
     * @param selEnd
     *            The selection end timestamp or <code>null</code> to show all
     *            data
     */
    @VisibleForTesting
    public void buildFlameGraph(Iterable<ICallGraphProvider> callGraphProviders, @Nullable ITmfTimestamp selStart, @Nullable ITmfTimestamp selEnd) {
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
        Job job = fJob;
        if (job != null) {
            job.cancel();
        }
        try (FlowScopeLog log = new FlowScopeLogBuilder(LOGGER, Level.FINE, "FlameGraphView:Building").setCategory(getViewId()).build()) { //$NON-NLS-1$
            try {
                fLock.acquire();
            } catch (InterruptedException e) {
                Activator.getDefault().logError(e.getMessage(), e);
                fLock.release();
            }
            /*
             * Load the symbol provider for the current trace, even if it does not provide a
             * call stack analysis module. See
             * https://bugs.eclipse.org/bugs/show_bug.cgi?id=494212
             */
            ITmfTrace trace = fTrace;
            if (trace != null) {
                /*
                 * Load the symbol provider for the current trace, even if it does not provide a
                 * call stack analysis module. See
                 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=494212
                 */
                Collection<ISymbolProvider> symbolProviders = fSymbolProviders.get(trace);
                if (symbolProviders.isEmpty()) {
                    symbolProviders = SymbolProviderManager.getInstance().getSymbolProviders(trace);
                    symbolProviders.forEach(provider -> provider.loadConfiguration(new NullProgressMonitor()));
                    fSymbolProviders.putAll(trace, symbolProviders);
                }
            }

            if (!callGraphProviders.iterator().hasNext()) {
                fTimeGraphViewer.setInput(null);
                fLock.release();
                return;
            }
            for (ICallGraphProvider provider : callGraphProviders) {
                if (provider instanceof IAnalysisModule) {
                    ((IAnalysisModule) provider).schedule();
                }
            }
            job = new Job(Messages.FlameGraphView_RetrievingData) {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try (FlowScopeLog runLog = new FlowScopeLogBuilder(LOGGER, Level.FINE, "FlameGraphView:GettingFlameGraphs").setParentScope(log).build()) { //$NON-NLS-1$
                        if (monitor.isCanceled()) {
                            return Status.CANCEL_STATUS;
                        }
                        // Set the view as dirty before releasing the lock
                        fDirty.incrementAndGet();
                        fLock.release();
                        Set<CallGraph> callgraphs = new HashSet<>();
                        ICallStackGroupDescriptor group = fGroupBy;
                        for (ICallGraphProvider provider : callGraphProviders) {
                            if (provider instanceof IAnalysisModule) {
                                ((IAnalysisModule) provider).waitForCompletion(monitor);
                            }
                            // FIXME: This waits for completion, there is no way of cancelling this call, so
                            // make the views responsive to updates in the model, so that we can return a
                            // partial callgraph
                            CallGraph callGraph;
                            if (selStart == null || selEnd == null) {
                                callGraph = provider.getCallGraph();
                            } else {
                                callGraph = provider.getCallGraph(selStart, selEnd);
                            }
                            if (group == null) {
                                callgraphs.add(callGraph);
                            } else {
                                callgraphs.add(CallGraphGroupBy.groupCallGraphBy(group, callGraph));
                            }
                        }
                        if (monitor.isCanceled()) {
                            // Decrease dirtiness, job canceled
                            fDirty.decrementAndGet();
                            return Status.CANCEL_STATUS;
                        }
                        Display.getDefault().asyncExec(() -> {
                            try (FlowScopeLog asyncLog = new FlowScopeLogBuilder(LOGGER, Level.FINE, "FlameGraphView:SettingInput").setParentScope(runLog).build()) { //$NON-NLS-1$
                                fTimeGraphViewer.setInput(callgraphs);
                                fTimeGraphViewer.resetStartFinishTime();
                            } finally {
                                // Finished updating, decrease dirtiness
                                fDirty.decrementAndGet();
                            }
                        });
                        return Status.OK_STATUS;
                    }
                }
            };
            IWorkbenchSiteProgressService service = null;
            IWorkbenchPartSite site = getSite();
            if (site != null) {
                service = site.getService(IWorkbenchSiteProgressService.class);
            }
            fJob = job;
            if (service != null) {
                service.schedule(job);
            } else {
                job.schedule();
            }
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
    @VisibleForTesting
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
     * Trace is closed: clear the data structures and the view
     *
     * @param signal
     *            the signal received
     */
    @TmfSignalHandler
    public void traceClosed(final TmfTraceClosedSignal signal) {
        if (signal.getTrace() == fTrace) {
            fTimeGraphViewer.setInput(null);
        }
    }

    @Override
    public void setFocus() {
        fTimeGraphViewer.setFocus();
    }

    // ------------------------------------------------------------------------
    // Helper methods
    // ------------------------------------------------------------------------

    private void createTimeEventContextMenu() {
        fEventMenuManager.setRemoveAllWhenShown(true);
        TimeGraphControl timeGraphControl = fTimeGraphViewer.getTimeGraphControl();
        final Menu timeEventMenu = fEventMenuManager.createContextMenu(timeGraphControl);

        timeGraphControl.addTimeGraphEntryMenuListener(new MenuDetectListener() {
            @Override
            public void menuDetected(MenuDetectEvent event) {
                /*
                 * The TimeGraphControl will call the TimeGraphEntryMenuListener
                 * before the TimeEventMenuListener. We need to clear the menu
                 * for the case the selection was done on the namespace where
                 * the time event listener below won't be called afterwards.
                 */
                timeGraphControl.setMenu(null);
                event.doit = false;
            }
        });
        timeGraphControl.addTimeEventMenuListener(new MenuDetectListener() {
            @Override
            public void menuDetected(MenuDetectEvent event) {
                Menu menu = timeEventMenu;
                if (event.data instanceof FlamegraphEvent) {
                    timeGraphControl.setMenu(menu);
                    return;
                }
                timeGraphControl.setMenu(null);
                event.doit = false;
            }
        });

        fEventMenuManager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                fillTimeEventContextMenu(fEventMenuManager);
                fEventMenuManager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
            }
        });
        getSite().registerContextMenu(fEventMenuManager, fTimeGraphViewer.getSelectionProvider());
    }

    /**
     * Fill context menu
     *
     * @param menuManager
     *            a menuManager to fill
     */
    protected void fillTimeEventContextMenu(@NonNull IMenuManager menuManager) {
        ISelection selection = getSite().getSelectionProvider().getSelection();
        if (selection instanceof IStructuredSelection) {
            for (Object object : ((IStructuredSelection) selection).toList()) {
                if (object instanceof FlamegraphEvent) {
//                    final FlamegraphEvent flamegraphEvent = (FlamegraphEvent) object;
//                    menuManager.add(new Action(Messages.FlameGraphView_GotoMaxDuration) {
//                        @Override
//                        public void run() {
//                            ISegment maxSeg = flamegraphEvent.getStatistics().getDurationStatistics().getMaxObject();
//                            if (maxSeg == null) {
//                                return;
//                            }
//                            TmfSelectionRangeUpdatedSignal sig = new TmfSelectionRangeUpdatedSignal(this, TmfTimestamp.fromNanos(maxSeg.getStart()), TmfTimestamp.fromNanos(maxSeg.getEnd()));
//                            broadcast(sig);
//                        }
//                    });
//
//                    menuManager.add(new Action(Messages.FlameGraphView_GotoMinDuration) {
//                        @Override
//                        public void run() {
//                            ISegment minSeg = flamegraphEvent.getStatistics().getDurationStatistics().getMinObject();
//                            if (minSeg == null) {
//                                return;
//                            }
//                            TmfSelectionRangeUpdatedSignal sig = new TmfSelectionRangeUpdatedSignal(this, TmfTimestamp.fromNanos(minSeg.getStart()), TmfTimestamp.fromNanos(minSeg.getEnd()));
//                            broadcast(sig);
//                        }
//                    });
                }
            }
        }
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(getConfigureSymbolsAction());
        manager.add(getAggregateByAction());
        manager.add(getSortByNameAction());
        manager.add(getSortByIdAction());
        manager.add(new Separator());
    }

    private Action getAggregateByAction() {
        if (fAggregateByAction == null) {
            fAggregateByAction = new Action(Messages.FlameGraphView_GroupByName, IAction.AS_DROP_DOWN_MENU) {
                @Override
                public void run() {
                    SortOption sortOption = fTimeGraphContentProvider.getSortOption();
                    if (sortOption == SortOption.BY_NAME) {
                        setSortOption(SortOption.BY_NAME_REV);
                    } else {
                        setSortOption(SortOption.BY_NAME);
                    }
                }
            };
            fAggregateByAction.setToolTipText(Messages.FlameGraphView_GroupByTooltip);
            fAggregateByAction.setImageDescriptor(AGGREGATE_BY_ICON);
            fAggregateByAction.setMenuCreator(new IMenuCreator () {
                Menu menu = null;
                @Override
                public void dispose() {
                    if (menu != null) {
                        menu.dispose();
                        menu = null;
                    }
                }

                @Override
                public Menu getMenu(Control parent) {
                    if (menu != null) {
                        menu.dispose();
                    }
                    menu = new Menu(parent);
                    Iterable<ICallGraphProvider> callgraphModules = getCallgraphModules();
                    Iterator<ICallGraphProvider> iterator = callgraphModules.iterator();
                    if (!iterator.hasNext()) {
                        return menu;
                    }
                    ICallGraphProvider provider = iterator.next();
                 // Add the all group element
                    Action allGroupAction = createActionForGroup(provider, AllGroupDescriptor.getInstance());
                    new ActionContributionItem(allGroupAction).fill(menu, -1);
                    Collection<ICallStackGroupDescriptor> series = provider.getGroupDescriptors();
                    series.forEach(group -> {
                        ICallStackGroupDescriptor subGroup = group;
                        do {
                            Action groupAction = createActionForGroup(provider, subGroup);
                            new ActionContributionItem(groupAction).fill(menu, -1);
                            subGroup = subGroup.getNextGroup();
                        } while (subGroup != null);
                    });
                    return menu;
                }

                @Override
                public Menu getMenu(Menu parent) {
                    return null;
                }
            });
        }
        return fAggregateByAction;
    }

    private Action createActionForGroup(ICallGraphProvider provider, ICallStackGroupDescriptor descriptor) {
        return new Action(descriptor.getName(), IAction.AS_RADIO_BUTTON) {
            @Override
            public void run() {
                fGroupBy = descriptor;
                buildFlameGraph(Collections.singleton(provider), null, null);
            }
        };
    }

    private Action getSortByNameAction() {
        if (fSortByNameAction == null) {
            fSortByNameAction = new Action(Messages.FlameGraph_SortByThreadName, IAction.AS_CHECK_BOX) {
                @Override
                public void run() {
                    SortOption sortOption = fTimeGraphContentProvider.getSortOption();
                    if (sortOption == SortOption.BY_NAME) {
                        setSortOption(SortOption.BY_NAME_REV);
                    } else {
                        setSortOption(SortOption.BY_NAME);
                    }
                }
            };
            fSortByNameAction.setToolTipText(Messages.FlameGraph_SortByThreadName);
            fSortByNameAction.setImageDescriptor(SORT_BY_NAME_ICON);
        }
        return fSortByNameAction;
    }

    private Action getSortByIdAction() {
        if (fSortByIdAction == null) {
            fSortByIdAction = new Action(Messages.FlameGraph_SortByThreadId, IAction.AS_CHECK_BOX) {
                @Override
                public void run() {
                    SortOption sortOption = fTimeGraphContentProvider.getSortOption();
                    if (sortOption == SortOption.BY_ID) {
                        setSortOption(SortOption.BY_ID_REV);
                    } else {
                        setSortOption(SortOption.BY_ID);
                    }
                }
            };
            fSortByIdAction.setToolTipText(Messages.FlameGraph_SortByThreadId);
            fSortByIdAction.setImageDescriptor(SORT_BY_ID_ICON);
        }
        return fSortByIdAction;
    }

    private Action getConfigureSymbolsAction() {
        if (fConfigureSymbolsAction != null) {
            return fConfigureSymbolsAction;
        }

        fConfigureSymbolsAction = new Action("get symbols") {
            @Override
            public void run() {
                SymbolProviderConfigDialog dialog = new SymbolProviderConfigDialog(getSite().getShell(), getProviderPages());
                if (dialog.open() == IDialogConstants.OK_ID) {
//                    fPresentationProvider.resetFunctionNames();
//                    refresh();
                }
            }
        };

        fConfigureSymbolsAction.setToolTipText("get symbols");
        fConfigureSymbolsAction.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(SYMBOL_MAPPING_ICON_PATH));

        /*
         * The updateConfigureSymbolsAction() method (called by refresh()) will
         * set the action to true if applicable after the symbol provider has
         * been properly loaded.
         */
        fConfigureSymbolsAction.setEnabled(true);

        return fConfigureSymbolsAction;
    }

    /**
     * @return an array of {@link ISymbolProviderPreferencePage} that will
     *         configure the current traces
     */
    private ISymbolProviderPreferencePage[] getProviderPages() {
        List<ISymbolProviderPreferencePage> pages = new ArrayList<>();
        ITmfTrace trace = fTrace;
        if (trace != null) {
            for (ITmfTrace subTrace : TmfTraceManager.getTraceSet(trace)) {
                for (ISymbolProvider provider : SymbolProviderManager.getInstance().getSymbolProviders(subTrace)) {
                    if (provider instanceof org.eclipse.tracecompass.tmf.ui.symbols.ISymbolProvider) {
                        org.eclipse.tracecompass.tmf.ui.symbols.ISymbolProvider provider2 = (org.eclipse.tracecompass.tmf.ui.symbols.ISymbolProvider) provider;
                        ISymbolProviderPreferencePage page = provider2.createPreferencePage();
                        if (page != null) {
                            pages.add(page);
                        }
                    }
                }
            }
        }
        return pages.toArray(new ISymbolProviderPreferencePage[pages.size()]);
    }

    private void setSortOption(SortOption sortOption) {
        // reset defaults
        getSortByNameAction().setChecked(false);
        getSortByNameAction().setImageDescriptor(SORT_BY_NAME_ICON);
        getSortByIdAction().setChecked(false);
        getSortByIdAction().setImageDescriptor(SORT_BY_ID_ICON);

        if (sortOption.equals(SortOption.BY_NAME)) {
            fTimeGraphContentProvider.setSortOption(SortOption.BY_NAME);
            getSortByNameAction().setChecked(true);
        } else if (sortOption.equals(SortOption.BY_NAME_REV)) {
            fTimeGraphContentProvider.setSortOption(SortOption.BY_NAME_REV);
            getSortByNameAction().setChecked(true);
            getSortByNameAction().setImageDescriptor(SORT_BY_NAME_REV_ICON);
        } else if (sortOption.equals(SortOption.BY_ID)) {
            fTimeGraphContentProvider.setSortOption(SortOption.BY_ID);
            getSortByIdAction().setChecked(true);
        } else if (sortOption.equals(SortOption.BY_ID_REV)) {
            fTimeGraphContentProvider.setSortOption(SortOption.BY_ID_REV);
            getSortByIdAction().setChecked(true);
            getSortByIdAction().setImageDescriptor(SORT_BY_ID_REV_ICON);
        }
        saveSortOption();
        fTimeGraphViewer.refresh();
    }

    private void saveSortOption() {
        SortOption sortOption = fTimeGraphContentProvider.getSortOption();
        IDialogSettings settings = Activator.getDefault().getDialogSettings();
        IDialogSettings section = settings.getSection(getClass().getName());
        if (section == null) {
            section = settings.addNewSection(getClass().getName());
        }
        section.put(SORT_OPTION_KEY, sortOption.name());
    }

    private void loadSortOption() {
        IDialogSettings settings = Activator.getDefault().getDialogSettings();
        IDialogSettings section = settings.getSection(getClass().getName());
        if (section == null) {
            return;
        }
        String sortOption = section.get(SORT_OPTION_KEY);
        if (sortOption == null) {
            return;
        }
        setSortOption(SortOption.fromName(sortOption));
    }

    /**
     * Symbol map provider updated
     *
     * @param signal
     *            the signal
     */
    @TmfSignalHandler
    public void symbolMapUpdated(TmfSymbolProviderUpdatedSignal signal) {
//        if (signal.getSource() != this) {
            fTimeGraphViewer.refresh();
//        }
    }

    @Override
    protected @Nullable IAction createSaveAction() {
        return SaveImageUtil.createSaveAction(getName(), this::getTimeGraphViewer);
    }

}
