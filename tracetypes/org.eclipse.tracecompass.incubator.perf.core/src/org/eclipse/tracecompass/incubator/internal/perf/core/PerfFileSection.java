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

/**
 * Immutable {@code perf_file_section} (offset, size) pair. Used both in the
 * file header to locate the attrs / data / event_types sections and in the
 * feature index at the end of the file.
 */
public final class PerfFileSection {

    /** Size in bytes of a single {@code perf_file_section} on disk. */
    public static final int SIZE = 16;

    private final long fOffset;
    private final long fSize;

    /**
     * Constructor
     *
     * @param offset
     *            byte offset of the section in the file
     * @param size
     *            size of the section, in bytes
     */
    public PerfFileSection(long offset, long size) {
        fOffset = offset;
        fSize = size;
    }

    /**
     * @return the byte offset of the section
     */
    public long getOffset() {
        return fOffset;
    }

    /**
     * @return the byte size of the section
     */
    public long getSize() {
        return fSize;
    }

    @Override
    public String toString() {
        return "[offset=" + fOffset + ", size=" + fSize + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
