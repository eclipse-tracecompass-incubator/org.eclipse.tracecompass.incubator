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
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcessPointer;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;

/**
 * Container for ROS 2 message take instance.
 *
 * @author Christophe Bedard
 */
public class Ros2TakeInstance extends Ros2Instance<@NonNull Ros2ObjectHandle> {

    /** Custom type value ID for this object */
    public static final byte CUSTOM_TYPE_ID = 66;
    /** CustomStateValueFactory for this object */
    @SuppressWarnings("restriction")
    public static final @NonNull CustomStateValueFactory ROS2_TAKE_INSTANCE_VALUE_FACTORY = Ros2TakeInstance::read;

    private final long fTid;
    private final @NonNull HostProcessPointer fMessage;
    private final long fSourceTimestamp;
    private final long fStartTime;
    private final long fEndTime;

    /**
     * Constructor
     *
     * @param subscriptionHandle
     *            the subscription handle
     * @param tid
     *            the TID
     * @param message
     *            the message pointer
     * @param sourceTimestamp
     *            the source timestamp
     * @param startTime
     *            the start time
     * @param endTime
     *            the end time
     */
    public Ros2TakeInstance(@NonNull Ros2ObjectHandle subscriptionHandle, long tid, @NonNull HostProcessPointer message, long sourceTimestamp, long startTime, long endTime) {
        super(subscriptionHandle);
        fTid = tid;
        fMessage = message;
        fSourceTimestamp = sourceTimestamp;
        fStartTime = startTime;
        fEndTime = endTime;
    }

    /**
     * @return the subscription handle
     */
    public @NonNull Ros2ObjectHandle getSubscriptionHandle() {
        return getOriginHandle();
    }

    /**
     * @return the TID
     */
    public long getTid() {
        return fTid;
    }

    /**
     * @return the message pointer
     */
    public @NonNull HostProcessPointer getMessage() {
        return fMessage;
    }

    /**
     * @return the source timestamp
     */
    public long getSourceTimestamp() {
        return fSourceTimestamp;
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
        return Objects.hash(fTid, fMessage, fSourceTimestamp, fStartTime, fEndTime, super.hashCode());
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
        Ros2TakeInstance o = (Ros2TakeInstance) obj;
        return fTid == o.fTid && fMessage.equals(o.fMessage) && fSourceTimestamp == o.fSourceTimestamp && fStartTime == o.fStartTime && fEndTime == o.fEndTime;
    }

    @Override
    public String toString() {
        return String.format(
                "Ros2TakeInstance: subscriptionHandle=%s, tid=%d, message=[%s], sourceTimestamp=%d, startTime=%d, endTime=%d", //$NON-NLS-1$
                super.toString(), fTid, fMessage.toString(), fSourceTimestamp, fStartTime, fEndTime);
    }

    @Override
    protected @NonNull Byte getCustomTypeId() {
        return CUSTOM_TYPE_ID;
    }

    @Override
    protected void serializeValue(ISafeByteBufferWriter buffer) {
        super.serializeValue(buffer);
        buffer.putLong(fTid);
        fMessage.serializeValue(buffer);
        buffer.putLong(fSourceTimestamp);
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
        size += fMessage.getSerializedValueSize();
        size += Long.BYTES;
        size += Long.BYTES;
        size += Long.BYTES;
        return size;
    }

    /**
     * @param buffer
     *            the buffer
     * @return the value
     */
    public static @NonNull Ros2TakeInstance read(ISafeByteBufferReader buffer) {
        Ros2ObjectHandle subscriptionHandle = Ros2ObjectHandle.read(buffer);
        long tid = buffer.getLong();
        HostProcessPointer message = HostProcessPointer.read(buffer);
        long sourceTimestamp = buffer.getLong();
        long startTime = buffer.getLong();
        long endTime = buffer.getLong();
        return new Ros2TakeInstance(subscriptionHandle, tid, message, sourceTimestamp, startTime, endTime);
    }
}
