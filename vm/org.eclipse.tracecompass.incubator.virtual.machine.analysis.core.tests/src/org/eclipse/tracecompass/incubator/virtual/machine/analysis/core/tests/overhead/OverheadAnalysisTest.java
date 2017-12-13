/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.overhead;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.tests.stubs.LinuxTestCase.IntervalInfo;
import org.eclipse.tracecompass.analysis.os.linux.core.tests.stubs.LinuxTestCase.PunctualInfo;
import org.eclipse.tracecompass.analysis.os.linux.core.tid.TidAnalysisModule;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.overhead.VmOverheadAnalysis;
import org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.vm.VmTestCase;
import org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.vm.VmTestExperiment;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.tests.shared.utils.StateSystemTestUtils;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test the {@link VmOverheadAnalysis} class
 *
 * @author Geneviève Bastien
 */
@RunWith(Parameterized.class)
public class OverheadAnalysisTest {

    private final VmTestCase fTestCase;
    private final TmfExperiment fExperiment;

    /**
     * Constructor
     *
     * @param testName
     *            A name for the test, to display in the header
     * @param test
     *            A test case parameter for this test
     */
    public OverheadAnalysisTest(String testName, VmTestCase test) {
        super();
        fTestCase = test;
        fExperiment = test.getExperiment();
    }

    /**
     * Clean up
     */
    @After
    public void tearDown() {
        fExperiment.dispose();
    }

    /**
     * @return The arrays of parameters
     */
    @Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
                { VmTestExperiment.ONE_QEMUKVM.name(), new OneQemuKvmTestCase() },
        });
    }

    private VmOverheadAnalysis setUp() {
        TmfExperiment experiment = fExperiment;

        /* Open the traces */
        for (ITmfTrace trace : experiment.getTraces()) {
            TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(this, experiment, null);
            ((TmfTrace) trace).traceOpened(signal);
            TmfTraceManager.getInstance().traceOpened(signal);
        }

        /*
         * TODO For now, make sure the LttngKernelAnalysis have been run for each trace
         * before running the analysis. When event request precedence is implemented, we
         * can remove this
         */
        for (ITmfTrace trace : experiment.getTraces()) {
            for (TidAnalysisModule module : TmfTraceUtils.getAnalysisModulesOfClass(trace, TidAnalysisModule.class)) {
                module.schedule();
                module.waitForCompletion();
            }
        }
        /* End of TODO block */

        TmfTraceOpenedSignal signal = new TmfTraceOpenedSignal(this, experiment, null);
        experiment.traceOpened(signal);
        TmfTraceManager.getInstance().traceOpened(signal);
        VmOverheadAnalysis module = null;
        for (VmOverheadAnalysis mod : TmfTraceUtils.getAnalysisModulesOfClass(experiment, VmOverheadAnalysis.class)) {
            module = mod;
            break;
        }
        assertNotNull(module);
        module.schedule();
        assertTrue(module.waitForCompletion());

        return module;
    }

    /**
     * Test that the analysis executes without problems
     */
    @Test
    public void testAnalysisExecution() {
        VmOverheadAnalysis module = setUp();
        assertNotNull(module.getStateSystem());
    }

    /**
     * Test the intervals built by the state provider
     */
    @Test
    public void testStateProviderIntervalData() {
        VmOverheadAnalysis module = setUp();
        assertNotNull(module);

        ITmfStateSystem ss = module.getStateSystem();
        assertNotNull(ss);

        for (@NonNull
        IntervalInfo info : fTestCase.getTestIntervals()) {
            StateSystemTestUtils.testIntervalForAttributes(ss, info.getIntervals(), info.getAttributePath());
        }
    }

    /**
     * Test the data of attributes at punctual times
     */
    @Test
    public void testStateProviderPunctualData() {
        VmOverheadAnalysis module = setUp();
        assertNotNull(module);

        ITmfStateSystem ss = module.getStateSystem();
        assertNotNull(ss);

        for (@NonNull
        PunctualInfo info : fTestCase.getPunctualTestData()) {
            StateSystemTestUtils.testValuesAtTime(ss, info.getTimestamp(), info.getValues());
        }
    }

}
