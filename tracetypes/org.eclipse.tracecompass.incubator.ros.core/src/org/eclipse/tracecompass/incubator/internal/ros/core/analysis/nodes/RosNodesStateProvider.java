/**********************************************************************
 * Copyright (c) 2018 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.nodes;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.AbstractRosStateProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * State provider for the ROS Nodes analysis
 *
 * @author Christophe Bedard
 */
public class RosNodesStateProvider extends AbstractRosStateProvider {

    private static int VERSION_NUMBER = 0;

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     */
    public RosNodesStateProvider(ITmfTrace trace) {
        super(trace, RosNodesAnalysis.getFullAnalysisId());
    }

    @Override
    public int getVersion() {
        return VERSION_NUMBER;
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        super.eventHandle(event);

        if (!considerEvent(event)) {
            return;
        }

        ITmfStateSystemBuilder ss = Objects.requireNonNull(getStateSystemBuilder());

        long timestamp = event.getTimestamp().toNanos();

        // init_node
        if (isEvent(event, fLayout.eventInitNode())) {
            String nodeNameFull = getNodeName(event);
            long processId = getProcessId(event);

            // Add to nodes list
            int nodeNameQuark = ss.getQuarkAbsoluteAndAdd(nodeNameFull);
            ss.modifyAttribute(timestamp, processId, nodeNameQuark);

            return;
        }

        // shutdown_node
        if (isEvent(event, fLayout.eventShutdownNode())) {
            String nodeName = getNodeName(event);

            // End state in the node's name attribute
            int nodeNameQuark = ss.getQuarkAbsoluteAndAdd(nodeName);
            ss.modifyAttribute(timestamp, (Object) null, nodeNameQuark);

            return;
        }
    }
}
