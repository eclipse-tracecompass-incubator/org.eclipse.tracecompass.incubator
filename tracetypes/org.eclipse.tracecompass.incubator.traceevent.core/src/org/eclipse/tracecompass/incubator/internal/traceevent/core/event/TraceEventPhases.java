/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.event;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Helper to keep all the trace event phase strings
 *
 * @author Matthew Khouzam
 */
@NonNullByDefault
public final class TraceEventPhases {

    private TraceEventPhases() {
        // Do nothing
    }

    public static final String DURATION_START = "B"; //$NON-NLS-1$

    public static final String DURATION_END = "E"; //$NON-NLS-1$

    public static final String DURATION = "X"; //$NON-NLS-1$

    public static final String COUNTER = "C"; //$NON-NLS-1$

    public static final String NESTABLE_START = "b"; //$NON-NLS-1$

    public static final String NESTABLE_INSTANT = "n"; //$NON-NLS-1$

    public static final String NESTABLE_END = "e"; //$NON-NLS-1$

    public static final String FLOW_START = "s"; //$NON-NLS-1$

    public static final String FLOW_STEP = "t"; //$NON-NLS-1$

    public static final String FLOW_END = "f"; //$NON-NLS-1$

    public static final String SAMPLE = "p"; //$NON-NLS-1$

    public static final String OBJECT_CREATED = "N"; //$NON-NLS-1$

    public static final String OBJECT_SNAPSHOT = "O"; //$NON-NLS-1$

    public static final String OBJECT_DESTROYED = "D"; //$NON-NLS-1$

    public static final String METADATA = "M"; //$NON-NLS-1$

    public static final String MEMORY_DUMP_GLOBAL = "V"; //$NON-NLS-1$

    public static final String MEMORY_DUMP_PROCESS = "v"; //$NON-NLS-1$

    public static final String MARK = "R"; //$NON-NLS-1$

    public static final String CLOCK_SYNC = "c"; //$NON-NLS-1$

    public static final String CONTEXT_START = "("; //$NON-NLS-1$

    public static final String CONTEXT_END = ")"; //$NON-NLS-1$
}
