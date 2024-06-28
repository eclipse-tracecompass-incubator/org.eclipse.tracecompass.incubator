/**********************************************************************
 * Copyright (c) 2023 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.internal.dpdk.core.lcore.analysis;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.tracecompass.analysis.os.linux.core.tid.TidAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement.PriorityLevel;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAnalysisEventRequirement;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

import com.google.common.collect.ImmutableList;

/**
 * This analysis is tracking logical core state changes and service state
 * changes in the state system. This is useful to see which cores of the machine
 * is currently used by which service. The events related to this analysis are
 * part of the Environment Abstraction Layer (EAL) module in DPDK.
 *
 * @author Adel Belkhiri
 * @author Arnaud Fiorini
 */
public class DpdkLogicalCoreAnalysisModule extends TmfStateSystemAnalysisModule {

    /** The ID of this analysis module */
    public static final String ID = "org.eclipse.tracecompass.incubator.dpdk.lcore.analysis"; //$NON-NLS-1$
    private final DpdkLogicalCoreEventLayout fLayout = new DpdkLogicalCoreEventLayout();

    private final TmfAbstractAnalysisRequirement REQUIREMENT = new TmfAnalysisEventRequirement(ImmutableList.of(
            fLayout.eventLcoreStateChange(), fLayout.eventServiceMapLcore()), PriorityLevel.AT_LEAST_ONE);

    @Override
    protected ITmfStateProvider createStateProvider() {
        ITmfTrace trace = checkNotNull(getTrace());

        if (trace instanceof TmfTrace) {
            return new DpdkLogicalCoreStateProvider(trace, fLayout, ID);
        }

        throw new IllegalStateException();
    }

    @Override
    protected Iterable<IAnalysisModule> getDependentAnalyses() {
        Set<IAnalysisModule> modules = new HashSet<>();

        ITmfTrace trace = getTrace();
        if (trace == null) {
            throw new IllegalStateException();
        }
        Iterable<TidAnalysisModule> tidModules = TmfTraceUtils.getAnalysisModulesOfClass(trace, TidAnalysisModule.class);
        for (TidAnalysisModule tidModule : tidModules) {
            modules.add(tidModule);
            break;
        }
        return modules;
    }

    @Override
    public Iterable<TmfAbstractAnalysisRequirement> getAnalysisRequirements() {
        return Collections.singleton(REQUIREMENT);
    }
}
