/*******************************************************************************
 * Copyright (c) 2018 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.ftrace.core.tests.trace;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tracecompass.incubator.internal.ftrace.core.trace.FtraceTrace;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

    /**
     * Test validation of ftrace trace
     */
    @Test
    public void testValidate() {

        FtraceTrace ftraceTrace = new FtraceTrace();

        IStatus status = ftraceTrace.validate(null, "res/trace-android-sched");

        assertEquals(0, status.getSeverity());

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
