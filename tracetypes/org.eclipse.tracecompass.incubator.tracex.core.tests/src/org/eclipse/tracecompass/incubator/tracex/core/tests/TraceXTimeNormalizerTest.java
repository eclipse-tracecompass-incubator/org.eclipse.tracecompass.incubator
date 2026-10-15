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

package org.eclipse.tracecompass.incubator.tracex.core.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXFormatException;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTimeNormalizer;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXTimerDirection;
import org.junit.Test;

/** Tests modular timer normalization and safe clock conversion. */
public class TraceXTimeNormalizerTest {

    /** Tests increasing timers without wrap, with wrap, and across two wraps. */
    @Test
    public void testIncreasingNormalization() throws Exception {
        assertArrayEquals(new long[0], normalize(new long[0],
                TraceXTimerDirection.INCREASING));
        assertArrayEquals(new long[] { 0 }, normalize(new long[] { 7 },
                TraceXTimerDirection.INCREASING));
        assertArrayEquals(new long[] { 0, 3, 3, 12 }, normalize(
                new long[] { 2, 5, 5, 14 }, TraceXTimerDirection.INCREASING));
        assertArrayEquals(new long[] { 0, 3 }, normalize(
                new long[] { 14, 1 }, TraceXTimerDirection.INCREASING));
        assertArrayEquals(new long[] { 0, 3, 17, 18, 20 }, normalize(
                new long[] { 14, 1, 15, 0, 2 },
                TraceXTimerDirection.INCREASING));
    }

    /** Tests decreasing timers without wrap, with wrap, and across two wraps. */
    @Test
    public void testDecreasingNormalization() throws Exception {
        assertArrayEquals(new long[] { 0, 4, 4, 12 }, normalize(
                new long[] { 14, 10, 10, 2 },
                TraceXTimerDirection.DECREASING));
        assertArrayEquals(new long[] { 0, 3 }, normalize(
                new long[] { 1, 14 }, TraceXTimerDirection.DECREASING));
        assertArrayEquals(new long[] { 0, 3, 17, 18, 20 }, normalize(
                new long[] { 1, 14, 0, 15, 13 },
                TraceXTimerDirection.DECREASING));
    }

    /** Tests masking and checked accumulation at the timestamp boundary. */
    @Test
    public void testMaskAndAccumulationLimits() throws Exception {
        assertEquals(2, TraceXTimeNormalizer.delta(0xF0E, 0xF00, 0xF,
                TraceXTimerDirection.INCREASING));
        assertEquals(2, TraceXTimeNormalizer.delta(0xF00, 0xF0E, 0xF,
                TraceXTimerDirection.DECREASING));
        assertEquals(Long.MAX_VALUE,
                TraceXTimeNormalizer.checkedAdd(Long.MAX_VALUE - 1, 1));
        TraceXFormatException failure = assertThrows(
                TraceXFormatException.class,
                () -> TraceXTimeNormalizer.checkedAdd(Long.MAX_VALUE, 1));
        assertNotNull(failure.getCause());
    }

    /** Tests accepted and rejected exact decimal frequencies. */
    @Test
    public void testFrequencyParsing() {
        assertEquals(1, TraceXTimeNormalizer.parseTicksPerSecond("1")); //$NON-NLS-1$
        assertEquals(1, TraceXTimeNormalizer.parseTicksPerSecond("0001")); //$NON-NLS-1$
        assertEquals(1_000_000_000L,
                TraceXTimeNormalizer.parseTicksPerSecond("1000000000")); //$NON-NLS-1$
        String[] invalid = { "", "0", "-1", "+1", "a", "1.0", " 1", "1 ", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
                "1000000001", "999999999999999999999999" }; //$NON-NLS-1$ //$NON-NLS-2$
        for (String value : invalid) {
            assertThrows(IllegalArgumentException.class,
                    () -> TraceXTimeNormalizer.parseTicksPerSecond(value));
        }
    }

    /** Tests exact, fractional, extreme, and rejected conversions. */
    @Test
    public void testNanosecondConversion() {
        assertEquals(0, TraceXTimeNormalizer.toNanoseconds(0, 1));
        assertEquals(2_000_000_000L,
                TraceXTimeNormalizer.toNanoseconds(4, 2));
        assertEquals(1_500_000_000L,
                TraceXTimeNormalizer.toNanoseconds(3, 2));
        assertEquals(1, TraceXTimeNormalizer.toNanoseconds(1,
                TraceXTimeNormalizer.MAX_TICKS_PER_SECOND));
        assertEquals(Long.MAX_VALUE, TraceXTimeNormalizer.toNanoseconds(
                Long.MAX_VALUE, TraceXTimeNormalizer.MAX_TICKS_PER_SECOND));
        assertThrows(IllegalArgumentException.class,
                () -> TraceXTimeNormalizer.toNanoseconds(Long.MAX_VALUE, 1));
        assertThrows(IllegalArgumentException.class,
                () -> TraceXTimeNormalizer.toNanoseconds(-1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> TraceXTimeNormalizer.toNanoseconds(1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> TraceXTimeNormalizer.toNanoseconds(1,
                        TraceXTimeNormalizer.MAX_TICKS_PER_SECOND + 1));
    }

    /** Tests stable timer-direction property values. */
    @Test
    public void testTimerDirectionProperties() {
        assertEquals("increasing", //$NON-NLS-1$
                TraceXTimerDirection.INCREASING.getPropertyValue());
        assertEquals(TraceXTimerDirection.INCREASING,
                TraceXTimerDirection.parse("increasing")); //$NON-NLS-1$
        assertEquals(TraceXTimerDirection.DECREASING,
                TraceXTimerDirection.parse("decreasing")); //$NON-NLS-1$
        assertThrows(IllegalArgumentException.class,
                () -> TraceXTimerDirection.parse("automatic")); //$NON-NLS-1$
    }

    private static long[] normalize(long[] values,
            TraceXTimerDirection direction) throws TraceXFormatException {
        return TraceXTimeNormalizer.normalize(values, 0xF, direction);
    }
}
