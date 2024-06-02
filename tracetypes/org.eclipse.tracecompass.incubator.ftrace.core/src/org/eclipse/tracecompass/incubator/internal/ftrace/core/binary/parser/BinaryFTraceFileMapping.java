package org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.parser;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;

/**
 * Helper class to map a large file (> 2GB) using a list of MappedByteBuffers.
 *
 * Provides an interface (read only) similar to ByteBuffer, but uses a long
 * index instead of int.
 */
public final class BinaryFTraceFileMapping {
    /**
     * Length of the file segment mapped by each buffer.
     */
    public static final long SEGMENT_LEN = ((long) 1) << 31;

    /**
     * Overlap of each segment into the next, to easily access data that
     * sits at segment boundary.
     */
    public static final long SEGMENT_OVERLAP = 1 << 20; // 1MB overlap

    private final long fLength;
    private ArrayList<MappedByteBuffer> fMappedBuffers = new ArrayList<>();

    /**
     * Create a mapping for the given file.
     *
     * @param filePath the file path
     * @throws IOException if the file can't be opened or mapped
     */
    public BinaryFTraceFileMapping(String filePath) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(filePath, "r")) { //$NON-NLS-1$
            @SuppressWarnings("resource")
            FileChannel channel = file.getChannel(); // channel is closed automatically by the file
            fLength = file.length();
            final int segmentCount = (int)(fLength / SEGMENT_LEN);
            fMappedBuffers.ensureCapacity(segmentCount + 1);
            for (int i = 0; i < segmentCount; i++) {
                fMappedBuffers.add(channel.map(MapMode.READ_ONLY, i * SEGMENT_LEN, SEGMENT_LEN + SEGMENT_OVERLAP));
            }
            long remaining = fLength - segmentCount * SEGMENT_LEN;
            if (remaining > 0) {
                fMappedBuffers.add(channel.map(MapMode.READ_ONLY, segmentCount * SEGMENT_LEN, remaining));
            }
        }
    }

    /**
     * Set the byte order for the read operations.
     *
     * @param endianess the byte order to set
     */
    public void order(ByteOrder endianess) {
        for (MappedByteBuffer buffer : fMappedBuffers) {
            buffer.order(endianess);
        }
    }

    /**
     * Fill the given array with bytes from the file at the given position.
     *
     * @param index position to read from
     * @param dst the byte array to fill
     */
    public void get(long index, byte[] dst) {
        MappedByteBuffer mappedBuffer = fMappedBuffers.get((int) (index / SEGMENT_LEN));
        mappedBuffer.get((int)(index % SEGMENT_LEN), dst);
    }

    /**
     * Read a single byte from the given position.
     *
     * @param index position to read from
     * @return the value
     */
    public byte getByte(long index) {
        MappedByteBuffer mappedBuffer = fMappedBuffers.get((int) (index / SEGMENT_LEN));
        return mappedBuffer.get((int)(index % SEGMENT_LEN));
    }

    /**
     * Read an int (4 bytes) from the given position.
     *
     * @param index position to read from
     * @return the value
     */
    public int getInt(long index) {
        MappedByteBuffer mappedBuffer = fMappedBuffers.get((int) (index / SEGMENT_LEN));
        return mappedBuffer.getInt((int)(index % SEGMENT_LEN));
    }

    /**
     * Read a double (8 bytes) from the given position.
     *
     * @param index position to read from
     * @return the value
     */
    public double getDouble(long index) {
        MappedByteBuffer mappedBuffer = fMappedBuffers.get((int) (index / SEGMENT_LEN));
        return mappedBuffer.getDouble((int)(index % SEGMENT_LEN));
    }

    /**
     * Read a long (8 bytes) from the given position.
     *
     * @param index position to read from
     * @return the value
     */
   public long getLong(long index) {
        MappedByteBuffer mappedBuffer = fMappedBuffers.get((int) (index / SEGMENT_LEN));
        return mappedBuffer.getLong((int)(index % SEGMENT_LEN));
    }

   /**
    * Read a short (2 bytes) from the given position.
    *
    * @param index position to read from
    * @return the value
    */
    public short getShort(long index) {
        MappedByteBuffer mappedBuffer = fMappedBuffers.get((int) (index / SEGMENT_LEN));
        return mappedBuffer.getShort((int)(index % SEGMENT_LEN));
    }

    /**
     * Get the mapped length (the file size).
     *
     * @return the mapped length (the file size)
     */
    public long length() {
        return fLength;
    }
}