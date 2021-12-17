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

package org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header;

/**
 * Represents information of the FTrace header event page. This information is
 * necessary to parse the CPU data section.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceHeaderEvent {
    // 0, constant because this is
    // not obtained from the file
    private static final int CUSTOM_LENGTH_EVENT = 0;
    // The two following fields form a 32 bits field
    private final int fTypeLen;
    private final int fTimeDelta;

    // The maximum value of typeLen
    private final int fDataMaxTypeLen;

    // To identify non-data events
    private final int fPaddingTypeLen; // Should be 29
    private final int fTimeExtendedTypeLen; // Should be 30
    private final int fTimeStampTypeLen; // Should be 31

    /**
     * Constructor
     *
     * @param builder
     *            A {@link BinaryFTraceHeaderEventBuilder} containing event
     *            header information.
     */
    public BinaryFTraceHeaderEvent(BinaryFTraceHeaderEventBuilder builder) {
        fTypeLen = builder.fBuilderTypeLen;
        fTimeDelta = builder.fBuilderTimeDelta;
        fDataMaxTypeLen = builder.fBuilderDataMaxTypeLen;

        // To identify non-data events
        fPaddingTypeLen = builder.fBuilderPaddingTypeLen;
        fTimeExtendedTypeLen = builder.fBuilderTimeExtendedTypeLen;
        fTimeStampTypeLen = builder.fBuilderTimeStampTypeLen;
    }

    /**
     * Get the number of bits (in a u32) that represent the size of an event
     *
     * @return the number of bits used for the size of an event
     */
    public int getTypeLen() {
        return fTypeLen;
    }

    /**
     * Get the number of bits (in a u32) that are used as a time stamp
     *
     * @return the number of bits used for time stamp
     */
    public int getTimeDelta() {
        return fTimeDelta;
    }

    /**
     * Get the maximum value for the type length for normal events
     *
     * @return The maximum value for the type length
     */
    public int getDataMaxTypeLen() {
        return fDataMaxTypeLen;
    }

    /**
     * Get the type length value for the padding event
     *
     * @return The type length value for the padding event
     */
    public int getPaddingTypeLen() {
        return fPaddingTypeLen;
    }

    /**
     * Get the type length value for the time extended event
     *
     * @return The type length value for the time extended event
     */
    public int getTimeExtendedTypeLen() {
        return fTimeExtendedTypeLen;
    }

    /**
     * Get the type length value for the time stamp event
     *
     * @return The type length value for the time stamp event
     */
    public int getTimeStampTypeLen() {
        return fTimeStampTypeLen;
    }

    /**
     * Get the typeLen value for events that has a length larger than
     * 4*dataMaxTypeLen bytes.
     *
     * @return The typeLen value for custom length events
     */
    public int getCustomLengthEventTypeLen() {
        return CUSTOM_LENGTH_EVENT;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("Number of bits for event type: ").append(fTypeLen).append('\n'); //$NON-NLS-1$
        strBuilder.append("Number of bits for time delta: ").append(fTimeDelta).append('\n'); //$NON-NLS-1$
        strBuilder.append("Maximum value of data event type: ").append(fDataMaxTypeLen).append('\n'); //$NON-NLS-1$
        strBuilder.append("If the event is a padding event, then the type value will be: ").append(fPaddingTypeLen).append('\n'); //$NON-NLS-1$
        strBuilder.append("If the event is a time extended event, then the type value will be: ").append(fTimeExtendedTypeLen).append('\n'); //$NON-NLS-1$
        strBuilder.append("If the event is a time stamp event, then the type value will be: ").append(fTimeStampTypeLen).append('\n'); //$NON-NLS-1$

        return strBuilder.toString();
    }

    /**
     * A builder to create immutable {@link BinaryFTraceHeaderEvent} objects.
     *
     * @author Hoang Thuan Pham
     */
    public static class BinaryFTraceHeaderEventBuilder {
        // The two following fields form a 32 bits field
        private int fBuilderTypeLen;
        private int fBuilderTimeDelta;

        // The maximum value of typeLen
        private int fBuilderDataMaxTypeLen;

        // To identify non-data events
        private int fBuilderPaddingTypeLen; // Should be 29
        private int fBuilderTimeExtendedTypeLen; // Should be 30
        private int fBuilderTimeStampTypeLen; // Should be 31

        /**
         * Constructor
         */
        public BinaryFTraceHeaderEventBuilder() {
            fBuilderTypeLen = -1;
            fBuilderTimeDelta = -1;
            fBuilderDataMaxTypeLen = -1;

            // To identify non-data events
            fBuilderPaddingTypeLen = -1;
            fBuilderTimeExtendedTypeLen = -1;
            fBuilderTimeStampTypeLen = -1;
        }

        /**
         * Set the number of bits reserved for the type length value of each
         * event in the event header.
         *
         * @param builderTypeLen
         *            The number of bits reserved for the type length value
         * @return The current {@link BinaryFTraceHeaderEventBuilder} builder
         */
        public BinaryFTraceHeaderEventBuilder typeLen(int builderTypeLen) {
            fBuilderTypeLen = builderTypeLen;
            return this;
        }

        /**
         * Set the number of bits reserved for the time delta value of each
         * event in the event header.
         *
         * @param builderTimeDelta
         *            The number of bits reserved for the time delta value
         * @return The current {@link BinaryFTraceHeaderEventBuilder} builder
         */
        public BinaryFTraceHeaderEventBuilder timeDelta(int builderTimeDelta) {
            fBuilderTimeDelta = builderTimeDelta;
            return this;
        }

        /**
         * Set the maximum value of the type length for data events (event with
         * payload).
         *
         * @param builderDataMaxTypeLen
         *            The maximum value of the type length for data events
         * @return The current {@link BinaryFTraceHeaderEventBuilder} builder
         */
        public BinaryFTraceHeaderEventBuilder dataMaxTypeLen(int builderDataMaxTypeLen) {
            fBuilderDataMaxTypeLen = builderDataMaxTypeLen;
            return this;
        }

        /**
         * Set the type length value of padding event.
         *
         * @param builderPaddingTypeLen
         *            The type length value of padding event.
         * @return The current {@link BinaryFTraceHeaderEventBuilder} builder
         */
        public BinaryFTraceHeaderEventBuilder paddingTypeLen(int builderPaddingTypeLen) {
            fBuilderPaddingTypeLen = builderPaddingTypeLen;
            return this;
        }

        /**
         * Set the type length value for extended time event.
         *
         * @param builderTimeExtendedTypeLen
         *            The type length value for extended time event
         * @return The current {@link BinaryFTraceHeaderEventBuilder} builder
         */
        public BinaryFTraceHeaderEventBuilder timeExtendedTypeLen(int builderTimeExtendedTypeLen) {
            fBuilderTimeExtendedTypeLen = builderTimeExtendedTypeLen;
            return this;
        }

        /**
         * Set the type length value for absolute time stamp event.
         *
         * @param builderTimeStampTypeLen
         *            The type length value for absolute time stamp event.
         * @return The current {@link BinaryFTraceHeaderEventBuilder} builder
         */
        public BinaryFTraceHeaderEventBuilder timeStampTypeLen(int builderTimeStampTypeLen) {
            fBuilderTimeStampTypeLen = builderTimeStampTypeLen;
            return this;
        }

        /**
         * Create an immutable {@link BinaryFTraceHeaderEvent} using the current
         * state of the builder.
         *
         * @return A {@link BinaryFTraceHeaderEvent} object
         */
        public BinaryFTraceHeaderEvent build() {
            return new BinaryFTraceHeaderEvent(this);
        }
    }
}
