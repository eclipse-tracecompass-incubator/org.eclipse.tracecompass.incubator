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

import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Message flow builder. Finds segments that come after and before a given
 * segment.
 *
 * This class and the ones it uses implement the technique described in the
 * following paper:
 *
 * C. Bédard, P.-Y. Lajoie, G. Beltrame, and M. Dagenais, "Message Flow Analysis
 * with Complex Causal Links for Distributed ROS 2 Systems," Robotics and
 * Autonomous Systems, vol. 161, p. 104361, 2023.
 * https://doi.org/10.1016/j.robot.2022.104361
 *
 * Preprint: https://arxiv.org/abs/2204.10208
 *
 * @author christophe.bedard
 */
public class Ros2MessageFlowBuilder {

    private final @NonNull Ros2MessageFlowBuildInfo fBuildInfo;

    /** Known/existing segments */
    protected final @NonNull HashMap<@NonNull Ros2MessageFlowSegment, @NonNull Ros2MessageFlowSegment> fSegments;

    /**
     * Constructor
     *
     * @param buildInfo
     *            the message flow build information
     * @param segments
     *            a reference to the known segments
     */
    public Ros2MessageFlowBuilder(@NonNull Ros2MessageFlowBuildInfo buildInfo, @NonNull HashMap<@NonNull Ros2MessageFlowSegment, @NonNull Ros2MessageFlowSegment> segments) {
        fBuildInfo = buildInfo;
        fSegments = segments;
    }

    /**
     * @return the build info
     */
    public @NonNull Ros2MessageFlowBuildInfo getBuildInfo() {
        return fBuildInfo;
    }

    /**
     * Find and connect the next segments with relation to the given segment,
     * recursively.
     *
     * @param segment
     *            the segment
     */
    public void getNextSegments(@NonNull Ros2MessageFlowSegment segment) {
        getNextSegments(segment, true);
    }

    /**
     * Find and connect the previous segments with relation to the given
     * segment, recursively.
     *
     * @param segment
     *            the segment
     */
    public void getPreviousSegments(@NonNull Ros2MessageFlowSegment segment) {
        getNextSegments(segment, false);
    }

    private void getNextSegments(@NonNull Ros2MessageFlowSegment segment, boolean forward) {
        Collection<@NonNull Ros2MessageFlowSegment> nextSegments = forward ? segment.getDirectSuccessors(getBuildInfo()) : segment.getDirectPredecessors(getBuildInfo());
        for (@NonNull
        Ros2MessageFlowSegment nextSegment : nextSegments) {
            /**
             * If we already have this next segment, we should use it instead.
             * This might happen when two segments have a common nextSegment,
             * for example when there is a "merge" in the flow. This also avoids
             * loops.
             */
            boolean nextExistsAlready = fSegments.containsKey(nextSegment);
            if (nextExistsAlready) {
                nextSegment = Objects.requireNonNull(fSegments.get(nextSegment));
            } else {
                fSegments.put(nextSegment, nextSegment);
            }

            // Link segments together
            nextSegment.addPrevious(segment);
            segment.addNext(nextSegment);

            // Do all of this again for each new next segment (yay recursion)
            if (!nextExistsAlready) {
                getNextSegments(nextSegment, forward);
            }
        }
    }
}
