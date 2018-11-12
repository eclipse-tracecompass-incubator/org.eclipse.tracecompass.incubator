/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.ui.views.fileaccess;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.tracecompass.analysis.os.linux.core.signals.TmfThreadSelectedSignal;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.incubator.internal.kernel.core.fileaccess.FileAccessAnalysis;
import org.eclipse.tracecompass.incubator.internal.kernel.core.fileaccess.FileAccessDataProvider;
import org.eclipse.tracecompass.incubator.internal.kernel.core.fileaccess.FileEntryModel;
import org.eclipse.tracecompass.incubator.internal.kernel.core.filedescriptor.ThreadEntryModel;
import org.eclipse.tracecompass.incubator.internal.kernel.core.filedescriptor.TidTimeQueryFilter;
import org.eclipse.tracecompass.incubator.internal.kernel.ui.Activator;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.actions.FollowThreadAction;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.resources.ResourcesView;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.presentation.RGBAColor;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEventStyleStrings;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NamedTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphControl;
import org.eclipse.ui.IWorkbenchActionConstants;

import com.google.common.collect.ImmutableMap;

/**
 * File access by file view
 *
 * @author Matthew Khouzam
 *
 */
public class FileAccessByFileView extends BaseDataProviderTimeGraphView {

    private static final String TITLE = Messages.FileAccessByFileView_title;

    private static final String ALL = Messages.FileAccessByFileView_all;

    private static final String ID = "org.eclipse.tracecompass.incubator.kernel.ui.filebyfile"; //$NON-NLS-1$

    private static final Image THREAD_IMAGE = Objects.requireNonNull(Activator.getDefault()).getImageFromPath("icons/obj16/thread_obj.gif"); //$NON-NLS-1$
    private static final Image FILE_IMAGE = Objects.requireNonNull(Activator.getDefault()).getImageFromPath("icons/obj16/file_obj.gif"); //$NON-NLS-1$
    private static final Image ADVANCED_IMAGE = Objects.requireNonNull(Activator.getDefault()).getImageFromPath("icons/obj16/advanced.png"); //$NON-NLS-1$

    private static final String RESOURCE_COLUMN = Messages.FileAccessByFileView_resource;
    private static final String TID_COLUMN = Messages.FileAccessByFileView_thread;
    private static final String READ_COLUMN = Messages.FileAccessByFileView_read;
    private static final String WRITE_COLUMN = Messages.FileAccessByFileView_write;

    private static final String[] COLUMN_NAMES = new String[] {
            RESOURCE_COLUMN,
            TID_COLUMN,
            READ_COLUMN,
            WRITE_COLUMN
    };

    private static final String ADVANCED_MODE_KEY = FileAccessByFileView.class.getCanonicalName() + File.separator + "ADVANCED"; //$NON-NLS-1$

    private static final String FOLLOW_A_THREAD = Messages.FileAccessByFileView_follow;

    private boolean fAdvancedMode = Activator.getDefault().getPreferenceStore().getBoolean(ADVANCED_MODE_KEY);

    private class FileAccessTreeLabelProvider extends TreeLabelProvider {

        @Override
        public @Nullable Image getColumnImage(@Nullable Object element, int columnIndex) {
            if (columnIndex == 0 && element instanceof TimeGraphEntry) {
                TimeGraphEntry entry = (TimeGraphEntry) element;
                ITimeGraphEntryModel entryModel = entry.getModel();
                if (entryModel instanceof FileEntryModel) {
                    return FILE_IMAGE;
                } else if (entryModel instanceof ThreadEntryModel) {
                    return THREAD_IMAGE;
                }
            }
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (!(element instanceof TimeGraphEntry)) {
                return super.getColumnText(element, columnIndex);
            }
            TimeGraphEntry entry = (TimeGraphEntry) element;

            if (columnIndex == 1) {
                ITimeGraphEntryModel model = entry.getModel();
                if (model instanceof ThreadEntryModel) {
                    return String.valueOf(((ThreadEntryModel) model).getTid());
                }
                return super.getColumnText(element, columnIndex);
            }
            if (columnIndex == 2) {
                FileAccessDataProvider dp = DataProviderManager
                        .getInstance().getDataProvider(getTrace(), getProviderId(), FileAccessDataProvider.class);
                ITmfTrace activeTrace = TmfTraceManager.getInstance().getActiveTrace();
                if (activeTrace != null && dp != null) {
                    TmfTimeRange tr = activeTrace.getTimeRange();
                    return NonNullUtils.nullToEmptyString(dp.getBytesRead(tr.getStartTime().toNanos(), tr.getEndTime().toNanos(), entry.getModel().getId()));
                }
            }
            if (columnIndex == 3) {
                FileAccessDataProvider dp = DataProviderManager
                        .getInstance().getDataProvider(getTrace(), getProviderId(), FileAccessDataProvider.class);
                ITmfTrace activeTrace = TmfTraceManager.getInstance().getActiveTrace();
                if (activeTrace != null && dp != null) {
                    TmfTimeRange tr = activeTrace.getTimeRange();
                    return NonNullUtils.nullToEmptyString(dp.getBytesWrite(tr.getStartTime().toNanos(), tr.getEndTime().toNanos(), entry.getModel().getId()));
                }
            }
            return super.getColumnText(element, columnIndex);
        }
    }

