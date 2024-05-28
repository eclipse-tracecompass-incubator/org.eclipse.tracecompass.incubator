/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.uftrace.core.trace;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.collect.PeekingIterator;

/**
 *
 * The data (.dat) file
 *
 * The data file contains actual trace data (record) for each task so the task
 * id (tid) will be used as a file name. The data is two 64-bit numbers - first
 * is a timestamp in nsec and second consists of 2-bit type, 1-bit marker, 3-bit
 * magic, 10-bit depth and 48-bit address.
 *
 * The type is one of 'ENTRY', 'EXIT', 'EVENT' or 'LOST'. The 'ENTRY' and 'EXIT'
 * types are for function tracing and 'EVENT' type is reserved for event tracing
 * like kernel-level tracepoint or user-level SDT. The 1-bit marker is whether
 * this record has additional data (like argument or return value). The 3-bit
 * magic is for data integrity and it should have a value of 5 (or 0b101). The
 * 10-bit depth shows the function call depth (or level). And finally 48-bit
 * address is to identify function (symbol); it's ok as most 64-bit systems only
 * use 48-bit address space for now.
 *
 * @author Matthew Khouzam
 *
 */
public class DatParser implements Iterable<DatEvent> {

    private long fUtcOffset;
    private final File fFile;
    private final long fStart;

    /**
     * Data event parser
     *
     * @param file
     *            file to read
     */
    public DatParser(File file) {
        this(file, 0, 0);
    }

    /**
     * Data event parser
     *
     * @param file
     *            file to read
     * @param start
     *            offset in the file
     * @param utcOffset
     *            The offset to UTC time
     */
    public DatParser(File file, long start, long utcOffset) {
        fFile = file;
        fStart = start;
        fUtcOffset = utcOffset;
    }

    @Override
    public PeekingIterator<DatEvent> iterator() {

        try (FileChannel fc = FileChannel.open(fFile.toPath(), StandardOpenOption.READ)) {
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, fStart, fc.size());
            if (bb == null) {
                throw new IllegalStateException("cannot create a byte buffer!"); //$NON-NLS-1$
            }
            return new PeekingIterator<DatEvent>() {

                DatEvent fCurrent = null;

                @Override
                public DatEvent next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException("no more data"); //$NON-NLS-1$
                    }
                    fCurrent = DatEvent.create(bb,
                            NumberUtils.toInt(fFile.getName().substring(0, fFile.getName().length() - 4)), fUtcOffset);
                    return fCurrent;
                }

                @Override
                public boolean hasNext() {
                    return bb.remaining() > Long.BYTES * 2;
                }

                @Override
                public DatEvent peek() {
                    if (fCurrent == null && hasNext()) {
                        return next();
                    }
                    return fCurrent;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("can't"); //$NON-NLS-1$
                }
            };
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Set new utc offset
     *
     * @param utcOffset
     *            the offset in ns
     */
    public void setUtcOffset(long utcOffset) {
        fUtcOffset = utcOffset;
    }
}
