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
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcessPointer;

/**
 * Container for ROS 2 publisher object.
 *
 * @author Christophe Bedard
 */
public class Ros2PublisherObject extends Ros2PubSubObject {

    /** Custom type value ID for this object */
    public static final byte CUSTOM_TYPE_ID = 61;
    /** CustomStateValueFactory for this object */
    @SuppressWarnings("restriction")
    public static final @NonNull CustomStateValueFactory ROS2_PUBLISHER_OBJECT_VALUE_FACTORY = b -> Ros2PublisherObject.read(b);

    /**
     * Constructor
     *
     * @param publisherHandle
     *            the publisher handle
     * @param rmwPublisherHandle
     *            the rmw publisher handle
     * @param nodeHandle
     *            the node handle
     * @param topicName
     *            the topic name
     * @param gid
     *            the DDS GID
     * @param ddsWriter
     *            the DDS writer
     */
    public Ros2PublisherObject(@NonNull Ros2ObjectHandle publisherHandle, @NonNull Ros2ObjectHandle rmwPublisherHandle, @NonNull String topicName, @NonNull Ros2ObjectHandle nodeHandle, @NonNull Gid gid, @NonNull HostProcessPointer ddsWriter) {
        super(publisherHandle, rmwPublisherHandle, topicName, nodeHandle, gid, ddsWriter);
    }

    /**
     * @return the DDS writer
     */
    public @NonNull HostProcessPointer getDdsWriter() {
        return super.getDdsHandle();
    }

    @Override
    public String toString() {
        return String.format("Ros2PublisherObject: %s", super.toString()); //$NON-NLS-1$
    }

    @Override
    protected @NonNull Byte getCustomTypeId() {
        return CUSTOM_TYPE_ID;
    }

    /**
     * @param buffer
     *            the buffer
     * @return the value
     */
    public static @NonNull Ros2PublisherObject read(ISafeByteBufferReader buffer) {
        Ros2ObjectHandle publisherHandle = Ros2ObjectHandle.read(buffer);
        Ros2ObjectHandle rmwPublisherHandle = Ros2ObjectHandle.read(buffer);
        String topicName = buffer.getString();
        Ros2ObjectHandle nodeHandle = Ros2ObjectHandle.read(buffer);
        Gid gid = Gid.read(buffer);
        HostProcessPointer ddsWriter = HostProcessPointer.read(buffer);
        return new Ros2PublisherObject(publisherHandle, rmwPublisherHandle, topicName, nodeHandle, gid, ddsWriter);
    }
}
