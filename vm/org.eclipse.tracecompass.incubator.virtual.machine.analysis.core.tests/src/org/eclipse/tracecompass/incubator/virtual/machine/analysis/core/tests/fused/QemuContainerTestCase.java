/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.fused;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.FusedAttributes;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.IVirtualMachineModel;
import org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.vm.VmTestCase;
import org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.vm.VmTestExperiment;
import org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.vm.VmTraces;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.statesystem.core.tests.shared.utils.StateIntervalStub;
import org.eclipse.tracecompass.statesystem.core.tests.shared.utils.StateSystemTestUtils;

import com.google.common.collect.ImmutableList;

/**
 * Test case for one host and one qemu/kvm guest with containers
 *
 * @author Geneviève Bastien
 */
public class QemuContainerTestCase extends VmTestCase {

    private static final ITmfStateValue HOST_SV_STRING = TmfStateValue.newValueString(VmTraces.HOST_QEMUKVM_CONTAINER.getHostId());
    private static final ITmfStateValue GUEST_SV_STRING = TmfStateValue.newValueString(VmTraces.GUEST_QEMUKVM_CONTAINER.getHostId());
    private static final long NS1 = 987654;
    private static final long NS0 = IVirtualMachineModel.ROOT_NAMESPACE;

    /**
     * Constructor
     */
    public QemuContainerTestCase() {
        super(VmTestExperiment.QEMU_CONTAINER);
    }

    @Override
    public Set<IntervalInfo> getTestIntervals() {
        Set<IntervalInfo> info = new LinkedHashSet<>();

        /* Verify the 'CPUs/0' attributes: first the machine */
        ImmutableList<ITmfStateInterval> intervals = ImmutableList.of(new StateIntervalStub(1, 49, TmfStateValue.nullValue()),
                new StateIntervalStub(50, 154, HOST_SV_STRING),
                new StateIntervalStub(155, 194, GUEST_SV_STRING),
                new StateIntervalStub(195, 209, HOST_SV_STRING),
                new StateIntervalStub(210, 244, GUEST_SV_STRING),
                new StateIntervalStub(245, 259, HOST_SV_STRING),
                new StateIntervalStub(260, 294, GUEST_SV_STRING),
                new StateIntervalStub(295, 354, HOST_SV_STRING),
                new StateIntervalStub(355, 375, GUEST_SV_STRING));
        info.add(new IntervalInfo(intervals, FusedAttributes.CPUS, "0", FusedAttributes.MACHINE_NAME));

        /* Verify the current thread */
        intervals = ImmutableList.of(new StateIntervalStub(1, 99, TmfStateValue.nullValue()),
                new StateIntervalStub(100, 149, TmfStateValue.newValueInt(30)),
                new StateIntervalStub(150, 154, TmfStateValue.newValueInt(31)),
                new StateIntervalStub(155, 174, TmfStateValue.newValueInt(131)),
                new StateIntervalStub(175, 194, TmfStateValue.newValueInt(130)),
                new StateIntervalStub(195, 209, TmfStateValue.newValueInt(31)),
                new StateIntervalStub(210, 224, TmfStateValue.newValueInt(130)),
                new StateIntervalStub(225, 244, TmfStateValue.newValueInt(131)),
                new StateIntervalStub(245, 259, TmfStateValue.newValueInt(31)),
                new StateIntervalStub(260, 269, TmfStateValue.newValueInt(131)),
                new StateIntervalStub(270, 274, TmfStateValue.newValueInt(132)),
                new StateIntervalStub(275, 294, TmfStateValue.newValueInt(130)),
                new StateIntervalStub(295, 299, TmfStateValue.newValueInt(31)),
                new StateIntervalStub(300, 349, TmfStateValue.newValueInt(30)),
                new StateIntervalStub(350, 354, TmfStateValue.newValueInt(31)),
                new StateIntervalStub(355, 374, TmfStateValue.newValueInt(130)),
                new StateIntervalStub(375, 375, TmfStateValue.newValueInt(131)));
        info.add(new IntervalInfo(intervals, FusedAttributes.CPUS, "0", FusedAttributes.CURRENT_THREAD));

        return info;
    }

