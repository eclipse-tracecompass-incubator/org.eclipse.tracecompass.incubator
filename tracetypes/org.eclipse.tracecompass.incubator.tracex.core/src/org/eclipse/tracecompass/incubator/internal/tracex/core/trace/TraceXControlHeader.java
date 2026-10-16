/*******************************************************************************
 * Copyright (c) 2026 Eclipse Trace Compass Incubator contributors
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.tracex.core.trace;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Immutable representation of the 32-bit TraceX control header.
 */
public final class TraceXControlHeader {

    /** Size of the documented control header. */
    public static final int SIZE = 48;

    private static final byte[] BIG_ENDIAN_MAGIC = { 0x54, 0x58, 0x54, 0x42 };
    private static final byte[] LITTLE_ENDIAN_MAGIC = { 0x42, 0x54, 0x58, 0x54 };

    private final ByteOrder fByteOrder;
    private final long fTimerValidMask;
    private final long fTraceBaseAddress;
    private final long fRegistryStartAddress;
    private final int fReserved1;
    private final int fRegistryNameSize;
    private final long fRegistryEndAddress;
    private final long fEventStartAddress;
    private final long fEventEndAddress;
    private final long fEventCurrentAddress;
    private final long fReserved2;
    private final long fReserved3;
    private final long fReserved4;

    private TraceXControlHeader(ByteOrder byteOrder, long timerValidMask,
            long traceBaseAddress, long registryStartAddress, int reserved1,
            int registryNameSize, long registryEndAddress, long eventStartAddress,
            long eventEndAddress, long eventCurrentAddress, long reserved2,
            long reserved3, long reserved4) {
        fByteOrder = byteOrder;
        fTimerValidMask = timerValidMask;
        fTraceBaseAddress = traceBaseAddress;
        fRegistryStartAddress = registryStartAddress;
        fReserved1 = reserved1;
        fRegistryNameSize = registryNameSize;
        fRegistryEndAddress = registryEndAddress;
        fEventStartAddress = eventStartAddress;
        fEventEndAddress = eventEndAddress;
        fEventCurrentAddress = eventCurrentAddress;
        fReserved2 = reserved2;
        fReserved3 = reserved3;
        fReserved4 = reserved4;
    }

    /**
     * Decode a control header after detecting byte order from its raw magic.
     *
     * @param bytes
     *            buffer positioned at the start of the file
     * @return decoded immutable header
     * @throws TraceXFormatException
     *             if the header is truncated or has unrecognized magic
     */
    public static TraceXControlHeader decode(ByteBuffer bytes) throws TraceXFormatException {
        ByteBuffer source = bytes.slice();
        int available = source.remaining();
        if (available < 4) {
            throw new TraceXFormatException("Truncated TraceX magic: expected 4 bytes, found " + available); //$NON-NLS-1$
        }

        ByteOrder byteOrder;
        if (matches(source, BIG_ENDIAN_MAGIC)) {
            byteOrder = ByteOrder.BIG_ENDIAN;
        } else if (matches(source, LITTLE_ENDIAN_MAGIC)) {
            byteOrder = ByteOrder.LITTLE_ENDIAN;
        } else {
            throw new TraceXFormatException("TraceX control-header magic mismatch"); //$NON-NLS-1$
        }
        if (available < SIZE) {
            throw new TraceXFormatException("Truncated TraceX control header: expected 48 bytes, found " + available); //$NON-NLS-1$
        }

        source.order(byteOrder);
        source.getInt();
        return new TraceXControlHeader(byteOrder,
                unsignedInt(source), unsignedInt(source), unsignedInt(source),
                Short.toUnsignedInt(source.getShort()), Short.toUnsignedInt(source.getShort()),
                unsignedInt(source), unsignedInt(source), unsignedInt(source),
                unsignedInt(source), unsignedInt(source), unsignedInt(source),
                unsignedInt(source));
    }

    private static boolean matches(ByteBuffer source, byte[] magic) {
        for (int i = 0; i < magic.length; i++) {
            if (source.get(i) != magic[i]) {
                return false;
            }
        }
        return true;
    }

    private static long unsignedInt(ByteBuffer source) {
        return Integer.toUnsignedLong(source.getInt());
    }

    /** @return byte order detected from the raw magic */
    public ByteOrder getByteOrder() {
        return fByteOrder;
    }

    /** @return raw unsigned timer-valid mask */
    public long getTimerValidMask() {
        return fTimerValidMask;
    }

    /** @return raw unsigned trace base address */
    public long getTraceBaseAddress() {
        return fTraceBaseAddress;
    }

    /** @return raw unsigned registry start address */
    public long getRegistryStartAddress() {
        return fRegistryStartAddress;
    }

    /** @return first reserved field */
    public int getReserved1() {
        return fReserved1;
    }

    /** @return unsigned object-name size */
    public int getRegistryNameSize() {
        return fRegistryNameSize;
    }

    /** @return raw unsigned registry end address */
    public long getRegistryEndAddress() {
        return fRegistryEndAddress;
    }

    /** @return raw unsigned event-area start address */
    public long getEventStartAddress() {
        return fEventStartAddress;
    }

    /** @return raw unsigned event-area end address */
    public long getEventEndAddress() {
        return fEventEndAddress;
    }

    /** @return raw unsigned current-event address */
    public long getEventCurrentAddress() {
        return fEventCurrentAddress;
    }

    /** @return second reserved field */
    public long getReserved2() {
        return fReserved2;
    }

    /** @return third reserved field */
    public long getReserved3() {
        return fReserved3;
    }

    /** @return fourth reserved field */
    public long getReserved4() {
        return fReserved4;
    }
}
