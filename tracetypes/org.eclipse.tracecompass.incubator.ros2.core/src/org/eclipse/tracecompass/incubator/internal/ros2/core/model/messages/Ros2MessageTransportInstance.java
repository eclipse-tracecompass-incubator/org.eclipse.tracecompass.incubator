/**********************************************************************
 * Copyright (c) 2023 Apex.AI, Inc.
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
import org.eclipse.tracecompass.internal.provisional.statesystem.core.statevalue.CustomStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;

/**
 * Transport instance, linking a message that was publisher on the publisher
 * side to the same message that was received by the subscription on the other
 * end.
 *
 * @author Christophe Bedard
 */
@SuppressWarnings("restriction")
public class Ros2MessageTransportInstance extends CustomStateValue {

    /** Custom type value ID for this object */
    public static final byte CUSTOM_TYPE_ID = 70;
    /** CustomStateValueFactory for this object */
    public static final @NonNull CustomStateValueFactory ROS2_MESSAGE_TRANSPORT_INSTANCE_VALUE_FACTORY = Ros2MessageTransportInstance::read;

    private final @NonNull Ros2ObjectHandle fPublisherHandle;
    private final @NonNull Ros2ObjectHandle fSubscriptionHandle;
    private final long fSourceTimestamp;
    private final long fDestinationTimestamp;

    /**
     * Constructor
     *
     * @param sourcePublisherHandle
     *            the source publisher handle
     * @param destinationSubscriptionHandle
     *            the destination subscription handle
     * @param sourcePublicationTimestamp
     *            the source publication timestmap
     * @param destinationTakeTimestamp
     *            the destination take timestamp
     */
    public Ros2MessageTransportInstance(@NonNull Ros2ObjectHandle sourcePublisherHandle, @NonNull Ros2ObjectHandle destinationSubscriptionHandle, long sourcePublicationTimestamp, long destinationTakeTimestamp) {
        fPublisherHandle = sourcePublisherHandle;
        fSubscriptionHandle = destinationSubscriptionHandle;
        fSourceTimestamp = sourcePublicationTimestamp;
        fDestinationTimestamp = destinationTakeTimestamp;
    }

    /**
     * @return the publisher handle
     */
    public @NonNull Ros2ObjectHandle getPublisherHandle() {
        return fPublisherHandle;
    }

    /**
     * @return the subscription handle
     */
    public @NonNull Ros2ObjectHandle getSubscriptionHandle() {
        return fSubscriptionHandle;
    }

    /**
     * @return the source timestamp
     */
    public long getSourceTimestamp() {
        return fSourceTimestamp;
    }

    /**
     * @return the destination timestamp
     */
    public long getDestinationTimestamp() {
        return fDestinationTimestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fPublisherHandle, fSubscriptionHandle, fSourceTimestamp, fDestinationTimestamp);
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
        Ros2MessageTransportInstance o = (Ros2MessageTransportInstance) obj;
        return o.fPublisherHandle.equals(fPublisherHandle) && o.fSubscriptionHandle.equals(fSubscriptionHandle) && o.fSourceTimestamp == fSourceTimestamp && o.fDestinationTimestamp == fDestinationTimestamp;
    }

    @Override
    public int compareTo(@NonNull ITmfStateValue o) {
        return this.equals(o) ? 0 : -1;
    }

    @Override
    public String toString() {
        return String.format(
                "Ros2MessageTransportInstance: pub=[%s], sub=[%s], sourceTimestamp=%d, destinationTimestamp=%d", //$NON-NLS-1$
                fPublisherHandle.toString(), fSubscriptionHandle.toString(), fSourceTimestamp, fDestinationTimestamp);
    }

    @Override
    protected @NonNull Byte getCustomTypeId() {
        return CUSTOM_TYPE_ID;
    }

    @Override
    protected void serializeValue(@NonNull ISafeByteBufferWriter buffer) {
        fPublisherHandle.serializeValue(buffer);
        fSubscriptionHandle.serializeValue(buffer);
        buffer.putLong(fSourceTimestamp);
        buffer.putLong(fDestinationTimestamp);
    }

    @Override
    protected int getSerializedValueSize() {
        int size = 0;
        size += fPublisherHandle.getSerializedValueSize();
        size += fSubscriptionHandle.getSerializedValueSize();
        size += Long.BYTES;
        size += Long.BYTES;
        return size;
    }

    /**
     * @param buffer
     *            the buffer
     * @return the value
     */
    public static @NonNull Ros2MessageTransportInstance read(ISafeByteBufferReader buffer) {
        Ros2ObjectHandle publisherHandle = Ros2ObjectHandle.read(buffer);
        Ros2ObjectHandle subscriptionHandle = Ros2ObjectHandle.read(buffer);
        long sourceTimestamp = buffer.getLong();
        long destinationTimestamp = buffer.getLong();
        return new Ros2MessageTransportInstance(publisherHandle, subscriptionHandle, sourceTimestamp, destinationTimestamp);
    }
}
