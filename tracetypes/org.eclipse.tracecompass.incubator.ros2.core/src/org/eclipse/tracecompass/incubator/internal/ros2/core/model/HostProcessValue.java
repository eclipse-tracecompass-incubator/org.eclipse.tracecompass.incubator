/**********************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal, Apex.AI, Inc.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros2.core.model;

import java.util.Comparator;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.internal.provisional.statesystem.core.statevalue.CustomStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;

import com.google.common.base.Objects;

/**
 * Unique identifier that combines a value and a {@link HostProcess}, i.e., some
 * value + process ID + host ID.
 *
 * @author Christophe Bedard
 * @param <T>
 *            the type of the wrapped value
 */
@SuppressWarnings("restriction")
public abstract class HostProcessValue<@NonNull T extends Comparable<T>> extends CustomStateValue {

    private static Comparator<HostProcessValue<?>> COMPARATOR = Comparator.comparing((HostProcessValue<?> h) -> h.getHostProcess())
            .thenComparing((HostProcessValue<?> h) -> h.getValue());
    private static final @NonNull String STRING_ID_SEP = "|"; //$NON-NLS-1$

    private final @NonNull HostProcess fHostProcess;
    private final T fValue;
    private final int fSerializedValueSize;

    /**
     * Constructor
     *
     * @param hostProcess
     *            the host process object
     * @param value
     *            the value
     */
    public HostProcessValue(@NonNull HostProcess hostProcess, T value) {
        fHostProcess = hostProcess;
        fValue = value;

        // Concrete classes will compute the value's size properly
        fSerializedValueSize = fHostProcess.getSerializedValueSize();
    }

    /**
     * @return the host process
     */
    public @NonNull HostProcess getHostProcess() {
        return fHostProcess;
    }

    /**
     * @return the PID
     */
    public @NonNull Long getPid() {
        return fHostProcess.getPid();
    }

    /**
     * Concrete classes will provide their own appropriately-named getter
     * method.
     *
     * @return the value
     */
    protected T getValue() {
        return fValue;
    }

    /**
     * @return the value as a string
     */
    protected abstract @NonNull String valueToString();

    @Override
    public int compareTo(@NonNull ITmfStateValue o) {
        return COMPARATOR.compare(this, (HostProcessValue<?>) o);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fHostProcess, fValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        /**
         * Compare attributes directly; do not compare object classes. We want
         * two objects of different classes that extend HostProcessValue to be
         * equal if they have the same HostProcess and value.
         */
        HostProcessValue<?> o = (HostProcessValue<?>) obj;
        return o.fHostProcess.equals(fHostProcess) && o.fValue.equals(fValue);
    }

    /**
     * @return the string ID to uniquely represent this pointer
     */
    public @NonNull String getStringId() {
        return String.format(
                "%s%s%d%s%s", //$NON-NLS-1$
                valueToString(), STRING_ID_SEP, getPid(), STRING_ID_SEP, getHostProcess().getHostId().getId());
    }

    @Override
    public @NonNull String toString() {
        return String.format(
                "%s, pid=%d, hostId=[%s]", //$NON-NLS-1$
                valueToString(), getPid(), getHostProcess().getHostId().toString());
    }

    @Override
    public void serializeValue(@NonNull ISafeByteBufferWriter buffer) {
        fHostProcess.serializeValue(buffer);
        // Concrete classes will serialize the value properly
    }

    @Override
    public int getSerializedValueSize() {
        return fSerializedValueSize;
    }
}
