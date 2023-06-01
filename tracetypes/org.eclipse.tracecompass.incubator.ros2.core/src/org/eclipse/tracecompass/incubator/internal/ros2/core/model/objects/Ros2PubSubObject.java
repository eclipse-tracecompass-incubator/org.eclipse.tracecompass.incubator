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
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcessPointer;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.Ros2SerializationUtil;

import com.google.common.base.Objects;

/**
 * Abstract ROS 2 pub/sub object. Has a topic name, a rmw handle, a node handle,
 * a DDS handle, and a GID.
 *
 * @author Christophe Bedard
 */
public abstract class Ros2PubSubObject extends Ros2Object<@NonNull Ros2ObjectHandle> {

    private final @NonNull Ros2ObjectHandle fRmwHandle;
    private final @NonNull String fTopicName;
    private final @NonNull Ros2ObjectHandle fNodeHandle;
    private final @NonNull Gid fGid;
    private final @NonNull HostProcessPointer fDdsHandle;

    /**
     * Constructor
     *
     * @param handle
     *            the handle
     * @param rmwHandle
     *            the rmw handle
     * @param topicName
     *            the topic name
     * @param nodeHandle
     *            the node handle
     * @param gid
     *            the DDS GID
     * @param ddsHandle
     *            the DDS handle (writer or reader)
     */
    protected Ros2PubSubObject(@NonNull Ros2ObjectHandle handle, @NonNull Ros2ObjectHandle rmwHandle, @NonNull String topicName, @NonNull Ros2ObjectHandle nodeHandle, @NonNull Gid gid, @NonNull HostProcessPointer ddsHandle) {
        super(handle);
        fRmwHandle = rmwHandle;
        fTopicName = topicName;
        fNodeHandle = nodeHandle;
        fGid = gid;
        fDdsHandle = ddsHandle;
    }

    /**
     * @return the rmw handle
     */
    public @NonNull Ros2ObjectHandle getRmwHandle() {
        return fRmwHandle;
    }

    /**
     * @return the topic name
     */
    public @NonNull String getTopicName() {
        return fTopicName;
    }

    /**
     * @return the node handle
     */
    public @NonNull Ros2ObjectHandle getNodeHandle() {
        return fNodeHandle;
    }

    /**
     * @return the GID
     */
    public @NonNull Gid getGid() {
        return fGid;
    }

    /**
     * @return the DDS handle (writer or reader)
     */
    public @NonNull HostProcessPointer getDdsHandle() {
        return fDdsHandle;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fRmwHandle, fTopicName, fNodeHandle, fGid, fDdsHandle, super.hashCode());
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
        Ros2PubSubObject o = (Ros2PubSubObject) obj;
        return o.fRmwHandle.equals(fRmwHandle) && o.fTopicName.equals(fTopicName) && o.fNodeHandle.equals(fNodeHandle) && o.fGid.equals(fGid) && o.fDdsHandle.equals(fDdsHandle);
    }

    @Override
    public String toString() {
        return String.format(
                "topicName=%s, %s, rmwHandle=[%s], nodeHandle=[%s], gid=%s, ddsHandle=[%s]", //$NON-NLS-1$
                fTopicName, super.toString(), fRmwHandle.toString(), fNodeHandle.toString(), fGid.toString(), fDdsHandle.toString());
    }

    @Override
    protected void serializeValue(ISafeByteBufferWriter buffer) {
        super.serializeValue(buffer);
        fRmwHandle.serializeValue(buffer);
        buffer.putString(fTopicName);
        fNodeHandle.serializeValue(buffer);
        fGid.serialize(buffer);
        fDdsHandle.serializeValue(buffer);
    }

    @Override
    protected int getSerializedValueSize() {
        int size = 0;
        size += super.getSerializedValueSize();
        size += fRmwHandle.getSerializedValueSize();
        size += Ros2SerializationUtil.getStringSerializedSize(fTopicName);
        size += fNodeHandle.getSerializedValueSize();
        size += fGid.getSerializedValueSize();
        size += fDdsHandle.getSerializedValueSize();
        return size;
    }
}
