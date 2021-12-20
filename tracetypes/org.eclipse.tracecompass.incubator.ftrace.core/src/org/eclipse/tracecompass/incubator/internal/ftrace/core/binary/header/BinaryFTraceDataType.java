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

import org.apache.commons.lang3.ArrayUtils;
/**
 * Represents different data types of a binary FTrace file
 *
 * Linux basic data types:
 * http://www.it.uc3m.es/pbasanta/asng/course_notes/data_types_en.html
 *
 * These are the special types that can be converted back to basic types: Linux
 * kernel data types:
 * https://kernelnewbies.org/InternalKernelDataTypes#:~:text=There%20are%20some%20native%20kernel,prevent%20a%20lot%20of%20problems.
 *
 * @author Hoang Thuan Pham
 */
public enum BinaryFTraceDataType {

    /**
     * Data type unknown (Regex rejects all)
     */
    UNKNOWN(0, null),
    /**
     * An 8-bit (1 byte) value that represents a character. C char is 1 byte
     * while Java char is 2 bytes.
     */
    CHAR(1, new String[] { "char", "s8", "u8" }), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    /**
     * 2 bytes numeric value
     */
    SHORT(2, new String[] { "short", "u16", "s16" }), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    /**
     * 4 bytes numeric value
     */
    INT(4, new String[] { "int", "s32", "u32", "pid_t", "uint32_t" }), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    /**
     * 4 to 8 bytes numeric value
     */
    LONG(8, new String[] { "long", "s64", "u64" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    private final int fSize;
    private final String[] fTypeIdentifiers;

    private BinaryFTraceDataType(int size, String[] typeIdentifiers) {
        fSize = size;
        fTypeIdentifiers = typeIdentifiers;
    }

    /**
     * Get the size of the data bytes.
     *
     * @return The size of the data in bytes
     */
    public int getSize() {
        return fSize;
    }

    /**
     * Return the {@link BinaryFTraceDataType} base on a string.
     *
     * @param typeIdentifier
     *            The string value of the data type
     * @return The data type as a {@link BinaryFTraceDataType} enum value
     */
    public static BinaryFTraceDataType getDataType(String typeIdentifier) {
        for (BinaryFTraceDataType type: BinaryFTraceDataType.values()) {
            if (ArrayUtils.contains(type.fTypeIdentifiers, typeIdentifier)) {
                return type;
            }
        }

        return BinaryFTraceDataType.UNKNOWN;
    }

}
