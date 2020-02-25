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

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.messagestransport;

import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.IRosModelProvider;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.RosModelUtils;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.connections.IRosConnectionsModel;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.connections.RosConnectionsModel;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.connections.RosConnectionsModelAnalysis;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 * ROS messages transport model analysis. Creates a
 * {@link RosMessagesTransportModel} from a {@link RosConnectionsModel} and
 * networking events.
 *
 * @author Christophe Bedard
 */
public class RosMessagesTransportModelAnalysis extends TmfAbstractAnalysisModule implements IRosModelProvider<IRosMessagesTransportModel> {

    /** The ID of this analysis module */
    public static final @NonNull String ID = "org.eclipse.tracecompass.incubator.ros.core.analysis.model.messagestransport"; //$NON-NLS-1$

    private @Nullable IProgressMonitor fMonitor;
    private @Nullable RosMessagesTransportModel fModel = null;

    /**
     * Constructor
     */
    public RosMessagesTransportModelAnalysis() {
        setId(ID);
    }

    @Override
    public @Nullable RosMessagesTransportModel getModel() {
        return fModel;
    }

    @Override
    protected boolean executeAnalysis(@NonNull IProgressMonitor monitor) throws TmfAnalysisException {
        ITmfTrace trace = getTrace();
        if (!(trace instanceof TmfExperiment)) {
            throw new IllegalStateException();
        }
        // Connections model
        @Nullable
        IRosConnectionsModel connectionsModel = RosModelUtils.getModelFromAnalysis(trace, RosConnectionsModelAnalysis.class, RosConnectionsModelAnalysis.ID);
        if (connectionsModel == null) {
            return false;
        }

        RosMessagesTransportModel model = new RosMessagesTransportModel(trace, connectionsModel);
        model.generateModel();
        fModel = model;
        return !monitor.isCanceled();
    }

    @Override
    protected @NonNull Iterable<@NonNull IAnalysisModule> getDependentAnalyses() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return Collections.emptySet();
        }
        RosConnectionsModelAnalysis connectionsModel = TmfTraceUtils.getAnalysisModuleOfClass(trace, RosConnectionsModelAnalysis.class, RosConnectionsModelAnalysis.ID);
        if (connectionsModel == null) {
            return Collections.emptySet();
        }
        return Collections.singleton(connectionsModel);
    }

    @Override
    protected void canceling() {
        IProgressMonitor mon = fMonitor;
        if ((mon != null) && (!mon.isCanceled())) {
            mon.setCanceled(true);
        }
    }
}
