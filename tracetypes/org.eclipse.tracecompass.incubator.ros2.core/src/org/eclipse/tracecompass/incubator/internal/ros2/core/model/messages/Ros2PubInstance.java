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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferReader;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcessPointer;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;

import com.google.common.base.Objects;

/**
 * Container for ROS 2 message publication instance.
 *
 * @author Christophe Bedard
 */
public class Ros2PubInstance extends Ros2Instance<@NonNull Ros2ObjectHandle> {

    /** Custom type value ID for this object */
    public static final byte CUSTOM_TYPE_ID = 65;
    /** CustomStateValueFactory for this object */
    @SuppressWarnings("restriction")
    public static final @NonNull CustomStateValueFactory ROS2_PUB_INSTANCE_VALUE_FACTORY = Ros2PubInstance::read;

    private final long fTid;
    private final @NonNull HostProcessPointer fMessage;
    private final long fSourceTimestamp;

    /**
     * Constructor
     *
     * @param publisherHandle
     *            the publisher handle
     * @param tid
     *            the TID
     * @param message
     *            the message pointer
     * @param sourceTimestamp
     *            the source timestamp
     */
    public Ros2PubInstance(@NonNull Ros2ObjectHandle publisherHandle, long tid, @NonNull HostProcessPointer message, long sourceTimestamp) {
        super(publisherHandle);
        fTid = tid;
        fMessage = message;
        fSourceTimestamp = sourceTimestamp;
    }

    /**
     * @return the publisher handle
     */
    public @NonNull Ros2ObjectHandle getPublisherHandle() {
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

    @Override
    public int hashCode() {
        return Objects.hashCode(fTid, fMessage, fSourceTimestamp, super.hashCode());
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
        Ros2PubInstance o = (Ros2PubInstance) obj;
        return fTid == o.fTid && fMessage.equals(o.fMessage) && fSourceTimestamp == o.fSourceTimestamp;
    }

    @Override
    public String toString() {
        return String.format("Ros2PubInstance: publisherHandle=%s, tid=%d, message=[%s], sourceTimestamp=%d", super.toString(), fTid, fMessage.toString(), fSourceTimestamp); //$NON-NLS-1$
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
    }

    @Override
    protected int getSerializedValueSize() {
        int size = 0;
        size += super.getSerializedValueSize();
        size += Long.BYTES;
        size += fMessage.getSerializedValueSize();
        size += Long.BYTES;
        return size;
    }

    /**
     * @param buffer
     *            the buffer
     * @return the value
     */
    public static @NonNull Ros2PubInstance read(ISafeByteBufferReader buffer) {
        Ros2ObjectHandle publisherHandle = Ros2ObjectHandle.read(buffer);
        long tid = buffer.getLong();
        HostProcessPointer message = HostProcessPointer.read(buffer);
        long sourceTimestamp = buffer.getLong();
        return new Ros2PubInstance(publisherHandle, tid, message, sourceTimestamp);
    }
}
