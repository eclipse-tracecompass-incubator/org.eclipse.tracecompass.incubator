/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.VirtualMachine;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.analysis.VirtualMachineModelAnalysis;
import org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.vm.VmTestExperiment;
import org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.vm.VmTraces;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.statesystem.core.tests.shared.utils.StateSystemTestUtils;

import com.google.common.collect.ImmutableList;

/**
 * Test case for the QemuKvm experiment and Fused VM analysis
 *
 * @author Geneviève Bastien
 */
public class OneQemuKvmModelTestCase extends VmModelTestCase {

    private static final String HOST_ID = VmTraces.HOST_ONE_QEMUKVM.getHostId();
    private static final String GUEST_ID = VmTraces.GUEST_ONE_QEMUKVM.getHostId();

    /**
     * Constructor
     */
    public OneQemuKvmModelTestCase() {
        super(VmTestExperiment.ONE_QEMUKVM);
    }

    @Override
    public Set<IntervalInfo> getTestIntervals() {
        return Collections.emptySet();
    }

    @Override
    public Set<PunctualInfo> getPunctualTestData() {
        Set<PunctualInfo> info = new HashSet<>();

        // Check the 'Machines' sub-tree towards the end of the trace
        PunctualInfo oneInfo = new PunctualInfo(300L);
        oneInfo.addValue(StateSystemTestUtils.makeAttribute(HOST_ID), TmfStateValue.newValueString(VmTraces.HOST_ONE_QEMUKVM.getFileName()));
        oneInfo.addValue(StateSystemTestUtils.makeAttribute(HOST_ID, VirtualMachineModelAnalysis.GUEST_VMS, GUEST_ID), TmfStateValue.newValueString(VmTraces.GUEST_ONE_QEMUKVM.getFileName()));
        oneInfo.addValue(StateSystemTestUtils.makeAttribute(HOST_ID, VirtualMachineModelAnalysis.GUEST_VMS, GUEST_ID, VirtualMachineModelAnalysis.CPUS, "0"), TmfStateValue.newValueInt(31));
        info.add(oneInfo);

        return info;
    }

    @Override
    public Collection<VirtualMachine> getMachines() {
        VirtualMachine host = VirtualMachine.newHostMachine(HOST_ID, HOST_ID);
        VirtualMachine guest = VirtualMachine.newGuestMachine(1, GUEST_ID, GUEST_ID);
        host.addChild(guest);

        return ImmutableList.of(host, guest);
    }

}
