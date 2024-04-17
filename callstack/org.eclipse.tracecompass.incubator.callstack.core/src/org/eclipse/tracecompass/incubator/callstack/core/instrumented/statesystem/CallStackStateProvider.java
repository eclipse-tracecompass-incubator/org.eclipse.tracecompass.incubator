/*******************************************************************************
 * Copyright (c) 2013, 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem;

import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tracecompass.incubator.analysis.core.aspects.ProcessNameAspect;
import org.eclipse.tracecompass.incubator.analysis.core.aspects.ThreadNameAspect;
import org.eclipse.tracecompass.incubator.callstack.core.flamechart.Messages;
import org.eclipse.tracecompass.incubator.internal.callstack.core.Activator;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * The default base state provider for traces that implement the default
 * {@link InstrumentedCallStackAnalysis} with a process / thread grouping, using
 * the default values.
 *
 * Specific analyses will need to override it to specify the function
 * entry/exit, as well as how to get the process ID and thread ID.
 *
 * The attribute tree should have the following structure:
 *
 * <pre>
 * (root)
 *   +-- Processes
 *       +-- (PID 1000)
 *       |    +-- (TID 1000)
 *       |    |    +-- CallStack (stack-attribute)
 *       |    |         +-- 1
 *       |    |         +-- 2
 *       |    |        ...
 *       |    |         +-- n
 *       |    +-- (TID 1001)
 *       |         +-- CallStack (stack-attribute)
 *       |              +-- 1
 *       |              +-- 2
 *       |             ...
 *       |              +-- n
 *       |
 *       +-- (PID 2000)
 *            +-- (TID 2000)
 *                 +-- CallStack (stack-attribute)
 *                      +-- 1
 *                      +-- 2
 *                     ...
 *                      +-- n
 * </pre>
 *
 * where:
 * <ul>
 * <li>(PID n) is an attribute whose name is the display name of the process.
 * Optionally, its value can be an int representing the process id. Otherwise,
 * the attribute name can be set to the process id formatted as a string.</li>
 * <li>(TID n) is an attribute whose name is the display name of the thread.
 * Optionally, its value can be a long representing the thread id. Otherwise,
 * the attribute name can be set to the thread id formatted as a string.</li>
 * <li>"CallStack" is a stack-attribute whose pushed values are either a string,
 * int or long representing the function name or address in the call stack. The
 * type of value used must be constant for a particular CallStack.</li>
 * </ul>
 *
 * TODO: Use the same state provider as the one currently in trace Compass. It
 * just needs the ProcessName and ThreadName Aspects.
 *
 * @author Patrick Tasse
 */
@Deprecated(since="0.10.0", forRemoval=true)
public abstract class CallStackStateProvider extends AbstractTmfStateProvider {

    /**
     * Thread attribute
     *
     * @since 2.0
     */
    public static final String PROCESSES = "Processes"; //$NON-NLS-1$

    /**
     * Unknown process ID
     *
     * @since 2.0
     */
    public static final int UNKNOWN_PID = -1;

    /**
     * Unknown name
     *
     * @since 2.0
     */
    public static final String UNKNOWN = "UNKNOWN"; //$NON-NLS-1$

    /** CallStack state system ID */
    private static final String ID = "org.eclipse.linuxtools.tmf.callstack"; //$NON-NLS-1$

    private boolean fHasErrors = false;

    /**
     * Default constructor
     *
     * @param trace
     *            The trace for which we build this state system
     */
    public CallStackStateProvider(ITmfTrace trace) {
        super(trace, ID);
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        if (!considerEvent(event)) {
            return;
        }

        ITmfStateSystemBuilder ss = Objects.requireNonNull(getStateSystemBuilder());

        handleFunctionEntry(ss, event);
        handleFunctionExit(ss, event);
    }

    private void handleFunctionEntry(ITmfStateSystemBuilder ss, ITmfEvent event) {
        /* Check if the event is a function entry */
        Object functionEntryName = functionEntry(event);
        if (functionEntryName != null) {
            long timestamp = event.getTimestamp().toNanos();

            String processName = getProcessName(event);
            int processId = getProcessId(event);
            if (processName == null) {
                processName = (processId == UNKNOWN_PID) ? UNKNOWN : Integer.toString(processId);
            }
            int processQuark = ss.getQuarkAbsoluteAndAdd(PROCESSES, processName);
            ss.updateOngoingState(TmfStateValue.newValueInt(processId), processQuark);

            String threadName = getThreadName(event);
            long threadId = getThreadId(event);
            if (threadName == null) {
                threadName = Long.toString(threadId);
            }
            int threadQuark = ss.getQuarkRelativeAndAdd(processQuark, threadName);
            ss.updateOngoingState(TmfStateValue.newValueLong(threadId), threadQuark);

            int callStackQuark = ss.getQuarkRelativeAndAdd(threadQuark, InstrumentedCallStackAnalysis.CALL_STACK);
            ss.pushAttribute(timestamp, functionEntryName, callStackQuark);
            return;
        }
    }

