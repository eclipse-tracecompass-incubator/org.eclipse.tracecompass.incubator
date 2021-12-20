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

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceEvent;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceEventDefinition;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceCPUDataPage;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceEventFormat;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceHeaderInfo;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.parser.BinaryFTraceByteBuffer;

/**
 * An iterator to loop through events in a single CPU data page. A CPU maybe
 * paged if the trace type is fly_record. This class is used to loop through the
 * event of a single CPUPage in a CPU section (the same CPU).
 *
 * @Author Hoang Thuan Pham
 */
public class BinaryFTraceCPUPageIterator implements Closeable {
    // The buffer
    private @Nullable BinaryFTraceByteBuffer fBuffer;

    private final BinaryFTraceHeaderInfo fFileHeader;
    private final long fLimit;

    // Reference to the page that is associated with this iterator
    private final BinaryFTraceCPUDataPage fPage;

    private long fCurrentOffset;

    // Event header information of the current event
    private long fCurrentTimeStamp;
    private int fCurrentTypeLen;
    private long fCurrentTimeDelta;

    // Info to retrieve the new event that has a payload
    private @Nullable BinaryFTraceEventDefinition fEventDef;

    // Helper to parse data
    private @NonNull BinaryFTraceIteratorHelper fDataParser;

    /**
     * Initialize an iterator to loop through the events of a specific CPU data
     * page
     *
     * @param page
     *            The CPU data page to read
     * @param fileHeader
     *            The FTrace header information object
     * @throws IOException
     *             When there is an error without opening/reading from the file
     */
    public BinaryFTraceCPUPageIterator(@NonNull BinaryFTraceCPUDataPage page, @NonNull BinaryFTraceHeaderInfo fileHeader) throws IOException {
        // Initialize buffer data
        fFileHeader = fileHeader;
        this.fPage = page;
        fLimit = page.getPageStartingOffset() + page.getSize();
        fDataParser = new BinaryFTraceIteratorHelper(fileHeader);

        // Initialize various values
        initializeIterator();
    }

    /**
     * --------------------------------------------------------
     *
     * Getters and Setters
     *
     * ---------------------------------------------------------
     */
    /**
     * Get the time stamp of the most recent event found
     *
     * @return The time stamp of the most recent event found
     */
    public long getCurrentTimeStamp() {
        return fCurrentTimeStamp;
    }

    /**
     * --------------------------------------------------------
     *
     * Closeable interface
     *
     * ---------------------------------------------------------
     */
    @Override
    public void close() throws IOException {
        BinaryFTraceByteBuffer buffer = fBuffer;
        if (buffer != null) {
            buffer.close();
            fBuffer = null;
        }
    }

    /**
     * --------------------------------------------------------
     *
     * BinaryFTraceEventIterator
     *
     * ---------------------------------------------------------
     */
    /**
     * Read the next event header (type length and time stamp), saves important
     * information and increment the file pointer.
     *
     * @return A BinaryFTraceResponse indicating whether the operation is
     *         successful or not.
     */
    public BinaryFTraceResponse readNextEvent() {
        try {
            // We reset the event definition everytime we try to read a new
            // event
            fEventDef = null;

            readNextEventTimeStamp();

            /*
             * If there is an event read, we return OK.
             */
            if (fEventDef != null) {
                return BinaryFTraceResponse.OK;
            }
        } catch (Exception ex) {
            Activator.getInstance().logError("There is an error reading the next binary FTrace event."); //$NON-NLS-1$
            return BinaryFTraceResponse.ERROR;
        }

        /*
         * Returns finish if no more event is read
         */
        return BinaryFTraceResponse.FINISH;
    }

    /**
     * Read the time stamp of the next event only (without reading the actual
     * event)
     *
     * @throws IOException
     */
    private void readNextEventTimeStamp() throws IOException {
        boolean readSuccess = readNextEventHeader();

        /**
         * Case 1: We handle absolute time stamps, which comes before the
         * events. Case 2: For each CPU page, the first event is a time extended
         * event with value 0. We read until we find an event with actual
         * payload.
         **/
        while (readSuccess && (fCurrentTypeLen == fFileHeader.getHeaderEventInfo().getTimeStampTypeLen() ||
                fCurrentTypeLen == fFileHeader.getHeaderEventInfo().getTimeExtendedTypeLen())) {
            // We found an absolute time stamp so we need to update the current
            // time stamp
            updateTimeStamp();

            // Read the next event
            readSuccess = readNextEventHeader();
        }

        if (readSuccess) {
            // Now we have a data event, we store the event type length, we save
            // the event type length and delta time
            // to load the event lazily later
            fEventDef = new BinaryFTraceEventDefinition(fCurrentOffset, getCurrentEventPayloadSize());

            // Move the pointer to the next event
            skip(fEventDef.getPayloadSize());

            // We peek the next event, to see if the event is an time extended
            // event
            int nextEventType = peekNextEventType();
            if (nextEventType == fFileHeader.getHeaderEventInfo().getTimeExtendedTypeLen()) {
                readNextEventHeader(); // No need to check for success because
                                       // the peek would have given -1 if failed
            }

            updateTimeStamp(); // Again we update the time stamp in case we have
                               // an extended timestamp event
        }
    }

