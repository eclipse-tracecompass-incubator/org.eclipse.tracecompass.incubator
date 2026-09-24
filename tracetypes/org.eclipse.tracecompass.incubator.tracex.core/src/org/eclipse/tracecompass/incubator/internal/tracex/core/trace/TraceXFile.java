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
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Owned TraceX input resource and its validated immutable layout.
 */
public final class TraceXFile implements AutoCloseable {

    private final TraceXDataSource fSource;
    private final TraceXLayout fLayout;
    private @Nullable TraceXRegistry fRegistry;
    private boolean fClosed;

    private TraceXFile(TraceXDataSource source, TraceXLayout layout) {
        fSource = source;
        fLayout = layout;
    }

    /**
     * Open and validate a TraceX file.
     *
     * @param path
     *            trace path
     * @return owned validated input
     * @throws IOException
     *             if input fails or the format is invalid
     */
    public static TraceXFile open(Path path) throws IOException {
        return open(new FileDataSource(FileChannel.open(path, StandardOpenOption.READ)));
    }

    /**
     * Validate an already-created source and take ownership of it.
     *
     * @param source
     *            position-independent source
     * @return owned validated input
     * @throws IOException
     *             if input fails or the format is invalid
     */
    public static TraceXFile open(TraceXDataSource source) throws IOException {
        Objects.requireNonNull(source);
        try {
            long fileSize = source.size();
            if (fileSize < 0) {
                throw new TraceXFormatException("TraceX source reports a negative size"); //$NON-NLS-1$
            }
            ByteBuffer buffer = ByteBuffer.allocate(TraceXControlHeader.SIZE);
            long position = 0;
            while (buffer.hasRemaining()) {
                int count = source.read(buffer, position);
                if (count <= 0) {
                    break;
                }
                position = TraceXCheckedMath.add(position, count, "Header read position"); //$NON-NLS-1$
            }
            buffer.flip();
            TraceXControlHeader header = TraceXControlHeader.decode(buffer);
            return new TraceXFile(source, TraceXLayout.validate(header, fileSize));
        } catch (IOException | RuntimeException | Error e) {
            closeAfterFailure(source, e);
            throw e;
        }
    }

    private static void closeAfterFailure(TraceXDataSource source, Throwable failure) {
        try {
            source.close();
        } catch (Throwable closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    /** @return validated immutable layout */
    public TraceXLayout getLayout() {
        return fLayout;
    }

    /**
     * Decode and cache the complete object registry.
     *
     * @return immutable registry
     * @throws IOException
     *             if the file is closed or a registry record is truncated
     */
    public synchronized TraceXRegistry getRegistry() throws IOException {
        requireOpen();
        TraceXRegistry registry = fRegistry;
        if (registry == null) {
            registry = TraceXRegistry.decode(fSource, fLayout);
            fRegistry = registry;
        }
        return registry;
    }

    /**
     * Decode an event at its physical buffer index.
     *
     * @param physicalIndex
     *            zero-based physical event index
     * @return immutable lossless event
     * @throws IOException
     *             if the index is invalid or the record cannot be read
     */
    public synchronized TraceXEvent readEvent(long physicalIndex) throws IOException {
        requireOpen();
        if (physicalIndex < 0 || physicalIndex >= fLayout.getEventCapacity()) {
            throw new IllegalArgumentException("Physical event index is outside the event area"); //$NON-NLS-1$
        }
        long relative = TraceXCheckedMath.multiply(physicalIndex,
                TraceXLayout.EVENT_RECORD_SIZE, "Physical event index"); //$NON-NLS-1$
        long offset = TraceXCheckedMath.add(fLayout.getEventStartOffset(),
                relative, "Physical event offset"); //$NON-NLS-1$
        ByteBuffer record = ByteBuffer.allocate(TraceXLayout.EVENT_RECORD_SIZE);
        long position = offset;
        while (record.hasRemaining()) {
            int count = fSource.read(record, position);
            if (count <= 0) {
                throw new TraceXFormatException("Truncated TraceX event record"); //$NON-NLS-1$
            }
            position = TraceXCheckedMath.add(position, count,
                    "Event read position"); //$NON-NLS-1$
        }
        record.flip();
        return TraceXEvent.decode(record, fLayout.getByteOrder(), getRegistry());
    }

    private void requireOpen() throws IOException {
        if (!isOpen()) {
            throw new IOException("TraceX file is closed"); //$NON-NLS-1$
        }
    }

    /** @return whether the owned source remains open */
    public synchronized boolean isOpen() {
        return !fClosed && fSource.isOpen();
    }

    @Override
    public synchronized void close() throws IOException {
        if (!fClosed) {
            fClosed = true;
            fSource.close();
        }
    }

    private static final class FileDataSource implements TraceXDataSource {

        private final FileChannel fChannel;

        private FileDataSource(FileChannel channel) {
            fChannel = channel;
        }

        @Override
        public long size() throws IOException {
            return fChannel.size();
        }

        @Override
        public int read(ByteBuffer destination, long position) throws IOException {
            return fChannel.read(destination, position);
        }

        @Override
        public boolean isOpen() {
            return fChannel.isOpen();
        }

        @Override
        public void close() throws IOException {
            fChannel.close();
        }
    }
}
