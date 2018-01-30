/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core;

import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxTidAspect;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.model.IVirtualEnvironmentModel;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * The interface that event handler for virtual machine model should implement
 *
 * @author Geneviève Bastien
 */
public interface IVirtualMachineEventHandler {

    /**
     * Get the events required by this event handler
     *
     * @param layout
     *            The event layout to get events for
     * @return The required events for a given handler
     */
    Set<String> getRequiredEvents(IKernelAnalysisEventLayout layout);

    /**
     * @param ss
     * @param event
     * @param virtEnv
     * @param eventLayout
     */
    void handleEvent(ITmfStateSystemBuilder ss, ITmfEvent event, IVirtualEnvironmentModel virtEnv, IKernelAnalysisEventLayout eventLayout);

    /**
     * Utility method to retrieve the current running thread
     *
     * @param event
     *            The event for which to get the thread
     * @param ts
     *            The timestamp
     * @return The currently running thread or <code>null</code> if no thread is
     *         running
     */
    static @Nullable HostThread getCurrentHostThread(ITmfEvent event, long ts) {
        /* Get the tid of the event */
        Integer tid = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), LinuxTidAspect.class, event);
        if (tid == null) {
            /* We couldn't find any CPU information, ignore this event */
            return null;
        }
        return new HostThread(event.getTrace().getHostId(), tid);
    }

}
