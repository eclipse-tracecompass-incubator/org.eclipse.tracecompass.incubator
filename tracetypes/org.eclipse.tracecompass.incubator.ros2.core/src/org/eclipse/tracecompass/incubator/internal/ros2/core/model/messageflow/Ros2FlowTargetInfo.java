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

import org.eclipse.jdt.annotation.NonNull;

/**
 * Represents a target item to be followed. This contains the necessary
 * information to find the actual corresponding element to then compute a flow
 * graph out from it.
 *
 * @author Christophe Bedard
 */
public class Ros2FlowTargetInfo {

    private final long fStartTime;
    private final long fEndTime;
    private final @NonNull Ros2FlowTargetType fType;
    private final @NonNull Object fTargetEvent;

    /**
     * Constructor
     *
     * @param startTime
     *            the start time
     * @param endTime
     *            the end time
     * @param type
     *            the target type
     * @param targetEvent
     *            the target event
     */
    public Ros2FlowTargetInfo(long startTime, long endTime, @NonNull Ros2FlowTargetType type, @NonNull Object targetEvent) {
        fStartTime = startTime;
        fEndTime = endTime;
        fType = type;
        fTargetEvent = targetEvent;
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
     * @return the target type
     */
    public @NonNull Ros2FlowTargetType getTargetType() {
        return fType;
    }

    /**
     * @return the target event
     */
    public @NonNull Object getTargetEvent() {
        return fTargetEvent;
    }

    @Override
    public String toString() {
        return String.format("Ros2FlowTargetInfo: startTime=%d, endTime=%d, type=%s, targetEvent=[%s]", fStartTime, fEndTime, fType.toString(), fTargetEvent.toString()); //$NON-NLS-1$
    }
}
