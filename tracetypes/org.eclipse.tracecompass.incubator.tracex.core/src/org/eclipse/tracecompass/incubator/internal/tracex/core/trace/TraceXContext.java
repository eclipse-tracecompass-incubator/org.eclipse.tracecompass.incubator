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

/** Immutable decoded execution context of a TraceX event. */
public final class TraceXContext {

    /** Kinds distinguish documented sentinels without synthetic addresses. */
    public enum Kind {
        /** Registered ThreadX thread. */
        THREAD,
        /** Interrupt-service routine sentinel. */
        ISR,
        /** Initialization sentinel. */
        INITIALIZATION,
        /** Null current-thread pointer on an otherwise valid event. */
        IDLE,
        /** Registered non-thread component. */
        COMPONENT,
        /** Unresolved non-sentinel address. */
        UNKNOWN,
        /** Invalid event slot. */
        UNUSED
    }

    private static final long ISR_ADDRESS = 0xFFFFFFFFL;
    private static final long INITIALIZATION_ADDRESS = 0xF0F0F0F0L;

    private final Kind fKind;
    private final long fRawAddress;
    private final @Nullable String fName;
    private final @Nullable Integer fPriority;
    private final @Nullable Integer fPreemptionThreshold;
    private final @Nullable Long fInterruptedThreadAddress;

    private TraceXContext(Kind kind, long rawAddress, @Nullable String name,
            @Nullable Integer priority, @Nullable Integer threshold,
            @Nullable Long interruptedThreadAddress) {
        fKind = kind;
        fRawAddress = rawAddress;
        fName = name;
        fPriority = priority;
        fPreemptionThreshold = threshold;
        fInterruptedThreadAddress = interruptedThreadAddress;
    }

    static TraceXContext decode(long address, long priorityWord,
            boolean unused, TraceXRegistry registry) {
        if (unused) {
            return new TraceXContext(Kind.UNUSED, address, "Unused", null, null, null); //$NON-NLS-1$
        }
        if (address == ISR_ADDRESS) {
            Long interrupted = priorityWord == 0 ? null : Long.valueOf(priorityWord);
            return new TraceXContext(Kind.ISR, address, "ISR", null, null, interrupted); //$NON-NLS-1$
        }
        if (address == INITIALIZATION_ADDRESS) {
            return new TraceXContext(Kind.INITIALIZATION, address,
                    "Initialization", null, null, null); //$NON-NLS-1$
        }
        if (address == 0) {
            return new TraceXContext(Kind.IDLE, address, "Idle", null, null, null); //$NON-NLS-1$
        }
        TraceXRegistryRecord object = registry.find(address);
        if (object == null) {
            return new TraceXContext(Kind.UNKNOWN, address, null,
                    priority(priorityWord), threshold(priorityWord), null);
        }
        Kind kind = object.getType() == TraceXObjectType.THREAD
                ? Kind.THREAD : Kind.COMPONENT;
        return new TraceXContext(kind, address, object.getName(),
                kind == Kind.THREAD ? priority(priorityWord) : null,
                kind == Kind.THREAD ? threshold(priorityWord) : null, null);
    }

    private static Integer priority(long word) {
        return Integer.valueOf((int) (word & 0x3ff));
    }

    private static Integer threshold(long word) {
        return (word & 0x80000000L) == 0 ? null
                : Integer.valueOf((int) ((word >>> 16) & 0x3ff));
    }

    /** @return decoded context kind */
    public Kind getKind() {
        return fKind;
    }

    /** @return unsigned raw context address */
    public long getRawAddress() {
        return fRawAddress;
    }

    /** @return formatted raw context address */
    public String getFormattedAddress() {
        return TraceXEvent.formatAddress(fRawAddress);
    }

    /** @return resolved or sentinel name, or {@code null} */
    public @Nullable String getName() {
        return fName;
    }

    /** @return decoded 10-bit priority, or {@code null} */
    public @Nullable Integer getPriority() {
        return fPriority;
    }

    /** @return decoded 10-bit preemption threshold, or {@code null} */
    public @Nullable Integer getPreemptionThreshold() {
        return fPreemptionThreshold;
    }

    /** @return interrupted thread address carried by ISR events, or {@code null} */
    public @Nullable Long getInterruptedThreadAddress() {
        return fInterruptedThreadAddress;
    }
}
