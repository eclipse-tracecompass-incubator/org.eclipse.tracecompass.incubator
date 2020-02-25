/**********************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.ui.actions;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.AbstractRosStateProvider;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.messageflow.TargetMessageInfo;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.messageflow.TargetMessageInfo.RosQueueType;
import org.eclipse.tracecompass.incubator.internal.ros.core.signals.RosMessageSelectedSignal;
import org.eclipse.tracecompass.incubator.internal.ros.ui.Activator;
import org.eclipse.tracecompass.incubator.internal.ros.ui.views.queues.RosQueuesView;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphViewer;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;

/**
 * Follow message action. Fetches and validates the selected message and
 * broadcasts a {@link RosMessageSelectedSignal} if valid
 *
 * @author Christophe Bedard
 */
public class RosFollowMessageAction extends Action {

    private static final @NonNull String IMG_UI = "icons/elcl16/follow_message.gif"; //$NON-NLS-1$

    /** The corresponding view */
    private final TmfView fView;
    /** The time graph viewer */
    private final TimeGraphViewer fTimeGraphViewer;

    /**
     * Constructor
     *
     * @param source
     *            the source view
     * @param timeGraphViewer
     *            the timegraph viewer
     */
    public RosFollowMessageAction(TmfView source, TimeGraphViewer timeGraphViewer) {
        fTimeGraphViewer = timeGraphViewer;
        fView = source;

        setText(Messages.FollowMessageAction_NameText);
        setToolTipText(Messages.FollowMessageAction_ToolTipText);
        setImageDescriptor(Objects.requireNonNull(Activator.getDefault()).getImageDescripterFromPath(IMG_UI));
    }

    @Override
    public void run() {
        long selectionBegin = fTimeGraphViewer.getSelectionBegin();
        ITimeGraphEntry selection = fTimeGraphViewer.getSelection();
        if (selection == null) {
            return;
        }

        ITimeEvent msgRefEvent = RosQueuesView.getMessageReferenceEvent(selection, selectionBegin);
        String msgRef = msgRefEvent.getLabel();
        if (msgRef != null) {
            RosQueueType queueType = getQueueTypeFromName(selection.getParent().getParent().getParent().getName());
            if (queueType == null) {
                System.err.println("Invalid queue type!"); //$NON-NLS-1$
                return;
            }

            String node = selection.getParent().getParent().getParent().getParent().getName();
            String topic = selection.getParent().getParent().getName();
            int position = Integer.parseInt(selection.getName());
            long msgBegin = msgRefEvent.getTime();
            TargetMessageInfo target = new TargetMessageInfo(node, topic, queueType, position, msgRef, selectionBegin, msgBegin);
            fView.broadcast(new RosMessageSelectedSignal(fView, target));
            super.run();
        }
    }

    private static @Nullable RosQueueType getQueueTypeFromName(String queueTypeName) {
        if (queueTypeName.equals(AbstractRosStateProvider.PUBLISHERS_LIST)) {
            return RosQueueType.PUB;
        } else if (queueTypeName.equals(AbstractRosStateProvider.SUBSCRIBERS_LIST)) {
            return RosQueueType.SUB;
        } else {
            return null;
        }
    }
}
