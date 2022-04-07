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

package org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages;

import org.eclipse.jdt.annotation.NonNull;

import com.google.common.base.Objects;

/**
 * Container for ROS 2 message timestamp.
 *
 * Since timestamps are used to match published messages with received messages,
 * we combine the timestamp with the full topic name to try to avoid collisions.
 * We could still get collisions, for example if two publishers publish at the
 * same exact time on the same topic, but the odds of this happening are very
 * low.
 *
 * @author Christophe Bedard
 */
public class Ros2MessageTimestamp {

    private final @NonNull Long fTimestamp;
    private final @NonNull String fTopicName;

    /**
     * Constructor
     *
     * @param timestamp
     *            the timestamp
     * @param topicName
     *            the topic name
     */
    public Ros2MessageTimestamp(@NonNull Long timestamp, @NonNull String topicName) {
        fTimestamp = timestamp;
        fTopicName = topicName;
    }

    /**
     * @return the timestamp
     */
    public Long getTimestamp() {
        return fTimestamp;
    }

    /**
     * @return the topic name
     */
    public String getTopicName() {
        return fTopicName;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fTimestamp, fTopicName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Ros2MessageTimestamp o = (Ros2MessageTimestamp) obj;
        return o.fTimestamp.equals(fTimestamp) && o.fTopicName.equals(fTopicName);
    }

    @Override
    public @NonNull String toString() {
        return String.format("Ros2MessageTimestamp: timestamp=%d, topicName=%s", fTimestamp, fTopicName); //$NON-NLS-1$
    }
}
