/*******************************************************************************
 * Copyright (c) 2021 Ericsson
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
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tracecompass.incubator.ftrace.core.tests.ActivatorTest;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.trace.FtraceTrace;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.trace.TrimmedFtraceTrace;
import org.junit.Test;

/**
 * Ftrace Trace Test Class
 *
 * @author Matthew Khouzam
 */
public class TrimmedFtraceTraceTest {

    private static final String TRACE_PATH = "res";

    /**
     * Test validation of ftrace traces
     */
    @Test
    public void testValidate() {
        IPath path = ActivatorTest.getAbsoluteFilePath(TRACE_PATH);
        File file = path.toFile();

        File[] traceFiles = file.listFiles();
        assertTrue(traceFiles.length > 0);
        FtraceTrace ftraceTrace = new TrimmedFtraceTrace();
        for (File f : traceFiles) {
            IStatus status = ftraceTrace.validate(null, f.getAbsolutePath());

            assertEquals("trace " + f, 0, status.getSeverity());
        }
    }

    /**
     * Test validation using file that does not exist
     */
    @Test
    public void testValidateFileDoesNotExist() {
        FtraceTrace ftraceTrace = new TrimmedFtraceTrace();

        IStatus status = ftraceTrace.validate(null, "");

        assertEquals(0x04, status.getSeverity());
    }

    /**
     * Test validation if a directory is passed
     */
    @Test
    public void testValidateDirectory() {
        FtraceTrace ftraceTrace = new TrimmedFtraceTrace();

        IStatus status = ftraceTrace.validate(null, "res/");

        assertEquals(0x04, status.getSeverity());
    }
}
