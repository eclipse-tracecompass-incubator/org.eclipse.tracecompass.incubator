/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.tests.stubs.LinuxTestCase.IntervalInfo;
import org.eclipse.tracecompass.analysis.os.linux.core.tests.stubs.LinuxTestCase.PunctualInfo;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.IVirtualEnvironmentModel;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualMachine;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.analysis.VirtualEnvironment;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.analysis.VirtualEnvironmentBuilder;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.analysis.VirtualMachineModelAnalysis;
import org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.vm.VmTestExperiment;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.tests.shared.utils.StateSystemTestUtils;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test the {@link VirtualMachineModelAnalysis} analysis
 *
 * @author Geneviève Bastien
 */
@RunWith(Parameterized.class)
public class VmModelAnalysisTest {

    private final VmModelTestCase fTestCase;
    private @Nullable VirtualMachineModelAnalysis fAnalysis;
    private final TmfExperiment fExperiment;

    /**
     * Constructor
     *
     * @param testName
     *            A name for the test, to display in the header
     * @param test
     *            A test case parameter for this test
     */
    public VmModelAnalysisTest(String testName, VmModelTestCase test) {
        super();
        fTestCase = test;
        fExperiment = test.getExperiment();
    }

    /**
     * Create the analysis
     *
     * @throws IOException
     *             Exception thrown deleting the supplementary files
     */
    @Before
    public void setUp() throws IOException {
        TmfExperiment experiment = fExperiment;
        deleteSupplementaryFiles(experiment);

        /* Open the traces */
        for (ITmfTrace trace : experiment.getTraces()) {
            ((TmfTrace) trace).traceOpened(new TmfTraceOpenedSignal(this, trace, null));
        }

        experiment.traceOpened(new TmfTraceOpenedSignal(this, experiment, null));

        VirtualMachineModelAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(experiment, VirtualMachineModelAnalysis.class, VirtualMachineModelAnalysis.ID);
        assertNotNull(module);
        module.schedule();
        assertTrue(module.waitForCompletion());
        fAnalysis = module;
    }

    private static void deleteSupplementaryFiles(TmfExperiment experiment) throws IOException {
        // Delete supplementary files
        File suppDir = new File(TmfTraceManager.getSupplementaryFileDir(experiment));
        FileUtils.deleteDirectory(suppDir);
    }

    /**
     * Clean up
     *
     * @throws IOException
     *             Exception thrown deleting the supplementary files
     */
    @After
    public void tearDown() throws IOException {
        deleteSupplementaryFiles(fExperiment);
        fExperiment.dispose();
    }

    /**
     * @return The arrays of parameters
     */
    @Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
                { VmTestExperiment.ONE_QEMUKVM.name(), new OneQemuKvmModelTestCase() },
                // { VmTestExperiment.ONE_CONTAINER.name(), new SimpleContainersTestCase() },
                // { VmTestExperiment.QEMU_CONTAINER.name(), new QemuContainerTestCase() },
                // TODO: Support experiment with multiple physical machines
                // { VmTestExperiment.TWO_HOSTS.name(), new TwoHostsTestCase() },
        });
    }

    /**
     * Test that the analysis executes without problems
     */
    @Test
    public void testAnalysisExecution() {
        VirtualMachineModelAnalysis module = fAnalysis;
        assertNotNull(module);
        assertNotNull(module.getStateSystem());
    }

    /**
     * Test the intervals built by the state provider
     */
    @Test
    public void testStateProviderIntervalData() {
        VirtualMachineModelAnalysis module = fAnalysis;
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
        VirtualMachineModelAnalysis module = fAnalysis;
        assertNotNull(module);

        ITmfStateSystem ss = module.getStateSystem();
        assertNotNull(ss);

        for (@NonNull
        PunctualInfo info : fTestCase.getPunctualTestData()) {
            StateSystemTestUtils.testValuesAtTime(ss, info.getTimestamp(), info.getValues());
        }
    }

    /**
     * Test the virtual environment
     *
     * @throws TmfAnalysisException
     *             Exception thrown by new module
     */
    @Test
    public void testVirtualEnvironmentModel() throws TmfAnalysisException {
        VirtualMachineModelAnalysis module = fAnalysis;
        assertNotNull(module);

        // Test the builder model
        IVirtualEnvironmentModel model = module.getVirtualEnvironmentModel();
        assertTrue(model instanceof VirtualEnvironmentBuilder);
        Collection<VirtualMachine> actual = model.getMachines();
        Collection<VirtualMachine> expected = fTestCase.getMachines();
        assertEquals(expected.size(), actual.size());

        // Create a new instance of the module, it should use the non-builder instance
        module = new VirtualMachineModelAnalysis();
        try {
            module.setId(VirtualMachineModelAnalysis.ID);
            module.setTrace(fExperiment);
            module.schedule();
            assertTrue(module.waitForCompletion());

            model = module.getVirtualEnvironmentModel();
            assertTrue(model instanceof VirtualEnvironment);
            actual = model.getMachines();
            assertEquals(expected.size(), actual.size());
        } finally {
            module.dispose();
        }

    }

}
