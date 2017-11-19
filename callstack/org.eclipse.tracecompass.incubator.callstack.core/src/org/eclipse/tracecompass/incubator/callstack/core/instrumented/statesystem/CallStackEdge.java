/*******************************************************************************
 * Copyright (c) 2013, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.datastore.core.interval.IHTIntervalReader;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.datastore.core.serialization.SafeByteBufferFactory;
import org.eclipse.tracecompass.segmentstore.core.ISegment;

/**
 * A model element to make a time link
 *
 * @author Matthew Khouzam
 */
public final class CallStackEdge implements ISegment {

    /**
     * ID
     */
    private static final long serialVersionUID = -224193581961404619L;
    /**
     * Size taken on a disk
     */
    public static final int BASE_SIZE = Integer.BYTES * 3 + Long.BYTES * 2;

    /**
     * Factory helper
     */
    public static final IHTIntervalReader<CallStackEdge> READER = bb -> {
        int srcTid = bb.getInt();
        String srcHost = bb.getString();
        int dstTid = bb.getInt();
        String dstHost = bb.getString();
        long start = bb.getLong();
        long duration = bb.getLong();
        int id = bb.getInt();
        return new CallStackEdge(new HostThread(srcHost, srcTid), new HostThread(dstHost, dstTid), start, duration, id);
    };

    private final transient HostThread fSrc;
    private final transient HostThread fDst;
    private final long fStart;
    private final long fDuration;
    private final int fId;

    /**
     * Constructor
     *
     * @param src
     *            source {@link HostThread}
     * @param dst
     *            destination {@link HostThread}
     * @param time
     *            start time
     * @param duration
     *            duration
     * @param id
     *            the unique id of this span
     */
    public CallStackEdge(HostThread src, HostThread dst, long time, long duration, int id) {
        fSrc = src;
        fDst = dst;
        fStart = time;
        fDuration = duration;
        fId = id;
    }

    /**
     * Get source host
     *
     * @return source host
     */
    public String getSrcHost() {
        return fSrc.getHost();
    }

    /**
     * Get destination host
     *
     * @return destination host
     */
    public String getDstHost() {
        return fDst.getHost();
    }

    /**
     * Source TID
     *
     * @return source
     */
    public int getSrcTid() {
        return fSrc.getTid();
    }

    /**
     * Destination TID
     *
     * @return destination
     */
    public int getDstTid() {
        return fDst.getTid();
    }

    @Override
    public long getStart() {
        return fStart;
    }

    @Override
    public long getEnd() {
        return fStart + fDuration;
    }

    /**
     * Get the duration of the vertex
     *
     * @return gets the duration of the legend
     */
    public long getDuration() {
        return fDuration;
    }

    /**
     * Get the span ID of the edge
     *
     * @return the span id
     */
    public int getId() {
        return fId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fSrc, fDst, fStart, fDuration, fId);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CallStackEdge other = (CallStackEdge) obj;
        return (fDuration == other.fDuration) &&
                (fStart == other.fStart) &&
                Objects.equals(fSrc, other.fSrc) &&
                Objects.equals(fDst, other.fDst) &&
                fId == other.fId;
    }

    @Override
    public void writeSegment(@NonNull ISafeByteBufferWriter bb) {
        bb.putInt(getSrcTid());
        bb.putString(getSrcHost());
        bb.putInt(getDstTid());
        bb.putString(getDstHost());
        bb.putLong(getStart());
        bb.putLong(getDuration());
        bb.putInt(fId);
    }

    @Override
    public int getSizeOnDisk() {
        return BASE_SIZE + SafeByteBufferFactory.getStringSizeInBuffer(fSrc.getHost()) + SafeByteBufferFactory.getStringSizeInBuffer(fDst.getHost());
    }
}
