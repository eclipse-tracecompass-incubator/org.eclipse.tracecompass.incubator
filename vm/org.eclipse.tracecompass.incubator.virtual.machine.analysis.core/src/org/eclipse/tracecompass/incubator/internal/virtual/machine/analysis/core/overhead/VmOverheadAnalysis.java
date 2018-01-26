/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.overhead;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.incubator.callstack.core.base.ICallStackElement;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackHostUtils.IHostIdProvider;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackHostUtils.TraceHostIdResolver;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackSeries.IThreadIdProvider;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackSeries.IThreadIdResolver;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackHostUtils;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.InstrumentedCallStackElement;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.FusedVMInformationProvider;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.fused.FusedVirtualMachineAnalysis;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.virtual.resources.VirtualResourcesAnalysis;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.google.common.collect.ImmutableList;

/**
 * This analysis uses a callstack to make statistics on virtual machine
 * overhead. At the first level is the status of a thread from a guest
 * perspective, level 2 detail when the thread is in VMM mode or its VCPU is
 * preempted by the host and level 3 gives reasons or status on that preemption.
 *
 * @author Geneviève Bastien
 */
@SuppressWarnings("restriction")
public class VmOverheadAnalysis extends InstrumentedCallStackAnalysis {

    /** The ID of this analysis module */
    public static final String ID = "org.eclipse.tracecompass.incubator.virtual.machine.analysis.core.overhead.analysis"; //$NON-NLS-1$
    /** The path where the vcpu is kept */
    public static final String HOST_CPU_TID = "hostTid"; //$NON-NLS-1$
    private static final String[] HOST_CPU_TID_PATH = { HOST_CPU_TID };

    private static final String[] DEFAULT_TRACES_PATTERN = new String[] { VmOverheadStateProvider.TRACES, "*" }; //$NON-NLS-1$
    private static final String[] DEFAULT_THREADS_PATTERN = new String[] { VmOverheadStateProvider.THREADS, "*" }; //$NON-NLS-1$

    private static final List<String[]> PATTERNS = ImmutableList.of(DEFAULT_TRACES_PATTERN, DEFAULT_THREADS_PATTERN);

    private @Nullable VirtualResourcesAnalysis getDependentAnalysis() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return null;
        }
        return TmfTraceUtils.getAnalysisModuleOfClass(trace, VirtualResourcesAnalysis.class, VirtualResourcesAnalysis.ID);
    }

    @Override
    protected @NonNull Iterable<@NonNull IAnalysisModule> getDependentAnalyses() {
        VirtualResourcesAnalysis dependentAnalysis = getDependentAnalysis();
        if (dependentAnalysis == null) {
            return Collections.emptySet();
        }
        return Collections.singleton(dependentAnalysis);
    }

    @Override
    protected @NonNull StateSystemBackendType getBackendType() {
        return StateSystemBackendType.FULL;
    }

    @Override
    protected ITmfStateProvider createStateProvider() {
        ITmfTrace trace = getTrace();
        if (!(trace instanceof TmfExperiment)) {
            throw new IllegalStateException();
        }
        return new VmOverheadStateProvider((TmfExperiment) trace);
    }

    /**
     * This class will retrieve the thread ID
     */
    private static final class VirtualCpuThreadProvider implements IThreadIdProvider {

        private final ITmfStateSystem fSs;
        private final int fCpuQuark;

        public VirtualCpuThreadProvider(ITmfStateSystem ss, int quark, String[] path) {
            fSs = ss;
            // Get the cpu quark
            List<@NonNull Integer> quarks = ss.getQuarks(quark, path);
            fCpuQuark = quarks.isEmpty() ? ITmfStateSystem.INVALID_ATTRIBUTE : quarks.get(0);
        }

        @Override
        public int getThreadId(long time) {
            if (fCpuQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                return IHostModel.UNKNOWN_TID;
            }
            // Get the CPU
            try {
                ITmfStateInterval querySingleState = fSs.querySingleState(time, fCpuQuark);

                if (querySingleState.getStateValue().isNull()) {
                    return IHostModel.UNKNOWN_TID;
                }
                return querySingleState.getStateValue().unboxInt();
            } catch (StateSystemDisposedException e) {

            }
            return IHostModel.UNKNOWN_TID;
        }

    }

    /**
     * This class will resolve the thread ID from the CPU on which the callstack was
     * running at a given time
     */
    public static final class VirtualCpuTidResolver implements IThreadIdResolver {

        private String[] fPath;

        /**
         * Constructor
         *
         * @param path
         *            The path relative to the leaf element that will contain the CPU ID
         */
        public VirtualCpuTidResolver(String[] path) {
            fPath = path;
        }

        @Override
        public @Nullable IThreadIdProvider resolve(IHostIdProvider hostProvider, ICallStackElement element) {
            if (!(element instanceof InstrumentedCallStackElement)) {
                throw new IllegalArgumentException();
            }
            InstrumentedCallStackElement insElement = (InstrumentedCallStackElement) element;

            return new VirtualCpuThreadProvider(insElement.getStateSystem(), insElement.getQuark(), fPath);
        }

    }

    @Override
    protected @Nullable IThreadIdResolver getCallStackTidResolver() {
        return new VirtualCpuTidResolver(HOST_CPU_TID_PATH);
    }

    @Override
    protected TraceHostIdResolver getCallStackHostResolver(ITmfTrace trace) {
        // FIXME: There should be a better way to get the host ID
        FusedVirtualMachineAnalysis analysisModule = TmfTraceUtils.getAnalysisModuleOfClass(trace, FusedVirtualMachineAnalysis.class, FusedVirtualMachineAnalysis.ID);
        if (analysisModule == null) {
            return super.getCallStackHostResolver(trace);
        }
        analysisModule.schedule();
        analysisModule.waitForCompletion();
        ITmfStateSystem stateSystem = analysisModule.getStateSystem();
        if (stateSystem == null) {
            return super.getCallStackHostResolver(trace);
        }
        Optional<ITmfTrace> hostTrace = TmfTraceManager.getTraceSet(trace).stream()
                .filter(t -> FusedVMInformationProvider.getParentMachineHostId(stateSystem, t.getHostId()).isEmpty())
                .findFirst();
        if (hostTrace.isPresent()) {
            return new CallStackHostUtils.TraceHostIdResolver(hostTrace.get());
        }
        return super.getCallStackHostResolver(trace);
    }

    @Override
    public @NonNull String getHostId() {
        // The host ID is the one from the host
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return super.getHostId();
        }
        // FIXME: There should be a better way to get the host ID
        FusedVirtualMachineAnalysis analysisModule = TmfTraceUtils.getAnalysisModuleOfClass(trace, FusedVirtualMachineAnalysis.class, FusedVirtualMachineAnalysis.ID);
        if (analysisModule == null) {
            return super.getHostId();
        }
        analysisModule.schedule();
        analysisModule.waitForCompletion();
        ITmfStateSystem stateSystem = analysisModule.getStateSystem();
        if (stateSystem == null) {
            return super.getHostId();
        }
        Optional<ITmfTrace> hostTrace = TmfTraceManager.getTraceSet(trace).stream()
                .filter(t -> FusedVMInformationProvider.getParentMachineHostId(stateSystem, t.getHostId()).isEmpty())
                .findFirst();
        if (hostTrace.isPresent()) {
            return hostTrace.get().getHostId();
        }
        return super.getHostId();
    }

    /**
     * Get the patterns for the process, threads and callstack levels in the state
     * system
     *
     * @return The patterns for the different levels in the state system
     */
    @Override
    protected List<String[]> getPatterns() {
        return PATTERNS;
    }

}
