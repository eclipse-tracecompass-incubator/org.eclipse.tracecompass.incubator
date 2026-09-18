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

import java.nio.ByteOrder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable, fully validated layout of a 32-bit TraceX binary trace.
 */
public final class TraceXLayout {

    /** Size in bytes of one 32-bit TraceX event entry. */
    public static final int EVENT_RECORD_SIZE = 32;

    /** Published maximum number of events accepted by TraceX. */
    public static final int MAX_EVENT_CAPACITY = 32768;

    private static final int REGISTRY_FIXED_SIZE = 16;
    private static final int WORD_ALIGNMENT = 4;
    private static final long RESERVED2 = 0xAAAAAAAAL;
    private static final long RESERVED3 = 0xBBBBBBBBL;
    private static final long RESERVED4 = 0xCCCCCCCCL;

    private final ByteOrder fByteOrder;
    private final long fBaseAddress;
    private final long fTimerValidMask;
    private final long fFileSize;
    private final long fRegistryStartOffset;
    private final long fRegistryEndOffset;
    private final long fRegistryEntrySize;
    private final long fRegistryCapacity;
    private final long fEventStartOffset;
    private final long fEventEndOffset;
    private final long fEventCurrentOffset;
    private final long fEventCapacity;

    private TraceXLayout(TraceXControlHeader header, long fileSize,
            long registryStartOffset, long registryEndOffset, long registryEntrySize,
            long registryCapacity, long eventStartOffset, long eventEndOffset,
            long eventCurrentOffset, long eventCapacity) {
        fByteOrder = header.getByteOrder();
        fBaseAddress = header.getTraceBaseAddress();
        fTimerValidMask = header.getTimerValidMask();
        fFileSize = fileSize;
        fRegistryStartOffset = registryStartOffset;
        fRegistryEndOffset = registryEndOffset;
        fRegistryEntrySize = registryEntrySize;
        fRegistryCapacity = registryCapacity;
        fEventStartOffset = eventStartOffset;
        fEventEndOffset = eventEndOffset;
        fEventCurrentOffset = eventCurrentOffset;
        fEventCapacity = eventCapacity;
    }

    /**
     * Validate a decoded header and construct its layout.
     *
     * @param header
     *            decoded control header
     * @param fileSize
     *            actual file size
     * @return immutable validated layout
     * @throws TraceXFormatException
     *             if any signature, arithmetic, alignment, or range invariant
     *             fails
     */
    public static TraceXLayout validate(TraceXControlHeader header, long fileSize) throws TraceXFormatException {
        if (fileSize < TraceXControlHeader.SIZE) {
            throw new TraceXFormatException("TraceX file is shorter than the control header"); //$NON-NLS-1$
        }
        validateSignature(header);
        validateTimerMask(header.getTimerValidMask());
        validateWordAlignment(header);

        long base = header.getTraceBaseAddress();
        long registryStart = offset(header.getRegistryStartAddress(), base, "Registry start address"); //$NON-NLS-1$
        long registryEnd = offset(header.getRegistryEndAddress(), base, "Registry end address"); //$NON-NLS-1$
        long eventStart = offset(header.getEventStartAddress(), base, "Event start address"); //$NON-NLS-1$
        long eventEnd = offset(header.getEventEndAddress(), base, "Event end address"); //$NON-NLS-1$
        long eventCurrent = offset(header.getEventCurrentAddress(), base, "Current event address"); //$NON-NLS-1$

        if (registryStart != TraceXControlHeader.SIZE) {
            throw new TraceXFormatException("Object registry must start immediately after the 48-byte header"); //$NON-NLS-1$
        }
        int nameSize = header.getRegistryNameSize();
        if (nameSize == 0) {
            throw new TraceXFormatException("Object registry name size is zero"); //$NON-NLS-1$
        }
        long registryEntrySize = alignWord(TraceXCheckedMath.add(REGISTRY_FIXED_SIZE, nameSize,
                "Registry entry size")); //$NON-NLS-1$
        long registrySpan = TraceXCheckedMath.subtract(registryEnd, registryStart, "Registry range"); //$NON-NLS-1$
        if (registrySpan % registryEntrySize != 0) {
            throw new TraceXFormatException("Object registry range is not a whole number of entries"); //$NON-NLS-1$
        }
        long registryCapacity = registrySpan / registryEntrySize;
        long checkedRegistrySpan = TraceXCheckedMath.multiply(registryCapacity, registryEntrySize,
                "Registry capacity"); //$NON-NLS-1$
        registryEnd = TraceXCheckedMath.add(registryStart, checkedRegistrySpan,
                "Registry end offset"); //$NON-NLS-1$
        if (eventStart != registryEnd) {
            throw new TraceXFormatException("Event area must immediately follow the object registry"); //$NON-NLS-1$
        }

        long eventSpan = TraceXCheckedMath.subtract(eventEnd, eventStart, "Event range"); //$NON-NLS-1$
        if (eventSpan % EVENT_RECORD_SIZE != 0) {
            throw new TraceXFormatException("Event area is not aligned to 32-byte records"); //$NON-NLS-1$
        }
        long eventCapacity = eventSpan / EVENT_RECORD_SIZE;
        if (eventCapacity == 0) {
            throw new TraceXFormatException("Event area contains no records"); //$NON-NLS-1$
        }
        if (eventCapacity > MAX_EVENT_CAPACITY) {
            throw new TraceXFormatException("Event capacity exceeds the documented limit of 32768"); //$NON-NLS-1$
        }
        long checkedEventSpan = TraceXCheckedMath.multiply(eventCapacity, EVENT_RECORD_SIZE,
                "Event capacity"); //$NON-NLS-1$
        eventEnd = TraceXCheckedMath.add(eventStart, checkedEventSpan, "Event end offset"); //$NON-NLS-1$
        if (eventCurrent < eventStart || eventCurrent >= eventEnd) {
            throw new TraceXFormatException("Current event pointer is outside the event area"); //$NON-NLS-1$
        }
        long currentIndex = TraceXCheckedMath.subtract(eventCurrent, eventStart,
                "Current event offset"); //$NON-NLS-1$
        if (currentIndex % EVENT_RECORD_SIZE != 0) {
            throw new TraceXFormatException("Current event pointer is not record-aligned"); //$NON-NLS-1$
        }
        if (registryEnd > fileSize || eventEnd > fileSize) {
            throw new TraceXFormatException("TraceX data range extends beyond the file"); //$NON-NLS-1$
        }

        return new TraceXLayout(header, fileSize, registryStart, registryEnd,
                registryEntrySize, registryCapacity, eventStart, eventEnd,
                eventCurrent, eventCapacity);
    }

