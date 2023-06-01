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

import java.util.Comparator;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferReader;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;

import com.google.common.base.Objects;

/**
 * Identifier for a process on a given host. Combines a host ID and a PID.
 *
 * @author Christophe Bedard
 */
public class HostProcess implements Comparable<HostProcess> {

    private static Comparator<HostProcess> COMPARATOR = Comparator.comparing(HostProcess::getHostId)
            .thenComparing(HostProcess::getPid);

    private final @NonNull HostInfo fHostId;
    private final @NonNull Long fPid;
    private final int fSerializedValueSize;

    /**
     * Constructor
     *
     * @param hostId
     *            the host ID
     * @param pid
     *            the PID
     */
    public HostProcess(@NonNull HostInfo hostId, @NonNull Long pid) {
        fHostId = hostId;
        fPid = pid;

        int size = 0;
        size += fHostId.getSerializedValueSize();
        size += Long.BYTES;
        fSerializedValueSize = size;
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
    public @NonNull Long getPid() {
        return fPid;
    }

    @Override
    public int compareTo(HostProcess o) {
        return COMPARATOR.compare(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fHostId, fPid);
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
        HostProcess o = (HostProcess) obj;
        return o.fHostId.equals(fHostId) && o.fPid.equals(fPid);
    }

    @Override
    public @NonNull String toString() {
        return String.format("HostProcess: pid=%d, hostId=[%s]", fPid, fHostId.toString()); //$NON-NLS-1$
    }

    /**
     * Serialize this state value into the byte buffer.
     *
     * @param buffer
     *            the buffer
     */
    public void serializeValue(@NonNull ISafeByteBufferWriter buffer) {
        fHostId.serializeValue(buffer);
        buffer.putLong(fPid);
    }

    /**
     * @return the serialized size
     */
    public int getSerializedValueSize() {
        return fSerializedValueSize;
    }

    /**
     * @param buffer
     *            the buffer
     * @return the value
     */
    public static final @NonNull HostProcess read(ISafeByteBufferReader buffer) {
        HostInfo hostId = HostInfo.read(buffer);
        Long pid = buffer.getLong();
        return new HostProcess(hostId, pid);
    }
}
