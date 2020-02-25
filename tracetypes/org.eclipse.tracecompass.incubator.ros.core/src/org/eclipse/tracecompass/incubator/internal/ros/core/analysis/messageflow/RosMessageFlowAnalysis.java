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

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.messageflow;

import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.IRosModelProvider;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.RosModelUtils;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.messageflow.IRosMessageFlowModel;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.messageflow.RosMessageFlowModel;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.messagestransport.IRosMessagesTransportModel;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.messagestransport.RosMessagesTransportModelAnalysis;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.queues.RosQueuesAnalysis;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.google.common.collect.ImmutableSet;

/**
 * ROS message flow analysis, which computes the path of a selected message
 * through ROS nodes, including queues and callbacks
 *
 * @author Christophe Bedard
 */
public class RosMessageFlowAnalysis extends TmfAbstractAnalysisModule implements IRosModelProvider<IRosMessageFlowModel> {

    /** The ID of this analysis module */
    public static final @NonNull String ID = "org.eclipse.tracecompass.incubator.ros.core.analysis.messageflow"; //$NON-NLS-1$
    /** Name for the {@link TargetMessageInfo} parameter */
    public static final @NonNull String TARGET_MESSAGE_INFO_KEY = "target.message.info"; //$NON-NLS-1$

    private @Nullable IProgressMonitor fMonitor;
    private @Nullable RosMessageFlowModel fModel = null;

    /**
     * Constructor
     */
    public RosMessageFlowAnalysis() {
        super();
        setId(ID);
        addParameter(TARGET_MESSAGE_INFO_KEY);
    }

    @Override
    public @Nullable IRosMessageFlowModel getModel() {
        return fModel;
    }

    @Override
    protected boolean executeAnalysis(@NonNull IProgressMonitor monitor) {
        // Target message info
        // Do not execute if it has not been set
        Object info = getParameter(TARGET_MESSAGE_INFO_KEY);
        if (info == null || !(info instanceof TargetMessageInfo)) {
            Activator.getInstance().logError("No target message set; not executing!"); //$NON-NLS-1$
            return false;
        }
        TargetMessageInfo targetMessageInfo = (TargetMessageInfo) info;

        ITmfTrace trace = getTrace();
        if (!(trace instanceof TmfExperiment)) {
            throw new IllegalStateException();
        }
        // Queues
        @Nullable
        ITmfStateSystem queuesStateSystem = RosModelUtils.getStateSystemFromAnalysis(trace, RosQueuesAnalysis.class, RosQueuesAnalysis.getFullAnalysisId());
        if (queuesStateSystem == null) {
            return false;
        }
        // Messages transport model
        @Nullable
        IRosMessagesTransportModel messagesTransportModel = RosModelUtils.getModelFromAnalysis(trace, RosMessagesTransportModelAnalysis.class, RosMessagesTransportModelAnalysis.ID);
        if (messagesTransportModel == null) {
            return false;
        }

        RosMessageFlowModel model = new RosMessageFlowModel(targetMessageInfo, queuesStateSystem, messagesTransportModel);
        model.generateModel();
        fModel = model;
        return fModel.isModelDone() && !monitor.isCanceled();
    }

    @Override
    protected void parameterChanged(@NonNull String name) {
        if (name.equals(TARGET_MESSAGE_INFO_KEY)) {
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
        RosQueuesAnalysis queues = TmfTraceUtils.getAnalysisModuleOfClass(trace, RosQueuesAnalysis.class, RosQueuesAnalysis.getFullAnalysisId());
        RosMessagesTransportModelAnalysis messagesTransportModelAnalysis = TmfTraceUtils.getAnalysisModuleOfClass(trace, RosMessagesTransportModelAnalysis.class, RosMessagesTransportModelAnalysis.ID);
        if (queues == null || messagesTransportModelAnalysis == null) {
            return Collections.emptySet();
        }
        return ImmutableSet.of(queues, messagesTransportModelAnalysis);
    }

    @Override
    protected void canceling() {
        IProgressMonitor mon = fMonitor;
        if ((mon != null) && (!mon.isCanceled())) {
            mon.setCanceled(true);
        }
    }
}
