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

import org.eclipse.jdt.annotation.Nullable;

/** Typed semantic alias that retains its source raw information word. */
public final class TraceXEventField {

    private final TraceXEventMetadata.Alias fDefinition;
    private final long fRawValue;
    private final @Nullable TraceXRegistryRecord fResolvedObject;

    TraceXEventField(TraceXEventMetadata.Alias definition, long rawValue,
            @Nullable TraceXRegistryRecord resolvedObject) {
        fDefinition = definition;
        fRawValue = rawValue;
        fResolvedObject = resolvedObject;
    }

    /** @return semantic alias name */
    public String getName() {
        return fDefinition.getName();
    }

    /** @return one-based source information-word index */
    public int getRawWord() {
        return fDefinition.getWord();
    }

    /** @return unchanged unsigned value from the source word */
    public long getRawValue() {
        return fRawValue;
    }

    /** @return whether this value has address semantics */
    public boolean isAddress() {
        return fDefinition.isAddress();
    }

    /** @return formatted address, or {@code null} for non-address fields */
    public @Nullable String getFormattedAddress() {
        return isAddress() ? TraceXEvent.formatAddress(fRawValue) : null;
    }

    /** @return object resolved from the registry, or {@code null} */
    public @Nullable TraceXRegistryRecord getResolvedObject() {
        return fResolvedObject;
    }
}
