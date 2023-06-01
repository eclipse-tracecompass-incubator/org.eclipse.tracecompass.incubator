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

package org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferReader;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcessPointer;

import com.google.common.base.Objects;

/**
 * Container for ROS 2 timer object.
 *
 * @author Christophe Bedard
 */
public class Ros2TimerObject extends Ros2Object<@NonNull Ros2ObjectHandle> {

    /** Custom type value ID for this object */
    public static final byte CUSTOM_TYPE_ID = 63;
    /** CustomStateValueFactory for this object */
    @SuppressWarnings("restriction")
    public static final @NonNull CustomStateValueFactory ROS2_TIMER_OBJECT_VALUE_FACTORY = b -> Ros2TimerObject.read(b);

    private final long fPeriod;
    private final @NonNull HostProcessPointer fCallback;
    private final @NonNull Ros2ObjectHandle fNodeHandle;

    /**
     * Constructor
     *
     * @param timerHandle
     *            the timer handle
     * @param period
     *            the timer period in ns
     * @param callback
     *            the corresponding callback object
     * @param nodeHandle
     *            the node handle
     */
    public Ros2TimerObject(@NonNull Ros2ObjectHandle timerHandle, long period, @NonNull HostProcessPointer callback, @NonNull Ros2ObjectHandle nodeHandle) {
        super(timerHandle);
        fPeriod = period;
        fCallback = callback;
        fNodeHandle = nodeHandle;
    }

    /**
     * @return the period
     */
    public long getPeriod() {
        return fPeriod;
    }

    /**
     * @return the callback
     */
    public @NonNull HostProcessPointer getCallback() {
        return fCallback;
    }

    /**
     * @return the node handle
     */
    public @NonNull Ros2ObjectHandle getNodeHandle() {
        return fNodeHandle;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fPeriod, fCallback, fNodeHandle, super.hashCode());
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
        if (!super.equals(obj)) {
            return false;
        }
        Ros2TimerObject o = (Ros2TimerObject) obj;
        return o.fPeriod == fPeriod && o.fCallback.equals(fCallback) && o.fNodeHandle.equals(fNodeHandle);
    }

    @Override
    public String toString() {
        return String.format(
                "Ros2TimerObject: %s, period=%d, callback=[%s], nodeHandle=[%s]", //$NON-NLS-1$
                super.toString(), fPeriod, fCallback.toString(), fNodeHandle.toString());
    }

    @Override
    protected @NonNull Byte getCustomTypeId() {
        return CUSTOM_TYPE_ID;
    }

    @Override
    protected void serializeValue(ISafeByteBufferWriter buffer) {
        super.serializeValue(buffer);
        buffer.putLong(fPeriod);
        fCallback.serializeValue(buffer);
        fNodeHandle.serializeValue(buffer);
    }

    @Override
    protected int getSerializedValueSize() {
        int size = 0;
        size += super.getSerializedValueSize();
        size += Long.BYTES;
        size += fCallback.getSerializedValueSize();
        size += fNodeHandle.getSerializedValueSize();
        return size;
    }

    /**
     * @param buffer
     *            the buffer
     * @return the value
     */
    public static @NonNull Ros2TimerObject read(ISafeByteBufferReader buffer) {
        Ros2ObjectHandle timerHandle = Ros2ObjectHandle.read(buffer);
        long period = buffer.getLong();
        HostProcessPointer callback = HostProcessPointer.read(buffer);
        Ros2ObjectHandle nodeHandle = Ros2ObjectHandle.read(buffer);
        return new Ros2TimerObject(timerHandle, period, callback, nodeHandle);
    }
}
