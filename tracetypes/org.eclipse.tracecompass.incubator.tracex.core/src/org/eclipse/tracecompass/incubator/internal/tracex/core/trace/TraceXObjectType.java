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

import java.util.HashMap;
import java.util.Map;

/** Documented TraceX object-registry types. */
public enum TraceXObjectType {
    /** Unused or invalid registry entry. */
    NOT_VALID(0, "Not Valid"), //$NON-NLS-1$
    /** ThreadX thread. */
    THREAD(1, "Thread"), //$NON-NLS-1$
    /** ThreadX timer. */
    TIMER(2, "Timer"), //$NON-NLS-1$
    /** ThreadX queue. */
    QUEUE(3, "Queue"), //$NON-NLS-1$
    /** ThreadX semaphore. */
    SEMAPHORE(4, "Semaphore"), //$NON-NLS-1$
    /** ThreadX mutex. */
    MUTEX(5, "Mutex"), //$NON-NLS-1$
    /** ThreadX event-flags group. */
    EVENT_FLAGS(6, "Event Flags Group"), //$NON-NLS-1$
    /** ThreadX block pool. */
    BLOCK_POOL(7, "Block Pool"), //$NON-NLS-1$
    /** ThreadX byte pool. */
    BYTE_POOL(8, "Byte Pool"), //$NON-NLS-1$
    /** FileX media. */
    MEDIA(9, "Media"), //$NON-NLS-1$
    /** FileX file. */
    FILE(10, "File"), //$NON-NLS-1$
    /** NetX Duo IP instance. */
    IP(11, "IP"), //$NON-NLS-1$
    /** NetX Duo packet pool. */
    PACKET_POOL(12, "Packet Pool"), //$NON-NLS-1$
    /** NetX Duo TCP socket. */
    TCP_SOCKET(13, "TCP Socket"), //$NON-NLS-1$
    /** NetX Duo UDP socket. */
    UDP_SOCKET(14, "UDP Socket"), //$NON-NLS-1$
    /** USBX host-stack device. */
    USB_HOST_DEVICE(21, "USB Host Device"), //$NON-NLS-1$
    /** USBX host-stack interface. */
    USB_HOST_INTERFACE(22, "USB Host Interface"), //$NON-NLS-1$
    /** USBX host endpoint. */
    USB_HOST_ENDPOINT(23, "USB Host Endpoint"), //$NON-NLS-1$
    /** USBX host class. */
    USB_HOST_CLASS(24, "USB Host Class"), //$NON-NLS-1$
    /** USBX device. */
    USB_DEVICE(25, "USB Device"), //$NON-NLS-1$
    /** USBX device interface. */
    USB_DEVICE_INTERFACE(26, "USB Device Interface"), //$NON-NLS-1$
    /** USBX device endpoint. */
    USB_DEVICE_ENDPOINT(27, "USB Device Endpoint"), //$NON-NLS-1$
    /** USBX device class. */
    USB_DEVICE_CLASS(28, "USB Device Class"), //$NON-NLS-1$
    /** Reserved or unfamiliar object type. */
    UNKNOWN(-1, "Unknown"); //$NON-NLS-1$

    private static final Map<Integer, TraceXObjectType> TYPES = new HashMap<>();

    static {
        for (TraceXObjectType type : values()) {
            if (type.fCode >= 0) {
                TYPES.put(type.fCode, type);
            }
        }
    }

    private final int fCode;
    private final String fDisplayName;

    TraceXObjectType(int code, String displayName) {
        fCode = code;
        fDisplayName = displayName;
    }

    /** @return encoded object-type byte, or -1 for unknown values */
    public int getCode() {
        return fCode;
    }

    /** @return stable display name */
    public String getDisplayName() {
        return fDisplayName;
    }

    /**
     * Resolve an encoded object type.
     *
     * @param code
     *            unsigned encoded byte
     * @return documented type, or {@link #UNKNOWN}
     */
    public static TraceXObjectType fromCode(int code) {
        TraceXObjectType type = TYPES.get(code);
        return type == null ? UNKNOWN : type;
    }
}
