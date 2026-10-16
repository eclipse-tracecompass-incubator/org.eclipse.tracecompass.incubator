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

package org.eclipse.tracecompass.incubator.internal.agent.trace.core.event;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * An event from an Agent Trace (JSONL format).
 *
 * @author Eya Matkho
 */
public class AgentTraceEvent extends TmfEvent {

    /**
     * Constructor
     *
     * @param trace
     *            the parent trace
     * @param rank
     *            the event rank (index)
     * @param timestamp
     *            the event timestamp
     * @param type
     *            the event type
     * @param content
     *            the event content (fields)
     */
    public AgentTraceEvent(@NonNull ITmfTrace trace, long rank,
            @NonNull ITmfTimestamp timestamp, @NonNull ITmfEventType type,
            @NonNull ITmfEventField content) {
        super(trace, rank, timestamp, type, content);
    }
}
