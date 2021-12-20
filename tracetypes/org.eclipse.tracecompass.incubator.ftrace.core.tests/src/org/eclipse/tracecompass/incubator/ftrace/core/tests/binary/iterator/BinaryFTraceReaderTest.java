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
import static org.junit.Assert.assertTrue;

import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.parser.BinaryFTraceFileParser;
import org.eclipse.tracecompass.incubator.ftrace.core.tests.shared.FTraceUtils;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.event.BinaryFTraceEvent;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceHeaderInfo;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.iterator.BinaryFTraceCPUSectionIterator;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.iterator.BinaryFTraceReader;
import org.eclipse.tracecompass.testtraces.ftrace.FtraceTestTrace;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for public methods of {@link BinaryFTraceReaderTest}
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceReaderTest {
    private static BinaryFTraceHeaderInfo multipleEventTrace; // trace with 4
                                                              // CPU
    private static BinaryFTraceHeaderInfo emptyTrace; // empty trace file

    /**
     * Initialize data for the test
     *
     * @throws Exception
     *             An error occurred while parsing the traces
     */
    @BeforeClass
    public static void initTest() throws Exception {
        multipleEventTrace = BinaryFTraceFileParser.parse(FTraceUtils.getTraceAbsolutePath(FtraceTestTrace.TEST_2_6_4_CPU));
        emptyTrace = BinaryFTraceFileParser.parse(FTraceUtils.getTraceAbsolutePath(FtraceTestTrace.TEST_2_6_EMPTY));
    }

    /**
     * This test the constructor of the {@link BinaryFTraceReaderTest}. It also
     * test the following functions, as they must be used together to complete
     * the test case. getStartTime() getTopStream()
     *
     * @throws Exception
     *             An error occurred while iterating through the trace events
     */
    @Test
    public void constructorTest() throws Exception {
        try (BinaryFTraceReader reader = new BinaryFTraceReader(multipleEventTrace);
                BinaryFTraceCPUSectionIterator iter = reader.getTopStream();) {
            BinaryFTraceEvent event = iter.getCurrentEvent();

            assertEquals(reader.getStartTime(), BinaryFTraceReaderTestData.FIRST_TRACE_EVENT_TS);
            assertNotNull(event);
            assertEquals(event.getTimeSinceBoot(), BinaryFTraceReaderTestData.FIRST_TRACE_EVENT_TS);
            assertEquals(event.getFields().get("name"), BinaryFTraceReaderTestData.FIRST_TRACE_EVENT_NAME);
        }
    }

    /**
     * Test both the close() and isClose() function
     *
     * @throws Exception
     *             An error occurred while iterating through the trace events
     */
    @Test
    public void testClose() throws Exception {
        try (BinaryFTraceReader reader = new BinaryFTraceReader(multipleEventTrace)) {
            assertFalse(reader.isClosed());
            reader.close();
            assertTrue(reader.isClosed());
            assertFalse(reader.hasMoreEvents());
        }
    }

    /**
     * Test getting the getStartTime() and getEndTime() of the reader.
     *
     * @throws Exception
     *             An error occurred while iterating through the trace events
     */
    @Test
    public void testGetStartTimeAndEndTime() throws Exception {
        try (BinaryFTraceReader reader = new BinaryFTraceReader(multipleEventTrace)) {
            reader.seek(BinaryFTraceReaderTestData.MID_TRACE_EVENT_TS);
            assertEquals(reader.getStartTime(), BinaryFTraceReaderTestData.FIRST_TRACE_EVENT_TS);

            // Move the pointer to read the next event
            assertEquals(reader.getEndTime(), BinaryFTraceReaderTestData.MID_TRACE_EVENT_TS);
        }
    }

    /**
     * Test for hasMoreEvent() function.
     *
     * @throws Exception
     *             An error occurred while iterating through the trace events
     */
    @Test
    public void testHasMoreEvents() throws Exception {
        try (BinaryFTraceReader reader = new BinaryFTraceReader(emptyTrace)) {
            assertFalse(reader.hasMoreEvents());
        }

        try (BinaryFTraceReader reader = new BinaryFTraceReader(multipleEventTrace)) {
            // The first event in trace
            assertTrue(reader.hasMoreEvents());

            // Move the reader to the last event
            reader.seek(BinaryFTraceReaderTestData.LAST_TRACE_EVENT_TS);
            reader.advance();
            assertFalse(reader.hasMoreEvents());
        }
    }

    /**
     * Test the advance function.
     *
     * @throws Exception
     *             An error occurred while iterating through the trace events
     */
    @Test
    public void testAdvance() throws Exception {
        try (BinaryFTraceReader reader = new BinaryFTraceReader(multipleEventTrace)) {
            // Advance to the second trace of the event and read it
            boolean ret = reader.advance();
            assertTrue(ret);

            // Do not close the top stream to ensure that the code functions
            // correctly
            BinaryFTraceEvent event = reader.getTopStream().getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getFields().get("name"), BinaryFTraceReaderTestData.SECOND_TRACE_EVENT_NAME);
            assertEquals(event.getTimeSinceBoot(), BinaryFTraceReaderTestData.SECOND_TRACE_EVENT_TS);

            // Position the reader at the last event
            reader.seek(BinaryFTraceReaderTestData.LAST_TRACE_EVENT_TS);
            ret = reader.advance();
            assertFalse(ret);
        }
    }

    /**
     * Seek the first trace event with timestamp 0 or its own timestamp.
     *
     * @throws Exception
     *             An error occurred while iterating through the trace events
     */
    @Test
    public void testSeekFirstEvent() throws Exception {
        try (BinaryFTraceReader reader = new BinaryFTraceReader(multipleEventTrace)) {
            reader.seek(0);
            // Do not close the top stream to ensure that the code functions
            // correctly
            BinaryFTraceEvent event = reader.getTopStream().getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimeSinceBoot(), BinaryFTraceReaderTestData.FIRST_TRACE_EVENT_TS);
            assertNotNull(event);
            assertEquals(reader.getEndTime(), BinaryFTraceReaderTestData.FIRST_TRACE_EVENT_TS);

            reader.seek(BinaryFTraceReaderTestData.FIRST_TRACE_EVENT_TS);
            // Do not close the top stream to ensure that the code functions
            // correctly
            event = reader.getTopStream().getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimeSinceBoot(), BinaryFTraceReaderTestData.FIRST_TRACE_EVENT_TS);
            assertEquals(reader.getEndTime(), BinaryFTraceReaderTestData.FIRST_TRACE_EVENT_TS);
        }
    }

    /**
     * Test seeking a random trace using a imprecise and precise timestamp.
     *
     * @throws Exception
     *             An error occurred while iterating through the trace events
     */
    @Test
    public void seekMidTraceEvent() throws Exception {
        try (BinaryFTraceReader reader = new BinaryFTraceReader(multipleEventTrace)) {
            reader.seek(BinaryFTraceReaderTestData.MID_TRACE_EVENT_IMPRECISE_TS);
            BinaryFTraceEvent event = reader.getTopStream().getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getFields().get("name"), BinaryFTraceReaderTestData.MID_TRACE_EVENT_NAME);
            assertEquals(event.getTimeSinceBoot(), BinaryFTraceReaderTestData.MID_TRACE_EVENT_TS);
            assertEquals(reader.getEndTime(), BinaryFTraceReaderTestData.MID_TRACE_EVENT_TS);

            reader.seek(BinaryFTraceReaderTestData.MID_TRACE_EVENT_TS);
            event = reader.getTopStream().getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getFields().get("name"), BinaryFTraceReaderTestData.MID_TRACE_EVENT_NAME);
            assertEquals(event.getTimeSinceBoot(), BinaryFTraceReaderTestData.MID_TRACE_EVENT_TS);
            assertEquals(reader.getEndTime(), BinaryFTraceReaderTestData.MID_TRACE_EVENT_TS);
        }
    }

    /**
     * Test seeking the last event.
     *
     * @throws Exception
     *             An error occurred while iterating through the trace events
     */
    @Test
    public void seekEndTraceEvent() throws Exception {
        try (BinaryFTraceReader reader = new BinaryFTraceReader(multipleEventTrace)) {
            reader.seek(BinaryFTraceReaderTestData.LAST_TRACE_EVENT_TS);
            // Do not close the top stream to ensure that the code functions
            // correctly
            BinaryFTraceEvent event = reader.getTopStream().getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimeSinceBoot(), BinaryFTraceReaderTestData.LAST_TRACE_EVENT_TS);
            assertEquals(reader.getEndTime(), BinaryFTraceReaderTestData.LAST_TRACE_EVENT_TS);

            // Confirm that the trace is the last event
            reader.advance();
            assertEquals(reader.hasMoreEvents(), false);
        }
    }
}
