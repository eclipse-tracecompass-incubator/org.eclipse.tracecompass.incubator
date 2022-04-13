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

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferReader;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;

/**
 * Custom GID wrapper, because <code>hashCode()</code> of two identical arrays
 * is not necessarily equal.
 *
 * GIDs are global-unique identifiers which are usually represented using an
 * array of bytes of a certain length. They can be used to uniquely identify
 * objects, like publishers and subscriptions. For the scope of the uniqueness
 * of the identifier, refer to the ROS 2 middleware interface (rmw) definition.
 *
 * @author Christophe Bedard
 */
public class Gid {

    private long[] fArray;

    /**
     * Constructor
     *
     * @param gid
     *            the GID array
     */
    public Gid(long[] gid) {
        fArray = gid;
    }

    /**
     * @return the array
     */
    public long[] array() {
        return fArray;
    }

    @Override
    public int hashCode() {
        // Do not use long[]#hashCode
        return Arrays.hashCode(fArray);
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
        Gid g = (Gid) obj;
        return Arrays.equals(fArray, g.array());
    }

    @Override
    public String toString() {
        return Arrays.toString(fArray);
    }

    /**
     * @param buffer
     *            the buffer
     */
    public void serialize(@NonNull ISafeByteBufferWriter buffer) {
        buffer.putInt(fArray.length);
        for (long item : fArray) {
            buffer.putLong(item);
        }
    }

    /**
     * @param buffer
     *            the buffer
     * @return the value
     */
    public static @NonNull Gid read(ISafeByteBufferReader buffer) {
        int len = buffer.getInt();
        long[] array = new long[len];
        for (int i = 0; i < len; i++) {
            array[i] = buffer.getLong();
        }
        return new Gid(array);
    }

    /**
     * @return the serialized size
     */
    public int getSerializedValueSize() {
        int size = 0;
        size += Integer.BYTES;
        size += fArray.length * Long.BYTES;
        return size;
    }
}
