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

package org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messageflow;

import java.util.Arrays;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros2.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.AbstractRos2StateSystemAnalysis;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.IRos2ModelProvider;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messagelinks.Ros2MessageCausalLinksAnalysis;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.messages.Ros2MessagesAnalysis;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.objects.Ros2ObjectsAnalysis;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.Ros2ModelUtils;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messageflow.Ros2FlowTargetInfo;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messageflow.Ros2MessageFlowModel;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.messagelinks.Ros2MessageCausalLinksModel;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * ROS 2 message flow analysis.
 *
 * @author Christophe Bedard
 */
public class Ros2MessageFlowAnalysis extends TmfAbstractAnalysisModule implements IRos2ModelProvider<Ros2MessageFlowModel> {

    /** Name for the {@link Ros2FlowTargetInfo} parameter */
    public static final @NonNull String TARGET_INFO_KEY = "target.info"; //$NON-NLS-1$

    private static final @NonNull String ID_SUFFIX = ".messageflow"; //$NON-NLS-1$

    private @Nullable IProgressMonitor fMonitor;
    private @Nullable Ros2MessageFlowModel fModel = null;

    /**
     * Constructor
     */
    public Ros2MessageFlowAnalysis() {
        super();
        setId(getFullAnalysisId());
        addParameter(TARGET_INFO_KEY);
    }

    /**
     * @return the full ID of this analysis module
     */
    public static @NonNull String getFullAnalysisId() {
        return AbstractRos2StateSystemAnalysis.getAnalysisIdFromSuffix(ID_SUFFIX);
    }

    @Override
    public @Nullable Ros2MessageFlowModel getModel() {
        return fModel;
    }

    @Override
    public boolean canExecute(@NonNull ITmfTrace trace) {
        // See AbstractRos2StateSystemAnalysis#canExecute
        return trace.getParent() == null;
    }

    @Override
    protected boolean executeAnalysis(@NonNull IProgressMonitor monitor) {
        // Get target info, do not execute if it has not been set
        Object info = getParameter(TARGET_INFO_KEY);
        if (!(info instanceof Ros2FlowTargetInfo)) {
            Activator.getInstance().logError("No target set; not executing!"); //$NON-NLS-1$
            return false;
        }
        Ros2FlowTargetInfo targetInfo = (Ros2FlowTargetInfo) info;

        ITmfTrace trace = getTrace();
        if (null == trace) {
            return false;
        }

        // Provide objects state system
        ITmfStateSystem objectsSs = Ros2ModelUtils.getStateSystemFromAnalysis(trace, Ros2ObjectsAnalysis.class, Ros2ObjectsAnalysis.getFullAnalysisId());
        if (null == objectsSs) {
            return false;
        }
        // Provides messages state system
        ITmfStateSystem messagesSs = Ros2ModelUtils.getStateSystemFromAnalysis(trace, Ros2MessagesAnalysis.class, Ros2MessagesAnalysis.getFullAnalysisId());
        if (null == messagesSs) {
            return false;
        }
        // Provide the full message links model
        Ros2MessageCausalLinksModel messageLinksModel = Ros2ModelUtils.getModelFromAnalysis(trace, Ros2MessageCausalLinksAnalysis.class, Ros2MessageCausalLinksAnalysis.getFullAnalysisId());
        if (null == messageLinksModel) {
            return false;
        }

        // Create and generate the model
        Ros2MessageFlowModel model = new Ros2MessageFlowModel(targetInfo, objectsSs, messagesSs, messageLinksModel);
        if (!monitor.isCanceled()) {
            model.generateModel();
        }
        fModel = model;
        return fModel.isDone() && !monitor.isCanceled();
    }

    @Override
    protected void parameterChanged(@NonNull String name) {
        if (name.equals(TARGET_INFO_KEY)) {
            // Re-run analysis
            cancel();
            resetAnalysis();
            schedule();
        }
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
        // Depends on the messages analysis
        Ros2MessagesAnalysis messagesAnalysis = TmfTraceUtils.getAnalysisModuleOfClass(trace, Ros2MessagesAnalysis.class, Ros2MessagesAnalysis.getFullAnalysisId());
        if (messagesAnalysis == null) {
            return Collections.emptySet();
        }
        // Depends on the message links analysis
        Ros2MessageCausalLinksAnalysis messageLinksAnalysis = TmfTraceUtils.getAnalysisModuleOfClass(trace, Ros2MessageCausalLinksAnalysis.class, Ros2MessageCausalLinksAnalysis.getFullAnalysisId());
        if (messageLinksAnalysis == null) {
            return Collections.emptySet();
        }
        return Arrays.asList(objectsAnalysis, messagesAnalysis, messageLinksAnalysis);
    }

    @Override
    protected void canceling() {
        IProgressMonitor mon = fMonitor;
        if ((mon != null) && (!mon.isCanceled())) {
            mon.setCanceled(true);
        }
    }
}
