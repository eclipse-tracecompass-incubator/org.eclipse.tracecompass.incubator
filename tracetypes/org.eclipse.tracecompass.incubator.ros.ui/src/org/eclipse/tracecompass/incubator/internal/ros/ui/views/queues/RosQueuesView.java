/**********************************************************************
 * Copyright (c) 2018 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.ui.views.queues;

import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.AbstractRosStateProvider;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.queues.RosQueuesDataProvider;
import org.eclipse.tracecompass.incubator.internal.ros.ui.actions.RosFollowMessageAction;
import org.eclipse.tracecompass.incubator.internal.ros.ui.views.AbstractRosView;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * View for ROS Queues
 *
 * @author Christophe Bedard
 */
public class RosQueuesView extends AbstractRosView {

    /** View ID suffix */
    public static final String ID_SUFFIX = "queues"; //$NON-NLS-1$

    private RosFollowMessageAction fFollowMessageAction = null;

    /**
     * Constructor
     */
    public RosQueuesView() {
        super(ID_SUFFIX, new RosQueuesPresentationProvider(), RosQueuesDataProvider.getFullDataProviderId());
        // Enable view filter dialog
        setFilterColumns(new String[] { StringUtils.EMPTY });
        setFilterLabelProvider(new RosViewFilterLabelProvider());
    }

    private static class RosViewFilterLabelProvider extends TreeLabelProvider {
        // Empty; only used to enable view filter dialog
    }

    @Override
    protected void fillLocalToolBar(IToolBarManager manager) {
        fFollowMessageAction = new RosFollowMessageAction(this, getTimeGraphViewer());
        fFollowMessageAction.setEnabled(false);
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, fFollowMessageAction);

        // add a separator to local tool bar
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
        if (fFollowMessageAction != null) {
            boolean isValid = false;
            ITimeGraphEntry selection = getTimeGraphViewer().getSelection();
            // FIXME an event is still selected in the view even if the focus is
            // on another view, so it could still be considered valid
            if (selection != null && source == this) {
                ITimeEvent event = getMessageReferenceEvent(selection, getTimeGraphViewer().getSelectionBegin());
                isValid = (event != null && event.getLabel() != null);
            }
            fFollowMessageAction.setEnabled(isValid);
        }
    }

    /**
     * Get the message event corresponding to an entry and timestamp
     *
     * @param selectedEntry
     *            the selected entry
     * @param timestamp
     *            the selected timestamp
     * @return the message event if found and valid, {@code null} otherwise
     */
    public static ITimeEvent getMessageReferenceEvent(ITimeGraphEntry selectedEntry, long timestamp) {
        // It has to be a child of a queue entry
        ITimeGraphEntry parent = selectedEntry.getParent();
        if (parent == null || !parent.getName().equals(AbstractRosStateProvider.QUEUE)) {
            return null;
        }

        // Find the corresponding message by comparing timestamps
        ITimeEvent refEvent = null;
        Iterator<@NonNull ? extends ITimeEvent> timeEventsIt = selectedEntry.getTimeEventsIterator();
        while (timeEventsIt.hasNext()) {
            ITimeEvent event = timeEventsIt.next();
            long duration = event.getDuration();
            long time = event.getTime();
            if (duration > 0 && (time <= timestamp && timestamp < (time + duration))) {
                refEvent = event;
                break;
            } else if (time > timestamp) {
                break;
            }
        }
        return refEvent;
    }

    /**
     * @return the full view ID for this view
     */
    public static String getFullViewId() {
        return AbstractRosView.ID_PREFIX + ID_SUFFIX;
    }
}
