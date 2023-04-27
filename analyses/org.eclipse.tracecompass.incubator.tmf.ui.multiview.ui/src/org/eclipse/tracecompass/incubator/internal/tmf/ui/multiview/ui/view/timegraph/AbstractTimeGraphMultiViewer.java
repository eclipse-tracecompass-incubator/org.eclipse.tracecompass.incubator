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

package org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.timegraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.FlowScopeLogBuilder;
import org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.IMultiViewer;
import org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.MultiView;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filter.parser.FilterCu;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filter.parser.IFilterStrings;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters.TraceCompassFilter;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.util.TimeGraphStyleUtil;
import org.eclipse.tracecompass.tmf.core.model.CoreFilterProperty;
import org.eclipse.tracecompass.tmf.core.model.ICoreElementResolver;
import org.eclipse.tracecompass.tmf.core.resources.ITmfMarker;
import org.eclipse.tracecompass.tmf.core.signal.TmfDataModelSelectedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfMarkerEventSourceUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTimestampFormatUpdateSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimePreferencesConstants;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimePreferences;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceAdapterManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.TmfUiRefreshHandler;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentInfo;
import org.eclipse.tracecompass.tmf.ui.viewers.TmfViewer;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.AbstractTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.ITimeGraphEntryComparator;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.ITimeGraphLegendProvider;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.Messages;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphBookmarkListener;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphContentProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphSelectionListener;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphBookmarkEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphContentProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphViewer;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ILinkEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEventSource;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.MarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry.Sampling;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphControl;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils.TimeFormat;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

/**
 * An abstract viewer all multi time graph viewers can inherit. This viewer
 * contains a time graph viewer.
 * <p>
 * The viewer corresponds (copy-pasted actually) to
 * {@link AbstractTimeGraphView} to be used with the {@link MultiView}.
 *
 * @author Ivan Grinenko
 *
 */
@SuppressWarnings("restriction")
public abstract class AbstractTimeGraphMultiViewer extends TmfViewer implements IMultiViewer, IResourceChangeListener {
    private static final @NonNull Logger LOGGER = TraceCompassLog.getLogger(AbstractTimeGraphMultiViewer.class);

    private static final String TIMEGRAPH_UI_CONTEXT = "org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.timegraph.context"; //$NON-NLS-1$
    private static final String TMF_VIEW_UI_CONTEXT = "org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.context"; //$NON-NLS-1$

    private static final String DIRTY_UNDERFLOW_ERROR = "Dirty underflow error"; //$NON-NLS-1$
    private static final int DEFAULT_BUFFER_SIZE = 3;

    private static final Pattern RGBA_PATTERN = Pattern.compile("RGBA \\{(\\d+), (\\d+), (\\d+), (\\d+)\\}"); //$NON-NLS-1$

    /**
     * Constant indicating that all levels of the time graph should be expanded
     */
    protected static final int ALL_LEVELS = AbstractTreeViewer.ALL_LEVELS;

    /**
     * Redraw state enum
     */
    private enum State {
        IDLE, BUSY, PENDING
    }

    private final String fName;

    /** The time graph viewer */
    private TimeGraphViewer fTimeGraphViewer;

    /** The time graph label provider */
    private ITableLabelProvider fLabelProvider = new TreeLabelProvider();

    /** The time graph content provider */
    private @NonNull ITimeGraphContentProvider fTimeGraphContentProvider = new TimeGraphContentProvider();

    /** The relative weight of the time graph viewer parts */
    private int[] fWeight = { 1, 3 };

    /** The filter column label array, or null if filter is not used */
    private String[] fFilterColumns;

    /** The filter content provider, or null if filter is not used */
    private ITreeContentProvider fFilterContentProvider;

    /** The filter label provider, or null if filter is not used */
    private TreeLabelProvider fFilterLabelProvider;

    private ITimeGraphLegendProvider fLegendProvider;

    private int fAutoExpandLevel = ALL_LEVELS;

    /**
     * The redraw state used to prevent unnecessary queuing of display runnables
     */
    private State fRedrawState = State.IDLE;

    /** The redraw synchronization object */
    private final Object fSyncObj = new Object();

    /** The presentation provider for this view */
    private final ITimeGraphPresentationProvider fPresentation;

    /** The tree column label array, or null for a single default column */
    private String[] fColumns;

    private Comparator<ITimeGraphEntry>[] fColumnComparators;

    /** The default column index for sorting */
    private int fInitialSortColumn = 0;

    /** The default column index for sorting */
    private int fCurrentSortColumn = 0;

    private @Nullable TimeFormat fTimeFormat = null;

    /** The current sort direction */
    private int fSortDirection = SWT.DOWN;

    /**
     * The width of the last time space that was zoomed on.
     */
    private int fPrevTimeSpace = -1;

    /**
     * Set of visible entries to zoom on.
     */
    private @NonNull Set<@NonNull TimeGraphEntry> fVisibleEntries = Collections.emptySet();

    /** Flag to indicate to reveal selection */
    private volatile boolean fIsRevealSelection = false;

    /** A comparator class */
    private Comparator<ITimeGraphEntry> fEntryComparator = null;

    private AtomicInteger fDirty = new AtomicInteger();

    private final Object fZoomThreadResultLock = new Object();

    /** The selected trace */
    private ITmfTrace fTrace;

    /** The selected trace editor file */
    private @Nullable IFile fEditorFile;

    /** The timegraph entry list */
    private List<TimeGraphEntry> fEntryList;

    /** The trace to entry list hash map */
    private final Map<ITmfTrace, List<@NonNull TimeGraphEntry>> fEntryListMap = new HashMap<>();

    /** The trace to filters hash map */
    private final Map<ITmfTrace, @NonNull ViewerFilter[]> fFiltersMap = new HashMap<>();

    /** The trace to viewer context hash map */
    private final Map<ITmfTrace, ViewerContext> fViewerContext = new HashMap<>();

    /** The trace to marker event sources hash map */
    private final Map<ITmfTrace, List<IMarkerEventSource>> fMarkerEventSourcesMap = new HashMap<>();

    /** The trace to build thread hash map */
    private final Map<ITmfTrace, Job> fBuildJobMap = new HashMap<>();

    /** The start time */
    private long fStartTime = SWT.DEFAULT;

    /** The end time */
    private long fEndTime = SWT.DEFAULT;

    /** The display width */
    private final int fDisplayWidth;

    /** The zoom thread */
    private ZoomThread fZoomThread;

    /** The timegraph event filter action */
    private Action fTimeEventFilterAction;

    /** The time graph event filter dialog */
    private TimeEventFilterDialog fTimeEventFilterDialog;

    private final IWorkbenchPartSite fSite;

    /** Time Graph Viewer part listener */
    private TimeGraphPartListener fPartListener;

    private TimeGraphPartListener2 fPartListener2;

    private IContextService fContextService;

    private List<IContextActivation> fActiveContexts = new ArrayList<>();

    /**
     * Menu Manager for context-sensitive menu for time graph entries. This will
     * be used on the name space of the time graph viewer.
     */
    private final @NonNull MenuManager fEntryMenuManager = new MenuManager();

    /**
     * Action for the find command. There is only one for all Time Graph views
     */
    private static final ShowFindDialogAction FIND_ACTION = new ShowFindDialogAction();

    /** The find action handler */
    private ActionHandler fFindActionHandler;

    /** The find handler activation */
    private IHandlerActivation fFindHandlerActivation;

    /** Listener that handles a click on an entry in the FusedVM View */
    private final ITimeGraphSelectionListener fMetadataSelectionListener = event -> {
        ITimeGraphEntry entry = event.getSelection();
        if (entry instanceof ICoreElementResolver) {
            Multimap<@NonNull String, @NonNull Object> metadata = ((ICoreElementResolver) entry).getMetadata();
            if (!metadata.isEmpty()) {
                TmfSignalManager.dispatchSignal(new TmfDataModelSelectedSignal(AbstractTimeGraphMultiViewer.this, metadata));
            }
        }
    };

    /**
     * ID of the viewer.
     */
    public static final @NonNull String ID = "org.eclipse.tracecompass.incubator.internal.tmf.ui.multiview.ui.view.MultiTimeGraphViewer"; //$NON-NLS-1$

    /**
     * Constructor.
     *
     * @param parent
     *            parent for the viewer
     * @param pres
     *            presentation provider for time graph
     * @param site
     *            workbench part site
     */
    public AbstractTimeGraphMultiViewer(Composite parent, ITimeGraphPresentationProvider pres, IWorkbenchPartSite site) {
        super(parent);
        fPresentation = pres;
        fSite = site;
        fName = AbstractTimeGraphMultiViewer.class.getSimpleName();
        fDisplayWidth = Display.getDefault().getBounds().width;
        TimeGraphViewer timeGraphViewer = new TimeGraphViewer(parent, SWT.NONE);
        timeGraphViewer.setTimeGraphScaleVisible(false);
        timeGraphViewer.setMarkerAxisControlVisible(false);
        timeGraphViewer.setHorizontalScrollBarVisible(false);
        fTimeGraphViewer = timeGraphViewer;
        setFilterColumns(new String[] { "Name" }); //$NON-NLS-1$
        setFilterLabelProvider(new TreeLabelProvider() {
            @Override
            public String getColumnText(Object element, int columnIndex) {
                if (columnIndex == 0) {
                    return this.getText(element);
                }
                return ""; //$NON-NLS-1$
            }
        });
    }

