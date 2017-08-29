/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.uftrace.core.trace;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Data (.dat) event type
 *
 * @author Matthew Khouzam
 *
 */
@NonNullByDefault
public class DatEvent {

    private static final int UFTRACE_MAGIC_NUMBER = 0b101;
    private static final long TYPE_MASK = (1L << 0) | (1L << 1);
    private static final long MARKER_MASK = (1L << 2);
    private static final long MAGIC_MASK = (1L << 3) | (1L << 4) | (1L << 5);
    private static final long DEPTH_MASK = ((1L << 10) - 1) << 6;
    private static final long ADDRESS_MASK = (1L << 48) - 1 << 16;

    private static final String[] TYPES = { "ENTRY", "EXIT", "EVENT", "LOST" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    private final long fTime;
    private final String fType;
    private final int fDepth;
    private final long fAddress;
    private final int fTid;

    /**
     * Builder for data events
     *
     * @param bb
     *            byte buffer containing the event
     * @param tid
     *            the thread id of the stream
     * @return an event
     */
    public static DatEvent create(ByteBuffer bb, int tid) {
        bb.order(ByteOrder.LITTLE_ENDIAN);
        long time = bb.getLong();
        long payload = bb.getLong();
        return create(time, payload, tid);
    }

    /**
     * Builder for in memory data to make a data event
     *
     * @param nanoseconds
     *            the timestamp in UTC format
     * @param payload
     *            the data of the event
     * @param tid
     *            the tid this event comes from
     * @return an event
     */
    public static DatEvent create(long nanoseconds, long payload, int tid) {
        String type = TYPES[(int) (payload & TYPE_MASK)];
        if(type == null){
            throw new IllegalStateException("Trace type cannot be null"); //$NON-NLS-1$
        }
        boolean moreData = (payload & MARKER_MASK) == MARKER_MASK;
        int magic = (int) ((payload & MAGIC_MASK) >>> 3);
        int depth = (int) ((payload & DEPTH_MASK) >>> 6);
        long address = (payload & ADDRESS_MASK) >>> 16;
        if (magic != UFTRACE_MAGIC_NUMBER) {
            throw new IllegalStateException("Magic missmatch"); //$NON-NLS-1$
        }
        if (moreData) {
            // TODO: do something here
            // it seems undefined at the moment in the spec
        }
        return new DatEvent(nanoseconds, type, depth, address, tid);
    }

    private DatEvent(long nanoseconds, String type, int depth, long address, int tid) {
        fTime = nanoseconds;
        fType = type;
        fDepth = depth;
        fAddress = address;
        fTid = tid;
    }

    /**
     * Get the event time
     *
     * @return the time
     */
    public long getTime() {
        return fTime;
    }

    /**
     * Get the event type
     *
     * @return the type
     */
    public String getType() {
        return fType;
    }

    /**
     * Get the event call stack depth
     *
     * @return the depth
     */
    public int getDepth() {
        return fDepth;
    }

    /**
     * get the event address
     *
     * @return the address
     */
    public long getAddress() {
        return fAddress;
    }

    /**
     * Get the event TID
     *
     * @return the event TID
     */
    public int getTid() {
        return fTid;
    }

}
