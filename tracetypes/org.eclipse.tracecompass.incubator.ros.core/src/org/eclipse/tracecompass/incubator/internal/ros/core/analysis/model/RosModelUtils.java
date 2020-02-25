/**********************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros.core.Activator;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Utils for ROS models (analyses and models themselves)
 *
 * @author Christophe Bedard
 */
public class RosModelUtils {

    /**
     * Get model from analysis after scheduling and waiting for it to complete
     *
     * @param trace
     *            the trace from which to get the analysis and model
     * @param moduleClass
     *            the analysis module class
     * @param id
     *            the ID of the analysis module
     * @param <M>
     *            the model
     * @param <P>
     *            the analysis that builds/provides the model
     * @return the model, or {@code null} if it failed
     */
    public static <M extends IRosModel, P extends IRosModelProvider<M> & IAnalysisModule> @Nullable M getModelFromAnalysis(
            @NonNull ITmfTrace trace,
            @NonNull Class<@NonNull P> moduleClass,
            @NonNull String id) {
        @Nullable
        P modelAnalysis = TmfTraceUtils.getAnalysisModuleOfClass(trace, moduleClass, id);
        if (modelAnalysis == null) {
            throw new IllegalStateException("There should be an analysis for this class"); //$NON-NLS-1$
        }
        modelAnalysis.schedule();
        if (!modelAnalysis.waitForCompletion()) {
            throw new IllegalStateException("Problem waiting for the analysis"); //$NON-NLS-1$
        }
        @Nullable
        M model = modelAnalysis.getModel();
        if (model == null) {
            Activator.getInstance().logError("Model is null"); //$NON-NLS-1$
        }
        return model;
    }

    /**
     * Get state system from analysis after scheduling and waiting for it to
     * complete
     *
     * @param trace
     *            the trace from which to get the analysis and model
     * @param moduleClass
     *            the analysis module class
     * @param id
     *            the ID of the analysis module
     * @param <A>
     *            the analysis that provides the state system
     * @return the state system, or {@code null} if it failed
     */
    public static <A extends TmfStateSystemAnalysisModule> @Nullable ITmfStateSystem getStateSystemFromAnalysis(
            @NonNull ITmfTrace trace,
            @NonNull Class<@NonNull A> moduleClass,
            @NonNull String id) {
        @Nullable
        A analysis = TmfTraceUtils.getAnalysisModuleOfClass(trace, moduleClass, id);
        if (analysis == null) {
            throw new IllegalStateException("There should be an analysis for this class"); //$NON-NLS-1$
        }
        analysis.schedule();
        if (!analysis.waitForCompletion()) {
            throw new IllegalStateException("Problem waiting for the analysis"); //$NON-NLS-1$
        }
        @Nullable
        ITmfStateSystem ss = analysis.getStateSystem();
        if (ss == null) {
            Activator.getInstance().logError("State system is null"); //$NON-NLS-1$
        }
        return ss;
    }
}
