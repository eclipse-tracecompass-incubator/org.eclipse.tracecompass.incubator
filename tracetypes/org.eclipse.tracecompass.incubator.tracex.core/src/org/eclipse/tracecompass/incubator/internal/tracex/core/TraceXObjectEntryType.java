package org.eclipse.tracecompass.incubator.internal.tracex.core;

public enum TraceXObjectEntryType {
    NotValid(0),
    Timer(2),
    Queue(3),
    Semaphore(4),
    Mutex(5),
    EventFlagsGroup(6),
    BlockPool(7),
    BytePool(8),
    Media(9),
    File(10),
    IP(11),
    PacketPool(12),
    TcpSocket(13),
    UdpSocket(14),
    Reserved1(15),
    Reserved2(16),
    Reserved3(17),
    Reserved4(18),
    Reserved5(19),
    Reserved6(20),
    UsbHostStackDevice(21),
    UsbHostStackInterface(22),
    UsbHostEndpoint(23),
    UsbHostClass(24),
    UsbDevice(25),
    UsbDeviceInterface(26),
    UsbDeviceEndpoint(27),
    UsbDeviceClass(28);

    private final int fValue;
    private TraceXObjectEntryType(int value) {
        fValue = value;
    }

    public int getValue() {
        return fValue;
    }
}