    private void handleFunctionExit(ITmfStateSystemBuilder ss, ITmfEvent event) {
        /* Check if the event is a function exit */
        Object functionExitState = functionExit(event);
        // FIXME: since
        if (functionExitState != null) {
            long timestamp = event.getTimestamp().toNanos();
            String processName = getProcessName(event);
            if (processName == null) {
                int processId = getProcessId(event);
                processName = (processId == UNKNOWN_PID) ? UNKNOWN : Integer.toString(processId);
            }
            String threadName = getThreadName(event);
            if (threadName == null) {
                threadName = Long.toString(getThreadId(event));
            }
            int quark = ss.getQuarkAbsoluteAndAdd(PROCESSES, processName, threadName, InstrumentedCallStackAnalysis.CALL_STACK);
            Object poppedValue = ss.popAttributeObject(timestamp, quark);
            /*
             * Verify that the value we are popping matches the one in the
             * event field, unless the latter is undefined.
             */
            if (!fHasErrors && !functionExitState.equals(poppedValue)) {
                Activator.getInstance().logWarning(NLS.bind(Messages.CallStackStateProvider_EventDescription, event.getName(),
                        event.getTimestamp().getValue()) + ": " //$NON-NLS-1$
                        + NLS.bind( Messages.CallStackStateProvider_UnmatchedPoppedValue,
                                functionExitState,
                                poppedValue));
                    fHasErrors = true;
            }
        }
    }

    /**
     * Restrict the return type for {@link ITmfStateProvider#getNewInstance}.
     *
     * @since 2.0
     */
    @Override
    public abstract CallStackStateProvider getNewInstance();

    /**
     * Check if this event should be considered at all for function entry/exit
     * analysis. This check is only run once per event, before
     * {@link #functionEntry} and {@link #functionExit} (to avoid repeating
     * checks in those methods).
     *
     * @param event
     *            The event to check
     * @return If false, the event will be ignored by the state provider. If
     *         true processing will continue.
     */
    protected abstract boolean considerEvent(ITmfEvent event);

    /**
     * Check an event if it indicates a function entry.
     *
     * @param event
     *            An event to check for function entry
     * @return The object representing the function being entered, or null
     *         if not a function entry
     * @since 2.0
     */
    protected abstract @Nullable Object functionEntry(ITmfEvent event);

    /**
     * Check an event if it indicates a function exit.
     *
     * @param event
     *            An event to check for function exit
     * @return The object representing the function being exited, or
     *         TmfStateValue#nullValue() if the exited function is undefined,
     *         or null if not a function exit.
     * @since 2.0
     */
    protected abstract @Nullable Object functionExit(ITmfEvent event);

    /**
     * Return the process ID of a function entry event.
     * <p>
     * Use {@link #UNKNOWN_PID} if it is not known.
     *
     * @param event
     *            The event
     * @return The process ID
     * @since 2.0
     */
    protected abstract int getProcessId(ITmfEvent event);

    /**
     * Return the process name of a function entry event.
     *
     * @param event
     *            The event
     * @return The process name (as will be shown in the view) or null to use
     *         the process ID formatted as a string (or {@link #UNKNOWN})
     * @since 2.0
     */
    protected @Nullable String getProcessName(ITmfEvent event) {
        /* Override to provide a process name */
        Object resolved = TmfTraceUtils.resolveEventAspectOfClassForEvent(event.getTrace(), ProcessNameAspect.class, event);
        return (resolved instanceof String) ? (String) resolved : null;
    }

    /**
     * Return the thread id of a function entry event.
     *
     * @param event
     *            The event
     * @return The thread id
     * @since 2.0
     */
    protected abstract long getThreadId(ITmfEvent event);

    /**
     * Return the thread name of a function entry or exit event.
     *
     * @param event
     *            The event
     * @return The thread name (as will be shown in the view) or null to use the
     *         thread id formatted as a string
     */
    protected @Nullable String getThreadName(ITmfEvent event) {
        /* Override to provide a thread name */
        Object resolved = TmfTraceUtils.resolveEventAspectOfClassForEvent(event.getTrace(), ThreadNameAspect.class, event);
        return (resolved instanceof String) ? (String) resolved : null;
    }
}