    /**
     * Simply read the next event header, store the information read and
     * increment the offset
     *
     * @return true if the event is read sucessfully
     * @throws IOException
     */
    private boolean readNextEventHeader() throws IOException {
        if (!hasNext()) {
            return false;
        }

        BinaryFTraceByteBuffer buffer = fBuffer;
        if (buffer != null) {
            buffer.movePointerToOffset(fCurrentOffset);
            int eventTypeLengthAndTimeStamp = buffer.getNextInt();
            fCurrentTypeLen = fDataParser.getEventTypeLength(eventTypeLengthAndTimeStamp);
            fCurrentTimeDelta = fDataParser.getDeltaTime(eventTypeLengthAndTimeStamp);
            fCurrentOffset += 4;

            return true;
        }

        return false;
    }

    /**
     * Simply read the next event type and return the next event type. This
     * method does not increment the file pointer.
     *
     * @return -1 if the there is no next event, else return the event type
     *         length.
     * @throws IOException
     *             if there is an error reading the next event, likely because
     *             of buffer overflow caused a logical error during reading.
     */
    private int peekNextEventType() throws IOException {
        if (!hasNext()) {
            return -1;
        }

        BinaryFTraceByteBuffer buffer = fBuffer;
        if (buffer != null) {
            buffer.movePointerToOffset(fCurrentOffset);
            int nextEventTypeLen = buffer.getNextInt();
            return fDataParser.getEventTypeLength(nextEventTypeLen);
        }

        return -1;
    }

    /**
     * Update the time stamp based on the state of the current event header and
     * increment the offset if needed
     *
     * @throws IOException
     *             IOException if there is an error reading the next event,
     *             likely because of buffer overflow caused a logical error
     *             during reading.
     */
    private void updateTimeStamp() throws IOException {
        if (fCurrentTypeLen <= fFileHeader.getHeaderEventInfo().getDataMaxTypeLen()) {
            fCurrentTimeStamp += fCurrentTimeDelta;
        } else {
            BinaryFTraceByteBuffer buffer = fBuffer;
            if (buffer != null) {
                long extend = buffer.getNextInt();
                extend = extend <<= 27;
                extend += fCurrentTimeDelta;

                if (fCurrentTypeLen == fFileHeader.getHeaderEventInfo().getTimeExtendedTypeLen()) {
                    fCurrentTimeStamp += extend;
                } else if (fCurrentTypeLen == fFileHeader.getHeaderEventInfo().getTimeStampTypeLen()) {
                    fCurrentTimeStamp = extend;
                }

                this.fCurrentOffset += 4;
            }
        }
    }

    private void skip(int bytesToSkip) {
        this.fCurrentOffset += bytesToSkip;
    }

    /**
     * Get the pay load size of the event according to its type length
     *
     * @return The size of the payload of the event
     * @throws IOException
     *             if there is an error reading the next event, likely because
     *             of buffer overflow caused a logical error during reading.
     */
    private int getCurrentEventPayloadSize() throws IOException {

        int payloadSize = 0;
        if (fCurrentTypeLen == fFileHeader.getHeaderEventInfo().getCustomLengthEventTypeLen()) {
            // If the type length is 0, then the next 4 bytes contain the actual
            // length
            BinaryFTraceByteBuffer buffer = fBuffer;
            if (buffer != null) {
                payloadSize = buffer.getNextInt();
                this.fCurrentOffset += 4;
            }
        } else if (fCurrentTypeLen <= fFileHeader.getHeaderEventInfo().getDataMaxTypeLen()) {
            payloadSize = fCurrentTypeLen * 4;
        }

        return payloadSize;
    }

    /**
     * Get the page associated with this iterator. Most common use is to obtain
     * the iterator of the next page.
     *
     * @return The BinaryFTraceCPUDataPage associated with this iterator.
     */
    public BinaryFTraceCPUDataPage getPage() {
        return fPage;
    }

