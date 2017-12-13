/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.overhead;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.overhead.VmOverheadStateProvider;
import org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.vm.VmTestCase;
import org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.vm.VmTestExperiment;
import org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.tests.shared.vm.VmTraces;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.statesystem.core.tests.shared.utils.StateIntervalStub;

import com.google.common.collect.ImmutableList;

/**
 * Test case for the QemuKvm experiment and Fused VM analysis
 *
 * @author Geneviève Bastien
 */
public class OneQemuKvmTestCase extends VmTestCase {

    /**
     * Constructor
     */
    public OneQemuKvmTestCase() {
        super(VmTestExperiment.ONE_QEMUKVM);
    }

    @Override
    public Set<IntervalInfo> getTestIntervals() {
        Set<IntervalInfo> info = new HashSet<>();

        /* Verify the first level of overhead attribute */
        ImmutableList<ITmfStateInterval> intervals = ImmutableList.of(new StateIntervalStub(1, 174, TmfStateValue.nullValue()),
                new StateIntervalStub(175, 224, TmfStateValue.newValueString("RUN")),
                new StateIntervalStub(225, 274, TmfStateValue.nullValue()),
                new StateIntervalStub(275, 374, TmfStateValue.newValueString("RUN")),
                new StateIntervalStub(375, 375, TmfStateValue.nullValue()));
        info.add(new IntervalInfo(intervals, VmOverheadStateProvider.TRACES, VmTraces.GUEST_ONE_QEMUKVM.getHostId(), VmOverheadStateProvider.THREADS, "130", "CallStack", "1"));

        /* Verify the second level of overhead */
        intervals = ImmutableList.of(new StateIntervalStub(1, 194, TmfStateValue.nullValue()),
                new StateIntervalStub(195, 209, TmfStateValue.newValueString("VMM")),
                new StateIntervalStub(210, 294, TmfStateValue.nullValue()),
                new StateIntervalStub(295, 299, TmfStateValue.newValueString("VMM")),
                new StateIntervalStub(300, 349, TmfStateValue.newValueString("VCPU Preempted")),
                new StateIntervalStub(350, 354, TmfStateValue.newValueString("VMM")),
                new StateIntervalStub(355, 375, TmfStateValue.nullValue()));
        info.add(new IntervalInfo(intervals, VmOverheadStateProvider.TRACES, VmTraces.GUEST_ONE_QEMUKVM.getHostId(), VmOverheadStateProvider.THREADS, "130", "CallStack", "2"));

        /* Verify the third level of overhead */
        intervals = ImmutableList.of(new StateIntervalStub(1, 194, TmfStateValue.nullValue()),
                new StateIntervalStub(195, 209, TmfStateValue.newValueString("null")),
                new StateIntervalStub(210, 294, TmfStateValue.nullValue()),
                new StateIntervalStub(295, 299, TmfStateValue.newValueString("null")),
                new StateIntervalStub(300, 349, TmfStateValue.newValueString("0")),
                new StateIntervalStub(350, 354, TmfStateValue.newValueString("null")),
                new StateIntervalStub(355, 375, TmfStateValue.nullValue()));
        info.add(new IntervalInfo(intervals, VmOverheadStateProvider.TRACES, VmTraces.GUEST_ONE_QEMUKVM.getHostId(), VmOverheadStateProvider.THREADS, "130", "CallStack", "3"));

        return info;
    }

    @Override
    public Set<PunctualInfo> getPunctualTestData() {
        return Collections.emptySet();
    }

}
