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

import static org.eclipse.tracecompass.incubator.ftrace.core.tests.binary.header.BinaryFTraceHeaderTestData.CPU2_INDEX;
import static org.eclipse.tracecompass.incubator.ftrace.core.tests.binary.header.BinaryFTraceHeaderTestData.CPU2_NUM_OF_PAGES;
import static org.eclipse.tracecompass.incubator.ftrace.core.tests.binary.header.BinaryFTraceHeaderTestData.FIRST_PAGE_DATA_OFFSET;
import static org.eclipse.tracecompass.incubator.ftrace.core.tests.binary.header.BinaryFTraceHeaderTestData.FIRST_PAGE_FLAG;
import static org.eclipse.tracecompass.incubator.ftrace.core.tests.binary.header.BinaryFTraceHeaderTestData.FIRST_PAGE_STARTING_OFFSET;
import static org.eclipse.tracecompass.incubator.ftrace.core.tests.binary.header.BinaryFTraceHeaderTestData.FIRST_PAGE_TIME_STAMP;
import static org.eclipse.tracecompass.incubator.ftrace.core.tests.binary.header.BinaryFTraceHeaderTestData.PAGE_SIZE;
import static org.eclipse.tracecompass.incubator.ftrace.core.tests.binary.header.BinaryFTraceHeaderTestData.SECOND_PAGE_DATA_OFFSET;
import static org.eclipse.tracecompass.incubator.ftrace.core.tests.binary.header.BinaryFTraceHeaderTestData.SECOND_PAGE_FLAG;
import static org.eclipse.tracecompass.incubator.ftrace.core.tests.binary.header.BinaryFTraceHeaderTestData.SECOND_PAGE_STARTING_OFFSET;
import static org.eclipse.tracecompass.incubator.ftrace.core.tests.binary.header.BinaryFTraceHeaderTestData.SECOND_PAGE_TIME_STAMP;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.eclipse.tracecompass.incubator.ftrace.core.tests.shared.FTraceUtils;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceCPUDataPage;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFileCPU;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFileType;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceHeaderInfo;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceVersion;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceVersionHeader;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.parser.BinaryFTraceFileParser;
import org.eclipse.tracecompass.testtraces.ftrace.FtraceTestTrace;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A test for {@link BinaryFTraceHeaderInfo} and {@link BinaryFTraceFileParser}
 *
 * @author Hoang Thuan Pham
 *
 */
public class BinaryFTraceHeaderTest {
    /**
     * The file is obtained by running trace-cmd for the ls command: trace-cmd
     * record -e all ls.
     */
    private static String traceURL;
    private static final byte[] TRACE_CMD_V6_DAT_MAGIC = { 0x17, 0x08, 0x44, 't', 'r', 'a', 'c', 'i', 'n', 'g' };
    private static final int[] SUPPORTED_FTRACE_VERSION = { 6 };

    // Might change in the future with ftrace v7.
    private static final int TYPE_LEN_SIZE = 5;
    private static final int DELTA_TIME_SIZE = 27;
    private static final int PADDING_EVENT_TYPE_LEN = 29;
    private static final int EXTENDED_TS_TYPE_LEN = 30;
    private static final int ABSOLUTE_TS_TYPE_LEN = 31;
    private static final int CUSTOM_TYPE_LEN = 0;
    private static final int MAX_EVENT_TYPE_LEN = 28;

    /**
     * Initialize data for the tests
     *
     * @throws IOException
     *             if there is an issue getting the URL of the trace
     */
    @BeforeClass
    public static void testInit() throws IOException {
        traceURL = FTraceUtils.getTraceAbsolutePath(FtraceTestTrace.TEST_2_6_MULTIPLE_CPUS);
    }

    /**
     * Test to make sure the file is a supported ftrace format
     *
     * @throws Exception
     *             if there is an issue getting the trace version header
     */
    @Test
    public void testMagicValues() throws Exception {
        BinaryFTraceVersionHeader magicValues = BinaryFTraceFileParser.getFtraceVersionHeader(traceURL);
        assertArrayEquals(magicValues.getMagicValues(), TRACE_CMD_V6_DAT_MAGIC);
        assertTrue(magicValues.getFTraceVersion() == BinaryFTraceVersion.V6);
    }

