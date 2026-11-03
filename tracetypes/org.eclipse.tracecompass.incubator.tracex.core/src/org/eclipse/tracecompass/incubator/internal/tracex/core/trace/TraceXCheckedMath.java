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

/**
 * Checked arithmetic used to derive file offsets and region sizes.
 */
public final class TraceXCheckedMath {

    private TraceXCheckedMath() {
    }

    /**
     * Add two non-negative values.
     *
     * @param left
     *            first value
     * @param right
     *            second value
     * @param description
     *            derived value name for diagnostics
     * @return the sum
     * @throws TraceXFormatException
     *             if an operand is negative or the sum overflows
     */
    public static long add(long left, long right, String description) throws TraceXFormatException {
        requireNonNegative(left, description);
        requireNonNegative(right, description);
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException e) {
            throw overflow(description);
        }
    }

    /**
     * Subtract two non-negative values without allowing underflow.
     *
     * @param left
     *            minuend
     * @param right
     *            subtrahend
     * @param description
     *            derived value name for diagnostics
     * @return the difference
     * @throws TraceXFormatException
     *             if an operand is negative or the result underflows
     */
    public static long subtract(long left, long right, String description) throws TraceXFormatException {
        requireNonNegative(left, description);
        requireNonNegative(right, description);
        if (left < right) {
            throw new TraceXFormatException(description + " underflows"); //$NON-NLS-1$
        }
        return left - right;
    }

    /**
     * Multiply two non-negative values.
     *
     * @param left
     *            first factor
     * @param right
     *            second factor
     * @param description
     *            derived value name for diagnostics
     * @return the product
     * @throws TraceXFormatException
     *             if an operand is negative or the product overflows
     */
    public static long multiply(long left, long right, String description) throws TraceXFormatException {
        requireNonNegative(left, description);
        requireNonNegative(right, description);
        try {
            return Math.multiplyExact(left, right);
        } catch (ArithmeticException e) {
            throw overflow(description);
        }
    }

    private static void requireNonNegative(long value, String description) throws TraceXFormatException {
        if (value < 0) {
            throw new TraceXFormatException(description + " is negative"); //$NON-NLS-1$
        }
    }

    private static TraceXFormatException overflow(String description) {
        return new TraceXFormatException(description + " overflows"); //$NON-NLS-1$
    }
}
