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

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.messageflow;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.AbstractRosStateProvider;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.messageflow.TargetMessageInfo;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.messageflow.TargetMessageInfo.RosQueueType;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.connections.RosConnectionEndpoint;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.messageflow.RosMessageFlowSegment.SegmentType;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.messagestransport.IRosMessagesTransportModel;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.messagestransport.RosMessageTransport;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;

/**
 * {@link IRosMessageFlowModel} implementation
 *
 * @author Christophe Bedard
 */
public class RosMessageFlowModel implements IRosMessageFlowModel {

    private final @NonNull TargetMessageInfo fInfo;
    private final @NonNull ITmfStateSystem fQueuesSs;
    private final @NonNull IRosMessagesTransportModel fMsgsTransportModel;

    private RosMessageFlowSegment fInitialSegment = null;
    private boolean fIsModelDone = false;

    /**
     * Constructor
     *
     * @param targetInfo
     *            the target message info
     * @param queuesStateSystem
     *            the queues state system
     * @param msgsTransportModel
     *            the messages transport model
     */
    public RosMessageFlowModel(@NonNull TargetMessageInfo targetInfo, @NonNull ITmfStateSystem queuesStateSystem, @NonNull IRosMessagesTransportModel msgsTransportModel) {
        fInfo = targetInfo;
        fQueuesSs = queuesStateSystem;
        fMsgsTransportModel = msgsTransportModel;
    }

    @Override
    public @Nullable RosMessageFlowSegment getFirstSegment() {
        return fInitialSegment;
    }

    @Override
    public boolean isModelDone() {
        return fIsModelDone;
    }

    /**
     * Contains necessary basic information to get/process the next segment.
     * Serves as a mutable container.
     */
    private class NextSegmentInfo {

        /** Type of the next segment */
        private SegmentType fType = null;
        /** Relevant quark for the next segment */
        private Integer fQuark = null;
        /** Relevant timestamp for the next segment */
        private Long fTimestamp = null;
        /** The next segment that was created */
        private RosMessageFlowSegment fSegment = null;
        /** The previous segment that was created */
        private RosMessageFlowSegment fPreviousSegment = null;

        public NextSegmentInfo() {
        }

        public SegmentType getType() {
            return fType;
        }

        public void setType(SegmentType type) {
            fType = type;
        }

        public Integer getQuark() {
            return fQuark;
        }

        public void setQuark(int quark) {
            fQuark = quark;
        }

        public Long getTimestamp() {
            return fTimestamp;
        }

        public void setTimestamp(long t) {
            fTimestamp = t;
        }

        public RosMessageFlowSegment getNextSegment() {
            return fSegment;
        }

        public void setNextSegment(RosMessageFlowSegment segment) {
            fPreviousSegment = fSegment;
            fSegment = segment;
        }

        public RosMessageFlowSegment getPreviousSegment() {
            return fPreviousSegment;
        }
    }

    @Override
    public void generateModel() {
        try {
            process();
            fIsModelDone = true;
        } catch (AttributeNotFoundException | StateSystemDisposedException e) {
            Activator.getInstance().logError("Error while processing! ", e); //$NON-NLS-1$
        }
    }

    private void process() throws AttributeNotFoundException, StateSystemDisposedException {
        // Find type of initial segment corresponding to target message
        SegmentType type = getSegmentTypeFromQueueType(fInfo.getQueueType());
        // Find other necessary info about first segment
        int queueQuark = getTargetQueueQuark();
        int queuePos = fInfo.getQueuePosition();
        int queuePosQuark = fQueuesSs.getQuarkRelative(queueQuark, String.valueOf(queuePos));
        NextSegmentInfo next = new NextSegmentInfo();
        next.setType(type);
        next.setQuark(queuePosQuark);
        next.setTimestamp(fInfo.getMsgTimestamp());

        while (next.getType() != SegmentType.INVALID) {
            // process
            switch (next.getType()) {
            case PUB_QUEUE:
                processPubQueue(next);
                break;
            case SUB_QUEUE:
                processSubQueue(next);
                break;
            case SUB_CALLBACK:
                processCallback(next);
                break;
            case INVALID:
            default:
                Activator.getInstance().logError("Case " + next.getType().name() + " should be handled!"); //$NON-NLS-1$ //$NON-NLS-2$
                break;
            }

            // Link previous to current
            RosMessageFlowSegment nextSegment = next.getNextSegment();
            RosMessageFlowSegment previousSegment = next.getPreviousSegment();
            if (previousSegment == null) {
                fInitialSegment = nextSegment;
            } else {
                previousSegment.addNext(nextSegment);
            }
        }
    }

