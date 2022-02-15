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

package org.eclipse.tracecompass.incubator.ftrace.core.tests.trace;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tracecompass.incubator.ftrace.core.tests.shared.FTraceUtils;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.trace.BinaryFTrace;
import org.eclipse.tracecompass.testtraces.ftrace.FtraceTestTrace;
import org.junit.Test;

/**
 * Tests for {@link BinaryFTrace}
 *
 * @author Hoang Thuan Pham
 */
public class BinaryFTraceTest {
    private static final String NON_EXISTING_PATH = "/wrong/path/no_trace.dat";

    /**
     * Test validation of a valid trace
     *
     * @throws IOException
     *             If an error occurred while validating the file
     */
    @Test
    public void testValidTrace() throws IOException {
        BinaryFTrace ftraceTrace = new BinaryFTrace();
        String validTracePath = FTraceUtils.getTraceAbsolutePath(FtraceTestTrace.TEST_2_6_MULTIPLE_CPUS);
        IStatus goodStatus = ftraceTrace.validate(null, validTracePath);
        assertEquals(0, goodStatus.getSeverity());
        ftraceTrace.dispose();
    }

    /**
     * Test validation of an invalid trace
     *
     * @throws IOException
     *             If an error occurred while validating the file
     */
    @Test
    public void testInvalidTrace() throws IOException {
        BinaryFTrace ftraceTrace = new BinaryFTrace();
        String invalidTracePath = FTraceUtils.getTraceAbsolutePath(FtraceTestTrace.TEST_2_6_INVALID);
        IStatus invalidStatus = ftraceTrace.validate(null, invalidTracePath);
        assertEquals(0x04, invalidStatus.getSeverity());
        ftraceTrace.dispose();
    }

    /**
     * Test validation using file that does not exist
     */
    @Test
    public void testValidateFileDoesNotExist() {
        BinaryFTrace ftraceTrace = new BinaryFTrace();
        IStatus status = ftraceTrace.validate(null, NON_EXISTING_PATH);
        assertEquals(0x04, status.getSeverity());
        ftraceTrace.dispose();
    }
}
