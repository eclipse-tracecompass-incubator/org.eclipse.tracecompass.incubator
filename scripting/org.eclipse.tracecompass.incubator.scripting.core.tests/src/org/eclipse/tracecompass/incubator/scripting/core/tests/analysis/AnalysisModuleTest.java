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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.scripting.core.analysis.AnalysisScriptingModule;
import org.eclipse.tracecompass.incubator.scripting.core.analysis.ScriptedAnalysis;
import org.eclipse.tracecompass.incubator.scripting.core.tests.stubs.ScriptingTestUtils;
import org.eclipse.tracecompass.incubator.scripting.core.trace.ScriptEventsIterator;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import com.google.common.collect.Iterables;

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
            ScriptedAnalysis analysis = scriptModule.createScriptedAnalysis(trace, ANALYSIS_NAME);
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

    /**
     * Test the event iterator with an events filter
     */
    @Test
    public void testEventIteratorWithFilter() {
        AnalysisScriptingModule scriptModule = new AnalysisScriptingModule();

        ITmfTrace trace = ScriptingTestUtils.getTrace();
        try {
            ScriptedAnalysis analysis = scriptModule.createScriptedAnalysis(trace, ANALYSIS_NAME);
            assertNotNull(analysis);

            ScriptEventsIterator eventIterator = analysis.getEventIterator();
            assertNotNull(eventIterator);
            eventIterator.addEvent("entry");

            int count = 0;
            while (eventIterator.hasNext()) {
                eventIterator.next();
                count++;
            }
            // Make sure it parsed the whole trace
            assertEquals(18, count);

        } finally {
            trace.dispose();
        }
    }


    /**
     * Test the
     * {@link AnalysisScriptingModule#getTraceAnalysis(ITmfTrace, String)}
     * method
     */
    @Test
    public void testGetAnalysis() {
        AnalysisScriptingModule scriptModule = new AnalysisScriptingModule();

        ITmfTrace trace = ScriptingTestUtils.getTrace();
        TmfExperiment exp = null;
        try {
            Iterable<@NonNull IAnalysisModule> analysisModules = trace.getAnalysisModules();
            assertTrue(Iterables.size(analysisModules) > 0);
            // Map to save trace modules
            Map<String, IAnalysisModule> traceModules = new HashMap<>();
            for (IAnalysisModule module : analysisModules) {
                // Get the module by ID
                IAnalysisModule analysis = scriptModule.getTraceAnalysis(trace, module.getId());
                assertEquals("Analysis by ID " + module.getId(), module, analysis);

                // Get the module by name, don't assert equality, as some
                // analysis may have the same name as others
                analysis = scriptModule.getTraceAnalysis(trace, module.getName());
                assertNotNull("Analysis by name " + module.getName(), analysis);

                // Add the module to the map
                traceModules.put(module.getId(), module);
            }

            // Add the trace to an experiment and make sure all those analyses
            // are found
            ITmfTrace[] traces = new ITmfTrace[] { trace };
            exp = new TmfExperiment(ITmfEvent.class, "test-exp", traces,
                    TmfExperiment.DEFAULT_INDEX_PAGE_SIZE, null);
            exp.traceOpened(new TmfTraceOpenedSignal(this, exp, null));

            Iterable<@NonNull IAnalysisModule> experimentModules = exp.getAnalysisModules();
            assertTrue(Iterables.size(experimentModules) > 0);
            for (IAnalysisModule module : experimentModules) {
                // Get the module by ID
                IAnalysisModule analysis = scriptModule.getTraceAnalysis(exp, module.getId());
                assertEquals("Experiment analysis by ID " + module.getId(), module, analysis);

                // Get the module by name, don't assert equality, as some
                // analysis may have the same name as others
                analysis = scriptModule.getTraceAnalysis(exp, module.getName());
                assertNotNull("Experiment analysis by name " + module.getName(), analysis);

                // Remove this analysis from the trace's, as the experiment also
                // has an analysis with this name, and it's the one that will be
                // returned by the method
                traceModules.remove(module.getId());
            }

            // Get the trace-only modules from the experiment
            assertTrue(!traceModules.isEmpty());
            for (IAnalysisModule module : traceModules.values()) {
                // Get the module by ID
                IAnalysisModule analysis = scriptModule.getTraceAnalysis(exp, module.getId());
                assertEquals("Trace-only analysis by ID " + module.getId(), module, analysis);

                // Get the module by name, don't assert equality, as some
                // analysis may have the same name as others
                analysis = scriptModule.getTraceAnalysis(exp, module.getName());
                assertNotNull("Trace-only analysis by name " + module.getName(), analysis);
            }

        } finally {
            trace.dispose();
            if (exp != null) {
                exp.dispose();
            }
        }
    }

}
