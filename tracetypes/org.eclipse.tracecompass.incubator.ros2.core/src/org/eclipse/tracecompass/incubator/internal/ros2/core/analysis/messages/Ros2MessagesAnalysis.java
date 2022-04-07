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

package org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages;

import java.util.Collections;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.AbstractRos2StateSystemAnalysis;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.objects.Ros2ObjectsAnalysis;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * ROS 2 messages analysis.
 *
 * @author Christophe Bedard
 */
public class Ros2MessagesAnalysis extends AbstractRos2StateSystemAnalysis {

    private static final @NonNull String ID_SUFFIX = ".messages"; //$NON-NLS-1$

    /**
     * Constructor
     */
    public Ros2MessagesAnalysis() {
        super(getFullAnalysisId());
    }

    /**
     * @return the full ID of this analysis module
     */
    public static @NonNull String getFullAnalysisId() {
        return AbstractRos2StateSystemAnalysis.getAnalysisIdFromSuffix(ID_SUFFIX);
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        @NonNull
        ITmfTrace trace = Objects.requireNonNull(getTrace());
        /**
         * Make sure we wait until the Ros2ObjectsAnalysis is done before we
         * create and execute, since we are currently relying on having a full
         * objects state system to simplify processing.
         *
         * FIXME improve this
         */
        Ros2ObjectsAnalysis objectsAnalysis = TmfTraceUtils.getAnalysisModuleOfClass(trace, Ros2ObjectsAnalysis.class, Ros2ObjectsAnalysis.getFullAnalysisId());
        Objects.requireNonNull(objectsAnalysis);
        objectsAnalysis.schedule();
        objectsAnalysis.waitForCompletion();
        ITmfStateSystem ss = getStateSystem(trace, Ros2ObjectsAnalysis.getFullAnalysisId());

        return new Ros2MessagesStateProvider(trace, Objects.requireNonNull(ss));
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
