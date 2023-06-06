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
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcess;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcessPointer;

/**
 * Represents a unique ID for a ROS 2 object.
 *
 * In practice, handles are pointers, so we need to avoid collisions between
 * processes and hosts. A Ros2ObjectHandle is essentially a HostProcessPointer,
 * but the distinction is due to a Ros2ObjectHandle being slightly more
 * important than just a pointer, since it represents and uniquely identifies a
 * ROS 2 object.
 *
 * @see HostProcessPointer
 *
 * @author Christophe Bedard
 */
public class Ros2ObjectHandle extends HostProcessPointer {

    /** Custom type value ID for this object */
    public static final byte CUSTOM_TYPE_ID_ROS2_OBJECT_HANDLE = 74;
    /** CustomStateValueFactory for this object */
    @SuppressWarnings("restriction")
    public static final @NonNull CustomStateValueFactory ROS2_OBJECT_HANDLE_VALUE_FACTORY = b -> Ros2ObjectHandle.read(b);

    /**
     * Constructor
     *
     * @param hostProcess
     *            the host process object
     * @param handle
     *            the handle
     */
    public Ros2ObjectHandle(@NonNull HostProcess hostProcess, @NonNull Long handle) {
        super(hostProcess, handle);
    }

    /**
     * @return the handle
     */
    public @NonNull Long getHandle() {
        return super.getPointer();
    }

    @Override
    public @NonNull String toString() {
        return String.format(
                "Ros2ObjectHandle: handle=0x%s, pid=%d, hostId=[%s]", //$NON-NLS-1$
                Long.toHexString(getHandle()), getPid(), getHostProcess().getHostId().toString());
    }

    @Override
    protected @NonNull Byte getCustomTypeId() {
        return CUSTOM_TYPE_ID_ROS2_OBJECT_HANDLE;
    }

    /**
     * @param buffer
     *            the buffer
     * @return the value
     */
    public static final @NonNull Ros2ObjectHandle read(ISafeByteBufferReader buffer) {
        HostProcess hostProcess = HostProcess.read(buffer);
        Long pointer = buffer.getLong();
        return new Ros2ObjectHandle(hostProcess, pointer);
    }
}
