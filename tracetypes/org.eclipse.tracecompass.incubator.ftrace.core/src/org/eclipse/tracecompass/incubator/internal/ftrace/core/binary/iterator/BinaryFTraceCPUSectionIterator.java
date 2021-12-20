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

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceEvent;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceCPUDataPage;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFileCPU;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceHeaderInfo;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;

/**
 * A wrapper class to group multiple BinaryFTraceCPUPageIterator under one
 * iterator. This wrapper can be considered as an iterator for a single CPU
 * section.
 *
 * @Author: Hoang Thuan Pham
 */
public class BinaryFTraceCPUSectionIterator implements AutoCloseable {
    private final @Nullable BinaryFTraceHeaderInfo headerInfo;

    private @Nullable BinaryFTraceCPUPageIterator fCurrPageIterator;

    /** The first page of the CPU section, required to reset the iterator **/
    private BinaryFTraceCPUDataPage fHeadPage;

    /**
     * Constructor
     *
     * @param cpu
     *            The CPU section to iterator to loop over
     * @param headerInfo
     *            The trace header
     * @throws IOException
     *             If fail to obtain the iterator
     */
    public BinaryFTraceCPUSectionIterator(BinaryFTraceFileCPU cpu, BinaryFTraceHeaderInfo headerInfo) throws IOException {
        this.headerInfo = headerInfo;

        if (!cpu.getPages().isEmpty()) {
            fHeadPage = cpu.getPages().get(0); // Get the first page
            fCurrPageIterator = BinaryFTraceIteratorHelper.getPageIterator(fHeadPage, headerInfo);
        } else {
            fHeadPage = null;
            fCurrPageIterator = null;
        }

    }

    /**
     * Read the next event from the section. If the iterator of a page reaches
     * the end, this method will try to check if there is a next page linked to
     * this iterator, if not there are no more events.
     *
     * @return BinaryFTraceResponse A response indicating the status of reading
     *         the next event.
     */
    public BinaryFTraceResponse readNextEvent() {
        BinaryFTraceCPUPageIterator iter = fCurrPageIterator;
        BinaryFTraceResponse response = BinaryFTraceResponse.ERROR;

        if (iter != null) {
            try {
                response = iter.readNextEvent();

                // An iterator has finished reading
                if (response == BinaryFTraceResponse.FINISH) {
                    // If there are more iterators, we move to the next
                    if (iter.getPage().getNextPage() != null) {

                        // Close the current iterator
                        iter.close();

                        // Get the next iterator
                        iter = BinaryFTraceIteratorHelper.getPageIterator(iter.getPage().getNextPage(), headerInfo);

                        // Read the next event if the iterator is not null
                        if (iter != null) {
                            response = iter.readNextEvent();
                        }
                    } else {
                        // There is no more event
                        response = BinaryFTraceResponse.FINISH;
                    }
                }
            } catch (IOException ex) {
                Activator.getInstance().logInfo("An error occured while reading the next trace event", ex); //$NON-NLS-1$
            }

            fCurrPageIterator = iter;
        }

        return response; // Returns the response from reading the next event
    }

    /**
     * Get the current timestamp of the iterator. This is a timestamp for the
     * iterator to calculate the actual timestamp of events with payload and
     * perform seeks. Hence, it should not be used as timestamp for events with
     * payload.
     *
     * @return The current timestamp of the CPU section iterator.
     */
    public long getCurrentTimeStamp() {
        BinaryFTraceCPUPageIterator iter = fCurrPageIterator;
        if (iter != null) {
            return iter.getCurrentTimeStamp();
        }

        return TmfTimestamp.BIG_BANG.toNanos();
    }

    /**
     * Close the current iterator.
     *
     * @throws IOException
     *             if cannot close the current iterator
     */
    @Override
    public void close() throws IOException {
        BinaryFTraceCPUPageIterator iter = fCurrPageIterator;
        if (iter != null) {
            iter.close();
        }
    }

    /**
     * Get the current event of this iterator lazily.
     *
     * @return The current event as a BinaryFTrace event.
     * @throws IOException
     *             if there is an error reading the event, likely because of
     *             byte buffer failure.
     */
    public @Nullable BinaryFTraceEvent getCurrentEvent() throws IOException {
        BinaryFTraceCPUPageIterator iter = fCurrPageIterator;
        if (iter != null) {
            return iter.getCurrentEvent();
        }

        return null;
    }

    /**
     * Seek the iterator to a specific timestamp. The iterator will try to find
     * the event with the the timestamp at least equals to the timestamp
     * parameter.
     *
     * @param timestamp
     *            the timestamp to seek to.
     * @return True if the seek is successful
     * @throws IOException
     *             If the iterator cannot locate an timestamp.
     */
    public boolean seek(long timestamp) throws IOException {
        // First we reset the iterator
        resetIterator();

        BinaryFTraceCPUDataPage currPage = fHeadPage;
        boolean ret = false;

        if (currPage != null) {
            BinaryFTraceCPUDataPage nextPage = currPage.getNextPage();

            if (timestamp > currPage.getTimeStamp()) {
                while (nextPage != null && timestamp >= nextPage.getTimeStamp()) {
                    currPage = nextPage;
                    nextPage = currPage.getNextPage();
                }
            }

            BinaryFTraceCPUPageIterator iter = BinaryFTraceIteratorHelper.getPageIterator(currPage, headerInfo);
            if (iter != null) {
                ret = iter.seek(timestamp);
            }

            /**
             * There is no events with ts equals or larger than the parameter
             * time stamp in this page
             **/
            if (!ret && nextPage != null) {
                /**
                 * We know for sure that all the events on the next page will
                 * have a time stamp larger than the parameter time stamp
                 **/
                iter = BinaryFTraceIteratorHelper.getPageIterator(nextPage, headerInfo);
                if (iter != null) {
                    ret = iter.seek(timestamp);
                }
            }

            // Update the current iterator of the object
            fCurrPageIterator = iter;
        }

        return ret;
    }

    /**
     * Reset the iterator from the start.
     * @throws IOException
     *
     * @throws Exception
     *             if there is an issue getting the iterator of the head page.
     */
    private void resetIterator() throws IOException {
        BinaryFTraceCPUPageIterator iter = fCurrPageIterator;
        if (iter != null) {
            iter.close();
        }

        fCurrPageIterator = BinaryFTraceIteratorHelper.getPageIterator(fHeadPage, headerInfo);
    }
}
