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

package org.eclipse.tracecompass.incubator.internal.ros.core.analysis.timers;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros.core.Activator;
import org.eclipse.tracecompass.incubator.internal.ros.core.analysis.AbstractRosStateProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * State provider for the ROS Timers analysis
 *
 * @author Christophe Bedard
 */
public class RosTimersStateProvider extends AbstractRosStateProvider {

    private static int VERSION_NUMBER = 0;

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     */
    public RosTimersStateProvider(ITmfTrace trace) {
        super(trace, RosTimersAnalysis.getFullAnalysisId());
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

        try {
            // timer_added
            if (isEvent(event, fLayout.eventTimerAdded())) {
                long timestamp = event.getTimestamp().toNanos();
                String nodeName = getNodeName(event);
                String callbackRef = formatLongDecToHex((Long) getField(event, fLayout.fieldCallbackRef()));

                // Add callback to node's timers list
                int timerCallbackRefQuark = ss.getQuarkAbsoluteAndAdd(nodeName, callbackRef);
                Long timerPeriodNs = getTimerPeriodInNs(event);
                ss.modifyAttribute(timestamp, timerPeriodNs, timerCallbackRefQuark);

                return;
            }

            // timer_scheduled
            if (isEvent(event, fLayout.eventTimerScheduled())) {
                long timestamp = event.getTimestamp().toNanos();
                String nodeName = getNodeName(event);
                String callbackRef = formatLongDecToHex((Long) getField(event, fLayout.fieldCallbackRef()));
                Long callbackQueueCbRef = (Long) getField(event, fLayout.fieldCallbackQueueCbRef());

                // Add callback ref to scheduled queue
                int timerCallbackRefQuark = ss.getQuarkAbsolute(nodeName, callbackRef);
                int timerScheduledQuark = ss.getQuarkRelativeAndAdd(timerCallbackRefQuark, TIMER_SCHEDULED);
                ss.pushAttribute(timestamp, callbackQueueCbRef, timerScheduledQuark);

                return;
            }
        } catch (AttributeNotFoundException e) {
            Activator.getInstance().logError("Could not get timer callback quark; there may be missing events: ", e); //$NON-NLS-1$
        }
    }
}
