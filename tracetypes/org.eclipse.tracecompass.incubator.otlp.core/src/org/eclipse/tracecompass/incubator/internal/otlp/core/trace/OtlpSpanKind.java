/*******************************************************************************
 * Copyright (c) 2026 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.otlp.core.trace;

/**
 * Enum representing the OTLP span kind.
 *
 * @author Matthew Khouzam
 */
public enum OtlpSpanKind {

    /** Internal span (default) */
    INTERNAL(0),
    /** Server span */
    SERVER(1),
    /** Client span */
    CLIENT(2),
    /** Producer span */
    PRODUCER(3),
    /** Consumer span */
    CONSUMER(4);

    private final int fValue;

    OtlpSpanKind(int value) {
        fValue = value;
    }

    /**
     * Get the numeric value of this span kind
     *
     * @return the numeric value
     */
    public int getValue() {
        return fValue;
    }

    /**
     * Get the span kind from its numeric value
     *
     * @param value
     *            the numeric value (0-4)
     * @return the corresponding span kind, or INTERNAL if not recognized
     */
    public static OtlpSpanKind fromValue(int value) {
        for (OtlpSpanKind kind : values()) {
            if (kind.fValue == value) {
                return kind;
            }
        }
        return INTERNAL;
    }
}
