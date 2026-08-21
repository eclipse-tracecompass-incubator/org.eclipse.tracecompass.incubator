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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents an OTLP span status with a status code and optional message.
 *
 * Status codes:
 * <ul>
 * <li>0 - UNSET</li>
 * <li>1 - OK</li>
 * <li>2 - ERROR</li>
 * </ul>
 *
 * @author Matthew Khouzam
 */
public class OtlpSpanStatus {

    /** Status code: Unset */
    public static final int STATUS_CODE_UNSET = 0;
    /** Status code: OK */
    public static final int STATUS_CODE_OK = 1;
    /** Status code: Error */
    public static final int STATUS_CODE_ERROR = 2;

    private final int fCode;
    private final @Nullable String fMessage;

    /**
     * Constructor
     *
     * @param code
     *            the status code (0=UNSET, 1=OK, 2=ERROR)
     * @param message
     *            optional status message, may be null
     */
    public OtlpSpanStatus(int code, @Nullable String message) {
        fCode = code;
        fMessage = message;
    }

    /**
     * Get the status code
     *
     * @return the status code
     */
    public int getCode() {
        return fCode;
    }

    /**
     * Get the status message
     *
     * @return the status message, or null if not set
     */
    public @Nullable String getMessage() {
        return fMessage;
    }

    @Override
    public @NonNull String toString() {
        String msg = fMessage;
        if (msg != null && !msg.isEmpty()) {
            return getCodeName() + ": " + msg; //$NON-NLS-1$
        }
        return getCodeName();
    }

    private String getCodeName() {
        switch (fCode) {
        case STATUS_CODE_OK:
            return "OK"; //$NON-NLS-1$
        case STATUS_CODE_ERROR:
            return "ERROR"; //$NON-NLS-1$
        default:
            return "UNSET"; //$NON-NLS-1$
        }
    }
}
