/*******************************************************************************
 * Copyright (c) 2026 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis;


import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;


/**
 * Analysis module that builds a state system from KVM exit events in a trace.
 * It tracks the number and types of VM exits for each CPU.
 *
 * @author Francois Belias
 */
public class KvmExitAnalysisModule extends TmfStateSystemAnalysisModule {

    /**
     * The ID of this analysis module
     */
    public static final @NonNull String ID = "org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.state.system.module"; //$NON-NLS-1$

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new KvmExitStateProvider(Objects.requireNonNull(getTrace()));
    }


    @Override
    protected @NonNull String getFullHelpText() {
        return "This analysis tracks KVM exit events by CPU. It shows when a virtual machine " + //$NON-NLS-1$
                "exits to the hypervisor, which helps identify performance bottlenecks in " + //$NON-NLS-1$
                "virtualized environments."; //$NON-NLS-1$
    }
}