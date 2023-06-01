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
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferReader;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;

import com.google.common.base.Objects;

/**
 * Unique identifier for a pointer or memory address value on a host. Combines
 * the {@link HostProcess} and the pointer to avoid collisions when analyzing
 * traces from multiple hosts.
 *
 * @see HostProcess
 *
 * @author Christophe Bedard
 */
public class HostProcessPointer {

    private static final @NonNull String STRING_ID_SEP = "|"; //$NON-NLS-1$
    private static final @NonNull String HEX_PREFIX = "0x"; //$NON-NLS-1$

    private final @NonNull HostProcess fHostProcess;
    private final @NonNull Long fPointer;
    private final int fSerializedValueSize;

    /**
     * Constructor
     *
     * @param hostProcess
     *            the host process object
     * @param pointer
     *            the pointer/memory address value
     */
    public HostProcessPointer(@NonNull HostProcess hostProcess, @NonNull Long pointer) {
        fHostProcess = hostProcess;
        fPointer = pointer;

        int size = 0;
        size += fHostProcess.getSerializedValueSize();
        size += Long.BYTES;
        fSerializedValueSize = size;
    }

    /**
     * @return the host process
     */
    public HostProcess getHostProcess() {
        return fHostProcess;
    }

    /**
     * @return the PID
     */
    public @NonNull Long getPid() {
        return fHostProcess.getPid();
    }

    /**
     * @return the pointer value
     */
    public @NonNull Long getPointer() {
        return fPointer;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fHostProcess, fPointer);
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
        HostProcessPointer o = (HostProcessPointer) obj;
        return o.fHostProcess.equals(fHostProcess) && o.fPointer.equals(fPointer);
    }

    /**
     * @return the string ID to uniquely represent this pointer
     */
    public @NonNull String getStringId() {
        return String.format(
                "%s%s%s%d%s%s", //$NON-NLS-1$
                HEX_PREFIX, Long.toHexString(getPointer()), STRING_ID_SEP, getPid(), STRING_ID_SEP, getHostProcess().getHostId().getId());
    }

    @Override
    public @NonNull String toString() {
        return String.format(
                "HostProcessPointer: pointer=0x%s, pid=%d, hostId=[%s]", //$NON-NLS-1$
                Long.toHexString(getPointer()), getPid(), getHostProcess().getHostId().toString());
    }

    /**
     * Serialize this state value into the byte buffer.
     *
     * @param buffer
     *            the buffer
     */
    public void serializeValue(@NonNull ISafeByteBufferWriter buffer) {
        fHostProcess.serializeValue(buffer);
        buffer.putLong(fPointer);
    }

    /**
     * @param buffer
     *            the buffer
     * @return the value
     */
    public static @NonNull HostProcessPointer read(ISafeByteBufferReader buffer) {
        HostProcess hostProcess = HostProcess.read(buffer);
        Long pointer = buffer.getLong();
        return new HostProcessPointer(hostProcess, pointer);
    }

    /**
     * @return the serialized size
     */
    public int getSerializedValueSize() {
        return fSerializedValueSize;
    }
}
