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

package org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event;

/**
 * A immutable definition of a binary FTrace event in the the binary file. It contains
 * information to lazily retrieve an event from the binary FTrace file.
 *
 * @author Hoang Thuan Pham
 *
 */
public final class BinaryFTraceEventDefinition {
    /**
     * Special value for null event definition.
     */
    public static final int NULL_OFFSET = -1;

    /**
     * Special value for event with empty payload.
     */
    public static final int NULL_PAYLOAD = 0;

    private final long fPayloadOffset;
    private final int fPayloadSize;

    /**
     * Constructor
     *
     * @param payloadOffset
     *            The offset (in bytes) where the payload of this event starts
     *            in the binary FTrace file.
     * @param payloadSize
     *            The size (in bytes) of the payload of this event in the binary
     *            FTrace file.
     */
    public BinaryFTraceEventDefinition(long payloadOffset, int payloadSize) {
        fPayloadOffset = payloadOffset;
        fPayloadSize = payloadSize;
    }

    /**
     * Get the offset (in bytes) where the payload of the event starts in the
     * binary FTrace file.
     *
     * @return The offset where the payload of the event starts.
     */
    public long getPayloadOffset() {
        return fPayloadOffset;
    }

    /**
     * Get the payload size (in bytes) of this event. This information is used to read the
     * correct number of bytes that contains the event payload.
     *
     * @return the payload size of this event.
     */
    public int getPayloadSize() {
        return fPayloadSize;
    }
}