    private void processPubQueue(NextSegmentInfo next) throws StateSystemDisposedException, AttributeNotFoundException {
        int queuePosQuark = next.getQuark();
        long msgStart = next.getTimestamp();

        ITmfStateInterval firstState = fQueuesSs.querySingleState(msgStart, queuePosQuark);
        ITmfStateInterval lastState = getLastStateOfMessageInQueue(firstState, queuePosQuark);
        int queueQuark = fQueuesSs.getParentAttributeQuark(queuePosQuark);

        long start = firstState.getStartTime();
        long end = lastState.getEndTime();
        int topicQuark = fQueuesSs.getParentAttributeQuark(fQueuesSs.getParentAttributeQuark(queuePosQuark));
        int nodeQuark = fQueuesSs.getParentAttributeQuark(fQueuesSs.getParentAttributeQuark(topicQuark));
        String nodeName = fQueuesSs.getAttributeName(nodeQuark);
        String topicName = fQueuesSs.getAttributeName(topicQuark);
        RosMessageFlowSegment segment = new RosMessageFlowSegment(start, end, next.getType(), nodeName, topicName);
        next.setNextSegment(segment);

        QueueSegmentTransition transition = getTransitionFromLastQueueState(lastState, fInfo.getQueueType(), queueQuark);
        if (transition == null) {
            Activator.getInstance().logError("Could not find transition!"); //$NON-NLS-1$
        }

        if (transition == null || transition == QueueSegmentTransition.DROP) {
            next.setType(SegmentType.INVALID);
        } else {
            next.setType(SegmentType.SUB_QUEUE);
            // Find first state after network
            RosMessageTransport transport = fMsgsTransportModel.getNextMessageTransport(end, nodeName, topicName);
            if (transport == null) {
                Activator.getInstance().logError("Could not find next message transport!"); //$NON-NLS-1$
                next.setType(SegmentType.INVALID);
            } else {
                // Find the subscriber queue from connection
                RosConnectionEndpoint sub = transport.getConnection().getSub();
                queueQuark = fQueuesSs.getQuarkAbsolute(sub.getNodeName(), AbstractRosStateProvider.SUBSCRIBERS_LIST, sub.getTopicName(), AbstractRosStateProvider.QUEUE);

                ITmfStateInterval stateBeforeMsg = getStateBeforeMessageAddedToQueue(queueQuark, transport.getDestinationTimestamp());
                int incomingMsgPosQuark = stateBeforeMsg.getAttribute();
                long msgStateStartTime = stateBeforeMsg.getEndTime() + 1;

                next.setQuark(incomingMsgPosQuark);
                next.setTimestamp(msgStateStartTime);
            }
        }
    }

    private void processSubQueue(NextSegmentInfo next) throws AttributeNotFoundException, StateSystemDisposedException {
        int incomingMsgPosQuark = next.getQuark();
        long msgStateStartTime = next.getTimestamp();
        ITmfStateInterval firstState = fQueuesSs.querySingleState(msgStateStartTime, incomingMsgPosQuark);
        ITmfStateInterval lastState = getLastStateOfMessageInQueue(firstState, incomingMsgPosQuark);

        long start = firstState.getStartTime();
        long end = lastState.getEndTime();
        int topicQuark = fQueuesSs.getParentAttributeQuark(fQueuesSs.getParentAttributeQuark(incomingMsgPosQuark));
        int nodeQuark = fQueuesSs.getParentAttributeQuark(fQueuesSs.getParentAttributeQuark(topicQuark));
        String nodeName = fQueuesSs.getAttributeName(nodeQuark);
        String topicName = fQueuesSs.getAttributeName(topicQuark);
        RosMessageFlowSegment segment = new RosMessageFlowSegment(start, end, next.getType(), nodeName, topicName);
        next.setNextSegment(segment);

        QueueSegmentTransition transition = getTransitionFromLastQueueState(lastState, RosQueueType.SUB, fQueuesSs.getParentAttributeQuark(incomingMsgPosQuark));
        if (transition == null) {
            Activator.getInstance().logError("Could not find transition!"); //$NON-NLS-1$
        }

        if (transition == null || transition == QueueSegmentTransition.DROP) {
            next.setType(SegmentType.INVALID);
        } else {
            next.setType(SegmentType.SUB_CALLBACK);

            // Callback state info
            long callbackStart = lastState.getEndTime() + 1;
            // Callbacks are not processed per-topic, but per-node/globally
            int subsQuark = fQueuesSs.getParentAttributeQuark(topicQuark);
            int callbackQuark = fQueuesSs.getQuarkRelative(subsQuark, AbstractRosStateProvider.CALLBACKS);

            next.setQuark(callbackQuark);
            next.setTimestamp(callbackStart);
        }
    }

