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

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import org.eclipse.tracecompass.incubator.internal.ftrace.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceEvent;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFileCPU;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceHeaderInfo;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;

/**
 * A reader for Binary FTrace where data is divided into sections by CPUs, and
 * in each section time is monotonic. It utilizes a priority queue to select the
 * next event to be parsed.
 *
 * @author Matthew Khouzam
 * @author Hoang Thuan Pham
 *
 */
public class BinaryFTraceReader implements AutoCloseable {
    private static final int MIN_PRIO_SIZE = 16;

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /**
     * The trace to read from.
     */
    private final BinaryFTraceHeaderInfo fTrace;

    /**
     * Vector of all the pages for the current pages and their iterators.
     */
    private final List<BinaryFTraceCPUSectionIterator> fIterators = Collections.synchronizedList(new ArrayList<BinaryFTraceCPUSectionIterator>());

    /**
     * Priority queue to order the trace file readers by timestamp.
     */
    private PriorityQueue<BinaryFTraceCPUSectionIterator> fPrio;

    /**
     * Array to count the number of event per trace file.
     */
    private long[] fEventCountPerTraceFile;

    /**
     * Timestamp of the first event in the trace
     */
    private long fStartTime;

    /**
     * Timestamp of the last event read so far
     */
    private long fEndTime;

    /**
     * Boolean to indicate if the BinaryFTraceReader has been closed
     */
    private boolean fClosed = false;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Constructs a BinaryFTraceReader to read a trace.
     *
     * @param trace
     *            The trace to read from.
     * @throws IOException
     *             if an error occurs
     */
    public BinaryFTraceReader(BinaryFTraceHeaderInfo trace) throws IOException {
        fTrace = trace;
        fIterators.clear();

        /**
         * Create the trace file readers.
         */
        createStreamInputReaders();

        /**
         * Populate the timestamp-based priority queue.
         */
        populateStreamInputReaderHeap();

        /**
         * Get the start time of this trace bear in mind that the trace could be
         * empty. Set the start time should be in nanoseconds of time since boot.
         */
        fStartTime = 0;
        if (hasMoreEvents()) {
            fStartTime = getTopStream().getCurrentTimeStamp();
            setEndTime(fStartTime);
        }
    }

    /**
     * Dispose the BinaryTraceReader
     */
    @Override
    public void close() {
        synchronized (fIterators) {
            for (BinaryFTraceCPUSectionIterator iterator : fIterators) {
                if (iterator != null) {
                    try {
                        iterator.close();
                    } catch (IOException e) {
                        Activator.getInstance().logInfo(e.getMessage(), e);
                    }
                }
            }
            fIterators.clear();
        }
        fPrio.clear();
        fClosed = true;
    }

    // ------------------------------------------------------------------------
     // Getters/Setters/Predicates
     // ------------------------------------------------------------------------

    /**
     * Return the start time of this trace (== timestamp of the first event) in
     * nanoseconds since the machine on which the trace is recorded booted.
     *
     * @return the trace start time
     */
    public long getStartTime() {
        return fStartTime;
    }

    /**
     * Set the trace's end time, must be in nanoseconds since boot
     *
     * @param endTime
     *            The end time to use.
     */
    protected final void setEndTime(long endTime) {
        fEndTime = endTime;
    }