    @Override
    public Set<PunctualInfo> getPunctualTestData() {
        Set<PunctualInfo> info = new LinkedHashSet<>();

        // Check the 'Machines' sub-tree towards the end of the trace
        PunctualInfo oneInfo = new PunctualInfo(300L);
        oneInfo.addValue(StateSystemTestUtils.makeAttribute(FusedAttributes.HOSTS, VmTraces.HOST_QEMUKVM_CONTAINER.getHostId(), FusedAttributes.CPUS, "0"), TmfStateValue.nullValue());
        oneInfo.addValue(StateSystemTestUtils.makeAttribute(FusedAttributes.HOSTS, VmTraces.HOST_QEMUKVM_CONTAINER.getHostId(), FusedAttributes.PARENT), TmfStateValue.nullValue());
        oneInfo.addValue(StateSystemTestUtils.makeAttribute(FusedAttributes.HOSTS, VmTraces.HOST_QEMUKVM_CONTAINER.getHostId(), FusedAttributes.CONTAINERS), TmfStateValue.nullValue());
        oneInfo.addValue(StateSystemTestUtils.makeAttribute(FusedAttributes.HOSTS, VmTraces.HOST_QEMUKVM_CONTAINER.getHostId(), FusedAttributes.MACHINE_NAME), TmfStateValue.newValueString(VmTraces.HOST_QEMUKVM_CONTAINER.getFileName()));

        oneInfo.addValue(StateSystemTestUtils.makeAttribute(FusedAttributes.HOSTS, VmTraces.GUEST_QEMUKVM_CONTAINER.getHostId(), FusedAttributes.CPUS, "0"), TmfStateValue.newValueInt(31));
        oneInfo.addValue(StateSystemTestUtils.makeAttribute(FusedAttributes.HOSTS, VmTraces.GUEST_QEMUKVM_CONTAINER.getHostId(), FusedAttributes.PARENT), HOST_SV_STRING);
        oneInfo.addValue(StateSystemTestUtils.makeAttribute(FusedAttributes.HOSTS, VmTraces.GUEST_QEMUKVM_CONTAINER.getHostId(), FusedAttributes.PCPUS, "0"), TmfStateValue.nullValue());
        oneInfo.addValue(StateSystemTestUtils.makeAttribute(FusedAttributes.HOSTS, VmTraces.GUEST_QEMUKVM_CONTAINER.getHostId(), FusedAttributes.MACHINE_NAME), TmfStateValue.newValueString(VmTraces.GUEST_QEMUKVM_CONTAINER.getFileName()));
        oneInfo.addValue(StateSystemTestUtils.makeAttribute(FusedAttributes.HOSTS, VmTraces.GUEST_QEMUKVM_CONTAINER.getHostId(), FusedAttributes.CONTAINERS), TmfStateValue.nullValue());
        oneInfo.addValue(StateSystemTestUtils.makeAttribute(FusedAttributes.HOSTS, VmTraces.GUEST_QEMUKVM_CONTAINER.getHostId(), FusedAttributes.CONTAINERS, String.valueOf(NS1)), TmfStateValue.nullValue());
        oneInfo.addValue(StateSystemTestUtils.makeAttribute(FusedAttributes.HOSTS, VmTraces.GUEST_QEMUKVM_CONTAINER.getHostId(), FusedAttributes.CONTAINERS, String.valueOf(NS1), FusedAttributes.THREADS, "131"), TmfStateValue.newValueInt(4));
        oneInfo.addValue(StateSystemTestUtils.makeAttribute(FusedAttributes.HOSTS, VmTraces.GUEST_QEMUKVM_CONTAINER.getHostId(), FusedAttributes.CONTAINERS, String.valueOf(NS1), FusedAttributes.THREADS, "132"), TmfStateValue.newValueInt(5));
        oneInfo.addValue(StateSystemTestUtils.makeAttribute(FusedAttributes.HOSTS, VmTraces.GUEST_QEMUKVM_CONTAINER.getHostId(), FusedAttributes.CONTAINERS, String.valueOf(NS0)), TmfStateValue.nullValue());
        oneInfo.addValue(StateSystemTestUtils.makeAttribute(FusedAttributes.HOSTS, VmTraces.GUEST_QEMUKVM_CONTAINER.getHostId(), FusedAttributes.CONTAINERS, String.valueOf(NS0), FusedAttributes.THREADS, "130"), TmfStateValue.newValueInt(130));
        oneInfo.addValue(StateSystemTestUtils.makeAttribute(FusedAttributes.HOSTS, VmTraces.GUEST_QEMUKVM_CONTAINER.getHostId(), FusedAttributes.CONTAINERS, String.valueOf(NS0), FusedAttributes.THREADS, "131"), TmfStateValue.newValueInt(131));
        oneInfo.addValue(StateSystemTestUtils.makeAttribute(FusedAttributes.HOSTS, VmTraces.GUEST_QEMUKVM_CONTAINER.getHostId(), FusedAttributes.CONTAINERS, String.valueOf(NS0), FusedAttributes.THREADS, "132"), TmfStateValue.newValueInt(132));
        info.add(oneInfo);

        return info;
    }

}