    private void processCallback(NextSegmentInfo next) throws StateSystemDisposedException {
        long callbackStart = next.getTimestamp();
        int callbackQuark = next.getQuark();
        ITmfStateInterval callbackState = fQueuesSs.querySingleState(callbackStart, callbackQuark);
        int nodeQuark = fQueuesSs.getParentAttributeQuark(fQueuesSs.getParentAttributeQuark(callbackQuark));

        // Find messages added to the pub queues during the callback
        List<ITmfStateInterval> pubMsgs = new ArrayList<>();
        try {
            int pubListQuark = fQueuesSs.getQuarkRelative(nodeQuark, AbstractRosStateProvider.PUBLISHERS_LIST);
            List<@NonNull Integer> pubTopicQuarks = fQueuesSs.getSubAttributes(pubListQuark, false);
            for (Integer pubTopicQuark : pubTopicQuarks) {
                String topicName = fQueuesSs.getAttributeName(pubTopicQuark);
                // Rejecting rosout for now
                // TODO put back in and support
                if (!topicName.contains("rosout")) { //$NON-NLS-1$
                    int queueQuark = fQueuesSs.getQuarkRelative(pubTopicQuark, AbstractRosStateProvider.QUEUE);
                    ITmfStateInterval stateBefore = getStateBeforeMessageAddedToQueue(queueQuark, callbackState.getStartTime() - 1);
                    // If there is a new state within the start/end of the
                    // callback
                    if (stateBefore.getEndTime() < callbackState.getEndTime()) {
                        long newMsgsStart = stateBefore.getEndTime() + 1;
                        ITmfStateInterval newPubMsg = fQueuesSs.querySingleState(newMsgsStart, stateBefore.getAttribute());
                        pubMsgs.add(newPubMsg);
                    }
                }
            }
        } catch (AttributeNotFoundException e) {
            // Do nothing, assume it means there are no more messages to be
            // found
        }

        long start = callbackState.getStartTime();
        long end;
        String nodeName = fQueuesSs.getAttributeName(nodeQuark);
        // No specific topic associated with a callback
        String topicName = StringUtils.EMPTY;

        if (pubMsgs.isEmpty()) {
            // No messages found
            // Still add a segment for time spent in callback
            end = callbackState.getEndTime();
            RosMessageFlowSegment segment = new RosMessageFlowSegment(start, end, next.getType(), nodeName, topicName);
            next.setNextSegment(segment);
            next.setType(SegmentType.INVALID);
        } else {
            // We do not support non-linear flow graphs, so just warn for now
            // and take the first message
            // TODO support more than one published message
            if (pubMsgs.size() > 1) {
                Activator.getInstance().logWarning("Found more than one published message during callback!"); //$NON-NLS-1$
            }

            ITmfStateInterval pubMsg = pubMsgs.get(0);
            end = pubMsg.getStartTime() - 1;
            RosMessageFlowSegment segment = new RosMessageFlowSegment(start, end, next.getType(), nodeName, topicName);
            next.setNextSegment(segment);

            next.setQuark(pubMsg.getAttribute());
            next.setTimestamp(pubMsg.getStartTime());
            next.setType(SegmentType.PUB_QUEUE);
        }
    }

    /**
     * Get the state before a new message is (possibly) added to a queue after a
     * specific timestamp
     *
     * @param queueQuark
     *            the quark of the queue to consider
     * @param initialTimestamp
     *            the last timestamp before the new message could be added to
     *            the queue (e.g. 1 ns before)
     * @return the state before a new message is (possibly) added to the queue
     * @throws StateSystemDisposedException
     * @throws AttributeNotFoundException
     */
    private ITmfStateInterval getStateBeforeMessageAddedToQueue(int queueQuark, long initialTimestamp) throws StateSystemDisposedException, AttributeNotFoundException {
        // Get the size of the queue right when the message
        // is about to be added to it
        ITmfStateInterval queueState = fQueuesSs.querySingleState(initialTimestamp, queueQuark);
        int queueSize = (queueState.getValue() == null) ? 0 : queueState.getValueInt();
        // Get the state at that moment; the state of the new message added to
        // the queue should be right after, with the same quark/queue position
        int incomingMsgPos = queueSize + 1;
        int incomingMsgPosQuark = fQueuesSs.getQuarkRelative(queueQuark, String.valueOf(incomingMsgPos));
        ITmfStateInterval stateBeforeNewMsg = fQueuesSs.querySingleState(initialTimestamp, incomingMsgPosQuark);
        return stateBeforeNewMsg;
    }

