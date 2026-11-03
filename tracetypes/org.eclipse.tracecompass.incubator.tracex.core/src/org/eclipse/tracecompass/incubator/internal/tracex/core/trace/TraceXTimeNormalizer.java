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

import java.util.Objects;

/** Overflow-safe modular timer normalization and clock conversion. */
public final class TraceXTimeNormalizer {

    /** Largest supported target clock frequency. */
    public static final long MAX_TICKS_PER_SECOND = 1_000_000_000L;

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private TraceXTimeNormalizer() {
    }

    /**
     * Normalize raw timer words in authoritative logical order.
     *
     * @param rawTimestamps
     *            unsigned raw timer words
     * @param timerMask
     *            validated contiguous low-bit timer mask
     * @param direction
     *            trace-wide target timer direction
     * @return monotonically nondecreasing ticks starting at zero
     * @throws TraceXFormatException
     *             if accumulation exceeds the timestamp domain
     */
    public static long[] normalize(long[] rawTimestamps, long timerMask,
            TraceXTimerDirection direction) throws TraceXFormatException {
        Objects.requireNonNull(rawTimestamps);
        Objects.requireNonNull(direction);
        long[] relative = new long[rawTimestamps.length];
        for (int rank = 1; rank < rawTimestamps.length; rank++) {
            long delta = delta(rawTimestamps[rank - 1], rawTimestamps[rank],
                    timerMask, direction);
            relative[rank] = checkedAdd(relative[rank - 1], delta);
        }
        return relative;
    }

    /**
     * Add a timer delta without allowing signed timestamp overflow.
     *
     * @param accumulated
     *            accumulated nonnegative relative ticks
     * @param delta
     *            next nonnegative modular delta
     * @return checked sum
     * @throws TraceXFormatException
     *             if the sum exceeds the timestamp domain
     */
    public static long checkedAdd(long accumulated, long delta)
            throws TraceXFormatException {
        try {
            return Math.addExact(accumulated, delta);
        } catch (ArithmeticException e) {
            throw new TraceXFormatException(
                    "TraceX relative ticks exceed the signed 64-bit timestamp domain", e); //$NON-NLS-1$
        }
    }

    /**
     * Calculate one modular delta.
     *
     * @param previousRaw
     *            preceding unsigned raw timer word
     * @param currentRaw
     *            current unsigned raw timer word
     * @param timerMask
     *            validated contiguous low-bit timer mask
     * @param direction
     *            trace-wide timer direction
     * @return nonnegative modular delta
     */
    public static long delta(long previousRaw, long currentRaw, long timerMask,
            TraceXTimerDirection direction) {
        Objects.requireNonNull(direction);
        long previous = previousRaw & timerMask;
        long current = currentRaw & timerMask;
        if (previous == current) {
            return 0;
        }
        long range = timerMask + 1;
        if (direction == TraceXTimerDirection.INCREASING) {
            return current > previous ? current - previous
                    : range - previous + current;
        }
        return previous > current ? previous - current
                : range - current + previous;
    }

    /**
     * Parse a positive decimal target clock frequency.
     *
     * @param value
     *            exact configuration text
     * @return ticks per second
     * @throws IllegalArgumentException
     *             if the value is malformed or unsupported
     */
    public static long parseTicksPerSecond(String value) {
        if (value.isEmpty()) {
            throw invalidFrequency();
        }
        long result = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < '0' || character > '9') {
                throw invalidFrequency();
            }
            int digit = character - '0';
            if (result > (MAX_TICKS_PER_SECOND - digit) / 10) {
                throw invalidFrequency();
            }
            result = result * 10 + digit;
        }
        if (result == 0) {
            throw invalidFrequency();
        }
        return result;
    }

    private static IllegalArgumentException invalidFrequency() {
        return new IllegalArgumentException("Ticks per second must be a positive decimal integer no greater than " //$NON-NLS-1$
                + MAX_TICKS_PER_SECOND);
    }

    /**
     * Convert relative ticks to nanoseconds, rounding fractional nanoseconds
     * down.
     *
     * @param ticks
     *            nonnegative relative ticks
     * @param ticksPerSecond
     *            validated frequency
     * @return converted nanoseconds
     * @throws IllegalArgumentException
     *             if an argument is invalid or the result cannot be represented
     */
    public static long toNanoseconds(long ticks, long ticksPerSecond) {
        if (ticks < 0 || ticksPerSecond <= 0
                || ticksPerSecond > MAX_TICKS_PER_SECOND) {
            throw new IllegalArgumentException("Invalid tick conversion arguments"); //$NON-NLS-1$
        }
        long seconds = ticks / ticksPerSecond;
        long remainder = ticks % ticksPerSecond;
        try {
            long whole = Math.multiplyExact(seconds, NANOS_PER_SECOND);
            long fraction = Math.multiplyExact(remainder, NANOS_PER_SECOND)
                    / ticksPerSecond;
            return Math.addExact(whole, fraction);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "Converted TraceX timestamp exceeds the signed 64-bit nanosecond domain", e); //$NON-NLS-1$
        }
    }
}