    @Override
    protected void fillTimeGraphEntryContextMenu(@NonNull IMenuManager menuManager) {
        ISelection selection = getSite().getSelectionProvider().getSelection();
        Object first = null;
        Object second = null;
        if (selection instanceof StructuredSelection) {
            StructuredSelection sSel = (StructuredSelection) selection;
            Iterator<Object> iter = sSel.iterator();
            if (iter.hasNext()) {
                first = iter.next();
            }
            if (iter.hasNext()) {
                second = iter.next();
            }
            if (second instanceof NamedTimeEvent) {
                NamedTimeEvent event = (NamedTimeEvent) second;
                int tid = event.getValue();
                menuManager.add(new FollowThreadAction(FileAccessByFileView.this, event.getLabel(), tid, getTrace((TimeGraphEntry) event.getEntry())));
            }
            if (first instanceof TimeGraphEntry) {
                TimeGraphEntry entry = (TimeGraphEntry) sSel.getFirstElement();
                ITimeGraphEntryModel model = entry.getModel();
                if (model instanceof ThreadEntryModel) {
                    menuManager.add(new FollowThreadAction(FileAccessByFileView.this, entry.getName(), ((ThreadEntryModel) model).getTid(), getTrace(entry)));
                }
            }

        }
    }

    /**
     * Constructor
     */
    public FileAccessByFileView() {
        this(ID, new TimeGraphPresentationProvider() {

            StateItem[] states = {
                    new StateItem(ImmutableMap.of(ITimeEventStyleStrings.label(), "Meta IO", //$NON-NLS-1$
                            ITimeEventStyleStrings.fillStyle(), ITimeEventStyleStrings.solidColorFillStyle(),
                            ITimeEventStyleStrings.fillColor(), new RGBAColor(174, 123, 131, 255).toInt(),
                            ITimeEventStyleStrings.heightFactor(), 1.0f)),
                    new StateItem(ImmutableMap.of(ITimeEventStyleStrings.label(), "IO", //$NON-NLS-1$
                            ITimeEventStyleStrings.fillStyle(), ITimeEventStyleStrings.solidColorFillStyle(),
                            ITimeEventStyleStrings.fillColor(), new RGBAColor(140, 180, 165, 255).toInt(),
                            ITimeEventStyleStrings.heightFactor(), 1.0f))
            };

            @Override
            public StateItem[] getStateTable() {
                return states;
            }

            @Override
            public int getStateTableIndex(ITimeEvent event) {
                if (event instanceof NullTimeEvent) {
                    return -1;
                } else if (event instanceof NamedTimeEvent || (event.getEntry() instanceof TimeGraphEntry && ((TimeGraphEntry)event.getEntry()).getModel() instanceof ThreadEntryModel)) {
                    return 1;
                }
                return 0;
            }

            @Override
            public int getItemHeight(ITimeGraphEntry entry) {
                if (entry instanceof TimeGraphEntry && ((TimeGraphEntry) entry).getModel() instanceof ThreadEntryModel) {
                    return (int) (super.getItemHeight(entry) * .6);
                }
                return super.getItemHeight(entry);
            }

            @Override
            public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event, long hoverTime) {
                Map<String, String> retMap = super.getEventHoverToolTipInfo(event, hoverTime);
                if (retMap == null) {
                    retMap = new LinkedHashMap<>(1);
                }

                if (!(event instanceof TimeEvent) || !((TimeEvent) event).hasValue() ||
                        !(event.getEntry() instanceof TimeGraphEntry)) {
                    return retMap;
                }

                TimeGraphEntry entry = (TimeGraphEntry) event.getEntry();
                ITimeGraphDataProvider<? extends TimeGraphEntryModel> dataProvider = BaseDataProviderTimeGraphView.getProvider(entry);
                TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> response = dataProvider.fetchTooltip(
                        FetchParametersUtils.selectionTimeQueryToMap(new SelectionTimeQueryFilter(hoverTime, hoverTime, 1, Collections.singletonList(entry.getModel().getId()))),
                        null);
                Map<@NonNull String, @NonNull String> map = response.getModel();
                if (map != null) {
                    retMap.putAll(map);
                }

                return retMap;
            }

        }, FileAccessAnalysis.ID + FileAccessDataProvider.SUFFIX);
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        createTimeEventContextMenu();
        Action action = new Action(Messages.FileAccessByFileView_advanced, IAction.AS_CHECK_BOX) {
            @Override
            public String getDescription() {
                return Messages.FileAccessByFileView_advancedDescription;
            }

            @Override
            public ImageDescriptor getImageDescriptor() {
                return ImageDescriptor.createFromImage(ADVANCED_IMAGE);
            }

            @Override
            public void run() {
                fAdvancedMode = !fAdvancedMode;
                setChecked(fAdvancedMode);
                Activator.getDefault().getPreferenceStore().setValue(ADVANCED_MODE_KEY, fAdvancedMode);
                ITmfTrace activeTrace = TmfTraceManager.getInstance().getActiveTrace();
                if (activeTrace != null) {
                    TmfTraceContext ctx = TmfTraceManager.getInstance().getTraceContext(activeTrace);
                    Integer data = (Integer) ctx.getData(ResourcesView.RESOURCES_FOLLOW_CURRENT_THREAD);
                    if (data == null || data < 0) {
                        rebuild();
                    }
                }
            }
        };
        action.setChecked(fAdvancedMode);
        getViewSite().getActionBars().getMenuManager().add(action);
    }

    private void createTimeEventContextMenu() {
        MenuManager eventMenuManager = new MenuManager();
        eventMenuManager.setRemoveAllWhenShown(true);
        TimeGraphControl timeGraphControl = getTimeGraphViewer().getTimeGraphControl();
        final Menu timeEventMenu = eventMenuManager.createContextMenu(timeGraphControl);

        timeGraphControl.addTimeEventMenuListener(event -> {
            Menu menu = timeEventMenu;
            if (event.data instanceof TimeEvent) {
                timeGraphControl.setMenu(menu);
                return;
            }
            timeGraphControl.setMenu(null);
            event.doit = false;
        });

        eventMenuManager.addMenuListener(manager -> {
            fillTimeEventContextMenu(eventMenuManager);
            eventMenuManager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        });
        getSite().registerContextMenu(eventMenuManager, getTimeGraphViewer().getSelectionProvider());
    }

    private void fillTimeEventContextMenu(MenuManager menuManager) {
        ISelection selection = getSite().getSelectionProvider().getSelection();
        Object first = null;
        Object second = null;
        if (selection instanceof StructuredSelection) {
            StructuredSelection sSel = (StructuredSelection) selection;
            Iterator<Object> iter = sSel.iterator();
            if (iter.hasNext()) {
                first = iter.next();
            }
            if (iter.hasNext()) {
                second = iter.next();
            }
            if (second instanceof NamedTimeEvent) {
                NamedTimeEvent event = (NamedTimeEvent) second;
                int tid = event.getValue();
                menuManager.add(new FollowThreadAction(FileAccessByFileView.this, event.getLabel(), tid, getTrace((TimeGraphEntry) event.getEntry())));
            }
            if (first instanceof TimeGraphEntry) {
                TimeGraphEntry entry = (TimeGraphEntry) sSel.getFirstElement();
                ITimeGraphEntryModel model = entry.getModel();
                if (model instanceof ThreadEntryModel) {
                    menuManager.add(new FollowThreadAction(FileAccessByFileView.this, entry.getName(), ((ThreadEntryModel) model).getTid(), getTrace(entry)));
                }
            }

        }

    }

    /**
     * Handle {@link TmfThreadSelectedSignal}
     *
     * @param signal
     *            the signal
     */
    @TmfSignalHandler
    public void handleThreadFollowed(TmfThreadSelectedSignal signal) {
        int threadId = signal.getThreadId();
        int data = threadId >= 0 ? threadId : -1;
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return;
        }
        // TODO: move to a common model somewhere
        TmfTraceManager.getInstance().updateTraceContext(trace,
                builder -> builder.setData(ResourcesView.RESOURCES_FOLLOW_CURRENT_THREAD, data));

        String threadName = threadId < 0 ? fAdvancedMode ? ALL : FOLLOW_A_THREAD : Integer.toString(threadId);
        setPartName(String.format(TITLE, threadName));
        rebuild();
    }

    public FileAccessByFileView(String id, TimeGraphPresentationProvider pres, String providerId) {
        super(id, pres, providerId);
        setTreeColumns(COLUMN_NAMES);
        setTreeLabelProvider(new FileAccessTreeLabelProvider());
    }

    @Override
    protected void buildEntryList(@NonNull ITmfTrace trace, @NonNull ITmfTrace parentTrace, @NonNull IProgressMonitor monitor) {
        TmfTraceContext ctx = TmfTraceManager.getInstance().getTraceContext(trace);
        Integer data = (Integer) ctx.getData(ResourcesView.RESOURCES_FOLLOW_CURRENT_THREAD);
        int tid = data != null ? data.intValue() : -1;
        String threadName = tid < 0 ? fAdvancedMode ? ALL : FOLLOW_A_THREAD : Integer.toString(tid);
        setPartName(String.format(TITLE, threadName));
        if (!fAdvancedMode && tid == -1) {
            return;
        }
        ITimeGraphDataProvider<@NonNull TimeGraphEntryModel> dataProvider = DataProviderManager
                .getInstance().getDataProvider(trace, getProviderId(), ITimeGraphDataProvider.class);
        if (dataProvider == null) {
            return;
        }
        boolean complete = false;
        while (!complete && !monitor.isCanceled()) {
            TimeQueryFilter filter = new TidTimeQueryFilter(0, Long.MAX_VALUE, 2, Collections.emptyList(), getRegexes(), tid == -1 ? Collections.emptyList() : Collections.singleton(tid));
            TmfModelResponse<List<TimeGraphEntryModel>> response = dataProvider.fetchTree(filter, monitor);
            // ------------- BEGIN COPY WITH MINIMAL CHANGE ------------
            if (response.getStatus() == ITmfResponse.Status.FAILED) {
                Activator.getDefault().logError(getClass().getSimpleName() + " Data Provider failed: " + response.getStatusMessage()); //$NON-NLS-1$
                return;
            } else if (response.getStatus() == ITmfResponse.Status.CANCELLED) {
                return;
            }
            complete = response.getStatus() == ITmfResponse.Status.COMPLETED;
            List<TimeGraphEntryModel> model = response.getModel();
            if (model != null) {
                synchronized (fEntries) {
                    for (TimeGraphEntryModel entry : model) {
                        TimeGraphEntry uiEntry = fEntries.get(dataProvider, entry.getId());
                        if (entry.getParentId() != -1) {
                            if (uiEntry == null) {
                                uiEntry = new TimeGraphEntry(entry);
                                TimeGraphEntry parent = fEntries.get(dataProvider, entry.getParentId());
                                if (parent != null) {
                                    parent.addChild(uiEntry);
                                }
                                fEntries.put(dataProvider, entry.getId(), uiEntry);
                            } else {
                                uiEntry.updateModel(entry);
                            }
                        } else {
                            setStartTime(Long.min(getStartTime(), entry.getStartTime()));
                            setEndTime(Long.max(getEndTime(), entry.getEndTime() + 1));

                            if (uiEntry != null) {
                                uiEntry.updateModel(entry);
                            } else {
                                uiEntry = new TraceEntry(entry, trace, dataProvider);
                                fEntries.put(dataProvider, entry.getId(), uiEntry);
                                addToEntryList(parentTrace, Collections.singletonList(uiEntry));
                            }
                        }
                        if (entry instanceof ThreadEntryModel) {

                            TimeGraphEntry toCollapse = uiEntry.getParent();
                            Display.getDefault().asyncExec(
                                    () -> {
                                        getTimeGraphViewer().getTimeGraphControl().setExpandedState(toCollapse, false);
                                    });
                        }
                    }
                }
                long start = getStartTime();
                long end = getEndTime();
                final long resolution = Long.max(1, (end - start) / getDisplayWidth());
                zoomEntries(fEntries.values(), start, end, resolution, monitor);
            }

            if (monitor.isCanceled()) {
                return;
            }

            if (parentTrace.equals(getTrace())) {
                synchingToTime(getTimeGraphViewer().getSelectionBegin());
                refresh();
            }
            monitor.worked(1);

            if (!complete && !monitor.isCanceled()) {
                try {
                    Thread.sleep(BUILD_UPDATE_TIMEOUT);
                } catch (InterruptedException e) {
                    Activator.getDefault().logError("Failed to wait for data provider", e); //$NON-NLS-1$
                }
            }
        }
        // -------------- END COPY WITH MINIMAL CHANGE -------------
    }

}
