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
import java.util.Collection;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents a single message flow segment, with a parent/children (or not), a
 * type, and some other information
 *
 * @author Christophe Bedard
 */
public class RosMessageFlowSegment {

    private final long fStartTime;
    private final long fEndTime;
    private final SegmentType fType;
    private final String fNodeName;
    private final String fTopicName;

    private @Nullable RosMessageFlowSegment fPrevious = null;
    private final Collection<RosMessageFlowSegment> fNext = new ArrayList<>();

    /**
     * {@link RosMessageFlowSegment} type
     *
     * @author Christophe Bedard
     */
    public enum SegmentType {
        /** Subscriber queue */
        SUB_QUEUE,
        /** Subscriber callback */
        SUB_CALLBACK,
        /** Publisher queue */
        PUB_QUEUE,
        /** Invalid/nothing */
        INVALID
    }

    /**
     * Constructor
     *
     * @param startTime
     *            the start timestamp of the segment
     * @param endTime
     *            the end timestamp of the segment
     * @param type
     *            the segment type
     * @param nodeName
     *            the name of the node associated with the segment
     * @param topicName
     *            the topic associated with the segment
     */
    public RosMessageFlowSegment(long startTime, long endTime, SegmentType type, String nodeName, String topicName) {
        this(startTime, endTime, type, nodeName, topicName, null);
    }

    /**
     * Constructor
     *
     * @param start
     *            the start timestamp of the segment
     * @param end
     *            the end timestamp of the segment
     * @param type
     *            the segment type
     * @param nodeName
     *            the name of the node associated with the segment
     * @param topicName
     *            the topic associated with the segment
     * @param previous
     *            the previous segment
     */
    public RosMessageFlowSegment(long start, long end, SegmentType type, String nodeName, String topicName, RosMessageFlowSegment previous) {
        fStartTime = start;
        fEndTime = end;
        fType = type;
        fNodeName = nodeName;
        fTopicName = topicName;
        fPrevious = previous;
    }

    /**
     * @param previous
     *            the previous segment
     */
    public void setPrevious(RosMessageFlowSegment previous) {
        fPrevious = previous;
    }

    /**
     * @param next
     *            the segment to add to the collection of next segments
     */
    public void addNext(RosMessageFlowSegment next) {
        fNext.add(next);
    }

    /**
     * @return the previous segment, or {@code null} if this is the first
     *         segment
     */
    public @Nullable RosMessageFlowSegment getPrevious() {
        return fPrevious;
    }

    /**
     * @return true if there is a previous segment, false otherwise
     */
    public boolean hasPrevious() {
        return fPrevious != null;
    }

    /**
     * @return the next segment(s), or empty if it's the last one
     */
    public Collection<RosMessageFlowSegment> getNext() {
        return fNext;
    }

    /**
     * @return true if there is a next segment, false otherwise
     */
    public boolean hasNext() {
        return !fNext.isEmpty();
    }

    /**
     * @return the start timestamp of the segment
     */
    public long getStartTime() {
        return fStartTime;
    }

    /**
     * @return the end timestamp of the segment
     */
    public long getEndTime() {
        return fEndTime;
    }

    /**
     * @return the segment type
     */
    public SegmentType getType() {
        return fType;
    }

    /**
     * @return the name of the node associated with the segment
     */
    public String getNodeName() {
        return fNodeName;
    }

    /**
     * @return the topic associated with the segment
     */
    public String getTopicName() {
        return fTopicName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fEndTime, fNext, fNodeName, fPrevious, fStartTime, fTopicName, fType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RosMessageFlowSegment)) {
            return false;
        }
        RosMessageFlowSegment other = (RosMessageFlowSegment) obj;
        return fEndTime == other.fEndTime && Objects.equals(fNext, other.fNext) && Objects.equals(fNodeName, other.fNodeName) && Objects.equals(fPrevious, other.fPrevious) && fStartTime == other.fStartTime
                && Objects.equals(fTopicName, other.fTopicName) && fType == other.fType;
    }
}
