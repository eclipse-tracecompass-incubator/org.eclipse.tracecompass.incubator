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

import java.util.regex.Pattern;

/**
 * Represents different data types of a binary FTrace file
 *
 * Linux basic data types: http://www.it.uc3m.es/pbasanta/asng/course_notes/data_types_en.html
 *
 * These are the special types that can be converted back to basic types:
 * Linux kernel data types: https://kernelnewbies.org/InternalKernelDataTypes#:~:text=There%20are%20some%20native%20kernel,prevent%20a%20lot%20of%20problems.
 *
 * @author Hoang Thuan Pham
 */
public enum BinaryFTraceDataType {

    /**
     * Data type unknown (Regex rejects all)
     */
    UNKNOWN(rejectAll()),
    /**
     * An 8-bit (1 byte) value that represents a character. C char is 1 byte
     * while Java char is 2 bytes.
     */
    CHAR(Pattern.compile("[^\\*]*(char)[^\\*\\[\\]]*")), //$NON-NLS-1$
    /**
     * 1 byte numeric value
     */
    BYTE(Pattern.compile("[^\\*]*(u8|s8)[^\\*\\[\\]]*")), //$NON-NLS-1$
    /**
     * 2 bytes numeric value
     */
    SHORT(Pattern.compile("[^\\*]*(short|u16|s16)[^\\*\\[\\]]*")), //$NON-NLS-1$
    /**
     * 4 bytes numeric value
     */
    INT(Pattern.compile("[^\\*]*(int|u32|s32|pid_t)[^\\*\\[\\]]*")), //$NON-NLS-1$
    /**
     * 4 to 8 bytes numeric value
     */
    LONG(Pattern.compile("[^\\*]*(long|u64|s64)[^\\*\\[\\]]*")), //$NON-NLS-1$
    /**
     * 8 bytes floating point value
     */
    DOUBLE(rejectAll()),
    /**
     * A string as an array of characters
     */
    STRING(Pattern.compile(".*char.*\\[[0-9]*\\]$")), //$NON-NLS-1$
    /**
     * A boolean value (1 byte)
     */
    BOOL(Pattern.compile(".*bool.*")), //$NON-NLS-1$
    /**
     * 4 bytes floating point value
     */
    FLOAT(rejectAll()),
    /**
     * Unsigned 8 bits value
     */
    U8(rejectAll()),
    /**
     * Unsigned 16 bits value
     */
    U16(rejectAll()),
    /**
     * Signed 8 bits value
     */
    S8(rejectAll()),
    /**
     * Signed 16 bits value
     */
    S16(rejectAll()),
    /**
     * Signed 32 bits value
     */
    U32(rejectAll()),
    /**
     * Signed 64 bits value
     */
    U64(rejectAll()),
    /**
     * Signed 32 bits value
     */
    S32(rejectAll()),
    /**
     * Signed 64 bits value
     */
    S64(rejectAll()),
    /**
     * The value is a pointer
     */
    POINTER(Pattern.compile(".*\\*.*")); //$NON-NLS-1$

    private static Pattern sRejectAll = null;

    private static Pattern rejectAll() {
        if (sRejectAll == null) {
            sRejectAll = Pattern.compile("^[^.]*"); //$NON-NLS-1$
        }
        return sRejectAll;
    }

    private final Pattern fPattern;

    /**
     * Return matcher pattern
     *
     * @return the matcher pattern
     */
    public Pattern getPattern() {
        return fPattern;
    }

    private BinaryFTraceDataType(Pattern pattern) {
        fPattern = pattern;
    }
}
