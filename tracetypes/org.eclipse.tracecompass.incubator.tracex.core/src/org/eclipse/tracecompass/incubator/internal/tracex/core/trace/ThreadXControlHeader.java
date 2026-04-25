package org.eclipse.tracecompass.incubator.internal.tracex.core.trace;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ThreadXControlHeader {

    private static final int CONTROL_HEADER_ID = 0x54585442;

    /**
     * Reads the control header
     * @param path
     * @return
     * @throws IOException
     */
    public static ThreadXControlHeader read(String path) throws IOException {
        try(FileInputStream fis = new FileInputStream(new File(path))){
            ByteBuffer bb = ByteBuffer.wrap(fis.readNBytes(48));
            bb.order(ByteOrder.LITTLE_ENDIAN);
            int id = bb.getInt();
            int timerValidMask = bb.getInt();
            int traceBaseAddress = bb.getInt();
            int registryStartPointer = bb.getInt()-4;
            short reserved1 = bb.getShort();
            short registryNameSize= bb.getShort();
            int registryEndPointer = bb.getInt()-4;
            int bufferStartPointer = bb.getInt()-4;
            int bufferEndPointer = bb.getInt()-4;
            int bufferCurrentPointer = bb.getInt();
            int reserved2 = bb.getInt();
            int reserved3 = bb.getInt();
            int reserved4 = bb.getInt();
            return new ThreadXControlHeader(id, timerValidMask, traceBaseAddress, registryStartPointer, reserved1, registryNameSize, registryEndPointer, bufferStartPointer, bufferEndPointer, bufferCurrentPointer, reserved2, reserved3, reserved4);
        }
    }

    private final int  fTxTraceControlHeaderId;
    private final int  fTxTraceControlHeaderTimerValidMask;
    private final int  fTxTraceControlHeaderTraceBaseAddress;
    private final int  fTxTraceControlHeaderObjectRegistryStartPointer;
    private final short fTxTraceControlHeaderReserved1;
    private final short fTxTraceControlHeaderObjectRegistryNameSize;
    private final int  fTxTraceControlHeaderObjectRegistryEndPointer;
    private final int  fTxTraceControlHeaderBufferStartPointer;
    private final int  fTxTraceControlHeaderBufferEndPointer;
    private final int  fTxTraceControlHeaderBufferCurrentPointer;
    private final int  fTxTraceControlHeaderReserved2;
    private final int  fTxTraceControlHeaderReserved3;
    private final int  fTxTraceControlHeaderReserved4;

    /**
     * Constructor
     * @param id Control Header ID, should be 0x54585442
     * @param timerValidMask
     * @param traceBaseAddress
     * @param objectRegistryStartPointer
     * @param reserved1
     * @param registryNameSize
     * @param objectRegistryEndPointer
     * @param bufferStartPointer
     * @param bufferEndPointer
     * @param bufferCurrentPointer
     * @param reserved2
     * @param reserved3
     * @param reserved4
     */
    public ThreadXControlHeader(
            int  id,
            int  timerValidMask,
            int  traceBaseAddress,
            int  objectRegistryStartPointer,
            short reserved1,
            short registryNameSize,
            int  objectRegistryEndPointer,
            int  bufferStartPointer,
            int  bufferEndPointer,
            int  bufferCurrentPointer,
            int  reserved2,
            int  reserved3,
            int  reserved4) {

        fTxTraceControlHeaderId = id;
        fTxTraceControlHeaderTimerValidMask = timerValidMask;
        fTxTraceControlHeaderTraceBaseAddress = traceBaseAddress;
        fTxTraceControlHeaderObjectRegistryStartPointer = objectRegistryStartPointer;
        fTxTraceControlHeaderReserved1 = reserved1;
        fTxTraceControlHeaderObjectRegistryNameSize = registryNameSize;
        fTxTraceControlHeaderObjectRegistryEndPointer = objectRegistryEndPointer;
        fTxTraceControlHeaderBufferStartPointer = bufferStartPointer;
        fTxTraceControlHeaderBufferEndPointer = bufferEndPointer;
        fTxTraceControlHeaderBufferCurrentPointer = bufferCurrentPointer;
        fTxTraceControlHeaderReserved2 = reserved2;
        fTxTraceControlHeaderReserved3 = reserved3;
        fTxTraceControlHeaderReserved4 = reserved4;
    }
    public boolean isHeaderId() {
        return fTxTraceControlHeaderId == CONTROL_HEADER_ID;
    }

    /**
     * @return the txTraceControlHeaderTimerValidMask
     */
    public int  getTxTraceControlHeaderTimerValidMask() {
        return fTxTraceControlHeaderTimerValidMask;
    }

    /**
     * @return the txTraceControlHeaderTraceBaseAddress
     */
    public int  getTxTraceControlHeaderTraceBaseAddress() {
        return fTxTraceControlHeaderTraceBaseAddress;
    }

    /**
     * @return the txTraceControlHeaderObjectRegistryStartPointer
     */
    public int  getObjectRegistryStartPointer() {
        return fTxTraceControlHeaderObjectRegistryStartPointer;
    }

    /**
     * @return the txTraceControlHeaderReserved1
     */
    public int  getReserved1() {
        return fTxTraceControlHeaderReserved1;
    }

    /**
     * @return the txTraceControlHeaderObjectRegistryNameSize
     */
    public int  getNameSize() {
        return fTxTraceControlHeaderObjectRegistryNameSize;
    }

    /**
     * @return the txTraceControlHeaderObjectRegistryEndPointer
     */
    public int  getObjectRegistryEndPointer() {
        return fTxTraceControlHeaderObjectRegistryEndPointer;
    }

    /**
     * @return the txTraceControlHeaderBufferStartPointer
     */
    public int  getBufferStartPointer() {
        return fTxTraceControlHeaderBufferStartPointer;
    }

    /**
     * @return the txTraceControlHeaderBufferEndPointer
     */
    public long getBufferEndPointer() {
        return fTxTraceControlHeaderBufferEndPointer;
    }

    /**
     * @return the txTraceControlHeaderBufferCurrentPointer
     */
    public int  getTxTraceControlHeaderBufferCurrentPointer() {
        return fTxTraceControlHeaderBufferCurrentPointer;
    }

    /**
     * @return the txTraceControlHeaderReserved2
     */
    public int  getTxTraceControlHeaderReserved2() {
        return fTxTraceControlHeaderReserved2;
    }

    /**
     * @return the txTraceControlHeaderReserved3
     */
    public int  getTxTraceControlHeaderReserved3() {
        return fTxTraceControlHeaderReserved3;
    }

    /**
     * @return the txTraceControlHeaderReserved4
     */
    public int  getTxTraceControlHeaderReserved4() {
        return fTxTraceControlHeaderReserved4;
    }

}
