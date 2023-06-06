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

/**
 * Unique identifier for a pointer or memory address value on a host. Combines
 * the {@link HostProcess} and the pointer to avoid collisions when analyzing
 * traces from multiple hosts.
 *
 * @see HostProcess
 *
 * @author Christophe Bedard
 */
public class HostProcessPointer extends HostProcessValue<@NonNull Long> {

    /** Custom type value ID for this object */
    public static final byte CUSTOM_TYPE_ID_HOST_PROCESS_POINTER = 73;
    /** CustomStateValueFactory for this object */
    @SuppressWarnings("restriction")
    public static final @NonNull CustomStateValueFactory HOST_PROCESS_POINTER_VALUE_FACTORY = b -> HostProcessPointer.read(b);

    private static final @NonNull String HEX_PREFIX = "0x"; //$NON-NLS-1$

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
        super(hostProcess, pointer);

        int size = 0;
        size += super.getSerializedValueSize();
        size += Long.BYTES;
        fSerializedValueSize = size;
    }

    /**
     * @return the pointer value
     */
    public @NonNull Long getPointer() {
        return super.getValue();
    }

    /**
     * @return whether the pointer is <code>nullptr</code>, i.e., 0
     */
    public boolean isNullptr() {
        return 0 == getPointer();
    }

    @Override
    protected @NonNull String valueToString() {
        return HEX_PREFIX + Long.toHexString(getPointer());
    }

    @Override
    public @NonNull String toString() {
        return String.format("HostProcessPointer: pointer=%s", super.toString()); //$NON-NLS-1$
    }

    @Override
    protected @NonNull Byte getCustomTypeId() {
        return CUSTOM_TYPE_ID_HOST_PROCESS_POINTER;
    }

    @Override
    public void serializeValue(@NonNull ISafeByteBufferWriter buffer) {
        super.serializeValue(buffer);
        buffer.putLong(getPointer());
    }

    @Override
    public int getSerializedValueSize() {
        return fSerializedValueSize;
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
}
