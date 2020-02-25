/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.scripting.core.trace;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.common.core.collect.BufferedBlockingQueue;
import org.eclipse.tracecompass.incubator.scripting.core.trace.ScriptEventsIterator;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * An event request for scripted analysis. This class has a {link
 * #getEventIterator()} method which returns the iterator to go through the
 * events. Callers need to explicitly send the request to a trace or experiment,
 * using {@link ITmfTrace#sendRequest(ITmfEventRequest)}.
 *
 * @author Geneviève Bastien
 */
public class ScriptEventRequest extends TmfEventRequest {

    /**
     * End event to note the last event of the request
     */
    public static final EndEvent END_EVENT = new EndEvent();

    private static final int DEFAULT_EVENTS_QUEUE_SIZE = 127;
    private static final int DEFAULT_EVENTS_CHUNK_SIZE = 127;

    /**
     * Fake event indicating the build is over, and the provider should close
     */
    private static class EndEvent extends TmfEvent {
        public EndEvent() {
            super(null, ITmfContext.UNKNOWN_RANK, null, null, null);
        }
    }

    private final BufferedBlockingQueue<ITmfEvent> fEventsQueue;
    private final ScriptEventsIterator fEventIterator;

    /**
     * Constructor
     */
    public ScriptEventRequest() {
        super(ITmfEvent.class, TmfTimeRange.ETERNITY, 0, ITmfEventRequest.ALL_DATA, ExecutionType.BACKGROUND, 100);
        fEventsQueue = new BufferedBlockingQueue<>(DEFAULT_EVENTS_QUEUE_SIZE, DEFAULT_EVENTS_CHUNK_SIZE);
        fEventIterator = new ScriptEventsIterator(fEventsQueue);
    }

    @Override
    public void handleData(@NonNull ITmfEvent event) {
        super.handleData(event);
        fEventsQueue.put(event);
    }

    @Override
    public synchronized void done() {
        super.done();
        if (!isCancelled()) {
            fEventsQueue.put(END_EVENT);
            fEventsQueue.flushInputBuffer();
        }
    }

    @Override
    public synchronized void cancel() {
        super.cancel();
        while (!fEventsQueue.isEmpty()) {
          fEventIterator.next();
        }
        fEventsQueue.put(END_EVENT);
        fEventsQueue.flushInputBuffer();
    }

    /**
     * Get the event iterator that will contain the events being read.
     *
     * @return The event iterator
     */
    public ScriptEventsIterator getEventIterator() {
        return fEventIterator;
    }

}