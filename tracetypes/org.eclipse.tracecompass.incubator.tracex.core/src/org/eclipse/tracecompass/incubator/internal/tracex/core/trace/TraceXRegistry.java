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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

/** Immutable decoded TraceX object registry and address lookup. */
public final class TraceXRegistry {

    private final List<TraceXRegistryRecord> fRecords;
    private final Map<Long, TraceXRegistryRecord> fObjectsByAddress;
    private final List<String> fDiagnostics;

    private TraceXRegistry(List<TraceXRegistryRecord> records,
            Map<Long, TraceXRegistryRecord> objectsByAddress,
            List<String> diagnostics) {
        fRecords = Collections.unmodifiableList(records);
        fObjectsByAddress = Collections.unmodifiableMap(objectsByAddress);
        fDiagnostics = Collections.unmodifiableList(diagnostics);
    }

    /**
     * Decode every registry slot using the validated layout.
     *
     * @param source
     *            position-independent trace source
     * @param layout
     *            validated trace layout
     * @return immutable registry
     * @throws IOException
     *             if a complete registry record cannot be read
     */
    public static TraceXRegistry decode(TraceXDataSource source,
            TraceXLayout layout) throws IOException {
        long encodedSize = layout.getRegistryEntrySize();
        int entrySize = (int) encodedSize;
        int nameSize = layout.getRegistryNameSize();

        List<TraceXRegistryRecord> records = new ArrayList<>();
        Map<Long, TraceXRegistryRecord> lookup = new LinkedHashMap<>();
        List<String> diagnostics = new ArrayList<>();
        long offset = layout.getRegistryStartOffset();
        for (long index = 0; index < layout.getRegistryCapacity(); index++) {
            byte[] raw = readRecord(source, offset, entrySize, index);
            TraceXRegistryRecord record = TraceXRegistryRecord.decode(index, raw,
                    nameSize, layout.getByteOrder());
            records.add(record);
            if (record.isValid()) {
                @Nullable TraceXRegistryRecord previous = lookup.get(record.getAddress());
                if (previous != null) {
                    diagnostics.add("Duplicate object address " + record.getFormattedAddress() //$NON-NLS-1$
                            + " at registry indexes " + previous.getIndex() + " and " + index); //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    lookup.put(record.getAddress(), record);
                }
            }
            offset = TraceXCheckedMath.add(offset, encodedSize,
                    "Object registry read position"); //$NON-NLS-1$
        }
        return new TraceXRegistry(records, lookup, diagnostics);
    }

    private static byte[] readRecord(TraceXDataSource source, long offset,
            int entrySize, long index) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(entrySize);
        long position = offset;
        while (buffer.hasRemaining()) {
            int count = source.read(buffer, position);
            if (count <= 0) {
                throw new TraceXFormatException("Truncated object registry record " + index); //$NON-NLS-1$
            }
            position = TraceXCheckedMath.add(position, count,
                    "Object registry read position"); //$NON-NLS-1$
        }
        return buffer.array();
    }

    /** @return every slot in physical registry order, including unused slots */
    public List<TraceXRegistryRecord> getRecords() {
        return fRecords;
    }

    /**
     * Find the first valid physical record with an address.
     *
     * Available records participate because their retained metadata can resolve
     * events captured before an object was unregistered.
     *
     * @param address
     *            unsigned 32-bit target address
     * @return matching record, or {@code null}
     */
    public @Nullable TraceXRegistryRecord find(long address) {
        return fObjectsByAddress.get(address);
    }

    /** @return duplicate-address diagnostics in encounter order */
    public List<String> getDiagnostics() {
        return fDiagnostics;
    }
}
