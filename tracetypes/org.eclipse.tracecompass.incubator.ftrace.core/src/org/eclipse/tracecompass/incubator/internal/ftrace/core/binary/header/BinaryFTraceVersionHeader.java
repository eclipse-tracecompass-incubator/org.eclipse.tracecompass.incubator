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
 * Represent the magic values, the tracing string and the FTrace version. It is
 * used to contain information to validate whether file is a valid binary FTrace
 * file.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceVersionHeader {
    private final byte[] fMagicValues;
    private final BinaryFTraceVersion fFTraceVersion;

    /**
     * Constructor
     *
     * @param magicValues
     *            The magic values of FTrace
     * @param fTraceVersion
     *            The version of FTrace
     */
    public BinaryFTraceVersionHeader(byte[] magicValues, BinaryFTraceVersion fTraceVersion) {
        fMagicValues = magicValues;
        fFTraceVersion = fTraceVersion;
    }

    /**
     * Get the magic values at the beginning of the file, including the
     * "tracing" string
     *
     * @return An array containing the magic values at the beginning of the file
     */
    public byte[] getMagicValues() {
        return fMagicValues;
    }

    /**
     * Get the FTrace version
     *
     * @return The FTrace version
     */
    public BinaryFTraceVersion getFTraceVersion() {
        return fFTraceVersion;
    }
}
