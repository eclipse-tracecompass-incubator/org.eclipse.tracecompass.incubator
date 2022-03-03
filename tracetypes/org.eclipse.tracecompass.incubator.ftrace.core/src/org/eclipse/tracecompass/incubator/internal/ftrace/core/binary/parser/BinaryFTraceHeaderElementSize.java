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

package org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.parser;

import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceConstants;

/**
 * This file contains the number of bytes reserved for different sections in the
 * header of a binary ftrace file.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceHeaderElementSize {

    private BinaryFTraceHeaderElementSize() {
        // Do nothing
    }

    /**
     * The number of bytes used for a section with a large size.
     */
    public static final int LARGE_SECTION_SIZE = 8;

    /**
     * The number of bytes used for a section with a small size.
     */
    public static final int SMALL_SECTION_SIZE = 4;

    /**
     * Each string in the header is terminated by /0. The size of the
     * termination character is 1 byte
     */
    public static final int STRING_TERMINATOR = 1;

    /**
     * The magic value at the start of the trace
     */
    public static final int MAGIC_VALUE = 3;

    /**
     * The string tracing that immediately follow the magic values
     */
    public static final int TRACING_STRING = 7;

    /**
     * The number of reserved bytes for the FTrace Version
     */
    public static final int FTRACE_VERSION = 1;

    /**
     * The size of the page header of a binary FTrace CPU data page in bytes
     */
    public static final int PAGE_HEADER_SIZE = 16;

    /**
     * The number of bytes reserved for the endianess of the file. 0 = little
     * endian, 1 = big endian
     */
    public static final int ENDIANESS_SIZE = 1;

    /**
     * The number of bytes reserved for the long value size
     */
    public static final int LONG_VALUE_SIZE = 1;

    /**
     * The number of bytes reserved for the host machine page size
     */
    public static final int PAGE_SIZE = 4;

    /**
     * Get the size of the magic value section in bytes.
     *
     * @return The size of the magic value section in bytes
     */
    public static int getMagicValueSectionSize() {
        /**
         * Make sure that the order of the sum follows the order of each value
         * in the trace
         */
        return MAGIC_VALUE +
                TRACING_STRING +
                FTRACE_VERSION +
                STRING_TERMINATOR;
    }

    /**
     * Get the size of the header in bytes.
     *
     * @return The size of the header in bytes
     */
    public static int getHeaderSize() {
        /**
         * Make sure that the order of the sum follows the order of each value
         * in the trace to make the code easy to follow
         */
        return MAGIC_VALUE +
                TRACING_STRING +
                FTRACE_VERSION +
                STRING_TERMINATOR +
                ENDIANESS_SIZE +
                LONG_VALUE_SIZE +
                PAGE_SIZE;
    }

    /**
     * Number of bytes reserved for the number of events
     */
    public static final int EVENT_COUNT = 4;

    /**
     * Number of bytes reserved for the number of event systems
     */
    public static final int EVENT_SYSTEM_COUNT = 4;

    /**
     * Number of bytes reserved for the size of an event
     */
    public static final int EVENT_SIZE = 8;

    /**
     * The number of bytes reserved for the number of CPUs
     */
    public static final int NUMBER_OF_CPUS = 4;

    /**
     * The number of bytes reserved for the header of each subsection
     */
    public static final int SUBSECTION_HEADER = 4;

    /**
     * The number of bytes reserved for the size of the option section
     */
    public static final int OPTION_TYPE = 2;

    /**
     * The number of bytes reserved for the option size
     */
    public static final int OPTION_SIZE = 4;

    /**
     * The number of bytes reserved for the starting offset of each CPU section
     */
    public static final int CPU_SECTION_OFFSET = 8;

    /**
     * The number of bytes reserved for the size of each CPU section
     */
    public static final int CPU_SECTION_SIZE = 8;

    /**
     * The header for the Options/Fly Record/Latency section. All should be the
     * same size
     */
    public static final int TRACE_DATA_HEADER_SIZE = 10;

    /**
     * Get the size (in bytes) of the title of the Header Page section.
     *
     * @return The size (in bytes) of the title of the Header Page section.
     */
    public static int getHeaderPageSectionTitleSize() {
        /**
         * Make sure that the order of the sum follows the order of each value
         * in the trace
         */
        return BinaryFTraceConstants.HEADER_PAGE_SECTION_HEADER.length() +
                STRING_TERMINATOR;
    }

    /**
     * Get the size (in bytes) of the header of the Header Page section.
     *
     * @return The size (in bytes) of the header of the Header Page section.
     */
    public static int getHeaderPageSectionHeaderSize() {
        /**
         * Make sure that the order of the sum follows the order of each value
         * in the trace
         */
        return getHeaderPageSectionTitleSize() +
                LARGE_SECTION_SIZE;
    }

    /**
     * Get the size (in bytes) of the title of the Header Page section.
     *
     * @return The size (in bytes) of the title of the Header Page section.
     */
    public static int getHeaderEventSectionTitleSize() {
        /**
         * Make sure that the order of the sum follows the order of each value
         * in the trace
         */
        return BinaryFTraceConstants.HEADER_EVENT_SECTION_HEADER.length() +
                STRING_TERMINATOR;
    }

    /**
     * Get the size (in bytes) of the header of the Header Event section.
     *
     * @return The size (in bytes) of the header of the Header Event section.
     */
    public static int getHeaderEventSectionHeaderSize() {
        /**
         * Make sure that the order of the sum follows the order of each value
         * in the trace
         */
        return getHeaderEventSectionTitleSize() +
                LARGE_SECTION_SIZE;
    }
}
