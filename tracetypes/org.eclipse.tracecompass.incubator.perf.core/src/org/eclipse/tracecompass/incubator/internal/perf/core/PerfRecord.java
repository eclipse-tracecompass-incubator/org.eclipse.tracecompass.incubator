/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.perf.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

/**
 * A parsed {@code perf.data} record.
 *
 * A record is the combination of a {@code perf_event_header} (type + misc +
 * size) and a type-dependent body. Bodies are exposed as a named-field map to
 * keep the parser simple and to avoid a class explosion for every record
 * type.
 */
public final class PerfRecord {

    private final int fType;
    private final int fMisc;
    private final int fSize;
    private final long fOffset;
    private final long fTimestamp;
    private final Map<String, Object> fFields;

    /**
     * Constructor
     *
     * @param type
     *            PERF_RECORD_* value
     * @param misc
     *            misc flags
     * @param size
     *            record size, including the 8-byte header
     * @param offset
     *            byte offset of the record in the data stream (0-based
     *            within the data section)
     * @param timestamp
     *            timestamp in nanoseconds, if known (0 otherwise)
     * @param fields
     *            parsed body fields, keyed by name (insertion-ordered)
     */
    public PerfRecord(int type, int misc, int size, long offset,
            long timestamp, Map<String, Object> fields) {
        fType = type;
        fMisc = misc;
        fSize = size;
        fOffset = offset;
        fTimestamp = timestamp;
        fFields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    /**
     * @return the record type (PERF_RECORD_*)
     */
    public int getType() {
        return fType;
    }

    /**
     * @return the misc flags
     */
    public int getMisc() {
        return fMisc;
    }

    /**
     * @return the record size, including the 8-byte header
     */
    public int getSize() {
        return fSize;
    }

    /**
     * @return the byte offset of the record in the data stream
     */
    public long getOffset() {
        return fOffset;
    }

    /**
     * @return the timestamp in nanoseconds, or 0 if unknown
     */
    public long getTimestamp() {
        return fTimestamp;
    }

    /**
     * @return the parsed body fields
     */
    public Map<String, Object> getFields() {
        return fFields;
    }

    /**
     * Look up a named field, typed.
     *
     * @param name
     *            the field name
     * @return the field value, or {@code null} if absent
     */
    public @Nullable Object getField(String name) {
        return fFields.get(name);
    }

    /**
     * @return the CPU mode encoded in the misc flags (one of
     *         {@link PerfConstants#PERF_RECORD_MISC_KERNEL},
     *         {@link PerfConstants#PERF_RECORD_MISC_USER}, etc.)
     */
    public int getCpuMode() {
        return fMisc & PerfConstants.PERF_RECORD_MISC_CPUMODE_MASK;
    }
}
