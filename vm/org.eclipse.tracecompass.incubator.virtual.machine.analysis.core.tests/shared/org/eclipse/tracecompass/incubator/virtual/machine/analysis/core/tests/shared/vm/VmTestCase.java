/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.vm;

import static org.junit.Assume.assumeTrue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.tests.stubs.LinuxTestCase;
import org.eclipse.tracecompass.analysis.os.linux.core.tests.stubs.trace.TmfXmlKernelTraceStub;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 * Test cases for Virtual machine analyses
 *
 * @author Geneviève Bastien
 */
public class VmTestCase extends LinuxTestCase {

    private final VmTestExperiment fExperiment;

    /**
     * Constructor for a virtual machine test case
     *
     * @param experiment
     *            The experiment on which to run
     */
    public VmTestCase(VmTestExperiment experiment) {
        super(experiment.name());
        assumeTrue(experiment.exists());
        fExperiment = experiment;
    }

    @Override
    public @NonNull String getTraceFileName() {
        throw new UnsupportedOperationException("This test case is for an experiment");
    }

    @Override
    public @NonNull TmfXmlKernelTraceStub getKernelTrace() {
        throw new UnsupportedOperationException("This test case is for an experiment");
    }

    /**
     * Get the experiment for this test case. The caller is responsible to destroy
     * this experiment
     *
     * @return The experiment, fully initialized
     */
    public @NonNull TmfExperiment getExperiment() {
        return fExperiment.getExperiment(true);
    }

}
