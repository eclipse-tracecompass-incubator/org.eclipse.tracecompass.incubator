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

package org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.iterator;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.context.BinaryFTraceLocation;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.context.BinaryFTraceLocationInfo;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceConstants;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceEvent;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceHeaderInfo;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.event.GenericFtraceEvent;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.event.GenericFtraceField;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.event.IGenericFtraceConstants;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.layout.GenericFtraceEventLayout;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.trace.BinaryFTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;

/**
 * An iterator that allows iteration over the events in a binary FTrace. The
 * file expected should be paged.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceIterator extends BinaryFTraceReader implements ITmfContext, Comparable<BinaryFTraceIterator> {
    private static final Comparator<BinaryFTraceIterator> COMPARATOR = Comparator.comparing(BinaryFTraceIterator::getRank);

    private static final String SYS_ENTER_EVENT_NAME = "sys_enter"; //$NON-NLS-1$
    private static final String SYS_EXIT_EVENT_NAME = "sys_exit"; //$NON-NLS-1$

    private final @NonNull BinaryFTrace fTrace;

    private BinaryFTraceLocation fCurLocation;
    private long fCurRank;

    private BinaryFTraceLocation fPreviousLocation;
    private GenericFtraceEvent fPreviousEvent;

    /**
     * An invalid {@link BinaryFTraceLocation} for trace events. This is the
     * default fallback value when a trace is empty, or an error occurred.
     */
    public static final BinaryFTraceLocation NULL_LOCATION = new BinaryFTraceLocation(BinaryFTraceLocation.INVALID_LOCATION);

    /**
     * Create a new CTF trace iterator, which initially points at the first
     * event in the trace.
     *
     * @param headerInfo
     *            The {@link BinaryFTraceHeaderInfo} linked to the trace. It
     *            should be provided by the corresponding 'ctfTmfTrace'.
     * @param ftrace
     *            The {@link BinaryFTraceDirectImp} to iterate over
     * @throws IOException
     *             If the iterator couldn't not be instantiated, probably due to
     *             a read error.
     */
    public BinaryFTraceIterator(BinaryFTraceHeaderInfo headerInfo, @NonNull BinaryFTrace ftrace) throws IOException {
        super(headerInfo);
        this.fTrace = ftrace;

        if (hasMoreEvents()) {
            // we try to skip the sys_exit and sys_enter events at the beginning
            // of the trace if needed
            skipToFirstValidEvent();

            fCurLocation = new BinaryFTraceLocation(ftrace.getStartTime());
            fCurRank = 0;
        } else {
            setUnknownLocation();
        }
    }

    /**
     * Return the event that the iterator currently points to.
     *
     * @return GenericFtraceEvent The current event that the iterator currently
     *         points to; null if there are no more events in the trace.
     * @throws IOException
     *             If reading the next event fails, most likely because of
     *             buffer overflow.
     */
    @SuppressWarnings("resource")
    public synchronized GenericFtraceEvent getCurrentEvent() throws IOException {
        final BinaryFTraceCPUSectionIterator top = super.getPrio().peek();
        if (top != null) {
            if (!fCurLocation.equals(fPreviousLocation)) {
                fPreviousLocation = fCurLocation;
                BinaryFTraceEvent event = top.getCurrentEvent();
                if (event != null) {
                    fPreviousEvent = parseEvent(event);
                }
            }
            return fPreviousEvent;
        }
        return null;
    }

    /**
     * Convert a {@link BinaryFTraceEvent} into a {@link GenericFTraceEvent}
     *
     * @param event
     *            A {@link BinaryFTraceEvent} to convert
     * @return the current event as a {@link GenericFTraceEvent}
     */
    private GenericFtraceEvent parseEvent(BinaryFTraceEvent event) {
        Integer pid = -1;
        Integer cpu = event.getCpu();
        Long timestampInNano = event.getTimeSinceBoot();

        // This function call guarantees that name will not be null
        String name = eventNameRewrite(event.getEventName());

        Object commonPidField = event.getFields().getOrDefault("common_pid", null); //$NON-NLS-1$
        if (commonPidField != null) {
            pid = ((Long) commonPidField).intValue();
        }
        Integer tid = pid;

        Object tgidField = event.getFields().getOrDefault("tgid", null); //$NON-NLS-1$
        if (tgidField != null) {
            int tgidNumeric = ((Long) tgidField).intValue();
            if (tgidNumeric != pid) {
                pid = tgidNumeric;
            }
        }

        Map<@NonNull String, @NonNull Object> fields = new HashMap<>();
        for (Entry<String, Object> field : event.getFields().entrySet()) {
            String key = field.getKey();
            Object value = field.getValue();

            if (value != null && !key.startsWith(BinaryFTraceConstants.EVENT_FORMAT_COMMON_FIELD_PREFIX)) {
                if (key.equals("parent_pid") && name.equals(GenericFtraceEventLayout.getInstance().eventSchedProcessFork())) { //$NON-NLS-1$
                    key = "pid"; //$NON-NLS-1$
                }

                fields.put(key, value);
            }
        }

        @SuppressWarnings("null")
        GenericFtraceField parsedEvent = new GenericFtraceField(name, cpu, timestampInNano, pid, tid, fields);
        return new GenericFtraceEvent(fTrace, fCurRank, parsedEvent);
    }

    @Override
    public void dispose() {
        close();
    }

    /**
     * Seek this iterator to a given location.
     *
     * @param ctfLocationData
     *            The LocationData representing the position to seek to
     * @return True if the seek was successful, false if there was an error
     *         seeking.
     * @throws IOException
     *             If an error occurred while seeking the event
     */
    public synchronized boolean seek(BinaryFTraceLocationInfo ctfLocationData) throws IOException {
        if (ctfLocationData.equals(BinaryFTraceLocation.INVALID_LOCATION)) {
            fCurLocation = NULL_LOCATION;
            return false;
        }

        // Avoid the cost of seeking at the current location.
        if (fCurLocation.getLocationInfo().equals(ctfLocationData)) {
            return super.hasMoreEvents();
        }

        // Update location to make sure the current event is updated
        fCurLocation = new BinaryFTraceLocation(ctfLocationData);

        // Adjust the timestamp depending on the trace's offset
        long seekToTimestamp = ctfLocationData.getTimestamp();
        boolean seekSuccess = seek(seekToTimestamp);

        /*
         * Check if there is already one or more events for that timestamp, and
         * assign the location index correctly
         */
        long index = positionIteratorByIndex(seekToTimestamp, ctfLocationData.getIndex());
        seekSuccess = seekSuccess && (index != -1);

        // Update the current location accordingly
        if (seekSuccess) {
            ITmfEvent currentEvent = getCurrentEvent();
            long time = Objects.requireNonNull(currentEvent).getTimestamp().getValue();
            fCurLocation = new BinaryFTraceLocation(new BinaryFTraceLocationInfo(time, time != seekToTimestamp ? 0 : index));
        } else {
            fCurLocation = NULL_LOCATION;
        }
        return seekSuccess;
    }

    @Override
    public boolean seek(long timestamp) {
        boolean seekSuccess = false;

        try {
            if (timestamp < 0) {
                seekSuccess = super.seek(0L);
            } else {
                seekSuccess = super.seek(timestamp);
            }

            /**
             * After finishing seeking we need to skip raw system event if
             * needed
             */
            seekSuccess = skipRawSystemEvents() && seekSuccess;
        } catch (Exception e) {
            Activator.getInstance().logInfo(e.getMessage(), e);
            seekSuccess = false;
        }

        return seekSuccess;
    }

    private long positionIteratorByIndex(long timestamp, long indexToSeek) throws IOException {
        boolean success = true;
        long currentIndex = 0;

        /**
         * Position the iterator at the right index among multiple events with
         * the same timestamp. A trace event with an index of n and timestamp t
         * is the n-th event with the timestamp t in a list of events that all
         * have the same timestamp t.
         */
        ITmfEvent currentEvent = getCurrentEvent();
        while (currentEvent != null && currentIndex < indexToSeek) {
            if (timestamp >= Objects.requireNonNull(currentEvent).getTimestamp().getValue()) {
                currentIndex++;
            } else {
                currentIndex = 0;
                break;
            }
            success = advance();
            currentEvent = getCurrentEvent();
        }

        // Return -1 if the action was unsuccessful
        if (!success) {
            return -1;
        }

        return currentIndex;
    }

    @Override
    public boolean advance() throws IOException {
        boolean readNextEventSuccess = readNextEvent();
        return readNextEventSuccess && skipRawSystemEvents();
    }

    private boolean readNextEvent() throws IOException {
        boolean hasMoreEvents = false;
        try {
            hasMoreEvents = super.advance();
        } catch (Exception ex) {
            Activator.getInstance().logInfo(ex.getMessage(), ex);
        }

        if (hasMoreEvents) {
            long timestamp = fCurLocation.getLocationInfo().getTimestamp();
            final long timestampValue = getCurrentTimestamp();
            if (timestamp == timestampValue) {
                long index = fCurLocation.getLocationInfo().getIndex();
                fCurLocation = new BinaryFTraceLocation(timestampValue, index + 1);
            } else {
                fCurLocation = new BinaryFTraceLocation(timestampValue, 0L);
            }
        } else {
            fCurLocation = NULL_LOCATION;
        }
        return hasMoreEvents;
    }

    private boolean skipRawSystemEvents() throws IOException {
        // Position the trace at the first non raw event
        GenericFtraceEvent event = getCurrentEvent();

        boolean ret = true;
        while (isRawSystemEvent(event)) {
            ret = readNextEvent();
            event = getCurrentEvent();
        }

        return ret;
    }

    private boolean skipToFirstValidEvent() throws IOException {
        // This means we are initializing the trace
        boolean initTrace = false;
        if (fCurLocation == null) {
            initTrace = true;

            // Temporarily create the location so that the skip would work
            fCurLocation = new BinaryFTraceLocation(fTrace.getStartTime());
            fCurRank = 0;
        }

        // Position the trace at the first non raw event
        boolean ret = skipRawSystemEvents();

        if (initTrace) {
            fCurLocation = null;
        }

        return ret;
    }

    private static boolean isRawSystemEvent(GenericFtraceEvent event) {
        return event.getName().equals(SYS_ENTER_EVENT_NAME) || event.getName().equals(SYS_EXIT_EVENT_NAME);
    }

    /**
     * Searches for certain event names and rewrites them in order for different
     * analysis to work.
     *
     * @param name
     *            The name of the event
     * @return The new or original event name
     */
    private static String eventNameRewrite(@Nullable String name) {
        if (name == null) {
            return ""; //$NON-NLS-1$
        }

        // Rewrite syscall exit events to conform to syscall analysis.
        if (name.startsWith(IGenericFtraceConstants.FTRACE_SYSCALL_EXIT_TRACECMD_PREFIX)) {
            return IGenericFtraceConstants.FTRACE_EXIT_SYSCALL;
        }

        /*
         * Rewrite syscall enter from trace-cmd traces to conform to syscall
         * analysis.
         */
        if (name.startsWith(IGenericFtraceConstants.FTRACE_SYSCALL_ENTER_TRACECMD_PREFIX)) {
            String newName = name.replaceFirst(IGenericFtraceConstants.FTRACE_SYSCALL_ENTER_TRACECMD_PREFIX, IGenericFtraceConstants.FTRACE_SYSCALL_PREFIX);
            if (newName != null) {
                return newName;
            }
        }

        return name;
    }

    /**
     * Return the current timestamp location pointed to by the iterator. This is
     * the timestamp for use in CtfLocation, not the event timestamp.
     *
     * @return 0 if there is no more event to read; else return the current
     *         timestamp location.
     * @throws IOException
     *             if an error occurred while getting the event
     */
    @SuppressWarnings("resource")
    public synchronized long getCurrentTimestamp() throws IOException {
        final BinaryFTraceCPUSectionIterator top = super.getPrio().peek();

        if (top != null) {
            BinaryFTraceEvent currentEvent = top.getCurrentEvent();
            if (currentEvent != null) {
                return currentEvent.getTimeSinceBoot();
            }
        }

        return 0;
    }

    private void setUnknownLocation() {
        fCurLocation = NULL_LOCATION;
        fCurRank = UNKNOWN_RANK;
    }

    @Override
    public long getRank() {
        return fCurRank;
    }

    @Override
    public void increaseRank() {
        if (hasValidRank()) {
            fCurRank++;
        }
    }

    @Override
    public boolean hasValidRank() {
        return (getRank() >= 0);
    }

    @Override
    public void setRank(long rank) {
        fCurRank = rank;
    }

    @Override
    public void setLocation(ITmfLocation location) {
        try {
            seek(((BinaryFTraceLocation) location).getLocationInfo());
            fCurLocation = (BinaryFTraceLocation) location;
        } catch (Exception ex) {
            Activator.getInstance().logError("There is an error setting the location of the iterator"); //$NON-NLS-1$
        }
    }

    @Override
    public BinaryFTraceLocation getLocation() {
        return fCurLocation;
    }

    @Override
    public int compareTo(final BinaryFTraceIterator o) {
        return COMPARATOR.compare(this, o);
    }
}
