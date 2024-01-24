/*******************************************************************************
 * Copyright (c) 2024 Ecole Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.gpu.core.handlers;

import org.eclipse.tracecompass.analysis.profiling.core.callstack.CallStackAnalysis;
import org.eclipse.tracecompass.analysis.profiling.core.callstack.CallStackStateProvider;
import org.eclipse.tracecompass.incubator.gpu.core.analysis.GpuCallStackAnalysis;
import org.eclipse.tracecompass.incubator.gpu.core.trace.IGpuTraceEventLayout;
import org.eclipse.tracecompass.incubator.gpu.core.trace.IGpuTraceEventLayout.IApiEventLayout;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider.FutureEventType;

/**
 * Handler for All APIs used to pilot the GPU.
 *
 * This handler handles all API calls that are written to call stacks.
 */
public class ApiEventHandler implements IGpuEventHandler {

    @Override
    public void handleEvent(ITmfEvent event, ITmfStateSystemBuilder ssb, IGpuTraceEventLayout layout, ITmfStateProvider stateProvider) {
        Integer tid = event.getContent().getFieldValue(Integer.class, layout.fieldThreadId());
        if (tid == null) {
            return;
        }
        int rootQuark = ssb.getQuarkAbsoluteAndAdd(GpuCallStackAnalysis.ROOT, CallStackStateProvider.PROCESSES);
        int processQuark = ssb.getQuarkRelativeAndAdd(rootQuark, tid.toString());

        IApiEventLayout apiLayout = layout.getCorrespondingApiLayout(event);
        int apiQuark = ssb.getQuarkRelativeAndAdd(processQuark, apiLayout.getApiName());
        int callStackQuark = ssb.getQuarkRelativeAndAdd(apiQuark, CallStackAnalysis.CALL_STACK);

        if (apiLayout.isBeginEvent()) {
            ssb.pushAttribute(event.getTimestamp().getValue(), apiLayout.getEventName(event), callStackQuark);
        } else {
            ssb.popAttribute(event.getTimestamp().getValue(), callStackQuark);
        }
        if (!layout.fieldDuration().equals("")) { //$NON-NLS-1$
            Long duration = event.getContent().getFieldValue(Long.class, layout.fieldDuration());
            if (duration == null) {
                duration = 1L;
            }
            stateProvider.addFutureEvent(event.getTimestamp().getValue() + duration, event, callStackQuark, FutureEventType.POP);
        }
    }
}
