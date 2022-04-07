/**********************************************************************
 * Copyright (c) 2022 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messagelinks;

import java.util.Collections;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.AbstractRos2StateSystemAnalysis;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.IRos2ModelProvider;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.objects.Ros2ObjectsAnalysis;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messagelinks.Ros2MessageCausalLinksModel;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * ROS 2 message causal links analysis. Collects causal links information from
 * annotations.
 *
 * @author Christophe Bedard
 */
public class Ros2MessageCausalLinksAnalysis extends AbstractRos2StateSystemAnalysis implements IRos2ModelProvider<Ros2MessageCausalLinksModel> {

    private static final @NonNull String ID_SUFFIX = ".messagelinks"; //$NON-NLS-1$

    private Ros2MessageCausalLinksModel fModel;

    /**
     * Constructor
     */
    public Ros2MessageCausalLinksAnalysis() {
        super(getFullAnalysisId());
        fModel = new Ros2MessageCausalLinksModel();
    }

    /**
     * @return the full ID of this analysis module
     */
    public static @NonNull String getFullAnalysisId() {
        return AbstractRos2StateSystemAnalysis.getAnalysisIdFromSuffix(ID_SUFFIX);
    }

    @Override
    public @Nullable Ros2MessageCausalLinksModel getModel() {
        return fModel;
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        @NonNull
        ITmfTrace trace = Objects.requireNonNull(getTrace());
        // Provide objects state system and (empty) message links model
        ITmfStateSystem ss = getStateSystem(trace, Ros2ObjectsAnalysis.getFullAnalysisId());
        return new Ros2MessageCausalLinksStateProvider(trace, Objects.requireNonNull(ss), fModel);
    }

    @Override
    protected @NonNull Iterable<@NonNull IAnalysisModule> getDependentAnalyses() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return Collections.emptySet();
        }
        // Depends on the objects analysis
        Ros2ObjectsAnalysis objectsAnalysis = TmfTraceUtils.getAnalysisModuleOfClass(trace, Ros2ObjectsAnalysis.class, Ros2ObjectsAnalysis.getFullAnalysisId());
        if (objectsAnalysis == null) {
            return Collections.emptySet();
        }
        return Collections.singleton(objectsAnalysis);
    }
}
