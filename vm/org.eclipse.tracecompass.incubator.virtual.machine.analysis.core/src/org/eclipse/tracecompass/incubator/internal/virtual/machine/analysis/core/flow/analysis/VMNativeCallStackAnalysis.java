package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis;


import java.util.Collections;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.profiling.core.instrumented.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 *
 *
 * @author Francois Belias
 */
public class VMNativeCallStackAnalysis extends InstrumentedCallStackAnalysis {
    /**
     * ID of this analysis
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.vm.native.callstack"; //$NON-NLS-1$

    @Override
    public boolean setTrace(ITmfTrace trace) throws TmfAnalysisException {
        if (trace instanceof TmfExperiment) {
            return false;
        }
        return super.setTrace(trace);
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new VMNativeCallStackStateProvider(Objects.requireNonNull(getTrace()));
    }


    @Override
    public Iterable<TmfAbstractAnalysisRequirement> getAnalysisRequirements() {
        // Return empty for now, can be customized later
        return Collections.emptyList();
    }

    @Override
    protected Iterable<IAnalysisModule> getDependentAnalyses() {
        // No dependent analyses for now
        return Collections.emptyList();
    }

}