    private static SegmentType getSegmentTypeFromQueueType(RosQueueType queueType) {
        switch (queueType) {
        case SUB:
            return SegmentType.SUB_QUEUE;
        case PUB:
            return SegmentType.PUB_QUEUE;
        default:
            Activator.getInstance().logError("Case " + queueType.name() + " should be handled!"); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
    }

    /**
     * Get the message's next transition after a queue
     *
     * @param lastState
     *            the last state of the message in the queue (at position 1)
     * @return the next transition, or {@code null} if unknown
     * @throws AttributeNotFoundException
     * @throws StateSystemDisposedException
     */
    private QueueSegmentTransition getTransitionFromLastQueueState(ITmfStateInterval lastState, RosQueueType queueType, int queueQuark) throws StateSystemDisposedException {
        // TODO check for "dropped" latched messages in pub queues!

        // Check for drop
        int topicQuark = fQueuesSs.getParentAttributeQuark(queueQuark);
        try {
            int dropsQuark = fQueuesSs.getQuarkRelative(topicQuark, AbstractRosStateProvider.DROPS);
            ITmfStateInterval possibleDrop = fQueuesSs.querySingleState(lastState.getStartTime(), dropsQuark);
            if (possibleDrop.getValue() != null) {
                return QueueSegmentTransition.DROP;
            }
        } catch (AttributeNotFoundException e) {
            // Do nothing, we know there's no drop if there is no "drops" quark
        }

        switch (queueType) {
        case PUB:
            return QueueSegmentTransition.NETWORK;
        case SUB:
            return QueueSegmentTransition.CALLBACK;
        default:
            Activator.getInstance().logError("Case " + queueType.name() + " should be handled!"); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
    }

    /**
     * Get the last state of a given message in a queue
     *
     * @param message
     *            one of the states of the message
     * @param queuePosQuark
     *            the quark corresponding to the message state
     * @return the last state of this message in the queue
     * @throws AttributeNotFoundException
     * @throws StateSystemDisposedException
     */
    private ITmfStateInterval getLastStateOfMessageInQueue(ITmfStateInterval message, int queuePosQuark) throws AttributeNotFoundException, StateSystemDisposedException {
        ITmfStateInterval msg = message;
        int queueQuark = fQueuesSs.getParentAttributeQuark(queuePosQuark);
        int pos = Integer.parseInt(fQueuesSs.getAttributeName(queuePosQuark));
        long ref = msg.getValueLong();
        while (pos > 1) {
            pos--;
            long nextStartTime = msg.getEndTime() + 1;
            int nextQueuePosQuark = fQueuesSs.getQuarkRelative(queueQuark, String.valueOf(pos));
            msg = fQueuesSs.querySingleState(nextStartTime, nextQueuePosQuark);
            // Check continuity
            if (msg.getValueLong() != ref) {
                Activator.getInstance().logWarning("References do not match! previous=" + ref + " vs. now=" + msg.getValueLong()); //$NON-NLS-1$ //$NON-NLS-2$
            }
            ref = msg.getValueLong();
        }
        return msg;
    }

    private int getTargetQueueQuark() throws AttributeNotFoundException {
        return fQueuesSs.getQuarkAbsolute(fInfo.getNode(), getQueueTypeName(fInfo.getQueueType()), fInfo.getTopic(), AbstractRosStateProvider.QUEUE);
    }

    /**
     * Possible transitions for a message after waiting in a queue
     */
    private enum QueueSegmentTransition {
        /** Transition to a callback */
        CALLBACK,
        /** Sent through the network (message write) */
        NETWORK,
        /** Dropped from the queue */
        DROP
    }

    private static @Nullable String getQueueTypeName(RosQueueType type) {
        switch (type) {
        case PUB:
            return AbstractRosStateProvider.PUBLISHERS_LIST;
        case SUB:
            return AbstractRosStateProvider.SUBSCRIBERS_LIST;
        default:
            Activator.getInstance().logError("Case " + type.name() + " should be handled!"); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
    }
}
