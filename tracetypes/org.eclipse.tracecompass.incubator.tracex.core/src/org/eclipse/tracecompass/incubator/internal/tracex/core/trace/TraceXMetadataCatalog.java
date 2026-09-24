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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXEventMetadata.Alias;

/** Table-driven metadata catalog for events exercised by the bundled demos. */
public final class TraceXMetadataCatalog {

    private static final Map<Integer, TraceXEventMetadata> EVENTS;
    private static final TraceXEventMetadata INVALID = new TraceXEventMetadata(0xffff,
            "TX_TRACE_INVALID_EVENT", TraceXEventCategory.INVALID); //$NON-NLS-1$

    static {
        Map<Integer, TraceXEventMetadata> events = new LinkedHashMap<>();

        add(events, 1, "TX_TRACE_THREAD_RESUME", t(), //$NON-NLS-1$
                a(1, "thread"), v(2, "previous_state"), a(3, "stack"), a(4, "next_thread")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 2, "TX_TRACE_THREAD_SUSPEND", t(), //$NON-NLS-1$
                a(1, "thread"), v(2, "new_state"), a(3, "stack"), a(4, "next_thread")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 3, "TX_TRACE_ISR_ENTER", t(), //$NON-NLS-1$
                a(1, "stack"), v(2, "isr_number"), v(3, "system_state"), v(4, "preempt_disable")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 4, "TX_TRACE_ISR_EXIT", t(), //$NON-NLS-1$
                a(1, "stack"), v(2, "isr_number"), v(3, "system_state"), v(4, "preempt_disable")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 5, "TX_TRACE_TIME_SLICE", t(), //$NON-NLS-1$
                a(1, "next_thread"), v(2, "system_state"), v(3, "preempt_disable"), a(4, "stack")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 32, "TX_TRACE_EVENT_FLAGS_GET", t(), //$NON-NLS-1$
                a(1, "event_flags"), v(2, "requested_flags"), v(3, "current_flags"), v(4, "get_option")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 36, "TX_TRACE_EVENT_FLAGS_SET", t(), //$NON-NLS-1$
                a(1, "event_flags"), v(2, "flags_to_set"), v(3, "set_option"), v(4, "suspended_count")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 50, "TX_TRACE_MUTEX_CREATE", t(), //$NON-NLS-1$
                a(1, "mutex"), v(2, "inheritance"), a(3, "stack")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        add(events, 51, "TX_TRACE_MUTEX_DELETE", t(), //$NON-NLS-1$
                a(1, "mutex"), a(2, "stack")); //$NON-NLS-1$ //$NON-NLS-2$
        add(events, 52, "TX_TRACE_MUTEX_GET", t(), //$NON-NLS-1$
                a(1, "mutex"), v(2, "wait_option"), a(3, "owning_thread"), v(4, "ownership_count")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 57, "TX_TRACE_MUTEX_PUT", t(), //$NON-NLS-1$
                a(1, "mutex"), a(2, "owning_thread"), v(3, "ownership_count"), a(4, "stack")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 68, "TX_TRACE_QUEUE_RECEIVE", t(), //$NON-NLS-1$
                a(1, "queue"), a(2, "destination"), v(3, "wait_option"), v(4, "enqueued")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 69, "TX_TRACE_QUEUE_SEND", t(), //$NON-NLS-1$
                a(1, "queue"), a(2, "source"), v(3, "wait_option"), v(4, "enqueued")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 83, "TX_TRACE_SEMAPHORE_GET", t(), //$NON-NLS-1$
                a(1, "semaphore"), v(2, "wait_option"), v(3, "current_count"), a(4, "stack")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 88, "TX_TRACE_SEMAPHORE_PUT", t(), //$NON-NLS-1$
                a(1, "semaphore"), v(2, "current_count"), v(3, "suspended_count"), a(4, "stack")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 103, "TX_TRACE_THREAD_IDENTIFY", t()); //$NON-NLS-1$
        add(events, 107, "TX_TRACE_THREAD_PREEMPTION_CHANGE", t(), //$NON-NLS-1$
                a(1, "thread"), v(2, "new_threshold"), v(3, "old_threshold"), v(4, "thread_state")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 109, "TX_TRACE_THREAD_RELINQUISH", t(), //$NON-NLS-1$
                a(1, "stack"), a(2, "next_thread")); //$NON-NLS-1$ //$NON-NLS-2$
        add(events, 112, "TX_TRACE_THREAD_SLEEP", t(), //$NON-NLS-1$
                v(1, "sleep_ticks"), v(2, "thread_state"), a(3, "stack")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        add(events, 201, "FX_TRACE_INTERNAL_LOG_SECTOR_CACHE_MISS", f(), //$NON-NLS-1$
                a(1, "media"), v(2, "sector"), v(3, "total_misses"), v(4, "cache_size")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 202, "FX_TRACE_INTERNAL_DIR_CACHE_MISS", f(), //$NON-NLS-1$
                a(1, "media"), v(2, "total_misses")); //$NON-NLS-1$ //$NON-NLS-2$
        add(events, 203, "FX_TRACE_INTERNAL_MEDIA_FLUSH", f(), //$NON-NLS-1$
                a(1, "media"), v(2, "dirty_sectors")); //$NON-NLS-1$ //$NON-NLS-2$
        add(events, 204, "FX_TRACE_INTERNAL_DIR_ENTRY_READ", f(), a(1, "media")); //$NON-NLS-1$ //$NON-NLS-2$
        add(events, 205, "FX_TRACE_INTERNAL_DIR_ENTRY_WRITE", f(), a(1, "media")); //$NON-NLS-1$ //$NON-NLS-2$
        add(events, 206, "FX_TRACE_INTERNAL_IO_DRIVER_READ", f(), //$NON-NLS-1$
                a(1, "media"), v(2, "sector"), v(3, "sector_count"), a(4, "buffer")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 207, "FX_TRACE_INTERNAL_IO_DRIVER_WRITE", f(), //$NON-NLS-1$
                a(1, "media"), v(2, "sector"), v(3, "sector_count"), a(4, "buffer")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 208, "FX_TRACE_INTERNAL_IO_DRIVER_FLUSH", f(), a(1, "media")); //$NON-NLS-1$ //$NON-NLS-2$
        add(events, 210, "FX_TRACE_INTERNAL_IO_DRIVER_INIT", f(), a(1, "media")); //$NON-NLS-1$ //$NON-NLS-2$
        add(events, 211, "FX_TRACE_INTERNAL_IO_DRIVER_BOOT_READ", f(), //$NON-NLS-1$
                a(1, "media"), a(2, "buffer")); //$NON-NLS-1$ //$NON-NLS-2$
        add(events, 214, "FX_TRACE_INTERNAL_IO_DRIVER_UNINIT", f(), a(1, "media")); //$NON-NLS-1$ //$NON-NLS-2$
        add(events, 243, "FX_TRACE_FILE_CLOSE", f(), a(1, "file"), v(3, "file_size")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        add(events, 244, "FX_TRACE_FILE_CREATE", f(), a(1, "media"), a(2, "file_name")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        add(events, 247, "FX_TRACE_FILE_OPEN", f(), //$NON-NLS-1$
                a(1, "media"), a(2, "file"), a(3, "file_name"), v(4, "open_type")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 248, "FX_TRACE_FILE_READ", f(), //$NON-NLS-1$
                a(1, "file"), a(2, "buffer"), v(3, "request_size"), v(4, "actual_size")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 251, "FX_TRACE_FILE_SEEK", f(), //$NON-NLS-1$
                a(1, "file"), v(2, "byte_offset"), v(3, "previous_offset")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        add(events, 254, "FX_TRACE_FILE_WRITE", f(), //$NON-NLS-1$
                a(1, "file"), a(2, "buffer"), v(3, "size"), v(4, "bytes_written")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 258, "FX_TRACE_MEDIA_CLOSE", f(), a(1, "media")); //$NON-NLS-1$ //$NON-NLS-2$
        add(events, 261, "FX_TRACE_MEDIA_OPEN", f(), //$NON-NLS-1$
                a(1, "media"), a(2, "media_driver"), a(3, "memory"), v(4, "memory_size")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        add(events, 308, "NX_TRACE_INTERNAL_IP_RECEIVE", n(), //$NON-NLS-1$
                a(1, "ip"), v(2, "source_ip"), a(3, "packet"), v(4, "packet_length")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 309, "NX_TRACE_INTERNAL_IP_SEND", n(), //$NON-NLS-1$
                a(1, "ip"), v(2, "destination_ip"), a(3, "packet"), v(4, "packet_length")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 310, "NX_TRACE_INTERNAL_TCP_DATA_RECEIVE", n(), //$NON-NLS-1$
                a(1, "ip"), v(2, "source_ip"), a(3, "packet"), v(4, "sequence")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 311, "NX_TRACE_INTERNAL_TCP_DATA_SEND", n(), //$NON-NLS-1$
                a(1, "ip"), a(2, "socket"), a(3, "packet"), v(4, "sequence")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 312, "NX_TRACE_INTERNAL_TCP_FIN_RECEIVE", n(), //$NON-NLS-1$
                a(1, "ip"), a(2, "socket"), a(3, "packet"), v(4, "sequence")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 313, "NX_TRACE_INTERNAL_TCP_FIN_SEND", n(), //$NON-NLS-1$
                a(1, "ip"), a(2, "socket"), a(3, "packet"), v(4, "sequence")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 316, "NX_TRACE_INTERNAL_TCP_SYN_RECEIVE", n(), //$NON-NLS-1$
                a(1, "ip"), a(2, "socket"), a(3, "packet"), v(4, "sequence")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 317, "NX_TRACE_INTERNAL_TCP_SYN_SEND", n(), //$NON-NLS-1$
                a(1, "ip"), a(2, "socket"), a(3, "packet"), v(4, "sequence")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 318, "NX_TRACE_INTERNAL_UDP_RECEIVE", n(), //$NON-NLS-1$
                a(1, "ip"), a(2, "socket"), a(3, "packet"), v(4, "header_word_0")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 319, "NX_TRACE_INTERNAL_UDP_SEND", n(), //$NON-NLS-1$
                a(1, "ip"), a(2, "socket"), a(3, "packet"), v(4, "header_word_0")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 323, "NX_TRACE_INTERNAL_TCP_STATE_CHANGE", n(), //$NON-NLS-1$
                a(1, "ip"), a(2, "socket"), v(3, "previous_state"), v(4, "new_state")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 324, "NX_TRACE_INTERNAL_IO_DRIVER_PACKET_SEND", n(), //$NON-NLS-1$
                a(1, "ip"), a(2, "packet"), v(3, "packet_size")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        add(events, 384, "NX_TRACE_IP_RAW_PACKET_SEND", n(), //$NON-NLS-1$
                a(1, "ip"), a(2, "packet"), v(3, "destination_ip"), v(4, "type_of_service")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 386, "NX_TRACE_PACKET_ALLOCATE", n(), //$NON-NLS-1$
                a(1, "packet_pool"), a(2, "packet"), v(3, "packet_type"), v(4, "available_packets")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 387, "NX_TRACE_PACKET_COPY", n(), //$NON-NLS-1$
                a(1, "packet"), a(2, "new_packet"), a(3, "packet_pool"), v(4, "wait_option")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 388, "NX_TRACE_PACKET_DATA_APPEND", n(), //$NON-NLS-1$
                a(1, "packet"), a(2, "data_start"), v(3, "data_size"), a(4, "packet_pool")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 390, "NX_TRACE_PACKET_LENGTH_GET", n(), //$NON-NLS-1$
                a(1, "packet"), v(2, "length")); //$NON-NLS-1$ //$NON-NLS-2$
        add(events, 394, "NX_TRACE_PACKET_RELEASE", n(), //$NON-NLS-1$
                a(1, "packet"), v(2, "packet_status"), v(3, "available_packets")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        add(events, 395, "NX_TRACE_PACKET_TRANSMIT_RELEASE", n(), //$NON-NLS-1$
                a(1, "packet"), v(2, "packet_status"), v(3, "available_packets")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        add(events, 400, "NX_TRACE_TCP_CLIENT_SOCKET_BIND", n(), //$NON-NLS-1$
                a(1, "ip"), a(2, "socket"), v(3, "port"), v(4, "wait_option")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 401, "NX_TRACE_TCP_CLIENT_SOCKET_CONNECT", n(), //$NON-NLS-1$
                a(1, "ip"), a(2, "socket"), v(3, "server_ip"), v(4, "server_port")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 403, "NX_TRACE_TCP_CLIENT_SOCKET_UNBIND", n(), //$NON-NLS-1$
                a(1, "ip"), a(2, "socket")); //$NON-NLS-1$ //$NON-NLS-2$
        add(events, 407, "NX_TRACE_TCP_SERVER_SOCKET_ACCEPT", n(), //$NON-NLS-1$
                a(1, "ip"), a(2, "socket"), v(3, "wait_option"), v(4, "socket_state")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 409, "NX_TRACE_TCP_SERVER_SOCKET_RELISTEN", n(), //$NON-NLS-1$
                a(1, "ip"), v(2, "port"), a(3, "socket"), v(4, "socket_state")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 410, "NX_TRACE_TCP_SERVER_SOCKET_UNACCEPT", n(), //$NON-NLS-1$
                a(1, "ip"), a(2, "socket"), v(3, "socket_state")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        add(events, 412, "NX_TRACE_TCP_SOCKET_CREATE", n(), //$NON-NLS-1$
                a(1, "ip"), a(2, "socket"), v(3, "type_of_service"), v(4, "window_size")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 413, "NX_TRACE_TCP_SOCKET_DELETE", n(), //$NON-NLS-1$
                a(1, "ip"), a(2, "socket"), v(3, "socket_state")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        add(events, 414, "NX_TRACE_TCP_SOCKET_DISCONNECT", n(), //$NON-NLS-1$
                a(1, "ip"), a(2, "socket"), v(3, "wait_option"), v(4, "socket_state")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 419, "NX_TRACE_TCP_SOCKET_RECEIVE", n(), //$NON-NLS-1$
                a(1, "socket"), a(2, "packet"), v(3, "length"), v(4, "receive_sequence")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 421, "NX_TRACE_TCP_SOCKET_SEND", n(), //$NON-NLS-1$
                a(1, "socket"), a(2, "packet"), v(3, "length"), v(4, "transmit_sequence")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 434, "NX_TRACE_UDP_SOCKET_RECEIVE", n(), //$NON-NLS-1$
                a(1, "ip"), a(2, "socket"), a(3, "packet"), v(4, "packet_size")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        add(events, 436, "NX_TRACE_UDP_SOCKET_SEND", n(), //$NON-NLS-1$
                a(1, "socket"), a(2, "packet"), v(3, "packet_size"), v(4, "ip_address")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        EVENTS = Collections.unmodifiableMap(events);
    }

    private TraceXMetadataCatalog() {
    }

    private static TraceXEventCategory t() {
        return TraceXEventCategory.THREADX;
    }

    private static TraceXEventCategory f() {
        return TraceXEventCategory.FILEX;
    }

    private static TraceXEventCategory n() {
        return TraceXEventCategory.NETX_DUO;
    }

    private static Alias a(int word, String name) {
        return new Alias(word, name, true);
    }

    private static Alias v(int word, String name) {
        return new Alias(word, name, false);
    }

    private static void add(Map<Integer, TraceXEventMetadata> events, int id,
            String name, TraceXEventCategory category, Alias... aliases) {
        events.put(id, new TraceXEventMetadata(id, name, category, aliases));
    }

    /**
     * Resolve semantic metadata without rejecting unfamiliar event IDs.
     *
     * @param normalizedId
     *            low-16-bit event ID
     * @return catalog entry or stable generic metadata
     */
    public static TraceXEventMetadata lookup(int normalizedId) {
        TraceXEventMetadata event = EVENTS.get(normalizedId);
        if (event != null) {
            return event;
        }
        TraceXEventCategory category = category(normalizedId);
        String prefix;
        switch (category) {
        case INVALID:
            prefix = "INVALID_EVENT"; //$NON-NLS-1$
            break;
        case THREADX:
            prefix = "THREADX_EVENT"; //$NON-NLS-1$
            break;
        case FILEX:
            prefix = "FILEX_EVENT"; //$NON-NLS-1$
            break;
        case NETX_DUO:
            prefix = "NETX_DUO_EVENT"; //$NON-NLS-1$
            break;
        case USBX:
            prefix = "USBX_EVENT"; //$NON-NLS-1$
            break;
        case RESERVED:
            prefix = "RESERVED_EVENT"; //$NON-NLS-1$
            break;
        case USER:
            prefix = "USER_EVENT"; //$NON-NLS-1$
            break;
        case UNKNOWN:
        default:
            prefix = "UNKNOWN_EVENT"; //$NON-NLS-1$
            break;
        }
        return new TraceXEventMetadata(normalizedId,
                String.format("%s_%04X", prefix, normalizedId), category); //$NON-NLS-1$
    }

    static TraceXEventMetadata invalid() {
        return INVALID;
    }

    private static TraceXEventCategory category(int id) {
        if (id < 0) {
            return TraceXEventCategory.UNKNOWN;
        }
        if (id == 0) {
            return TraceXEventCategory.INVALID;
        }
        if (id <= 199) {
            return TraceXEventCategory.THREADX;
        }
        if (id <= 299) {
            return TraceXEventCategory.FILEX;
        }
        if (id <= 599) {
            return TraceXEventCategory.NETX_DUO;
        }
        if (id <= 999) {
            return TraceXEventCategory.USBX;
        }
        if (id <= 4095) {
            return TraceXEventCategory.RESERVED;
        }
        if (id <= 0xffff) {
            return TraceXEventCategory.USER;
        }
        return TraceXEventCategory.UNKNOWN;
    }

    /** @return immutable catalog entries used by the bundled demo traces */
    public static Map<Integer, TraceXEventMetadata> getCatalog() {
        return EVENTS;
    }
}
