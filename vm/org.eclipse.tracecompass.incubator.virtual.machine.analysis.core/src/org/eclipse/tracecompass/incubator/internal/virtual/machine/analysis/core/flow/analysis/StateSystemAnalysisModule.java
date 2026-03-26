package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.flow.analysis;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;


/**
 *
 */
public class StateSystemAnalysisModule extends TmfStateSystemAnalysisModule {

    /**
     * Module ID
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.overhead.state.system.module"; //$NON-NLS-1$

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        return new StateProvider(Objects.requireNonNull(getTrace()));
    }

}
