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

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.tasks;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.AbstractRosStateProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * State provider for the ROS Tasks analysis
 *
 * @author Christophe Bedard
 */
public class RosTasksStateProvider extends AbstractRosStateProvider {

    private static int VERSION_NUMBER = 0;

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     */
    public RosTasksStateProvider(ITmfTrace trace) {
        super(trace, RosTasksAnalysis.getFullAnalysisId());
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

        // task_start
        if (isEvent(event, fLayout.eventTaskStart())) {
            long timestamp = event.getTimestamp().toNanos();
            String nodeName = getNodeName(event);
            String fullTaskName = (String) getField(event, fLayout.fieldTaskName());
            String shortTaskName = extractGenericTaskName(fullTaskName);

            // Add to node's tasks list
            int taskQuark = ss.getQuarkAbsoluteAndAdd(nodeName, shortTaskName);
            ss.modifyAttribute(timestamp, shortTaskName, taskQuark);

            return;
        }
    }
}
