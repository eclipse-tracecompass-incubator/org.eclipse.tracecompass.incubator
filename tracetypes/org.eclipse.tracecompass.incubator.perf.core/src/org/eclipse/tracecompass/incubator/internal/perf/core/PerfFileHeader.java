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

import java.nio.ByteOrder;

/**
 * Parsed form of {@code perf_file_header} (non-piped) or
 * {@code perf_pipe_file_header} (piped).
 */
public final class PerfFileHeader {

    private final long fMagic;
    private final long fSize;
    private final long fAttrSize;
    private final PerfFileSection fAttrs;
    private final PerfFileSection fData;
    private final PerfFileSection fEventTypes;
    private final long[] fFeatures;
    private final ByteOrder fOrder;
    private final boolean fPiped;

    /**
     * Full file header (non-piped).
     *
     * @param magic
     *            magic number as read (already corrected for endianness)
     * @param size
     *            header struct size
     * @param attrSize
     *            size of a single {@code perf_file_attr}
     * @param attrs
     *            attrs section
     * @param data
     *            data section
     * @param eventTypes
     *            legacy event_types section
     * @param features
     *            adds_features bitmap (4 u64 words)
     * @param order
     *            byte order detected while parsing
     */
    public PerfFileHeader(long magic, long size, long attrSize,
            PerfFileSection attrs, PerfFileSection data, PerfFileSection eventTypes,
            long[] features, ByteOrder order) {
        fMagic = magic;
        fSize = size;
        fAttrSize = attrSize;
        fAttrs = attrs;
        fData = data;
        fEventTypes = eventTypes;
        fFeatures = features;
        fOrder = order;
        fPiped = false;
    }

    /**
     * Piped file header.
     *
     * @param magic
     *            magic number
     * @param size
     *            struct size (16 for piped headers)
     * @param order
     *            byte order
     */
    public PerfFileHeader(long magic, long size, ByteOrder order) {
        fMagic = magic;
        fSize = size;
        fAttrSize = 0;
        fAttrs = new PerfFileSection(0, 0);
        fData = new PerfFileSection(0, 0);
        fEventTypes = new PerfFileSection(0, 0);
        fFeatures = new long[4];
        fOrder = order;
        fPiped = true;
    }

    /**
     * @return the magic number
     */
    public long getMagic() {
        return fMagic;
    }

    /**
     * @return the header struct size
     */
    public long getSize() {
        return fSize;
    }

    /**
     * @return the size in bytes of a single perf_file_attr entry
     */
    public long getAttrSize() {
        return fAttrSize;
    }

    /**
     * @return the attrs section
     */
    public PerfFileSection getAttrs() {
        return fAttrs;
    }

    /**
     * @return the data section
     */
    public PerfFileSection getData() {
        return fData;
    }

    /**
     * @return the (legacy) event_types section
     */
    public PerfFileSection getEventTypes() {
        return fEventTypes;
    }

    /**
     * Return the adds_features bitmap, as four 64-bit words.
     *
     * @return the feature bitmap
     */
    public long[] getFeatures() {
        return fFeatures;
    }

    /**
     * Test whether a given feature bit is set.
     *
     * @param bit
     *            the bit index
     * @return true if set
     */
    public boolean hasFeature(int bit) {
        if (bit < 0 || bit >= PerfConstants.HEADER_FEAT_BITS) {
            return false;
        }
        int word = bit >>> 6;
        int shift = bit & 63;
        return ((fFeatures[word] >>> shift) & 1L) != 0L;
    }

    /**
     * @return the byte order in which this file is encoded
     */
    public ByteOrder getOrder() {
        return fOrder;
    }

    /**
     * @return true if the header is a piped header
     */
    public boolean isPiped() {
        return fPiped;
    }
}
