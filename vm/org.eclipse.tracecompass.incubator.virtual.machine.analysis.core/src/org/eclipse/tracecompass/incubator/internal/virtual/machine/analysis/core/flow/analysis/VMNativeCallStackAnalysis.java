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
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Call stack analysis for VM/Native performance comparison.
 *
 * <p>
 * Builds a call stack state system from kernel traces collected in a native
 * Linux environment and a virtualized KVM/QEMU environment. The analysis
 * window is delimited by a {@code syscall_entry_openat} marker event on
 * {@code ./VM_ANALYSIS.txt} and tracks system calls, hardware interrupts,
 * hypervisor overhead periods, and forked child processes.
 * </p>
 *
 * @see VMNativeCallStackStateProvider
 * @author Francois Belias
 */
public class VMNativeCallStackAnalysis extends InstrumentedCallStackAnalysis {
    /**
     * ID of this analysis
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.vm.native.callstack"; //$NON-NLS-1$

    @Override
    public boolean setTrace(ITmfTrace trace) throws TmfAnalysisException {
        return super.setTrace(trace);
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new VMNativeCallStackStateProvider(Objects.requireNonNull(getTrace()));
    }

}