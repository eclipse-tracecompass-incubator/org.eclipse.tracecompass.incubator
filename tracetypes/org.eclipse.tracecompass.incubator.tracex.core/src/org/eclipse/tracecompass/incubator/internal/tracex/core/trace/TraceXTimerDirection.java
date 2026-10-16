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

/** Direction in which a target timer advances. */
public enum TraceXTimerDirection {
    /** Larger timer words represent forward progress until wraparound. */
    INCREASING("increasing"), //$NON-NLS-1$
    /** Smaller timer words represent forward progress until wraparound. */
    DECREASING("decreasing"); //$NON-NLS-1$

    private final String fPropertyValue;

    TraceXTimerDirection(String propertyValue) {
        fPropertyValue = propertyValue;
    }

    /** @return stable persistent-property value */
    public String getPropertyValue() {
        return fPropertyValue;
    }

    /**
     * Decode a stable persistent-property value.
     *
     * @param value
     *            property value
     * @return decoded direction
     * @throws IllegalArgumentException
     *             if the value is not supported
     */
    public static TraceXTimerDirection parse(String value) {
        for (TraceXTimerDirection direction : values()) {
            if (direction.fPropertyValue.equals(value)) {
                return direction;
            }
        }
        throw new IllegalArgumentException("Timer direction must be increasing or decreasing"); //$NON-NLS-1$
    }
}
