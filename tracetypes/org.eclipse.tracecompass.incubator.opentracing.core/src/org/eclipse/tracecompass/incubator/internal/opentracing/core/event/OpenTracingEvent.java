/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.core.event;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Open tracing span (event)
 *
 * @author Katherine Nadeau
 */
public class OpenTracingEvent extends TmfEvent {

    private final String fName;
    private final OpenTracingField fField;

    /**
     * Constructor for simple traceEventEvent
     *
     * @param trace
     *            the trace
     * @param rank
     *            the rank
     * @param field
     *            the event field, contains all the needed data
     */
    public OpenTracingEvent(ITmfTrace trace, long rank, OpenTracingField field) {
        super(trace, rank, TmfTimestamp.fromNanos(field.getStartTime()), new TmfEventType("OpenTracingSpan", null), field.getContent()); //$NON-NLS-1$
        fField = field;
        fName = field.getName();
    }

    @Override
    public ITmfEventField getContent() {
        return fField.getContent();
    }

    @Override
    public @NonNull String getName() {
        return fName;
    }

    /**
     * Get the fields of the event
     *
     * @return the fields of the event
     */
    public OpenTracingField getField() {
        return fField;
    }
}