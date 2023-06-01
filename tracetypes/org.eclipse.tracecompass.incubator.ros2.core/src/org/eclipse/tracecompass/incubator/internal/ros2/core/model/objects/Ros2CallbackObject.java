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
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostProcessPointer;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.Ros2SerializationUtil;

import com.google.common.base.Objects;

/**
 * Container for ROS 2 callback object.
 *
 * @author Christophe Bedard
 */
public class Ros2CallbackObject extends Ros2Object<@NonNull HostProcessPointer> {

    /** Custom type value ID for this object */
    public static final byte CUSTOM_TYPE_ID = 64;
    /** CustomStateValueFactory for this object */
    @SuppressWarnings("restriction")
    public static final @NonNull CustomStateValueFactory ROS2_CALLBACK_OBJECT_VALUE_FACTORY = b -> Ros2CallbackObject.read(b);

    private final @NonNull Ros2ObjectHandle fOwnerHandle;
    private final @NonNull String fSymbol;
    private final Ros2CallbackType fType;

    /**
     * Constructor
     *
     * @param callback
     *            the callback object pointer
     * @param ownerHandle
     *            the callback owner handle
     * @param symbol
     *            the callback symbol
     * @param type
     *            the callback type
     */
    public Ros2CallbackObject(@NonNull HostProcessPointer callback, @NonNull Ros2ObjectHandle ownerHandle, @NonNull String symbol, Ros2CallbackType type) {
        super(callback);
        fOwnerHandle = ownerHandle;
        fSymbol = symbol;
        fType = type;
    }

    /**
     * @return the callback
     */
    public @NonNull HostProcessPointer getCallback() {
        return getHandle();
    }

    /**
     * @return the owner handle
     */
    public @NonNull Ros2ObjectHandle getOwnerHandle() {
        return fOwnerHandle;
    }

    /**
     * @return the symbol
     */
    public @NonNull String getSymbol() {
        return fSymbol;
    }

    /**
     * @return the callback type
     */
    public Ros2CallbackType getCallbackType() {
        return fType;
    }

    /**
     * @return the string ID to identify this object in a state system
     */
    @Override
    public String getStringId() {
        return getCallback().getStringId();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fOwnerHandle, fSymbol, fType, super.hashCode());
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
        Ros2CallbackObject o = (Ros2CallbackObject) obj;
        return o.fOwnerHandle.equals(fOwnerHandle) && o.fSymbol.equals(fSymbol) && o.fType.equals(fType);
    }

    @Override
    public String toString() {
        return String.format(
                "Ros2CallbackObject: %s, ownerHandle=[%s], symbol=%s, type=%s", //$NON-NLS-1$
                super.toString(), fOwnerHandle.toString(), fSymbol, fType.toString());
    }

    @Override
    protected @NonNull Byte getCustomTypeId() {
        return CUSTOM_TYPE_ID;
    }

    @Override
    protected void serializeValue(@NonNull ISafeByteBufferWriter buffer) {
        super.serializeValue(buffer);
        fOwnerHandle.serializeValue(buffer);
        buffer.putString(fSymbol);
        buffer.putInt(fType.ordinal());
    }

    @Override
    protected int getSerializedValueSize() {
        int size = 0;
        size += super.getSerializedValueSize();
        size += fOwnerHandle.getSerializedValueSize();
        size += Ros2SerializationUtil.getStringSerializedSize(fSymbol);
        size += Integer.BYTES;
        return size;
    }

    /**
     * @param buffer
     *            the buffer
     * @return the value
     */
    public static @NonNull Ros2CallbackObject read(ISafeByteBufferReader buffer) {
        HostProcessPointer callback = HostProcessPointer.read(buffer);
        Ros2ObjectHandle ownerHandle = Ros2ObjectHandle.read(buffer);
        String symbol = buffer.getString();
        Ros2CallbackType type = Ros2CallbackType.values()[buffer.getInt()];
        return new Ros2CallbackObject(callback, ownerHandle, symbol, type);
    }
}
