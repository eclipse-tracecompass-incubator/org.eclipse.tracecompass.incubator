/**********************************************************************
 * Copyright (c) 2018 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.queues;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.AbstractRosStateProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemBuilderUtils;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
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
                } catch (StateValueTypeException | AttributeNotFoundException e) {
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

                int subsListQuark = ss.getQuarkAbsolute(nodeName, SUBSCRIBERS_LIST);

                // Add drop to the subscriber's drops
                int subDropsQuark = ss.getQuarkRelative(subsListQuark, topicName, DROPS);
                ss.modifyAttribute(timestamp - 1, msgRef, subDropsQuark);
                ss.modifyAttribute(timestamp, (Object) null, subDropsQuark);

                // Pop from subscriber queue
                int subQueueQuark = ss.getQuarkRelative(subsListQuark, topicName, QUEUE);
                StateSystemUtils.queuePollAttribute(ss, timestamp, subQueueQuark);

                return;
            }

            // TODO

            // publisher_message_queued
            if (isEvent(event, fLayout.eventPubMsgQueued())) {
                String nodeName = getNodeName(event);
                String topicName = (String) getField(event, fLayout.fieldTopic());
                Long buffRef = (Long) getField(event, fLayout.fieldBufferRef());

                // Push to publisher queue
                int pubQueueQuark = ss.getQuarkAbsoluteAndAdd(nodeName, PUBLISHERS_LIST, topicName, QUEUE);
                StateSystemUtils.queueOfferAttribute(ss, timestamp, Objects.requireNonNull(buffRef), pubQueueQuark);

                return;
            }

            // publisher_link_handle_message
            // TODO assuming this is when a msg from the pub queue gets "sent"
            // if (isEvent(event, fLayout.eventPubLinkHandleMsg())) {
            // long timestamp = event.getTimestamp().toNanos();
            // String nodeName = getNodeName(event);
            // String topicName = (String) getField(event, fLayout.fieldTopic());
            // // Long msgRef = (Long) getField(event, fLayout.fieldMsgRef());
            //
            // // Pop from publisher queue
            // int pubQueueQuark = ss.getQuarkAbsoluteAndAdd(NODES_LIST, nodeName,
            // PUBLISHERS_LIST, topicName, QUEUE);
            // popQueue(ss, timestamp, pubQueueQuark);
            //
            // return;
            // }
        } catch (AttributeNotFoundException e) {
            Activator.getInstance().logError("Could not get queue quark; there may be missing events: ", e); //$NON-NLS-1$
        }
    }
}
