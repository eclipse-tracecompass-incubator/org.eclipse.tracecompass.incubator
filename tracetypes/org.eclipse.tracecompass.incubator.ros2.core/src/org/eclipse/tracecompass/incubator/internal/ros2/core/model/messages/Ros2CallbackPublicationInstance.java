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
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2CallbackType;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;
import org.eclipse.tracecompass.internal.provisional.statesystem.core.statevalue.CustomStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;

/**
 * Callback-publication instance, linking a callback that is publishing a
 * message (subscription/timer) to the publisher that is publishing the message.
 *
 * @author Christophe Bedard
 */
@SuppressWarnings("restriction")
public class Ros2CallbackPublicationInstance extends CustomStateValue {

    /** Custom type value ID for this object */
    public static final byte CUSTOM_TYPE_ID = 71;
    /** CustomStateValueFactory for this object */
    public static final @NonNull CustomStateValueFactory ROS2_CALLBACK_PUBLICATION_INSTANCE_VALUE_FACTORY = Ros2CallbackPublicationInstance::read;

    private final @NonNull Ros2ObjectHandle fCallbackOwnerHandle;
    private final @NonNull Ros2ObjectHandle fPublisherHandle;
    private final long fPublicationTimestamp;
    private final @NonNull Ros2CallbackType fCallbackType;

    /**
     * Constructor
     *
     * @param callbackOwnerHandle
     *            the callback owner handle
     * @param publisherHandle
     *            the publisher handle
     * @param publicationTimestamp
     *            the publication timestamp
     * @param callbackType
     *            the callback type
     */
    public Ros2CallbackPublicationInstance(@NonNull Ros2ObjectHandle callbackOwnerHandle, @NonNull Ros2ObjectHandle publisherHandle, long publicationTimestamp, @NonNull Ros2CallbackType callbackType) {
        fCallbackOwnerHandle = callbackOwnerHandle;
        fPublisherHandle = publisherHandle;
        fPublicationTimestamp = publicationTimestamp;
        fCallbackType = callbackType;
    }

    /**
     * @return the callback owner handle
     */
    public @NonNull Ros2ObjectHandle getCallbackOwnerHandle() {
        return fCallbackOwnerHandle;
    }

    /**
     * @return the publisher handle
     */
    public @NonNull Ros2ObjectHandle getPublisherHandle() {
        return fPublisherHandle;
    }

    /**
     * @return the publication timestamp
     */
    public long getPublicationTimestamp() {
        return fPublicationTimestamp;
    }

    /**
     * @return the callback type
     */
    public @NonNull Ros2CallbackType getCallbackType() {
        return fCallbackType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fCallbackOwnerHandle, fPublisherHandle, fPublicationTimestamp, fCallbackType);
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
        Ros2CallbackPublicationInstance o = (Ros2CallbackPublicationInstance) obj;
        return o.fCallbackOwnerHandle.equals(fCallbackOwnerHandle) && o.fPublisherHandle.equals(fPublisherHandle) && o.fPublicationTimestamp == fPublicationTimestamp && o.fCallbackType.equals(fCallbackType);
    }

    @Override
    public int compareTo(@NonNull ITmfStateValue o) {
        return this.equals(o) ? 0 : -1;
    }

    @Override
    public String toString() {
        return String.format(
                "Ros2CallbackPublicationInstance: callbackOwner=[%s], pub=[%s], publicationTimestamp=%s, callbackType=%s", //$NON-NLS-1$
                fCallbackOwnerHandle.toString(), fPublisherHandle.toString(), fPublicationTimestamp, fCallbackType.toString());
    }

    @Override
    protected @NonNull Byte getCustomTypeId() {
        return CUSTOM_TYPE_ID;
    }

    @Override
    protected void serializeValue(@NonNull ISafeByteBufferWriter buffer) {
        fCallbackOwnerHandle.serializeValue(buffer);
        fPublisherHandle.serializeValue(buffer);
        buffer.putLong(fPublicationTimestamp);
        buffer.putInt(fCallbackType.ordinal());
    }

    @Override
    protected int getSerializedValueSize() {
        int size = 0;
        size += fCallbackOwnerHandle.getSerializedValueSize();
        size += fPublisherHandle.getSerializedValueSize();
        size += Long.BYTES;
        size += Integer.BYTES;
        return size;
    }

    /**
     * @param buffer
     *            the buffer
     * @return the value
     */
    public static @NonNull Ros2CallbackPublicationInstance read(ISafeByteBufferReader buffer) {
        Ros2ObjectHandle callbackOwnerHandle = Ros2ObjectHandle.read(buffer);
        Ros2ObjectHandle publisherHandle = Ros2ObjectHandle.read(buffer);
        long publicationTimestamp = buffer.getLong();
        Ros2CallbackType callbackType = Ros2CallbackType.values()[buffer.getInt()];
        return new Ros2CallbackPublicationInstance(callbackOwnerHandle, publisherHandle, publicationTimestamp, callbackType);
    }
}
