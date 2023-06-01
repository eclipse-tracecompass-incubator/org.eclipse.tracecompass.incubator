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
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcessValue;
import org.eclipse.tracecompass.internal.provisional.statesystem.core.statevalue.CustomStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;

import com.google.common.base.Objects;

/**
 * Abstract ROS 2 object. Has a handle, which is some form of pointer.
 *
 * @author Christophe Bedard
 * @param <T>
 *            the type of the object's handle object
 */
@SuppressWarnings("restriction")
public abstract class Ros2Object<@NonNull T extends HostProcessValue<?>> extends CustomStateValue {

    private final @NonNull T fHandle;

    /**
     * Constructor
     *
     * @param handle
     *            the handle
     */
    protected Ros2Object(@NonNull T handle) {
        fHandle = handle;
    }

    /**
     * @return the handle
     */
    public @NonNull T getHandle() {
        return fHandle;
    }

    /**
     * @return the string ID to identify this object in a state system
     */
    public @NonNull String getStringId() {
        return getHandle().getStringId();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fHandle);
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
        @SuppressWarnings("unchecked")
        Ros2Object<T> o = (Ros2Object<T>) obj;
        return o.fHandle.equals(fHandle);
    }

    @Override
    public String toString() {
        return String.format("[%s]", fHandle.toString()); //$NON-NLS-1$
    }

    @Override
    public int compareTo(@NonNull ITmfStateValue o) {
        return this.equals(o) ? 0 : -1;
    }

    @Override
    protected void serializeValue(ISafeByteBufferWriter buffer) {
        fHandle.serializeValue(buffer);
    }

    @Override
    protected int getSerializedValueSize() {
        return fHandle.getSerializedValueSize();
    }
}
