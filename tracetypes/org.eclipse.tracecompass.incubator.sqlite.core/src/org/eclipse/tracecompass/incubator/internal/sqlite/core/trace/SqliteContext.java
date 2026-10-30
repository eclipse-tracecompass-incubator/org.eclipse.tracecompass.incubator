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

package org.eclipse.tracecompass.incubator.internal.sqlite.core.trace;

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;

/**
 * Read cursor over the merged, timestamp-ordered list of SQLite trace events.
 * <p>
 * The events from every trace table are merged and sorted once at
 * {@code initTrace} time; this context simply keeps an index into that list.
 * The index doubles as the {@link TmfLongLocation} for the trace.
 *
 * @author Matthew Khouzam
 */
public class SqliteContext extends TmfContext {

    private final List<SqliteEvent> fEvents;
    private long fIndex;

    /**
     * Constructor.
     *
     * @param events
     *            the merged, timestamp-ordered events
     * @param index
     *            the index to start reading from
     */
    public SqliteContext(List<SqliteEvent> events, long index) {
        super(new TmfLongLocation(index), index);
        fEvents = events;
        fIndex = index;
    }

    /**
     * Peek the event at the current index without advancing.
     *
     * @return the current event, or {@code null} at end of trace
     */
    public @Nullable SqliteEvent current() {
        if (fIndex < 0 || fIndex >= fEvents.size()) {
            return null;
        }
        return fEvents.get((int) fIndex);
    }

    /**
     * Advance the cursor to the next event.
     */
    public void advance() {
        fIndex++;
        setLocation(new TmfLongLocation(fIndex));
    }

    /** @return the current index into the event list */
    public long getIndex() {
        return fIndex;
    }
}
