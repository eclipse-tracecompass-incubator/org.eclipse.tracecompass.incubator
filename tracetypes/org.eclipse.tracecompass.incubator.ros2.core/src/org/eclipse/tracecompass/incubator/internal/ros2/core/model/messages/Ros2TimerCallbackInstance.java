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
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferReader;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;

/**
 * Container for a ROS 2 timer callback instance. Combination of a timer handle
 * and a callback instance.
 *
 * @author Christophe Bedard
 */
public class Ros2TimerCallbackInstance extends Ros2Instance<@NonNull Ros2ObjectHandle> {

    /** Custom type value ID for this object */
    public static final byte CUSTOM_TYPE_ID = 69;
    /** CustomStateValueFactory for this object */
    @SuppressWarnings("restriction")
    public static final @NonNull CustomStateValueFactory ROS2_TIMER_CALLBACK_INSTANCE_VALUE_FACTORY = Ros2TimerCallbackInstance::read;

    private final @NonNull Ros2CallbackInstance fCallbackInstance;

    /**
     * Constructor
     *
     * @param timerHandle
     *            the timer handle
     * @param callbackInstance
     *            the callback instance
     */
    public Ros2TimerCallbackInstance(@NonNull Ros2ObjectHandle timerHandle, @NonNull Ros2CallbackInstance callbackInstance) {
        super(timerHandle);
        fCallbackInstance = callbackInstance;
    }

    /**
     * @return the timer handle
     */
    public @NonNull Ros2ObjectHandle getTimerHandle() {
        return getOriginHandle();
    }

    /**
     * @return the callback instance
     */
    public @NonNull Ros2CallbackInstance getCallbackInstance() {
        return fCallbackInstance;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fCallbackInstance, super.hashCode());
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
        Ros2TimerCallbackInstance o = (Ros2TimerCallbackInstance) obj;
        return fCallbackInstance.equals(o.fCallbackInstance);
    }

    @Override
    public String toString() {
        return String.format("Ros2TimerCallbackInstance: timerHandle=%s, callbackInstance=[%s]", super.toString(), fCallbackInstance.toString()); //$NON-NLS-1$
    }

    @Override
    protected @NonNull Byte getCustomTypeId() {
        return CUSTOM_TYPE_ID;
    }

    @Override
    protected void serializeValue(@NonNull ISafeByteBufferWriter buffer) {
        super.serializeValue(buffer);
        fCallbackInstance.serializeObject(buffer);
    }

    @Override
    protected int getSerializedValueSize() {
        int size = 0;
        size += super.getSerializedValueSize();
        size += fCallbackInstance.getSerializedValueSize();
        return size;
    }

    /**
     * @param buffer
     *            the buffer
     * @return the value
     */
    public static @NonNull Ros2TimerCallbackInstance read(ISafeByteBufferReader buffer) {
        Ros2ObjectHandle timerHandle = Ros2ObjectHandle.read(buffer);
        Ros2CallbackInstance callbackInstance = Ros2CallbackInstance.read(buffer);
        return new Ros2TimerCallbackInstance(timerHandle, callbackInstance);
    }
}
