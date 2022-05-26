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

package org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.executor;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.ros2.core.analysis.AbstractRos2StateProvider;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.HostInfo;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.executor.Ros2ExecutorStateInstance;
import org.eclipse.tracecompass.incubator.internal.ros2.core.model.executor.Ros2ExecutorStateInstance.ExecutorState;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * State provider for the ROS 2 executor analysis.
 *
 * @author Christophe Bedard
 */
public class Ros2ExecutorStateProvider extends AbstractRos2StateProvider {

    private static final int VERSION_NUMBER = 0;

    private final ITmfStateSystem fObjectsSs;

    /**
     * Constructor
     *
     * @param trace
     *            the trace
     * @param objectsSs
     *            the objects state system
     */
    public Ros2ExecutorStateProvider(ITmfTrace trace, ITmfStateSystem objectsSs) {
        super(trace, Ros2ExecutorAnalysis.getFullAnalysisId());
        fObjectsSs = objectsSs;
    }

    @Override
    public int getVersion() {
        return VERSION_NUMBER;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new Ros2ExecutorStateProvider(getTrace(), fObjectsSs);
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        if (!considerEvent(event)) {
            return;
        }

        ITmfStateSystemBuilder ss = Objects.requireNonNull(getStateSystemBuilder());
        long timestamp = event.getTimestamp().toNanos();

        Ros2ExecutorStateInstance state = null;
        if (isEvent(event, LAYOUT.eventRclcppExecutorGetNextReady())) {
            state = new Ros2ExecutorStateInstance(ExecutorState.GET_NEXT_READY, null);
        } else if (isEvent(event, LAYOUT.eventRclcppExecutorWaitForWork())) {
            // TODO use timeout value
            state = new Ros2ExecutorStateInstance(ExecutorState.WAIT_FOR_WORK, null);
        } else if (isEvent(event, LAYOUT.eventRclcppExecutorExecute())) {
            long handle = (long) getField(event, LAYOUT.fieldHandle());
            state = new Ros2ExecutorStateInstance(ExecutorState.EXECUTE, handle);
        }

        if (null != state) {
            int executorQuark = getQuarkAndAdd(ss, event);
            ss.modifyAttribute(timestamp, state, executorQuark);
        }
    }

    private static int getQuarkAndAdd(ITmfStateSystemBuilder ss, @NonNull ITmfEvent event) {
        /**
         * First group by host/trace.
         *
         * Then group by PID and then TID because an executor might use multiple
         * threads, or there might be multiple executor instances using
         * different threads.
         *
         * TODO distinguish between multiple single/multi-threaded executor
         * instances.
         */
        HostInfo hostInfo = hostInfoFrom(event);
        int traceQuark = ss.getQuarkAbsoluteAndAdd(event.getTrace().getName());
        ss.updateOngoingState(hostInfo, traceQuark);
        return ss.getQuarkRelativeAndAdd(traceQuark, getPid(event).toString(), getTid(event).toString());
    }
}
