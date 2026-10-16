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

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents a link from one span to another in the OTLP model.
 *
 * @author Matthew Khouzam
 */
public class OtlpSpanLink {

    private final @NonNull String fTraceId;
    private final @NonNull String fSpanId;
    private final @NonNull Map<@NonNull String, @NonNull String> fAttributes;
    private final @Nullable String fTraceState;

    /**
     * Constructor
     *
     * @param traceId
     *            the trace ID of the linked span
     * @param spanId
     *            the span ID of the linked span
     * @param attributes
     *            link attributes
     * @param traceState
     *            the trace state, may be null
     */
    public OtlpSpanLink(@NonNull String traceId, @NonNull String spanId,
            @NonNull Map<@NonNull String, @NonNull String> attributes, @Nullable String traceState) {
        fTraceId = traceId;
        fSpanId = spanId;
        fAttributes = Collections.unmodifiableMap(attributes);
        fTraceState = traceState;
    }

    /**
     * Get the trace ID
     *
     * @return the trace ID
     */
    public @NonNull String getTraceId() {
        return fTraceId;
    }

    /**
     * Get the span ID
     *
     * @return the span ID
     */
    public @NonNull String getSpanId() {
        return fSpanId;
    }

    /**
     * Get the link attributes
     *
     * @return an unmodifiable map of attributes
     */
    public @NonNull Map<@NonNull String, @NonNull String> getAttributes() {
        return fAttributes;
    }

    /**
     * Get the trace state
     *
     * @return the trace state, or null if not set
     */
    public @Nullable String getTraceState() {
        return fTraceState;
    }
}
