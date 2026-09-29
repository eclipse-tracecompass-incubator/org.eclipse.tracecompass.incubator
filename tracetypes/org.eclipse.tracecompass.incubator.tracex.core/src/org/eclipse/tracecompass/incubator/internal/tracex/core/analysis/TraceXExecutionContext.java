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

package org.eclipse.tracecompass.incubator.internal.tracex.core.analysis;

import java.util.Locale;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXContext;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXObjectType;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXRegistry;
import org.eclipse.tracecompass.incubator.internal.tracex.core.trace.TraceXRegistryRecord;

/** Immutable execution identity stored in the TraceX state system. */
final class TraceXExecutionContext {

    /** Display categories shared by the analysis and data provider. */
    enum Type {
        THREAD("Thread"), //$NON-NLS-1$
        ISR("ISR"), //$NON-NLS-1$
        INITIALIZATION("Initialization"), //$NON-NLS-1$
        IDLE("Idle"), //$NON-NLS-1$
        UNKNOWN("Unknown"); //$NON-NLS-1$

        private final String fLabel;

        Type(String label) {
            fLabel = label;
        }

        String getLabel() {
            return fLabel;
        }
    }

    private static final String SEPARATOR = "|"; //$NON-NLS-1$
    private static final long NO_ADDRESS = -1;

    private final Type fType;
    private final long fAddress;
    private final String fName;

    private TraceXExecutionContext(Type type, long address, String name) {
        fType = type;
        fAddress = address;
        fName = name;
    }

    static TraceXExecutionContext fromEventContext(TraceXContext context) {
        switch (context.getKind()) {
        case THREAD:
            return thread(context.getRawAddress(), context.getName());
        case ISR:
            return isr(-1);
        case INITIALIZATION:
            return initialization();
        case IDLE:
            return idle();
        case COMPONENT:
        case UNKNOWN:
            return unknown(context.getRawAddress(), context.getName());
        case UNUSED:
        default:
            return unknown(NO_ADDRESS, null);
        }
    }

    static TraceXExecutionContext scheduler(long address,
            TraceXRegistry registry) {
        if (address == 0) {
            return idle();
        }
        TraceXRegistryRecord record = registry.find(address);
        if (record != null && record.getType() == TraceXObjectType.THREAD) {
            return thread(address, record.getName());
        }
        return unknown(address, record == null ? null : record.getName());
    }

    static TraceXExecutionContext thread(long address, @Nullable String name) {
        return new TraceXExecutionContext(Type.THREAD, address,
                displayName(name, "Thread")); //$NON-NLS-1$
    }

    static TraceXExecutionContext isr(long number) {
        return new TraceXExecutionContext(Type.ISR, NO_ADDRESS,
                number < 0 ? "ISR (unknown)" : "ISR " + Long.toUnsignedString(number)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    static TraceXExecutionContext initialization() {
        return new TraceXExecutionContext(Type.INITIALIZATION, 0xF0F0F0F0L,
                "Initialization"); //$NON-NLS-1$
    }

    static TraceXExecutionContext idle() {
        return new TraceXExecutionContext(Type.IDLE, 0, "Idle"); //$NON-NLS-1$
    }

    static TraceXExecutionContext unknown(long address, @Nullable String name) {
        return new TraceXExecutionContext(Type.UNKNOWN, address,
                displayName(name, "Unknown")); //$NON-NLS-1$
    }

    private static String displayName(@Nullable String name, String fallback) {
        return name == null || name.isEmpty() ? fallback : name;
    }

    Type getType() {
        return fType;
    }

    String getName() {
        return fName;
    }

    String getAddressText() {
        return fAddress < 0 ? "unavailable" //$NON-NLS-1$
                : String.format(Locale.ROOT, "0x%08X", fAddress & 0xffffffffL); //$NON-NLS-1$
    }

    String getEntryName() {
        switch (fType) {
        case THREAD:
        case UNKNOWN:
            return fName + " [" + getAddressText() + ']'; //$NON-NLS-1$
        case ISR:
        case INITIALIZATION:
        case IDLE:
        default:
            return fName;
        }
    }

    String serialize() {
        return fType.name() + SEPARATOR + Long.toUnsignedString(fAddress)
                + SEPARATOR + fName;
    }

    static @Nullable TraceXExecutionContext deserialize(@Nullable Object value) {
        if (!(value instanceof String)) {
            return null;
        }
        String[] fields = ((String) value).split("\\|", 3); //$NON-NLS-1$
        if (fields.length != 3) {
            return null;
        }
        try {
            return new TraceXExecutionContext(Type.valueOf(fields[0]),
                    Long.parseUnsignedLong(fields[1]), fields[2]);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
