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

package org.eclipse.tracecompass.incubator.internal.ros2.core.model.messages;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcessPointer;
import org.eclipse.tracecompass.internal.provisional.statesystem.core.statevalue.CustomStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;

/**
 * ROS 2-related instance or time-based event.
 *
 * @author Christophe Bedard
 * @param <T>
 *            the type of the object from which the instance originates
 */
@SuppressWarnings("restriction")
public abstract class Ros2Instance<@NonNull T extends HostProcessPointer> extends CustomStateValue {

    private final @NonNull T fOriginHandle;

    /**
     * Constructor
     *
     * @param originHandle
     *            the origin handle
     */
    protected Ros2Instance(@NonNull T originHandle) {
        fOriginHandle = originHandle;
    }

    /**
     * @return the handle of the origin object
     */
    protected @NonNull T getOriginHandle() {
        return fOriginHandle;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fOriginHandle);
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
        Ros2Instance<T> o = (Ros2Instance<T>) obj;
        return o.fOriginHandle.equals(fOriginHandle);
    }

    @Override
    public String toString() {
        return String.format("[%s]", fOriginHandle.toString()); //$NON-NLS-1$
    }

    @Override
    public int compareTo(@NonNull ITmfStateValue o) {
        return this.equals(o) ? 0 : -1;
    }

    @Override
    protected void serializeValue(ISafeByteBufferWriter buffer) {
        fOriginHandle.serializeValue(buffer);
    }

    @Override
    protected int getSerializedValueSize() {
        return fOriginHandle.getSerializedValueSize();
    }
}
