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

package org.eclipse.tracecompass.incubator.internal.ros2.core.model;

import org.eclipse.jdt.annotation.NonNull;

import com.google.common.base.Objects;

/**
 * Identifier for a thread on a given host. Combines a host ID and a TID.
 *
 * @author Christophe Bedard
 */
public class HostThread {

    private final @NonNull HostInfo fHostId;
    private final @NonNull Long fTid;

    /**
     * Constructor
     *
     * @param hostId
     *            the host ID
     * @param tid
     *            the TID
     */
    public HostThread(@NonNull HostInfo hostId, @NonNull Long tid) {
        fHostId = hostId;
        fTid = tid;
    }

    /**
     * @return the host ID
     */
    public @NonNull HostInfo getHostId() {
        return fHostId;
    }

    /**
     * @return the PID
     */
    public @NonNull Long getTid() {
        return fTid;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fHostId, fTid);
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
        HostThread o = (HostThread) obj;
        return o.fHostId.equals(fHostId) && o.fTid.equals(fTid);
    }

    @Override
    public @NonNull String toString() {
        return String.format("HostThread: tid=%d, hostId=[%s]", fTid, fHostId.toString()); //$NON-NLS-1$
    }
}
