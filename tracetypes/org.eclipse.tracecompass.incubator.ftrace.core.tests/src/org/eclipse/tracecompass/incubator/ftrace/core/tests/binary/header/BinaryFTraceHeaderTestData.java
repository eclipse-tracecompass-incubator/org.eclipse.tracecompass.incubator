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
package org.eclipse.tracecompass.incubator.ftrace.core.tests.binary.header;

import org.eclipse.tracecompass.testtraces.ftrace.FtraceTestTrace;

/**
 * Test data for {@link FTraceBinaryHeaderTest}. This data is obtained from
 * {@link FtraceTestTrace#TEST_2_6_MULTIPLE_CPUS}.
 *
 * @author Hoang Thuan Pham
 */
final class BinaryFTraceHeaderTestData {

    private BinaryFTraceHeaderTestData() {
        // Do thing
    }

    /**
     * The page size (in bytes) for each trace data page
     */
    static final long PAGE_SIZE = 4096;

    // Data obtained from CPU number 2

    /**
     * The index number of CPU number 2 in the list of cpus in the trace file
     */
    static final int CPU2_INDEX = 2;

    /**
     * The number of data pages of CPU number 2
     */
    static final int CPU2_NUM_OF_PAGES = 2;

    /**
     * The starting offset (in bytes) of the first page
     */
    static final long FIRST_PAGE_STARTING_OFFSET = 8192000L;

    /**
     * The starting offset (in bytes) of the actual trace data of the first page
     */
    static final long FIRST_PAGE_DATA_OFFSET = 8192016L;

    /**
     * The flag value of the first page
     */
    static final long FIRST_PAGE_FLAG = 4048;

    /**
     * The timestamp (in nanoseconds since boot) of the first page
     */
    static final long FIRST_PAGE_TIME_STAMP = 93083942254L;

    /**
     * The starting offset (in bytes) of the second page
     */
    static final long SECOND_PAGE_STARTING_OFFSET = 8196096L;

    /**
     * The starting offset (in bytes) of the actual trace data of the second
     * page
     */
    static final long SECOND_PAGE_DATA_OFFSET = 8196112L;

    /**
     * The flag value of the second page
     */
    static final long SECOND_PAGE_FLAG = 3612;

    /**
     * The starting offset (in bytes) of the second page
     */
    static final long SECOND_PAGE_TIME_STAMP = 93084693295L;
}
