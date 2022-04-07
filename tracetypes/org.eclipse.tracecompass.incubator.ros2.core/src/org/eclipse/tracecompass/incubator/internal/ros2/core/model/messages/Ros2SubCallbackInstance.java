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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferReader;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.objects.Ros2ObjectHandle;

import com.google.common.base.Objects;

/**
 * Container for a ROS 2 subscription callback instance. Combination of a
 * {@link Ros2TakeInstance} and a {@link Ros2CallbackInstance}.
 *
 * @author Christophe Bedard
 */
public class Ros2SubCallbackInstance extends Ros2Instance<@NonNull Ros2ObjectHandle> {

    /** Custom type value ID for this object */
    public static final byte CUSTOM_TYPE_ID = 68;
    /** CustomStateValueFactory for this object */
    @SuppressWarnings("restriction")
    public static final @NonNull CustomStateValueFactory ROS2_SUB_CALLBACK_INSTANCE_VALUE_FACTORY = Ros2SubCallbackInstance::read;

    private final @NonNull Ros2TakeInstance fTakeInstance;
    private final @NonNull Ros2CallbackInstance fCallbackInstance;

    /**
     * Constructor
     *
     * @param takeInstance
     *            the take instance
     * @param callbackInstance
     *            the callback instance
     */
    public Ros2SubCallbackInstance(@NonNull Ros2TakeInstance takeInstance, @NonNull Ros2CallbackInstance callbackInstance) {
        super(callbackInstance.getOwnerHandle());
        fTakeInstance = takeInstance;
        fCallbackInstance = callbackInstance;
    }

    /**
     * @return the subscription handle
     */
    public @NonNull Ros2ObjectHandle getSubscriptionHandle() {
        return getOriginHandle();
    }

    /**
     * @return the take instance
     */
    public @NonNull Ros2TakeInstance getTakeInstance() {
        return fTakeInstance;
    }

    /**
     * @return the callback instance
     */
    public @NonNull Ros2CallbackInstance getCallbackInstance() {
        return fCallbackInstance;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fTakeInstance, fCallbackInstance);
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
        Ros2SubCallbackInstance o = (Ros2SubCallbackInstance) obj;
        return fTakeInstance.equals(o.fTakeInstance) && fCallbackInstance.equals(o.fCallbackInstance);
    }

    @Override
    public String toString() {
        return String.format(
                "Ros2SubCallbackInstance: subscriptionHandle=%s, takeInstance=[%s], callbackInstance=[%s]", //$NON-NLS-1$
                super.toString(), fTakeInstance.toString(), fCallbackInstance.toString());
    }

    @Override
    protected @NonNull Byte getCustomTypeId() {
        return CUSTOM_TYPE_ID;
    }

    @Override
    protected void serializeValue(ISafeByteBufferWriter buffer) {
        /**
         * Don't serialize origin handle here since it's already included in the
         * underyling instances.
         */
        fTakeInstance.serializeObject(buffer);
        fCallbackInstance.serializeObject(buffer);
    }

    @Override
    protected int getSerializedValueSize() {
        int size = 0;
        size += fTakeInstance.getSerializedValueSize();
        size += fCallbackInstance.getSerializedValueSize();
        return size;
    }

    /**
     * @param buffer
     *            the buffer
     * @return the value
     */
    public static @NonNull Ros2SubCallbackInstance read(ISafeByteBufferReader buffer) {
        Ros2TakeInstance takeInstance = Ros2TakeInstance.read(buffer);
        Ros2CallbackInstance callbackInstance = Ros2CallbackInstance.read(buffer);
        return new Ros2SubCallbackInstance(takeInstance, callbackInstance);
    }
}
