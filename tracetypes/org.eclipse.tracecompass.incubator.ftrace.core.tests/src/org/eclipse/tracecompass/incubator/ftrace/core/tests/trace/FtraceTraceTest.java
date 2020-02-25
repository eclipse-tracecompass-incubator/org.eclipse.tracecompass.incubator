/*******************************************************************************
 * Copyright (c) 2018 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.ftrace.core.tests.trace;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tracecompass.incubator.ftrace.core.tests.ActivatorTest;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.trace.FtraceTrace;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

/**
 * Ftrace Trace Test Class
 *
 * @author Guillaume Champagne
 * @author Alexis-Maurer Fortin
 * @author Hugo Genesse
 * @author Pierre-Yves Lajoie
 * @author Eva Terriault
 */
public class FtraceTraceTest {

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
        FtraceTrace ftraceTrace = new FtraceTrace();
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
        FtraceTrace ftraceTrace = new FtraceTrace();

        IStatus status = ftraceTrace.validate(null, "");

        assertEquals(0x04, status.getSeverity());
    }

    /**
     * Test validation if a directory is passed
     */
    @Test
    public void testValidateDirectory() {
        FtraceTrace ftraceTrace = new FtraceTrace();

        IStatus status = ftraceTrace.validate(null, "res/");

        assertEquals(0x04, status.getSeverity());
    }
}
