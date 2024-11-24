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
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Gid;

import com.google.common.base.Objects;

/**
 * Container for ROS 2 request info.
 *
 * This is used to match a request sent by a client to a request taken by a
 * service.
 *
 * @author Christophe Bedard
 */
public class Ros2Request {

    private final @NonNull Gid fClientGid;
    private final long fSequenceNumber;

    /**
     * Constructor
     *
     * @param clientGid
     *            the GID of the client sending the request
     * @param sequenceNumber
     *            the request sequence number
     */
    public Ros2Request(@NonNull Gid clientGid, long sequenceNumber) {
        fClientGid = clientGid;
        fSequenceNumber = sequenceNumber;
    }

    /**
     * @return the GID of the client sending the request
     */
    public @NonNull Gid getClientGid() {
        return fClientGid;
    }

    /**
     * @return the request sequence number
     */
    public long getSequenceNumber() {
        return fSequenceNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fClientGid, fSequenceNumber);
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
        Ros2Request o = (Ros2Request) obj;
        return o.fClientGid.equals(fClientGid) && o.fSequenceNumber == fSequenceNumber;
    }

    @Override
    public @NonNull String toString() {
        return String.format("Ros2Request: clientGid=%d, sequenceNumber=%s", fClientGid.toString(), fSequenceNumber); //$NON-NLS-1$
    }
}
