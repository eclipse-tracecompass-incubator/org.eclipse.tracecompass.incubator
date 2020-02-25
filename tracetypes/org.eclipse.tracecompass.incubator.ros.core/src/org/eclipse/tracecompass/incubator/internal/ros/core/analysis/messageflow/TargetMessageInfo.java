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

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.messageflow;

/**
 * Represents a target message to be followed. This contains the necessary
 * information to find the actual corresponding message.
 *
 * @author Christophe Bedard
 */
public class TargetMessageInfo {

    private final String fNode;
    private final String fTopic;
    private final RosQueueType fQueueType;
    private final int fQueuePosition;
    private final long fMsgRef;
    private final long fSelectedTimestamp;
    private final long fMsgTimestamp;

    /**
     * Type of the queue for the selected message
     *
     * @author Christophe Bedard
     */
    public enum RosQueueType {
        /**
         * Subscriber queue
         */
        SUB,
        /**
         * Publisher queue
         */
        PUB
    }

    /**
     * Constructor
     *
     * @param node
     *            the node name
     * @param topic
     *            the topic name
     * @param queueType
     *            the type of the queue in which this message is
     * @param queuePosition
     *            the message's position in the queue
     * @param msgRef
     *            the message reference (0x0123abc)
     * @param selectedTimestamp
     *            the selected timestamp
     * @param msgTimestamp
     *            the selected message's begin timestamp
     */
    public TargetMessageInfo(String node, String topic, RosQueueType queueType, int queuePosition, String msgRef, long selectedTimestamp, long msgTimestamp) {
        fNode = node;
        fTopic = topic;
        fQueueType = queueType;
        fQueuePosition = queuePosition;
        fMsgRef = Long.decode(msgRef);
        fSelectedTimestamp = selectedTimestamp;
        fMsgTimestamp = msgTimestamp;
    }

    /**
     * @return the node name
     */
    public String getNode() {
        return fNode;
    }

    /**
     * @return the topic name
     */
    public String getTopic() {
        return fTopic;
    }

    /**
     * @return the type of the queue in which this message is
     */
    public RosQueueType getQueueType() {
        return fQueueType;
    }

    /**
     * @return the message's position in the queue
     */
    public int getQueuePosition() {
        return fQueuePosition;
    }

    /**
     * @return the message reference
     */
    public long getMsgRef() {
        return fMsgRef;
    }

    /**
     * @return the selected timestamp
     */
    public long getSelectedTimestamp() {
        return fSelectedTimestamp;
    }

    /**
     * @return the selected message's begin timestamp
     */
    public long getMsgTimestamp() {
        return fMsgTimestamp;
    }

    @Override
    public String toString() {
        return "TargetMessageInfo: " //$NON-NLS-1$
                + fNode + ", " //$NON-NLS-1$
                + fTopic + "(" + fQueueType.name() + "):"  //$NON-NLS-1$//$NON-NLS-2$
                + "0x" + Long.toHexString(fMsgRef) //$NON-NLS-1$
                + " @ " + fQueuePosition //$NON-NLS-1$
                + ", " + fMsgTimestamp //$NON-NLS-1$
                + " | " + fSelectedTimestamp; //$NON-NLS-1$
    }
}
