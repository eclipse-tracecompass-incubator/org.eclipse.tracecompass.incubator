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
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.flamechart.CallStack;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.ICalledFunction;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.SymbolProviderUtils;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * An entry, or row, in the Call Stack view
 *
 * @author Patrick Tasse
 * @author Geneviève Bastien
 */
public class FlameChartEntry extends TimeGraphEntry {

    private final int fStackLevel;
    private String fFunctionName;
    private long fFunctionEntryTime;
    private long fFunctionExitTime;
    private final CallStack fCallStack;
    private final Collection<ISymbolProvider> fSymbolProviders;
    // FIXME: This is only for the zoomed event list. Not the complete one
    private List<ITimeEvent> fLastEvents;

    /**
     * Standard constructor
     *
     * @param symbolProviders
     *            The symbol provider for this entry
     * @param stackLevel
     *            The stack level
     * @param element
     *            The call stack state system
     * @since 2.0
     */
    public FlameChartEntry(Collection<ISymbolProvider> symbolProviders, int stackLevel,
            @NonNull CallStack element) {
        super(String.valueOf(stackLevel), 0, 0);
        fStackLevel = stackLevel;
        fFunctionName = ""; //$NON-NLS-1$
        fCallStack = element;
        fSymbolProviders = symbolProviders;
    }

    /**
     * Get the function name of the call stack entry
     *
     * @return the function name
     */
    public String getFunctionName() {
        return fFunctionName;
    }

    /**
     * Set the function name of the call stack entry
     *
     * @param functionName
     *            the function name
     */
    public void setFunctionName(String functionName) {
        fFunctionName = functionName;
    }

    /**
     * Set the selected function entry time
     *
     * @param entryTime
     *            the function entry time
     */
    public void setFunctionEntryTime(long entryTime) {
        fFunctionEntryTime = entryTime;
    }

    /**
     * Get the selected function entry time
     *
     * @return the function entry time
     */
    public long getFunctionEntryTime() {
        return fFunctionEntryTime;
    }

    /**
     * Set the selected function exit time
     *
     * @param exitTime
     *            the function exit time
     */
    public void setFunctionExitTime(long exitTime) {
        fFunctionExitTime = exitTime;
    }

    /**
     * Get the selected function exit time
     *
     * @return the function exit time
     */
    public long getFunctionExitTime() {
        return fFunctionExitTime;
    }

    /**
     * Retrieve the stack level associated with this entry.
     *
     * @return The stack level or 0
     */
    public int getStackLevel() {
        return fStackLevel;
    }

    @Override
    public boolean matches(@NonNull Pattern pattern) {
        return pattern.matcher(fFunctionName).find();
    }

    /**
     * Get the list of time events for this entry. A time event will be
     * constructed for each function in the stack
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
        List<ICalledFunction> callList = fCallStack.getCallListAtDepth(fStackLevel, startTime, endTime, resolution, monitor);

        List<ITimeEvent> events = new ArrayList<>();
        final int modulo = FlameChartPresentationProvider.NUM_COLORS / 2;

        long lastEndTime = Long.MAX_VALUE;
        for (ICalledFunction function : callList) {
            if (monitor.isCanceled()) {
                return null;
            }
            long time = function.getStart();
            long duration = function.getLength();

            // Do we add an null event before the function
            if (time > lastEndTime && time > startTime) {
                long a = (lastEndTime == Long.MAX_VALUE ? startTime : lastEndTime);
                events.add(new NullTimeEvent(this, a, time - a));
            }

            // Add a call stack event for this function
            events.add(new FlameChartEvent(this, time, duration, function, function.getSymbol().toString().hashCode() % modulo + modulo));
            lastEndTime = function.getEnd();

        }
        return events;
    }

    @Override
    public void setZoomedEventList(List<ITimeEvent> eventList) {
        fLastEvents = eventList;
        super.setZoomedEventList(eventList);
    }

    List<ITimeEvent> getLastEvents() {
        return fLastEvents;
    }

    @NonNull String resolveFunctionName(ICalledFunction function, long time) {
        long address = Long.MAX_VALUE;
        Object symbol = function.getSymbol();
        if (symbol instanceof Number) {
            address = (Long) symbol;
        } else if (symbol instanceof String) {
            try {
                address = Long.parseLong((String) symbol, 16);
            } catch (NumberFormatException e) {
                return String.valueOf(symbol);
            }
        }

        Collection<ISymbolProvider> providers = fSymbolProviders;
        return SymbolProviderUtils.getSymbolText(providers, function.getProcessId(), time, address);
    }

    /**
     * Return the called function at the requested time
     *
     * @param time
     *            The time of request
     * @return The called function at the requested time, or <code>null</code>
     *         if there is no function call at this time
     */
    public @Nullable ICalledFunction updateAt(long time) {
        List<ICalledFunction> callList = fCallStack.getCallListAtDepth(fStackLevel, time, time, 1, new NullProgressMonitor());
        if (callList.isEmpty()) {
            fFunctionName = ""; //$NON-NLS-1$
            return null;
        }
        ICalledFunction function = callList.get(0);
        fFunctionName = resolveFunctionName(function, time);
        fFunctionEntryTime = function.getStart();
        fFunctionExitTime = function.getEnd();
        return function;
    }

    /**
     * Get the time of the next event, either entry or exit, for this entry
     *
     * @param time
     *            The time of the request
     * @return The time of the next event
     */
    public long getNextEventTime(long time) {
        ICalledFunction nextFunction = fCallStack.getNextFunction(time, fStackLevel);
        if (nextFunction == null) {
            return time;
        }
        return (nextFunction.getStart() <= time ? nextFunction.getEnd() : nextFunction.getStart());
    }

}
