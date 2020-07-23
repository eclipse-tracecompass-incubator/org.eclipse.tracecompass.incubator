/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.traceevent.core.analysis.callstack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxPidAspect;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxTidAspect;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.incubator.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.incubator.callstack.core.base.EdgeStateValue;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackStateProvider;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.event.ITraceEventConstants;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.event.TraceEventAspects;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.event.TraceEventField;
import org.eclipse.tracecompass.incubator.internal.traceevent.core.event.TraceEventPhases;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfAttributePool;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Trace event callstack provider
 *
 * Has links, so we have a tmfGraph.
 *
 * @author Matthew Khouzam
 *
 */
public class TraceEventCallStackProvider extends CallStackStateProvider {

    private static final int VERSION_NUMBER = 7;
    private static final int UNSET_ID = -1;
    static final String EDGES = "EDGES"; //$NON-NLS-1$

    private static final Function<EventTreeKey, Integer> FUNCTION = s -> {
        try {
            return Integer.decode(s.fId);
        } catch (NumberFormatException e) {
            return UNSET_ID;
        }
    };

    private ITmfTimestamp fSafeTime;

    /**
     * A map of tid/stacks of timestamps
     */
    private final Map<Integer, Deque<Long>> fStack = new TreeMap<>();

    private final ITmfEventAspect<?> fIdAspect;

    private final ITmfEventAspect<?> fCatAspect;

    /**
     * Map of trace event scope ID string to their start times
     */
    private final Map<EventTreeKey, Long> fEdgeStartTimes = new HashMap<>();
    /**
     * Map of trace event scope ID string to the source {@link HostThread} of the edge.
     */
    private final Map<EventTreeKey, HostThread> fEdgeSrcHosts = new HashMap<>();
    /**
     * Cache of trace event scope ID string to their parsed values
     */
    private final Map<EventTreeKey, Integer> fIdCache = new HashMap<>();

    private static class EventTreeKey {
        private final String fCategory;
        private final String fId;
        private final @Nullable String fScope;

        public EventTreeKey(String category, String id) {
            fCategory = category;
            fId = id;
            fScope = null;
        }

        public EventTreeKey(String category, String id, String scope) {
            fCategory = category;
            fId = id;
            fScope = scope;
        }

        @Override
        public String toString() {
            return fCategory + ':' + fId + (fScope == null ? "" : ':' + fScope); //$NON-NLS-1$
        }

        @Override
        public boolean equals(Object arg0) {
            if (!(arg0 instanceof EventTreeKey)) {
                return false;
            }
            EventTreeKey other = (EventTreeKey) arg0;
            boolean catAndIdEqual = Objects.equals(fCategory, other.fCategory) && Objects.equals(fId, other.fId);
            if (catAndIdEqual && fScope != null && other.fScope != null) {
                return Objects.equals(fScope, other.fScope);
            }
            return catAndIdEqual;
        }

        @Override
        public int hashCode() {
            return Objects.hash(fCategory, fId, fScope);
        }
    }

    /**
     * Constructor
     *
     * @param trace
     *            the trace to follow
     */
    public TraceEventCallStackProvider(@NonNull ITmfTrace trace) {
        super(trace);
        ITmfStateSystemBuilder stateSystemBuilder = getStateSystemBuilder();
        if (stateSystemBuilder != null) {
            int quark = stateSystemBuilder.getQuarkAbsoluteAndAdd("dummy entry to make gpu entries work"); //$NON-NLS-1$
            stateSystemBuilder.modifyAttribute(0, 0, quark);
        }
        fSafeTime = trace.getStartTime();
        fIdAspect = TraceEventAspects.ID_ASPECT;
        fCatAspect = TraceEventAspects.CATEGORY_ASPECT;
    }

    @Override
    protected @Nullable String getProcessName(@NonNull ITmfEvent event) {
        String pName = super.getProcessName(event);
        if (pName == null) {
            pName = event.getContent().getFieldValue(String.class, ITraceEventConstants.PID);
        }

        if (pName == null) {
            int processId = getProcessId(event);
            pName = (processId == UNKNOWN_PID) ? UNKNOWN : Integer.toString(processId);
        }

        return pName;
    }

