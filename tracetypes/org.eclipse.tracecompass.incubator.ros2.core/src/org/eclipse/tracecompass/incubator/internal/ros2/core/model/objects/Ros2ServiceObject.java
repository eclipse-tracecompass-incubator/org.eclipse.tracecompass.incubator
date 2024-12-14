/**********************************************************************
 * Copyright (c) 2024 Apex.AI, Inc.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferReader;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostInfo;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcess;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcessPointer;

/**
 * Container for ROS 2 service object (i.e., service server).
 *
 * @author Christophe Bedard
 */
public class Ros2ServiceObject extends Ros2PubSubObject {

    /** Custom type value ID for this object */
    public static final byte CUSTOM_TYPE_ID = 76;
    /** CustomStateValueFactory for this object */
    @SuppressWarnings("restriction")
    public static final @NonNull CustomStateValueFactory ROS2_SERVICE_OBJECT_VALUE_FACTORY = b -> Ros2ServiceObject.read(b);

    // We do not currently collect a DDS pointer for services
    private static final @NonNull HostProcessPointer HOST_PROCESS_POINTER_UNUSED = new HostProcessPointer(new HostProcess(new HostInfo(StringUtils.EMPTY, StringUtils.EMPTY), 0L), 0L);

    private final @NonNull HostProcessPointer fCallback;
    private final int fSerializedValueSize;

    /**
     * Constructor
     *
     * @param serviceHandle
     *            the service handle
     * @param rmwServiceHandle
     *            the rmw service handle
     * @param nodeHandle
     *            the node handle
     * @param serviceName
     *            the service name
     * @param callback
     *            the corresponding callback object
     */
    public Ros2ServiceObject(@NonNull Ros2ObjectHandle serviceHandle, @NonNull Ros2ObjectHandle rmwServiceHandle, @NonNull String serviceName, @NonNull Ros2ObjectHandle nodeHandle, @NonNull HostProcessPointer callback) {
        super(serviceHandle, rmwServiceHandle, serviceName, nodeHandle, new Gid(ArrayUtils.EMPTY_LONG_ARRAY), HOST_PROCESS_POINTER_UNUSED);
        fCallback = callback;

        int size = 0;
        size += super.getSerializedValueSize();
        size += fCallback.getSerializedValueSize();
        fSerializedValueSize = size;
    }

    @Override
    public String toString() {
        return String.format("Ros2ServiceObject: %s", super.toString()); //$NON-NLS-1$
    }

    @Override
    protected @NonNull Byte getCustomTypeId() {
        return CUSTOM_TYPE_ID;
    }

    @Override
    protected void serializeValue(@NonNull ISafeByteBufferWriter buffer) {
        super.serializeValue(buffer);
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
    public static @NonNull Ros2ServiceObject read(ISafeByteBufferReader buffer) {
        Ros2ObjectHandle serviceHandle = Ros2ObjectHandle.read(buffer);
        Ros2ObjectHandle rmwServiceHandle = Ros2ObjectHandle.read(buffer);
        String serviceName = buffer.getString();
        Ros2ObjectHandle nodeHandle = Ros2ObjectHandle.read(buffer);
        // GID and HostProcessPointer are unused, but make sure to read the whole buffer
        Gid.read(buffer);
        HostProcessPointer.read(buffer);
        HostProcessPointer callback = HostProcessPointer.read(buffer);
        return new Ros2ServiceObject(serviceHandle, rmwServiceHandle, serviceName, nodeHandle, callback);
    }
}
