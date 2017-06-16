/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.husdon.maven.core.trace;

import org.eclipse.tracecompass.tmf.core.event.TmfEventType;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.text.TextTrace;
import org.eclipse.tracecompass.tmf.core.trace.text.TextTraceEvent;

/**
 * Maven Trace Event
 *
 * @author Matthew Khouzam
 */
public final class MavenEvent extends TextTraceEvent {

    /**
     * Default Constructor
     */
    public MavenEvent() {
        super(null, null, null, null);
    }

    /**
     * Normal Constructor
     *
     * @param parentTrace
     *            the parent trace
     * @param timestamp
     *            the timestamp
     * @param type
     *            the trace type
     */
    public MavenEvent(TextTrace<? extends TextTraceEvent> parentTrace, ITmfTimestamp timestamp, String type) {
        super(parentTrace, timestamp, new TmfEventType(String.valueOf(type), null), null);
    }
}