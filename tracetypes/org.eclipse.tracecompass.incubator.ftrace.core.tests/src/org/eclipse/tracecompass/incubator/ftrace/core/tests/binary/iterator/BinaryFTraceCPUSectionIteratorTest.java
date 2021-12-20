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

package org.eclipse.tracecompass.incubator.ftrace.core.tests.binary.iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.parser.BinaryFTraceFileParser;
import org.eclipse.tracecompass.incubator.ftrace.core.tests.shared.FTraceUtils;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceEvent;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceFileCPU;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceHeaderInfo;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.iterator.BinaryFTraceCPUSectionIterator;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.iterator.BinaryFTraceResponse;
import org.eclipse.tracecompass.testtraces.ftrace.FtraceTestTrace;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This class is a wrapper for multiple BinaryFTraceCPUPageIterator under 1 CPU
 * section. It should has all the tests that are performed for each
 * BinaryFTraceCPUPageIterator. We test for the wrapper functionality that
 * coordinates multiple BinaryFTraceCPUPageIterator(s).
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceCPUSectionIteratorTest {
    private static BinaryFTraceHeaderInfo multipleEventTrace; // trace with 4
                                                              // CPUs
    private static BinaryFTraceFileCPU cpu;

    /**
     * Initializing the tests
     *
     * @throws Exception
     *             if an error occurred while parsing the trace header
     */
    @BeforeClass
    public static void init() throws Exception {
        multipleEventTrace = BinaryFTraceFileParser.parse(FTraceUtils.getTraceAbsolutePath(FtraceTestTrace.TEST_2_6_4_CPU));
        cpu = multipleEventTrace.getCpus().get(BinaryFTraceCPUSectionIteratorTestData.CPU_NUMBER);
    }

    /**
     * Test to make sure that the response code returned by the iterator is
     * correct
     *
     * @throws Exception
     *             if an error occurred while iterating through the CPU pages
     */
    @Test
    public void testReadNextEvent() throws Exception {
        try (BinaryFTraceCPUSectionIterator iterator = new BinaryFTraceCPUSectionIterator(cpu, multipleEventTrace)) {
            BinaryFTraceResponse response;

            iterator.seek(BinaryFTraceCPUSectionIteratorTestData.DEFAULT_TS);
            response = iterator.readNextEvent(); // Should return OK
            assertEquals(response, BinaryFTraceResponse.OK);
            assertNotNull(iterator.getCurrentEvent());

            // Move the pointer to the next to last event
            iterator.seek(BinaryFTraceCPUSectionIteratorTestData.NEXT_TO_LAST_TS);

            // just before the end
            response = iterator.readNextEvent(); // Should return FINISH
            assertEquals(response, BinaryFTraceResponse.OK);
            assertNotNull(iterator.getCurrentEvent());

            response = iterator.readNextEvent(); // Try to read an event after
                                                 // the last event, should
                                                 // return ERROR
            assertEquals(response, BinaryFTraceResponse.FINISH);
            assertNull(iterator.getCurrentEvent());
        }
    }

    /**
     * Seek for the first event
     *
     * @throws Exception
     *             if an error occurred while iterating through the CPU pages
     */
    @Test
    public void testSeekEventAtTimeStamp0() throws Exception {
        try (BinaryFTraceCPUSectionIterator iterator = new BinaryFTraceCPUSectionIterator(cpu, multipleEventTrace)) {
            iterator.seek(BinaryFTraceCPUSectionIteratorTestData.DEFAULT_TS);
            BinaryFTraceEvent event = iterator.getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimeSinceBoot(), BinaryFTraceCPUSectionIteratorTestData.PAGE0_FIRST_EVENT_TS);
            assertEquals(event.getFields().get("name"), BinaryFTraceCPUSectionIteratorTestData.PAGE0_FIRST_EVENT_NAME);
        }
    }

    /**
     * Seek for the first event of the section
     *
     * @throws Exception
     *             if an error occurred while iterating through the CPU pages
     */
    @Test
    public void testSeekEventAtSpecificTimeStampAtStartOfPage() throws Exception {
        try (BinaryFTraceCPUSectionIterator iterator = new BinaryFTraceCPUSectionIterator(cpu, multipleEventTrace)) {
            // seek at the start of page
            iterator.seek(BinaryFTraceCPUSectionIteratorTestData.PAGE0_FIRST_EVENT_TS);
            BinaryFTraceEvent event = iterator.getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimeSinceBoot(), BinaryFTraceCPUSectionIteratorTestData.PAGE0_FIRST_EVENT_TS);
            assertEquals(event.getFields().get("name"), BinaryFTraceCPUSectionIteratorTestData.PAGE0_FIRST_EVENT_NAME);
        }
    }

    /**
     * Seek for an event that is at the middle of the section with unspecific
     * timestamp.
     *
     * @throws Exception
     *             if an error occurred while iterating through the CPU pages
     */
    @Test
    public void testSeekEventAtImpreciseTimeStampAtMidPage() throws Exception {
        try (BinaryFTraceCPUSectionIterator iterator = new BinaryFTraceCPUSectionIterator(cpu, multipleEventTrace)) {
            // seek in the middle with imprecise timestamp
            iterator.seek(BinaryFTraceCPUSectionIteratorTestData.MID_SECTION_IMPRECISE_TS_CPU2);
            BinaryFTraceEvent event = iterator.getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimeSinceBoot(), BinaryFTraceCPUSectionIteratorTestData.MID_SECTION_PRECISE_TS_CPU2);
            assertEquals(event.getFields().get("name"), BinaryFTraceCPUSectionIteratorTestData.MID_SECTION_EVENT_NAME);
        }
    }

    /**
     * Seek for an event that is at the middle of a section with specific
     * timestamp.
     *
     * @throws Exception
     *             if an error occurred while iterating through the CPU pages
     */
    @Test
    public void testSeekEventAtPreciseTimeStampAtMidPage() throws Exception {
        try (BinaryFTraceCPUSectionIterator iterator = new BinaryFTraceCPUSectionIterator(cpu, multipleEventTrace)) {
            // seek in the middle with imprecise timestamp
            iterator.seek(BinaryFTraceCPUSectionIteratorTestData.MID_SECTION_PRECISE_TS_CPU2);
            BinaryFTraceEvent event = iterator.getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimeSinceBoot(), BinaryFTraceCPUSectionIteratorTestData.MID_SECTION_PRECISE_TS_CPU2);
            assertEquals(event.getFields().get("name"), BinaryFTraceCPUSectionIteratorTestData.MID_SECTION_EVENT_NAME);
        }
    }

    /**
     * Test 1: Seek for an event with the ts larger than the time stamp of the
     * last event of the last page, but less then the start of the next page.
     * The expected result should be the first event of the next page.
     *
     * Test 2: Seek for an event with the ts larger than the time stamp of the
     * last event of the second page (CPU 2 has 2 pages only. We expect the
     * result is null.
     *
     * @throws Exception
     *             if an error occurred while iterating through the CPU pages
     */
    @Test
    public void testSeekEndOfPageEvent() throws Exception {
        boolean ret = false;
        BinaryFTraceEvent event = null;

        try (BinaryFTraceCPUSectionIterator iterator = new BinaryFTraceCPUSectionIterator(cpu, multipleEventTrace)) {
            /**
             * An event at the end of the page
             */
            ret = iterator.seek(BinaryFTraceCPUSectionIteratorTestData.PAGE0_LAST_EVENT_TS);
            assertTrue(ret);
            event = iterator.getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimeSinceBoot(), BinaryFTraceCPUSectionIteratorTestData.PAGE0_LAST_EVENT_TS);
            assertEquals(event.getFields().get("name"), BinaryFTraceCPUSectionIteratorTestData.PAGE0_LAST_EVENT_NAME);

            /**
             * Seek for an event with ts larger than the last event of the first
             * page, but smaller than the first event of the next page.
             */
            ret = iterator.seek(BinaryFTraceCPUSectionIteratorTestData.PAGE1_TS - 1);
            assertTrue(ret);
            event = iterator.getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimeSinceBoot(), BinaryFTraceCPUSectionIteratorTestData.PAGE1_TS);
            assertEquals(event.getFields().get("name"), BinaryFTraceCPUSectionIteratorTestData.PAGE1_FIRST_EVENT_TS);

            /**
             * Seek for an event with ts larger than all valid events
             */
            ret = iterator.seek(BinaryFTraceCPUSectionIteratorTestData.INVALID_TS);
            assertFalse(ret);
            event = iterator.getCurrentEvent();
            assertNull(event);
        }
    }

    /**
     * Seek for an event at the next page
     *
     * @throws Exception
     *             if an error occurred while iterating through the CPU pages
     */
    @Test
    public void testSeekEventAtNextPage() throws Exception {
        try (BinaryFTraceCPUSectionIterator iterator = new BinaryFTraceCPUSectionIterator(cpu, multipleEventTrace)) {
            // Seek in the middle of the page with precise timestamp
            iterator.seek(BinaryFTraceCPUSectionIteratorTestData.MID_PAGE1_EVENT_TS);
            BinaryFTraceEvent event = iterator.getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimeSinceBoot(), BinaryFTraceCPUSectionIteratorTestData.MID_PAGE1_EVENT_TS);
            assertEquals(event.getFields().get("name"), BinaryFTraceCPUSectionIteratorTestData.MID_PAGE1_EVENT_NAME);
        }
    }
}
