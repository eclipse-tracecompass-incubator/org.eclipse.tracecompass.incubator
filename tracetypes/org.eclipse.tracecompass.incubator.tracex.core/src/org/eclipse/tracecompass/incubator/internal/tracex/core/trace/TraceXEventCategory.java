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

/** Stable event ownership categories derived from the current trace ranges. */
public enum TraceXEventCategory {
    /** Invalid event marker or zero event ID. */
    INVALID,
    /** ThreadX kernel event. */
    THREADX,
    /** FileX event. */
    FILEX,
    /** NetX Duo event. */
    NETX_DUO,
    /** USBX event. */
    USBX,
    /** Reserved component or future system event. */
    RESERVED,
    /** User-defined event. */
    USER,
    /** Value outside a recognized category. */
    UNKNOWN
}