    @Override
    protected @Nullable String getThreadName(@NonNull ITmfEvent event) {
        String tName = super.getThreadName(event);
        if (tName == null) {
            tName = event.getContent().getFieldValue(String.class, "tname"); //$NON-NLS-1$
        }

        if (tName == null) {
            long threadId = getThreadId(event);
            tName = (threadId == IHostModel.UNKNOWN_TID) ? UNKNOWN : Long.toString(threadId);
        }

        return tName;
    }

    @Override
    protected int getProcessId(@NonNull ITmfEvent event) {
        Integer pid = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), LinuxPidAspect.class, event);
        if (pid == null) {
            // Fallback to a pid field in the event
            pid = event.getContent().getFieldValue(Integer.class, ITraceEventConstants.PID);
        }
        return pid == null ? UNKNOWN_PID : pid.intValue();
    }

    @Override
    protected long getThreadId(@NonNull ITmfEvent event) {
        Integer tid = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), LinuxTidAspect.class, event);
        if (tid == null) {
            // Fallback to a tid field in the event
            tid = event.getContent().getFieldValue(Integer.class, ITraceEventConstants.TID);
        }
        return tid == null ? IHostModel.UNKNOWN_TID : tid.intValue();
    }

    @Override
    public int getVersion() {
        return VERSION_NUMBER;
    }

    @Override
    public @NonNull CallStackStateProvider getNewInstance() {
        return new TraceEventCallStackProvider(getTrace());
    }

    @Override
    protected boolean considerEvent(@NonNull ITmfEvent event) {
        String ph = event.getContent().getFieldValue(String.class, ITraceEventConstants.PHASE);
        return (ph != null);
    }

    @Override
    protected @Nullable Object functionEntry(@NonNull ITmfEvent event) {
        if (isEntry(event)) {
            return event.getName();
        }
        return null;
    }

    private static boolean isEntry(ITmfEvent event) {
        String phase = event.getContent().getFieldValue(String.class, ITraceEventConstants.PHASE);
        return TraceEventPhases.NESTABLE_START.equals(phase) || TraceEventPhases.DURATION_START.equals(phase);
    }

    @Override
    protected @Nullable Object functionExit(@NonNull ITmfEvent event) {
        if (isExit(event)) {
            return event.getName();
        }
        return null;
    }

    private static boolean isExit(ITmfEvent event) {
        String phase = event.getContent().getFieldValue(String.class, ITraceEventConstants.PHASE);
        return TraceEventPhases.NESTABLE_END.equals(phase) || TraceEventPhases.DURATION_END.equals(phase);
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        if (!considerEvent(event)) {
            return;
        }
        ITmfStateSystemBuilder ss = Objects.requireNonNull(getStateSystemBuilder());

        /* Check if the event is a function entry */
        long timestamp = event.getTimestamp().toNanos();
        updateCloseCandidates(ss, timestamp);
        String processName = getProcessName(event);
        String ph = event.getContent().getFieldValue(String.class, ITraceEventConstants.PHASE);
        if (ph == null) {
            return;
        }
        switch (ph) {
        case TraceEventPhases.NESTABLE_START:
        case TraceEventPhases.DURATION_START:
            handleStart(event, ss, timestamp, processName);
            break;

        case TraceEventPhases.DURATION:
            Number duration = event.getContent().getFieldValue(Number.class, ITraceEventConstants.DURATION);
            if (duration != null) {
                handleComplete(event, ss, processName);
            }
            break;

        case TraceEventPhases.NESTABLE_END:
        case TraceEventPhases.DURATION_END:
            handleEnd(event, ss, timestamp, processName);
            break;

        case TraceEventPhases.FLOW_START:
            updateSLinks(event, ss, timestamp, processName);
            break;

        case TraceEventPhases.FLOW_STEP:
            updateTLinks(event, ss, timestamp, processName);
            break;

        case TraceEventPhases.FLOW_END:
            updateFLinks(event, ss, timestamp, processName);
            break;
        default:
            return;
        }
    }

    private void updateCloseCandidates(ITmfStateSystemBuilder ss, long timestamp) {
        for (Entry<Integer, Deque<Long>> stackEntry : fStack.entrySet()) {
            Deque<Long> stack = stackEntry.getValue();
            if (!stack.isEmpty()) {
                Long closeCandidate = stack.pop();
                while (closeCandidate != null && closeCandidate < timestamp) {
                    ss.popAttribute(closeCandidate, stackEntry.getKey());
                    closeCandidate = (stack.isEmpty()) ? null : stack.pop();
                }
                if (closeCandidate != null) {
                    stack.push(closeCandidate);
                }
            }
        }
    }

    private EventTreeKey getEventTreeKey(ITmfEvent event) {
        String sId = event.getContent().getFieldValue(String.class, ITraceEventConstants.ID);
        if (sId == null) {
            Object resolve = fIdAspect.resolve(event);
            if (resolve == null) {
                resolve = Integer.valueOf(0);
            }
            sId = String.valueOf(resolve);
        }
        String sCat = event.getContent().getFieldValue(String.class, ITraceEventConstants.CATEGORY);
        if (sCat == null) {
            Object resolve = fCatAspect.resolve(event);
            if (resolve == null) {
                resolve = Integer.valueOf(0);
            }
            sCat = String.valueOf(resolve);
        }
        String sScope = event.getContent().getFieldValue(String.class, ITraceEventConstants.SCOPE);

        return (sScope == null) ? new EventTreeKey(sCat, sId) : new EventTreeKey(sCat, sId, sScope);

    }

    private void updateFLinks(ITmfEvent event, ITmfStateSystemBuilder ss, long ts, String processName) {
        EventTreeKey key = getEventTreeKey(event);

        String bindingPoint = event.getContent().getFieldValue(String.class, ITraceEventConstants.BINDING_POINT);
        if (bindingPoint == null) {
            bindingPoint = "n"; //$NON-NLS-1$
        }

        int tid = (int) getThreadId(event);

        Long startTime = fEdgeStartTimes.get(key);
        if (startTime == null) {
            // We don't have the starting point of the flow, ignore
            return;
        }

        HostThread srcHostThread = fEdgeSrcHosts.remove(key);
        HostThread currHostThread = new HostThread(event.getTrace().getHostId(), tid);

        // If binding point is "enclosing slice", make sure there is such a slice
        if (bindingPoint == "e" && !validateEnclosingSlice(event, ss, ts, processName, tid)) { //$NON-NLS-1$
            return;
        }

        if (srcHostThread != null) {
            int edgeQuark = getAvailableEdgeQuark(ss, startTime);

            Object edgeStateValue = new EdgeStateValue(fIdCache.computeIfAbsent(key, FUNCTION), srcHostThread, currHostThread);
            ss.modifyAttribute(startTime, edgeStateValue, edgeQuark);
            ss.modifyAttribute(ts, (Object) null, edgeQuark);
        }
    }

    private void updateTLinks(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss, long ts, String processName) {
        EventTreeKey key = getEventTreeKey(event);

        int tid = (int) getThreadId(event);

        Long startTime = fEdgeStartTimes.get(key);
        if (startTime == null) {
            // We don't have the starting point of the flow, ignore
            return;
        }

        HostThread srcHostThread = fEdgeSrcHosts.remove(key);
        HostThread currHostThread = new HostThread(event.getTrace().getHostId(), tid);

        // Scope is "enclosing slice", make sure there is such a slice
        if (!validateEnclosingSlice(event, ss, ts, processName, tid)) {
            return;
        }

        if (srcHostThread != null) {
            int edgeQuark = getAvailableEdgeQuark(ss, startTime);

            Object edgeStateValue = new EdgeStateValue(fIdCache.computeIfAbsent(key, FUNCTION), srcHostThread, currHostThread);
            ss.modifyAttribute(startTime, edgeStateValue, edgeQuark);
            ss.modifyAttribute(ts, (Object) null, edgeQuark);

        }
        // update data for next edge
        fEdgeStartTimes.put(key, ts);
        fEdgeSrcHosts.put(key, currHostThread);
    }

    private boolean validateEnclosingSlice(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss, long ts, String processName, int tid) {
        String threadName = getThreadName(event);
        if (threadName == null) {
            threadName = Long.toString(tid);
        }
        int callstackQuark = ss.optQuarkAbsolute(PROCESSES, processName, threadName, InstrumentedCallStackAnalysis.CALL_STACK);
        if (callstackQuark < 0) {
            // No callstack for this thread, no enclosing slice
            return false;
        }
        try {
            ITmfStateInterval interval = StateSystemUtils.querySingleStackTop(ss, ts, callstackQuark);
            if (interval == null || interval.getValue() == null) {
                // Nothing on the top of stack, so no enclosing slice
                return false;
            }
        } catch (AttributeNotFoundException | StateSystemDisposedException e) {
            // Other problem, no enclosing slice
            return false;
        }
        return true;
    }

    private void updateSLinks(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss, long ts, String processName) {
        EventTreeKey key = getEventTreeKey(event);

        int tid = (int) getThreadId(event);
        ITmfStateSystemBuilder ssb = getStateSystemBuilder();
        if (ssb == null) {
            return;
        }

        HostThread currHostThread = new HostThread(event.getTrace().getHostId(), tid);

        // Scope is "enclosing slice", make sure there is such a slice
        if (!validateEnclosingSlice(event, ss, ts, processName, tid)) {
            return;
        }

        fEdgeStartTimes.put(key, ts);
        fEdgeSrcHosts.put(key, currHostThread);
    }

    /**
     * Get an available quark to insert an {@link EdgeStateValue} from startTime to
     * the current end time. The {@link TmfAttributePool} cannot be used as it
     * cannot tell that a quark is available for a time range
     *
     * @param ssb
     *            the {@link ITmfStateSystemBuilder} for this analysis.
     * @param startTime
     *            the start time of the {@link EdgeStateValue}.
     * @return a quark which is available from start time to now (i.e. its ongoing
     *         value is <code>null</code> and its start time is smaller than the
     *         queried start time).
     */
    private static int getAvailableEdgeQuark(ITmfStateSystemBuilder ssb, long startTime) {
        int edgeRoot = ssb.getQuarkAbsoluteAndAdd(EDGES);
        List<@NonNull Integer> subQuarks = ssb.getSubAttributes(edgeRoot, false);

        for (int quark : subQuarks) {
            long start = ssb.getOngoingStartTime(quark);
            Object value = ssb.queryOngoing(quark);
            if (value == null && start <= startTime) {
                return quark;
            }
        }

        return ssb.getQuarkRelativeAndAdd(edgeRoot, Integer.toString(subQuarks.size()));
    }

    private void handleStart(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss, long timestamp, String processName) {
        Object functionBeginName = functionEntry(event);
        if (functionBeginName != null) {
            int processQuark = ss.getQuarkAbsoluteAndAdd(PROCESSES, processName);
            int pid = getProcessId(event);
            ss.modifyAttribute(timestamp, pid, processQuark);

            String threadName = getThreadName(event);
            long threadId = getThreadId(event);
            if (threadName == null) {
                threadName = Long.toString(threadId);
            }
            int threadQuark = ss.getQuarkRelativeAndAdd(processQuark, threadName);
            ss.modifyAttribute(timestamp, threadId, threadQuark);

            int callStackQuark = ss.getQuarkRelativeAndAdd(threadQuark, InstrumentedCallStackAnalysis.CALL_STACK);
            ss.pushAttribute(timestamp, functionBeginName, callStackQuark);
            prepareNextSlice(ss, callStackQuark, timestamp);
        }
    }

    private static void prepareNextSlice(ITmfStateSystemBuilder ss, int quark, long timestamp) {
        // Since the beginning can mark the beginning of more than one slice, make sure the next level in the stack is ready to be updated and has the same start time as the current one
        ss.pushAttribute(timestamp, StringUtils.EMPTY, quark);
        ss.popAttribute(timestamp, quark);
    }

    private void handleEnd(@NonNull ITmfEvent event, ITmfStateSystemBuilder ss, long timestamp, String processName) {
        /* Check if the event is a function exit */
        Object functionExitName = functionExit(event);
        if (functionExitName != null) {
            String pName = processName;

            if (pName == null) {
                int processId = getProcessId(event);
                pName = (processId == UNKNOWN_PID) ? UNKNOWN : Integer.toString(processId);
            }
            String threadName = getThreadName(event);
            if (threadName == null) {
                threadName = Long.toString(getThreadId(event));
            }
            int quark = ss.getQuarkAbsoluteAndAdd(PROCESSES, pName, threadName, InstrumentedCallStackAnalysis.CALL_STACK);
            // The function to end is not necessarily the tip of the stack. Unstack up to function name
            List<Object> callStack = getCallStack(ss, quark);
            int indexOf = callStack.lastIndexOf(functionExitName);
            // Function not found, just unstack the last one?
            if (indexOf < 0) {
                if (functionExitName.equals(TraceEventField.UNKNOWN_EXIT_EVENT) || functionExitName.equals(TraceEventField.UNKNOWN_DURATION_EXIT_EVENT)) {
                    // The event has no name, assume the last one in the stack
                    indexOf = callStack.size() - 1;
                } else {
                    // The event had a name, maybe the beginning was lost, update it in the stack, then pop it
                    int stackQuark = ss.optQuarkRelative(quark, String.valueOf(callStack.size() + 1));
                    if (stackQuark >= 0) {
                        ss.updateOngoingState(functionExitName, stackQuark);
                        ss.pushAttribute(timestamp, (Object) null, quark);
                    }
                    // Pop the last element
                    indexOf = callStack.size() - 1;
                }
            }
            // Pop all the attributes up to the exiting function
            for (int i = indexOf; i < callStack.size(); i++) {
                ss.popAttribute(timestamp, quark);
            }
        }
    }

    private static List<Object> getCallStack(ITmfStateSystemBuilder ss, int quark) {
        List<Object> callstackObjects = new ArrayList<>();
        Integer currentDepth = (Integer) ss.queryOngoing(quark);
        if (currentDepth == null) {
            return callstackObjects;
        }
        for (int i = 0; i < currentDepth; i++) {
            int stackQuark = ss.optQuarkRelative(quark, String.valueOf(i + 1));
            if (stackQuark < 0) {
                return callstackObjects;
            }
            callstackObjects.add(ss.queryOngoing(stackQuark));
        }
        return callstackObjects;
    }

    /**
     * This handles phase "complete" elements. They arrive by end time first, some
     * some flipping is being performed.
     *
     * @param event
     * @param ss
     * @param processName
     */
    private void handleComplete(ITmfEvent event, ITmfStateSystemBuilder ss, String processName) {

        ITmfTimestamp timestamp = event.getTimestamp();
        fSafeTime = fSafeTime.compareTo(timestamp) > 0 ? fSafeTime : timestamp;
        String currentProcessName = processName;
        if (currentProcessName == null) {
            int processId = getProcessId(event);
            currentProcessName = (processId == UNKNOWN_PID) ? UNKNOWN : Integer.toString(processId).intern();
        }
        int processQuark = ss.getQuarkAbsoluteAndAdd(PROCESSES, currentProcessName);
        long startTime = event.getTimestamp().toNanos();
        long end = startTime;
        Number duration = event.getContent().getFieldValue(Number.class, ITraceEventConstants.DURATION);
        if (duration != null) {
            end += Math.max(duration.longValue(), 0);
        }
        String threadName = getThreadName(event);
        long threadId = getThreadId(event);
        if (threadName == null) {
            threadName = Long.toString(threadId).intern();
        }
        int threadQuark = ss.getQuarkRelativeAndAdd(processQuark, threadName);
        ss.modifyAttribute(event.getTimestamp().toNanos(), threadId, threadQuark);

        int callStackQuark = ss.getQuarkRelativeAndAdd(threadQuark, InstrumentedCallStackAnalysis.CALL_STACK);
        ss.pushAttribute(startTime, event.getName(), callStackQuark);
        Deque<Long> stack = fStack.computeIfAbsent(callStackQuark, ArrayDeque::new);
        stack.push(end);
    }

}
