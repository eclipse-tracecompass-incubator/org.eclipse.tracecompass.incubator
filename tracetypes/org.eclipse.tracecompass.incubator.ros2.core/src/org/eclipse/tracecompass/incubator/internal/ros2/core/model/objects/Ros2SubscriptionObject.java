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
 * Container for ROS 2 subscription object.
 *
 * @author Christophe Bedard
 */
public class Ros2SubscriptionObject extends Ros2PubSubObject {

    /** Custom type value ID for this object */
    public static final byte CUSTOM_TYPE_ID = 62;
    /** CustomStateValueFactory for this object */
    @SuppressWarnings("restriction")
    public static final @NonNull CustomStateValueFactory ROS2_SUBSCRIPTION_OBJECT_VALUE_FACTORY = b -> Ros2SubscriptionObject.read(b);

    private final @NonNull HostProcessPointer fSubscription;
    private final @NonNull HostProcessPointer fCallback;
    private final int fSerializedValueSize;

    /**
     * Constructor
     *
     * TODO: reduce number of parameters to max 7
     *
     * @param subscriptionHandle
     *            the subscription handle
     * @param rmwSubscriptionHandle
     *            the rmw subscription handle
     * @param subscription
     *            the rcl subscription handle
     * @param callback
     *            the corresponding callback object
     * @param nodeHandle
     *            the node handle
     * @param topicName
     *            the topic name
     * @param gid
     *            the DDS GID
     * @param ddsReader
     *            the DDS reader
     */
    public Ros2SubscriptionObject(
            @NonNull Ros2ObjectHandle subscriptionHandle, @NonNull Ros2ObjectHandle rmwSubscriptionHandle, @NonNull String topicName, @NonNull Ros2ObjectHandle nodeHandle, @NonNull Gid gid,
            @NonNull HostProcessPointer ddsReader, @NonNull HostProcessPointer subscription, @NonNull HostProcessPointer callback) {
        super(subscriptionHandle, rmwSubscriptionHandle, topicName, nodeHandle, gid, ddsReader);
        fSubscription = subscription;
        fCallback = callback;

        int size = 0;
        size += super.getSerializedValueSize();
        size += fSubscription.getSerializedValueSize();
        size += fCallback.getSerializedValueSize();
        fSerializedValueSize = size;
    }

    /**
     * @return the subscription
     */
    public @NonNull HostProcessPointer getSubscription() {
        return fSubscription;
    }

    /**
     * @return the callback
     */
    public @NonNull HostProcessPointer getCallback() {
        return fCallback;
    }

    /**
     * @return the DDS reader
     */
    public @NonNull HostProcessPointer getDdsReader() {
        return super.getDdsHandle();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fSubscription, fCallback, super.hashCode());
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
        Ros2SubscriptionObject o = (Ros2SubscriptionObject) obj;
        return o.fSubscription.equals(fSubscription) && o.fCallback.equals(fCallback);
    }

    @Override
    public String toString() {
        return String.format(
                "Ros2SubscriptionObject: %s, subscription=[%s], callback=[%s]", //$NON-NLS-1$
                super.toString(), fSubscription.toString(), fCallback.toString());
    }

    @Override
    protected @NonNull Byte getCustomTypeId() {
        return CUSTOM_TYPE_ID;
    }

    @Override
    protected void serializeValue(@NonNull ISafeByteBufferWriter buffer) {
        super.serializeValue(buffer);
        fSubscription.serializeValue(buffer);
        fCallback.serializeValue(buffer);
    }

    @Override
    protected int getSerializedValueSize() {
        return fSerializedValueSize;
    }

    /**
     * @param buffer
     *            the buffer
     * @return the value
     */
    public static @NonNull Ros2SubscriptionObject read(ISafeByteBufferReader buffer) {
        Ros2ObjectHandle subscriptionHandle = Ros2ObjectHandle.read(buffer);
        Ros2ObjectHandle rmwPublisherHandle = Ros2ObjectHandle.read(buffer);
        String topicName = buffer.getString();
        Ros2ObjectHandle nodeHandle = Ros2ObjectHandle.read(buffer);
        Gid gid = Gid.read(buffer);
        HostProcessPointer ddsReader = HostProcessPointer.read(buffer);
        HostProcessPointer subscription = HostProcessPointer.read(buffer);
        HostProcessPointer callback = HostProcessPointer.read(buffer);
        return new Ros2SubscriptionObject(subscriptionHandle, rmwPublisherHandle, topicName, nodeHandle, gid, ddsReader, subscription, callback);
    }
}
