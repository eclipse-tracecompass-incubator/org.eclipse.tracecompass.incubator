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

package org.eclipse.tracecompass.incubator.ftrace.core.tests.binary.buffer;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.eclipse.tracecompass.incubator.ftrace.core.tests.shared.FTraceUtils;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.header.BinaryFTraceDataType;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.parser.BinaryFTraceByteBuffer;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.binary.parser.BinaryFTraceHeaderElementSize;
import org.eclipse.tracecompass.testtraces.ftrace.FtraceTestTrace;
import org.junit.Test;

/**
 * Tests for {@link BinaryFTraceByteBuffer}
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceByteBufferTest {
    private static final String TRACING = "tracing";
    private static final String VERSION = "6";
    private static final int RANDOM_OFFSET = 3;

    /**
     * Test to make sure the current offset is incremented properly when reading
     * the data.
     *
     * @throws IOException
     *             If an error occurred while getting the file path or reading
     *             the data
     */
    @Test
    public void testOffsetCounter() throws IOException {
        String traceLocation = FTraceUtils.getTraceAbsolutePath(FtraceTestTrace.TEST_2_6_MULTIPLE_CPUS);
        long currentOffset = 0;

        BinaryFTraceByteBuffer buffer = new BinaryFTraceByteBuffer(traceLocation);
        // Read the magic values
        buffer.getNextBytes(BinaryFTraceHeaderElementSize.MAGIC_VALUE);
        currentOffset += BinaryFTraceHeaderElementSize.MAGIC_VALUE;
        assertEquals(buffer.getCurrentOffset(), currentOffset);

        String tracingString = buffer.getNextBytesAsString(BinaryFTraceHeaderElementSize.TRACING_STRING);
        currentOffset += BinaryFTraceHeaderElementSize.TRACING_STRING;
        assertEquals(tracingString, TRACING);
        assertEquals(buffer.getCurrentOffset(), currentOffset);

        String versionString = buffer.getNextString();
        currentOffset += (BinaryFTraceHeaderElementSize.FTRACE_VERSION + BinaryFTraceHeaderElementSize.STRING_TERMINATOR);
        assertEquals(versionString, VERSION);
        assertEquals(buffer.getCurrentOffset(), currentOffset);

        // Read various data types and validate offset changes
        buffer.getNextShort();
        currentOffset += BinaryFTraceDataType.SHORT.getSize();
        assertEquals(buffer.getCurrentOffset(), currentOffset);

        buffer.getNextInt();
        currentOffset += BinaryFTraceDataType.INT.getSize();
        assertEquals(buffer.getCurrentOffset(), currentOffset);

        buffer.getNextLong();
        currentOffset += BinaryFTraceDataType.LONG.getSize();
        assertEquals(buffer.getCurrentOffset(), currentOffset);

        buffer.movePointerToOffset(RANDOM_OFFSET);
        currentOffset = RANDOM_OFFSET;
        assertEquals(buffer.getCurrentOffset(), currentOffset);
    }
}
