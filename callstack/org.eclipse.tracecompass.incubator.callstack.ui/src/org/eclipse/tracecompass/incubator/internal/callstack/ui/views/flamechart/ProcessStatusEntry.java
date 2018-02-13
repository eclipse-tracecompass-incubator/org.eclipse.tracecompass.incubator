/*******************************************************************************
 * Copyright (c) 2013, 2016 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.ui.views.flamechart;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.concepts.ProcessStatusInterval;
import org.eclipse.tracecompass.incubator.callstack.core.flamechart.CallStack;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.ICalledFunction;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * An entry, or row, in the Call Stack view
 *
 * @author Patrick Tasse
 * @author Geneviève Bastien
 */
public class ProcessStatusEntry extends TimeGraphEntry {

    private final FlameChartEntry fEntry;
    private final CallStack fCallStack;

    /**
     * Constructor
     *
     * @param callstack
     *            The callstack this entry will get kernel statuses for
     * @param entry
     *            The flame chart entry associated with this process status
     */
    public ProcessStatusEntry(CallStack callstack, FlameChartEntry entry) {
        super(Messages.CallStackView_KernelStatus, 0, 0);
        fEntry = entry;
        fCallStack = callstack;
    }

    /**
     * Get the list of time events for this entry. A time event will be constructed
     * for each function in the stack
     *
     * @param startTime
     *            The start of the requested period
     * @param endTime
     *            The end time of the requested period
     * @param resolution
     *            The resolution
     * @param monitor
     *            The progress monitor to use for cancellation
     * @return The list of {@link ITimeEvent} to display in the view, or
     *         <code>null</code> if the analysis was cancelled.
     */
    public @Nullable List<ITimeEvent> getEventList(long startTime, long endTime, long resolution, @NonNull IProgressMonitor monitor) {
        List<ITimeEvent> lastEvents = fEntry.getLastEvents();
        List<ITimeEvent> events = new ArrayList<>();
        if (lastEvents == null) {
            return events;
        }
        for (ITimeEvent event : lastEvents) {
            if (!(event instanceof FlameChartEvent)) {
                continue;
            }
            FlameChartEvent fcEvent = (FlameChartEvent) event;
            ICalledFunction function = fcEvent.getFunction();
            // FIXME: This gets all the statuses, that can be big for large time ranges. Use
            // a method with resolution when it is available
            Iterator<@NonNull ProcessStatusInterval> statuses = fCallStack.getKernelStatuses(function, resolution);
            while (statuses.hasNext()) {
                ProcessStatusInterval status = statuses.next();
                events.add(new TimeEvent(this, status.getStart(), status.getLength(), status.getProcessStatus().getStateValue().unboxInt()));
            }
        }
        return events;
    }

}
