/*******************************************************************************
 * Copyright (c) 2018 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.atrace.core.tests.trace;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tracecompass.incubator.internal.atrace.trace.ATrace;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Testing of systrace html files
 *
 * @author Guillaume Champagne
 * @author Alexis-Maurer Fortin
 * @author Hugo Genesse
 * @author Pierre-Yves Lajoie
 * @author Eva Terriault
 */
public class ATraceTest {

    /**
     * Test validation of systrace html trace
     */
    @Test
    public void testValidate() {

        ATrace androidTrace = new ATrace();

        IStatus status = androidTrace.validate(null, "res/android_trace.html");

        assertEquals(0, status.getSeverity());

    }

    /**
     * Test validation using file that does not exist
     */
    @Test
    public void testValidateFileDoesNotExist() {
        ATrace androidTrace = new ATrace();

        IStatus status = androidTrace.validate(null, "");

        assertEquals(0x04, status.getSeverity());
    }

    /**
     * Test validation if a directory is passed
     */
    @Test
    public void testValidateDirectory() {
        ATrace androidTrace = new ATrace();

        IStatus status = androidTrace.validate(null, "res/");

        assertEquals(0x04, status.getSeverity());
    }
}
