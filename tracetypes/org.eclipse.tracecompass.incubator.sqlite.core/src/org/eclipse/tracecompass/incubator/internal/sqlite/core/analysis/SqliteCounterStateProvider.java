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

package org.eclipse.tracecompass.incubator.internal.sqlite.core.analysis;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.counters.core.CounterAnalysis;
import org.eclipse.tracecompass.incubator.internal.sqlite.core.Activator;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * State provider backing {@link SqliteCounterAnalysis}.
 * <p>
 * For every event, every field whose value is numeric (an integer/long or a
 * floating-point value) is recorded as a counter under
 * <pre>
 * ungrouped
 *   \- &lt;event name&gt;
 *        \- &lt;column&gt; (value)
 * </pre>
 * The value stored is the column's absolute reading (a gauge), so plotting the
 * counter shows the column's value over time.
 *
 * @author Matthew Khouzam
 */
public class SqliteCounterStateProvider extends AbstractTmfStateProvider {

    private static final int VERSION = 1;

    /**
     * Constructor.
     *
     * @param trace
     *            the trace
     */
    public SqliteCounterStateProvider(@NonNull ITmfTrace trace) {
        super(trace, SqliteCounterAnalysis.ANALYSIS_ID);
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new SqliteCounterStateProvider(Objects.requireNonNull(getTrace()));
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        ITmfStateSystemBuilder ss = getStateSystemBuilder();
        if (ss == null) {
            return;
        }
        long ts = event.getTimestamp().toNanos();
        String eventName = event.getName();
        for (ITmfEventField field : event.getContent().getFields()) {
            Object value = field.getValue();
            if (!(value instanceof Number)) {
                continue;
            }
            String column = field.getName();
            try {
                int quark = ss.getQuarkAbsoluteAndAdd(CounterAnalysis.UNGROUPED_COUNTER_ASPECTS_ATTRIB, eventName, column);
                Object stateValue = (value instanceof Double || value instanceof Float)
                        ? Double.valueOf(((Number) value).doubleValue())
                        : Long.valueOf(((Number) value).longValue());
                ss.modifyAttribute(ts, Objects.requireNonNull(stateValue), quark);
            } catch (StateValueTypeException e) {
                Activator.getInstance().logError("Error building SQLite counter for column " + column, e); //$NON-NLS-1$
            }
        }
    }
}
