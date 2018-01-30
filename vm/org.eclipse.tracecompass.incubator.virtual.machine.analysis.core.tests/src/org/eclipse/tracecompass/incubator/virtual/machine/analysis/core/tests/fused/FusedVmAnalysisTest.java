/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.fused;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.tests.stubs.LinuxTestCase.IntervalInfo;
import org.eclipse.tracecompass.analysis.os.linux.core.tests.stubs.LinuxTestCase.PunctualInfo;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.FusedVirtualMachineAnalysis;
import org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.vm.VmTestCase;
import org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.vm.VmTestExperiment;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.tests.shared.utils.StateSystemTestUtils;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test the Fused Virtual Machine analysis
 *
 * @author Geneviève Bastien
 */
@RunWith(Parameterized.class)
public class FusedVmAnalysisTest {

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
    public FusedVmAnalysisTest(String testName, VmTestCase test) {
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
                { VmTestExperiment.ONE_QEMUKVM.name(), new OneQemuKvmFusedTestCase() },
                { VmTestExperiment.ONE_CONTAINER.name(), new SimpleContainersTestCase() },
                { VmTestExperiment.QEMU_CONTAINER.name(), new QemuContainerTestCase() },
                // TODO: Support experiment with multiple physical machines
//                { VmTestExperiment.TWO_HOSTS.name(), new TwoHostsTestCase() },
        });
    }

    private FusedVirtualMachineAnalysis setUp() {
        TmfExperiment experiment = fExperiment;

        /* Open the traces */
        for (ITmfTrace trace : experiment.getTraces()) {
            ((TmfTrace) trace).traceOpened(new TmfTraceOpenedSignal(this, trace, null));
        }

        /*
         * TODO For now, make sure the LttngKernelAnalysis have been run for
         * each trace before running the analysis. When event request precedence
         * is implemented, we can remove this
         */
        for (ITmfTrace trace : experiment.getTraces()) {
            for (KernelAnalysisModule module : TmfTraceUtils.getAnalysisModulesOfClass(trace, KernelAnalysisModule.class)) {
                module.schedule();
                module.waitForCompletion();
            }
        }
        /* End of TODO block */

        experiment.traceOpened(new TmfTraceOpenedSignal(this, experiment, null));
        FusedVirtualMachineAnalysis module = null;
        for (FusedVirtualMachineAnalysis mod : TmfTraceUtils.getAnalysisModulesOfClass(experiment, FusedVirtualMachineAnalysis.class)) {
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
        FusedVirtualMachineAnalysis module = setUp();
        assertNotNull(module.getStateSystem());
    }

    /**
     * Test the intervals built by the state provider
     */
    @Test
    public void testStateProviderIntervalData() {
        FusedVirtualMachineAnalysis module = setUp();
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
        FusedVirtualMachineAnalysis module = setUp();
        assertNotNull(module);

        ITmfStateSystem ss = module.getStateSystem();
        assertNotNull(ss);

        for (@NonNull
        PunctualInfo info : fTestCase.getPunctualTestData()) {
            StateSystemTestUtils.testValuesAtTime(ss, info.getTimestamp(), info.getValues());
        }
    }

}
