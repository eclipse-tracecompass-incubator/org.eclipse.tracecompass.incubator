/*******************************************************************************
 * Copyright (c) 2018, 2020 Ericsson and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.ui.views.fileaccess;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.analysis.os.linux.core.signals.TmfThreadSelectedSignal;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.incubator.internal.kernel.core.fileaccess.FileAccessDataProvider;
import org.eclipse.tracecompass.incubator.internal.kernel.core.fileaccess.FileEntryModel;
import org.eclipse.tracecompass.incubator.internal.kernel.core.fileaccess.ThreadEntryModel;
import org.eclipse.tracecompass.incubator.internal.kernel.ui.Activator;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.actions.FollowThreadAction;
import org.eclipse.tracecompass.internal.provisional.tmf.ui.widgets.timegraph.BaseDataProviderTimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NamedTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphControl;
import org.eclipse.ui.IWorkbenchActionConstants;

import com.google.common.collect.ImmutableMap;

/**
 * File access by file view
 *
 * @author Matthew Khouzam
 */
@SuppressWarnings("restriction")
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
                ITmfTreeDataModel entryModel = entry.getEntryModel();
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
                ITmfTreeDataModel model = entry.getEntryModel();
                if (model instanceof ThreadEntryModel) {
                    return String.valueOf(((ThreadEntryModel) model).getTid());
                }
                return super.getColumnText(element, columnIndex);
            }
            if (columnIndex == 2) {
                FileAccessDataProvider dp = DataProviderManager
                        .getInstance().getOrCreateDataProvider(getTrace(), getProviderId(), FileAccessDataProvider.class);
                ITmfTrace activeTrace = TmfTraceManager.getInstance().getActiveTrace();
                if (activeTrace != null && dp != null) {
                    TmfTimeRange tr = activeTrace.getTimeRange();
                    return NonNullUtils.nullToEmptyString(dp.getBytesRead(tr.getStartTime().toNanos(), tr.getEndTime().toNanos(), entry.getEntryModel().getId()));
                }
            }
            if (columnIndex == 3) {
                FileAccessDataProvider dp = DataProviderManager
                        .getInstance().getOrCreateDataProvider(getTrace(), getProviderId(), FileAccessDataProvider.class);
                ITmfTrace activeTrace = TmfTraceManager.getInstance().getActiveTrace();
                if (activeTrace != null && dp != null) {
                    TmfTimeRange tr = activeTrace.getTimeRange();
                    return NonNullUtils.nullToEmptyString(dp.getBytesWrite(tr.getStartTime().toNanos(), tr.getEndTime().toNanos(), entry.getEntryModel().getId()));
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
                ITmfTreeDataModel model = entry.getEntryModel();
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
        super(ID, new BaseDataProviderTimeGraphPresentationProvider(), FileAccessDataProvider.ID);
        setTreeColumns(COLUMN_NAMES);
        setTreeLabelProvider(new FileAccessTreeLabelProvider());
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
                    HostThread data = (HostThread) ctx.getData(HostThread.SELECTED_HOST_THREAD_KEY);
                    if (data == null || data.getTid() < 0) {
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
                ITmfTreeDataModel model = entry.getEntryModel();
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
        HostThread ht = signal.getThreadId() >= 0 ? signal.getHostThread() : null;
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return;
        }

        // TODO: move to a common model somewhere

        TmfTraceManager.getInstance().updateTraceContext(trace,
                builder -> builder.setData(HostThread.SELECTED_HOST_THREAD_KEY, ht));

        if (ht == null) {
            return;
        }
        String threadName = ht.getTid() < 0 ? fAdvancedMode ? ALL : FOLLOW_A_THREAD : Integer.toString(ht.getTid());
        setPartName(String.format(TITLE, threadName));
        rebuild();
    }

    @Override
    protected @NonNull Map<@NonNull String, @NonNull Object> getFetchTreeParameters() {
        TmfTraceContext ctx = TmfTraceManager.getInstance().getTraceContext(getTrace());
        HostThread data = (HostThread) ctx.getData(HostThread.SELECTED_HOST_THREAD_KEY);
        int tid = data != null ? data.getTid() : -1;
        String threadName = tid < 0 ? fAdvancedMode ? ALL : FOLLOW_A_THREAD : Integer.toString(tid);
        setPartName(String.format(TITLE, threadName));
        if (!fAdvancedMode && tid == -1) {
            return Collections.emptyMap();
        }
        return ImmutableMap.of(FileAccessDataProvider.TID_PARAM, tid);
    }

}
