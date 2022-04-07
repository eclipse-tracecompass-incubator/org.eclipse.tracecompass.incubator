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

package org.eclipse.tracecompass.incubator.internal.ros2.core.model.executor;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferReader;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.internal.provisional.statesystem.core.statevalue.CustomStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;

/**
 * Container for ROS 2 executor state instance.
 *
 * @author Christophe Bedard
 */
@SuppressWarnings("restriction")
public class Ros2ExecutorStateInstance extends CustomStateValue {

    /** Custom type value ID for this object */
    public static final byte CUSTOM_TYPE_ID = 72;
    /** CustomStateValueFactory for this object */
    public static final @NonNull CustomStateValueFactory ROS2_EXECUTOR_STATE_INSTANCE_VALUE_FACTORY = Ros2ExecutorStateInstance::read;

    /**
     * Possible states for the executor.
     *
     * @author Christophe Bedard
     */
    public enum ExecutorState {
        /** Get next ready executable */
        GET_NEXT_READY,
        /** Wait for work */
        WAIT_FOR_WORK,
        /**
         * Execute (has a corresponding handle for the object being executed)
         */
        EXECUTE
    }

    private final @NonNull ExecutorState fState;
    private final @NonNull Long fHandle;

    /**
     * @param state
     *            the state
     * @param handle
     *            the corresponding handle, if any
     */
    public Ros2ExecutorStateInstance(@NonNull ExecutorState state, @Nullable Long handle) {
        fState = state;
        fHandle = null != handle ? handle : 0;
    }

    /**
     * @return the state
     */
    public @NonNull ExecutorState getState() {
        return fState;
    }

    /**
     * @return the corresponding handle, or <code>0</code> if none; see
     *         {@link ExecutorState}
     */
    public @NonNull Long getHandle() {
        return fHandle;
    }

    @Override
    public String toString() {
        return String.format("Ros2ExecutorState: type=%s, handle=%d", fState.toString(), fHandle); //$NON-NLS-1$
    }

    @Override
    public int compareTo(@NonNull ITmfStateValue o) {
        return this.equals(o) ? 0 : -1;
    }

    @Override
    protected @NonNull Byte getCustomTypeId() {
        return CUSTOM_TYPE_ID;
    }

    @Override
    protected void serializeValue(@NonNull ISafeByteBufferWriter buffer) {
        buffer.putInt(fState.ordinal());
        buffer.putLong(fHandle);
    }

    @Override
    protected int getSerializedValueSize() {
        int size = 0;
        size += Integer.BYTES;
        size += Long.BYTES;
        return size;
    }

    /**
     * @param buffer
     *            the buffer
     * @return the value
     */
    public static @NonNull Ros2ExecutorStateInstance read(ISafeByteBufferReader buffer) {
        ExecutorState state = ExecutorState.values()[buffer.getInt()];
        Long handle = buffer.getLong();
        return new Ros2ExecutorStateInstance(state, handle);
    }
}
