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
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.Ros2SerializationUtil;

import com.google.common.base.Objects;

/**
 * Container for ROS 2 node object.
 *
 * @author Christophe Bedard
 */
public class Ros2NodeObject extends Ros2Object<@NonNull Ros2ObjectHandle> {

    /** Custom type value ID for this object */
    public static final byte CUSTOM_TYPE_ID = 60;
    /** CustomStateValueFactory for this object */
    @SuppressWarnings("restriction")
    public static final @NonNull CustomStateValueFactory ROS2_NODE_OBJECT_VALUE_FACTORY = b -> Ros2NodeObject.read(b);

    private final @NonNull String fNodeName;
    private final @NonNull String fTraceName;

    /**
     * Constructor
     *
     * @param nodeHandle
     *            the node handle
     * @param nodeName
     *            the node name, with full namespace
     * @param traceName
     *            the trace name
     */
    public Ros2NodeObject(@NonNull Ros2ObjectHandle nodeHandle, @NonNull String nodeName, @NonNull String traceName) {
        super(nodeHandle);
        fNodeName = nodeName;
        fTraceName = traceName;
    }

    /**
     * @return the node name
     */
    public String getNodeName() {
        return fNodeName;
    }

    /**
     * @return the trace name
     */
    public @NonNull String getTraceName() {
        return fTraceName;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fNodeName, fTraceName, super.hashCode());
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
        Ros2NodeObject o = (Ros2NodeObject) obj;
        return o.fNodeName == fNodeName && o.fTraceName.equals(fTraceName);
    }

    @Override
    public String toString() {
        return String.format(
                "Ros2NodeObject: nodeName=%s, %s, traceName=%s", //$NON-NLS-1$
                fNodeName, super.toString(), fTraceName);
    }

    @Override
    protected @NonNull Byte getCustomTypeId() {
        return CUSTOM_TYPE_ID;
    }

    @Override
    protected void serializeValue(ISafeByteBufferWriter buffer) {
        super.serializeValue(buffer);
        buffer.putString(fNodeName);
        buffer.putString(fTraceName);
    }

    @Override
    protected int getSerializedValueSize() {
        int size = 0;
        size += super.getSerializedValueSize();
        size += Ros2SerializationUtil.getStringSerializedSize(fNodeName);
        size += Ros2SerializationUtil.getStringSerializedSize(fTraceName);
        return size;
    }

    /**
     * @param buffer
     *            the buffer
     * @return the value
     */
    public static @NonNull Ros2NodeObject read(ISafeByteBufferReader buffer) {
        Ros2ObjectHandle nodeHandle = Ros2ObjectHandle.read(buffer);
        String nodeName = buffer.getString();
        String traceName = buffer.getString();
        return new Ros2NodeObject(nodeHandle, nodeName, traceName);
    }
}
