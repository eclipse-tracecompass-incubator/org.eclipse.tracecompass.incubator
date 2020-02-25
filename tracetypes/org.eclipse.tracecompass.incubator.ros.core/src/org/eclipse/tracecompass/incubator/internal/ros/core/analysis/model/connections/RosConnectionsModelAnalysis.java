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

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.connections;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.connections.RosConnectionsAnalysis;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.connections.RosConnectionsAnalysisRequirement;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.IRosModelProvider;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.model.RosModelUtils;
import org.eclipse.tracecompass.incubator.internal.ros.core.trace.RosExperiment;
import org.eclipse.tracecompass.incubator.internal.ros.core.trace.RosTrace;
import org.eclipse.tracecompass.incubator.internal.ros.core.trace.layout.IRosEventLayout;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.requirements.TmfAbstractAnalysisRequirement;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

import com.google.common.collect.ImmutableSet;

/**
 * ROS Connections model analysis. Creates a {@link RosConnectionsModel} from
 * the {@link RosConnectionsAnalysis} state system.
 *
 * @author Christophe Bedard
 */
public class RosConnectionsModelAnalysis extends TmfAbstractAnalysisModule implements IRosModelProvider<IRosConnectionsModel> {

    /** The ID of this analysis module */
    public static final @NonNull String ID = "org.eclipse.tracecompass.incubator.ros.core.analysis.model.connections"; //$NON-NLS-1$

    /** The analysis requirements */
    protected @Nullable Set<@NonNull TmfAbstractAnalysisRequirement> fAnalysisRequirements = null;

    private @Nullable IProgressMonitor fMonitor;
    private @Nullable RosConnectionsModel fModel = null;

    /**
     * Constructor
     */
    public RosConnectionsModelAnalysis() {
        setId(ID);
    }

    @Override
    public @Nullable RosConnectionsModel getModel() {
        return fModel;
    }

    @Override
    protected boolean executeAnalysis(@NonNull IProgressMonitor monitor) throws TmfAnalysisException {
        fMonitor = monitor;
        ITmfTrace trace = getTrace();
        if (!(trace instanceof TmfExperiment)) {
            throw new IllegalStateException();
        }
        // Connections
        @Nullable ITmfStateSystem ss = RosModelUtils.getStateSystemFromAnalysis(trace, RosConnectionsAnalysis.class, RosConnectionsAnalysis.getFullAnalysisId());
        if (ss == null) {
            return false;
        }

        RosConnectionsModel model = new RosConnectionsModel(ss, monitor);
        model.generateModel();
        fModel = model;
        return !monitor.isCanceled();
    }

    @Override
    public @NonNull Iterable<@NonNull TmfAbstractAnalysisRequirement> getAnalysisRequirements() {
        // Based on RosConnectionsAnalysis' requirements
        Set<@NonNull TmfAbstractAnalysisRequirement> requirements = fAnalysisRequirements;
        if (requirements == null) {
            RosExperiment experiment = (RosExperiment) getTrace();
            IRosEventLayout layout = IRosEventLayout.getDefault();
            if (experiment != null) {
                List<@NonNull ITmfTrace> traces = experiment.getTraces();
                for (@NonNull ITmfTrace trace : traces) {
                    if (trace instanceof RosTrace) {
                        layout = ((RosTrace) trace).getEventLayout();
                    }
                }
            }
            requirements = ImmutableSet.of(new RosConnectionsAnalysisRequirement(checkNotNull(layout)));
            fAnalysisRequirements = requirements;
        }
        return requirements;
    }

    @Override
    protected @NonNull Iterable<@NonNull IAnalysisModule> getDependentAnalyses() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return Collections.emptySet();
        }
        RosConnectionsAnalysis connections = TmfTraceUtils.getAnalysisModuleOfClass(trace, RosConnectionsAnalysis.class, RosConnectionsAnalysis.getFullAnalysisId());
        if (connections == null) {
            throw new IllegalStateException("There should be an analysis for this class"); //$NON-NLS-1$
        }
        return Collections.singleton(connections);
    }

    @Override
    protected void canceling() {
        IProgressMonitor mon = fMonitor;
        if ((mon != null) && (!mon.isCanceled())) {
            mon.setCanceled(true);
        }
    }
}
