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
import org.eclipse.jdt.annotation.Nullable;

/**
 * Data (.dat) event type. This is to be used in Tracecompass as an event field,
 * but encapsulates a full event from dat.
 *
 * @author Matthew Khouzam
 *
 */
@NonNullByDefault
public class DatEvent {

    /**
     * Magic number, always 5
     */
    private static final int UFTRACE_MAGIC_NUMBER = 5;
    /**
     * Type mask, first two bits
     */
    private static final long TYPE_MASK = (1L << 0) | (1L << 1);
    /**
     * Marker mask, third bit
     */
    private static final long MARKER_MASK = (1L << 2);
    /**
     * Magic number mask (4th, 5th and 6th bit)
     */
    private static final long MAGIC_MASK = (1L << 3) | (1L << 4) | (1L << 5);
    /**
     * Depth mask, 7th to 16th bit
     */
    private static final long DEPTH_MASK = ((1L << 10) - 1) << 6;
    /**
     * Address mask, the rest
     */
    private static final long ADDRESS_MASK = (1L << 48) - 1 << 16;

    private static final String[] TYPES = { "ENTRY", "EXIT", "EVENT", "LOST" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    private final long fTime;
    private final String fType;
    private final int fDepth;
    private final long fAddress;
    private final int fTid;

    /**
     * Builder for data events. The data must point to an event in the dat file.
     * The data format to read is described below.
     *
     * <pre>
     * ##################################################################
     * #                        64 bit time stamp                       #
     * ##################################################################
     * #TTM101DDDDDDDDAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA#
     * ##################################################################
     *
     * Where
     *
     * TT is a two bit type
     * 00 = entry
     * 01 = exit
     * 10 = event
     * 11 = lost
     *
     * M means more more data
     *
     * 101 is a magic number and will always be 101
     *
     * DDDDDDDDDD is the 10 bit depth field
     *
     * AAA... is a 48 bit address space
     * </pre>
     *
     * @param bb
     *            byte buffer containing the event
     * @param tid
     *            the thread id of the stream
     * @return an event
     */
    public static @Nullable DatEvent create(ByteBuffer bb, int tid) {
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
    public static @Nullable DatEvent create(long nanoseconds, long payload, int tid) {
        String type = TYPES[(int) (payload & TYPE_MASK)];
        if (type == null) {
            throw new IllegalStateException("Trace type cannot be null"); //$NON-NLS-1$
        }
        boolean moreData = (payload & MARKER_MASK) == MARKER_MASK;
        int magic = (int) ((payload & MAGIC_MASK) >>> 3);
        int depth = (int) ((payload & DEPTH_MASK) >>> 6);
        long address = (payload & ADDRESS_MASK) >>> 16;
        if (magic != UFTRACE_MAGIC_NUMBER) {
            return null;
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
