/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.uftrace.core.trace;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;

import com.google.common.collect.ImmutableSet;

/**
 * The event type lookup
 *
 * @author Matthew Khouzam
 */
@NonNullByDefault
public class UfEventType extends TmfEventType {

    private static final ITmfEventField ROOT = new TmfEventField(ITmfEventField.ROOT_FIELD_ID, DatEvent.create(0, 0xaaaaaaaaaaaaaaaaL, 0), null);

    /** Entry event type */
    public static final UfEventType ENTRY = new UfEventType("entry"); //$NON-NLS-1$
    /** Exit event type */
    public static final UfEventType EXIT = new UfEventType("exit"); //$NON-NLS-1$
    /** Event type */
    private static final UfEventType EVENT = new UfEventType("event"); //$NON-NLS-1$
    /** Lost event type */
    private static final UfEventType LOST = new UfEventType("Lost event"); //$NON-NLS-1$

    /** The event types */
    public static final Set<? extends ITmfEventType> TYPES = ImmutableSet.of(UfEventType.ENTRY, UfEventType.EXIT, UfEventType.EVENT, UfEventType.LOST);

    private UfEventType(String name) {
        super(name, ROOT);
    }

    /**
     * Lookup the event type from the name
     *
     * @param type
     *            the type
     * @return the event type
     */
    public static @Nullable ITmfEventType lookup(String type) {
        switch (type.toLowerCase()) {
        case "entry": //$NON-NLS-1$
            return ENTRY;
        case "exit": //$NON-NLS-1$
            return EXIT;
        case "event": //$NON-NLS-1$
            return EVENT;
        case "lost": //$NON-NLS-1$
            return LOST;
        default:
            return null;
        }
    }

}
