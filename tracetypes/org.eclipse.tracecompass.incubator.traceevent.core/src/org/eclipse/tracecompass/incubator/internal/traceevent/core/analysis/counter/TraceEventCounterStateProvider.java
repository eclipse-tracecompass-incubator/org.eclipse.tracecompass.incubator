/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.counter;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.counters.core.CounterAnalysis;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.Activator;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.event.TraceEventEvent;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.event.TraceEventField;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemBuilderUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Longs;

/**
 * Counter state provider. The counter events have (optional) PIDs and TIDs.
 * These 'C' events state that the arguments are in a key-value pair in the args
 * field. The state provider creates a state tree with the following structure
 *
 * <pre>
 * PID
 *  |- 1
 *  |- 2
 *  \- 3 - TID
 *          |- 1
 *          \- 2 - Value
 *                    |- Dogs  (value 1)
 *                    \  Cats  (value 2)
 * </pre>
 *
 * @author Matthew Khouzam
 */
public class TraceEventCounterStateProvider extends AbstractTmfStateProvider {

    private static final @NonNull String ID = "org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.counter"; //$NON-NLS-1$

    /**
     * Builder function
     *
     * @param trace
     *            the trace
     * @return the provider
     */
    public static @NonNull ITmfStateProvider createTraceEventCounterStateProvider(@Nullable ITmfTrace trace) {
        return new TraceEventCounterStateProvider(Objects.requireNonNull(trace));
    }

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     */
    public TraceEventCounterStateProvider(@NonNull ITmfTrace trace) {
        super(trace, ID);
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        ITmfStateSystemBuilder ss = getStateSystemBuilder();
        if (ss == null || !(event instanceof TraceEventEvent)) {
            return;
        }
        TraceEventField fields = ((TraceEventEvent) event).getField();
        if (fields.getPhase() != 'C') {
            return;
        }
        Map<@NonNull String, @NonNull Object> data = fields.getArgs();
        if (data != null) {
            for (Entry<@NonNull String, @NonNull Object> entry : data.entrySet()) {
                try {
                    Object entryValue = Objects.requireNonNull(entry.getValue());
                    Long value = null;
                    if (entryValue instanceof Number) {
                        value = ((Number) entryValue).longValue();
                    }
                    String valueOf = String.valueOf(entryValue);
                    value = Longs.tryParse(valueOf);
                    if (value == null) {
                        Double dbl = Doubles.tryParse(valueOf);
                        if (dbl != null) {
                            value = dbl.longValue();
                        }
                    }
                    if (value != null) {
                        int ungroupQuark = ss.getQuarkAbsoluteAndAdd(CounterAnalysis.UNGROUPED_COUNTER_ASPECTS_ATTRIB);
                        int ungroupValQuark = ss.getQuarkRelativeAndAdd(ungroupQuark, entry.getKey());
                        StateSystemBuilderUtils.incrementAttributeLong(ss, event.getTimestamp().toNanos(), ungroupValQuark, value.longValue());
                        int groupQuark = ss.getQuarkAbsoluteAndAdd(CounterAnalysis.GROUPED_COUNTER_ASPECTS_ATTRIB);
                        groupQuark = ss.getQuarkRelativeAndAdd(groupQuark, "PID", String.valueOf(fields.getPid())); //$NON-NLS-1$
                        groupQuark = ss.getQuarkRelativeAndAdd(groupQuark, "TID", String.valueOf(fields.getTid())); //$NON-NLS-1$
                        groupQuark = ss.getQuarkRelativeAndAdd(groupQuark, "Name", String.valueOf(fields.getName())); //$NON-NLS-1$
                        groupQuark = ss.getQuarkRelativeAndAdd(groupQuark, entry.getKey());
                        StateSystemBuilderUtils.incrementAttributeLong(ss, event.getTimestamp().toNanos(), groupQuark, value.longValue());
                    }
                } catch (StateValueTypeException | AttributeNotFoundException e) {
                    Activator.getInstance().logError("error in counters analysis", e); //$NON-NLS-1$
                }
            }
        }
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return createTraceEventCounterStateProvider(getTrace());
    }

}
