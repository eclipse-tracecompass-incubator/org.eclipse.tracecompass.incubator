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

package org.eclipse.tracecompass.incubator.internal.ros2.ui.actions;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.Ros2ObjectTimeGraphEntryModelType;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.Ros2ObjectTimeGraphEntryModel;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages.Ros2CallbackTimeGraphState;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages.Ros2PubTimeGraphState;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages.Ros2TakeTimeGraphState;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messageflow.Ros2FlowTargetInfo;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messageflow.Ros2FlowTargetType;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2CallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2PubInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.signals.Ros2FlowItemSelectedSignal;
import org.eclipse.tracecompass.incubator.internal.ros2.ui.Activator;
import org.eclipse.tracecompass.incubator.internal.ros2.ui.views.messages.Ros2MessagesView;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphViewer;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

import com.google.common.collect.Multimap;

/**
 * Follow message action. Fetches and validates the selected message and
 * broadcasts a {@link Ros2FlowItemSelectedSignal} if valid.
 *
 * @author Christophe Bedard
 */
public class Ros2FollowMessageAction extends Action {

    private static final @NonNull String IMG_UI = "icons/elcl16/follow_message.gif"; //$NON-NLS-1$
    private static final @NonNull String ACTION_TEXT = "Follow"; //$NON-NLS-1$
    private static final @NonNull String ACTION_TOOLTIP_TEXT = "Follow this element"; //$NON-NLS-1$

    private final TmfView fView;
    private final TimeGraphViewer fTimeGraphViewer;

    /**
     * Constructor
     *
     * @param source
     *            the source view
     * @param timeGraphViewer
     *            the timegraph viewer
     */
    public Ros2FollowMessageAction(TmfView source, TimeGraphViewer timeGraphViewer) {
        fTimeGraphViewer = timeGraphViewer;
        fView = source;

        setText(ACTION_TEXT);
        setToolTipText(ACTION_TOOLTIP_TEXT);
        setImageDescriptor(Objects.requireNonNull(Activator.getDefault()).getImageDescripterFromPath(IMG_UI));
    }

    @Override
    public void run() {
        long selectionBegin = fTimeGraphViewer.getSelectionBegin();
        ITimeGraphEntry selection = fTimeGraphViewer.getSelection();
        if (null == selection) {
            return;
        }

        @Nullable
        ITimeEvent refEvent = Ros2MessagesView.getReferenceEvent(selection, selectionBegin);
        if (null != refEvent) {

            // Get target event/instance
            ITmfTreeDataModel model = ((TimeGraphEntry) refEvent.getEntry()).getEntryModel();
            if (!(model instanceof Ros2ObjectTimeGraphEntryModel)) {
                return;
            }
            Ros2ObjectTimeGraphEntryModel messagesModel = (Ros2ObjectTimeGraphEntryModel) model;
            Multimap<@NonNull String, @NonNull Object> metadata = refEvent.getMetadata();
            Ros2ObjectTimeGraphEntryModelType type = messagesModel.getType();
            Ros2FlowTargetInfo info;
            long startTime = refEvent.getTime();
            long endTime = startTime + refEvent.getDuration() - 1;
            if (Ros2ObjectTimeGraphEntryModelType.PUBLISHER == type) {
                Ros2PubInstance pub = (Ros2PubInstance) metadata.get(Ros2PubTimeGraphState.KEY_DATA).iterator().next();
                info = new Ros2FlowTargetInfo(startTime, endTime, Ros2FlowTargetType.PUBLISHER, pub);
            } else if (Ros2ObjectTimeGraphEntryModelType.SUBSCRIPTION == type) {
                // Ros2TakeInstance or Ros2CallbackInstance
                Object takeOrCallback = metadata.get(Ros2TakeTimeGraphState.KEY_DATA).iterator().next();
                info = new Ros2FlowTargetInfo(startTime, endTime, Ros2FlowTargetType.SUBSCRIPTION, takeOrCallback);
            } else if (Ros2ObjectTimeGraphEntryModelType.TIMER == type) {
                Ros2CallbackInstance callback = (Ros2CallbackInstance) metadata.get(Ros2CallbackTimeGraphState.KEY_DATA).iterator().next();
                info = new Ros2FlowTargetInfo(startTime, endTime, Ros2FlowTargetType.TIMER, callback);
            } else {
                return;
            }

            // Broadcast signal for Ros2MessageFlowParameterProvider
            fView.broadcast(new Ros2FlowItemSelectedSignal(fView, info));
            super.run();
        }
    }
}