    /**
     * Get the priority queue of this trace reader.
     *
     * @return The priority queue of input readers
     */
    protected PriorityQueue<BinaryFTraceCPUSectionIterator> getPrio() {
        return fPrio;
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    /**
     * Creates one trace file reader per trace file contained in the trace.
     *
     * @throws IOException
     *             if an error occurs
     */
    private void createStreamInputReaders() throws IOException {
        /*
         * For each stream.
         */
        for (BinaryFTraceFileCPU cpu : fTrace.getCpus()) {
            BinaryFTraceCPUSectionIterator iterator = new BinaryFTraceCPUSectionIterator(cpu, fTrace);
            fIterators.add(iterator);
        }

        /*
         * Create the array to count the number of event per trace file.
         */
        fEventCountPerTraceFile = new long[fIterators.size()];
    }

    /**
     * Returns whether or not this BinaryFTraceReader has been closed
     *
     * @return true if it has been closed, false else
     */
    public boolean isClosed() {
        return fClosed;
    }

    /**
     * Initializes the priority queue used to choose the trace file with the
     * lower next event timestamp.
     */
    private void populateStreamInputReaderHeap() {
        if (fIterators.isEmpty()) {
            fPrio = new PriorityQueue<>(MIN_PRIO_SIZE,
                    new BinaryFTraceCPUSectionIteratorComparator());
            return;
        }

        /*
         * Create the priority queue with a size twice as bigger as the number
         * of reader in order to avoid constant resizing.
         */
        fPrio = new PriorityQueue<>(
                Math.max(fIterators.size() * 2, MIN_PRIO_SIZE),
                new BinaryFTraceCPUSectionIteratorComparator());

        int pos = 0;

        for (BinaryFTraceCPUSectionIterator iterator : fIterators) {
            /*
             * Add each trace file reader in the priority queue, if we are able
             * to read an event from it.
             */
            BinaryFTraceResponse readNextEvent = iterator.readNextEvent();
            if (readNextEvent != BinaryFTraceResponse.ERROR) {
                fPrio.add(iterator);

                fEventCountPerTraceFile[pos] = 0;

                pos++;
            }
        }
    }

    /**
     * Does the trace have more events?
     *
     * @return true if yes.
     */
    public final boolean hasMoreEvents() {
        return !fPrio.isEmpty();
    }

    /**
     * Gets the stream with the oldest event
     *
     * @return the stream with the oldest event
     */
    public BinaryFTraceCPUSectionIterator getTopStream() {
        return fPrio.peek();
    }

    /**
     * Seek to the given timestamp
     *
     * @param timestamp
     *            The timestamp to seek to
     * @return true if the seek was successful, false otherwise.
     * @throws IOException
     *             if the seek fails
     */
    public boolean seek(long timestamp) throws IOException {
        /*
         * Remove all the trace readers from the priority queue
         */
        fPrio.clear();
        long newEndTime = TmfTimestamp.BIG_CRUNCH.toNanos();

        for (int i = 0; i < fIterators.size(); i++) {
            BinaryFTraceCPUSectionIterator currentIterator = fIterators.get(i);
            /*
             * Binary FTrace data is paged, for each page there is an iterator
             * So the seek might return a new iterator if the event is in a
             * different page
             */
            currentIterator.seek(timestamp);

            /*
             * Add it to the priority queue if there is a current event.
             */
            BinaryFTraceEvent currentEvent = currentIterator.getCurrentEvent();
            if (currentEvent != null) {
                fPrio.add(currentIterator);
                newEndTime = Math.min(currentEvent.getTimeSinceBoot(), newEndTime);
            }
        }

        boolean ret = hasMoreEvents();
        // Set the new timestamp if there are more events to be read
        if (ret) {
            setEndTime(Math.max(getEndTime(), newEndTime));
        }

        return ret;
    }

    /**
     * Go to the next event.
     *
     * @return True if an event was read.
     * @throws IOException
     *             if an error occurs
     */
    public boolean advance() throws IOException {
        /*
         * Remove the reader from the top of the priority queue.
         */
        BinaryFTraceCPUSectionIterator top = fPrio.poll();

        /*
         * If the queue was empty.
         */
        if (top == null) {
            return false;
        }

        /*
         * Read the next event of this reader.
         */
        switch (top.readNextEvent()) {
        case OK: {
            /*
             * Add it back in the queue.
             */
            fPrio.add(top);
            /*
             * We're in OK, there's a guaranteed top#getCurrentEvent() unless
             * another thread does something bad.
             */
            BinaryFTraceEvent currentEvent = checkNotNull(top.getCurrentEvent());
            setEndTime(Math.max(currentEvent.getTimeSinceBoot(), getEndTime()));
            break;
        }
        case FINISH:
            break;
        case ERROR:
        default:
            // something bad happened
        }
        /*
         * If there is no reader in the queue, it means the trace reader reached
         * the end of the trace.
         */
        return hasMoreEvents();
    }

    /**
     * Gets the last event timestamp that was read. This is NOT necessarily the
     * last event in a trace, just the last one read so far. Time is in
     * nanoseconds since boot.
     *
     * @return the last event
     */
    public long getEndTime() {
        return fEndTime;
    }
}
