/**********************************************************************
 * Copyright (c) 2018 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **********************************************************************/

package org.eclipse.tracecompass.incubator.ros.ui.listener;

import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.AbstractRosAnalysis;
import org.eclipse.tracecompass.incubator.internal.ros.ui.views.connections.RosConnectionsView;
import org.eclipse.tracecompass.incubator.internal.ros.ui.views.nodes.RosNodesView;
import org.eclipse.tracecompass.incubator.internal.ros.ui.views.queues.RosQueuesView;
import org.eclipse.tracecompass.incubator.internal.ros.ui.views.tasks.RosTasksView;
import org.eclipse.tracecompass.incubator.internal.ros.ui.views.timers.RosTimersView;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.ITmfNewAnalysisModuleListener;
import org.eclipse.tracecompass.tmf.ui.analysis.TmfAnalysisViewOutput;

/**
 * Registers the ROS views to the ROS analyses.
 *
 * @author Christophe Bedard
 */
public class RosAnalysisListener implements ITmfNewAnalysisModuleListener {

    @Override
    public void moduleCreated(IAnalysisModule module) {
        if (module instanceof AbstractRosAnalysis) {
            module.registerOutput(new TmfAnalysisViewOutput(RosNodesView.ID));
            module.registerOutput(new TmfAnalysisViewOutput(RosQueuesView.ID));
            module.registerOutput(new TmfAnalysisViewOutput(RosTasksView.ID));
            module.registerOutput(new TmfAnalysisViewOutput(RosTimersView.ID));
            module.registerOutput(new TmfAnalysisViewOutput(RosConnectionsView.ID));
        }
    }
}
