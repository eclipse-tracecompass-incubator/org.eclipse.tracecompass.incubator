package org.eclipse.tracecompass.incubator.internal.tracex.core.trace;

public class ThreadXObjectRegistryEntry {
    private static final int fTX_TRACE_OBJECT_REGISTRY_NAME = 32;

    private final char[][] fObjectAvailable;
    private final char[][] fObjectType;
    private final char fObjectReserved1;
    private final char fObjectReserved2;
    private final long fTxTraceThreadRegistryEntryObjectPointer;
    private final long fObjectParameter_1;
    private final long fObjectParameter_2;
    private final char[] fTxTraceThreadRegistryEntryObjectName;

    /**
     * Constructor
     * @param ObjectAvailable
     * @param ObjectType
     * @param ObjectReserved1
     * @param ObjectReserved2
     * @param txTraceThreadRegistryEntryObjectPointer
     * @param ObjectParameter_1
     * @param ObjectParameter_2
     * @param txTraceThreadRegistryEntryObjectName
     */
    public ThreadXObjectRegistryEntry(char[][] objectAvailable, char[][] ObjectType, char ObjectReserved1, char ObjectReserved2,
            long txTraceThreadRegistryEntryObjectPointer, long ObjectParameter_1, long ObjectParameter_2, char[] txTraceThreadRegistryEntryObjectName) {
        super();
        fObjectAvailable = objectAvailable;
        fObjectType = ObjectType;
        fObjectReserved1 = ObjectReserved1;
        fObjectReserved2 = ObjectReserved2;
        fTxTraceThreadRegistryEntryObjectPointer = txTraceThreadRegistryEntryObjectPointer;
        fObjectParameter_1 = ObjectParameter_1;
        fObjectParameter_2 = ObjectParameter_2;
        fTxTraceThreadRegistryEntryObjectName = txTraceThreadRegistryEntryObjectName;
    }

    /**
     * @return the ftxTraceObjectRegistryName
     */
    public static int getFtxTraceObjectRegistryName() {
        return fTX_TRACE_OBJECT_REGISTRY_NAME;
    }

    /**
     * @return the ObjectAvailable
     */
    public char[][] getObjectAvailable() {
        return fObjectAvailable;
    }

    /**
     * @return the ObjectType
     */
    public char[][] getObjectType() {
        return fObjectType;
    }

    /**
     * @return the ObjectReserved1
     */
    public char getObjectReserved1() {
        return fObjectReserved1;
    }

    /**
     * @return the ObjectReserved2
     */
    public char getObjectReserved2() {
        return fObjectReserved2;
    }

    /**
     * @return the txTraceThreadRegistryEntryObjectPointer
     */
    public long getTxTraceThreadRegistryEntryObjectPointer() {
        return fTxTraceThreadRegistryEntryObjectPointer;
    }

    /**
     * @return the ObjectParameter_1
     */
    public long getObjectParameter_1() {
        return fObjectParameter_1;
    }

    /**
     * @return the ObjectParameter_2
     */
    public long getObjectParameter_2() {
        return fObjectParameter_2;
    }

    /**
     * @return the txTraceThreadRegistryEntryObjectName
     */
    public char[] getTxTraceThreadRegistryEntryObjectName() {
        return fTxTraceThreadRegistryEntryObjectName;
    }

}
