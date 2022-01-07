/*******************************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.context;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.Objects;

/**
 * An object that contains the timestamp and the rank of an event to locate the
 * event in the trace file.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceLocationInfo implements Comparable<BinaryFTraceLocationInfo> {
    private static final Comparator<BinaryFTraceLocationInfo> COMPARATOR = Comparator.comparing(BinaryFTraceLocationInfo::getTimestamp).thenComparing(BinaryFTraceLocationInfo::getIndex);

    private final long fTimestamp;
    private final long fIndex;

    /**
     * Construct a location from the provided timestamp and index.
     *
     * @param ts
     *            Timestamp
     * @param index
     *            Index of this event (if there are N elements with the same
     *            timestamp, which one is it.)
     */
    public BinaryFTraceLocationInfo(long ts, long index) {
        fTimestamp = ts;
        fIndex = index;
    }

    /**
     * Construct the location from the ByteBuffer.
     *
     * @param bufferIn
     *            The buffer to read from
     */
    public BinaryFTraceLocationInfo(ByteBuffer bufferIn) {
        fTimestamp = bufferIn.getLong();
        fIndex = bufferIn.getLong();
    }

    /**
     * Get the time stamp of the binary FTrace event at the current position.
     *
     * @return The time stamp of the trace event at the current position
     */
    public long getTimestamp() {
        return fTimestamp;
    }

    /**
     * Get the index of the binary FTrace event at the current position.
     *
     * @return The index of the trace event
     */
    public long getIndex() {
        return fIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIndex(), getTimestamp());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof BinaryFTraceLocationInfo)) {
            return false;
        }
        BinaryFTraceLocationInfo other = (BinaryFTraceLocationInfo) obj;
        if (getIndex() != other.getIndex()) {
            return false;
        }
        return (getTimestamp() == other.getTimestamp());
    }

    @Override
    public String toString() {
        return "Element [" + fTimestamp + '/' + fIndex + ']'; //$NON-NLS-1$
    }

    @Override
    public int compareTo(BinaryFTraceLocationInfo other) {
        return COMPARATOR.compare(this, other);
    }

    /**
     * Write the location to the ByteBuffer so that it can be saved to disk.
     *
     * @param bufferOut
     *            the buffer to write to
     */
    public void serialize(ByteBuffer bufferOut) {
        bufferOut.putLong(fTimestamp);
        bufferOut.putLong(fIndex);
    }
}
