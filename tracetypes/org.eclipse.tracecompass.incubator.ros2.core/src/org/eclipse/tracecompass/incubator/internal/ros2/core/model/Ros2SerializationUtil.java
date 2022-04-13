/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros2.core.model;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Utility functions for state system object serialization.
 *
 * @author Christophe Bedard
 */
public class Ros2SerializationUtil {

    private Ros2SerializationUtil() {
        // Do nothing
    }

    /** 2 bytes for size + 1 byte for \0 at the end */
    private static final int STRING_SIZE_SUFFIX_CONSTANT = 3;
    /** Charset for typical string serialization */
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    /**
     * Get the serialized size of a string.
     *
     * @param string
     *            the string
     * @return the string's serialized size
     */
    public static int getStringSerializedSize(@NonNull String string) {
        return string.getBytes(CHARSET).length + STRING_SIZE_SUFFIX_CONSTANT;
    }
}
