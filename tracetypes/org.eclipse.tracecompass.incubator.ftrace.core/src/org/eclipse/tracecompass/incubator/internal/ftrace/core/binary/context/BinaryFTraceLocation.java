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

import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLocation;

/**
 * A TmfLocation implementation for (paged) Binary FTrace dat file.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceLocation extends TmfLocation {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------
    /**
     * An invalid location
     */
    public static final ITmfLocation INVALID_LOCATION = new BinaryFTraceLocation(-1, -1) {
        @Override
        public String toString() {
            return "[INVALID]"; //$NON-NLS-1$
        }
    };

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Basic constructor for BinaryFTraceLocation. Uses a default index of 0.
     *
     * @param timestamp
     *            The timestamp of this location
     */
    public BinaryFTraceLocation(final ITmfTimestamp timestamp) {
        this(timestamp.toNanos(), 0);
    }

    /**
     * Constructor using timestamp object and index
     *
     * @param timestamp
     *            The timestamp of this location
     * @param index
     *            The index of this location for this timestamp
     */
    public BinaryFTraceLocation(final ITmfTimestamp timestamp, long index) {
        this(timestamp.toNanos(), index);
    }

    /**
     * Constructor using a long value for the timestamp, and an index
     *
     * @param timestampValue
     *            The new timestamp
     * @param index
     *            The new index
     */
    public BinaryFTraceLocation(final long timestampValue, final long index) {
        super(new BinaryFTraceLocationInfo(timestampValue, index));
    }

    /**
     * Constructor using a pre-made locationInfo object
     *
     * @param locationInfo
     *            The locationInfo object to use
     */
    public BinaryFTraceLocation(BinaryFTraceLocationInfo locationInfo) {
        super(locationInfo);
    }

    /**
     * Copy constructor
     *
     * @param location
     *            Other location to copy
     */
    public BinaryFTraceLocation(final ITmfLocation location) {
        super((TmfLocation) location);
    }

    // ------------------------------------------------------------------------
    // TmfLocation
    // ------------------------------------------------------------------------

    /**
     * Construct the location from the ByteBuffer.
     *
     * @param bufferIn
     *            the buffer to read from
     */
    public BinaryFTraceLocation(ByteBuffer bufferIn) {
        super(new BinaryFTraceLocationInfo(bufferIn));
    }

    @Override
    public BinaryFTraceLocationInfo getLocationInfo() {
        return (BinaryFTraceLocationInfo) super.getLocationInfo();
    }

    // ------------------------------------------------------------------------
    // Object
    // ------------------------------------------------------------------------
    /**
     * Constructs the location from the ByteBuffer. This typically happens when
     * reading from disk.
     */
    @Override
    public void serialize(ByteBuffer bufferOut) {
        getLocationInfo().serialize(bufferOut);
    }
}
