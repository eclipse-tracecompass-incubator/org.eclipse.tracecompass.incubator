/*******************************************************************************
 * Copyright (c) 2014, 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Mohamad Gebai - Initial API and implementation
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.analysis;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.tid.TidAnalysisModule;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.IVirtualEnvironmentModel;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 * Module for the virtual machine CPU analysis. It tracks the status of the
 * virtual CPUs for each guest of the experiment.
 *
 * @author Mohamad Gebai
 * @author Geneviève Bastien
 */
public class VirtualMachineModelAnalysis extends TmfStateSystemAnalysisModule {

    /**
     * The name of the guest VMs attribute in the state system
     */
    public static final String GUEST_VMS = "Guests"; //$NON-NLS-1$
    /**
     * The name of the CPUs attribute in the state system
     */
    public static final String CPUS = "CPUs"; //$NON-NLS-1$
    /**
     * The name of the process attribute in the state system
     */
    public static final String PROCESS = "Process Id"; //$NON-NLS-1$
    /**
     * The hypervisor attribute in the state system
     */
    public static final String HYPERVISOR = "Hypervisor"; //$NON-NLS-1$

    /** The ID of this analysis module */
    public static final String ID = "org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.model.analysis"; //$NON-NLS-1$

    private @Nullable IVirtualEnvironmentModel fVirtualEnvironment = null;

    /**
     * Constructor
     */
    public VirtualMachineModelAnalysis() {
        super();
    }

    @Override
    protected ITmfStateProvider createStateProvider() {
        ITmfTrace trace = getTrace();
        if (!(trace instanceof TmfExperiment)) {
            throw new IllegalStateException();
        }
        return new VirtualMachineModelStateProvider((TmfExperiment) trace, this);
    }

    @Override
    protected StateSystemBackendType getBackendType() {
        return StateSystemBackendType.FULL;
    }

    @Override
    public String getHelpText() {
        return Messages.getMessage(Messages.VirtualMachineModelAnalysis_Help);
    }

    @Override
    protected Iterable<IAnalysisModule> getDependentAnalyses() {
        Set<IAnalysisModule> modules = new HashSet<>();
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return Collections.emptySet();
        }
        TmfTraceUtils.getAnalysisModulesOfClass(trace, TidAnalysisModule.class).forEach(modules::add);
        return modules;
    }

    @Override
    @TmfSignalHandler
    public void traceClosed(TmfTraceClosedSignal signal) {
        super.traceClosed(signal);
        // Dispose of this analysis, since it is not linked to the trace it will not be
        // automatic
        if (signal.getTrace() == getTrace()) {
            dispose();
        }
    }

    /**
     * Get the virtual environment computed by this analysis. The caller must first
     * make sure this analysis is initialized by calling the
     * {@link #waitForInitialization()} method, otherwise it will throw a
     * NullPointerException.
     *
     * @return The virtual environment
     */
    public synchronized IVirtualEnvironmentModel getVirtualEnvironmentModel() {
        IVirtualEnvironmentModel ve = fVirtualEnvironment;
        if (ve == null) {
            ITmfStateSystem stateSystem = getStateSystem();
            if (stateSystem == null) {
                throw new NullPointerException("State System null: must call #waitForInitialization() before calling this method"); //$NON-NLS-1$
            }
            // If the state system is being built, create a builder model
            if (stateSystem.waitUntilBuilt(0)) {
                ve = new VirtualEnvironment(stateSystem);
            } else {
                ve = new VirtualEnvironmentBuilder((ITmfStateSystemBuilder) stateSystem, this);
            }
            fVirtualEnvironment = ve;
        }
        return ve;
    }

}
