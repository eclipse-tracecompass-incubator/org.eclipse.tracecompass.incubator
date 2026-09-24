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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalLong;

import org.eclipse.jdt.annotation.Nullable;

/** Immutable lossless internal representation of one TraceX event record. */
public final class TraceXEvent {

    /** Stable raw context field name. */
    public static final String FIELD_CONTEXT = "context"; //$NON-NLS-1$
    /** Stable context address field name. */
    public static final String FIELD_CONTEXT_ADDRESS = "context_address"; //$NON-NLS-1$
    /** Stable resolved context name field name. */
    public static final String FIELD_CONTEXT_NAME = "context_name"; //$NON-NLS-1$
    /** Stable priority field name. */
    public static final String FIELD_PRIORITY = "priority"; //$NON-NLS-1$
    /** Stable preemption-threshold field name. */
    public static final String FIELD_PREEMPTION_THRESHOLD = "preemption_threshold"; //$NON-NLS-1$
    /** Stable raw priority word field name. */
    public static final String FIELD_RAW_PRIORITY = "raw_priority"; //$NON-NLS-1$
    /** Stable raw event-ID field name. */
    public static final String FIELD_RAW_EVENT_ID = "raw_event_id"; //$NON-NLS-1$
    /** Stable normalized event-ID field name. */
    public static final String FIELD_EVENT_ID = "event_id"; //$NON-NLS-1$
    /** Stable raw timestamp field name. */
    public static final String FIELD_RAW_TIMESTAMP = "raw_timestamp"; //$NON-NLS-1$
    /** Stable relative-tick placeholder field name. */
    public static final String FIELD_RELATIVE_TICKS = "relative_ticks"; //$NON-NLS-1$

    private static final int RECORD_SIZE = 32;
    private static final long INVALID_EVENT = 0xFFFFFFFFL;

    private final long fRawContextAddress;
    private final long fRawPriority;
    private final long fRawEventId;
    private final int fNormalizedEventId;
    private final long fRawTimestamp;
    private final long[] fInformationWords;
    private final TraceXContext fContext;
    private final TraceXEventMetadata fMetadata;
    private final Map<String, TraceXEventField> fAliases;
    private final Map<String, Object> fFields;
    private final OptionalLong fRelativeTicks;

    private TraceXEvent(long rawContextAddress, long rawPriority,
            long rawEventId, long rawTimestamp, long[] informationWords,
            TraceXRegistry registry, OptionalLong relativeTicks) {
        fRawContextAddress = rawContextAddress;
        fRawPriority = rawPriority;
        fRawEventId = rawEventId;
        fNormalizedEventId = (int) (rawEventId & 0xffff);
        fRawTimestamp = rawTimestamp;
        fInformationWords = informationWords;
        fRelativeTicks = relativeTicks;
        boolean unused = rawEventId == INVALID_EVENT;
        fContext = TraceXContext.decode(rawContextAddress, rawPriority, unused, registry);
        fMetadata = unused ? TraceXMetadataCatalog.invalid()
                : TraceXMetadataCatalog.lookup(fNormalizedEventId);
        fAliases = createAliases(registry);
        fFields = createFields();
    }

    /**
     * Decode one fixed-size event record.
     *
     * @param bytes
     *            bytes positioned at the event record
     * @param order
     *            target byte order
     * @param registry
     *            decoded registry used for name resolution
     * @return immutable event
     * @throws TraceXFormatException
     *             if the event record is truncated
     */
    public static TraceXEvent decode(ByteBuffer bytes, ByteOrder order,
            TraceXRegistry registry) throws TraceXFormatException {
        if (bytes.remaining() < RECORD_SIZE) {
            throw new TraceXFormatException("Truncated TraceX event record"); //$NON-NLS-1$
        }
        ByteBuffer record = bytes.slice().order(order);
        long context = unsignedInt(record);
        long priority = unsignedInt(record);
        long eventId = unsignedInt(record);
        long timestamp = unsignedInt(record);
        long[] information = { unsignedInt(record), unsignedInt(record),
                unsignedInt(record), unsignedInt(record) };
        return new TraceXEvent(context, priority, eventId, timestamp,
                information, registry, OptionalLong.empty());
    }