    /**
     * Initializes the viewer in the
     * {@link AbstractTimeGraphView#createPartControl(Composite)} manner.
     */
    public void init() {
        if (fLabelProvider != null) {
            fTimeGraphViewer.setTimeGraphLabelProvider(fLabelProvider);
        }
        if (fLegendProvider != null) {
            fTimeGraphViewer.setLegendProvider(fLegendProvider);
        }
        if (fColumns != null) {
            fTimeGraphViewer.setColumns(fColumns);
            if (fColumnComparators != null) {
                createColumnSelectionListener(fTimeGraphViewer.getTree());
            }
        }
        fTimeGraphViewer.setTimeGraphContentProvider(fTimeGraphContentProvider);
        fTimeGraphViewer.setFilterContentProvider(
                fFilterContentProvider != null ? fFilterContentProvider : fTimeGraphContentProvider);
        fTimeGraphViewer.setFilterLabelProvider(fFilterLabelProvider);
        fTimeGraphViewer.setFilterColumns(fFilterColumns);
        fTimeGraphViewer.addSelectionListener(fMetadataSelectionListener);

        ITimeGraphPresentationProvider presentationProvider = getPresentationProvider();
        fTimeGraphViewer.setTimeGraphProvider(presentationProvider);
        presentationProvider.addColorListener(stateItems -> TimeGraphStyleUtil.loadValues(getPresentationProvider()));
        presentationProvider.refresh();
        fTimeGraphViewer.setAutoExpandLevel(fAutoExpandLevel);

        fTimeGraphViewer.setWeights(fWeight);

        TimeGraphControl timeGraphControl = fTimeGraphViewer.getTimeGraphControl();
        Action timeEventFilterAction = new Action() {

            @Override
            public void run() {
                int xCoord = timeGraphControl.toControl(timeGraphControl.getDisplay().getCursorLocation()).x;
                if ((fTimeGraphViewer.getNameSpace() < xCoord) && (xCoord < timeGraphControl.getSize().x)) {
                    if (fTimeEventFilterDialog != null) {
                        fTimeEventFilterDialog.close();
                        fTimeEventFilterDialog = null;
                    }
                    fTimeEventFilterDialog = new TimeEventFilterDialog(timeGraphControl.getShell(),
                            AbstractTimeGraphMultiViewer.this, getTimeGraphViewer().getTimeGraphControl());
                    fTimeEventFilterDialog.open();
                }
            }
        };

        fTimeEventFilterAction = timeEventFilterAction;

        fTimeGraphViewer.addRangeListener(event -> {
            final long startTime = event.getStartTime();
            final long endTime = event.getEndTime();
            TmfTimeRange range = new TmfTimeRange(TmfTimestamp.fromNanos(startTime), TmfTimestamp.fromNanos(endTime));
            TmfSignalManager.dispatchSignal(
                    new TmfWindowRangeUpdatedSignal(AbstractTimeGraphMultiViewer.this, range, fTrace));
            startZoomThread(startTime, endTime);
        });

        fTimeGraphViewer.addTimeListener(event -> {
            ITmfTimestamp startTime = TmfTimestamp.fromNanos(event.getBeginTime());
            ITmfTimestamp endTime = TmfTimestamp.fromNanos(event.getEndTime());
            TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(
                    AbstractTimeGraphMultiViewer.this, startTime, endTime, fTrace));
        });

        fTimeGraphViewer.addBookmarkListener(new ITimeGraphBookmarkListener() {
            @Override
            public void bookmarkAdded(final TimeGraphBookmarkEvent event) {
                try {
                    ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
                        @Override
                        public void run(IProgressMonitor monitor) throws CoreException {
                            IMarkerEvent bookmark = event.getBookmark();
                            IFile editorFile = fEditorFile;
                            if (editorFile == null) {
                                return;
                            }
                            IMarker marker = editorFile.createMarker(IMarker.BOOKMARK);
                            marker.setAttribute(IMarker.MESSAGE, bookmark.getLabel());
                            marker.setAttribute(ITmfMarker.MARKER_TIME, Long.toString(bookmark.getTime()));
                            if (bookmark.getDuration() > 0) {
                                marker.setAttribute(ITmfMarker.MARKER_DURATION, Long.toString(bookmark.getDuration()));
                                marker.setAttribute(IMarker.LOCATION,
                                        NLS.bind(org.eclipse.tracecompass.internal.tmf.ui.Messages.TmfMarker_LocationTimeRange,
                                                TmfTimestamp.fromNanos(bookmark.getTime()),
                                                TmfTimestamp.fromNanos(bookmark.getTime() + bookmark.getDuration())));
                            } else {
                                marker.setAttribute(IMarker.LOCATION,
                                        NLS.bind(org.eclipse.tracecompass.internal.tmf.ui.Messages.TmfMarker_LocationTime,
                                                TmfTimestamp.fromNanos(bookmark.getTime())));
                            }
                            marker.setAttribute(ITmfMarker.MARKER_COLOR, bookmark.getColor().toString());
                        }
                    }, null);
                } catch (CoreException e) {
                    Activator.getDefault().logError(e.getMessage());
                }
            }

