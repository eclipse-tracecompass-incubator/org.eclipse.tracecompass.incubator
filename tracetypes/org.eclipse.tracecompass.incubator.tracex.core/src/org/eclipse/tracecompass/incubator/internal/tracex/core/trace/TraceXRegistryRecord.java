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
import org.eclipse.jdt.annotation.Nullable;

/** Immutable lossless representation of one TraceX registry record. */
public final class TraceXRegistryRecord {

    private static final int FIXED_SIZE = 16;

    private final long fIndex;
    private final int fAvailable;
    private final int fTypeCode;
    private final int fReserved1;
    private final int fReserved2;
    private final long fAddress;
    private final long fParameter1;
    private final long fParameter2;
    private final byte[] fNameBytes;
    private final String fName;
    private final byte[] fRawBytes;
    private final @Nullable Integer fInitialPriority;

    private TraceXRegistryRecord(long index, int available, int typeCode,
            int reserved1, int reserved2, long address, long parameter1,
            long parameter2, byte[] nameBytes, byte[] rawBytes) {
        fIndex = index;
        fAvailable = available;
        fTypeCode = typeCode;
        fReserved1 = reserved1;
        fReserved2 = reserved2;
        fAddress = address;
        fParameter1 = parameter1;
        fParameter2 = parameter2;
        fNameBytes = nameBytes;
        fName = decodeName(nameBytes);
        fRawBytes = rawBytes;
        fInitialPriority = typeCode == TraceXObjectType.THREAD.getCode()
                && (reserved1 & 0x80) != 0
                        ? Integer.valueOf(((reserved1 & 0x03) << 8) | reserved2)
                        : null;
    }

    public static TraceXRegistryRecord decode(long index, byte[] rawBytes,
            int nameSize, ByteOrder order) throws TraceXFormatException {
        if (nameSize < 1) {
            throw new TraceXFormatException("Object registry name size is zero"); //$NON-NLS-1$
        }
        if (rawBytes.length < FIXED_SIZE + nameSize) {
            throw new TraceXFormatException("Truncated object registry record " + index); //$NON-NLS-1$
        }
        ByteBuffer buffer = ByteBuffer.wrap(rawBytes).order(order);
        int available = Byte.toUnsignedInt(buffer.get());
        int type = Byte.toUnsignedInt(buffer.get());
        int reserved1 = Byte.toUnsignedInt(buffer.get());
        int reserved2 = Byte.toUnsignedInt(buffer.get());
        long address = Integer.toUnsignedLong(buffer.getInt());
        long parameter1 = Integer.toUnsignedLong(buffer.getInt());
        long parameter2 = Integer.toUnsignedLong(buffer.getInt());
        byte[] name = new byte[nameSize];
        buffer.get(name);
        return new TraceXRegistryRecord(index, available, type, reserved1,
                reserved2, address, parameter1, parameter2, name,
                rawBytes.clone());
    }

    private static String decodeName(byte[] bytes) {
        StringBuilder name = new StringBuilder(bytes.length);
        for (byte value : bytes) {
            int unsigned = Byte.toUnsignedInt(value);
            if (unsigned == 0) {
                break;
            }
            name.append(unsigned >= 0x20 && unsigned <= 0x7e
                    ? (char) unsigned : '\ufffd');
        }
        return name.toString();
    }

    /** @return zero-based physical registry index */
    public long getIndex() {
        return fIndex;
    }

    /** @return raw unsigned availability byte */
    public int getAvailable() {
        return fAvailable;
    }

    /** @return whether the slot is currently available for reuse */
    public boolean isAvailable() {
        return fAvailable == 1;
    }

    /** @return whether the record retains object metadata */
    public boolean isValid() {
        return fTypeCode != TraceXObjectType.NOT_VALID.getCode();
    }

    /** @return whether the record describes an object still in use */
    public boolean isInUse() {
        return isValid() && !isAvailable();
    }

    /** @return raw unsigned object-type byte */
    public int getTypeCode() {
        return fTypeCode;
    }

    /** @return recognized object type, or unknown */
    public TraceXObjectType getType() {
        return TraceXObjectType.fromCode(fTypeCode);
    }

    /** @return first raw reserved byte */
    public int getReserved1() {
        return fReserved1;
    }

    /** @return second raw reserved byte */
    public int getReserved2() {
        return fReserved2;
    }

    /** @return unsigned 32-bit object address */
    public long getAddress() {
        return fAddress;
    }

    /** @return consistently formatted object address */
    public String getFormattedAddress() {
        return TraceXEvent.formatAddress(fAddress);
    }

    /** @return first unsigned documented parameter */
    public long getParameter1() {
        return fParameter1;
    }

    /** @return second unsigned documented parameter */
    public long getParameter2() {
        return fParameter2;
    }

    /** @return decoded display name */
    public String getName() {
        return fName;
    }

    /** @return copy of the complete fixed-size name area */
    public byte[] getNameBytes() {
        return fNameBytes.clone();
    }

    /** @return copy of the complete encoded registry record */
    public byte[] getRawBytes() {
        return fRawBytes.clone();
    }

    /** @return encoded initial thread priority, or {@code null} when absent */
    public @Nullable Integer getInitialPriority() {
        return fInitialPriority;
    }

}
