/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.scripting.core.tests.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.scripting.core.analysis.AnalysisScriptingModule;
import org.eclipse.tracecompass.incubator.scripting.core.analysis.ScriptedAnalysis;
import org.eclipse.tracecompass.incubator.scripting.core.tests.stubs.ScriptingTestUtils;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

/**
 * Test the {@link AnalysisScriptingModule}'s specific objects and methods
 *
 * @author Geneviève Bastien
 */
public class AnalysisModuleTest {

    private static final @NonNull String ANALYSIS_NAME = "scriptAnalysisTest";

    /** Time-out tests after 1 minute. */
    @Rule
    public TestRule globalTimeout = new Timeout(5, TimeUnit.MINUTES);

    /**
     * Test the event iterator
     */
    @Test
    public void testEventIterator() {
        AnalysisScriptingModule scriptModule = new AnalysisScriptingModule();

        ITmfTrace trace = ScriptingTestUtils.getTrace();
        try {
            ScriptedAnalysis analysis = scriptModule.getAnalysis(ANALYSIS_NAME);
            assertNotNull(analysis);

            Iterator<ITmfEvent> eventIterator = analysis.getEventIterator();
            assertNotNull(eventIterator);

            int count = 0;
            while (eventIterator.hasNext()) {
                eventIterator.next();
                count++;
            }
            // Make sure it parsed the whole trace
            assertEquals(36, count);

        } finally {
            trace.dispose();
        }
    }

}
