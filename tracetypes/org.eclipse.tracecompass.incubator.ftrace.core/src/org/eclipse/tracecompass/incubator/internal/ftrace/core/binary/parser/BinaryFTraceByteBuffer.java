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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * A reader for Ftrace files that utilizes ByteBuffer
 *
 * @author HoangPham
 */
public class BinaryFTraceByteBuffer implements AutoCloseable {
    private static final int BUFFER_SIZE = 4096;

    private final RandomAccessFile fTraceFile;
    private FileChannel fFileChannel;
    private ByteBuffer fByteBuffer;
    private ByteOrder fByteOrder = ByteOrder.BIG_ENDIAN;

    /**
     * Constructor
     *
     * @param path
     *            The path to the file
     * @throws FileNotFoundException
     *             Exception thrown when a file is not found
     */
    public BinaryFTraceByteBuffer(String path) throws FileNotFoundException {
        fTraceFile = new RandomAccessFile(path, "r"); //$NON-NLS-1$
        fFileChannel = fTraceFile.getChannel();
        fByteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    }

    /**
     * Get the byte order of the byte buffer
     *
     * @return The current byte order of the buffer
     */
    public ByteOrder getByteOrder() {
        return fByteOrder;
    }

    /**
     * Set the byte order of the byte buffer
     *
     * @param byteOrder
     *            The new byte order for the buffer
     */
    public void setByteOrder(ByteOrder byteOrder) {
        this.fByteOrder = byteOrder;
    }

    /**
     * Close the file
     *
     * @throws IOException
     *             Cannot close the file
     */
    @Override
    public void close() throws IOException {
        fFileChannel.close();
        fTraceFile.close();
    }

    /**
     * Read into the byte buffer
     *
     * @return The number of bytes read into the buffer
     * @throws IOException
     *             Cannot read data into the buffer
     */
    public int read() throws IOException {
        fByteBuffer.compact(); // Write mode
        int numOfBytesRead = fFileChannel.read(fByteBuffer);
        fByteBuffer.flip(); // Enable reading

        return numOfBytesRead;
    }

    /**
     * Get the next n amount of bytes
     *
     * @param byteCount
     *            The number of byte to read from the buffer
     * @return A byte array containing the data read from the buffer
     * @throws IOException
     *             An error occur while reading data from the buffer
     */
    public byte[] getNextBytes(int byteCount) throws IOException {
        byte[] bytesArray = new byte[byteCount];

        fByteBuffer.flip(); // To switch to read mode

        // Buffer will overflow
        if (fByteBuffer.remaining() < byteCount) {
            read(); // Get more data
        }

        fByteBuffer.get(bytesArray);
        fByteBuffer.compact(); // Compact the size of the buffer

        return bytesArray;
    }

    /**
     * Read the buffer and treat data as a string. The string ends when the we
     * encounter a null terminating character (\0)
     *
     * @return The string read from the file
     * @throws IOException
     *             Cannot read data from the buffer
     */
    public String getNextString() throws IOException {
        StringBuilder strBuilder = new StringBuilder();

        fByteBuffer.flip();

        // Make sure that there are some value to be read
        if (fByteBuffer.remaining() == 0) {
            read(); // Get more data
        }
        int value = fByteBuffer.get();

        while (value > 0) {
            strBuilder.append((char) value);
            if (fByteBuffer.remaining() == 0) { // If we run out of data
                read(); // Get more data
            }
            value = fByteBuffer.get();
        }

        fByteBuffer.compact(); // Compact the size of the buffer

        return strBuilder.toString();
    }

    /**
     * Get the next n bytes as a string
     *
     * @param byteCount
     *            Number of bytes to read
     * @return The string obtained by parsing n number of bytes
     * @throws IOException
     *             Cannot read data from the buffer
     */
    public String getNextBytesAsString(long byteCount) throws IOException {
        StringBuilder strBuilder = new StringBuilder();
        long remainingByte = byteCount;
        String str;

        while (remainingByte >= BUFFER_SIZE) {
            str = new String(getNextBytes(BUFFER_SIZE));
            strBuilder.append(str);
            remainingByte = remainingByte - BUFFER_SIZE;
        }

        str = new String(getNextBytes((int) remainingByte)); // we are sure that
                                                             // the value is
                                                             // less than 1024
        strBuilder.append(str);

        return strBuilder.toString();
    }

    /**
     * Get the next integer in the buffer stream
     *
     * @return The next integer in the buffer
     * @throws IOException
     *             Cannot read data from the buffer
     */
    public int getNextInt() throws IOException {
        byte[] byteArray = getNextBytes(4);

        ByteBuffer wrapped = ByteBuffer.wrap(byteArray).order(fByteOrder);
        return wrapped.getInt();
    }

    /**
     * Get the next double in the buffer stream
     *
     * @return The next double in the buffer
     * @throws IOException
     *             Cannot read data from the buffer
     */
    public double getNextDouble() throws IOException {
        byte[] byteArray = getNextBytes(8);
        ByteBuffer wrapped = ByteBuffer.wrap(byteArray).order(fByteOrder);
        return wrapped.getDouble();
    }

    /**
     * Get the next long in the buffer stream
     *
     * @return The next long in the buffer stream
     * @throws IOException
     *             Cannot read data from the buffer
     */
    public long getNextLong() throws IOException {
        byte[] byteArray = getNextBytes(8);
        ByteBuffer wrapped = ByteBuffer.wrap(byteArray).order(fByteOrder);
        return wrapped.getLong();
    }

    /**
     * Get the next short in the buffer stream
     *
     * @return The next short in the buffer stream
     * @throws IOException
     *             Cannot read data from the buffer
     */
    public short getNextShort() throws IOException {
        byte[] byteArray = getNextBytes(2);
        ByteBuffer wrapped = ByteBuffer.wrap(byteArray).order(fByteOrder);
        return wrapped.getShort();
    }

    /**
     * Move the byte buffer pointer to a specific offset in the file
     *
     * @param offset
     *            The new offset of the file
     * @throws IOException
     *             Cannot move the pointer to the value of the offset parameter
     */
    public void movePointerToOffset(long offset) throws IOException {
        fByteBuffer.clear();
        fTraceFile.seek(offset);
    }

    /**
     * Get the current offset location of the pointer of the byte buffer
     *
     * @return The current offset of the file pointer
     * @throws IOException
     *             Cannot get the current offset of the byte buffer
     */
    public long getCurrentFileOffset() throws IOException {
        return fTraceFile.getFilePointer();
    }
}
