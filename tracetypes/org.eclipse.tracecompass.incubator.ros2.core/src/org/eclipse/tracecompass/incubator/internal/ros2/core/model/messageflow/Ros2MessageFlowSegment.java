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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;

/**
 * Abstract data container for a ROS 2 message flow segment. Has a start time,
 * an end time, and previous & next segments.
 *
 * @author Christophe Bedard
 */
public abstract class Ros2MessageFlowSegment {

    private final long fStartTime;
    private final long fEndTime;
    private final @Nullable Ros2ObjectHandle fNodeHandle;

    private final Collection<@NonNull Ros2MessageFlowSegment> fPrevious = new ArrayList<>();
    private final Collection<@NonNull Ros2MessageFlowSegment> fNext = new ArrayList<>();

    /**
     * Constructor
     *
     * @param startTime
     *            the start time
     * @param endTime
     *            the end time
     * @param nodeHandle
     *            the node handle corresponding to this segment, or <code>null</code> if this segment is a link segment
     */
    protected Ros2MessageFlowSegment(long startTime, long endTime, @Nullable Ros2ObjectHandle nodeHandle) {
        fStartTime = startTime;
        fEndTime = endTime;
        fNodeHandle = nodeHandle;
    }

    /**
     * @param next
     *            the next segment to add
     */
    public void addNext(@NonNull Ros2MessageFlowSegment next) {
        fNext.add(next);
    }

    /**
     * @return the next segment(s)
     */
    public Collection<@NonNull Ros2MessageFlowSegment> getNext() {
        return fNext;
    }

    /**
     * @param previous
     *            the previous segment to add
     */
    public void addPrevious(@NonNull Ros2MessageFlowSegment previous) {
        fPrevious.add(previous);
    }

    /**
     * @return the previous segment(s)
     */
    public Collection<@NonNull Ros2MessageFlowSegment> getPrevious() {
        return fPrevious;
    }

    /**
     * @return whether the segment has a next segment
     */
    public boolean hasNext() {
        return !fNext.isEmpty();
    }

    /**
     * @return whether the segment has a previous segment
     */
    public boolean hasPrevious() {
        return !fPrevious.isEmpty();
    }

    /**
     * @return the start time
     */
    public long getStartTime() {
        return fStartTime;
    }

    /**
     * @return the end time
     */
    public long getEndTime() {
        return fEndTime;
    }

    /**
     * @return whether the segment is a link segment, i.e., between two objects
     */
    public boolean isLink() {
        return null == fNodeHandle;
    }

    /**
     * @return the node handle corresponding to this segment, only valid and
     *         non-<code>null</code> for non-link segments
     * @see #isLink()
     */
    public @Nullable Ros2ObjectHandle getNodeHandle() {
        return fNodeHandle;
    }

    /**
     * Get the direct successors to this segment.
     *
     * @param info
     *            information that can be used to find the successors
     * @return the pipeline segment successors
     */
    public abstract @NonNull Collection<@NonNull Ros2MessageFlowSegment> getDirectSuccessors(@NonNull Ros2MessageFlowBuildInfo info);

    /**
     * Get the direct predecessors to this segment.
     *
     * @param info
     *            information that can be used to find the predecessors
     * @return the pipeline segment predecessors
     */
    public abstract @NonNull Collection<@NonNull Ros2MessageFlowSegment> getDirectPredecessors(@NonNull Ros2MessageFlowBuildInfo info);

    @Override
    public int hashCode() {
        return Objects.hash(fStartTime, fEndTime, fPrevious, fNext);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Ros2MessageFlowSegment)) {
            return false;
        }
        Ros2MessageFlowSegment o = (Ros2MessageFlowSegment) obj;
        return fStartTime == o.fStartTime && fEndTime == o.fEndTime;
    }

    @Override
    public String toString() {
        return String.format("startTime=%d, endTime=%d", fStartTime, fEndTime); //$NON-NLS-1$
    }
}
