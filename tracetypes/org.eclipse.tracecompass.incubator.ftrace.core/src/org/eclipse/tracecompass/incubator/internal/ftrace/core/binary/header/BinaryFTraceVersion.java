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
 * An enumerator for supported FTrace versions. Currently, the only supported
 * version is v6, but v7 is expected to come out soon.
 *
 * @author Hoang Thuan Pham
 */
public enum BinaryFTraceVersion {
    /**
     * Binary FTrace version 6
     */
    V6(6),
    /**
     * Default value when no version or an unsupported version is detected
     */
    NOT_SUPPORTED(0);

    private final int fVersion;

    private BinaryFTraceVersion(int version) {
        fVersion = version;
    }

    /**
     * Get the binary FTrace version as an integer
     *
     * @return The binary FTrace version as an integer
     */
    public int getVersionAsInteger() {
        return fVersion;
    }

    /**
     * Parse an integer that represents the FTrace version to a
     * {@link BinaryFTraceVersion} value
     *
     * @param version
     *            The integer that represents the FTrace version
     * @return A {@link BinaryFTraceVersion} that matches the version parameter
     */
    public static BinaryFTraceVersion getVersionAsEnum(int version) {
        for (BinaryFTraceVersion type : BinaryFTraceVersion.values()) {
            if (type.getVersionAsInteger() == version) {
                return type;
            }
        }

        return BinaryFTraceVersion.NOT_SUPPORTED;
    }

    /**
     * Compare two {@link BinaryFTraceVersion} and check if they are equal
     *
     * @param version
     *            The {@link BinaryFTraceVersion} to compare
     * @return True if the {@link BinaryFTraceVersion} is equal to the version
     *         parameter
     */
    public boolean equals(BinaryFTraceVersion version) {
        return fVersion == version.getVersionAsInteger();
    }
}