    /**
     * Test to make sure event headers information are parsed correctly This
     * information is important to parse the events after wards
     *
     * @throws Exception
     *             if there is an error while parsing the trace
     */
    @Test
    public void testEventHeaderFormat() throws Exception {
        BinaryFTraceHeaderInfo test = BinaryFTraceFileParser.parse(traceURL);
        assertEquals(test.getHeaderEventInfo().getTypeLen(), TYPE_LEN_SIZE);
        assertEquals(test.getHeaderEventInfo().getTimeDelta(), DELTA_TIME_SIZE);
        assertEquals(test.getHeaderEventInfo().getCustomLengthEventTypeLen(), CUSTOM_TYPE_LEN);
        assertEquals(test.getHeaderEventInfo().getDataMaxTypeLen(), MAX_EVENT_TYPE_LEN);
        assertEquals(test.getHeaderEventInfo().getPaddingTypeLen(), PADDING_EVENT_TYPE_LEN);
        assertEquals(test.getHeaderEventInfo().getTimeExtendedTypeLen(), EXTENDED_TS_TYPE_LEN);
        assertEquals(test.getHeaderEventInfo().getTimeStampTypeLen(), ABSOLUTE_TS_TYPE_LEN);
    }

    /**
     * Test to make sure the trace type is set correctly
     *
     * @throws Exception
     *             if there is an error while parsing the trace
     */
    @Test
    public void testTraceType() throws Exception {
        BinaryFTraceHeaderInfo test = BinaryFTraceFileParser.parse(traceURL);
        assertEquals(test.getFileType(), BinaryFTraceFileType.FLY_RECORD);
    }

    /**
     * Test for CPU Section paging. Each CPU section data is paged into multiple
     * pages with the same size. This test checks if the pages are parsed
     * correcly.
     *
     * @throws TmfTraceException
     *             if an error occurred while parsing the trace header
     */
    @Test
    public void testCpuSectionPaging() throws TmfTraceException {
        BinaryFTraceHeaderInfo traceHeader = BinaryFTraceFileParser.parse(traceURL);
        BinaryFTraceFileCPU cpuSection = traceHeader.getCpus().get(CPU2_INDEX);
        assertNotNull(cpuSection);

        assertEquals(cpuSection.getPages().size(), CPU2_NUM_OF_PAGES);

        // Get the first page in the list of pages
        BinaryFTraceCPUDataPage firstPage = cpuSection.getPages().get(0);
        assertEquals(firstPage.getPageStartingOffset(), FIRST_PAGE_STARTING_OFFSET);
        assertEquals(firstPage.getDataStartingOffset(), FIRST_PAGE_DATA_OFFSET);
        assertEquals(firstPage.getFlags(), FIRST_PAGE_FLAG);
        assertEquals(firstPage.getSize(), PAGE_SIZE);
        assertEquals(firstPage.getTimeStamp(), FIRST_PAGE_TIME_STAMP);

        BinaryFTraceCPUDataPage nextPage = firstPage.getNextPage();
        assertEquals(nextPage.getPageStartingOffset(), SECOND_PAGE_STARTING_OFFSET);
        assertEquals(nextPage.getDataStartingOffset(), SECOND_PAGE_DATA_OFFSET);
        assertEquals(nextPage.getFlags(), SECOND_PAGE_FLAG);
        assertEquals(nextPage.getSize(), PAGE_SIZE);
        assertEquals(nextPage.getTimeStamp(), SECOND_PAGE_TIME_STAMP);

        BinaryFTraceCPUDataPage nullPage = nextPage.getNextPage();
        assertNull(nullPage);
    }

    /**
     * Check if the version is a supported version
     *
     * @param ftraceVersion
     *            The version of the ftrace file
     * @return true if the version is supported
     */
    public boolean isSupportedFTraceVersion(int ftraceVersion) {
        for (int value : SUPPORTED_FTRACE_VERSION) {
            if (ftraceVersion == value) {
                return true;
            }
        }

        return false;
    }
}
