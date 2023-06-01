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
 * Identifier for a host. Contains a host ID and a hostname, but the host ID is
 * the only real ID. The hostname is optional and is not used when comparing
 * HostInfo objects.
 *
 * @author Christophe Bedard
 */
public class HostInfo implements Comparable<HostInfo> {

    /**
     * Use hostname for comparison, since that probably makes more sense to the
     * user.
     */
    private static Comparator<HostInfo> COMPARATOR = Comparator.comparing(HostInfo::getHostname);

    private final @NonNull String fHostId;
    private final @NonNull String fHostname;
    private final int fSerializedValueSize;

    /**
     * Constructor
     *
     * @param hostId
     *            the host ID
     * @param hostname
     *            the hostname
     */
    public HostInfo(@NonNull String hostId, @NonNull String hostname) {
        fHostId = hostId;
        fHostname = hostname;

        int size = 0;
        size += Ros2SerializationUtil.getStringSerializedSize(fHostId);
        size += Ros2SerializationUtil.getStringSerializedSize(fHostname);
        fSerializedValueSize = size;
    }

    /**
     * @return the host ID
     */
    public @NonNull String getId() {
        return fHostId;
    }

    /**
     * @return the hostname
     */
    public @NonNull String getHostname() {
        return fHostname;
    }

    @Override
    public int compareTo(HostInfo o) {
        return COMPARATOR.compare(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fHostId);
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
        HostInfo o = (HostInfo) obj;
        // Use host ID for equality
        return o.fHostId.equals(fHostId);
    }

    @Override
    public @NonNull String toString() {
        return String.format("HostInfo: hostId=%s, hostname=%s", fHostId, fHostname); //$NON-NLS-1$
    }

    /**
     * Serialize this state value into the byte buffer.
     *
     * @param buffer
     *            the buffer
     */
    public void serializeValue(@NonNull ISafeByteBufferWriter buffer) {
        buffer.putString(fHostId);
        buffer.putString(fHostname);
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
    public static final @NonNull HostInfo read(ISafeByteBufferReader buffer) {
        String hostId = buffer.getString();
        String hostname = buffer.getString();
        return new HostInfo(hostId, hostname);
    }
}
