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

/**
 * Represents an event (timestamped log) within an OTLP span. This is NOT a TMF
 * event — it is a sub-event within the span data model.
 *
 * @author Matthew Khouzam
 */
public class OtlpSpanEvent {

    private final @NonNull String fName;
    private final long fTimeUnixNano;
    private final @NonNull Map<@NonNull String, @NonNull String> fAttributes;

    /**
     * Constructor
     *
     * @param name
     *            the event name
     * @param timeUnixNano
     *            the event timestamp in nanoseconds since Unix epoch
     * @param attributes
     *            the event attributes
     */
    public OtlpSpanEvent(@NonNull String name, long timeUnixNano,
            @NonNull Map<@NonNull String, @NonNull String> attributes) {
        fName = name;
        fTimeUnixNano = timeUnixNano;
        fAttributes = Collections.unmodifiableMap(attributes);
    }

    /**
     * Get the event name
     *
     * @return the event name
     */
    public @NonNull String getName() {
        return fName;
    }

    /**
     * Get the event timestamp in nanoseconds since Unix epoch
     *
     * @return the timestamp in nanoseconds
     */
    public long getTimeUnixNano() {
        return fTimeUnixNano;
    }

    /**
     * Get the event attributes
     *
     * @return an unmodifiable map of attributes
     */
    public @NonNull Map<@NonNull String, @NonNull String> getAttributes() {
        return fAttributes;
    }
}
