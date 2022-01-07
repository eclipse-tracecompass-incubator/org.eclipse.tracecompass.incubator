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

import java.io.IOException;

import org.eclipse.tracecompass.incubator.ftrace.core.tests.shared.FTraceUtils;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.context.BinaryFTraceLocation;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.context.BinaryFTraceLocationInfo;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceHeaderInfo;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.iterator.BinaryFTraceIterator;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.parser.BinaryFTraceFileParser;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.event.GenericFtraceEvent;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.trace.BinaryFTrace;
import org.eclipse.tracecompass.testtraces.ftrace.FtraceTestTrace;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for {@link BinaryFTraceIterator}.
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceIteratorTest {
    // Trace with 4 CPUs
    private static BinaryFTraceHeaderInfo multipleEventTrace;
    // An empty trace that is recorded on a machine that has 4 CPUs
    private static BinaryFTraceHeaderInfo emptyTrace;

    private static BinaryFTrace ftrace;

    /**
     * Initialize variables used in the test
     *
     * @throws IOException
     *             if an error occured while getting the test trace paths
     * @throws TmfTraceException
     *             if an error occured while parsing the test traces
     */
    @BeforeClass
    public static void initTest() throws TmfTraceException, IOException {
        ftrace = new BinaryFTrace();
        multipleEventTrace = BinaryFTraceFileParser.parse(FTraceUtils.getTraceAbsolutePath(FtraceTestTrace.TEST_2_6_MULTIPLE_CPUS));
        emptyTrace = BinaryFTraceFileParser.parse(FTraceUtils.getTraceAbsolutePath(FtraceTestTrace.TEST_2_6_EMPTY));
    }

    /**
     * Clean up after tests are finished
     */
    @AfterClass
    public static void onFinishTests() {
        ftrace.dispose();
    }

    /**
     * Test initialize an iterator without a location. The iterator should
     * position itself at the first location in the trace.
     *
     * @throws IOException
     *             if an error occurred while iterating through trace events
     */
    @Test
    public void constructorTestWithNoLocation() throws IOException {
        assertNotNull(ftrace);

        try (BinaryFTraceIterator iterator = new BinaryFTraceIterator(multipleEventTrace, ftrace)) {
            BinaryFTraceLocation location = iterator.getLocation();

            // Make sure that the iterator positions itself at the correct
            // location (Min timestamp value)
            assertNotNull(location);
            assertEquals(location.getLocationInfo().getTimestamp(), TmfTimestamp.BIG_BANG.toNanos());
            assertEquals(iterator.getRank(), 0);
        }
    }

    /**
     * Test the has more events method
     *
     * @throws IOException
     *             if an error occurred while iterating through trace events
     */
    @Test
    public void testHasMoreEvents() throws IOException {
        // This trace has no events, so has more events returns false
        assertNotNull(ftrace);
        try (BinaryFTraceIterator iterator = new BinaryFTraceIterator(emptyTrace, ftrace)) {
            assertFalse(iterator.hasMoreEvents());
        }

        // This trace has multiple events, so has more events returns true
        assertNotNull(ftrace);
        try (BinaryFTraceIterator iterator = new BinaryFTraceIterator(multipleEventTrace, ftrace)) {
            assertTrue(iterator.hasMoreEvents());
        }
    }

    /**
     * Test getting the current event of the iterator
     *
     * @throws IOException
     *             if an error occurred while iterating through trace events
     */
    @Test
    public void testGetCurrentEvent() throws IOException {
        assertNotNull(ftrace);
        try (BinaryFTraceIterator iterator = new BinaryFTraceIterator(multipleEventTrace, ftrace)) {
            GenericFtraceEvent event = iterator.getCurrentEvent();
            assertEquals(event.getName(), BinaryFTraceIteratorTestData.FIRST_TRACE_EVENT_NAME);
            assertEquals(event.getTimestamp().toNanos(), BinaryFTraceIteratorTestData.FIRST_TRACE_EVENT_TS);
        }

        assertNotNull(ftrace);
        try (BinaryFTraceIterator iterator = new BinaryFTraceIterator(emptyTrace, ftrace)) {
            GenericFtraceEvent event = iterator.getCurrentEvent();
            assertNull(event);
        }
    }

    /**
     * Seek an event at the beginning of the trace
     *
     * @throws IOException
     *             if an error occurred while iterating through trace events
     */
    @Test
    public void testSeekFirstEvent() throws IOException {
        assertNotNull(ftrace);

        try (BinaryFTraceIterator iterator = new BinaryFTraceIterator(multipleEventTrace, ftrace)) {
            GenericFtraceEvent event = iterator.getCurrentEvent();

            assertNotNull(event);
            assertEquals(event.getTimestamp().toNanos(), BinaryFTraceIteratorTestData.FIRST_TRACE_EVENT_TS);
            assertEquals(event.getName(), BinaryFTraceIteratorTestData.FIRST_TRACE_EVENT_NAME);
        }
    }

    /**
     * Seek an event in the middle of the trace with an imprecise timestamp
     *
     * @throws IOException
     *             if an error occurred while iterating through trace events
     */
    @Test
    public void testSeekEventWithImpreciseTimestamp() throws IOException {
        BinaryFTraceLocationInfo locationInfo = new BinaryFTraceLocationInfo(BinaryFTraceIteratorTestData.MID_TRACE_EVENT_IMPRECISE_TS, 0);

        assertNotNull(ftrace);
        try (BinaryFTraceIterator iterator = new BinaryFTraceIterator(multipleEventTrace, ftrace)) {
            boolean ret = iterator.seek(locationInfo);
            assertTrue(ret);

            GenericFtraceEvent event = iterator.getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimestamp().toNanos(), BinaryFTraceIteratorTestData.MID_TRACE_EVENT_TS);
            assertEquals(event.getName(), BinaryFTraceIteratorTestData.MID_TRACE_EVENT_NAME);
        }
    }

    /**
     * Seek an event in the middle of the trace with a precise timestamp
     *
     * @throws IOException
     *             if an error occurred while iterating through trace events
     */
    @Test
    public void testSeekEventWithPreciseTimestamp() throws IOException {
        BinaryFTraceLocationInfo locationInfo = new BinaryFTraceLocationInfo(BinaryFTraceIteratorTestData.MID_TRACE_EVENT_TS, 0);

        assertNotNull(ftrace);
        try (BinaryFTraceIterator iterator = new BinaryFTraceIterator(multipleEventTrace, ftrace)) {
            boolean ret = iterator.seek(locationInfo);
            assertTrue(ret);

            GenericFtraceEvent event = iterator.getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimestamp().toNanos(), BinaryFTraceIteratorTestData.MID_TRACE_EVENT_TS);
            assertEquals(event.getName(), BinaryFTraceIteratorTestData.MID_TRACE_EVENT_NAME);
        }
    }

    /**
     * Seek the last event of the trace
     *
     * @throws IOException
     *             If an error occured while reading the trace
     */
    @Test
    public void testSeekLastEvent() throws IOException {
        BinaryFTraceLocationInfo locationInfo = new BinaryFTraceLocationInfo(BinaryFTraceIteratorTestData.LAST_TRACE_EVENT_TS, 0);

        assertNotNull(ftrace);
        try (BinaryFTraceIterator iterator = new BinaryFTraceIterator(multipleEventTrace, ftrace)) {
            boolean ret = iterator.seek(locationInfo);
            assertTrue(ret);

            GenericFtraceEvent event = iterator.getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimestamp().toNanos(), BinaryFTraceIteratorTestData.LAST_TRACE_EVENT_TS);
        }
    }

    /**
     * Test getting the next event
     *
     * @throws IOException
     *             if an error occurred while iterating through trace events
     */
    @Test
    public void testAdvance() throws IOException {
        assertNotNull(ftrace);

        try (BinaryFTraceIterator iterator = new BinaryFTraceIterator(multipleEventTrace, ftrace)) {
            // Advance to the second event of the trace
            boolean ret = iterator.advance();
            assertTrue(ret);

            GenericFtraceEvent event = iterator.getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimestamp().toNanos(), BinaryFTraceIteratorTestData.SECOND_TRACE_EVENT_TS);
            assertEquals(event.getName(), BinaryFTraceIteratorTestData.SECOND_TRACE_EVENT_NAME);

            // Position the iterator at the last event and try to seek the next
            // one
            iterator.seek(BinaryFTraceIteratorTestData.LAST_TRACE_EVENT_TS);
            ret = iterator.advance();
            assertFalse(ret);
        }
    }

    /**
     * Test getting the current timestamp
     *
     * @throws IOException
     *             if an error occurred while iterating through trace events
     */
    @Test
    public void testGetCurrentTimestamp() throws IOException {
        BinaryFTraceLocationInfo locationInfo = new BinaryFTraceLocationInfo(BinaryFTraceIteratorTestData.MID_TRACE_EVENT_IMPRECISE_TS, 0);

        assertNotNull(ftrace);
        try (BinaryFTraceIterator iterator = new BinaryFTraceIterator(multipleEventTrace, ftrace)) {
            boolean ret = iterator.seek(locationInfo);
            assertTrue(ret);
            assertEquals(iterator.getCurrentTimestamp(), BinaryFTraceIteratorTestData.MID_TRACE_EVENT_TS);
        }
    }

    /**
     * Test the setLocation() method. The setLocation() method should position
     * the trace at the correct position.
     *
     * @throws IOException
     *             if an error occurred while iterating through trace events
     */
    @Test
    public void testSetLocation() throws IOException {
        BinaryFTraceLocationInfo locationInfo = new BinaryFTraceLocationInfo(BinaryFTraceIteratorTestData.MID_TRACE_EVENT_TS, 0);

        assertNotNull(ftrace);
        try (BinaryFTraceIterator iterator = new BinaryFTraceIterator(multipleEventTrace, ftrace)) {
            // The setLocation method should position the trace at the correct
            // position
            iterator.setLocation(new BinaryFTraceLocation(locationInfo));

            GenericFtraceEvent event = iterator.getCurrentEvent();
            assertNotNull(event);
            assertEquals(event.getTimestamp().toNanos(), BinaryFTraceIteratorTestData.MID_TRACE_EVENT_TS);
            assertEquals(event.getName(), BinaryFTraceIteratorTestData.MID_TRACE_EVENT_NAME);
        }
    }

    /**
     * Test increaseRank() function, but also getRank()
     *
     * @throws IOException
     *             if an error occurred while iterating through trace events
     */
    @Test
    public void testIncreaseRank() throws IOException {
        assertNotNull(ftrace);
        try (BinaryFTraceIterator iterator = new BinaryFTraceIterator(multipleEventTrace, ftrace)) {
            assertEquals(iterator.getRank(), 0);
            iterator.increaseRank();
            assertEquals(iterator.getRank(), 1);
        }
    }

    /**
     * Test for setting rank
     *
     * @throws IOException
     *             if an error occurred while iterating through trace events
     */
    @Test
    public void testSetRank() throws IOException {
        assertNotNull(ftrace);
        try (BinaryFTraceIterator iterator = new BinaryFTraceIterator(multipleEventTrace, ftrace)) {
            iterator.setRank(12);
            assertEquals(iterator.getRank(), 12);
        }
    }

    /**
     * Test for checking if the rank is valid. Test for setRank and
     * hasValidRank.
     *
     * @throws IOException
     *             if an error occurred while iterating through trace events
     */
    @Test
    public void testHasValidRank() throws IOException {
        assertNotNull(ftrace);
        try (BinaryFTraceIterator iterator = new BinaryFTraceIterator(multipleEventTrace, ftrace)) {
            // Intially the rank should be 0
            assertTrue(iterator.hasValidRank());

            iterator.setRank(-1);
            assertFalse(iterator.hasValidRank());
        }
    }

    /**
     * Seek an event in the middle of the trace with a precise timestamp
     *
     * @throws IOException
     *             if an error occurred while iterating through trace events
     */
    @Test
    public void testGetLocation() throws IOException {
        BinaryFTraceLocationInfo locationInfo = new BinaryFTraceLocationInfo(BinaryFTraceIteratorTestData.MID_TRACE_EVENT_TS, 0);

        assertNotNull(ftrace);
        try (BinaryFTraceIterator iterator = new BinaryFTraceIterator(multipleEventTrace, ftrace)) {
            iterator.seek(locationInfo);
            assertEquals(iterator.getLocation().getLocationInfo().getTimestamp(), BinaryFTraceIteratorTestData.MID_TRACE_EVENT_TS);
        }
    }

    /**
     * Test the compareTo() method. The comparison is made by rank.
     *
     * @throws IOException
     *             if an error occurred while iterating through trace events
     */
    @Test
    public void testCompareTo() throws IOException {
        assertNotNull(ftrace);
        try (BinaryFTraceIterator iterator1 = new BinaryFTraceIterator(multipleEventTrace, ftrace);
                BinaryFTraceIterator iterator2 = new BinaryFTraceIterator(multipleEventTrace, ftrace)) {
            // Initially 2 iterators have the same rank of 0, so they are equal
            assertEquals(iterator1.compareTo(iterator2), 0);

            // Iterator 1 is larger
            iterator1.increaseRank();
            assertTrue(iterator1.compareTo(iterator2) > 0);
            assertTrue(iterator2.compareTo(iterator1) < 0);
        }
    }
}