    /**
     * Lazily read the next event using the event definition information.
     *
     * @return A BinaryFTraceEvent at the current event definition.
     * @throws IOException
     *             if there is an error reading the next event, likely because
     *             of buffer overflow caused a logical error during reading.
     */
    public @Nullable BinaryFTraceEvent getCurrentEvent() throws IOException {
        // Make a local copy to prevent multithreading null check
        BinaryFTraceEventDefinition eventDef = fEventDef;

        if (eventDef != null) {
            // We use a new buffer to read the current event
            try (BinaryFTraceByteBuffer tempBuffer = new BinaryFTraceByteBuffer(fFileHeader.getFilePath());) {
                tempBuffer.setByteOrder(fFileHeader.getEndianess());
                tempBuffer.movePointerToOffset(eventDef.getPayloadOffset());

                byte[] data = tempBuffer.getNextBytes(eventDef.getPayloadSize());
                BinaryFTraceEventFormat eventFormat = fDataParser.getEventFormat(data);
                if (eventFormat == null) {
                    return null;
                }

                Map<String, Object> properties;
                if (eventDef.getPayloadSize() > 0) {
                    properties = fDataParser.parseEventData(eventFormat, data);
                } else {
                    properties = new HashMap<>();
                }

                BinaryFTraceEvent event = new BinaryFTraceEvent(fCurrentTimeStamp,
                        properties,
                        eventFormat.getEventName(),
                        fPage.getCpu());
                return event;
            }
        }

        return null;
    }

    /**
     * Seek an event with the specified time stamp
     *
     * @param timeStampToSeekTo
     *            The timestamp to seek to
     * @return true if the seek was successful
     * @throws IOException
     *             if there is an error reading the next event, likely because
     *             that the iterator has reached the end.
     */
    public boolean seek(long timeStampToSeekTo) throws IOException {
        // Reset the iterator
        initializeIterator();

        // Read the first event
        BinaryFTraceResponse response = readNextEvent();

        while (response == BinaryFTraceResponse.OK &&
                timeStampToSeekTo > fCurrentTimeStamp &&
                hasNext()) {
            response = readNextEvent();
        }

        /*
         * We reached the end of the page, but the ts to seek is still larger
         * than the last event timestamp
         */
        if (timeStampToSeekTo > fCurrentTimeStamp) {
            fEventDef = null; // Set the eventDef to null to indicate no events
            return false;
        }

        return true;
    }

    /**
     * Check if there are more stuff to read in this iterator
     *
     * @return true if there are more events to read in this page
     *
     * @throws IOException
     *             if there is an error reading the next event, likely because
     *             that the iterator has reached the end.
     */
    public boolean hasNext() throws IOException {
        boolean ret = true;

        BinaryFTraceByteBuffer buffer = fBuffer;

        if (buffer == null) {
            ret = false;
        }

        /*
         * Next we check if reading the next event will go over the limit We
         * check reading the header first
         */
        else {
            if (fCurrentOffset + 4 >= fLimit) {
                ret = false;
            } else {
                // We check the header type length and timestamp
                int typeLengthAndTimeStamp = buffer.getNextInt();

                /*
                 * If the type length and timestamp is 0, it might be a custom
                 * length event, followed by an extended timestamp so we need to
                 * check for the custom length too
                 */
                if (typeLengthAndTimeStamp == 0) {
                    // The type length is actually just paddings
                    if (fCurrentOffset + 8 >= fLimit) {
                        ret = false;
                    } else {
                        // No custom length, guaranteed to be paddings
                        int customLength = buffer.getNextInt();
                        if (customLength == 0) {
                            ret = false;
                        }
                    }
                }
            }

            // Reset the buffer to avoid issues with other methods
            buffer.movePointerToOffset(fCurrentOffset);
        }

        return ret;
    }

    private void initializeIterator() throws IOException {
        fCurrentOffset = fPage.getDataStartingOffset();
        fCurrentTimeStamp = fPage.getTimeStamp();
        fCurrentTypeLen = -1;
        fCurrentTimeDelta = -1;
        fEventDef = null;

        // Reset the iterator
        if (fBuffer != null) {
            fBuffer.movePointerToOffset(fPage.getDataStartingOffset());
        } else {
            // Prepare the buffer
            BinaryFTraceByteBuffer newBuffer = new BinaryFTraceByteBuffer(fFileHeader.getFilePath());
            newBuffer.setByteOrder(fFileHeader.getEndianess());
            newBuffer.movePointerToOffset(fPage.getDataStartingOffset());

            fBuffer = newBuffer;
        }
    }
}
