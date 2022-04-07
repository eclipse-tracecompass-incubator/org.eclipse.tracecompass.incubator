/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros2.ui.views.messages;

import java.util.Iterator;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.Ros2ObjectTimeGraphEntryModel;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages.Ros2MessagesDataProvider;
import org.eclipse.tracecompass.incubator.internal.ros2.ui.Activator;
import org.eclipse.tracecompass.incubator.internal.ros2.ui.actions.Ros2FollowMessageAction;
import org.eclipse.tracecompass.incubator.internal.ros2.ui.views.AbstractRos2DataProviderTimeGraphView;
import org.eclipse.tracecompass.incubator.internal.ros2.ui.views.Ros2HideInternalObjectsAction;
import org.eclipse.tracecompass.incubator.internal.ros2.ui.views.Ros2ObjectTreeLabelProvider;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * View for ROS 2 Messages.
 *
 * @author Christophe Bedard
 */
public class Ros2MessagesView extends AbstractRos2DataProviderTimeGraphView {

    private static final String ID_SUFFIX = ".messages"; //$NON-NLS-1$

    private Ros2FollowMessageAction fFollowAction;

    private class Ros2MessagesViewTreeLabelProvider extends TreeLabelProvider {
        @Override
        public String getColumnText(Object element, int columnIndex) {
            return Ros2ObjectTreeLabelProvider.getColumnText(element, columnIndex);
        }
    }

    /**
     * Constructor
     */
    public Ros2MessagesView() {
        super(getFullViewId(), new Ros2MessagesPresentationProvider(), Ros2MessagesDataProvider.getFullDataProviderId());
        setTreeColumns(Ros2ObjectTreeLabelProvider.TREE_COLUMNS);
        setTreeLabelProvider(new Ros2MessagesViewTreeLabelProvider());
        setFilterColumns(Ros2ObjectTreeLabelProvider.TREE_COLUMNS);
        setFilterLabelProvider(new Ros2MessagesViewTreeLabelProvider());
    }

    /**
     * @return the full ID of this view
     */
    public static @NonNull String getFullViewId() {
        return AbstractRos2DataProviderTimeGraphView.getViewIdFromSuffix(ID_SUFFIX);
    }

    @Override
    protected void fillLocalToolBar(IToolBarManager manager) {
        IDialogSettings settings = Objects.requireNonNull(Activator.getDefault()).getDialogSettings();
        IDialogSettings section = settings.getSection(getClass().getName());
        if (section == null) {
            section = settings.addNewSection(getClass().getName());
        }

        // Message flow action
        fFollowAction = new Ros2FollowMessageAction(this, getTimeGraphViewer());
        fFollowAction.setEnabled(false);
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, fFollowAction);

        // Hide arrows button
        IAction hideArrowsAction = getTimeGraphViewer().getHideArrowsAction(section);
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, hideArrowsAction);

        // Hide internal objects
        IAction hideInternalAction = new Ros2HideInternalObjectsAction(getTimeGraphViewer());
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, hideInternalAction);

        // Add a separator to local tool bar
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());

        super.fillLocalToolBar(manager);
    }

    /**
     * {@link TmfSignalHandler} for when the selection is updated
     *
     * @param signal
     *            the selection range updated signal
     */
    @TmfSignalHandler
    public synchronized void selection(TmfSelectionRangeUpdatedSignal signal) {
        updateFollowMessageAction(signal.getSource());
    }

    private void updateFollowMessageAction(Object source) {
        if (null != fFollowAction) {
            boolean isValid = false;
            ITimeGraphEntry selection = getTimeGraphViewer().getSelection();
            // FIXME an event is still selected in the view even if the focus is
            // on another view, so it could still be considered valid
            if (selection != null && source == this) {
                @Nullable
                ITimeEvent event = getReferenceEvent(selection, getTimeGraphViewer().getSelectionBegin());
                isValid = (event != null && event.getLabel() != null);
            }
            fFollowAction.setEnabled(isValid);
        }
    }

    /**
     * Get the event corresponding to an entry and timestamp.
     *
     * @param selectedEntry
     *            the selected entry
     * @param timestamp
     *            the selected timestamp
     * @return the reference event if found and valid, or <code>null</null>
     *         otherwise
     */
    public static @Nullable ITimeEvent getReferenceEvent(ITimeGraphEntry selectedEntry, long timestamp) {
        // Make sure the selected timegraph entry is valid (pub/sub/timer)
        if (!(selectedEntry instanceof TimeGraphEntry)) {
            return null;
        }
        ITmfTreeDataModel model = ((TimeGraphEntry) selectedEntry).getEntryModel();
        if (!(model instanceof Ros2ObjectTimeGraphEntryModel)) {
            return null;
        }
        Ros2ObjectTimeGraphEntryModel messagesModel = (Ros2ObjectTimeGraphEntryModel) model;
        if (!messagesModel.isLeafObject()) {
            return null;
        }

        // Find the corresponding time event by comparing timestamps
        ITimeEvent refEvent = null;
        Iterator<@NonNull ? extends ITimeEvent> timeEventsIt = selectedEntry.getTimeEventsIterator();
        boolean searchDone = false;
        while (timeEventsIt.hasNext() && !searchDone) {
            ITimeEvent event = timeEventsIt.next();
            long duration = event.getDuration();
            long time = event.getTime();
            if (duration > 0 && (time <= timestamp && timestamp < (time + duration))) {
                refEvent = event;
                searchDone = true;
            } else if (time > timestamp) {
                searchDone = true;
            }
        }
        return refEvent;
    }
}
