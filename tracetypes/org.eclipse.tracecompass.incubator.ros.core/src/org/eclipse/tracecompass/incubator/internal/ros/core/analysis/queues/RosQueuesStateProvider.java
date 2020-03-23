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

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.queues;

import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.AbstractRosStateProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemBuilderUtils;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * State provider for the ROS Queues analysis
 *
 * @author Christophe Bedard
 *
 */
public class RosQueuesStateProvider extends AbstractRosStateProvider {

    private static final int VERSION_NUMBER = 0;

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     */
    public RosQueuesStateProvider(ITmfTrace trace) {
        super(trace, RosQueuesAnalysis.getFullAnalysisId());
    }

    @Override
    public int getVersion() {
        return VERSION_NUMBER;
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        super.eventHandle(event);

        if (!considerEvent(event)) {
            return;
        }

        ITmfStateSystemBuilder ss = Objects.requireNonNull(getStateSystemBuilder());

        long timestamp = event.getTimestamp().toNanos();

        try {
            // subscriber_callback_added
            if (isEvent(event, fLayout.eventSubCallbackAdded())) {
                String nodeName = getNodeName(event);
                String topicName = (String) getField(event, fLayout.fieldSourceName());

                // Add to the node's subscribers list
                int subsListQuark = ss.getQuarkAbsoluteAndAdd(nodeName, SUBSCRIBERS_LIST);
                try {
                    StateSystemBuilderUtils.incrementAttributeInt(ss, timestamp, subsListQuark, 1);
                } catch (StateValueTypeException e) {
                }

                // Increment subscribers list counter
                int subQuark = ss.getQuarkRelativeAndAdd(subsListQuark, topicName);
                ss.modifyAttribute(timestamp, topicName, subQuark);

                // Create subscriber-specific quarks right away
                ss.getQuarkRelativeAndAdd(subsListQuark, CALLBACKS);
                ss.getQuarkRelativeAndAdd(subQuark, QUEUE);
                ss.getQuarkRelativeAndAdd(subQuark, SUBSCRIBER_MESSAGE_PROCESSING);
                ss.getQuarkRelativeAndAdd(subQuark, DROPS);

                return;
            }

            // publisher_message_queued
            if (isEvent(event, fLayout.eventPubMsgQueued())) {
                String nodeName = getNodeName(event);
                String topicName = (String) getField(event, fLayout.fieldTopic());
                Long buffRef = (Long) getField(event, fLayout.fieldBufferRef());

                // If it's a latched message (no subscribers and latching is
                // enabled), replace last message if there is one
                Long isLatchedMsgInt = (Long) getField(event, fLayout.fieldIsLatchedMsg());
                boolean isLatchedMsg = (isLatchedMsgInt == 1);
                if (isLatchedMsg) {
                    // Pop from publisher queue
                    int pubQueueQuark = ss.getQuarkAbsoluteAndAdd(nodeName, PUBLISHERS_LIST, topicName, QUEUE);
                    StateSystemUtils.queuePollAttribute(ss, timestamp, pubQueueQuark);
                }

                // Push to publisher queue
                int pubQueueQuark = ss.getQuarkAbsoluteAndAdd(nodeName, PUBLISHERS_LIST, topicName, QUEUE);
                StateSystemUtils.queueOfferAttribute(ss, timestamp, Objects.requireNonNull(buffRef), pubQueueQuark);

                return;
            }

            // subscriber_link_message_dropped
            // subscriber_link_message_write
            boolean isSubLinkDrop = false;
            if ((isSubLinkDrop = isEvent(event, fLayout.eventSubLinkMsgDropped())) || isEvent(event, fLayout.eventSubLinkMsgWrite())) {
                String nodeName = getNodeName(event);
                Long msgRef = (Long) getField(event, fLayout.fieldMsgRef());

                // Find topicName by looking at publisher node's queues and finding matching bufferRef
                String topicName = null;
                int nodePubsQuark = ss.getQuarkAbsolute(nodeName, PUBLISHERS_LIST);
                List<@NonNull Integer> pubTopics = ss.getSubAttributes(nodePubsQuark, false);
                for (@NonNull Integer topicQuark : pubTopics) {
                    @NonNull String topicQuarkName = ss.getAttributeName(topicQuark);
                    List<@NonNull Integer> queueQuarks = ss.getSubAttributes(topicQuark, false);
                    if (!queueQuarks.isEmpty()) {
                        Integer queueQuark = queueQuarks.get(0);
                        Object bufferRef = StateSystemUtils.queuePeekAttribute(ss, timestamp, queueQuark);
                        if (bufferRef != null && msgRef.equals(bufferRef)) {
                            topicName = topicQuarkName;
                            break;
                        }
                    }
                }

                // If a topic was not found, there may be lost events
                if (topicName == null) {
                    Activator.getInstance()
                            .logError("[" + nodeName + "] Could not find matching topic for subscriber_link_message_" + (isSubLinkDrop ? "dropped" : "write") + " with msgRef=" + formatLongDecToHex(msgRef) + "; there may be lost events"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
                    return;
                }

                // Pop from publisher queue
                int topicQuark = ss.getQuarkAbsolute(nodeName, PUBLISHERS_LIST, topicName);
                int pubQueueQuark = ss.getQuarkRelativeAndAdd(topicQuark, QUEUE);
                StateSystemUtils.queuePollAttribute(ss, timestamp, pubQueueQuark);

                if (isSubLinkDrop) {
                    // Add drop to the publisher's drops
                    markDroppedMsg(ss, timestamp, topicQuark, msgRef);
                }

                return;
            }

            // publisher_link_handle_message
            // TODO figure out what to do here
            // if (isEvent(event, fLayout.eventPubLinkHandleMsg())) {
            // return;
            // }

            // subscription_message_queued
            if (isEvent(event, fLayout.eventSubMsgQueued())) {
                String nodeName = getNodeName(event);
                String topicName = (String) getField(event, fLayout.fieldTopic());
                Long msgRef = (Long) getField(event, fLayout.fieldMsgRef());

                // Push to subscriber queue
                int subQueueQuark = ss.getQuarkAbsolute(nodeName, SUBSCRIBERS_LIST, topicName, QUEUE);
                StateSystemUtils.queueOfferAttribute(ss, timestamp, Objects.requireNonNull(msgRef), subQueueQuark);

                return;
            }

            // subscription_message_dropped
            if (isEvent(event, fLayout.eventSubMsgDropped())) {
                String nodeName = getNodeName(event);
                String topicName = (String) getField(event, fLayout.fieldTopic());
                Long msgRef = (Long) getField(event, fLayout.fieldMsgRef());

                int topicQuark = ss.getQuarkAbsolute(nodeName, SUBSCRIBERS_LIST, topicName);

                // Add drop to the subscriber's drops
                markDroppedMsg(ss, timestamp, topicQuark, msgRef);

                // Pop from subscriber queue
                int subQueueQuark = ss.getQuarkRelative(topicQuark, QUEUE);
                StateSystemUtils.queuePollAttribute(ss, timestamp, subQueueQuark);

                return;
            }

            // callback_start
            if (isEvent(event, fLayout.eventCallbackStart())) {
                String nodeName = getNodeName(event);
                Long callbackRef = (Long) getField(event, fLayout.fieldCallbackRef());

                // Start callback in the node's general callbacks list
                int subCallbacksQuark = ss.getQuarkAbsolute(nodeName, SUBSCRIBERS_LIST, CALLBACKS);
                ss.modifyAttribute(timestamp, callbackRef, subCallbacksQuark);

                return;
            }

            // subscriber_callback_start
            if (isEvent(event, fLayout.eventSubCallbackStart())) {
                String nodeName = getNodeName(event);
                String topicName = (String) getField(event, fLayout.fieldTopic());
                Long msgRef = (Long) getField(event, fLayout.fieldMsgRef());

                int subsListQuark = ss.getQuarkAbsolute(nodeName, SUBSCRIBERS_LIST);

                // Start callback in subscriber
                int subCallbackQuark = ss.getQuarkRelative(subsListQuark, topicName, SUBSCRIBER_MESSAGE_PROCESSING);
                ss.modifyAttribute(timestamp, msgRef, subCallbackQuark);

                // Insert state change in the node's general callbacks list
                int subCallbacksQuark = ss.getQuarkRelative(subsListQuark, CALLBACKS);
                ss.modifyAttribute(timestamp, msgRef, subCallbacksQuark);

                // Pop from subscriber queue
                int subQueueQuark = ss.getQuarkRelative(subsListQuark, topicName, QUEUE);
                StateSystemUtils.queuePollAttribute(ss, timestamp, subQueueQuark);

                return;
            }

            // subscriber_callback_end
            if (isEvent(event, fLayout.eventSubCallbackEnd())) {
                String nodeName = getNodeName(event);
                String topicName = (String) getField(event, fLayout.fieldTopic());

                int subsListQuark = ss.getQuarkAbsolute(nodeName, SUBSCRIBERS_LIST);

                // End callback in subscriber
                int subCallbackQuark = ss.getQuarkRelative(subsListQuark, topicName, SUBSCRIBER_MESSAGE_PROCESSING);
                ss.modifyAttribute(timestamp, (Object) null, subCallbackQuark);

                // End state change in node's general callbacks list
                int subCallbacksQuark = ss.getQuarkRelative(subsListQuark, CALLBACKS);
                ss.modifyAttribute(timestamp, (Object) null, subCallbacksQuark);

                return;
            }

            // callback_end
            if (isEvent(event, fLayout.eventCallbackEnd())) {
                String nodeName = getNodeName(event);
                Long callbackRef = (Long) getField(event, fLayout.fieldCallbackRef());

                // End subscribers callback
                int subCallbacksQuark = ss.getQuarkAbsolute(nodeName, SUBSCRIBERS_LIST, CALLBACKS);
                ss.updateOngoingState(callbackRef, subCallbacksQuark);
                ss.modifyAttribute(timestamp, (Object) null, subCallbacksQuark);

                return;
            }
        } catch (AttributeNotFoundException e) {
            Activator.getInstance().logError("Could not get queue quark; there may be missing events: ", e); //$NON-NLS-1$
        } catch (StateSystemDisposedException e) {
            Activator.getInstance().logError("Error querying statesystem: ", e); //$NON-NLS-1$
        }
    }

    /**
     * Mark a message as dropped. To make it clearly visible, the dropped state
     * duration will match that of the dropped message in the queue.
     */
    private static void markDroppedMsg(ITmfStateSystemBuilder ss, long timestamp, int topicQuark, Long msgRef) throws StateSystemDisposedException {
        // Get start time of the last state before getting dropped
        int queueQuark = ss.getQuarkRelativeAndAdd(topicQuark, QUEUE, "1"); //$NON-NLS-1$
        long droppedMsgStartTime = ss.querySingleState(timestamp - 1, queueQuark).getStartTime();
        int dropsQuark = ss.getQuarkRelativeAndAdd(topicQuark, DROPS);
        ss.modifyAttribute(droppedMsgStartTime, msgRef, dropsQuark);
        ss.modifyAttribute(timestamp, (Object) null, dropsQuark);
    }
}
