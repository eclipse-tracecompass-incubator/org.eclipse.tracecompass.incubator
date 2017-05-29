/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.analysis.core.aspects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.event.aspect.LinuxTidAspect;
import org.eclipse.tracecompass.incubator.analysis.core.model.IHostModel;
import org.eclipse.tracecompass.incubator.analysis.core.model.ModelManager;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * This class is an event aspect to resolve the thread ID from an event. It
 * finds the model for the host the aspect is from and returns the thread ID at
 * the time of the event if available.
 *
 * @author Geneviève Bastien
 */
public class AnalysisTidAspect extends LinuxTidAspect {

    private static final AnalysisTidAspect INSTANCE = new AnalysisTidAspect();

    private AnalysisTidAspect() {
        // Nothing to do
    }

    /**
     * Get the instance of this aspect
     *
     * @return The instance of this aspect
     */
    public static AnalysisTidAspect getInstance() {
        return INSTANCE;
    }

    @Override
    public @Nullable Integer resolve(@NonNull ITmfEvent event) {
        String hostId = event.getTrace().getHostId();
        Object cpuObj = TmfTraceUtils.resolveEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);
        if (cpuObj == null) {
            /* We couldn't find any CPU information, ignore this event */
            return null;
        }
        IHostModel model = ModelManager.getModelFor(hostId);
        int tid = model.getThreadOnCpu((Integer) cpuObj, event.getTimestamp().toNanos());
        if (tid == IHostModel.UNKNOWN_TID) {
            return null;
        }
        return tid;
    }

}
