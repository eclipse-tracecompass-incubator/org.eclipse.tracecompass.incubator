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

package org.eclipse.tracecompass.incubator.scripting.core.tests.stubs;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.incubator.scripting.core.analysis.AnalysisScriptingModule;
import org.eclipse.tracecompass.incubator.scripting.core.analysis.ScriptedAnalysis;
import org.eclipse.tracecompass.incubator.scripting.core.tests.ActivatorTest;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStub;
import org.eclipse.tracecompass.tmf.tests.stubs.trace.xml.TmfXmlTraceStubNs;

/**
 * Utility methods for scripting tests.
 *
 * @author Geneviève Bastien
 */
@NonNullByDefault
public class ScriptingTestUtils {

    private static final String CALLSTACK_FILE = "testfiles/traces/callstack.xml";

    private ScriptingTestUtils() {
        // Nothing to do
    }

    /**
     * Get a trace to run tests with. The trace will be initialized and opened.
     * The caller should dispose of it at the end of the test.
     *
     * @return A trace to run tests on
     */
    public static ITmfTrace getTrace() {
        TmfXmlTraceStub trace = new TmfXmlTraceStubNs();
        try {

            IPath filePath = ActivatorTest.getAbsoluteFilePath(CALLSTACK_FILE);
            IStatus status = trace.validate(null, filePath.toOSString());
            if (!status.isOK()) {
                fail(status.getException().getMessage());
            }
            trace.initTrace(null, filePath.toOSString(), TmfEvent.class);

            TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(null, trace, null);
            trace.traceOpened(signal);
            TmfTraceManager.getInstance().traceOpened(signal);

            return trace;
        } catch (Exception e) {
            trace.dispose();
            throw new NullPointerException("The trace could not be instantiated" + e.getMessage());
        }

    }

    /**
     * Get a {@link ScriptedAnalysis} instance with the given name for a trace
     *
     * @param trace
     *            The trace associated with the analysis. The trace can be
     *            obtained using {@link #getTrace()}.
     * @param analysisName
     *            The name of the analysis to generate.
     * @return The scripted analysis
     */
    public static ScriptedAnalysis getAnalysis(ITmfTrace trace, String analysisName) {
        AnalysisScriptingModule scriptModule = new AnalysisScriptingModule();

        ScriptedAnalysis analysis = scriptModule.createScriptedAnalysis(trace, analysisName);
        assertNotNull(analysis);

        return analysis;
    }

}
