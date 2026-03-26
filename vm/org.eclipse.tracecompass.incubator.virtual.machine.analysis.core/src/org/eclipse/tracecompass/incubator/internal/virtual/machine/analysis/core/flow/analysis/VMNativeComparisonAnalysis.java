package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 * Analysis module for correlating VM vs Native execution traces
 *
 * This analysis handle two trace types:
 * 1. Native system trace (kernel + user)
 * 2. VM guest trace (guest kernel + user events)
 *
 * @author Francois Belias
 */
public class VMNativeComparisonAnalysis extends TmfStateSystemAnalysisModule {
    /** Analysis ID */
    public static final String ID = "org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis.vmcomparison"; //$NON-NLS-1$

    /**
     * Create the state provider for the analysis.
     * Expects to receive a TmfExperiment (group of traces).
     */
    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        ITmfTrace trace = getTrace();
        if (trace instanceof TmfExperiment) {
            return new VMNativeStateProvider((TmfExperiment) trace);
        }
        throw new IllegalStateException("VMNativeComparisonAnalysis must run on a TmfExperiment"); //$NON-NLS-1$

    }

    @Override
    protected @NonNull String getFullHelpText() {
        return "VM vs Native Flow Analysis"; //$NON-NLS-1$
    }

}