            @Override
            public void bookmarkRemoved(TimeGraphBookmarkEvent event) {
                try {
                    IMarkerEvent bookmark = event.getBookmark();
                    IFile editorFile = fEditorFile;
                    if (editorFile == null) {
                        return;
                    }
                    IMarker[] markers = editorFile.findMarkers(IMarker.BOOKMARK, false, IResource.DEPTH_ZERO);
                    for (IMarker marker : markers) {
                        if (bookmark.getLabel().equals(marker.getAttribute(IMarker.MESSAGE)) &&
                                Long.toString(bookmark.getTime()).equals(marker.getAttribute(ITmfMarker.MARKER_TIME, (String) null)) &&
                                Long.toString(bookmark.getDuration()).equals(marker.getAttribute(ITmfMarker.MARKER_DURATION, Long.toString(0))) &&
                                bookmark.getColor().toString().equals(marker.getAttribute(ITmfMarker.MARKER_COLOR))) {
                            marker.delete();
                            break;
                        }
                    }
                } catch (CoreException e) {
                    Activator.getDefault().logError(e.getMessage());
                }
            }
        });

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
            public void paintControl(PaintEvent e) {
                TmfUiRefreshHandler.getInstance().queueUpdate(this, () -> {
                    if (timeGraphControl.isDisposed()) {
                        return;
                    }
                    int timeSpace = getTimeGraphViewer().getTimeSpace();
                    Set<@NonNull TimeGraphEntry> newSet = getVisibleItems(DEFAULT_BUFFER_SIZE);
                    if (fPrevTimeSpace != timeSpace || !fVisibleEntries.equals(newSet)) {
                        /*
                         * Start a zoom thread if the set of visible entries has
                         * changed. We do not use lists as the order is not
                         * important. We cannot use the start index / size of
                         * the visible entries as we can collapse / reorder
                         * events.
                         */
                        fVisibleEntries = newSet;
                        fPrevTimeSpace = timeSpace;
                        startZoomThread(getTimeGraphViewer().getTime0(), getTimeGraphViewer().getTime1());
                    }
                });
            }
        });

        if (fSite instanceof IViewSite) {
            IStatusLineManager statusLineManager = ((IViewSite) fSite).getActionBars().getStatusLineManager();
            timeGraphControl.setStatusLineManager(statusLineManager);
        }

        // View Action Handling
        // makeActions();
        // contributeToActionBars();

        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace != null) {
            traceSelected(new TmfTraceSelectedSignal(this, trace));
        }

        // make selection available to other views
        IWorkbenchPartSite site = getSite();
        site.setSelectionProvider(fTimeGraphViewer.getSelectionProvider());

        ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);

        createContextMenu();
        fPartListener = new TimeGraphPartListener();
        site.getPage().addPartListener(fPartListener);

        fPartListener2 = new TimeGraphPartListener2();
        site.getPage().addPartListener(fPartListener2);

        contextServiceInit(timeGraphControl, site);

        updateTimeFormat();
        TmfSignalManager.register(this);
    }

    private void contextServiceInit(TimeGraphControl timeGraphControl, IWorkbenchPartSite site) {
        @Nullable
        IWorkbenchWindow workbenchWindow = site.getWorkbenchWindow();
        if (workbenchWindow == null) {
            return;
        }

        fContextService = workbenchWindow.getService(IContextService.class);

        if (timeGraphControl.isInFocus()) {
            activateContextService();
        }
        timeGraphControl.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(FocusEvent e) {
                deactivateContextService();
            }

            @Override
            public void focusGained(FocusEvent e) {
                activateContextService();
            }
        });
    }

    @Override
    public TmfTimeViewAlignmentInfo getTimeViewAlignmentInfo() {
        if (fTimeGraphViewer == null) {
            return null;
        }
        return fTimeGraphViewer.getTimeViewAlignmentInfo();
    }

    @Override
    public int getAvailableWidth(int requestedOffset) {
        if (fTimeGraphViewer == null) {
            return 0;
        }
        return fTimeGraphViewer.getAvailableWidth(requestedOffset);
    }

    @Override
    public void performAlign(int offset, int width) {
        if (fTimeGraphViewer != null) {
            fTimeGraphViewer.performAlign(offset, width);
        }
    }

    @Override
    public void dispose() {
        fTimeGraphViewer.dispose();
        TmfSignalManager.deregister(this);
        synchronized (fBuildJobMap) {
            fBuildJobMap.values().forEach(Job::cancel);
        }
        if (fZoomThread != null) {
            fZoomThread.cancel();
        }
        deactivateContextService();
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        IWorkbenchPage page = fSite.getPage();
        if (fPartListener != null) {
            page.removePartListener(fPartListener);
        }
        if (fPartListener2 != null) {
            page.removePartListener(fPartListener2);
        }
    }

    @Override
    public void resourceChanged(final IResourceChangeEvent event) {
        for (final IMarkerDelta delta : event.findMarkerDeltas(IMarker.BOOKMARK, false)) {
            if (delta.getResource().equals(fEditorFile)) {
                fTimeGraphViewer.setBookmarks(refreshBookmarks(fEditorFile));
                redraw();
                return;
            }
        }
    }

    @Override
    public String getName() {
        return fName;
    }

    /**
     * @return ID of the viewer
     */
    public String getViewerId() {
        return ID;
    }

    /**
     * Getter for the time graph viewer
     *
     * @return The time graph viewer
     */
    public TimeGraphViewer getTimeGraphViewer() {
        return fTimeGraphViewer;
    }

    /**
     * @return workbench part site
     */
    public IWorkbenchPartSite getSite() {
        return fSite;
    }

    private void activateContextService() {
        if (fContextService == null) {
            return;
        }
        if (fActiveContexts.isEmpty()) {
            fActiveContexts.add(fContextService.activateContext(TIMEGRAPH_UI_CONTEXT));
            fActiveContexts.add(fContextService.activateContext(TMF_VIEW_UI_CONTEXT));
        }
    }

    private void deactivateContextService() {
        if (fContextService == null) {
            return;
        }
        fContextService.deactivateContexts(fActiveContexts);
        fActiveContexts.clear();
    }

    // ------------------------------------------------------------------------
    // Signal handlers
    // ------------------------------------------------------------------------

    /**
     * Handler for the trace opened signal.
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public void traceOpened(TmfTraceOpenedSignal signal) {
        loadTrace(signal.getTrace());
    }

    /**
     * Handler for the trace selected signal
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public void traceSelected(final TmfTraceSelectedSignal signal) {
        if (signal.getTrace() == fTrace) {
            return;
        }
        loadTrace(signal.getTrace());
    }

    /**
     * Trace is closed: clear the data structures and the viewer
     *
     * @param signal
     *            the signal received
     */
    @TmfSignalHandler
    public void traceClosed(final TmfTraceClosedSignal signal) {
        resetViewer(signal.getTrace());
        if (signal.getTrace() == fTrace) {
            fTrace = null;
            fEditorFile = null;
            setStartTime(SWT.DEFAULT);
            setEndTime(SWT.DEFAULT);
            refresh();
        }
    }

    /**
     * Trace is updated: update the viewer range
     *
     * @param signal
     *            the signal received
     */
    @TmfSignalHandler
    public void traceUpdated(final TmfTraceUpdatedSignal signal) {
        if (signal.getTrace() == fTrace) {
            setTimeBoundsAndRefresh();
        }
    }

    /**
     * Handler for the selection range signal.
     *
     * @param signal
     *            The signal that's received
     */
    @TmfSignalHandler
    public void selectionRangeUpdated(final TmfSelectionRangeUpdatedSignal signal) {
        final ITmfTrace trace = fTrace;
        if (signal.getSource() == this || trace == null) {
            return;
        }
        ITmfTrace signalTrace = signal.getTrace();
        if (signalTrace != null && !TmfTraceManager.getInstance().isSynchronized(trace, signalTrace)) {
            return;
        }
        TmfTraceContext ctx = TmfTraceManager.getInstance().getTraceContext(trace);
        long beginTime = ctx.getSelectionRange().getStartTime().toNanos();
        long endTime = ctx.getSelectionRange().getEndTime().toNanos();

        Display.getDefault().asyncExec(() -> {
            if (fTimeGraphViewer.getControl().isDisposed()) {
                return;
            }
            if (beginTime == endTime) {
                fTimeGraphViewer.setSelectedTime(beginTime, true);
            } else {
                fTimeGraphViewer.setSelectionRange(beginTime, endTime, true);
            }
            synchingToTime(fTimeGraphViewer.getSelectionBegin());
        });
    }

    /**
     * Handler for the window range signal.
     *
     * @param signal
     *            The signal that's received
     */
    @TmfSignalHandler
    public void windowRangeUpdated(final TmfWindowRangeUpdatedSignal signal) {
        if (signal.getSource() == this || fTrace == null) {
            return;
        }
        TmfTraceContext ctx = TmfTraceManager.getInstance().getTraceContext(Objects.requireNonNull(fTrace));
        final long startTime = ctx.getWindowRange().getStartTime().toNanos();
        final long endTime = ctx.getWindowRange().getEndTime().toNanos();
        Display.getDefault().asyncExec(() -> {
            if (fTimeGraphViewer.getControl().isDisposed()) {
                return;
            }
            if (startTime == fTimeGraphViewer.getTime0() && endTime == fTimeGraphViewer.getTime1()) {
                return;
            }
            fTimeGraphViewer.setStartFinishTime(startTime, endTime);
            startZoomThread(startTime, endTime);
        });
    }

    /**
     * @param signal
     *            the format of the timestamps was updated.
     */
    @TmfSignalHandler
    public void updateTimeFormat(final TmfTimestampFormatUpdateSignal signal) {
        updateTimeFormat();
        fTimeGraphViewer.refresh();
    }

    /**
     * A marker event source has been updated
     *
     * @param signal
     *            the signal
     */
    @TmfSignalHandler
    public void markerEventSourceUpdated(final TmfMarkerEventSourceUpdatedSignal signal) {
        getTimeGraphViewer().setMarkerCategories(getMarkerCategories());
        getTimeGraphViewer().setMarkers(null);
        refresh();
    }

    /**
     * Gets the trace displayed in the view.
     *
     * @return the trace
     */
    public ITmfTrace getTrace() {
        return fTrace;
    }

    /**
     * Getter for the presentation provider
     *
     * @return The time graph presentation provider
     */
    protected ITimeGraphPresentationProvider getPresentationProvider() {
        return fPresentation;
    }

    /**
     * Sets the comparator class for the entries.
     * <p>
     * This comparator will apply recursively to entries that implement
     * {@link TimeGraphEntry#sortChildren(Comparator)}.
     *
     * @param comparator
     *            A comparator object
     */
    protected void setEntryComparator(final Comparator<ITimeGraphEntry> comparator) {
        fEntryComparator = comparator;
    }

    /**
     * Method called when the viewer is being loaded with a trace, ie when a
     * trace becomes active in the viewer because it was opened, selected,
     * pinned, etc. When this is called, the trace is already loaded in the base
     * class, but it has not been redrawn or entries built yet. Implementing
     * classes can add any logic here for a specific trace.
     *
     * Unlike the {@link #resetViewer(ITmfTrace)} method, this one is called
     * even if entries already exist for the trace. And if entries need to be
     * rebuilt, both {@link #resetViewer(ITmfTrace)} and this method will be
     * called
     *
     * @param trace
     *            The trace that is being loaded
     */
    protected void loadingTrace(@NonNull ITmfTrace trace) {
        // To be implemented by children classes
    }

    /**
     * Return the list of traces whose data or analysis results will be used to
     * populate the viewer. By default, if the trace is an experiment, the
     * traces under it will be returned, otherwise, the trace itself is
     * returned.
     *
     * A build thread will be started for each trace returned by this method,
     * some of which may receive events in live streaming mode.
     *
     * @param trace
     *            The trace associated with this view, can be null
     * @return List of traces with data to display
     */
    protected @NonNull Iterable<ITmfTrace> getTracesToBuild(@Nullable ITmfTrace trace) {
        return TmfTraceManager.getTraceSet(trace);
    }

    /**
     * Get the set of items that are currently visible in the viewer, according
     * to the visible area height, the vertical scroll position and the expanded
     * state of the items. A buffer of items above and below can be included.
     *
     * @param buffer
     *            number of items above and below the current visible area that
     *            should be included
     * @return a set of visible items in the viewer with buffer above and below
     */
    protected @NonNull Set<@NonNull TimeGraphEntry> getVisibleItems(int buffer) {
        TimeGraphControl timeGraphControl = fTimeGraphViewer.getTimeGraphControl();
        if (timeGraphControl.isDisposed()) {
            return Collections.emptySet();
        }

        int start = Integer.max(0, fTimeGraphViewer.getTopIndex() - buffer);
        int end = Integer.min(fTimeGraphViewer.getExpandedElementCount() - 1,
                fTimeGraphViewer.getTopIndex() + timeGraphControl.countPerPage() + buffer);

        Set<@NonNull TimeGraphEntry> visible = new HashSet<>(end - start + 1);
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

    /**
     * Build the entry list to show in this time graph view.
     * <p>
     * Called from the BuildJob for each trace returned by
     * {@link #getTracesToBuild(ITmfTrace)}.
     * <p>
     * Root entries must be added to the entry list by calling the
     * {@link #addToEntryList(ITmfTrace, List)} method with the list of entries
     * to add and where the trace in parameter should be the parentTrace.
     * Entries that are children of other entries will be automatically picked
     * up after refreshing the root entries.
     * <p>
     * The full event list is also normally computed for every entry that is
     * created. It should be set for each entry by calling the
     * {@link TimeGraphEntry#setEventList(List)}. These full event lists will be
     * used to display something while the zoomed event lists are being
     * calculated when the window range is updated. Also, when fully zoomed out,
     * it is this list of events that is displayed.
     * <p>
     * Also, when all the entries have been added and their events set, this
     * method can finish by calling the refresh() method like this:
     *
     * <pre>
     * if (parentTrace.equals(getTrace())) {
     *     refresh();
     * }
     * </pre>
     *
     * @param trace
     *            The trace being built
     * @param parentTrace
     *            The parent of the trace set, or the trace itself
     * @param monitor
     *            The progress monitor object
     */
    protected abstract void buildEntryList(@NonNull ITmfTrace trace, @NonNull ITmfTrace parentTrace, @NonNull IProgressMonitor monitor);

    /**
     * Gets the list of event for an entry in a given time range.
     * <p>
     * Called from the ZoomThread for every entry to update the zoomed event
     * list. Can be an empty implementation if the viewer does not support
     * zoomed event lists. Can also be used to compute the full event list.
     *
     * @param entry
     *            The entry to get events for
     * @param startTime
     *            Start of the time range
     * @param endTime
     *            End of the time range
     * @param resolution
     *            The resolution
     * @param monitor
     *            The progress monitor object
     * @return The list of events for the entry
     */
    protected @Nullable List<@NonNull ITimeEvent> getEventList(@NonNull TimeGraphEntry entry,
            long startTime, long endTime, long resolution, @NonNull IProgressMonitor monitor) {
        return new ArrayList<>();
    }

    /**
     * Gets the list of view-specific marker categories. Default implementation
     * returns an empty list.
     *
     * @return The list of marker categories
     */
    protected @NonNull List<String> getViewMarkerCategories() {
        return new ArrayList<>();
    }

    /**
     * Gets the list of view-specific markers for a trace in a given time range.
     * Default implementation returns an empty list.
     *
     * @param startTime
     *            Start of the time range
     * @param endTime
     *            End of the time range
     * @param resolution
     *            The resolution
     * @param monitor
     *            The progress monitor object
     * @return The list of marker events
     */
    protected @NonNull List<IMarkerEvent> getViewMarkerList(long startTime, long endTime,
            long resolution, @NonNull IProgressMonitor monitor) {
        return new ArrayList<>();
    }

    /**
     * Gets the list of links (displayed as arrows) for a trace in a given
     * timerange. Default implementation returns an empty list.
     *
     * @param startTime
     *            Start of the time range
     * @param endTime
     *            End of the time range
     * @param resolution
     *            The resolution
     * @param monitor
     *            The progress monitor object
     * @return The list of link events
     */
    protected @Nullable List<@NonNull ILinkEvent> getLinkList(long startTime, long endTime,
            long resolution, @NonNull IProgressMonitor monitor) {
        return new ArrayList<>();
    }

    /**
     * Gets the list of viewer-specific marker categories. Default
     * implementation returns an empty list.
     *
     * @return The list of marker categories
     */
    protected @NonNull List<String> getViewerMarkerCategories() {
        return new ArrayList<>();
    }

    /**
     * Gets the list of viewer-specific markers for a trace in a given time
     * range. Default implementation returns an empty list.
     *
     * @param startTime
     *            Start of the time range
     * @param endTime
     *            End of the time range
     * @param resolution
     *            The resolution
     * @param monitor
     *            The progress monitor object
     * @return The list of marker events
     */
    protected @NonNull List<IMarkerEvent> getViewerMarkerList(long startTime, long endTime,
            long resolution, @NonNull IProgressMonitor monitor) {
        return new ArrayList<>();
    }

    /**
     * Gets the list of trace-specific markers for a trace in a given time
     * range.
     *
     * @param startTime
     *            Start of the time range
     * @param endTime
     *            End of the time range
     * @param resolution
     *            The resolution
     * @param monitor
     *            The progress monitor object
     * @return The list of marker events
     */
    protected @NonNull List<IMarkerEvent> getTraceMarkerList(long startTime, long endTime,
            long resolution, @NonNull IProgressMonitor monitor) {
        List<IMarkerEvent> markers = new ArrayList<>();
        for (IMarkerEventSource markerEventSource : getMarkerEventSources(fTrace)) {
            markers.addAll(markerEventSource.getMarkerList(startTime, endTime, resolution, monitor));
        }
        return markers;
    }

    /**
     * Get the list of current marker categories.
     *
     * @return The list of marker categories
     */
    protected @NonNull List<String> getMarkerCategories() {
        Set<String> categories = new LinkedHashSet<>(getViewerMarkerCategories());
        for (IMarkerEventSource markerEventSource : getMarkerEventSources(fTrace)) {
            categories.addAll(markerEventSource.getMarkerCategories());
        }
        return new ArrayList<>(categories);
    }

    /**
     * Sets the filter column labels.
     * <p>
     * This should be called from the constructor.
     *
     * @param filterColumns
     *            The array of filter column labels
     */
    protected void setFilterColumns(final String[] filterColumns) {
        fFilterColumns = Arrays.copyOf(filterColumns, filterColumns.length);
    }

    /**
     * Sets the filter content provider.
     * <p>
     * This should be called from the constructor.
     *
     * @param contentProvider
     *            The filter content provider
     * @since 1.2
     */
    protected void setFilterContentProvider(final ITreeContentProvider contentProvider) {
        fFilterContentProvider = contentProvider;
    }

    /**
     * Sets the filter label provider.
     * <p>
     * This should be called from the constructor.
     *
     * @param labelProvider
     *            The filter label provider
     */
    protected void setFilterLabelProvider(final TreeLabelProvider labelProvider) {
        fFilterLabelProvider = labelProvider;
    }

    /**
     * Gets the display width
     *
     * @return the display width
     */
    protected int getDisplayWidth() {
        return fDisplayWidth;
    }

    /**
     * Gets the start time
     *
     * @return The start time
     */
    protected long getStartTime() {
        return fStartTime;
    }

    /**
     * Sets the start time
     *
     * @param time
     *            The start time
     */
    protected void setStartTime(long time) {
        fStartTime = time;
    }

    /**
     * Gets the end time
     *
     * @return The end time
     */
    protected long getEndTime() {
        return fEndTime;
    }

    /**
     * Sets the end time
     *
     * @param time
     *            The end time
     */
    protected void setEndTime(long time) {
        fEndTime = time;
    }

    /**
     * Gets the entry list for a trace
     *
     * @param trace
     *            the trace
     *
     * @return the entry list map
     */
    protected @Nullable List<@NonNull TimeGraphEntry> getEntryList(ITmfTrace trace) {
        synchronized (fEntryListMap) {
            return fEntryListMap.get(trace);
        }
    }

    /**
     * Adds a trace entry list to the entry list map
     *
     * @param trace
     *            the trace to add
     * @param list
     *            the list of time graph entries
     */
    protected void putEntryList(ITmfTrace trace, List<@NonNull TimeGraphEntry> list) {
        synchronized (fEntryListMap) {
            fEntryListMap.put(trace, new CopyOnWriteArrayList<>(list));
        }
    }

    /**
     * Adds a list of entries to a trace's entry list
     *
     * @param trace
     *            the trace
     * @param list
     *            the list of time graph entries to add
     */
    protected void addToEntryList(ITmfTrace trace, List<@NonNull TimeGraphEntry> list) {
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

    /**
     * Removes a list of entries from a trace's entry list
     *
     * @param trace
     *            the trace
     * @param list
     *            the list of time graph entries to remove
     */
    protected void removeFromEntryList(ITmfTrace trace, List<TimeGraphEntry> list) {
        synchronized (fEntryListMap) {
            List<TimeGraphEntry> entryList = fEntryListMap.get(trace);
            if (entryList != null) {
                entryList.removeAll(list);
            }
        }
    }

    /**
     * Gets the list of marker event sources for a given trace.
     *
     * @param trace
     *            The trace
     * @return The list of marker event sources
     */
    private @NonNull List<IMarkerEventSource> getMarkerEventSources(ITmfTrace trace) {
        List<IMarkerEventSource> markerEventSources = fMarkerEventSourcesMap.get(trace);
        if (markerEventSources == null) {
            markerEventSources = Collections.emptyList();
        }
        return markerEventSources;
    }

    // ------------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------------

    private void updateTimeFormat() {
        if (fTimeFormat == null) {
            String datime = TmfTimePreferences.getPreferenceMap().get(ITmfTimePreferencesConstants.DATIME);
            if (ITmfTimePreferencesConstants.TIME_ELAPSED_FMT.equals(datime)) {
                fTimeGraphViewer.setTimeFormat(TimeFormat.RELATIVE);
            } else {
                fTimeGraphViewer.setTimeFormat(TimeFormat.CALENDAR);
            }
        } else {
            fTimeGraphViewer.setTimeFormat(fTimeFormat);
        }
    }

    private void loadTrace(final ITmfTrace trace) {
        if (fZoomThread != null) {
            fZoomThread.cancel();
            fZoomThread = null;
        }
        if (fTrace != null) {
            /* save the filters of the previous trace */
            fFiltersMap.put(fTrace, fTimeGraphViewer.getFilters());
            fViewerContext.put(fTrace, new ViewerContext(fCurrentSortColumn, fSortDirection, fTimeGraphViewer.getSelection(), fTimeGraphViewer.getAllCollapsedElements()));
        }
        fTrace = trace;

        TraceCompassLogUtils.traceInstant(LOGGER, Level.FINE, "MultiTimeGraphViewer:LoadingTrace", "trace", trace.getName(), "viewerId", getViewerId()); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$

        restoreViewContext();
        fEditorFile = TmfTraceManager.getInstance().getTraceEditorFile(trace);
        synchronized (fEntryListMap) {
            fEntryList = fEntryListMap.get(fTrace);
            loadingTrace(trace);
            if (fEntryList == null) {
                rebuild();
            } else {
                setTimeBoundsAndRefresh();
            }
        }
        getPresentationProvider().refresh();
    }

    private void setTimeBoundsAndRefresh() {
        setStartTime(fTrace.getStartTime().toNanos());
        setEndTime(fTrace.getEndTime().toNanos());
        refresh();
    }

    /**
     * Forces a rebuild of the entries list, even if entries already exist for
     * this trace
     */
    protected void rebuild() {
        try (FlowScopeLog parentLogger = new FlowScopeLogBuilder(LOGGER, Level.FINE, "MultiTimeGraphViewer:Rebuilding").setCategory(getViewerId()).build()) { //$NON-NLS-1$
            setTimeBoundsAndRefresh();
            ITmfTrace viewerTrace = fTrace;
            if (viewerTrace == null) {
                return;
            }
            resetViewer(viewerTrace);

            List<IMarkerEventSource> markerEventSources = new ArrayList<>();
            synchronized (fBuildJobMap) {
                // Run the build jobs through the site progress service if
                // available
                IWorkbenchSiteProgressService service = null;
                if (fSite != null) {
                    service = fSite.getService(IWorkbenchSiteProgressService.class);
                }
                for (ITmfTrace trace : getTracesToBuild(viewerTrace)) {
                    if (trace == null) {
                        break;
                    }
                    List<@NonNull IMarkerEventSource> adapters = TmfTraceAdapterManager.getAdapters(trace, IMarkerEventSource.class);
                    markerEventSources.addAll(adapters);

                    Job buildJob = new Job(getName() + Messages.AbstractTimeGraphView_BuildJob) {
                        @Override
                        protected IStatus run(IProgressMonitor monitor) {
                            new BuildRunnable(trace, viewerTrace, parentLogger).run(monitor);
                            monitor.done();
                            return Status.OK_STATUS;
                        }
                    };
                    fBuildJobMap.put(trace, buildJob);
                    if (service != null) {
                        service.schedule(buildJob);
                    } else {
                        buildJob.schedule();
                    }
                }
            }
            fMarkerEventSourcesMap.put(viewerTrace, markerEventSources);
        }
    }

    /**
     * Triggers a rebuild of the entries and rows. The entries are typically
     * immutable once set and the underlying analysis is complete, but they can
     * sometimes change, given some user action or other. If this is the case,
     * this method should be called.
     */
    public void triggerRebuild() {
        rebuild();
    }

    /**
     * Method called when synching to a given timestamp. Inheriting classes can
     * perform actions here to update the viewer at the given timestamp.
     *
     * @param time
     *            The currently selected time
     */
    protected void synchingToTime(long time) {
    }

    @Override
    public void refresh() {
        try (FlowScopeLog parentLogger = new FlowScopeLogBuilder(LOGGER, Level.FINE, "RefreshRequested").setCategory(getViewerId()).build()) { //$NON-NLS-1$
            final boolean isZoomThread = Thread.currentThread() instanceof ZoomThread;
            TmfUiRefreshHandler.getInstance().queueUpdate(this, () -> {
                try (FlowScopeLog log = new FlowScopeLogBuilder(LOGGER, Level.FINE, "MultiTimeGraphViewer:Refresh").setParentScope(parentLogger).build()) { //$NON-NLS-1$
                    if (fTimeGraphViewer.getControl().isDisposed()) {
                        return;
                    }
                    fDirty.incrementAndGet();
                    try {
                        synchronized (fEntryListMap) {
                            fEntryList = fEntryListMap.get(fTrace);
                            if (fEntryList == null) {
                                fEntryList = new CopyOnWriteArrayList<>();
                            } else if (fEntryComparator != null) {
                                List<TimeGraphEntry> list = new ArrayList<>(fEntryList);
                                Collections.sort(list, fEntryComparator);
                                for (ITimeGraphEntry entry : list) {
                                    sortChildren(entry, fEntryComparator);
                                }
                                fEntryList.clear();
                                fEntryList.addAll(list);
                            }
                        }
                        boolean inputChanged = fEntryList != fTimeGraphViewer.getInput();
                        if (inputChanged) {
                            fTimeGraphViewer.setInput(fEntryList);
                            /*
                             * restore the previously saved filters, if any
                             */
                            fTimeGraphViewer.setFilters(fFiltersMap.get(fTrace));
                            fTimeGraphViewer.setLinks(null);
                            fTimeGraphViewer.setBookmarks(refreshBookmarks(fEditorFile));
                            fTimeGraphViewer.setMarkerCategories(getMarkerCategories());
                            fTimeGraphViewer.setMarkers(null);
                            applyViewContext();
                        } else {
                            fTimeGraphViewer.refresh();
                        }
                        // reveal selection
                        if (fIsRevealSelection) {
                            fIsRevealSelection = false;
                            fTimeGraphViewer.setSelection(fTimeGraphViewer.getSelection(), true);
                        }
                        long startBound = (fStartTime == Long.MAX_VALUE ? SWT.DEFAULT : fStartTime);
                        long endBound = (fEndTime == Long.MIN_VALUE ? SWT.DEFAULT : fEndTime);
                        fTimeGraphViewer.setTimeBounds(startBound, endBound);

                        ITmfTrace trace = fTrace;
                        TmfTraceContext ctx = (trace == null) ? null : TmfTraceManager.getInstance().getTraceContext(trace);
                        long selectionBeginTime = ctx == null ? SWT.DEFAULT : ctx.getSelectionRange().getStartTime().toNanos();
                        long selectionEndTime = ctx == null ? SWT.DEFAULT : ctx.getSelectionRange().getEndTime().toNanos();
                        long startTime = ctx == null ? SWT.DEFAULT : ctx.getWindowRange().getStartTime().toNanos();
                        long endTime = ctx == null ? SWT.DEFAULT : ctx.getWindowRange().getEndTime().toNanos();
                        if (fStartTime > fEndTime) {
                            startTime = SWT.DEFAULT;
                            endTime = SWT.DEFAULT;
                        } else {
                            startTime = Math.min(Math.max(startTime, fStartTime), fEndTime);
                            endTime = Math.min(Math.max(endTime, fStartTime), fEndTime);
                        }
                        fTimeGraphViewer.setSelectionRange(selectionBeginTime, selectionEndTime, false);
                        fTimeGraphViewer.setStartFinishTime(startTime, endTime);

                        if (inputChanged && selectionBeginTime != SWT.DEFAULT) {
                            synchingToTime(selectionBeginTime);
                        }

                        ZoomThread zoomThread = fZoomThread;
                        if (!isZoomThread ||
                                (zoomThread != null && (zoomThread.getZoomStartTime() != startTime || zoomThread.getZoomEndTime() != endTime))) {
                            startZoomThread(startTime, endTime);
                        }
                    } finally {
                        if (fDirty.decrementAndGet() < 0) {
                            Activator.getDefault().logError(DIRTY_UNDERFLOW_ERROR, new Throwable());
                        }
                    }
                }
            });
        }
    }

    /**
     * Redraw the canvas
     */
    protected void redraw() {
        synchronized (fSyncObj) {
            if (fRedrawState == State.IDLE) {
                fRedrawState = State.BUSY;
            } else {
                fRedrawState = State.PENDING;
                return;
            }
        }
        try (FlowScopeLog flowParent = new FlowScopeLogBuilder(LOGGER, Level.FINE, "RedrawRequested").setCategory(getViewerId()).build()) { //$NON-NLS-1$
            Display.getDefault().asyncExec(() -> {
                try (FlowScopeLog log = new FlowScopeLogBuilder(LOGGER, Level.FINE, "MultiTimeGraphViewer:Redraw").setParentScope(flowParent).build()) { //$NON-NLS-1$
                    if (fTimeGraphViewer.getControl().isDisposed()) {
                        return;
                    }
                    fTimeGraphViewer.getControl().redraw();
                    fTimeGraphViewer.getControl().update();
                    synchronized (fSyncObj) {
                        if (fRedrawState == State.PENDING) {
                            fRedrawState = State.IDLE;
                            redraw();
                        } else {
                            fRedrawState = State.IDLE;
                        }
                    }
                }
            });
        }
    }

    /**
     * Start or restart the zoom thread. This function needs to be called in the
     * UI thread.
     *
     * @param startTime
     *            the zoom start time
     * @param endTime
     *            the zoom end time
     */
    protected final void startZoomThread(long startTime, long endTime) {
        try (FlowScopeLog log = new FlowScopeLogBuilder(LOGGER, Level.FINE, "MultiTimeGraphViewer:ZoomThreadCreated").setCategory(getViewerId()).build()) { //$NON-NLS-1$
            long clampedStartTime = (fStartTime == Long.MAX_VALUE ? startTime : Math.min(Math.max(startTime, fStartTime), fEndTime));
            long clampedEndTime = (fEndTime == Long.MIN_VALUE ? endTime : Math.max(Math.min(endTime, fEndTime), fStartTime));
            fDirty.incrementAndGet();
            boolean restart = false;
            ZoomThread zoomThread = fZoomThread;
            if (zoomThread != null) {
                zoomThread.cancel();
                if (zoomThread.fZoomStartTime == clampedStartTime && zoomThread.fZoomEndTime == clampedEndTime) {
                    restart = true;
                }
            }
            // This line requires to be in the UI thread
            int timeSpace = getTimeGraphViewer().getTimeSpace();
            if (timeSpace > 0) {
                long resolution = Long.max(1, (clampedEndTime - clampedStartTime) / timeSpace);
                zoomThread = createZoomThread(clampedStartTime, clampedEndTime, resolution, restart);
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
                Activator.getDefault().logError(DIRTY_UNDERFLOW_ERROR, new Throwable());
            }
        }
    }

    /**
     * Create a zoom thread.
     *
     * @param startTime
     *            the zoom start time
     * @param endTime
     *            the zoom end time
     * @param resolution
     *            the resolution
     * @param restart
     *            true if restarting zoom for the same time range
     * @return a zoom thread
     */
    protected @Nullable ZoomThread createZoomThread(long startTime, long endTime, long resolution, boolean restart) {
        return new ZoomThreadByEntry(getVisibleItems(DEFAULT_BUFFER_SIZE), startTime, endTime, resolution);
    }

    private void createColumnSelectionListener(Tree tree) {
        for (int i = 0; i < fColumnComparators.length; i++) {
            final int index = i;
            final Comparator<ITimeGraphEntry> comp = fColumnComparators[index];
            final TreeColumn column = tree.getColumn(i);

            if (comp != null) {
                column.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        TreeColumn prevSortcolumn = tree.getSortColumn();
                        int direction = tree.getSortDirection();
                        if (prevSortcolumn == column) {
                            direction = (direction == SWT.DOWN) ? SWT.UP : SWT.DOWN;
                        } else {
                            direction = SWT.DOWN;
                        }
                        tree.setSortColumn(column);
                        tree.setSortDirection(direction);
                        fSortDirection = direction;
                        fCurrentSortColumn = index;
                        Comparator<ITimeGraphEntry> comparator = comp;

                        if (comparator instanceof ITimeGraphEntryComparator) {
                            ((ITimeGraphEntryComparator) comparator).setDirection(direction);
                        }
                        if (direction != SWT.DOWN) {
                            comparator = Objects.requireNonNull(Collections.reverseOrder(comparator));
                        }
                        setEntryComparator(comparator);
                        fIsRevealSelection = true;
                        fTimeGraphViewer.getControl().setFocus();
                        refresh();
                    }
                });
            }
        }
    }

    private void sortChildren(ITimeGraphEntry entry, Comparator<ITimeGraphEntry> comparator) {
        if (entry instanceof TimeGraphEntry) {
            ((TimeGraphEntry) entry).sortChildren(comparator);
        }
        for (ITimeGraphEntry child : entry.getChildren()) {
            sortChildren(child, comparator);
        }
    }

    private void restoreViewContext() {
        ViewerContext viewerContext = fViewerContext.get(fTrace);
        if (fColumnComparators != null) {
            // restore sort settings
            fSortDirection = SWT.DOWN;
            fCurrentSortColumn = fInitialSortColumn;
            if (viewerContext != null) {
                fSortDirection = viewerContext.getSortDirection();
                fCurrentSortColumn = viewerContext.getSortColumn();
            }
            if ((fCurrentSortColumn < fColumnComparators.length) && (fColumnComparators[fCurrentSortColumn] != null)) {
                Comparator<ITimeGraphEntry> comparator = fColumnComparators[fCurrentSortColumn];
                if (comparator instanceof ITimeGraphEntryComparator) {
                    ((ITimeGraphEntryComparator) comparator).setDirection(fSortDirection);
                }
                if (fSortDirection != SWT.DOWN) {
                    comparator = Objects.requireNonNull(Collections.reverseOrder(comparator));
                }
                setEntryComparator(comparator);
            }
        }
    }

    private void applyViewContext() {
        ViewerContext viewContext = fViewerContext.remove(fTrace);
        applyExpandedStateContext(viewContext);
        if (fColumnComparators != null) {
            final Tree tree = fTimeGraphViewer.getTree();
            final TreeColumn column = tree.getColumn(fCurrentSortColumn);
            tree.setSortDirection(fSortDirection);
            tree.setSortColumn(column);
        }
        // restore and reveal selection
        if ((viewContext != null) && (viewContext.getSelection() != null)) {
            fTimeGraphViewer.setSelection(viewContext.getSelection(), true);
        }
    }

    private void applyExpandedStateContext(ViewerContext viewContext) {
        if (viewContext != null) {
            fTimeGraphViewer.expandAll();
            fTimeGraphViewer.setExpandedState(viewContext.getCollapsedEntries(), false);
        }
    }

    private static List<IMarkerEvent> refreshBookmarks(final IFile editorFile) {
        List<IMarkerEvent> bookmarks = new ArrayList<>();
        if (editorFile == null || !editorFile.exists()) {
            return bookmarks;
        }
        try {
            IMarker[] markers = editorFile.findMarkers(IMarker.BOOKMARK, false, IResource.DEPTH_ZERO);
            for (IMarker marker : markers) {
                String label = marker.getAttribute(IMarker.MESSAGE, (String) null);
                String time = marker.getAttribute(ITmfMarker.MARKER_TIME, (String) null);
                String duration = marker.getAttribute(ITmfMarker.MARKER_DURATION, Long.toString(0));
                String rgba = marker.getAttribute(ITmfMarker.MARKER_COLOR, (String) null);
                if (label != null && time != null && rgba != null) {
                    Matcher matcher = RGBA_PATTERN.matcher(rgba);
                    if (matcher.matches()) {
                        try {
                            int red = Integer.valueOf(matcher.group(1));
                            int green = Integer.valueOf(matcher.group(2));
                            int blue = Integer.valueOf(matcher.group(3));
                            int alpha = Integer.valueOf(matcher.group(4));
                            RGBA color = new RGBA(red, green, blue, alpha);
                            bookmarks.add(new MarkerEvent(null, Long.valueOf(time), Long.valueOf(duration), IMarkerEvent.BOOKMARKS, color, label, true));
                        } catch (NumberFormatException e) {
                            Activator.getDefault().logError(e.getMessage());
                        }
                    }
                }
            }
        } catch (CoreException e) {
            Activator.getDefault().logError(e.getMessage());
        }
        return bookmarks;
    }

    // TODO: This can implement ICoreRunnable once support for Eclipse 4.5. is
    // not necessary anymore.
    private class BuildRunnable {
        private final @NonNull ITmfTrace fBuildTrace;
        private final @NonNull ITmfTrace fParentTrace;
        private final @NonNull FlowScopeLog fScope;

        public BuildRunnable(final @NonNull ITmfTrace trace, final @NonNull ITmfTrace parentTrace, final @NonNull FlowScopeLog log) {
            fBuildTrace = trace;
            fParentTrace = parentTrace;
            fScope = log;
        }

        public void run(IProgressMonitor monitor) {
            try (FlowScopeLog log = new FlowScopeLogBuilder(LOGGER, Level.FINE, "MultiTimeGraphViewer:BuildThread", "trace", fBuildTrace.getName()).setParentScope(fScope).build()) { //$NON-NLS-1$ //$NON-NLS-2$
                buildEntryList(fBuildTrace, fParentTrace, Objects.requireNonNull(monitor));
                synchronized (fBuildJobMap) {
                    fBuildJobMap.remove(fBuildTrace);
                }
            }
        }
    }

    /**
     * Zoom thread
     */
    protected abstract class ZoomThread extends Thread {
        private final long fZoomStartTime;
        private final long fZoomEndTime;
        private final long fResolution;
        private int fScopeId = -1;
        private final @NonNull IProgressMonitor fMonitor;

        /**
         * Constructor
         *
         * @param startTime
         *            the start time
         * @param endTime
         *            the end time
         * @param resolution
         *            the resolution
         */
        public ZoomThread(long startTime, long endTime, long resolution) {
            super(AbstractTimeGraphMultiViewer.this.getName() + " zoom"); //$NON-NLS-1$
            fZoomStartTime = startTime;
            fZoomEndTime = endTime;
            fResolution = resolution;
            fMonitor = new NullProgressMonitor();
        }

        /**
         * @return the zoom start time
         */
        public long getZoomStartTime() {
            return fZoomStartTime;
        }

        /**
         * @return the zoom end time
         */
        public long getZoomEndTime() {
            return fZoomEndTime;
        }

        /**
         * @return the resolution
         */
        public long getResolution() {
            return fResolution;
        }

        /**
         * @return the monitor
         */
        public @NonNull IProgressMonitor getMonitor() {
            return fMonitor;
        }

        /**
         * Cancel the zoom thread
         */
        public void cancel() {
            fMonitor.setCanceled(true);
        }

        @Override
        public final void run() {
            try (FlowScopeLog log = new FlowScopeLogBuilder(LOGGER, Level.FINE, "MultiTimeGraphViewer:ZoomThread", "start", fZoomStartTime, "end", fZoomEndTime).setCategoryAndId(getViewerId(), fScopeId).build()) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                doRun();
            } finally {
                if (fDirty.decrementAndGet() < 0) {
                    Activator.getDefault().logError(DIRTY_UNDERFLOW_ERROR, new Throwable());
                }
            }
        }

        /**
         * Applies the results of the ZoomThread calculations asynchronously on
         * the UI thread.
         * <p>
         * Note: This method makes sure that only the results of the last
         * created ZoomThread are applied.
         *
         * @param runnable
         *            the code to run in order to apply the results
         */
        protected void applyResults(Runnable runnable) {
            AbstractTimeGraphMultiViewer.this.applyResults(runnable);
        }

        /**
         * Run the zoom operation.
         *
         */
        public abstract void doRun();

        /**
         * Set the ID of the calling flow scope. This data will allow to
         * determine the causality between the zoom thread and its caller if
         * tracing is enabled.
         *
         * @param scopeId
         *            The ID of the calling flow scope
         */
        public void setScopeId(int scopeId) {
            fScopeId = scopeId;
        }
    }

    /**
     * Applies the results of the ZoomThread calculations asynchronously on the
     * UI thread.
     * <p>
     * Note: This method makes sure that only the results of the last created
     * ZoomThread are applied.
     *
     * @param runnable
     *            the code to run in order to apply the results
     */
    protected void applyResults(Runnable runnable) {
        synchronized (fZoomThreadResultLock) {
            if (Thread.currentThread() == fZoomThread) {
                Display.getDefault().asyncExec(runnable);
            }
        }
    }

    private class ZoomThreadByEntry extends ZoomThread {
        private final @NonNull Collection<@NonNull TimeGraphEntry> fEntries;

        public ZoomThreadByEntry(@NonNull Collection<@NonNull TimeGraphEntry> entries, long startTime, long endTime, long resolution) {
            super(startTime, endTime, resolution);
            fEntries = entries;
        }

        @Override
        public void doRun() {
            Sampling sampling = new Sampling(getZoomStartTime(), getZoomEndTime(), getResolution());
            boolean isFilterActive = !getRegexes().values().isEmpty();
            try (TraceCompassLogUtils.ScopeLog log = new TraceCompassLogUtils.ScopeLog(LOGGER, Level.FINER, "ZoomThread:GettingStates")) { //$NON-NLS-1$
                boolean isFilterCleared = !isFilterActive && getTimeGraphViewer().isTimeEventFilterActive();
                getTimeGraphViewer().setTimeEventFilterApplied(isFilterActive);

                boolean hasSavedFilter = fTimeEventFilterDialog != null && fTimeEventFilterDialog.hasActiveSavedFilters();
                getTimeGraphViewer().setSavedFilterStatus(hasSavedFilter);

                Iterable<@NonNull TimeGraphEntry> incorrectSample = Iterables.filter(fEntries, entry -> isFilterActive || isFilterCleared || !sampling.equals(entry.getSampling()));
                zoomEntries(incorrectSample, getZoomStartTime(), getZoomEndTime(), getResolution(), getMonitor());
            }
            List<ILinkEvent> computedLinks;
            try (TraceCompassLogUtils.ScopeLog linkLog = new TraceCompassLogUtils.ScopeLog(LOGGER, Level.FINER, "ZoomThread:GettingLinks")) { //$NON-NLS-1$
                /* Refresh the arrows when zooming */
                computedLinks = getLinkList(getZoomStartTime(), getZoomEndTime(), getResolution(), getMonitor());
                TimeEventFilterDialog filterDialog = getTimeEventFilterDialog();
                if (filterDialog != null && computedLinks != null) {
                    if (filterDialog.hasActiveSavedFilters()) {
                        computedLinks = Collections.emptyList();
                    } else {
                        computedLinks.forEach(link -> link.setProperty(CoreFilterProperty.DIMMED, filterDialog.isFilterActive()));
                    }
                }
            }
            List<ILinkEvent> links = computedLinks;
            /* Refresh the viewer-specific markers when zooming */
            try (TraceCompassLogUtils.ScopeLog markerLoglog = new TraceCompassLogUtils.ScopeLog(LOGGER, Level.FINER, "ZoomThread:GettingMarkers")) { //$NON-NLS-1$
                List<IMarkerEvent> markers = new ArrayList<>(getViewerMarkerList(getZoomStartTime(), getZoomEndTime(), getResolution(), getMonitor()));
                /* Refresh the trace-specific markers when zooming */
                markers.addAll(getTraceMarkerList(getZoomStartTime(), getZoomEndTime(), getResolution(), getMonitor()));
                applyResults(() -> {
                    if (links != null) {
                        fTimeGraphViewer.setLinks(links);
                    }
                    fTimeGraphViewer.setMarkerCategories(getMarkerCategories());
                    fTimeGraphViewer.setMarkers(markers);
                });
                synchronized (fZoomThreadResultLock) {
                    if (Thread.currentThread() == fZoomThread) {
                        refresh();
                    }
                }
            }

            if (isFilterActive && Thread.currentThread() == fZoomThread) {
                /* Do a full filter search as a second pass */
                try (TraceCompassLogUtils.ScopeLog log = new TraceCompassLogUtils.ScopeLog(LOGGER, Level.FINER, "ZoomThread:GettingStatesFullSearch")) { //$NON-NLS-1$
                    for (TimeGraphEntry entry : fEntries) {
                        if (getMonitor().isCanceled()) {
                            return;
                        }
                        zoomEntries(Collections.singleton(entry), getZoomStartTime(), getZoomEndTime(), getResolution(), true, getMonitor());
                        refresh();
                    }
                }
            }
        }

    }

    /**
     * Add events from the queried time range to the queried entries.
     * <p>
     * Called from the ZoomThread for every entry to update the zoomed event
     * list.
     * <p>
     * The implementation should call
     * {@link TimeGraphEntry#setSampling(Sampling)} if the zoomed event list is
     * successfully set.
     *
     * @param entries
     *            List of entries to zoom on.
     * @param zoomStartTime
     *            Start of the time range
     * @param zoomEndTime
     *            End of the time range
     * @param resolution
     *            The resolution
     * @param monitor
     *            The progress monitor object
     */
    protected void zoomEntries(@NonNull Iterable<@NonNull TimeGraphEntry> entries,
            long zoomStartTime, long zoomEndTime, long resolution, @NonNull IProgressMonitor monitor) {
        zoomEntries(entries, zoomStartTime, zoomEndTime, resolution, false, monitor);
    }

    /**
     * Add events from the queried time range to the queried entries.
     * <p>
     * Called from the ZoomThread for every entry to update the zoomed event
     * list.
     * <p>
     * The implementation should call
     * {@link TimeGraphEntry#setSampling(Sampling)} if the zoomed event list is
     * successfully set.
     * <p>
     * When a full search is requested, the gaps in between samples of the
     * queried time range should be searched and at least one event matching the
     * regex filter should be included per gap, if any is found.
     *
     * @param entries
     *            List of entries to zoom on.
     * @param zoomStartTime
     *            Start of the time range
     * @param zoomEndTime
     *            End of the time range
     * @param resolution
     *            The resolution
     * @param fullSearch
     *            True to perform a full search
     * @param monitor
     *            The progress monitor object
     */
    protected void zoomEntries(@NonNull Iterable<@NonNull TimeGraphEntry> entries,
            long zoomStartTime, long zoomEndTime, long resolution, boolean fullSearch, @NonNull IProgressMonitor monitor) {

        try {
            Map<Integer, Predicate<Multimap<String, Object>>> predicates = generateRegexPredicate();

            for (TimeGraphEntry entry : entries) {
                List<ITimeEvent> zoomedEventList = getEventList(entry, zoomStartTime, zoomEndTime, fullSearch ? 1L : resolution, monitor);
                if (monitor.isCanceled()) {
                    return;
                }
                if (zoomedEventList != null) {
                    doFilterEvents(entry, zoomedEventList, predicates);
                    applyResults(() -> entry.setZoomedEventList(zoomedEventList));
                }
            }
            redraw();
        } catch (PatternSyntaxException e) {
            Activator.getDefault().logInfo("Invalid regex"); //$NON-NLS-1$
        }
    }

    /**
     * Filter the given eventList using the predicates
     *
     * @param entry
     *            The timegraph entry
     * @param eventList
     *            The event list at this zoom level
     * @param predicates
     *            The predicate for the filter dialog text box
     */
    @NonNullByDefault
    protected void doFilterEvents(TimeGraphEntry entry, List<ITimeEvent> eventList, Map<Integer, Predicate<Multimap<String, Object>>> predicates) {
        if (!predicates.isEmpty()) {
            // For each event in the events list, test each predicates and set
            // the status of the property associated to the predicate
            eventList.forEach(te -> {
                for (Map.Entry<Integer, Predicate<Multimap<String, Object>>> mapEntry : predicates.entrySet()) {
                    Predicate<Multimap<String, Object>> value = Objects.requireNonNull(mapEntry.getValue());
                    Multimap<String, Object> toTest = HashMultimap.create();
                    getPresentationProvider().getFilterInput(te).forEach((k, e) -> toTest.put(k, e));
                    te.getMetadata().forEach((k, e) -> toTest.put(k, e));

                    boolean status = value.test(toTest);
                    Integer property = mapEntry.getKey();
                    if (property == CoreFilterProperty.DIMMED || property == CoreFilterProperty.EXCLUDE) {
                        te.setProperty(property, !status);
                    } else {
                        te.setProperty(property, status);
                    }
                }
            });
        }
        fillWithNullEvents(entry, eventList);
    }

    /**
     * Fill the gaps between non-excluded events with null time events
     *
     * @param entry
     *            The entry
     * @param eventList
     *            The entry event list
     */
    private void fillWithNullEvents(TimeGraphEntry entry, List<ITimeEvent> eventList) {
        List<ITimeEvent> filtered = new ArrayList<>();
        if (!eventList.isEmpty()
                && getTimeEventFilterDialog() != null && getTimeEventFilterDialog().hasActiveSavedFilters()) {

            eventList.forEach(te -> {
                // Keep only the events that do not have the 'exclude' property
                // activated
                if (!te.isPropertyActive(CoreFilterProperty.EXCLUDE)) {
                    filtered.add(te);
                }
            });
            long prevTime = eventList.get(0).getTime();
            long endTime = eventList.get(eventList.size() - 1).getTime() + eventList.get(eventList.size() - 1).getDuration();
            eventList.clear();

            // Replace unused events with null time events to fill gaps
            for (ITimeEvent event : filtered) {
                if (prevTime < event.getTime()) {
                    NullTimeEvent nullTimeEvent = new NullTimeEvent(entry, prevTime, event.getTime() - prevTime);
                    nullTimeEvent.setProperty(CoreFilterProperty.DIMMED, true);
                    nullTimeEvent.setProperty(CoreFilterProperty.EXCLUDE, true);
                    eventList.add(nullTimeEvent);
                }
                eventList.add(event);
                prevTime = event.getTime() + event.getDuration();
            }
            if (prevTime < endTime) {
                NullTimeEvent nullTimeEvent = new NullTimeEvent(entry, prevTime, endTime - prevTime);
                nullTimeEvent.setProperty(CoreFilterProperty.DIMMED, true);
                nullTimeEvent.setProperty(CoreFilterProperty.EXCLUDE, true);
                eventList.add(nullTimeEvent);
            }
        }
    }

    /**
     * Generate the predicate for every property from the regexes
     *
     * @return A map of predicate by property
     */
    @NonNullByDefault
    protected Map<Integer, Predicate<Multimap<String, Object>>> generateRegexPredicate() {
        Multimap<Integer, String> regexes = getRegexes();
        Map<Integer, Predicate<Multimap<String, Object>>> predicates = new HashMap<>();
        for (Entry<Integer, Collection<String>> entry : regexes.asMap().entrySet()) {
            String regex = IFilterStrings.mergeFilters(entry.getValue());
            FilterCu cu = FilterCu.compile(regex);
            Predicate<Multimap<String, Object>> predicate = cu != null ? cu.generate() : null;
            if (predicate != null) {
                predicates.put(entry.getKey(), predicate);
            }
        }
        return predicates;
    }

    /**
     * Base class to provide the labels for the tree viewer. Views extending
     * this class typically need to override the getColumnText method if they
     * have more than one column to display
     */
    protected static class TreeLabelProvider implements ITableLabelProvider, ILabelProvider {

        @Override
        public void addListener(ILabelProviderListener listener) {
            // do nothing
        }

        @Override
        public void dispose() {
            // do nothing
        }

        @Override
        public boolean isLabelProperty(Object element, String property) {
            return false;
        }

        @Override
        public void removeListener(ILabelProviderListener listener) {
            // do nothing
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            TimeGraphEntry entry = (TimeGraphEntry) element;
            if (columnIndex == 0) {
                return entry.getName();
            }
            return ""; //$NON-NLS-1$
        }

        @Override
        public Image getImage(Object element) {
            return null;
        }

        @Override
        public String getText(Object element) {
            TimeGraphEntry entry = (TimeGraphEntry) element;
            return entry.getName();
        }

    }

    class TimeGraphPartListener implements IPartListener {
        @Override
        public void partActivated(IWorkbenchPart part) {
            if (part == AbstractTimeGraphMultiViewer.this) {
                synchronized (FIND_ACTION) {
                    if (fFindActionHandler == null) {
                        fFindActionHandler = new ActionHandler(FIND_ACTION);
                    }
                    if (fFindHandlerActivation == null) {
                        final Object service = PlatformUI.getWorkbench().getService(IHandlerService.class);
                        fFindHandlerActivation = ((IHandlerService) service).activateHandler(ActionFactory.FIND.getCommandId(), fFindActionHandler);
                    }
                }
            }
            // Notify action for all parts
            FIND_ACTION.partActivated(part);
        }

        @Override
        public void partDeactivated(IWorkbenchPart part) {
            if ((part == AbstractTimeGraphMultiViewer.this) && (fFindHandlerActivation != null)) {
                final Object service = PlatformUI.getWorkbench().getService(IHandlerService.class);
                ((IHandlerService) service).deactivateHandler(fFindHandlerActivation);
                fFindHandlerActivation = null;
            }
        }

        @Override
        public void partBroughtToTop(IWorkbenchPart part) {
            // do nothing
        }

        @Override
        public void partClosed(IWorkbenchPart part) {
            // do nothing
        }

        @Override
        public void partOpened(IWorkbenchPart part) {
            // do nothing
        }
    }

    class TimeGraphPartListener2 implements IPartListener2 {

        @Override
        public void partActivated(IWorkbenchPartReference partRef) {
            // do nothing
        }

        @Override
        public void partBroughtToTop(IWorkbenchPartReference partRef) {
            // do nothing
        }

        @Override
        public void partClosed(IWorkbenchPartReference partRef) {
            // do nothing
        }

        @Override
        public void partDeactivated(IWorkbenchPartReference partRef) {
            // do nothing
        }

        @Override
        public void partOpened(IWorkbenchPartReference partRef) {
            // do nothing
        }

        @Override
        public void partHidden(IWorkbenchPartReference partRef) {
            IWorkbenchPart part = partRef.getPart(false);
            if (part != null && part == AbstractTimeGraphMultiViewer.this) {
                Display.getDefault().asyncExec(() -> {
                    if (fTimeEventFilterDialog != null) {
                        fTimeEventFilterDialog.close();
                    }
                });
            }
        }

        @Override
        public void partVisible(IWorkbenchPartReference partRef) {
            IWorkbenchPart part = partRef.getPart(false);
            if (part != null && part == AbstractTimeGraphMultiViewer.this) {
                Display.getDefault().asyncExec(() -> {
                    if (fTimeEventFilterDialog != null && fTimeEventFilterDialog.isFilterActive()) {
                        fTimeEventFilterDialog.open();
                    }
                });
            }
        }

        @Override
        public void partInputChanged(IWorkbenchPartReference partRef) {
            // do nothing
        }
    }

    private static class ViewerContext {
        private final int fSortColumnIndex;
        private final int fSortDirection;
        private final @Nullable ITimeGraphEntry fSelection;
        private final @NonNull Set<@NonNull ITimeGraphEntry> fCollapsedEntries;

        ViewerContext(int sortColumn, int sortDirection, ITimeGraphEntry selection, @NonNull Set<@NonNull ITimeGraphEntry> collapsedEntries) {
            fSortColumnIndex = sortColumn;
            fSortDirection = sortDirection;
            fSelection = selection;
            fCollapsedEntries = ImmutableSet.copyOf(collapsedEntries);
        }

        /**
         * @return the sortColumn
         */
        public int getSortColumn() {
            return fSortColumnIndex;
        }

        /**
         * @return the sortDirection
         */
        public int getSortDirection() {
            return fSortDirection;
        }

        /**
         * @return the selection
         */
        public ITimeGraphEntry getSelection() {
            return fSelection;
        }

        /**
         * Get the set of collapsed entries
         *
         * @return The set of collapsed entries
         */
        public @NonNull Set<@NonNull ITimeGraphEntry> getCollapsedEntries() {
            return fCollapsedEntries;
        }
    }

    /**
     * Method to reset the viewer internal data for a given trace.
     *
     * When overriding this method make sure to call the super implementation.
     *
     * @param viewerTrace
     *            trace to reset the viewer for.
     */
    protected void resetViewer(ITmfTrace viewerTrace) {
        if (viewerTrace == null) {
            return;
        }
        synchronized (fBuildJobMap) {
            for (ITmfTrace trace : getTracesToBuild(viewerTrace)) {
                Job buildJob = fBuildJobMap.remove(trace);
                if (buildJob != null) {
                    buildJob.cancel();
                }
            }
        }
        synchronized (fEntryListMap) {
            fEntryListMap.remove(viewerTrace);
        }
        fViewerContext.remove(viewerTrace);
        fFiltersMap.remove(viewerTrace);
        fMarkerEventSourcesMap.remove(viewerTrace);
        if (viewerTrace == fTrace) {
            if (fZoomThread != null) {
                fZoomThread.cancel();
                fZoomThread = null;
            }
        }
    }

    private void createContextMenu() {
        fEntryMenuManager.setRemoveAllWhenShown(true);
        TimeGraphControl timeGraphControl = getTimeGraphViewer().getTimeGraphControl();
        final Menu entryMenu = fEntryMenuManager.createContextMenu(timeGraphControl);
        timeGraphControl.addTimeGraphEntryMenuListener(event -> {
            Point p = timeGraphControl.toControl(event.x, event.y);
            /*
             * The TimeGraphControl will call the TimeGraphEntryMenuListener
             * before the TimeEventMenuListener. If the event is triggered on
             * the name space then show the menu else clear the menu.
             */
            if (p.x < getTimeGraphViewer().getNameSpace()) {
                timeGraphControl.setMenu(entryMenu);
            } else {
                timeGraphControl.setMenu(null);
                event.doit = false;
            }
        });
        fEntryMenuManager.addMenuListener(manager -> {
            fillTimeGraphEntryContextMenu(fEntryMenuManager);
            fEntryMenuManager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        });
        getSite().registerContextMenu(fEntryMenuManager, fTimeGraphViewer.getSelectionProvider());
    }

    /**
     * This method builds the multimap of regexes by property that will be used
     * to filter the timegraph states.
     *
     * Override this method to add other regexes with their properties. The data
     * provider should handle everything after.
     *
     * @return The multimap of regexes by property
     */
    protected @NonNull Multimap<@NonNull Integer, @NonNull String> getRegexes() {
        Multimap<@NonNull Integer, @NonNull String> regexes = HashMultimap.create();

        @NonNull
        String dialogRegex = fTimeEventFilterDialog != null ? fTimeEventFilterDialog.getTextBoxRegex() : ""; //$NON-NLS-1$
        if (!dialogRegex.isEmpty()) {
            regexes.put(CoreFilterProperty.DIMMED, dialogRegex);
        }

        Set<@NonNull String> savedFilters = fTimeEventFilterDialog != null ? fTimeEventFilterDialog.getSavedFilters() : Collections.emptySet();
        for (String savedFilter : savedFilters) {
            regexes.put(CoreFilterProperty.EXCLUDE, savedFilter);
            regexes.put(CoreFilterProperty.DIMMED, savedFilter);
        }

        ITmfTrace trace = fTrace;
        if (trace == null) {
            return regexes;
        }
        TraceCompassFilter globalFilter = TraceCompassFilter.getFilterForTrace(trace);
        if (globalFilter == null) {
            return regexes;
        }
        regexes.putAll(CoreFilterProperty.DIMMED, globalFilter.getRegexes());

        return regexes;
    }

    /**
     * get the time event filter dialog
     *
     * @return The time event filter dialog. Could be null.
     */
    protected TimeEventFilterDialog getTimeEventFilterDialog() {
        return fTimeEventFilterDialog;
    }

    /**
     * Action to show a time graph find dialog to search for a
     * {@link ITimeGraphEntry}
     *
     * @author Jean-Christian Kouame
     */
    static class ShowFindDialogAction extends Action {

        // private static TimeGraphFindDialog fDialog;
        // private FindTarget fFindTarget;

        /**
         * Constructor
         */
        public ShowFindDialogAction() {
            // TODO: decide if needed
        }

        @Override
        public void run() {
            // TODO: decide if needed
        }

        /**
         * Checks if the dialogs shell is the same as the given
         * <code>shell</code> and if not clears the stub and closes the dialog.
         *
         * @param target
         *            the target that owns the shell to check
         */
        public void checkShell(FindTarget target) {
            // TODO: decide if needed
        }

        /**
         * Define what to do when a part is activated.
         *
         * @param part
         *            The activated workbenchPart
         */
        public synchronized void partActivated(IWorkbenchPart part) {
            // TODO: decide if needed
        }

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
        Runnable runnable = () -> {
            startZoomThread(getTimeGraphViewer().getTime0(), getTimeGraphViewer().getTime1());
        };
        Display display = PlatformUI.getWorkbench().getDisplay();
        if (display == Display.getCurrent()) {
            runnable.run();
        } else {
            Display.getDefault().asyncExec(runnable);
        }

    }

    /**
     * Gets the time event filter action
     *
     * @return the timeEventFilterAction
     */
    public Action getTimeEventFilterAction() {
        return fTimeEventFilterAction;
    }

    /**
     * Fill context menu
     *
     * @param menuManager
     *            a menuManager to fill
     */
    protected void fillTimeGraphEntryContextMenu(@NonNull IMenuManager menuManager) {
        // do nothing
    }

    /*
     * Inner classes used for searching
     */
    class FindTarget {
        public ITimeGraphEntry getSelection() {
            return fTimeGraphViewer.getSelection();
        }

        public void selectAndReveal(@NonNull ITimeGraphEntry entry) {
            fTimeGraphViewer.selectAndReveal(entry);
        }

        public ITimeGraphEntry[] getEntries() {
            TimeGraphViewer viewer = getTimeGraphViewer();
            return viewer.getTimeGraphContentProvider().getElements(viewer.getInput());
        }

        public Shell getShell() {
            return fSite.getShell();
        }
    }

}