    private static void validateSignature(TraceXControlHeader header) throws TraceXFormatException {
        if (header.getReserved1() != 0 || header.getReserved2() != RESERVED2
                || header.getReserved3() != RESERVED3 || header.getReserved4() != RESERVED4) {
            throw new TraceXFormatException("TraceX control-header reserved signature mismatch"); //$NON-NLS-1$
        }
    }

    private static void validateTimerMask(long timerMask) throws TraceXFormatException {
        if (timerMask == 0 || (timerMask & (timerMask + 1)) != 0) {
            throw new TraceXFormatException("Timer valid mask is not a contiguous low-bit mask"); //$NON-NLS-1$
        }
    }

    private static void validateWordAlignment(TraceXControlHeader header) throws TraceXFormatException {
        requireWordAligned(header.getTraceBaseAddress(), "Trace base address"); //$NON-NLS-1$
        requireWordAligned(header.getRegistryStartAddress(), "Registry start address"); //$NON-NLS-1$
        requireWordAligned(header.getRegistryEndAddress(), "Registry end address"); //$NON-NLS-1$
        requireWordAligned(header.getEventStartAddress(), "Event start address"); //$NON-NLS-1$
        requireWordAligned(header.getEventEndAddress(), "Event end address"); //$NON-NLS-1$
        requireWordAligned(header.getEventCurrentAddress(), "Current event address"); //$NON-NLS-1$
    }

    private static void requireWordAligned(long address, String description) throws TraceXFormatException {
        if (address % WORD_ALIGNMENT != 0) {
            throw new TraceXFormatException(description + " is not 4-byte aligned"); //$NON-NLS-1$
        }
    }

    private static long alignWord(long value) throws TraceXFormatException {
        return TraceXCheckedMath.add(value, WORD_ALIGNMENT - 1, "Registry entry alignment") //$NON-NLS-1$
                & -WORD_ALIGNMENT;
    }

    private static long offset(long address, long base, String description) throws TraceXFormatException {
        return TraceXCheckedMath.subtract(address, base, description);
    }

    /** @return decoded byte order */
    public ByteOrder getByteOrder() {
        return fByteOrder;
    }

    /** @return unsigned 32-bit trace base address represented as a long */
    public long getBaseAddress() {
        return fBaseAddress;
    }

    /** @return unsigned 32-bit timer-valid mask represented as a long */
    public long getTimerValidMask() {
        return fTimerValidMask;
    }

    /** @return actual file size */
    public long getFileSize() {
        return fFileSize;
    }

    /** @return validated object-registry start offset */
    public long getRegistryStartOffset() {
        return fRegistryStartOffset;
    }

    /** @return validated object-registry end offset */
    public long getRegistryEndOffset() {
        return fRegistryEndOffset;
    }

    /** @return validated size of one object-registry entry */
    public long getRegistryEntrySize() {
        return fRegistryEntrySize;
    }

    /** @return validated object-registry capacity */
    public long getRegistryCapacity() {
        return fRegistryCapacity;
    }

    /** @return validated event-area start offset */
    public long getEventStartOffset() {
        return fEventStartOffset;
    }

    /** @return validated event-area end offset */
    public long getEventEndOffset() {
        return fEventEndOffset;
    }

    /** @return validated current-event offset */
    public long getEventCurrentOffset() {
        return fEventCurrentOffset;
    }

    /** @return validated event-record capacity */
    public long getEventCapacity() {
        return fEventCapacity;
    }

    /**
     * Create the stable trace properties exposed to Trace Compass clients.
     *
     * @return immutable property map
     */
    public Map<String, String> getProperties() {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("byte_order", fByteOrder.toString()); //$NON-NLS-1$
        properties.put("base_address", hex32(fBaseAddress)); //$NON-NLS-1$
        properties.put("timer_valid_mask", hex32(fTimerValidMask)); //$NON-NLS-1$
        properties.put("registry_capacity", Long.toString(fRegistryCapacity)); //$NON-NLS-1$
        properties.put("registry_entry_size", Long.toString(fRegistryEntrySize)); //$NON-NLS-1$
        properties.put("registry_start_offset", Long.toString(fRegistryStartOffset)); //$NON-NLS-1$
        properties.put("registry_end_offset", Long.toString(fRegistryEndOffset)); //$NON-NLS-1$
        properties.put("event_capacity", Long.toString(fEventCapacity)); //$NON-NLS-1$
        properties.put("event_start_offset", Long.toString(fEventStartOffset)); //$NON-NLS-1$
        properties.put("event_end_offset", Long.toString(fEventEndOffset)); //$NON-NLS-1$
        properties.put("event_current_offset", Long.toString(fEventCurrentOffset)); //$NON-NLS-1$
        return Collections.unmodifiableMap(properties);
    }

    private static String hex32(long value) {
        return String.format("0x%08X", value); //$NON-NLS-1$
    }
}
