/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.uftrace.core.trace;

import java.util.PriorityQueue;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

import com.google.common.collect.PeekingIterator;

/**
 * Naive context
 *
 * @author Matthew Khouzam
 *
 */
public class UfContext extends TmfContext {

    private PriorityQueue<PeekingIterator<DatEvent>> fQueue = new PriorityQueue<>(
            (o1, o2) -> Long.compare(o1.peek().getTime(), o2.peek().getTime()));
    private ITmfTrace fTrace;

    /**
     * Constructor
     *
     * @param dats
     *            data streams
     * @param trace
     *            trace
     */
    public UfContext(Iterable<DatParser> dats, ITmfTrace trace) {
        fTrace = trace;
        for (DatParser dp : dats) {
            fQueue.add(dp.iterator());
        }
    }

    @Override
    public @Nullable TmfLongLocation getLocation() {
        return (@Nullable TmfLongLocation) super.getLocation();
    }

    /**
     * Get next event, like an iterator
     *
     * @return the next event or null
     */
    public ITmfEvent getNext() {
        PeekingIterator<DatEvent> eventSource = fQueue.poll();
        if (eventSource != null) {
            DatEvent event = eventSource.next();
            if (eventSource.hasNext()) {
                fQueue.add(eventSource);
            }
            TmfEvent tmfEvent = new TmfEvent(fTrace, getRank(), TmfTimestamp.fromNanos(event.getTime()),
                    UfEventType.lookup(event.getType()),
                    new TmfEventField(ITmfEventField.ROOT_FIELD_ID, event, null));
            TmfLongLocation location = getLocation();
            if (location != null) {
                setLocation(new TmfLongLocation(location.getLocationInfo() + Long.BYTES * 2));
            } else {
                setLocation(new TmfLongLocation(Long.BYTES * 2));
            }
            return tmfEvent;
        }
        return null;
    }

}
