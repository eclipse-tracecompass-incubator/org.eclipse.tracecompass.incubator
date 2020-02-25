/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.scripting.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.scripting.core.ScriptExecutionHelper;
import org.eclipse.tracecompass.incubator.scripting.core.tests.stubs.ScriptingTestUtils;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the {@link ScriptExecutionHelper} class
 *
 * @author Geneviève Bastien
 */
@NonNullByDefault
public class HeadlessScriptExecutionTest {

    private static final String JS_FILE = "testfiles/scripts/testAnalysis.js";
    private static final String PY_FILE = "testfiles/scripts/testAnalysis.py";
    private static final String OTHER_FILE = "testfiles/scripts/test.bla";
    private static final String UNEXISTING_FILE = "testfiles/scripts/noFile";

    private @Nullable ITmfTrace fTrace;

    /**
     * Prepare the trace for the test
     */
    @Before
    public void setUpData() {
        fTrace = ScriptingTestUtils.getTrace();
    }

    /**
     * Dispose of objects after the test
     */
    @After
    public void cleanup() {
        ITmfTrace trace = fTrace;
        if (trace != null) {
            trace.dispose();
        }
    }

    /**
     * Test executing an unexisting file
     */
    @Test
    public void testUnexistingScript() {
        assertNull(ScriptExecutionHelper.executeScript(UNEXISTING_FILE));
    }

    /**
     * Test executing a file that has no script engine associated with it
     */
    @Test
    public void testUnsupportedEngine() {
        IPath path = ActivatorTest.getAbsoluteFilePath(OTHER_FILE);
        assertNull(ScriptExecutionHelper.executeScript(String.valueOf(path.toOSString())));
    }

    /**
     * Test a simple execution of a javascript file
     */
    @Test
    public void testJs() {
        IPath path = ActivatorTest.getAbsoluteFilePath(JS_FILE);
        Object result = ScriptExecutionHelper.executeScript(String.valueOf(path.toOSString()));
        assertEquals(36, result);
    }

    /**
     * Test a simple execution of a python file
     */
    @Test
    public void testPython() {
        IPath path = ActivatorTest.getAbsoluteFilePath(PY_FILE);
        Object result = ScriptExecutionHelper.executeScript(String.valueOf(path.toOSString()));
        assertEquals(36, result);
    }

}
