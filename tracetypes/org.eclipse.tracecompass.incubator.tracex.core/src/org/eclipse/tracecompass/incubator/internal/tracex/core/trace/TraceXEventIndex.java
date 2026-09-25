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
import java.io.InterruptedIOException;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Immutable oldest-to-newest mapping of logical ranks to physical TraceX
 * event records.
 */
public final class TraceXEventIndex {

    private static final long EMPTY_CONTEXT = 0;

    private final TraceXLayout fLayout;
    private final int[] fPhysicalIndices;

    private TraceXEventIndex(TraceXLayout layout, int[] physicalIndices) {
        fLayout = layout;
        fPhysicalIndices = physicalIndices;
    }

    /**
     * Build an index while honoring thread interruption.
     *
     * @param input
     *            validated TraceX input
     * @return immutable logical event index
     * @throws IOException
     *             if an event marker cannot be read, the buffer layout is
     *             inconsistent, or the operation is interrupted
     */
    public static TraceXEventIndex build(TraceXFile input) throws IOException {
        return build(input, () -> Thread.currentThread().isInterrupted());
    }

    /**
     * Build an index with an explicit cancellation source.
     *
     * @param input
     *            validated TraceX input
     * @param cancelled
     *            cancellation predicate checked around every physical record
     * @return immutable logical event index
     * @throws IOException
     *             if an event marker cannot be read, the buffer layout is
     *             inconsistent, or the operation is cancelled
     */
    public static TraceXEventIndex build(TraceXFile input,
            BooleanSupplier cancelled) throws IOException {
        Objects.requireNonNull(input);
        Objects.requireNonNull(cancelled);
        TraceXLayout layout = input.getLayout();
        int capacity = Math.toIntExact(layout.getEventCapacity());
        int firstEmpty = -1;
        for (int physical = 0; physical < capacity; physical++) {
            checkCancelled(cancelled);
            boolean empty = input.readEventContext(physical) == EMPTY_CONTEXT;
            if (empty) {
                if (firstEmpty < 0) {
                    firstEmpty = physical;
                }
            } else if (firstEmpty >= 0) {
                throw new TraceXFormatException(
                        "Non-empty TraceX event follows an empty record"); //$NON-NLS-1$
            }
        }
        checkCancelled(cancelled);

        int current = Math.toIntExact((layout.getEventCurrentOffset()
                - layout.getEventStartOffset()) / TraceXLayout.EVENT_RECORD_SIZE);
        if (firstEmpty >= 0) {
            if (current != firstEmpty) {
                throw new TraceXFormatException(
                        "Current event pointer does not identify the first empty record"); //$NON-NLS-1$
            }
            int[] partial = new int[firstEmpty];
            for (int rank = 0; rank < firstEmpty; rank++) {
                partial[rank] = rank;
            }
            return new TraceXEventIndex(layout, partial);
        }

        int[] circular = new int[capacity];
        for (int rank = 0; rank < capacity; rank++) {
            circular[rank] = (current + rank) % capacity;
        }
        return new TraceXEventIndex(layout, circular);
    }

    private static void checkCancelled(BooleanSupplier cancelled)
            throws InterruptedIOException {
        if (cancelled.getAsBoolean()) {
            throw new InterruptedIOException("TraceX event indexing was cancelled"); //$NON-NLS-1$
        }
    }

    /** @return number of logical events, which may be zero */
    public long getEventCount() {
        return fPhysicalIndices.length;
    }

    /**
     * Clamp an arbitrary requested rank to the inclusive start/exclusive end
     * context range.
     *
     * @param rank
     *            requested logical rank
     * @return rank in {@code [0, eventCount]}
     */
    public long clampRank(long rank) {
        return Math.max(0, Math.min(rank, getEventCount()));
    }

    /**
     * Convert a seek ratio to an inclusive start/exclusive end rank.
     * NaN and negative values select the start; positive infinity and values
     * above one select the end.
     *
     * @param ratio
     *            requested ratio
     * @return rank in {@code [0, eventCount]}
     */
    public long rankForRatio(double ratio) {
        if (Double.isNaN(ratio) || ratio <= 0.0) {
            return 0;
        }
        long count = getEventCount();
        if (ratio >= 1.0) {
            return count;
        }
        return Math.min(count, (long) Math.floor(ratio * count));
    }

    /**
     * Resolve one logical rank to its physical record index.
     *
     * @param rank
     *            logical event rank
     * @return physical record index
     */
    public int getPhysicalIndex(long rank) {
        if (rank < 0 || rank >= fPhysicalIndices.length) {
            throw new IllegalArgumentException("Logical event rank is outside the trace"); //$NON-NLS-1$
        }
        return fPhysicalIndices[(int) rank];
    }

    /**
     * Resolve one logical rank to its physical file offset.
     *
     * @param rank
     *            logical event rank
     * @return physical byte offset
     */
    public long getPhysicalOffset(long rank) {
        int physical = getPhysicalIndex(rank);
        return fLayout.getEventStartOffset()
                + (long) physical * TraceXLayout.EVENT_RECORD_SIZE;
    }

    /**
     * Read one indexed event without changing any shared channel position.
     *
     * @param input
     *            source that produced this index
     * @param rank
     *            logical event rank
     * @return event annotated with logical rank and physical offset
     * @throws IOException
     *             if the record cannot be read
     */
    public TraceXEvent readEvent(TraceXFile input, long rank) throws IOException {
        Objects.requireNonNull(input);
        int physical = getPhysicalIndex(rank);
        return input.readEvent(physical).withPosition(rank, getPhysicalOffset(rank));
    }
}
