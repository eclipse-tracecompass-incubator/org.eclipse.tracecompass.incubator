/**********************************************************************
 * Copyright (c) 2024 Apex.AI, Inc.
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
 * Container for ROS 2 response info.
 *
 * This is used to match a response sent by a service to a response taken by a
 * client.
 *
 * @author Christophe Bedard
 */
public class Ros2Response {

    private final @NonNull Ros2Request fRequestInfo;
    private final long fSourceTimestamp;

    /**
     * Constructor
     *
     * @param requestInfo
     *            the info of the request that this response is for
     * @param sourceTimestamp
     *            the source timestamp of the response
     */
    public Ros2Response(@NonNull Ros2Request requestInfo, long sourceTimestamp) {
        fRequestInfo = requestInfo;
        fSourceTimestamp = sourceTimestamp;
    }

    /**
     * @return the info of the request that this response is for
     */
    public @NonNull Ros2Request getRequestInfo() {
        return fRequestInfo;
    }

    /**
     * @return the source timestamp of the response
     */
    public long getSourceTimestamp() {
        return fSourceTimestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fRequestInfo, fSourceTimestamp);
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
        Ros2Response o = (Ros2Response) obj;
        return o.fRequestInfo.equals(fRequestInfo) && o.fSourceTimestamp == fSourceTimestamp;
    }

    @Override
    public @NonNull String toString() {
        return String.format("Ros2Response: requestInfo=[%s], sourceTimestamp=%s", fRequestInfo.toString(), fSourceTimestamp); //$NON-NLS-1$
    }
}
