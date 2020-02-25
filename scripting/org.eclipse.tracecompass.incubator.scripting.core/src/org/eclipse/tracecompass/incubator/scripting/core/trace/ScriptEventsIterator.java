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

package org.eclipse.tracecompass.incubator.scripting.core.trace;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.collect.BufferedBlockingQueue;
import org.eclipse.tracecompass.incubator.internal.scripting.core.trace.ScriptEventRequest;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;

/**
 * An event iterator class for scripting which allows to return to the script
 * only the requested events, by setting the desired event names with the
 * {@link #addEvent(String)} method. If no event is added, all events will be
 * returned.
 *
 * @author Geneviève Bastien
 */
public class ScriptEventsIterator implements Iterator<ITmfEvent> {

    private final BufferedBlockingQueue<ITmfEvent> fEventsQueue;
    private final Collection<String> fNames = new HashSet<>();
    private @Nullable ITmfEvent fNext;

    /**
     * Constructor
     *
     * @param eventsQueue
     *            The blocking queue from which to get the events
     */
    public ScriptEventsIterator(BufferedBlockingQueue<ITmfEvent> eventsQueue) {
        fEventsQueue = eventsQueue;
    }

    @Override
    public synchronized boolean hasNext() {
        ITmfEvent next = fNext;
        if (next == null) {
            next = findNextEvent();
            fNext = next;
        }
        return next != ScriptEventRequest.END_EVENT;
    }

    private ITmfEvent findNextEvent() {
        ITmfEvent event = fEventsQueue.take();
        if (fNames.isEmpty()) {
            return event;
        }
        while (event != ScriptEventRequest.END_EVENT && !fNames.contains(event.getName())) {
            event = fEventsQueue.take();
        }
        return event;
    }

    /**
     * Add an event name to the list of events to return. Once an event has been
     * added to this iterator, only those events will be returned.
     * <p>
     * To return multiple events, this method can be called multiple times, once
     * for each event.
     * </p>
     * @param eventName
     *            The name of the event to return
     */
    public void addEvent(String eventName) {
        fNames.add(eventName);
    }

    @Override
    public synchronized ITmfEvent next() {
        if (hasNext()) {
            ITmfEvent next = fNext;
            fNext = null;
            if (next != null) {
                return next;
            }
        }
        throw new NoSuchElementException("No more elements in the queue"); //$NON-NLS-1$
    }

}
