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

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferReader;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;

/**
 * Container for ROS 2 callback instance.
 *
 * @author Christophe Bedard
 */
public class Ros2CallbackInstance extends Ros2Instance<@NonNull Ros2ObjectHandle> {

    /** Custom type value ID for this object */
    public static final byte CUSTOM_TYPE_ID = 67;
    /** CustomStateValueFactory for this object */
    @SuppressWarnings("restriction")
    public static final @NonNull CustomStateValueFactory ROS2_CALLBACK_INSTANCE_VALUE_FACTORY = Ros2CallbackInstance::read;

    private final long fTid;
    private final boolean fIsIntraProcess;
    private final long fStartTime;
    private final long fEndTime;

    /**
     * Constructor
     *
     * @param ownerHandle
     *            the owner handle
     * @param tid
     *            the TID
     * @param isIntraProcess
     *            whether the callback is intra-process or not
     * @param startTime
     *            the start time
     * @param endTime
     *            the end time
     */
    public Ros2CallbackInstance(@NonNull Ros2ObjectHandle ownerHandle, long tid, boolean isIntraProcess, long startTime, long endTime) {
        super(ownerHandle);
        fTid = tid;
        fIsIntraProcess = isIntraProcess;
        fStartTime = startTime;
        fEndTime = endTime;
    }

    /**
     * @return the PID
     */
    public @NonNull Ros2ObjectHandle getOwnerHandle() {
        return getOriginHandle();
    }

    /**
     * @return the TID
     */
    public long getTid() {
        return fTid;
    }

    /**
     * @return the intra process flag
     */
    public boolean isIntraProcess() {
        return fIsIntraProcess;
    }

    /**
     * @return the start time
     */
    public long getStartTime() {
        return fStartTime;
    }

    /**
     * @return the end time
     */
    public long getEndTime() {
        return fEndTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fTid, fIsIntraProcess, fStartTime, fEndTime, super.hashCode());
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
        Ros2CallbackInstance o = (Ros2CallbackInstance) obj;
        return fTid == o.fTid && fIsIntraProcess == o.fIsIntraProcess && fStartTime == o.fStartTime && fEndTime == o.fEndTime;
    }

    @Override
    public String toString() {
        return String.format(
                "Ros2CallbackInstance: ownerHandle=%s, tid=%d, isIntraProcess=%b, startTime=%d, endTime=%d", //$NON-NLS-1$
                super.toString(), fTid, fIsIntraProcess, fStartTime, fEndTime);
    }

    @Override
    protected @NonNull Byte getCustomTypeId() {
        return CUSTOM_TYPE_ID;
    }

    @Override
    protected void serializeValue(ISafeByteBufferWriter buffer) {
        super.serializeValue(buffer);
        buffer.putLong(fTid);
        buffer.putInt(fIsIntraProcess ? 1 : 0);
        buffer.putLong(fStartTime);
        buffer.putLong(fEndTime);
    }

    /**
     * @param buffer
     *            the buffer
     */
    public void serializeObject(@NonNull ISafeByteBufferWriter buffer) {
        serializeValue(buffer);
    }

    @Override
    protected int getSerializedValueSize() {
        int size = 0;
        size += super.getSerializedValueSize();
        size += Long.BYTES;
        size += Integer.BYTES;
        size += Long.BYTES;
        size += Long.BYTES;
        return size;
    }

    /**
     * @param buffer
     *            the buffer
     * @return the value
     */
    public static @NonNull Ros2CallbackInstance read(ISafeByteBufferReader buffer) {
        Ros2ObjectHandle ownerHandle = Ros2ObjectHandle.read(buffer);
        long tid = buffer.getLong();
        boolean isIntraProcess = buffer.getInt() == 1;
        long startTime = buffer.getLong();
        long endTime = buffer.getLong();
        return new Ros2CallbackInstance(ownerHandle, tid, isIntraProcess, startTime, endTime);
    }
}
