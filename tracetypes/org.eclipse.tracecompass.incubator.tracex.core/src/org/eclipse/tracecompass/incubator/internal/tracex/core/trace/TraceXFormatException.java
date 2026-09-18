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

import java.io.IOException;

/**
 * Exception raised when a TraceX binary violates the documented format.
 */
public class TraceXFormatException extends IOException {

    private static final long serialVersionUID = 1L;

    /**
     * Construct a format exception.
     *
     * @param message
     *            concise description of the invalid field or invariant
     */
    public TraceXFormatException(String message) {
        super(message);
    }
}
