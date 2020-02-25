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

package org.eclipse.tracecompass.incubator.scripting.core.tests.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.scripting.core.analysis.TmfScriptAnalysis;
import org.eclipse.tracecompass.incubator.scripting.core.tests.ActivatorTest;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStub;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStubNs;
import org.junit.Test;

/**
 * Test the {@link TmfScriptAnalysis} class
 *
 * @author Geneviève Bastien
 */
public class TmfScriptAnalysisTest {

    private static final String CALLSTACK_FILE = "testfiles/traces/callstack.xml";

    /** Callers need to dispose the trace */
    private @NonNull ITmfTrace getTrace() {
        TmfXmlTraceStub trace = new TmfXmlTraceStubNs();
        IPath filePath = ActivatorTest.getAbsoluteFilePath(CALLSTACK_FILE);
        IStatus status = trace.validate(null, filePath.toOSString());
        if (!status.isOK()) {
            trace.dispose();
            fail(status.getException().getMessage());
        }
        try {
            trace.initTrace(null, filePath.toOSString(), TmfEvent.class);
        } catch (TmfTraceException e) {
            trace.dispose();
            fail(e.getMessage());
        }
        TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(this, trace, null);
        trace.traceOpened(signal);
        TmfTraceManager.getInstance().traceOpened(signal);
        return trace;
    }

    /**
     * Test that the analysis exists
     */
    @Test
    public void testGettingStateSystems() {
        String ssid = "abc";
        ITmfTrace trace = getTrace();
        try {
            IAnalysisModule analysisModule = trace.getAnalysisModule(TmfScriptAnalysis.ID);
            assertTrue(analysisModule instanceof TmfScriptAnalysis);

            TmfScriptAnalysis module = (TmfScriptAnalysis) analysisModule;

            assertNull(module.getStateSystem(ssid));

            // Create a new state system
            ITmfStateSystem stateSystem = module.getStateSystem(ssid, true);
            assertNotNull(stateSystem);

            // make sure it is returned by the analysis
            assertNotNull(module.getStateSystem(ssid));

            // Get the same state system
            ITmfStateSystem ss2 = module.getStateSystem(ssid, true);
            assertEquals(stateSystem, ss2);

            // Force a new state system and make sure it is different
            ss2 = module.getStateSystem(ssid, false);
            assertNotEquals(stateSystem, ss2);

            // Dispose of the trace and re-open it, get the analysis and make sure the state system exists
            trace.dispose();
            trace = getTrace();

            module = TmfTraceUtils.getAnalysisModuleOfClass(trace, TmfScriptAnalysis.class, TmfScriptAnalysis.ID);
            assertNotNull(module);

            assertNull(module.getStateSystem(ssid));

        } finally {
            trace.dispose();
        }

    }

}
