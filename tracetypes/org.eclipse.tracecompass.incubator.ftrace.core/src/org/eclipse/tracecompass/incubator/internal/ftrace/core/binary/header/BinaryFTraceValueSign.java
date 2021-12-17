/*******************************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header;

/**
 * The sign of a numeric value in the FTrace binary file. A numeric value can be
 * either signed or unsigned.
 *
 * @author Hoang Thuan Pham
 */
public enum BinaryFTraceValueSign {
    /**
     * Unsigned value
     */
    UNSIGNED(false),
    /**
     * Signed value
     */
    SIGNED(true);

    private final boolean fSigned;

    private BinaryFTraceValueSign(boolean val) {
        fSigned = val;
    }

    /**
     * Get the numeric value of the sign
     *
     * @return The numeric value of the sign
     */
    public boolean isSigned() {
        return fSigned;
    }

    /**
     * Parse a string to an {@link BinaryFTraceValueSign} value.
     *
     * @param val The string to parse
     * @return The {@link BinaryFTraceValueSign} value that correspond to the string
     */
    public static BinaryFTraceValueSign getEnumFromString(String val) {
        return Boolean.parseBoolean(val) ? SIGNED : UNSIGNED;
    }
}
