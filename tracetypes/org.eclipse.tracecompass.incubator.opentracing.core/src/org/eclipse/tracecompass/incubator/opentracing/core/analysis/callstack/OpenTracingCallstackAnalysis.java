package org.eclipse.tracecompass.incubator.opentracing.core.analysis.callstack;

import java.util.Collections;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;

/**
 *
 */
public class OpenTracingCallstackAnalysis extends SpanCallStackAnalysis {
        /**
         * ID
         */
        public static final @NonNull String ID = "org.eclipse.tracecompass.incubator.opentracing.analysis.callstack"; //$NON-NLS-1$


        @Override
        protected ITmfStateProvider createStateProvider() {
            return new AsincCallStackStateProvider(Objects.requireNonNull(getTrace()));
        }


        @Override
        public @NonNull Iterable<@NonNull TmfAbstractAnalysisRequirement> getAnalysisRequirements() {
            return Collections.emptyList();
        }



    }
