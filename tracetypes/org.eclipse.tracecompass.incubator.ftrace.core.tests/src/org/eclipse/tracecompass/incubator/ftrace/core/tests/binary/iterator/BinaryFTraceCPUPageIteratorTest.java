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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.eclipse.tracecompass.incubator.ftrace.core.tests.shared.FTraceUtils;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceEvent;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceCPUDataPage;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceHeaderInfo;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.iterator.BinaryFTraceCPUPageIterator;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.iterator.BinaryFTraceIteratorHelper;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.iterator.BinaryFTraceResponse;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.parser.BinaryFTraceFileParser;
import org.eclipse.tracecompass.testtraces.ftrace.FtraceTestTrace;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for {@link BinaryFTraceCPUPageIterator}
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceCPUPageIteratorTest {
    /**
     * Trace with 1 CPU and 1 single event (to isolate other factors).
     */
    private static BinaryFTraceHeaderInfo singleEventTrace;
    /**
     * Trace with on a virtual machine with 4 CPUs.
     */

    private static BinaryFTraceHeaderInfo multipleEventTrace;

    /**
     * Intialize the elements for the test
     *
     * @throws Exception
     *             if an error occurred while parsing a trace
     */
    @BeforeClass
    public static void init() throws Exception {
        singleEventTrace = BinaryFTraceFileParser.parse(FTraceUtils.getTraceAbsolutePath(FtraceTestTrace.TEST_2_6_SINGLE_EVENT));
        multipleEventTrace = BinaryFTraceFileParser.parse(FTraceUtils.getTraceAbsolutePath(FtraceTestTrace.TEST_2_6_MULTIPLE_CPUS));
    }

    /**
     * Parse a file with a single event
     *
     * @throws Exception
     *             if an error occurred while iterating through the trace events
     */
    @Test
    public void testParseEvent() throws Exception {
        BinaryFTraceCPUDataPage page = singleEventTrace.getCpus().get(0).getPages().get(0);

        try (BinaryFTraceCPUPageIterator iterator = BinaryFTraceIteratorHelper.getPageIterator(page, singleEventTrace)) {
            assertNotNull(iterator);

            BinaryFTraceResponse response = iterator.readNextEvent();

            assertEquals(response, BinaryFTraceResponse.OK);

            BinaryFTraceEvent event = iterator.getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimeSinceBoot(), BinaryFTraceCPUPageIteratorTestData.SINGLE_TRACE_EVENT_TS);
            assertEquals(event.getFields().get("name"), BinaryFTraceCPUPageIteratorTestData.SINGLE_TRACE_EVENT_NAME);
            assertEquals(event.getFields().get("count"), BinaryFTraceCPUPageIteratorTestData.SINGLE_TRACE_EVENT_COUNT);
            assertEquals(event.getFields().get("flags"), BinaryFTraceCPUPageIteratorTestData.SINGLE_TRACE_EVENT_FLAGS);
        }
    }

    /**
     * Test to make sure the page iterator return finish at the end of a page
     *
     * @throws Exception
     *             if an error occurred while iterating through the trace events
     */
    @Test
    public void testReadNextEvent() throws Exception {
        BinaryFTraceCPUDataPage page = multipleEventTrace.getCpus().get(0).getPages().get(0);
        try (BinaryFTraceCPUPageIterator iterator = BinaryFTraceIteratorHelper.getPageIterator(page, multipleEventTrace)) {
            assertNotNull(iterator);

            BinaryFTraceResponse response;

            iterator.seek(BinaryFTraceCPUPageIteratorTestData.DEFAULT_TS);
            response = iterator.readNextEvent(); // Should return OK
            assertEquals(response, BinaryFTraceResponse.OK);
            assertNotNull(iterator.getCurrentEvent());

            // Points the iterator to the last event
            iterator.seek(BinaryFTraceCPUPageIteratorTestData.LAST_EVENT_TS);
            response = iterator.readNextEvent(); // Should return FINISH
            assertEquals(response, BinaryFTraceResponse.FINISH);
            assertNull(iterator.getCurrentEvent());
        }
    }

    /**
     * Test to get a page
     *
     * @throws Exception
     *             if an error occurred while iterating through the trace events
     */
    @Test
    public void testGetPage() throws Exception {
        BinaryFTraceCPUDataPage page = singleEventTrace.getCpus().get(0).getPages().get(0);
        try (BinaryFTraceCPUPageIterator iterator = BinaryFTraceIteratorHelper.getPageIterator(page, multipleEventTrace)) {
            assertNotNull(iterator);

            iterator.getPage();
            assertEquals(page, page); // They should point to the same object
        }
    }

    /**
     * Seek for the first event of the page
     *
     * @throws Exception
     *             if an error occurred while iterating through the trace events
     */
    @Test
    public void testSeekEventAtTimeStamp0() throws Exception {
        BinaryFTraceCPUDataPage page = multipleEventTrace.getCpus().get(0).getPages().get(0);

        try (BinaryFTraceCPUPageIterator iterator = BinaryFTraceIteratorHelper.getPageIterator(page, multipleEventTrace)) {
            assertNotNull(iterator);

            iterator.seek(BinaryFTraceCPUPageIteratorTestData.DEFAULT_TS);
            BinaryFTraceEvent event = iterator.getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimeSinceBoot(), BinaryFTraceCPUPageIteratorTestData.FIRST_EVENT_TS);
            assertEquals(event.getFields().get("name"), BinaryFTraceCPUPageIteratorTestData.FIRST_EVENT_NAME);
        }
    }

    /**
     * Seek for an event the start of the page
     *
     * @throws Exception
     *             if an error occurred while iterating through the trace events
     */
    @Test
    public void testSeekEventAtSpecificTimeStampAtStartOfPage() throws Exception {
        BinaryFTraceCPUDataPage page = multipleEventTrace.getCpus().get(0).getPages().get(0);

        try (BinaryFTraceCPUPageIterator iterator = BinaryFTraceIteratorHelper.getPageIterator(page, multipleEventTrace)) {
            assertNotNull(iterator);

            // seek at the start of page
            iterator.seek(BinaryFTraceCPUPageIteratorTestData.FIRST_EVENT_TS);
            BinaryFTraceEvent event = iterator.getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimeSinceBoot(), BinaryFTraceCPUPageIteratorTestData.FIRST_EVENT_TS);
            assertEquals(event.getFields().get("name"), BinaryFTraceCPUPageIteratorTestData.FIRST_EVENT_NAME);
        }
    }

    /**
     * Seek for an event that is at the middle of the page with unspecific
     * timestamp.
     *
     * @throws Exception
     *             if an error occurred while iterating through the trace events
     */
    @Test
    public void testSeekEventAtImpreciseTimeStampAtMidPage() throws Exception {
        BinaryFTraceCPUDataPage page = multipleEventTrace.getCpus().get(0).getPages().get(0);

        try (BinaryFTraceCPUPageIterator iterator = BinaryFTraceIteratorHelper.getPageIterator(page, multipleEventTrace)) {
            assertNotNull(iterator);

            // seek in the middle with imprecise timestamp
            iterator.seek(BinaryFTraceCPUPageIteratorTestData.MID_PAGE_EVENT_IMPRECISE_TS);
            BinaryFTraceEvent event = iterator.getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimeSinceBoot(), BinaryFTraceCPUPageIteratorTestData.MID_PAGE_EVENT_PRECISE_TS);
            assertEquals(event.getFields().get("name"), BinaryFTraceCPUPageIteratorTestData.MID_PAGE_EVENT_NAME);
        }
    }

    /**
     * Seek for an event that is at the middle of the page with precise
     * timestamp.
     *
     * @throws Exception
     *             if an error occurred while iterating through the trace events
     */
    @Test
    public void testSeekEventAtPreciseTimeStampAtMidPage() throws Exception {
        BinaryFTraceCPUDataPage page = multipleEventTrace.getCpus().get(0).getPages().get(0);

        try (BinaryFTraceCPUPageIterator iterator = BinaryFTraceIteratorHelper.getPageIterator(page, multipleEventTrace)) {
            assertNotNull(iterator);

            // Seek in the middle of the page with precise timestamp
            iterator.seek(BinaryFTraceCPUPageIteratorTestData.MID_PAGE_EVENT_PRECISE_TS);
            BinaryFTraceEvent event = iterator.getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimeSinceBoot(), BinaryFTraceCPUPageIteratorTestData.MID_PAGE_EVENT_PRECISE_TS);
            assertEquals(event.getFields().get("name"), BinaryFTraceCPUPageIteratorTestData.MID_PAGE_EVENT_NAME);
        }
    }

    /**
     * Seek for an event at the end of the page
     *
     * @throws Exception
     *             if an error occurred while iterating through the trace events
     */
    @Test
    public void testSeekEventAtEndOfPage() throws Exception {
        BinaryFTraceCPUDataPage page = multipleEventTrace.getCpus().get(0).getPages().get(0);

        try (BinaryFTraceCPUPageIterator iterator = BinaryFTraceIteratorHelper.getPageIterator(page, multipleEventTrace)) {
            assertNotNull(iterator);

            iterator.seek(BinaryFTraceCPUPageIteratorTestData.LAST_EVENT_TS);
            BinaryFTraceEvent event = iterator.getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimeSinceBoot(), BinaryFTraceCPUPageIteratorTestData.LAST_EVENT_TS);
            assertEquals(event.getFields().get("name"), BinaryFTraceCPUPageIteratorTestData.LAST_EVENT_NAME);
        }
    }

    /**
     * Seek for an event with timestamp larger than the last timestamp of the
     * page
     *
     * @throws Exception
     *             if an error occurred while iterating through the trace events
     */
    @Test
    public void testSeekEventWithTimeStampLargerThanLastEvent() throws Exception {
        BinaryFTraceCPUDataPage page = multipleEventTrace.getCpus().get(0).getPages().get(0);

        try (BinaryFTraceCPUPageIterator iterator = BinaryFTraceIteratorHelper.getPageIterator(page, multipleEventTrace)) {
            assertNotNull(iterator);

            // Seek in the middle of the page with precise timestamp
            // seek at more than the end
            iterator.seek(BinaryFTraceCPUPageIteratorTestData.INVALID_TS);
            BinaryFTraceEvent event = iterator.getCurrentEvent();
            assertNull(event);
        }
    }

    /**
     * Test get time stamp
     *
     * @throws Exception
     *             if an error occurred while iterating through the trace events
     */
    @Test
    public void testGetTimeStamp() throws Exception {
        BinaryFTraceCPUDataPage page = multipleEventTrace.getCpus().get(0).getPages().get(0);

        try (BinaryFTraceCPUPageIterator iterator = BinaryFTraceIteratorHelper.getPageIterator(page, multipleEventTrace)) {
            assertNotNull(iterator);

            // The intial timestamp should be the page timestamp
            assertEquals(iterator.getCurrentTimeStamp(), page.getTimeStamp());
            iterator.readNextEvent();
            assertEquals(iterator.getCurrentTimeStamp(), BinaryFTraceCPUPageIteratorTestData.FIRST_EVENT_TS);
        }
    }

    /**
     * Test read all events from CPU 0 page 0 of trace_4cpu.dat
     *
     * @throws Exception
     *             if an error occurred while iterating through the trace events
     */
    @Test
    public void readAllEvents() throws Exception {
        BinaryFTraceCPUDataPage page = multipleEventTrace.getCpus().get(0).getPages().get(0);

        try (BinaryFTraceCPUPageIterator iterator = BinaryFTraceIteratorHelper.getPageIterator(page, multipleEventTrace)) {
            assertNotNull(iterator);

            int eventCount = 0;
            while (iterator.hasNext()) {
                iterator.readNextEvent();
                BinaryFTraceEvent event = iterator.getCurrentEvent();
                assertNotNull(event);
                eventCount++;
            }
            assertEquals(eventCount, 19);
        }
    }
}
