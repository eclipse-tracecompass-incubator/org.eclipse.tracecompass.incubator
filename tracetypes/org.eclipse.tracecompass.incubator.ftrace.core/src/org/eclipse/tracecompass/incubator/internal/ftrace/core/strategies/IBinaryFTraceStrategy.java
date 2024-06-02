/*******************************************************************************
 * Copyright (c) 2022 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.ftrace.core.strategies;

import java.io.IOException;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;

/**
 * An interface for implementing various FTrace trace reading strategies for
 * different versions.
 *
 * @author Hoang Thuan Pham
 */
public interface IBinaryFTraceStrategy {
    /**
     * Initialize a binary FTrace trace file by reading the trace header
     *
     * @param path
     *            The path to the trace file
     * @throws TmfTraceException
     *             If an error occurred while parsing the trace header
     */
    public void initTrace(String path) throws TmfTraceException;

    /**
     * Get the next trace event based on the trace context
     *
     * @param context
     *            A {@link ITmfContext} that represents the current state of the
     *            trace reading process
     * @return The next trace event; null if there is no more event to read
     */
    public ITmfEvent getNext(final ITmfContext context);

    /**
     * Create an iterator to iterate through the events of a binary Ftrace trace
     *
     * @return A newly created iterator implementing the {@link ITmfContext}
     *         interface
     * @throws IOException
     *             If an error occurred while creating the iterator
     */
    public @Nullable ITmfContext createIterator() throws IOException;

    /**
     * Seek the trace file and position the trace iterator based on the location
     * parameter. The location of the event is determined by its timestamp, and
     * this method tries to seek for the first event that has a timestamp equals
     * or larger than the timestamp provided by the location parameter.
     *
     * @param location
     *            The location to seek
     * @return The trace iterator that is pointing to the trace at the location
     *         provided by the parameter.
     */
    public ITmfContext seekEvent(ITmfLocation location);

    /**
     * Dispose of the strategy.
     *
     * Can be used to release system resources.
     */
    public void dispose();
}
