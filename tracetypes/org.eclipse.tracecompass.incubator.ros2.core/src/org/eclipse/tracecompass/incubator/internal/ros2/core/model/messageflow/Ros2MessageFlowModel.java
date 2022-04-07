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

package org.eclipse.tracecompass.incubator.internal.ros2.core.model.messageflow;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros2.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.IRos2Model;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages.Ros2MessagesUtil;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.objects.Ros2ObjectsUtil;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messagelinks.Ros2MessageCausalLinksModel;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2CallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2PubInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2SubCallbackInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages.Ros2TakeInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2PublisherObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2SubscriptionObject;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2TimerObject;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;

/**
 * Container and interface that represents the model for ROS 2 message flow.
 *
 * @author Christophe Bedard
 */
public class Ros2MessageFlowModel implements IRos2Model {

    // Information about the initial target
    private final @NonNull Ros2FlowTargetInfo fInfo;
    private final @NonNull Ros2MessageFlowBuildInfo fBuildInfo;
    private final @NonNull Ros2MessageFlowBuilder fBuilder;

    private Ros2MessageFlowSegment fInitialSegment = null;
    private boolean fIsModelDone = false;

    private final @NonNull HashMap<@NonNull Ros2MessageFlowSegment, @NonNull Ros2MessageFlowSegment> fSegments = new HashMap<>();

    /**
     * Constructor
     *
     * @param info
     *            the target info
     * @param objectsSs
     *            the objects state system
     * @param messagesSs
     *            the messages state system
     * @param messageLinksModel
     *            the message links model
     */
    public Ros2MessageFlowModel(@NonNull Ros2FlowTargetInfo info, @NonNull ITmfStateSystem objectsSs, @NonNull ITmfStateSystem messagesSs, @NonNull Ros2MessageCausalLinksModel messageLinksModel) {
        fInfo = info;
        fBuildInfo = new Ros2MessageFlowBuildInfo(objectsSs, messagesSs, messageLinksModel);
        fBuilder = new Ros2MessageFlowBuilder(fBuildInfo, fSegments);
    }

    /**
     * @return whether the model is done being built
     */
    public boolean isDone() {
        return fIsModelDone;
    }

    /**
     * @return the initial segment
     */
    public @Nullable Ros2MessageFlowSegment getInitialSegment() {
        return fInitialSegment;
    }

    /**
     * Generate
     */
    public void generateModel() {
        fIsModelDone = false;
        generate();
        fIsModelDone = true;
    }

    private void generate() {
        // Create first segment from initial target info
        fInitialSegment = getSegmentFromTargetInfo(fInfo);

        if (null != fInitialSegment) {
            buildFlow(fInitialSegment);
        }
    }

    private @Nullable Ros2MessageFlowSegment getSegmentFromTargetInfo(Ros2FlowTargetInfo targetInfo) {
        Ros2MessageFlowSegment segment = null;
        switch (targetInfo.getTargetType()) {
        case PUBLISHER:
            Ros2PubInstance pubInstance = (Ros2PubInstance) targetInfo.getTargetEvent();
            Ros2PublisherObject publisherObject = Ros2ObjectsUtil.getPublisherObjectFromHandle(fBuildInfo.getObjectsSs(), targetInfo.getStartTime(), pubInstance.getPublisherHandle());
            if (null == publisherObject) {
                break;
            }
            segment = new Ros2PublicationMessageFlowSegment(targetInfo.getStartTime(), targetInfo.getEndTime(), pubInstance, publisherObject);
            break;
        case SUBSCRIPTION:
            // Ros2TakeInstance or Ros2CallbackInstance
            Ros2ObjectHandle subHandle;
            long timestamp;
            if (targetInfo.getTargetEvent() instanceof Ros2TakeInstance) {
                Ros2TakeInstance takeInstance = (Ros2TakeInstance) targetInfo.getTargetEvent();
                subHandle = takeInstance.getSubscriptionHandle();
                timestamp = takeInstance.getStartTime();
            } else if (targetInfo.getTargetEvent() instanceof Ros2CallbackInstance) {
                Ros2CallbackInstance callbackInstance = (Ros2CallbackInstance) targetInfo.getTargetEvent();
                subHandle = callbackInstance.getOwnerHandle();
                timestamp = callbackInstance.getStartTime();
            } else {
                throw new IllegalStateException();
            }
            Ros2SubscriptionObject subObject = Ros2ObjectsUtil.getSubscriptionObjectFromHandle(fBuildInfo.getObjectsSs(), targetInfo.getStartTime(), subHandle);
            ITmfStateInterval subCallbackInstanceInterval = Ros2MessagesUtil.getSubCallbackInstanceInterval(fBuildInfo.getMessagesSs(), fBuildInfo.getObjectsSs(), timestamp, subHandle);
            if (null == subObject || null == subCallbackInstanceInterval) {
                break;
            }
            Ros2SubCallbackInstance subCallbackInstance = (Ros2SubCallbackInstance) subCallbackInstanceInterval.getValue();
            if (null == subCallbackInstance) {
                break;
            }
            segment = new Ros2SubCallbackMessageFlowSegment(targetInfo.getStartTime(), targetInfo.getEndTime(), subCallbackInstance, subObject);
            break;
        case TIMER:
            Ros2CallbackInstance callbackInstance = (Ros2CallbackInstance) targetInfo.getTargetEvent();
            Ros2TimerObject timerObject = Ros2ObjectsUtil.getTimerObjectFromHandle(fBuildInfo.getObjectsSs(), targetInfo.getStartTime(), callbackInstance.getOwnerHandle());
            if (null == timerObject) {
                break;
            }
            segment = new Ros2TimerCallbackMessageFlowSegment(targetInfo.getStartTime(), targetInfo.getEndTime(), callbackInstance, timerObject);
            break;
        default:
            throw new IllegalStateException();
        }
        return segment;
    }

    private void buildFlow(@NonNull Ros2MessageFlowSegment initialSegment) {
        fBuilder.getNextSegments(initialSegment);
        fBuilder.getPreviousSegments(initialSegment);
    }

    /**
     * Print message flow in both directions, starting from the given segment.
     *
     * @param segment
     *            the initial segment
     */
    public static void printFlow(@NonNull Ros2MessageFlowSegment segment) {
        printFlow(segment, 0, true, false);
        printFlow(segment, 0, false, true);
    }

    private static void printFlow(@NonNull Ros2MessageFlowSegment segment, int depth, boolean forward, boolean skip) {
        if (!skip) {
            Activator.getInstance().logInfo(StringUtils.repeat(StringUtils.SPACE, depth * 2) + segment.toString());
        }
        for (@NonNull
        Ros2MessageFlowSegment next : (forward ? segment.getNext() : segment.getPrevious())) {
            printFlow(next, depth + 1, forward, false);
        }
    }
}
