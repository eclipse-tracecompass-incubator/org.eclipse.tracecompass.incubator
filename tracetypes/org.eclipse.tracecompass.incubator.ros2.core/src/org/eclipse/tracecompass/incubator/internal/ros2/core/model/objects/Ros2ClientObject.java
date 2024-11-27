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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferReader;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostInfo;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcess;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcessPointer;

/**
 * Container for ROS 2 client object (i.e., service client).
 *
 * @author Christophe Bedard
 */
public class Ros2ClientObject extends Ros2PubSubObject {

    /** Custom type value ID for this object */
    public static final byte CUSTOM_TYPE_ID = 75;
    /** CustomStateValueFactory for this object */
    @SuppressWarnings("restriction")
    public static final @NonNull CustomStateValueFactory ROS2_CLIENT_OBJECT_VALUE_FACTORY = b -> Ros2ClientObject.read(b);

    // We do not currently collect a DDS pointer for clients
    private static final @NonNull HostProcessPointer HOST_PROCESS_POINTER_UNUSED = new HostProcessPointer(new HostProcess(new HostInfo(StringUtils.EMPTY, StringUtils.EMPTY), 0L), 0L);

    /**
     * Constructor
     *
     * @param clientHandle
     *            the client handle
     * @param rmwClientHandle
     *            the rmw client handle
     * @param nodeHandle
     *            the node handle
     * @param serviceName
     *            the service name
     * @param gid
     *            the rmw GID
     */
    public Ros2ClientObject(@NonNull Ros2ObjectHandle clientHandle, @NonNull Ros2ObjectHandle rmwClientHandle, @NonNull String serviceName, @NonNull Ros2ObjectHandle nodeHandle, @NonNull Gid gid) {
        super(clientHandle, rmwClientHandle, serviceName, nodeHandle, gid, HOST_PROCESS_POINTER_UNUSED);
    }

    @Override
    public String toString() {
        return String.format("Ros2ClientObject: %s", super.toString()); //$NON-NLS-1$
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
    public static @NonNull Ros2ClientObject read(ISafeByteBufferReader buffer) {
        Ros2ObjectHandle clientHandle = Ros2ObjectHandle.read(buffer);
        Ros2ObjectHandle rmwClientHandle = Ros2ObjectHandle.read(buffer);
        String serviceName = buffer.getString();
        Ros2ObjectHandle nodeHandle = Ros2ObjectHandle.read(buffer);
        Gid gid = Gid.read(buffer);
        // It's unused, but make sure to read the whole buffer
        HostProcessPointer.read(buffer);
        return new Ros2ClientObject(clientHandle, rmwClientHandle, serviceName, nodeHandle, gid);
    }
}
