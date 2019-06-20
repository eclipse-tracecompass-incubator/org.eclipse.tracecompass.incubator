/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.scripting.core.analysis;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.ease.modules.ScriptParameter;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.collect.BufferedBlockingQueue;
import org.eclipse.tracecompass.incubator.internal.scripting.core.analysis.TmfScriptAnalysis;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Provide a class for scripted analysis. It provides an event iterator, as well
 * as backends to store data. Scripts can thus parse events and fill the backend
 * appropriately.
 *
 * @author Geneviève Bastien
 */
public class ScriptedAnalysis {

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

    private static final EndEvent END_EVENT = new EndEvent();

    private final ITmfTrace fTrace;
    private final String fName;

    /**
     * Constructor
     *
     * @param activeTrace
     *            The trace to associate with this analysis
     * @param name
     *            The name of the analysis
     */
    public ScriptedAnalysis(ITmfTrace activeTrace, String name) {
        fTrace = activeTrace;
        fName = name;
    }

    /**
     * Get a state system to go with this analysis. If an analysis of the same
     * name already exists, it can be re-used instead of re-created.
     *
     * @param useExisting
     *            if <code>true</code>, any state system with the same name for
     *            the trace will be reused.
     * @return A state system builder
     */
    @WrapToScript
    public @Nullable ITmfStateSystemBuilder getStateSystem(@ScriptParameter(defaultValue = "false") boolean useExisting) {

        TmfScriptAnalysis analysisModule = TmfTraceUtils.getAnalysisModuleOfClass(fTrace, TmfScriptAnalysis.class, TmfScriptAnalysis.ID);
        if (analysisModule == null) {
            return null;
        }

        return (ITmfStateSystemBuilder) analysisModule.getStateSystem(fName, useExisting);

    }

    /**
     * Get an iterator to iterate chronologically through the events of the
     * trace.
     *
     * Thus, to iterate through a trace in a scripted analysis, one can just do
     * the following snippet (javascript)
     *
     * <pre>
     * var iter = analysis.getEventIterator();
     *
     * var event = null;
     * while (iter.hasNext()) {
     *
     *     event = iter.next();
     *
     *     // Do something with the event
     * }
     * </pre>
     *
     * @return The event iterator, starting from the first event
     */
    @WrapToScript
    public Iterator<ITmfEvent> getEventIterator() {
        BufferedBlockingQueue<ITmfEvent> eventsQueue = new BufferedBlockingQueue<>(DEFAULT_EVENTS_QUEUE_SIZE, DEFAULT_EVENTS_CHUNK_SIZE);
        fTrace.sendRequest(new ScriptEventRequest(eventsQueue));
        return new EventIterator(eventsQueue);
    }

    private static class EventIterator implements Iterator<ITmfEvent> {

        private final BufferedBlockingQueue<ITmfEvent> fEventsQueue;
        private @Nullable ITmfEvent fNext;

        public EventIterator(BufferedBlockingQueue<ITmfEvent> eventsQueue) {
            fEventsQueue = eventsQueue;
        }

        @Override
        public synchronized boolean hasNext() {
            ITmfEvent next = fNext;
            if (next == null) {
                next = fEventsQueue.take();
                fNext = next;
            }
            return next != END_EVENT;
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

    private static class ScriptEventRequest extends TmfEventRequest {

        private BufferedBlockingQueue<ITmfEvent> fEventsQueue;

        public ScriptEventRequest(BufferedBlockingQueue<ITmfEvent> eventsQueue) {
            super(ITmfEvent.class, TmfTimeRange.ETERNITY, 0, ITmfEventRequest.ALL_DATA, ExecutionType.BACKGROUND, 100);
            fEventsQueue = eventsQueue;
        }

        @Override
        public void handleData(@NonNull ITmfEvent event) {
            super.handleData(event);
            fEventsQueue.put(event);
        }

        @Override
        public synchronized void done() {
            super.done();
            fEventsQueue.put(END_EVENT);
            fEventsQueue.flushInputBuffer();
        }

        @Override
        public synchronized void cancel() {
            super.cancel();
            fEventsQueue.put(END_EVENT);
            fEventsQueue.flushInputBuffer();
        }

    }

    /**
     * Get the trace
     *
     * @return The trace
     */
    public ITmfTrace getTrace() {
        return fTrace;
    }

    /**
     * Get the name of this analysis
     *
     * @return The name of the analysis
     */
    public String getName() {
        return fName;
    }
}
