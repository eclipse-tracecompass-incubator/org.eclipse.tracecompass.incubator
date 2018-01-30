/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.model;

import java.util.Collection;

import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualMachine;
import org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.vm.VmTestCase;
import org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.vm.VmTestExperiment;

/**
 * Abstract test case for the VM analysis. Adds the methods to test the
 * generated model.
 *
 * @author Geneviève Bastien
 */
public abstract class VmModelTestCase extends VmTestCase {

    /**
     * Constructor for a virtual machine test case
     *
     * @param experiment
     *            The experiment on which to run
     */
    public VmModelTestCase(VmTestExperiment experiment) {
        super(experiment);
    }

    /**
     * Get the virtual machines contained in the model for this test case. They will
     * be tested against what is obtained by the model
     *
     * @return The machines involved in the analysis
     */
    public abstract Collection<VirtualMachine> getMachines();

}
