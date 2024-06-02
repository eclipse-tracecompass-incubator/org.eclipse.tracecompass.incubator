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

package org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.parser;

import java.io.IOException;

import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceDataType;

/**
 * A reader for Ftrace files using a ByteBuffer obtained mem-mapping the .dat file.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceByteBuffer {
    private BinaryFTraceFileMapping fMappedBuffer;
    private long fCurrentOffset = 0;

    /**
     * Create a buffer mapping of the given file.
     *
     * @param filePath the file to map
     * @throws IOException if the file can't be opened or mapped
     */
    public BinaryFTraceByteBuffer(String filePath) throws IOException {
        this(new BinaryFTraceFileMapping(filePath));
    }

    /**
     * Create a (usually temporary) buffer that uses the given mapping.
     *
     * @param mappedBuffer the mem-mapped .dat file
     */
    public BinaryFTraceByteBuffer(BinaryFTraceFileMapping mappedBuffer) {
        fMappedBuffer = mappedBuffer;
    }

    /**
     * Move the byte buffer pointer to a specific offset in the file
     *
     * @param offset
     *            The new offset of the file
     */
    public void movePointerToOffset(long offset) {
        fCurrentOffset = offset;
    }

    /**
     * Get the next n amount of bytes
     *
     * @param byteCount
     *            The number of byte to read from the buffer
     * @return A byte array containing the data read from the buffer
     */
    public byte[] getNextBytes(int byteCount) {
        byte[] bytesArray = new byte[byteCount];
        fMappedBuffer.get(fCurrentOffset, bytesArray);
        fCurrentOffset += byteCount;
        return bytesArray;
    }

    /**
     * Read the buffer and treat data as a string. The string ends when the we
     * encounter a null terminating character (\0)
     *
     * @return The string read from the file
     */
    public String getNextString() {
        long pos = fCurrentOffset;
        StringBuilder strBuilder = new StringBuilder();

        byte value = fMappedBuffer.getByte(pos++);

        while (value > 0) {
            strBuilder.append((char) value);
            value = fMappedBuffer.getByte(pos++);
        }

        String returnString = strBuilder.toString();
        fCurrentOffset += (returnString.length() + BinaryFTraceHeaderElementSize.STRING_TERMINATOR);

        return returnString;
    }

    /**
     * Get the next n bytes as a string
     *
     * @param byteCount
     *            Number of bytes to read
     * @return The string obtained by parsing n number of bytes
     */
    public String getNextBytesAsString(long byteCount) {
        if (byteCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("FTrace Binary buffer: byteCount too large: " + byteCount); //$NON-NLS-1$
        }
        return new String(getNextBytes((int) byteCount));
    }

    /**
     * Get the next integer in the buffer stream
     *
     * @return The next integer in the buffer
     */
    public int getNextInt() {
        int value = fMappedBuffer.getInt(fCurrentOffset);
        fCurrentOffset += BinaryFTraceDataType.INT.getSize();
        return value;
    }

    /**
     * Get the next double in the buffer stream
     *
     * @return The next double in the buffer
     */
    public double getNextDouble() {
        double value = fMappedBuffer.getDouble(fCurrentOffset);
        fCurrentOffset += 8;
        return value;
    }

    /**
     * Get the next long in the buffer stream
     *
     * @return The next long in the buffer stream
     */
    public long getNextLong() {
        long value = fMappedBuffer.getLong(fCurrentOffset);
        fCurrentOffset += 8;
        return value;
    }

    /**
     * Get the next short in the buffer stream
     *
     * @return The next short in the buffer stream
     */
    public short getNextShort() {
        short value = fMappedBuffer.getShort(fCurrentOffset);
        fCurrentOffset += 2;
        return value;
    }

    /**
     * Get the current offset location of the pointer of the byte buffer
     *
     * @return The current offset of the file pointer
     */
    public long getCurrentOffset() {
        return fCurrentOffset;
    }

    /**
     * Get the size of the file that is currently being read
     *
     * @return The file size
     */
    public long getFileSize() {
        return fMappedBuffer.length();
    }
}