    private static long unsignedInt(ByteBuffer record) {
        return Integer.toUnsignedLong(record.getInt());
    }

    private Map<String, TraceXEventField> createAliases(TraceXRegistry registry) {
        Map<String, TraceXEventField> aliases = new LinkedHashMap<>();
        for (TraceXEventMetadata.Alias definition : fMetadata.getAliases()) {
            long rawValue = fInformationWords[definition.getWord() - 1];
            TraceXRegistryRecord object = definition.isAddress()
                    ? registry.find(rawValue) : null;
            aliases.put(definition.getName(),
                    new TraceXEventField(definition, rawValue, object));
        }
        return Collections.unmodifiableMap(aliases);
    }

    private Map<String, Object> createFields() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put(FIELD_CONTEXT, fContext.getKind().name());
        fields.put(FIELD_CONTEXT_ADDRESS, fContext.getFormattedAddress());
        putIfPresent(fields, FIELD_CONTEXT_NAME, fContext.getName());
        putIfPresent(fields, FIELD_PRIORITY, fContext.getPriority());
        putIfPresent(fields, FIELD_PREEMPTION_THRESHOLD,
                fContext.getPreemptionThreshold());
        fields.put(FIELD_RAW_PRIORITY, fRawPriority);
        fields.put(FIELD_RAW_EVENT_ID, fRawEventId);
        fields.put(FIELD_EVENT_ID, fNormalizedEventId);
        fields.put(FIELD_RAW_TIMESTAMP, fRawTimestamp);
        fields.put(FIELD_RELATIVE_TICKS, fRelativeTicks);
        for (int i = 0; i < fInformationWords.length; i++) {
            fields.put("raw_info_" + (i + 1), fInformationWords[i]); //$NON-NLS-1$
        }
        for (TraceXEventField alias : fAliases.values()) {
            fields.put(alias.getName(), alias.getRawValue());
            String address = alias.getFormattedAddress();
            if (address != null) {
                fields.put(alias.getName() + "_address", address); //$NON-NLS-1$
            }
            TraceXRegistryRecord object = alias.getResolvedObject();
            if (object != null) {
                fields.put(alias.getName() + "_name", object.getName()); //$NON-NLS-1$
            }
        }
        return Collections.unmodifiableMap(fields);
    }

    private static void putIfPresent(Map<String, Object> fields, String name,
            @Nullable Object value) {
        if (value != null) {
            fields.put(name, value);
        }
    }

    /** @return unsigned raw event ID, including high flag bits */
    public long getRawEventId() {
        return fRawEventId;
    }

    /** @return normalized low-16-bit event ID */
    public int getNormalizedEventId() {
        return fNormalizedEventId;
    }

    /** @return semantic metadata or stable generic metadata */
    public TraceXEventMetadata getMetadata() {
        return fMetadata;
    }

    /** @return decoded context */
    public TraceXContext getContext() {
        return fContext;
    }

    /** @return unsigned raw context address */
    public long getRawContextAddress() {
        return fRawContextAddress;
    }

    /** @return unsigned raw priority/current-thread word */
    public long getRawPriority() {
        return fRawPriority;
    }

    /** @return unsigned raw timestamp */
    public long getRawTimestamp() {
        return fRawTimestamp;
    }

    /** @return relative ticks, deliberately unset until time normalization */
    public OptionalLong getRelativeTicks() {
        return fRelativeTicks;
    }

    /**
     * Read an unchanged unsigned raw information word.
     *
     * @param word
     *            one-based word number from 1 through 4
     * @return raw unsigned value
     */
    public long getInformationWord(int word) {
        if (word < 1 || word > fInformationWords.length) {
            throw new IllegalArgumentException("Information word must be between 1 and 4"); //$NON-NLS-1$
        }
        return fInformationWords[word - 1];
    }

    /** @return semantic aliases referencing the raw information words */
    public Map<String, TraceXEventField> getAliases() {
        return fAliases;
    }

    /** @return immutable Trace Compass-style field map */
    public Map<String, Object> getFields() {
        return fFields;
    }

    /** Format an unsigned 32-bit address consistently. */
    static String formatAddress(long address) {
        return String.format("0x%08X", address & 0xffffffffL); //$NON-NLS-1$
    }
}
